package com.example.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.RonyWallpaperApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home screen widget providing 1-tap playlist advancement or direct app launch.
 */
class RonyWallpaperWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_NEXT) {
            val app = context.applicationContext as? RonyWallpaperApp ?: return
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val playlist = app.container.repository.activePlaylist.first()
                    if (playlist != null) {
                        app.container.repository.skipPlaylistToNext()
                    } else {
                        app.container.repository.advancePlaylistIfDue()
                    }
                    updateAllWidgetsSync(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_NEXT = "com.aistudio.ronylivewallpaper.action.WIDGET_NEXT_WALLPAPER"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
            val component = ComponentName(context, RonyWallpaperWidgetProvider::class.java)
            val ids = runCatching { appWidgetManager.getAppWidgetIds(component) }.getOrNull() ?: return
            if (ids.isNotEmpty()) {
                updateWidgetsAsync(context, appWidgetManager, ids)
            }
        }

        private fun updateWidgetsAsync(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val (title, subtitle) = loadDisplayText(context)
                for (widgetId in appWidgetIds) {
                    val views = buildRemoteViews(context, title, subtitle)
                    runCatching { appWidgetManager.updateAppWidget(widgetId, views) }
                }
            }
        }

        private suspend fun updateAllWidgetsSync(context: Context) {
            val appWidgetManager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
            val component = ComponentName(context, RonyWallpaperWidgetProvider::class.java)
            val ids = runCatching { appWidgetManager.getAppWidgetIds(component) }.getOrNull() ?: return
            if (ids.isEmpty()) return
            val (title, subtitle) = loadDisplayText(context)
            for (widgetId in ids) {
                val views = buildRemoteViews(context, title, subtitle)
                runCatching { appWidgetManager.updateAppWidget(widgetId, views) }
            }
        }

        private suspend fun loadDisplayText(context: Context): Pair<String, String> {
            val app = context.applicationContext as? RonyWallpaperApp
                ?: return context.getString(R.string.app_name) to context.getString(R.string.app_tagline)
            val playlist = runCatching { app.container.repository.activePlaylist.first() }.getOrNull()
            val wallpaper = runCatching { app.container.repository.activeWallpaper.first() }.getOrNull()
            val title = playlist?.name ?: context.getString(R.string.app_name)
            val subtitle = wallpaper?.title ?: context.getString(R.string.app_tagline)
            return title to subtitle
        }

        private fun buildRemoteViews(
            context: Context,
            title: String,
            subtitle: String
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_rony_wallpaper)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_subtitle, subtitle)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val openPending = PendingIntent.getActivity(
                context,
                4103,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            val nextIntent = Intent(context, RonyWallpaperWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_NEXT
            }
            val nextPending = PendingIntent.getBroadcast(
                context,
                4104,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next_btn, nextPending)
            return views
        }
    }
}
