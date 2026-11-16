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
 * Exported background trigger receiver responding to system wake events (screen unlock,
 * clock/timezone changes, charging) and external automation broadcasts to keep scheduled
 * wallpapers and playlists in sync across OEM background restrictions.
 */
class StartupSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return

        val app = context.applicationContext as? RonyWallpaperApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = app.container.repository
                when (action) {
                    ACTION_NEXT_WALLPAPER -> {
                        val active = repo.activePlaylist.first()
                        if (active != null) {
                            repo.skipPlaylistToNext()
                        } else {
                            repo.advancePlaylistIfDue()
                        }
                    }
                    else -> {
                        repo.advancePlaylistIfDue()
                    }
                }
                val isPlaylistActive = repo.activePlaylist.first() != null
                PlaylistSyncScheduler.syncWithPlaylistState(context, isPlaylistActive)
                RonyWallpaperWidgetProvider.updateAllWidgets(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SYNC_WALLPAPER = "com.aistudio.ronylivewallpaper.action.SYNC_WALLPAPER"
        const val ACTION_NEXT_WALLPAPER = "com.aistudio.ronylivewallpaper.action.NEXT_WALLPAPER"

        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_POWER_CONNECTED,
            ACTION_SYNC_WALLPAPER,
            ACTION_NEXT_WALLPAPER
        )
    }
}
