package com.example

import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Looper
import android.service.wallpaper.WallpaperService
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MediaStorageManager
import com.example.data.local.RonyDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.WallpaperEntity
import com.example.data.repository.ActiveWallpaperDeleteException
import com.example.data.repository.WallpaperRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.MediaType
import com.example.domain.model.ScaleMode
import com.example.domain.model.ScheduleUnit
import com.example.domain.model.SpeedCategory
import com.example.domain.model.SpeedPresets
import com.example.domain.model.ThemeMode
import com.example.domain.model.WallpaperTarget
import com.example.engine.ExoPlayerAudioHelper
import com.example.engine.GifFrameDecoder
import android.appwidget.AppWidgetManager
import android.service.quicksettings.TileService
import com.example.service.BootCompletedReceiver
import com.example.service.RonyLiveWallpaperService
import com.example.service.RonyPlaylistJobService
import com.example.service.RonyQuickTileService
import com.example.service.RonyWallpaperWidgetProvider
import com.example.service.StartupSyncReceiver
import com.example.ui.i18n.BengaliStrings
import com.example.ui.i18n.EnglishStrings
import com.example.util.WallpaperHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: RonyDatabase
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var storageManager: MediaStorageManager
    private lateinit var repository: WallpaperRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, RonyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsDataStore = SettingsDataStore(context)
        storageManager = MediaStorageManager(context)
        repository = WallpaperRepository(
            wallpaperDao = database.wallpaperDao(),
            playlistDao = database.playlistDao(),
            settingsDataStore = settingsDataStore,
            mediaStorageManager = storageManager
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `app name matches Rony Live Wallpaper`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Rony Live Wallpaper", appName)
    }

    @Test
    fun `wallpaper service is registered with BIND_WALLPAPER permission`() {
        val intent = Intent(WallpaperService.SERVICE_INTERFACE)
        intent.component = ComponentName(context, RonyLiveWallpaperService::class.java)
        val resolveInfo = context.packageManager.resolveService(
            intent,
            PackageManager.GET_META_DATA
        )
        assertNotNull(resolveInfo)
        assertEquals(
            android.Manifest.permission.BIND_WALLPAPER,
            resolveInfo?.serviceInfo?.permission
        )
    }

    @Test
    fun `speed presets clamp and classify correctly`() {
        assertEquals(0.25f, SpeedPresets.clamp(0.05f), 0.001f)
        assertEquals(0.40f, SpeedPresets.clamp(0.40f), 0.001f)
        assertEquals(3.00f, SpeedPresets.clamp(5.0f), 0.001f)
        assertEquals(1.37f, SpeedPresets.clamp(1.374f), 0.001f)
        assertEquals(SpeedCategory.SLOW_MOTION, SpeedPresets.classify(0.40f))
        assertEquals(SpeedCategory.NORMAL, SpeedPresets.classify(1.00f))
        assertEquals(SpeedCategory.FAST_MOTION, SpeedPresets.classify(2.50f))
        assertEquals("0.25x", SpeedPresets.format(0.25f))
        assertEquals("0.40x", SpeedPresets.format(0.40f))
        assertEquals("3.00x", SpeedPresets.format(3.00f))
    }

    @Test
    fun `import gif copies to persistent internal storage and persists speed and active protection`() = runTest {
        val tempSource = File(context.cacheDir, "sample_anim.gif")
        tempSource.writeBytes(buildMinimalGifBytes())

        val result = repository.importMedia(Uri.fromFile(tempSource), MediaType.GIF)
        assertTrue(result.isSuccess)
        val imported = result.getOrThrow()
        assertEquals(MediaType.GIF, imported.mediaType)
        assertTrue(File(imported.filePath).exists())

        // Delete original source to verify persistence independence
        tempSource.delete()
        assertTrue(File(imported.filePath).exists())

        // Activate wallpaper & customize speed + video trim range
        repository.setActiveWallpaper(imported.id, WallpaperTarget.BOTH)
        repository.updateWallpaperCustomization(
            id = imported.id,
            playbackSpeed = 2.25f,
            isLooping = true,
            isMuted = true,
            brightness = 0.85f,
            scaleMode = ScaleMode.FIT,
            zoom = 1.5f,
            offsetX = 0.2f,
            offsetY = -0.1f,
            trimStartMs = 1500L,
            trimEndMs = 8500L
        )

        val activeConfig = repository.activeWallpaperConfig.first()
        assertEquals(2.25f, activeConfig.playbackSpeed, 0.001f)
        assertEquals(ScaleMode.FIT, activeConfig.scaleMode)
        assertEquals(1.5f, activeConfig.zoom, 0.001f)
        assertEquals(0.85f, activeConfig.brightness, 0.001f)
        assertEquals(1500L, activeConfig.trimStartMs)
        assertEquals(8500L, activeConfig.trimEndMs)

        // Active wallpaper cannot be accidentally deleted
        val deleteResult = repository.deleteWallpaper(imported.id)
        assertTrue(deleteResult.isFailure)
        assertTrue(deleteResult.exceptionOrNull() is ActiveWallpaperDeleteException)
    }

    @Test
    fun `settings persist theme language and battery saver`() = runTest {
        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setLanguage(AppLanguage.BENGALI)
        repository.setBatterySaverEnabled(true)
        repository.setTargetFps(30)
        repository.setDefaultSpeed(1.75f)

        val settings = repository.settings.first()
        assertEquals(ThemeMode.LIGHT, settings.themeMode)
        assertEquals(AppLanguage.BENGALI, settings.language)
        assertTrue(settings.batterySaverEnabled)
        assertEquals(30, settings.targetFps)
        assertEquals(1.75f, settings.defaultSpeed, 0.001f)
    }

    @Test
    fun `gif frame decoder decodes valid minimal gif`() {
        val decoder = GifFrameDecoder(buildMinimalGifBytes())
        assertTrue(decoder.isValid)
        assertEquals(1, decoder.frameCount)
        assertEquals(2, decoder.width)
        assertEquals(2, decoder.height)
        val delay = decoder.advance()
        assertTrue(delay >= 16)
        val bmp = decoder.getNextFrameBitmap()
        assertNotNull(bmp)
        decoder.release()
    }

    @Test
    fun `non-active wallpaper deletes cleanly from room and disk`() = runTest {
        val file = File(context.filesDir, "wallpapers/test_delete.mp4").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }
        val id = database.wallpaperDao().insertWallpaper(
            WallpaperEntity(
                title = "Temp Video",
                filePath = file.absolutePath,
                thumbnailPath = "",
                mediaType = MediaType.VIDEO.name,
                mimeType = "video/mp4",
                fileSizeBytes = 4L,
                durationMs = 5000L,
                width = 1080,
                height = 1920,
                isActive = false
            )
        )
        val res = repository.deleteWallpaper(id)
        assertTrue(res.isSuccess)
        assertFalse(file.exists())
    }

    @Test
    fun `custom photo logo and uploaded payment drawables exist and whatsapp strings hide phone number`() {
        assertNotNull(context.getDrawable(R.drawable.ic_rony_logo_photo))
        assertNotNull(context.getDrawable(R.drawable.bg_gta6_edition))
        assertNotNull(context.getDrawable(R.drawable.ic_whatsapp))
        assertNotNull(context.getDrawable(R.drawable.img_bkash_logo))
        assertNotNull(context.getDrawable(R.drawable.img_nagad_logo))

        assertFalse(EnglishStrings.connectWithDeveloper.contains("01637360712"))
        assertFalse(EnglishStrings.connectWhatsAppSubtitle.contains("01637360712"))
        assertFalse(BengaliStrings.connectWithDeveloper.contains("01637360712"))
        assertFalse(BengaliStrings.connectWhatsAppSubtitle.contains("01637360712"))

        assertEquals("01637360712", WallpaperHelper.DONATION_NUMBER)
        assertTrue(WallpaperHelper.copyDonationNumber(context))
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("01637360712", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun `import image photo copies cleanly and counts toward image storage`() = runTest {
        val tempPhoto = File(context.cacheDir, "sample_photo.png")
        val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        tempPhoto.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()

        val result = repository.importMedia(Uri.fromFile(tempPhoto), MediaType.IMAGE)
        assertTrue(result.isSuccess)
        val imported = result.getOrThrow()
        assertEquals(MediaType.IMAGE, imported.mediaType)
        assertTrue(File(imported.filePath).exists())

        val summary = repository.getStorageSummary()
        assertTrue(summary.imageBytes > 0L)
    }

    @Test
    fun `playlist schedules rotate across seconds days and months accurately`() = runTest {
        val id1 = database.wallpaperDao().insertWallpaper(
            WallpaperEntity(
                title = "Video 1",
                filePath = "/tmp/v1.mp4",
                thumbnailPath = "",
                mediaType = MediaType.VIDEO.name,
                mimeType = "video/mp4",
                fileSizeBytes = 100L,
                durationMs = 10_000L,
                width = 1080,
                height = 1920
            )
        )
        val id2 = database.wallpaperDao().insertWallpaper(
            WallpaperEntity(
                title = "Video 2",
                filePath = "/tmp/v2.mp4",
                thumbnailPath = "",
                mediaType = MediaType.VIDEO.name,
                mimeType = "video/mp4",
                fileSizeBytes = 100L,
                durationMs = 10_000L,
                width = 1080,
                height = 1920
            )
        )

        val playlistId = repository.createPlaylist(
            name = "Daily Mix",
            wallpaperIds = listOf(id1, id2),
            intervalValue = 1,
            intervalUnit = ScheduleUnit.DAYS,
            isShuffle = false
        )
        val activated = repository.activatePlaylist(playlistId, WallpaperTarget.BOTH)
        assertNotNull(activated)
        assertEquals(id1, repository.activeWallpaperConfig.first().wallpaperId)

        // Advance time by 1 day + 1 second -> should switch to Video 2
        val futureMs = activated!!.lastSwitchEpochMs + ScheduleUnit.DAYS.toApproxMillis(1) + 1000L
        val nextDue = repository.advancePlaylistIfDue(futureMs)
        assertNotNull(nextDue)
        assertEquals(id2, repository.activeWallpaperConfig.first().wallpaperId)

        // Verify MONTHS calendar computation advances cleanly (~28..31 days)
        val base = 1_700_000_000_000L
        val plusOneMonth = ScheduleUnit.MONTHS.computeNextSwitchEpochMs(base, 1)
        assertTrue(plusOneMonth - base >= 28L * 86_400_000L)
    }

    @Test
    fun `editing existing playlist adds new items updates interval and preserves active state`() = runTest {
        val id1 = database.wallpaperDao().insertWallpaper(
            WallpaperEntity(
                title = "Photo 1",
                filePath = "/tmp/p1.jpg",
                thumbnailPath = "",
                mediaType = MediaType.IMAGE.name,
                mimeType = "image/jpeg",
                fileSizeBytes = 100L,
                durationMs = 0L,
                width = 1080,
                height = 1920
            )
        )
        val id2 = database.wallpaperDao().insertWallpaper(
            WallpaperEntity(
                title = "Photo 2",
                filePath = "/tmp/p2.jpg",
                thumbnailPath = "",
                mediaType = MediaType.IMAGE.name,
                mimeType = "image/jpeg",
                fileSizeBytes = 100L,
                durationMs = 0L,
                width = 1080,
                height = 1920
            )
        )
        val id3 = database.wallpaperDao().insertWallpaper(
            WallpaperEntity(
                title = "Photo 3",
                filePath = "/tmp/p3.jpg",
                thumbnailPath = "",
                mediaType = MediaType.IMAGE.name,
                mimeType = "image/jpeg",
                fileSizeBytes = 100L,
                durationMs = 0L,
                width = 1080,
                height = 1920
            )
        )

        val playlistId = repository.createPlaylist(
            name = "Initial Mix",
            wallpaperIds = listOf(id1, id2),
            intervalValue = 15,
            intervalUnit = ScheduleUnit.SECONDS,
            isShuffle = false
        )
        repository.activatePlaylist(playlistId, WallpaperTarget.BOTH)

        // Edit existing playlist to rename, change interval, and add Photo 3
        val updated = repository.updatePlaylist(
            playlistId = playlistId,
            name = "Expanded Mix",
            wallpaperIds = listOf(id1, id2, id3),
            intervalValue = 30,
            intervalUnit = ScheduleUnit.MINUTES,
            isShuffle = true
        )

        assertNotNull(updated)
        assertEquals("Expanded Mix", updated!!.name)
        assertEquals(30, updated.intervalValue)
        assertEquals(ScheduleUnit.MINUTES, updated.intervalUnit)
        assertTrue(updated.isShuffle)
        assertTrue(updated.isActive)
        assertEquals(listOf(id1, id2, id3), updated.items.map { it.id })
        assertEquals(id1, repository.activeWallpaperConfig.first().wallpaperId)
    }

    @Test
    fun `per-item zoom scale speed and trim settings apply when playlist switches items`() = runTest {
        val photoId = database.wallpaperDao().insertWallpaper(
            WallpaperEntity(
                title = "Custom Photo",
                filePath = "/tmp/p_custom.jpg",
                thumbnailPath = "",
                mediaType = MediaType.IMAGE.name,
                mimeType = "image/jpeg",
                fileSizeBytes = 100L,
                durationMs = 0L,
                width = 1080,
                height = 1920
            )
        )
        val videoId = database.wallpaperDao().insertWallpaper(
            WallpaperEntity(
                title = "Custom Video",
                filePath = "/tmp/v_custom.mp4",
                thumbnailPath = "",
                mediaType = MediaType.VIDEO.name,
                mimeType = "video/mp4",
                fileSizeBytes = 200L,
                durationMs = 20_000L,
                width = 1080,
                height = 1920
            )
        )

        repository.updateWallpaperCustomization(
            id = photoId,
            playbackSpeed = 1.0f,
            isLooping = true,
            isMuted = true,
            brightness = 0.9f,
            scaleMode = ScaleMode.FIT,
            zoom = 2.1f,
            offsetX = 0.35f,
            offsetY = -0.25f,
            trimStartMs = 0L,
            trimEndMs = 0L
        )
        repository.updateWallpaperCustomization(
            id = videoId,
            playbackSpeed = 2.0f,
            isLooping = true,
            isMuted = true,
            brightness = 1.0f,
            scaleMode = ScaleMode.CROP,
            zoom = 1.4f,
            offsetX = 0f,
            offsetY = 0.1f,
            trimStartMs = 3_000L,
            trimEndMs = 12_000L
        )

        val playlistId = repository.createPlaylist(
            name = "Customized Mix",
            wallpaperIds = listOf(photoId, videoId),
            intervalValue = 10,
            intervalUnit = ScheduleUnit.SECONDS,
            isShuffle = false
        )
        repository.activatePlaylist(playlistId, WallpaperTarget.BOTH)

        val cfg1 = repository.activeWallpaperConfig.first()
        assertEquals(photoId, cfg1.wallpaperId)
        assertEquals(ScaleMode.FIT, cfg1.scaleMode)
        assertEquals(2.1f, cfg1.zoom, 0.001f)
        assertEquals(0.35f, cfg1.offsetX, 0.001f)
        assertEquals(-0.25f, cfg1.offsetY, 0.001f)

        repository.skipPlaylistToNext()
        val cfg2 = repository.activeWallpaperConfig.first()
        assertEquals(videoId, cfg2.wallpaperId)
        assertEquals(2.0f, cfg2.playbackSpeed, 0.001f)
        assertEquals(1.4f, cfg2.zoom, 0.001f)
        assertEquals(3_000L, cfg2.trimStartMs)
        assertEquals(12_000L, cfg2.trimEndMs)
    }

    @Test
    fun `boot startup receivers quick tile job service and widget are properly registered`() {
        val pm = context.packageManager

        // Boot & package replacement actions
        listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        ).forEach { action ->
            val resolved = pm.queryBroadcastReceivers(Intent(action), 0)
            assertTrue(
                "Expected BootCompletedReceiver for $action",
                resolved.any { it.activityInfo.name == BootCompletedReceiver::class.java.name }
            )
        }

        // Cross-app & system wake-up triggers
        listOf(
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            StartupSyncReceiver.ACTION_SYNC_WALLPAPER,
            StartupSyncReceiver.ACTION_NEXT_WALLPAPER
        ).forEach { action ->
            val resolved = pm.queryBroadcastReceivers(Intent(action), 0)
            assertTrue(
                "Expected StartupSyncReceiver for $action",
                resolved.any { it.activityInfo.name == StartupSyncReceiver::class.java.name }
            )
        }

        // Home widget receiver
        val widgetResolved = pm.queryBroadcastReceivers(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE), 0)
        assertTrue(
            widgetResolved.any { it.activityInfo.name == RonyWallpaperWidgetProvider::class.java.name }
        )

        // Quick Settings TileService with BIND_QUICK_SETTINGS_TILE
        val tileIntent = Intent(TileService.ACTION_QS_TILE).apply {
            component = ComponentName(context, RonyQuickTileService::class.java)
        }
        val tileInfo = pm.resolveService(tileIntent, PackageManager.GET_META_DATA)
        assertNotNull(tileInfo)
        assertEquals(android.Manifest.permission.BIND_QUICK_SETTINGS_TILE, tileInfo?.serviceInfo?.permission)

        // Persisted JobService with BIND_JOB_SERVICE
        val jobInfo = pm.getServiceInfo(ComponentName(context, RonyPlaylistJobService::class.java), 0)
        assertEquals("android.permission.BIND_JOB_SERVICE", jobInfo.permission)
        assertTrue(jobInfo.exported)
    }

    @Test
    fun `first launch onboarding defaults to false and persists completion`() = runTest {
        assertFalse(repository.hasCompletedOnboarding.first())
        assertFalse(repository.settings.first().hasCompletedOnboarding)

        repository.setHasCompletedOnboarding(true)

        assertTrue(repository.hasCompletedOnboarding.first())
        assertTrue(repository.settings.first().hasCompletedOnboarding)
    }

    @Test
    fun `muted exoplayer disables audio track type and zeroes volume while unmute restores it`() {
        val player = ExoPlayerAudioHelper.configureBuilderAudio(
            ExoPlayer.Builder(context).setLooper(Looper.getMainLooper())
        ).build()
        try {
            ExoPlayerAudioHelper.applyMuteState(player, isMuted = true)
            assertEquals(0f, player.volume, 0.0001f)
            assertTrue(C.TRACK_TYPE_AUDIO in player.trackSelectionParameters.disabledTrackTypes)

            // Re-applying mute remains idempotent with audio track disabled
            ExoPlayerAudioHelper.applyMuteState(player, isMuted = true)
            assertEquals(0f, player.volume, 0.0001f)
            assertTrue(C.TRACK_TYPE_AUDIO in player.trackSelectionParameters.disabledTrackTypes)

            // Unmuting enables audio track and restores full volume
            ExoPlayerAudioHelper.applyMuteState(player, isMuted = false)
            assertEquals(1f, player.volume, 0.0001f)
            assertFalse(C.TRACK_TYPE_AUDIO in player.trackSelectionParameters.disabledTrackTypes)

            // Custom fractional volume (e.g., 35%) keeps audio track active at exact gain
            ExoPlayerAudioHelper.applyAudioState(player, isMuted = false, volume = 0.35f)
            assertEquals(0.35f, player.volume, 0.0001f)
            assertFalse(C.TRACK_TYPE_AUDIO in player.trackSelectionParameters.disabledTrackTypes)

            // Muting even with stored volume > 0 disables audio track and forces volume 0f
            ExoPlayerAudioHelper.applyAudioState(player, isMuted = true, volume = 0.75f)
            assertEquals(0f, player.volume, 0.0001f)
            assertTrue(C.TRACK_TYPE_AUDIO in player.trackSelectionParameters.disabledTrackTypes)

            // Dragging volume down to 0% disables audio track cleanly
            ExoPlayerAudioHelper.applyAudioState(player, isMuted = false, volume = 0.0f)
            assertEquals(0f, player.volume, 0.0001f)
            assertTrue(C.TRACK_TYPE_AUDIO in player.trackSelectionParameters.disabledTrackTypes)
        } finally {
            player.release()
        }
    }

    @Test
    fun `custom video volume persists in room and active datastore config`() = runTest {
        val videoId = database.wallpaperDao().insertWallpaper(
            WallpaperEntity(
                title = "Sound Video",
                filePath = "/tmp/sound_video.mp4",
                thumbnailPath = "",
                mediaType = MediaType.VIDEO.name,
                mimeType = "video/mp4",
                fileSizeBytes = 500L,
                durationMs = 15_000L,
                width = 1080,
                height = 1920
            )
        )
        repository.setActiveWallpaper(videoId, WallpaperTarget.BOTH)
        repository.updateWallpaperCustomization(
            id = videoId,
            playbackSpeed = 1.0f,
            isLooping = true,
            isMuted = false,
            brightness = 1.0f,
            scaleMode = ScaleMode.CROP,
            zoom = 1.0f,
            offsetX = 0f,
            offsetY = 0f,
            trimStartMs = 0L,
            trimEndMs = 15_000L,
            volume = 0.42f
        )

        val savedItem = repository.observeWallpaper(videoId).first()
        assertNotNull(savedItem)
        assertFalse(savedItem!!.isMuted)
        assertEquals(0.42f, savedItem.volume, 0.001f)

        val activeCfg = repository.activeWallpaperConfig.first()
        assertFalse(activeCfg.isMuted)
        assertEquals(0.42f, activeCfg.volume, 0.001f)
    }

    @Test
    fun `replaceWallpaperMedia replaces file metadata deletes old file and preserves favorite and active config`() = runTest {
        val originalPhoto = File(context.cacheDir, "original_photo.png")
        val bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        originalPhoto.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()

        val imported = repository.importMedia(Uri.fromFile(originalPhoto), MediaType.IMAGE).getOrThrow()
        val oldPath = imported.filePath
        assertTrue(File(oldPath).exists())

        repository.toggleFavorite(imported.id)
        repository.setActiveWallpaper(imported.id, WallpaperTarget.BOTH)

        val replacementGif = File(context.cacheDir, "replacement_anim.gif")
        replacementGif.writeBytes(buildMinimalGifBytes())

        val replaced = repository.replaceWallpaperMedia(imported.id, Uri.fromFile(replacementGif)).getOrThrow()
        assertEquals(imported.id, replaced.id)
        assertEquals(MediaType.GIF, replaced.mediaType)
        assertTrue(replaced.isFavorite)
        assertTrue(replaced.isActive)
        assertTrue(File(replaced.filePath).exists())
        assertFalse(File(oldPath).exists())

        val activeCfg = repository.activeWallpaperConfig.first()
        assertEquals(imported.id, activeCfg.wallpaperId)
        assertEquals(MediaType.GIF, activeCfg.mediaType)
        assertEquals(replaced.filePath, activeCfg.filePath)
    }

    @Test
    fun `discardSessionImports deletes unconfirmed session imports from room and disk`() = runTest {
        val tempGif = File(context.cacheDir, "session_temp.gif")
        tempGif.writeBytes(buildMinimalGifBytes())

        val imported = repository.importMedia(Uri.fromFile(tempGif), MediaType.GIF).getOrThrow()
        assertTrue(File(imported.filePath).exists())
        assertTrue(repository.allWallpapers.first().any { it.id == imported.id })

        repository.discardSessionImports(listOf(imported.id))

        assertFalse(File(imported.filePath).exists())
        assertFalse(repository.allWallpapers.first().any { it.id == imported.id })
    }

    @Test
    fun `removeItemFromPlaylist removes fourth item switches active if needed and cleans unused file`() = runTest {
        val items = (1..4).map { idx ->
            val tmp = File(context.cacheDir, "pl_item_$idx.gif").apply {
                writeBytes(buildMinimalGifBytes())
            }
            repository.importMedia(Uri.fromFile(tmp), MediaType.GIF).getOrThrow()
        }
        val playlistId = repository.createPlaylist(
            name = "Four Items",
            wallpaperIds = items.map { it.id },
            intervalValue = 10,
            intervalUnit = ScheduleUnit.MINUTES,
            isShuffle = false
        )
        repository.activatePlaylist(playlistId, WallpaperTarget.BOTH)

        // Advance active index to the 4th item, then remove it
        repository.skipPlaylistToNext()
        repository.skipPlaylistToNext()
        repository.skipPlaylistToNext()
        assertEquals(items[3].id, repository.activeWallpaperConfig.first().wallpaperId)

        val updated = repository.removeItemFromPlaylist(
            playlistId = playlistId,
            wallpaperId = items[3].id,
            alsoDeleteIfUnused = true
        )
        assertNotNull(updated)
        assertEquals(items.take(3).map { it.id }, updated!!.items.map { it.id })
        assertEquals(items[0].id, repository.activeWallpaperConfig.first().wallpaperId)
        assertFalse(File(items[3].filePath).exists())
        assertFalse(repository.allWallpapers.first().any { it.id == items[3].id })
    }

    @Test
    fun `replaceItemInPlaylist swaps single item in place and preserves shared item for other playlists`() = runTest {
        val f1 = File(context.cacheDir, "shared_1.gif").apply { writeBytes(buildMinimalGifBytes()) }
        val f2 = File(context.cacheDir, "shared_2.gif").apply { writeBytes(buildMinimalGifBytes()) }
        val item1 = repository.importMedia(Uri.fromFile(f1), MediaType.GIF).getOrThrow()
        val item2 = repository.importMedia(Uri.fromFile(f2), MediaType.GIF).getOrThrow()

        val p1 = repository.createPlaylist("Only Here", listOf(item1.id), 1, ScheduleUnit.DAYS, false)
        val replA = File(context.cacheDir, "repl_photo.png")
        val bmp = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888)
        replA.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()

        // Single-playlist replace swaps item1 in place
        val res1 = repository.replaceItemInPlaylist(p1, item1.id, Uri.fromFile(replA))
        assertTrue(res1.isSuccess)
        val p1After = res1.getOrNull()!!
        assertEquals(1, p1After.items.size)
        assertEquals(item1.id, p1After.items.first().id)
        assertEquals(MediaType.IMAGE, p1After.items.first().mediaType)

        // Shared item across two playlists keeps original item2 intact in p3 when replaced in p2
        val p2 = repository.createPlaylist("Mix A", listOf(item2.id), 1, ScheduleUnit.DAYS, false)
        val p3 = repository.createPlaylist("Mix B", listOf(item2.id), 2, ScheduleUnit.DAYS, false)

        val replB = File(context.cacheDir, "repl_b.gif").apply { writeBytes(buildMinimalGifBytes()) }
        val res2 = repository.replaceItemInPlaylist(p2, item2.id, Uri.fromFile(replB))
        assertTrue(res2.isSuccess)
        val p2After = res2.getOrNull()!!
        assertEquals(1, p2After.items.size)
        assertTrue(p2After.items.first().id != item2.id)

        val p3After = repository.allPlaylists.first().find { it.id == p3 }!!
        assertEquals(listOf(item2.id), p3After.items.map { it.id })
        assertTrue(File(item2.filePath).exists())
    }

    private fun buildMinimalGifBytes(): ByteArray {
        // Valid 2x2 GIF89a with 2-color GCT and Graphic Control Extension
        return byteArrayOf(
            // Header: GIF89a
            0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
            // Logical Screen Descriptor: 2x2, GCT flag + 2 colors
            0x02, 0x00, 0x02, 0x00, 0x80.toByte(), 0x00, 0x00,
            // Global Color Table (2 colors: black, violet)
            0x00, 0x00, 0x00, 0x8B.toByte(), 0x5C, 0xF6.toByte(),
            // Graphic Control Extension (100ms delay)
            0x21, 0xF9.toByte(), 0x04, 0x00, 0x0A, 0x00, 0x00, 0x00,
            // Image Descriptor: 0,0, 2x2, no LCT
            0x2C, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x02, 0x00, 0x00,
            // Image Data: LZW min code size = 2
            0x02, 0x02, 0x4C, 0x01, 0x00,
            // Trailer
            0x3B
        )
    }
}
