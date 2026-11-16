package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ActiveWallpaperConfig
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.MediaType
import com.example.domain.model.ScaleMode
import com.example.domain.model.SpeedPresets
import com.example.domain.model.ThemeMode
import com.example.domain.model.WallpaperItem
import com.example.domain.model.WallpaperTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ronySettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "rony_live_wallpaper_prefs"
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val DEFAULT_LOOPING = booleanPreferencesKey("default_looping")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val TARGET_FPS = intPreferencesKey("target_fps")
        val PREFER_HW_DECODER = booleanPreferencesKey("prefer_hw_decoder")
        val PREFERRED_TARGET = stringPreferencesKey("preferred_target")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")

        // Active wallpaper config synced for instant WallpaperService boot/process recovery
        val ACTIVE_ID = longPreferencesKey("active_id")
        val ACTIVE_PATH = stringPreferencesKey("active_path")
        val ACTIVE_MEDIA_TYPE = stringPreferencesKey("active_media_type")
        val ACTIVE_SPEED = floatPreferencesKey("active_speed")
        val ACTIVE_LOOPING = booleanPreferencesKey("active_looping")
        val ACTIVE_MUTED = booleanPreferencesKey("active_muted")
        val ACTIVE_BRIGHTNESS = floatPreferencesKey("active_brightness")
        val ACTIVE_SCALE_MODE = stringPreferencesKey("active_scale_mode")
        val ACTIVE_ZOOM = floatPreferencesKey("active_zoom")
        val ACTIVE_OFFSET_X = floatPreferencesKey("active_offset_x")
        val ACTIVE_OFFSET_Y = floatPreferencesKey("active_offset_y")
        val ACTIVE_TRIM_START_MS = longPreferencesKey("active_trim_start_ms")
        val ACTIVE_TRIM_END_MS = longPreferencesKey("active_trim_end_ms")
        val ACTIVE_PLAYLIST_ID = longPreferencesKey("active_playlist_id")
    }

    val settingsFlow: Flow<AppSettings> = context.ronySettingsDataStore.data.map { prefs ->
        val rawPlaylistId = prefs[Keys.ACTIVE_PLAYLIST_ID] ?: -1L
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.DARK,
            language = prefs[Keys.LANGUAGE]?.let {
                runCatching { AppLanguage.valueOf(it) }.getOrNull()
            } ?: AppLanguage.ENGLISH,
            defaultSpeed = SpeedPresets.clamp(prefs[Keys.DEFAULT_SPEED] ?: SpeedPresets.DEFAULT_SPEED),
            defaultLooping = prefs[Keys.DEFAULT_LOOPING] ?: true,
            batterySaverEnabled = prefs[Keys.BATTERY_SAVER] ?: false,
            targetFps = (prefs[Keys.TARGET_FPS] ?: 60).coerceIn(24, 60),
            preferHardwareDecoder = prefs[Keys.PREFER_HW_DECODER] ?: true,
            activeWallpaperId = prefs[Keys.ACTIVE_ID],
            activePlaylistId = if (rawPlaylistId > 0L) rawPlaylistId else null,
            preferredTarget = prefs[Keys.PREFERRED_TARGET]?.let {
                runCatching { WallpaperTarget.valueOf(it) }.getOrNull()
            } ?: WallpaperTarget.BOTH,
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false
        )
    }

    val hasCompletedOnboardingFlow: Flow<Boolean> = context.ronySettingsDataStore.data.map { prefs ->
        prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    val activeConfigFlow: Flow<ActiveWallpaperConfig> = context.ronySettingsDataStore.data.map { prefs ->
        val batterySaver = prefs[Keys.BATTERY_SAVER] ?: false
        val targetFps = (prefs[Keys.TARGET_FPS] ?: 60).coerceIn(24, 60)
        val rawPlaylistId = prefs[Keys.ACTIVE_PLAYLIST_ID] ?: -1L
        ActiveWallpaperConfig(
            wallpaperId = prefs[Keys.ACTIVE_ID],
            filePath = prefs[Keys.ACTIVE_PATH],
            mediaType = prefs[Keys.ACTIVE_MEDIA_TYPE]?.let {
                runCatching { MediaType.valueOf(it) }.getOrNull()
            } ?: MediaType.VIDEO,
            playbackSpeed = SpeedPresets.clamp(prefs[Keys.ACTIVE_SPEED] ?: SpeedPresets.DEFAULT_SPEED),
            isLooping = prefs[Keys.ACTIVE_LOOPING] ?: true,
            isMuted = prefs[Keys.ACTIVE_MUTED] ?: true,
            brightness = (prefs[Keys.ACTIVE_BRIGHTNESS] ?: 1.0f).coerceIn(0.3f, 1.2f),
            scaleMode = prefs[Keys.ACTIVE_SCALE_MODE]?.let {
                runCatching { ScaleMode.valueOf(it) }.getOrNull()
            } ?: ScaleMode.CROP,
            zoom = (prefs[Keys.ACTIVE_ZOOM] ?: 1.0f).coerceIn(1.0f, 3.0f),
            offsetX = (prefs[Keys.ACTIVE_OFFSET_X] ?: 0f).coerceIn(-1f, 1f),
            offsetY = (prefs[Keys.ACTIVE_OFFSET_Y] ?: 0f).coerceIn(-1f, 1f),
            trimStartMs = (prefs[Keys.ACTIVE_TRIM_START_MS] ?: 0L).coerceAtLeast(0L),
            trimEndMs = (prefs[Keys.ACTIVE_TRIM_END_MS] ?: 0L).coerceAtLeast(0L),
            activePlaylistId = if (rawPlaylistId > 0L) rawPlaylistId else null,
            batterySaverEnabled = batterySaver,
            targetFps = if (batterySaver) minOf(targetFps, 24) else targetFps,
            preferHardwareDecoder = prefs[Keys.PREFER_HW_DECODER] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.ronySettingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.ronySettingsDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.ronySettingsDataStore.edit { it[Keys.DEFAULT_SPEED] = SpeedPresets.clamp(speed) }
    }

    suspend fun setDefaultLooping(looping: Boolean) {
        context.ronySettingsDataStore.edit { it[Keys.DEFAULT_LOOPING] = looping }
    }

    suspend fun setBatterySaverEnabled(enabled: Boolean) {
        context.ronySettingsDataStore.edit { it[Keys.BATTERY_SAVER] = enabled }
    }

    suspend fun setTargetFps(fps: Int) {
        context.ronySettingsDataStore.edit { it[Keys.TARGET_FPS] = fps.coerceIn(24, 60) }
    }

    suspend fun setPreferHardwareDecoder(prefer: Boolean) {
        context.ronySettingsDataStore.edit { it[Keys.PREFER_HW_DECODER] = prefer }
    }

    suspend fun setPreferredTarget(target: WallpaperTarget) {
        context.ronySettingsDataStore.edit { it[Keys.PREFERRED_TARGET] = target.name }
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.ronySettingsDataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = completed }
    }

    suspend fun setActivePlaylistId(playlistId: Long?) {
        context.ronySettingsDataStore.edit {
            it[Keys.ACTIVE_PLAYLIST_ID] = playlistId ?: -1L
        }
    }

    suspend fun saveActiveWallpaperConfig(item: WallpaperItem) {
        context.ronySettingsDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_ID] = item.id
            prefs[Keys.ACTIVE_PATH] = item.filePath
            prefs[Keys.ACTIVE_MEDIA_TYPE] = item.mediaType.name
            prefs[Keys.ACTIVE_SPEED] = SpeedPresets.clamp(item.playbackSpeed)
            prefs[Keys.ACTIVE_LOOPING] = item.isLooping
            prefs[Keys.ACTIVE_MUTED] = item.isMuted
            prefs[Keys.ACTIVE_BRIGHTNESS] = item.brightness.coerceIn(0.3f, 1.2f)
            prefs[Keys.ACTIVE_SCALE_MODE] = item.scaleMode.name
            prefs[Keys.ACTIVE_ZOOM] = item.zoom.coerceIn(1.0f, 3.0f)
            prefs[Keys.ACTIVE_OFFSET_X] = item.offsetX.coerceIn(-1f, 1f)
            prefs[Keys.ACTIVE_OFFSET_Y] = item.offsetY.coerceIn(-1f, 1f)
            prefs[Keys.ACTIVE_TRIM_START_MS] = item.trimStartMs.coerceAtLeast(0L)
            prefs[Keys.ACTIVE_TRIM_END_MS] = item.trimEndMs.coerceAtLeast(0L)
        }
    }
}
