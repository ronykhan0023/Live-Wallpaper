package com.example.service

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.R
import com.example.RonyWallpaperApp
import com.example.domain.model.ActiveWallpaperConfig
import com.example.domain.model.MediaType
import com.example.engine.ExoPlayerAudioHelper
import com.example.engine.GifSurfaceRenderer
import com.example.engine.ImageSurfaceRenderer
import com.example.engine.WallpaperGlRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class RonyLiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = RonyWallpaperEngine()

    inner class RonyWallpaperEngine : Engine() {
        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var configJob: Job? = null
        private var playlistTickerJob: Job? = null

        private var currentConfig: ActiveWallpaperConfig = ActiveWallpaperConfig()
        private var activeLoadedPath: String? = null
        private var activeLoadedType: MediaType? = null
        private var activeTrimStartMs: Long = 0L
        private var activeTrimEndMs: Long = 0L

        private var exoPlayer: ExoPlayer? = null
        private var glRenderer: WallpaperGlRenderer? = null
        private var gifRenderer: GifSurfaceRenderer? = null
        private var imageRenderer: ImageSurfaceRenderer? = null
        private var isSurfaceReady = false

        private val powerManager: PowerManager?
            get() = getSystemService(Context.POWER_SERVICE) as? PowerManager

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            val repo = (application as RonyWallpaperApp).container.repository
            engineScope.launch {
                runCatching { repo.advancePlaylistIfDue() }
            }
            configJob = engineScope.launch {
                repo.activeWallpaperConfig.collectLatest { config ->
                    val systemPowerSave = powerManager?.isPowerSaveMode == true
                    val effectiveFps = if (systemPowerSave || config.batterySaverEnabled) {
                        minOf(config.targetFps, 24)
                    } else {
                        config.targetFps
                    }
                    val adjusted = config.copy(targetFps = effectiveFps)
                    currentConfig = adjusted
                    if (isSurfaceReady) {
                        applyConfig(adjusted)
                    }
                    restartPlaylistTicker(adjusted.activePlaylistId)
                }
            }
        }

        private fun restartPlaylistTicker(playlistId: Long?) {
            playlistTickerJob?.cancel()
            if (playlistId == null) return
            val repo = (application as RonyWallpaperApp).container.repository
            playlistTickerJob = engineScope.launch {
                while (isActive) {
                    val nextDueEpoch = runCatching { repo.advancePlaylistIfDue() }.getOrNull() ?: break
                    val now = System.currentTimeMillis()
                    val waitMs = (nextDueEpoch - now).coerceIn(1_000L, 60_000L)
                    delay(waitMs)
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            isSurfaceReady = true
            engineScope.launch {
                val repo = (application as RonyWallpaperApp).container.repository
                runCatching { repo.advancePlaylistIfDue() }
                applyConfig(currentConfig)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            glRenderer?.updateSurfaceSize(width, height)
            gifRenderer?.redrawCurrent()
            imageRenderer?.redraw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            glRenderer?.setVisible(visible)
            gifRenderer?.setVisible(visible)
            if (visible) {
                engineScope.launch {
                    val repo = (application as RonyWallpaperApp).container.repository
                    runCatching { repo.advancePlaylistIfDue() }
                }
                exoPlayer?.let { player ->
                    ExoPlayerAudioHelper.applyAudioState(player, currentConfig.isMuted, currentConfig.volume)
                    player.playbackParameters = PlaybackParameters(currentConfig.playbackSpeed)
                    if (!player.isPlaying) {
                        player.playWhenReady = true
                        player.play()
                    }
                }
                imageRenderer?.redraw()
            } else {
                exoPlayer?.pause()
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            isSurfaceReady = false
            releaseRenderers()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            playlistTickerJob?.cancel()
            configJob?.cancel()
            releaseRenderers()
            engineScope.cancel()
            super.onDestroy()
        }

        private fun applyConfig(config: ActiveWallpaperConfig) {
            val path = config.filePath
            if (path.isNullOrBlank() || !File(path).exists()) {
                releaseRenderers()
                drawFallbackArtwork()
                return
            }

            when (config.mediaType) {
                MediaType.VIDEO -> {
                    releaseGifRenderer()
                    releaseImageRenderer()
                    ensureVideoRenderer(config, path)
                }
                MediaType.GIF -> {
                    releaseVideoRenderer()
                    releaseImageRenderer()
                    ensureGifRenderer(config, path)
                }
                MediaType.IMAGE -> {
                    releaseVideoRenderer()
                    releaseGifRenderer()
                    ensureImageRenderer(config, path)
                }
            }
        }

        private fun buildMediaItem(path: String, trimStartMs: Long, trimEndMs: Long): MediaItem {
            val builder = MediaItem.Builder().setUri(File(path).toURI().toString())
            if (trimStartMs > 0L || trimEndMs > 0L) {
                val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(trimStartMs.coerceAtLeast(0L))
                if (trimEndMs > trimStartMs) {
                    clipBuilder.setEndPositionMs(trimEndMs)
                }
                builder.setClippingConfiguration(clipBuilder.build())
            }
            return builder.build()
        }

        @OptIn(UnstableApi::class)
        private fun ensureVideoRenderer(config: ActiveWallpaperConfig, path: String) {
            val holder = surfaceHolder ?: return

            if (glRenderer == null) {
                val renderer = WallpaperGlRenderer(holder) { surface ->
                    engineScope.launch {
                        val latestConfig = currentConfig
                        val latestPath = latestConfig.filePath ?: path
                        attachExoPlayer(surface, latestConfig, latestPath)
                    }
                }
                glRenderer = renderer
                renderer.init()
                val rect = holder.surfaceFrame
                if (rect.width() > 0 && rect.height() > 0) {
                    renderer.updateSurfaceSize(rect.width(), rect.height())
                }
            } else if (
                activeLoadedPath != path ||
                activeLoadedType != MediaType.VIDEO ||
                activeTrimStartMs != config.trimStartMs ||
                activeTrimEndMs != config.trimEndMs
            ) {
                exoPlayer?.let { player ->
                    ExoPlayerAudioHelper.applyAudioState(player, config.isMuted, config.volume)
                    player.setMediaItem(buildMediaItem(path, config.trimStartMs, config.trimEndMs))
                    player.prepare()
                    player.playWhenReady = isVisible
                }
                activeLoadedPath = path
                activeLoadedType = MediaType.VIDEO
                activeTrimStartMs = config.trimStartMs
                activeTrimEndMs = config.trimEndMs
            }

            glRenderer?.updateParameters(
                scaleMode = config.scaleMode,
                zoom = config.zoom,
                offsetX = config.offsetX,
                offsetY = config.offsetY,
                brightness = config.brightness,
                targetFps = config.targetFps
            )
            glRenderer?.setVisible(isVisible)

            exoPlayer?.let { player ->
                player.playbackParameters = PlaybackParameters(config.playbackSpeed)
                player.repeatMode = if (config.isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                ExoPlayerAudioHelper.applyAudioState(player, config.isMuted, config.volume)
            }
        }

        @OptIn(UnstableApi::class)
        private fun attachExoPlayer(surface: Surface, config: ActiveWallpaperConfig, path: String) {
            if (!isSurfaceReady) return
            exoPlayer?.release()

            val renderersFactory = DefaultRenderersFactory(applicationContext)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(
                    if (config.preferHardwareDecoder) {
                        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                    } else {
                        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                    }
                )

            val player = ExoPlayerAudioHelper.configureBuilderAudio(
                ExoPlayer.Builder(applicationContext, renderersFactory)
            )
                .build()
                .apply {
                    setVideoSurface(surface)
                    ExoPlayerAudioHelper.applyAudioState(this, config.isMuted, config.volume)
                    setMediaItem(buildMediaItem(path, config.trimStartMs, config.trimEndMs))
                    playbackParameters = PlaybackParameters(config.playbackSpeed)
                    repeatMode = if (config.isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    addListener(object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: VideoSize) {
                            if (videoSize.width > 0 && videoSize.height > 0) {
                                glRenderer?.updateVideoSize(videoSize.width, videoSize.height)
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            ExoPlayerAudioHelper.applyAudioState(this@apply, currentConfig.isMuted, currentConfig.volume)
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            ExoPlayerAudioHelper.applyAudioState(this@apply, currentConfig.isMuted, currentConfig.volume)
                        }

                        override fun onVolumeChanged(volume: Float) {
                            val expected = if (currentConfig.isMuted || currentConfig.volume <= 0.005f) {
                                0f
                            } else {
                                currentConfig.volume.coerceIn(0f, 1f)
                            }
                            if (kotlin.math.abs(volume - expected) > 0.015f) {
                                ExoPlayerAudioHelper.applyAudioState(this@apply, currentConfig.isMuted, currentConfig.volume)
                            }
                        }
                    })
                    prepare()
                    playWhenReady = isVisible
                }
            exoPlayer = player
            activeLoadedPath = path
            activeLoadedType = MediaType.VIDEO
            activeTrimStartMs = config.trimStartMs
            activeTrimEndMs = config.trimEndMs
        }

        private fun ensureGifRenderer(config: ActiveWallpaperConfig, path: String) {
            val holder = surfaceHolder ?: return
            val renderer = gifRenderer ?: GifSurfaceRenderer(holder).also { gifRenderer = it }
            renderer.setVisible(isVisible)
            renderer.loadAndStart(
                filePath = path,
                speed = config.playbackSpeed,
                isLooping = config.isLooping,
                scaleMode = config.scaleMode,
                zoom = config.zoom,
                offsetX = config.offsetX,
                offsetY = config.offsetY,
                brightness = config.brightness,
                targetFps = config.targetFps
            )
            activeLoadedPath = path
            activeLoadedType = MediaType.GIF
        }

        private fun ensureImageRenderer(config: ActiveWallpaperConfig, path: String) {
            val holder = surfaceHolder ?: return
            val renderer = imageRenderer ?: ImageSurfaceRenderer(holder).also { imageRenderer = it }
            renderer.loadAndDraw(
                filePath = path,
                scaleMode = config.scaleMode,
                zoom = config.zoom,
                offsetX = config.offsetX,
                offsetY = config.offsetY,
                brightness = config.brightness
            )
            activeLoadedPath = path
            activeLoadedType = MediaType.IMAGE
        }

        private fun drawFallbackArtwork() {
            val holder = surfaceHolder ?: return
            val surface = holder.surface
            if (surface == null || !surface.isValid) return
            val canvas = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
            } catch (_: Throwable) {
                null
            } ?: return

            try {
                canvas.drawColor(Color.parseColor("#05050A"))
                val bmp = BitmapFactory.decodeResource(resources, R.drawable.img_hero_aurora)
                if (bmp != null) {
                    val scale = maxOf(
                        canvas.width.toFloat() / bmp.width,
                        canvas.height.toFloat() / bmp.height
                    )
                    val dstW = (bmp.width * scale).toInt()
                    val dstH = (bmp.height * scale).toInt()
                    val left = (canvas.width - dstW) / 2
                    val top = (canvas.height - dstH) / 2
                    canvas.drawBitmap(bmp, null, Rect(left, top, left + dstW, top + dstH), null)
                    bmp.recycle()
                }
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }

        private fun releaseVideoRenderer() {
            exoPlayer?.let { player ->
                runCatching {
                    player.clearVideoSurface()
                    player.release()
                }
            }
            exoPlayer = null
            glRenderer?.release()
            glRenderer = null
            if (activeLoadedType == MediaType.VIDEO) {
                activeLoadedPath = null
                activeLoadedType = null
            }
        }

        private fun releaseGifRenderer() {
            gifRenderer?.release()
            gifRenderer = null
            if (activeLoadedType == MediaType.GIF) {
                activeLoadedPath = null
                activeLoadedType = null
            }
        }

        private fun releaseImageRenderer() {
            imageRenderer?.release()
            imageRenderer = null
            if (activeLoadedType == MediaType.IMAGE) {
                activeLoadedPath = null
                activeLoadedType = null
            }
        }

        private fun releaseRenderers() {
            releaseVideoRenderer()
            releaseGifRenderer()
            releaseImageRenderer()
        }
    }
}
