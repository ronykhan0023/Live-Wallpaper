package com.example.domain.model

import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class MediaType {
    VIDEO,
    GIF,
    IMAGE
}

enum class ScaleMode {
    CROP,
    FIT,
    FILL,
    CENTER
}

enum class WallpaperTarget {
    HOME,
    LOCK,
    BOTH
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    BENGALI("bn")
}

enum class SortOption {
    DATE_DESC,
    NAME_ASC,
    SIZE_DESC
}

enum class MediaFilter {
    ALL,
    VIDEOS,
    GIFS,
    PHOTOS
}

enum class SpeedCategory {
    SLOW_MOTION,
    NORMAL,
    FAST_MOTION
}

enum class ScheduleUnit {
    SECONDS,
    MINUTES,
    HOURS,
    DAYS,
    MONTHS;

    fun toApproxMillis(value: Int): Long {
        val safeVal = value.coerceAtLeast(1).toLong()
        return when (this) {
            SECONDS -> safeVal * 1_000L
            MINUTES -> safeVal * 60_000L
            HOURS -> safeVal * 3_600_000L
            DAYS -> safeVal * 86_400_000L
            MONTHS -> safeVal * 30L * 86_400_000L
        }
    }

    fun computeNextSwitchEpochMs(fromEpochMs: Long, value: Int): Long {
        val safeVal = value.coerceAtLeast(1)
        if (this == MONTHS) {
            val cal = Calendar.getInstance().apply { timeInMillis = fromEpochMs }
            cal.add(Calendar.MONTH, safeVal)
            return cal.timeInMillis
        }
        return fromEpochMs + toApproxMillis(safeVal)
    }
}

object SpeedPresets {
    const val MIN_SPEED = 0.25f
    const val MAX_SPEED = 3.00f
    const val DEFAULT_SPEED = 1.00f

    val values: List<Float> = listOf(
        0.25f,
        0.50f,
        0.75f,
        1.00f,
        1.25f,
        1.50f,
        2.00f,
        2.50f,
        3.00f
    )

    fun clamp(speed: Float): Float {
        val clamped = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        return (clamped * 100f).roundToInt() / 100f
    }

    fun classify(speed: Float): SpeedCategory = when {
        speed < 0.98f -> SpeedCategory.SLOW_MOTION
        speed > 1.02f -> SpeedCategory.FAST_MOTION
        else -> SpeedCategory.NORMAL
    }

    fun format(speed: Float): String {
        val clamped = clamp(speed)
        return String.format(Locale.US, "%.2fx", clamped)
    }
}

data class WallpaperItem(
    val id: Long = 0L,
    val title: String,
    val filePath: String,
    val thumbnailPath: String,
    val mediaType: MediaType,
    val mimeType: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val playbackSpeed: Float = SpeedPresets.DEFAULT_SPEED,
    val isLooping: Boolean = true,
    val isMuted: Boolean = true,
    val brightness: Float = 1.0f,
    val scaleMode: ScaleMode = ScaleMode.CROP,
    val zoom: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isActive: Boolean = false,
    val lastUsedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

data class WallpaperPlaylist(
    val id: Long = 0L,
    val name: String,
    val intervalValue: Int = 1,
    val intervalUnit: ScheduleUnit = ScheduleUnit.DAYS,
    val isShuffle: Boolean = false,
    val isActive: Boolean = false,
    val currentIndex: Int = 0,
    val lastSwitchEpochMs: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val items: List<WallpaperItem> = emptyList()
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val defaultSpeed: Float = SpeedPresets.DEFAULT_SPEED,
    val defaultLooping: Boolean = true,
    val batterySaverEnabled: Boolean = false,
    val targetFps: Int = 60,
    val preferHardwareDecoder: Boolean = true,
    val activeWallpaperId: Long? = null,
    val activePlaylistId: Long? = null,
    val preferredTarget: WallpaperTarget = WallpaperTarget.BOTH,
    val hasCompletedOnboarding: Boolean = false
)

data class ActiveWallpaperConfig(
    val wallpaperId: Long? = null,
    val filePath: String? = null,
    val mediaType: MediaType = MediaType.VIDEO,
    val playbackSpeed: Float = SpeedPresets.DEFAULT_SPEED,
    val isLooping: Boolean = true,
    val isMuted: Boolean = true,
    val brightness: Float = 1.0f,
    val scaleMode: ScaleMode = ScaleMode.CROP,
    val zoom: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val activePlaylistId: Long? = null,
    val batterySaverEnabled: Boolean = false,
    val targetFps: Int = 60,
    val preferHardwareDecoder: Boolean = true
)

data class StorageSummary(
    val videoBytes: Long = 0L,
    val gifBytes: Long = 0L,
    val imageBytes: Long = 0L,
    val thumbnailBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val wallpaperCount: Int = 0
)
