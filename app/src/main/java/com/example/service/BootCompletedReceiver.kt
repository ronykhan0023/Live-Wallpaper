package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.RonyWallpaperApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores playlist timers, persisted JobScheduler/WorkManager tasks, and widget state
 * automatically on device power-on, quick-boot, or app update.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return

        val app = context.applicationContext as? RonyWallpaperApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = app.container.repository
                repo.advancePlaylistIfDue()
                val isPlaylistActive = repo.activePlaylist.first() != null
                PlaylistSyncScheduler.syncWithPlaylistState(context, isPlaylistActive)
                if (isPlaylistActive) {
                    PlaylistSyncScheduler.enqueueImmediateSync(context)
                }
                RonyWallpaperWidgetProvider.updateAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )
    }
}
