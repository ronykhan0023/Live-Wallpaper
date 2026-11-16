package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity
import com.example.R
import com.example.RonyWallpaperApp
import com.example.util.WallpaperHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * System Quick Settings tile letting the user advance an active wallpaper playlist
 * or jump straight into Rony Live Wallpaper from the notification shade.
 */
class RonyQuickTileService : TileService() {

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        refreshTileState()
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext as? RonyWallpaperApp ?: return
        tileScope.launch {
            val activePlaylist = runCatching {
                app.container.repository.activePlaylist.first()
            }.getOrNull()

            if (activePlaylist != null) {
                runCatching {
                    app.container.repository.skipPlaylistToNext()
                    RonyWallpaperWidgetProvider.updateAllWidgets(applicationContext)
                }
                refreshTileState()
            } else {
                val launchIntent = Intent(this@RonyQuickTileService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pi = PendingIntent.getActivity(
                        this@RonyQuickTileService,
                        4105,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    startActivityAndCollapse(pi)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(launchIntent)
                }
            }
        }
    }

    private fun refreshTileState() {
        val tile = qsTile ?: return
        val app = applicationContext as? RonyWallpaperApp ?: return
        tileScope.launch {
            val activePlaylist = runCatching { app.container.repository.activePlaylist.first() }.getOrNull()
            val activeWallpaper = runCatching { app.container.repository.activeWallpaper.first() }.getOrNull()
            val isLive = WallpaperHelper.isRonyWallpaperActive(this@RonyQuickTileService)

            tile.icon = Icon.createWithResource(this@RonyQuickTileService, R.drawable.ic_tile_wallpaper)
            tile.label = activePlaylist?.name
                ?: activeWallpaper?.title
                ?: getString(R.string.quick_tile_label)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (activePlaylist != null) {
                    getString(R.string.widget_next_action)
                } else {
                    getString(R.string.app_name)
                }
            }
            tile.state = if (isLive || activePlaylist != null) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        tileScope.cancel()
        super.onDestroy()
    }
}
