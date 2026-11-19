package com.example.data.repository

import android.net.Uri
import com.example.data.local.MediaStorageManager
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistWithItems
import com.example.data.local.SettingsDataStore
import com.example.data.local.WallpaperDao
import com.example.data.local.WallpaperEntity
import com.example.domain.model.ActiveWallpaperConfig
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.MediaType
import com.example.domain.model.ScaleMode
import com.example.domain.model.ScheduleUnit
import com.example.domain.model.SpeedPresets
import com.example.domain.model.StorageSummary
import com.example.domain.model.ThemeMode
import com.example.domain.model.WallpaperItem
import com.example.domain.model.WallpaperPlaylist
import com.example.domain.model.WallpaperTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class ActiveWallpaperDeleteException : IllegalStateException("Cannot delete active wallpaper")

class WallpaperRepository(
    private val wallpaperDao: WallpaperDao,
    private val playlistDao: PlaylistDao,
    private val settingsDataStore: SettingsDataStore,
    private val mediaStorageManager: MediaStorageManager
) {
    val allWallpapers: Flow<List<WallpaperItem>> =
        wallpaperDao.observeAllWallpapers().map { list -> list.map { it.toDomain() } }

    val favoriteWallpapers: Flow<List<WallpaperItem>> =
        wallpaperDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    val recentlyUsedWallpapers: Flow<List<WallpaperItem>> =
        wallpaperDao.observeRecentlyUsed().map { list -> list.map { it.toDomain() } }

    val activeWallpaper: Flow<WallpaperItem?> =
        wallpaperDao.observeActiveWallpaper().map { it?.toDomain() }

    val allPlaylists: Flow<List<WallpaperPlaylist>> =
        playlistDao.observeAllPlaylists().map { list ->
            list.map { toOrderedPlaylist(it) }
        }

    val activePlaylist: Flow<WallpaperPlaylist?> =
        playlistDao.observeActivePlaylist().map { pair ->
            pair?.let { toOrderedPlaylist(it) }
        }

    val settings: Flow<AppSettings> = settingsDataStore.settingsFlow
    val hasCompletedOnboarding: Flow<Boolean> = settingsDataStore.hasCompletedOnboardingFlow

    val activeWallpaperConfig: Flow<ActiveWallpaperConfig> = settingsDataStore.activeConfigFlow

    fun observeWallpaper(id: Long): Flow<WallpaperItem?> =
        wallpaperDao.observeWallpaperById(id).map { it?.toDomain() }

    private suspend fun toOrderedPlaylist(raw: PlaylistWithItems): WallpaperPlaylist {
        val entries = playlistDao.getOrderedEntries(raw.playlist.id)
        val byId = raw.wallpapers.associateBy { it.id }
        val ordered = entries.mapNotNull { byId[it.wallpaperId] }
        return raw.toDomain(ordered)
    }

    suspend fun importMedia(uri: Uri, forcedType: MediaType? = null): Result<WallpaperItem> {
        val currentSettings = settingsDataStore.settingsFlow.first()
        val importedResult = mediaStorageManager.importMediaFromUri(uri, forcedType)
        return importedResult.map { meta ->
            val entity = WallpaperEntity(
                title = meta.title,
                filePath = meta.filePath,
                thumbnailPath = meta.thumbnailPath,
                mediaType = meta.mediaType.name,
                mimeType = meta.mimeType,
                fileSizeBytes = meta.fileSizeBytes,
                durationMs = meta.durationMs,
                width = meta.width,
                height = meta.height,
                playbackSpeed = currentSettings.defaultSpeed,
                isLooping = currentSettings.defaultLooping,
                isMuted = true,
                volume = 1.0f,
                brightness = 1.0f,
                scaleMode = ScaleMode.CROP.name,
                zoom = 1.0f,
                offsetX = 0f,
                offsetY = 0f,
                trimStartMs = 0L,
                trimEndMs = if (meta.mediaType == MediaType.VIDEO) meta.durationMs else 0L,
                isFavorite = false,
                isActive = false,
                lastUsedAt = 0L,
                createdAt = System.currentTimeMillis()
            )
            val insertedId = wallpaperDao.insertWallpaper(entity)
            entity.copy(id = insertedId).toDomain()
        }
    }

    suspend fun toggleFavorite(id: Long) {
        val existing = wallpaperDao.getWallpaperById(id) ?: return
        wallpaperDao.updateFavorite(id, !existing.isFavorite)
    }

    suspend fun renameWallpaper(id: Long, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isNotEmpty()) {
            wallpaperDao.renameWallpaper(id, trimmed)
        }
    }

    suspend fun deleteWallpaper(id: Long): Result<Unit> {
        val existing = wallpaperDao.getWallpaperById(id)
            ?: return Result.success(Unit)
        if (existing.isActive) {
            return Result.failure(ActiveWallpaperDeleteException())
        }
        wallpaperDao.deleteWallpaperById(id)
        mediaStorageManager.deleteMediaFiles(existing.filePath, existing.thumbnailPath)
        return Result.success(Unit)
    }

    suspend fun replaceWallpaperMedia(targetId: Long, uri: Uri): Result<WallpaperItem> {
        val existing = wallpaperDao.getWallpaperById(targetId)
            ?: return Result.failure(IllegalArgumentException("Wallpaper not found"))
        val importedResult = mediaStorageManager.importMediaFromUri(uri, forcedType = null)
        return importedResult.map { meta ->
            val updatedEntity = existing.copy(
                title = meta.title,
                filePath = meta.filePath,
                thumbnailPath = meta.thumbnailPath,
                mediaType = meta.mediaType.name,
                mimeType = meta.mimeType,
                fileSizeBytes = meta.fileSizeBytes,
                durationMs = meta.durationMs,
                width = meta.width,
                height = meta.height,
                trimStartMs = 0L,
                trimEndMs = if (meta.mediaType == MediaType.VIDEO) meta.durationMs else 0L
            )
            wallpaperDao.updateWallpaper(updatedEntity)
            if (existing.filePath != meta.filePath || existing.thumbnailPath != meta.thumbnailPath) {
                mediaStorageManager.deleteMediaFiles(existing.filePath, existing.thumbnailPath)
            }
            val domain = updatedEntity.toDomain()
            if (domain.isActive) {
                settingsDataStore.saveActiveWallpaperConfig(domain)
            }
            domain
        }
    }

    suspend fun discardSessionImports(ids: Collection<Long>) {
        ids.distinct().forEach { id ->
            val existing = wallpaperDao.getWallpaperById(id) ?: return@forEach
            if (!existing.isActive) {
                wallpaperDao.deleteWallpaperById(id)
                mediaStorageManager.deleteMediaFiles(existing.filePath, existing.thumbnailPath)
            }
        }
    }

    suspend fun cleanupRemovedPlaylistMedia(
        idsToRemove: Collection<Long>,
        sessionImportedIds: Set<Long>
    ) {
        idsToRemove.distinct().forEach { id ->
            val existing = wallpaperDao.getWallpaperById(id) ?: return@forEach
            if (existing.isActive) return@forEach
            val usedInOtherPlaylists = playlistDao.countPlaylistsContainingWallpaper(id) > 0
            if (id in sessionImportedIds || !usedInOtherPlaylists) {
                wallpaperDao.deleteWallpaperById(id)
                mediaStorageManager.deleteMediaFiles(existing.filePath, existing.thumbnailPath)
            }
        }
    }

    suspend fun updateWallpaperCustomization(
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
        volume: Float = 1.0f
    ) {
        val clampedSpeed = SpeedPresets.clamp(playbackSpeed)
        val clampedVolume = volume.coerceIn(0f, 1f)
        val clampedBrightness = brightness.coerceIn(0.3f, 1.2f)
        val clampedZoom = zoom.coerceIn(1.0f, 3.0f)
        val clampedOffsetX = offsetX.coerceIn(-1f, 1f)
        val clampedOffsetY = offsetY.coerceIn(-1f, 1f)
        val safeTrimStart = trimStartMs.coerceAtLeast(0L)
        val safeTrimEnd = if (trimEndMs > 0L) maxOf(trimEndMs, safeTrimStart + 500L) else 0L

        wallpaperDao.updateCustomization(
            id = id,
            playbackSpeed = clampedSpeed,
            isLooping = isLooping,
            isMuted = isMuted,
            volume = clampedVolume,
            brightness = clampedBrightness,
            scaleMode = scaleMode.name,
            zoom = clampedZoom,
            offsetX = clampedOffsetX,
            offsetY = clampedOffsetY,
            trimStartMs = safeTrimStart,
            trimEndMs = safeTrimEnd
        )

        val updated = wallpaperDao.getWallpaperById(id)?.toDomain() ?: return
        if (updated.isActive) {
            settingsDataStore.saveActiveWallpaperConfig(updated)
        }
    }

    suspend fun setActiveWallpaper(id: Long, target: WallpaperTarget): WallpaperItem? {
        val now = System.currentTimeMillis()
        playlistDao.clearAllActive()
        settingsDataStore.setActivePlaylistId(null)
        wallpaperDao.setActiveWallpaper(id, now)
        settingsDataStore.setPreferredTarget(target)
        val active = wallpaperDao.getWallpaperById(id)?.toDomain() ?: return null
        settingsDataStore.saveActiveWallpaperConfig(active)
        return active
    }

    suspend fun createPlaylist(
        name: String,
        wallpaperIds: List<Long>,
        intervalValue: Int,
        intervalUnit: ScheduleUnit,
        isShuffle: Boolean
    ): Long {
        val trimmedName = name.trim().ifBlank { "My Playlist" }
        val playlist = PlaylistEntity(
            name = trimmedName,
            intervalValue = intervalValue.coerceAtLeast(1),
            intervalUnit = intervalUnit.name,
            isShuffle = isShuffle,
            isActive = false,
            currentIndex = 0,
            lastSwitchEpochMs = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        return playlistDao.createPlaylistWithItems(playlist, wallpaperIds)
    }

    suspend fun updatePlaylist(
        playlistId: Long,
        name: String,
        wallpaperIds: List<Long>,
        intervalValue: Int,
        intervalUnit: ScheduleUnit,
        isShuffle: Boolean
    ): WallpaperPlaylist? {
        if (wallpaperIds.isEmpty()) return null
        val before = playlistDao.getPlaylistWithItems(playlistId)?.let { toOrderedPlaylist(it) } ?: return null
        val trimmedName = name.trim().ifBlank { before.name }
        val previousItemId = before.items.getOrNull(before.currentIndex)?.id

        playlistDao.updatePlaylistWithItems(
            playlistId = playlistId,
            name = trimmedName,
            intervalValue = intervalValue.coerceAtLeast(1),
            intervalUnit = intervalUnit.name,
            isShuffle = isShuffle,
            wallpaperIds = wallpaperIds
        )

        val updatedRaw = playlistDao.getPlaylistWithItems(playlistId) ?: return null
        val updated = toOrderedPlaylist(updatedRaw)

        if (updated.isActive && updated.items.isNotEmpty()) {
            val retainedIndex = previousItemId?.let { id ->
                updated.items.indexOfFirst { it.id == id }
            } ?: -1
            val safeIndex = if (retainedIndex >= 0) retainedIndex else 0
            playlistDao.updatePlaylistProgress(
                id = playlistId,
                currentIndex = safeIndex,
                switchEpochMs = updated.lastSwitchEpochMs
            )
            val currentItem = updated.items[safeIndex]
            val now = System.currentTimeMillis()
            wallpaperDao.setActiveWallpaper(currentItem.id, now)
            settingsDataStore.saveActiveWallpaperConfig(currentItem)
            return updated.copy(currentIndex = safeIndex)
        }
        return updated
    }

    suspend fun deletePlaylist(playlistId: Long) {
        val active = playlistDao.getActivePlaylistOnce()
        if (active?.playlist?.id == playlistId) {
            deactivatePlaylist()
        }
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun removeItemFromPlaylist(
        playlistId: Long,
        wallpaperId: Long,
        alsoDeleteIfUnused: Boolean = false
    ): WallpaperPlaylist? {
        val raw = playlistDao.getPlaylistWithItems(playlistId) ?: return null
        val playlist = toOrderedPlaylist(raw)
        val remainingIds = playlist.items.map { it.id }.filter { it != wallpaperId }
        if (remainingIds.isEmpty()) return playlist
        val updated = updatePlaylist(
            playlistId = playlistId,
            name = playlist.name,
            wallpaperIds = remainingIds,
            intervalValue = playlist.intervalValue,
            intervalUnit = playlist.intervalUnit,
            isShuffle = playlist.isShuffle
        )
        if (alsoDeleteIfUnused) {
            cleanupRemovedPlaylistMedia(listOf(wallpaperId), emptySet())
        }
        return updated
    }

    suspend fun replaceItemInPlaylist(
        playlistId: Long,
        oldWallpaperId: Long,
        uri: Uri
    ): Result<WallpaperPlaylist?> {
        val raw = playlistDao.getPlaylistWithItems(playlistId)
            ?: return Result.failure(IllegalArgumentException("Playlist not found"))
        val playlist = toOrderedPlaylist(raw)
        val usageCount = playlistDao.countPlaylistsContainingWallpaper(oldWallpaperId)
        return if (usageCount <= 1) {
            replaceWallpaperMedia(oldWallpaperId, uri).map {
                val refreshed = playlistDao.getPlaylistWithItems(playlistId)
                refreshed?.let { toOrderedPlaylist(it) }
            }
        } else {
            importMedia(uri, forcedType = null).map { newItem ->
                val newIds = playlist.items.map { item ->
                    if (item.id == oldWallpaperId) newItem.id else item.id
                }
                updatePlaylist(
                    playlistId = playlistId,
                    name = playlist.name,
                    wallpaperIds = newIds,
                    intervalValue = playlist.intervalValue,
                    intervalUnit = playlist.intervalUnit,
                    isShuffle = playlist.isShuffle
                )
            }
        }
    }

    suspend fun activatePlaylist(playlistId: Long, target: WallpaperTarget): WallpaperPlaylist? {
        val raw = playlistDao.getPlaylistWithItems(playlistId) ?: return null
        val playlist = toOrderedPlaylist(raw)
        if (playlist.items.isEmpty()) return null

        val now = System.currentTimeMillis()
        val startIndex = 0
        playlistDao.activatePlaylist(playlistId, startIndex, now)
        settingsDataStore.setPreferredTarget(target)
        settingsDataStore.setActivePlaylistId(playlistId)

        val firstItem = playlist.items[startIndex]
        wallpaperDao.setActiveWallpaper(firstItem.id, now)
        settingsDataStore.saveActiveWallpaperConfig(firstItem)
        return playlist.copy(isActive = true, currentIndex = startIndex, lastSwitchEpochMs = now)
    }

    suspend fun deactivatePlaylist() {
        playlistDao.clearAllActive()
        settingsDataStore.setActivePlaylistId(null)
    }

    /**
     * Checks the active playlist schedule against current time. If one or more intervals
     * (seconds, minutes, hours, days, or months) have elapsed, advances the playlist item
     * and updates Room + DataStore so the WallpaperService immediately switches media.
     *
     * @return epoch milliseconds when the next switch is due, or null if no playlist is active.
     */
    suspend fun advancePlaylistIfDue(nowMs: Long = System.currentTimeMillis()): Long? {
        val raw = playlistDao.getActivePlaylistOnce() ?: return null
        val playlist = toOrderedPlaylist(raw)
        if (playlist.items.isEmpty()) return null

        val unit = playlist.intervalUnit
        val intervalValue = playlist.intervalValue.coerceAtLeast(1)
        var switchEpoch = playlist.lastSwitchEpochMs
        var index = playlist.currentIndex.coerceIn(0, playlist.items.lastIndex)
        var advanced = false

        var nextDue = unit.computeNextSwitchEpochMs(switchEpoch, intervalValue)
        var guard = 0
        while (nowMs >= nextDue && guard < 1000) {
            switchEpoch = nextDue
            index = if (playlist.isShuffle && playlist.items.size > 1) {
                var candidate = Random.nextInt(playlist.items.size)
                if (candidate == index) candidate = (index + 1) % playlist.items.size
                candidate
            } else {
                (index + 1) % playlist.items.size
            }
            nextDue = unit.computeNextSwitchEpochMs(switchEpoch, intervalValue)
            advanced = true
            guard++
        }

        if (advanced) {
            playlistDao.updatePlaylistProgress(playlist.id, index, switchEpoch)
            val chosen = playlist.items[index]
            wallpaperDao.setActiveWallpaper(chosen.id, nowMs)
            settingsDataStore.saveActiveWallpaperConfig(chosen)
        } else {
            // Ensure active config matches current playlist item
            val currentItem = playlist.items[index]
            val activeCfg = settingsDataStore.activeConfigFlow.first()
            if (activeCfg.wallpaperId != currentItem.id) {
                wallpaperDao.setActiveWallpaper(currentItem.id, nowMs)
                settingsDataStore.saveActiveWallpaperConfig(currentItem)
            }
        }
        return nextDue
    }

    suspend fun skipPlaylistToNext(): WallpaperItem? {
        val raw = playlistDao.getActivePlaylistOnce() ?: return null
        val playlist = toOrderedPlaylist(raw)
        if (playlist.items.isEmpty()) return null
        val now = System.currentTimeMillis()
        val nextIdx = (playlist.currentIndex + 1) % playlist.items.size
        playlistDao.updatePlaylistProgress(playlist.id, nextIdx, now)
        val chosen = playlist.items[nextIdx]
        wallpaperDao.setActiveWallpaper(chosen.id, now)
        settingsDataStore.saveActiveWallpaperConfig(chosen)
        return chosen
    }

    suspend fun getStorageSummary(): StorageSummary {
        val all = wallpaperDao.getAllWallpapersOnce().map { it.toDomain() }
        return mediaStorageManager.calculateStorageSummary(all)
    }

    suspend fun cleanOrphanStorage(): Int {
        val all = wallpaperDao.getAllWallpapersOnce().map { it.toDomain() }
        return mediaStorageManager.clearOrphanFiles(all)
    }

    suspend fun setThemeMode(mode: ThemeMode) = settingsDataStore.setThemeMode(mode)
    suspend fun setLanguage(language: AppLanguage) = settingsDataStore.setLanguage(language)
    suspend fun setDefaultSpeed(speed: Float) = settingsDataStore.setDefaultSpeed(speed)
    suspend fun setDefaultLooping(looping: Boolean) = settingsDataStore.setDefaultLooping(looping)
    suspend fun setBatterySaverEnabled(enabled: Boolean) = settingsDataStore.setBatterySaverEnabled(enabled)
    suspend fun setTargetFps(fps: Int) = settingsDataStore.setTargetFps(fps)
    suspend fun setPreferHardwareDecoder(prefer: Boolean) = settingsDataStore.setPreferHardwareDecoder(prefer)
    suspend fun setHasCompletedOnboarding(completed: Boolean) = settingsDataStore.setHasCompletedOnboarding(completed)
}
