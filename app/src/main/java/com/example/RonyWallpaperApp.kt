package com.example

import android.app.Application
import com.example.data.local.MediaStorageManager
import com.example.data.local.RonyDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.WallpaperRepository
import com.example.service.PlaylistSyncScheduler
import com.example.service.RonyWallpaperWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    val database: RonyDatabase by lazy { RonyDatabase.getInstance(application) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(application) }
    val mediaStorageManager: MediaStorageManager by lazy { MediaStorageManager(application) }
    val repository: WallpaperRepository by lazy {
        WallpaperRepository(
            wallpaperDao = database.wallpaperDao(),
            playlistDao = database.playlistDao(),
            settingsDataStore = settingsDataStore,
            mediaStorageManager = mediaStorageManager
        )
    }
}

class RonyWallpaperApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        observeBackgroundSyncState()
    }

    private fun observeBackgroundSyncState() {
        appScope.launch {
            runCatching {
                container.repository.activePlaylist
                    .map { it != null }
                    .distinctUntilChanged()
                    .collectLatest { hasActivePlaylist ->
                        PlaylistSyncScheduler.syncWithPlaylistState(this@RonyWallpaperApp, hasActivePlaylist)
                        RonyWallpaperWidgetProvider.updateAllWidgets(this@RonyWallpaperApp)
                    }
            }
        }
        appScope.launch {
            runCatching {
                container.repository.activeWallpaperConfig
                    .map { it.wallpaperId to it.activePlaylistId }
                    .distinctUntilChanged()
                    .collectLatest {
                        RonyWallpaperWidgetProvider.updateAllWidgets(this@RonyWallpaperApp)
                    }
            }
        }
    }
}
