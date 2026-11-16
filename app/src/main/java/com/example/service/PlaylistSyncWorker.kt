package com.example.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.RonyWallpaperApp

/**
 * Background WorkManager worker that evaluates scheduled playlist transitions
 * after device boot, app updates, or periodic wake-ups even if the UI is closed.
 */
class PlaylistSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RonyWallpaperApp ?: return Result.success()
        runCatching {
            app.container.repository.advancePlaylistIfDue()
            RonyWallpaperWidgetProvider.updateAllWidgets(applicationContext)
        }
        return Result.success()
    }
}
