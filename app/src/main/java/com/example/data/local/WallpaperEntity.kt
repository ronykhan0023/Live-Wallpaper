package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.MediaType
import com.example.domain.model.ScaleMode
import com.example.domain.model.SpeedPresets
import com.example.domain.model.WallpaperItem

@Entity(tableName = "wallpapers")
data class WallpaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val filePath: String,
    val thumbnailPath: String,
    val mediaType: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val playbackSpeed: Float = SpeedPresets.DEFAULT_SPEED,
    val isLooping: Boolean = true,
    val isMuted: Boolean = true,
    val brightness: Float = 1.0f,
    val scaleMode: String = ScaleMode.CROP.name,
    val zoom: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isActive: Boolean = false,
    val lastUsedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): WallpaperItem = WallpaperItem(
        id = id,
        title = title,
        filePath = filePath,
        thumbnailPath = thumbnailPath,
        mediaType = runCatching { MediaType.valueOf(mediaType) }.getOrDefault(MediaType.VIDEO),
        mimeType = mimeType,
        fileSizeBytes = fileSizeBytes,
        durationMs = durationMs,
        width = width,
        height = height,
        playbackSpeed = playbackSpeed,
        isLooping = isLooping,
        isMuted = isMuted,
        brightness = brightness,
        scaleMode = runCatching { ScaleMode.valueOf(scaleMode) }.getOrDefault(ScaleMode.CROP),
        zoom = zoom,
        offsetX = offsetX,
        offsetY = offsetY,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        isFavorite = isFavorite,
        isActive = isActive,
        lastUsedAt = lastUsedAt,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: WallpaperItem): WallpaperEntity = WallpaperEntity(
            id = item.id,
            title = item.title,
            filePath = item.filePath,
            thumbnailPath = item.thumbnailPath,
            mediaType = item.mediaType.name,
            mimeType = item.mimeType,
            fileSizeBytes = item.fileSizeBytes,
            durationMs = item.durationMs,
            width = item.width,
            height = item.height,
            playbackSpeed = item.playbackSpeed,
            isLooping = item.isLooping,
            isMuted = item.isMuted,
            brightness = item.brightness,
            scaleMode = item.scaleMode.name,
            zoom = item.zoom,
            offsetX = item.offsetX,
            offsetY = item.offsetY,
            trimStartMs = item.trimStartMs,
            trimEndMs = item.trimEndMs,
            isFavorite = item.isFavorite,
            isActive = item.isActive,
            lastUsedAt = item.lastUsedAt,
            createdAt = item.createdAt
        )
    }
}
