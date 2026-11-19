package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ActiveWallpaperDeleteException
import com.example.data.repository.WallpaperRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.MediaFilter
import com.example.domain.model.MediaType
import com.example.domain.model.ScaleMode
import com.example.domain.model.ScheduleUnit
import com.example.domain.model.SortOption
import com.example.domain.model.StorageSummary
import com.example.domain.model.ThemeMode
import com.example.domain.model.WallpaperItem
import com.example.domain.model.WallpaperPlaylist
import com.example.domain.model.WallpaperTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiEvent {
    data class Imported(val wallpaperId: Long) : UiEvent
    data class BatchImported(val count: Int) : UiEvent
    data object ImportFailed : UiEvent
    data object ActiveWallpaperDeleteBlocked : UiEvent
    data class CacheCleared(val filesRemoved: Int) : UiEvent
    data object SettingsSynced : UiEvent
    data object PlaylistActivated : UiEvent
    data object PlaylistUpdated : UiEvent
    data object MediaReplaced : UiEvent
    data object MediaReplaceFailed : UiEvent
    data object PlaylistLastItemProtected : UiEvent
}

class WallpaperViewModel(
    private val repository: WallpaperRepository
) : ViewModel() {

    val allWallpapers: StateFlow<List<WallpaperItem>> = repository.allWallpapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<WallpaperItem>> = repository.favoriteWallpapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyUsed: StateFlow<List<WallpaperItem>> = repository.recentlyUsedWallpapers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeWallpaper: StateFlow<WallpaperItem?> = repository.activeWallpaper
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val playlists: StateFlow<List<WallpaperPlaylist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePlaylist: StateFlow<WallpaperPlaylist?> = repository.activePlaylist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val onboardingCompleted: StateFlow<Boolean?> = repository.hasCompletedOnboarding
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _storageSummary = MutableStateFlow(StorageSummary())
    val storageSummary: StateFlow<StorageSummary> = _storageSummary.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _mediaFilter = MutableStateFlow(MediaFilter.ALL)
    val mediaFilter: StateFlow<MediaFilter> = _mediaFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val filteredWallpapers: StateFlow<List<WallpaperItem>> = combine(
        allWallpapers,
        _searchQuery,
        _mediaFilter,
        _sortOption
    ) { items, query, filter, sort ->
        val filtered = items.filter { item ->
            val matchesQuery = query.isBlank() || item.title.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                MediaFilter.ALL -> true
                MediaFilter.VIDEOS -> item.mediaType == MediaType.VIDEO
                MediaFilter.GIFS -> item.mediaType == MediaType.GIF
                MediaFilter.PHOTOS -> item.mediaType == MediaType.IMAGE
            }
            matchesQuery && matchesFilter
        }
        when (sort) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
            SortOption.NAME_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortOption.SIZE_DESC -> filtered.sortedByDescending { it.fileSizeBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allWallpapers.collect {
                _storageSummary.value = repository.getStorageSummary()
            }
        }
    }

    fun observeWallpaper(id: Long): Flow<WallpaperItem?> = repository.observeWallpaper(id)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMediaFilter(filter: MediaFilter) {
        _mediaFilter.value = filter
    }

    fun setSortOption(sort: SortOption) {
        _sortOption.value = sort
    }

    fun importMedia(uri: Uri, forcedType: MediaType? = null) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = repository.importMedia(uri, forcedType)
            _isImporting.value = false
            result.onSuccess { imported ->
                _storageSummary.value = repository.getStorageSummary()
                _events.emit(UiEvent.Imported(imported.id))
            }.onFailure {
                _events.emit(UiEvent.ImportFailed)
            }
        }
    }

    fun importMultipleMedia(uris: List<Uri>, forcedType: MediaType? = null) {
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            importMedia(uris.first(), forcedType)
            return
        }
        viewModelScope.launch {
            _isImporting.value = true
            var successCount = 0
            uris.forEach { uri ->
                if (repository.importMedia(uri, forcedType).isSuccess) {
                    successCount++
                }
            }
            _isImporting.value = false
            _storageSummary.value = repository.getStorageSummary()
            if (successCount > 0) {
                _events.emit(UiEvent.BatchImported(successCount))
            } else {
                _events.emit(UiEvent.ImportFailed)
            }
        }
    }

    fun importForPlaylistSession(
        uris: List<Uri>,
        forcedType: MediaType? = null,
        onImported: (List<Long>) -> Unit
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            val importedIds = mutableListOf<Long>()
            uris.forEach { uri ->
                repository.importMedia(uri, forcedType).onSuccess { item ->
                    importedIds.add(item.id)
                }
            }
            _isImporting.value = false
            _storageSummary.value = repository.getStorageSummary()
            if (importedIds.isNotEmpty()) {
                onImported(importedIds)
            } else {
                _events.emit(UiEvent.ImportFailed)
            }
        }
    }

    fun discardSessionImports(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.discardSessionImports(ids)
            _storageSummary.value = repository.getStorageSummary()
        }
    }

    fun replaceWallpaperMedia(targetId: Long, uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = repository.replaceWallpaperMedia(targetId, uri)
            _isImporting.value = false
            result.onSuccess {
                _storageSummary.value = repository.getStorageSummary()
                _events.emit(UiEvent.MediaReplaced)
            }.onFailure {
                _events.emit(UiEvent.MediaReplaceFailed)
            }
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun renameWallpaper(id: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameWallpaper(id, newTitle)
        }
    }

    fun deleteWallpaper(id: Long) {
        viewModelScope.launch {
            val result = repository.deleteWallpaper(id)
            result.onFailure { err ->
                if (err is ActiveWallpaperDeleteException) {
                    _events.emit(UiEvent.ActiveWallpaperDeleteBlocked)
                }
            }.onSuccess {
                _storageSummary.value = repository.getStorageSummary()
            }
        }
    }

    fun updateWallpaperCustomization(
        id: Long,
        playbackSpeed: Float,
        isLooping: Boolean,
        isMuted: Boolean,
        brightness: Float,
        scaleMode: ScaleMode,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        trimStartMs: Long,
        trimEndMs: Long,
        notifyIfActive: Boolean = false,
        volume: Float = 1.0f
    ) {
        viewModelScope.launch {
            repository.updateWallpaperCustomization(
                id = id,
                playbackSpeed = playbackSpeed,
                isLooping = isLooping,
                isMuted = isMuted,
                brightness = brightness,
                scaleMode = scaleMode,
                zoom = zoom,
                offsetX = offsetX,
                offsetY = offsetY,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                volume = volume
            )
            if (notifyIfActive) {
                _events.emit(UiEvent.SettingsSynced)
            }
        }
    }

    fun activateAndPrepareWallpaper(
        id: Long,
        target: WallpaperTarget,
        onReadyForSystemPicker: () -> Unit
    ) {
        viewModelScope.launch {
            repository.setActiveWallpaper(id, target)
            onReadyForSystemPicker()
        }
    }

    fun createPlaylist(
        name: String,
        wallpaperIds: List<Long>,
        intervalValue: Int,
        intervalUnit: ScheduleUnit,
        isShuffle: Boolean,
        activateImmediately: Boolean = false,
        target: WallpaperTarget = WallpaperTarget.BOTH,
        onActivated: (() -> Unit)? = null
    ) {
        if (wallpaperIds.isEmpty()) return
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(
                name = name,
                wallpaperIds = wallpaperIds,
                intervalValue = intervalValue,
                intervalUnit = intervalUnit,
                isShuffle = isShuffle
            )
            if (activateImmediately) {
                repository.activatePlaylist(playlistId, target)
                _events.emit(UiEvent.PlaylistActivated)
                onActivated?.invoke()
            }
        }
    }

    fun updatePlaylist(
        playlistId: Long,
        name: String,
        wallpaperIds: List<Long>,
        intervalValue: Int,
        intervalUnit: ScheduleUnit,
        isShuffle: Boolean
    ) {
        if (wallpaperIds.isEmpty()) return
        viewModelScope.launch {
            repository.updatePlaylist(
                playlistId = playlistId,
                name = name,
                wallpaperIds = wallpaperIds,
                intervalValue = intervalValue,
                intervalUnit = intervalUnit,
                isShuffle = isShuffle
            )
            _events.emit(UiEvent.PlaylistUpdated)
        }
    }

    fun activatePlaylist(
        playlistId: Long,
        target: WallpaperTarget,
        onReadyForSystemPicker: () -> Unit
    ) {
        viewModelScope.launch {
            repository.activatePlaylist(playlistId, target)
            _events.emit(UiEvent.PlaylistActivated)
            onReadyForSystemPicker()
        }
    }

    fun deactivatePlaylist() {
        viewModelScope.launch { repository.deactivatePlaylist() }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { repository.deletePlaylist(playlistId) }
    }

    fun removeItemFromPlaylist(playlistId: Long, wallpaperId: Long) {
        viewModelScope.launch {
            val before = playlists.value.find { it.id == playlistId }
            if (before != null && before.items.size <= 1) {
                _events.emit(UiEvent.PlaylistLastItemProtected)
                return@launch
            }
            repository.removeItemFromPlaylist(
                playlistId = playlistId,
                wallpaperId = wallpaperId,
                alsoDeleteIfUnused = true
            )
            _storageSummary.value = repository.getStorageSummary()
            _events.emit(UiEvent.PlaylistUpdated)
        }
    }

    fun replaceItemInPlaylist(playlistId: Long, oldWallpaperId: Long, uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = repository.replaceItemInPlaylist(playlistId, oldWallpaperId, uri)
            _isImporting.value = false
            result.onSuccess {
                _storageSummary.value = repository.getStorageSummary()
                _events.emit(UiEvent.MediaReplaced)
            }.onFailure {
                _events.emit(UiEvent.MediaReplaceFailed)
            }
        }
    }

    fun commitPlaylistEditor(
        existingPlaylistId: Long?,
        name: String,
        wallpaperIds: List<Long>,
        intervalValue: Int,
        intervalUnit: ScheduleUnit,
        isShuffle: Boolean,
        target: WallpaperTarget,
        stagedCustomizations: Map<Long, WallpaperItem>,
        pendingDeleteIds: Set<Long>,
        replacedOldIds: Set<Long>,
        sessionImportedIds: Set<Long>,
        onActivatedAfterCreate: (() -> Unit)? = null
    ) {
        if (wallpaperIds.isEmpty()) return
        viewModelScope.launch {
            // 1. Apply any staged per-item adjustments only for retained items
            val retainedSet = wallpaperIds.toSet()
            stagedCustomizations.values.forEach { item ->
                if (item.id in retainedSet) {
                    repository.updateWallpaperCustomization(
                        id = item.id,
                        playbackSpeed = item.playbackSpeed,
                        isLooping = item.isLooping,
                        isMuted = item.isMuted,
                        brightness = item.brightness,
                        scaleMode = item.scaleMode,
                        zoom = item.zoom,
                        offsetX = item.offsetX,
                        offsetY = item.offsetY,
                        trimStartMs = item.trimStartMs,
                        trimEndMs = item.trimEndMs,
                        volume = item.volume
                    )
                }
            }

            // 2. Create or update playlist (this updates active wallpaper if active item changed)
            if (existingPlaylistId != null) {
                repository.updatePlaylist(
                    playlistId = existingPlaylistId,
                    name = name,
                    wallpaperIds = wallpaperIds,
                    intervalValue = intervalValue,
                    intervalUnit = intervalUnit,
                    isShuffle = isShuffle
                )
                _events.emit(UiEvent.PlaylistUpdated)
            } else {
                val createdId = repository.createPlaylist(
                    name = name,
                    wallpaperIds = wallpaperIds,
                    intervalValue = intervalValue,
                    intervalUnit = intervalUnit,
                    isShuffle = isShuffle
                )
                repository.activatePlaylist(createdId, target)
                _events.emit(UiEvent.PlaylistActivated)
                onActivatedAfterCreate?.invoke()
            }

            // 3. Clean up deleted, replaced, or unselected session-imported media
            val unusedSessionIds = sessionImportedIds.filter { it !in retainedSet }
            val cleanupIds = (pendingDeleteIds + replacedOldIds + unusedSessionIds)
                .filter { it !in retainedSet }
            if (cleanupIds.isNotEmpty()) {
                repository.cleanupRemovedPlaylistMedia(cleanupIds, sessionImportedIds)
            }
            _storageSummary.value = repository.getStorageSummary()
        }
    }

    fun skipPlaylistNext() {
        viewModelScope.launch { repository.skipPlaylistToNext() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch { repository.setDefaultSpeed(speed) }
    }

    fun setDefaultLooping(looping: Boolean) {
        viewModelScope.launch { repository.setDefaultLooping(looping) }
    }

    fun setBatterySaverEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBatterySaverEnabled(enabled) }
    }

    fun setTargetFps(fps: Int) {
        viewModelScope.launch { repository.setTargetFps(fps) }
    }

    fun setPreferHardwareDecoder(prefer: Boolean) {
        viewModelScope.launch { repository.setPreferHardwareDecoder(prefer) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.setHasCompletedOnboarding(true) }
    }

    fun cleanOrphanStorage() {
        viewModelScope.launch {
            val removed = repository.cleanOrphanStorage()
            _storageSummary.value = repository.getStorageSummary()
            _events.emit(UiEvent.CacheCleared(removed))
        }
    }

    class Factory(private val repository: WallpaperRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WallpaperViewModel(repository) as T
        }
    }
}
