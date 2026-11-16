package com.example.service

import android.app.job.JobParameters
import android.app.job.JobService
import com.example.RonyWallpaperApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Persisted JobService scheduled when a wallpaper playlist is active so Android's
 * JobScheduler can re-invoke background playlist sync across reboots.
 */
class RonyPlaylistJobService : JobService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        val app = applicationContext as? RonyWallpaperApp
        if (app == null) {
            jobFinished(params, false)
            return false
        }

        activeJob = serviceScope.launch {
            runCatching {
                app.container.repository.advancePlaylistIfDue()
                RonyWallpaperWidgetProvider.updateAllWidgets(applicationContext)
            }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        activeJob?.cancel()
        return true
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
