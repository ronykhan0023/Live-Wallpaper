package com.example.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Coordinates WorkManager + JobScheduler persisted background scheduling so
 * live wallpaper playlists keep rotating reliably across reboots and OEM process sweeps.
 */
object PlaylistSyncScheduler {

    private const val PERIODIC_WORK_NAME = "rony_playlist_periodic_sync"
    private const val IMMEDIATE_WORK_NAME = "rony_playlist_immediate_sync"
    const val PLAYLIST_JOB_ID = 4102

    fun enqueueImmediateSync(context: Context) {
        runCatching {
            val request = OneTimeWorkRequestBuilder<PlaylistSyncWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun syncWithPlaylistState(context: Context, isPlaylistActive: Boolean) {
        val appContext = context.applicationContext
        if (isPlaylistActive) {
            schedulePeriodicWork(appContext)
            schedulePersistedJob(appContext)
        } else {
            cancelBackgroundSync(appContext)
        }
    }

    private fun schedulePeriodicWork(context: Context) {
        runCatching {
            val periodic = PeriodicWorkRequestBuilder<PlaylistSyncWorker>(
                15L,
                TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic
            )
        }
    }

    private fun schedulePersistedJob(context: Context) {
        runCatching {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                ?: return
            val existing = scheduler.allPendingJobs.any { it.id == PLAYLIST_JOB_ID }
            if (existing) return

            val component = ComponentName(context, RonyPlaylistJobService::class.java)
            val jobInfo = JobInfo.Builder(PLAYLIST_JOB_ID, component)
                .setPersisted(true)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15L))
                .build()
            scheduler.schedule(jobInfo)
        }
    }

    private fun cancelBackgroundSync(context: Context) {
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        }
        runCatching {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            scheduler?.cancel(PLAYLIST_JOB_ID)
        }
    }

    fun isPersistedJobScheduled(context: Context): Boolean {
        return runCatching {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                ?: return false
            scheduler.allPendingJobs.any { it.id == PLAYLIST_JOB_ID }
        }.getOrDefault(false)
    }
}
