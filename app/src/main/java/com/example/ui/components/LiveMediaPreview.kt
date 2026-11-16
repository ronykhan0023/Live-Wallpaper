package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.MediaType
import com.example.domain.model.ScaleMode
import com.example.engine.GifFrameDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

@Composable
fun LiveMediaPreview(
    filePath: String,
    mediaType: MediaType,
    playbackSpeed: Float,
    isLooping: Boolean,
    isMuted: Boolean,
    brightness: Float,
    scaleMode: ScaleMode,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    trimStartMs: Long = 0L,
    trimEndMs: Long = 0L,
    allowDragPosition: Boolean = false,
    onOffsetChange: ((Float, Float) -> Unit)? = null
) {
    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)
    val dragModifier = if (allowDragPosition && onOffsetChange != null) {
        Modifier.pointerInput(filePath) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val w = size.width.coerceAtLeast(1).toFloat()
                val h = size.height.coerceAtLeast(1).toFloat()
                val newX = (currentOffsetX + (dragAmount.x / w) * 2f).coerceIn(-1f, 1f)
                val newY = (currentOffsetY + (dragAmount.y / h) * 2f).coerceIn(-1f, 1f)
                onOffsetChange(newX, newY)
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .then(dragModifier)
    ) {
        when (mediaType) {
            MediaType.VIDEO -> VideoTexturePreview(
                filePath = filePath,
                playbackSpeed = playbackSpeed,
                isLooping = isLooping,
                isMuted = isMuted,
                brightness = brightness,
                scaleMode = scaleMode,
                zoom = zoom,
                offsetX = offsetX,
                offsetY = offsetY,
                isPlaying = isPlaying,
                trimStartMs = trimStartMs,
                trimEndMs = trimEndMs,
                modifier = Modifier.fillMaxSize()
            )
            MediaType.GIF -> GifCanvasPreview(
                filePath = filePath,
                playbackSpeed = playbackSpeed,
                isLooping = isLooping,
                brightness = brightness,
                scaleMode = scaleMode,
                zoom = zoom,
                offsetX = offsetX,
                offsetY = offsetY,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize()
            )
            MediaType.IMAGE -> ImageCanvasPreview(
                filePath = filePath,
                brightness = brightness,
                scaleMode = scaleMode,
                zoom = zoom,
                offsetX = offsetX,
                offsetY = offsetY,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoTexturePreview(
    filePath: String,
    playbackSpeed: Float,
    isLooping: Boolean,
    isMuted: Boolean,
    brightness: Float,
    scaleMode: ScaleMode,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    isPlaying: Boolean,
    trimStartMs: Long,
    trimEndMs: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var videoW by remember { mutableIntStateOf(1080) }
    var videoH by remember { mutableIntStateOf(1920) }
    var viewW by remember { mutableIntStateOf(0) }
    var viewH by remember { mutableIntStateOf(0) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var surfaceRef by remember { mutableStateOf<Surface?>(null) }

    val player = remember(filePath) {
        val renderersFactory = DefaultRenderersFactory(context.applicationContext)
            .setEnableDecoderFallback(true)
        ExoPlayer.Builder(context.applicationContext, renderersFactory).build()
    }

    LaunchedEffect(player, filePath, trimStartMs, trimEndMs) {
        val builder = MediaItem.Builder().setUri(File(filePath).toURI().toString())
        if (trimStartMs > 0L || trimEndMs > 0L) {
            val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(trimStartMs.coerceAtLeast(0L))
            if (trimEndMs > trimStartMs) {
                clipBuilder.setEndPositionMs(trimEndMs)
            }
            builder.setClippingConfiguration(clipBuilder.build())
        }
        player.setMediaItem(builder.build())
        player.prepare()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoW = videoSize.width
                    videoH = videoSize.height
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.clearVideoSurface()
            player.release()
            surfaceRef?.release()
            surfaceRef = null
        }
    }

    DisposableEffect(lifecycleOwner, player, isPlaying) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(player, playbackSpeed, isLooping, isMuted, isPlaying) {
        player.playbackParameters = PlaybackParameters(playbackSpeed.coerceIn(0.25f, 3.0f))
        player.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.volume = if (isMuted) 0f else 1f
        player.playWhenReady = isPlaying
        if (isPlaying) {
            player.play()
        } else {
            player.pause()
        }
    }

    LaunchedEffect(textureViewRef, viewW, viewH, videoW, videoH, scaleMode, zoom, offsetX, offsetY, brightness) {
        val tv = textureViewRef ?: return@LaunchedEffect
        if (viewW <= 0 || viewH <= 0 || videoW <= 0 || videoH <= 0) return@LaunchedEffect
        tv.setTransform(
            computeTextureTransformMatrix(
                viewWidth = viewW.toFloat(),
                viewHeight = viewH.toFloat(),
                mediaWidth = videoW.toFloat(),
                mediaHeight = videoH.toFloat(),
                scaleMode = scaleMode,
                zoom = zoom,
                offsetX = offsetX,
                offsetY = offsetY
            )
        )
        tv.alpha = brightness.coerceIn(0.3f, 1.0f)
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                TextureView(ctx).apply {
                    isOpaque = false
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                            viewW = width
                            viewH = height
                            surfaceRef?.release()
                            val s = Surface(st)
                            surfaceRef = s
                            player.setVideoSurface(s)
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                            viewW = width
                            viewH = height
                        }

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            player.clearVideoSurface()
                            surfaceRef?.release()
                            surfaceRef = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    }
                    textureViewRef = this
                }
            }
        )
        if (brightness > 1.0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = ((brightness - 1.0f) * 0.45f).coerceIn(0f, 0.15f)))
            )
        }
    }
}

@Composable
private fun GifCanvasPreview(
    filePath: String,
    playbackSpeed: Float,
    isLooping: Boolean,
    brightness: Float,
    scaleMode: ScaleMode,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var decoder by remember(filePath) { mutableStateOf<GifFrameDecoder?>(null) }
    var currentBitmap by remember(filePath) { mutableStateOf<Bitmap?>(null) }
    var frameTick by remember { mutableIntStateOf(0) }

    val speedState by rememberUpdatedState(playbackSpeed)
    val loopingState by rememberUpdatedState(isLooping)
    val playingState by rememberUpdatedState(isPlaying)

    DisposableEffect(filePath) {
        onDispose {
            decoder?.release()
            decoder = null
        }
    }

    LaunchedEffect(filePath) {
        val loaded = withContext(Dispatchers.IO) {
            GifFrameDecoder.fromFile(File(filePath), maxDimension = 960)
        }
        decoder = loaded
        if (loaded != null) {
            loaded.advance()
            currentBitmap = loaded.getNextFrameBitmap()
            frameTick++
            while (isActive) {
                if (!playingState) {
                    delay(80L)
                    continue
                }
                val delayRaw = loaded.advance()
                val bmp = loaded.getNextFrameBitmap()
                if (bmp != null) {
                    currentBitmap = bmp
                    frameTick++
                }
                if (!loopingStateStateSafe(loopingState, loaded)) {
                    delay(100L)
                    continue
                }
                val effectiveDelay = (delayRaw / speedState.coerceIn(0.25f, 3.0f)).toLong().coerceAtLeast(16L)
                delay(effectiveDelay)
            }
        }
    }

    val colorFilter = remember(brightness) {
        val b = brightness.coerceIn(0.3f, 1.2f)
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    b, 0f, 0f, 0f, 0f,
                    0f, b, 0f, 0f, 0f,
                    0f, 0f, b, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    BoxWithConstraints(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_EXPRESSION")
            frameTick
            val bmp = currentBitmap ?: return@Canvas
            drawTransformedBitmap(
                bmp = bmp,
                scaleMode = scaleMode,
                zoom = zoom,
                offsetX = offsetX,
                offsetY = offsetY,
                colorFilter = colorFilter
            )
        }
    }
}

@Composable
private fun ImageCanvasPreview(
    filePath: String,
    brightness: Float,
    scaleMode: ScaleMode,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(filePath) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(filePath) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(filePath, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 1440 || bounds.outHeight / sample > 1440) {
                    sample *= 2
                }
                BitmapFactory.decodeFile(filePath, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()
        }
    }

    DisposableEffect(filePath) {
        onDispose {
            bitmap?.recycle()
            bitmap = null
        }
    }

    val colorFilter = remember(brightness) {
        val b = brightness.coerceIn(0.3f, 1.2f)
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    b, 0f, 0f, 0f, 0f,
                    0f, b, 0f, 0f, 0f,
                    0f, 0f, b, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }

    Canvas(modifier = modifier) {
        val bmp = bitmap ?: return@Canvas
        drawTransformedBitmap(
            bmp = bmp,
            scaleMode = scaleMode,
            zoom = zoom,
            offsetX = offsetX,
            offsetY = offsetY,
            colorFilter = colorFilter
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTransformedBitmap(
    bmp: Bitmap,
    scaleMode: ScaleMode,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    colorFilter: ColorFilter
) {
    val cw = size.width
    val ch = size.height
    val bw = bmp.width.toFloat().coerceAtLeast(1f)
    val bh = bmp.height.toFloat().coerceAtLeast(1f)

    var sx = 1f
    var sy = 1f
    when (scaleMode) {
        ScaleMode.CROP -> {
            val s = max(cw / bw, ch / bh)
            sx = s; sy = s
        }
        ScaleMode.FIT -> {
            val s = min(cw / bw, ch / bh)
            sx = s; sy = s
        }
        ScaleMode.FILL -> {
            sx = cw / bw
            sy = ch / bh
        }
        ScaleMode.CENTER -> {
            val s = min(1f, min(cw / bw, ch / bh))
            sx = s; sy = s
        }
    }
    sx *= zoom
    sy *= zoom

    val scaledW = bw * sx
    val scaledH = bh * sy
    val extraX = max(0f, (scaledW - cw) / 2f)
    val extraY = max(0f, (scaledH - ch) / 2f)
    val tx = (cw - scaledW) / 2f + offsetX * extraX
    val ty = (ch - scaledH) / 2f + offsetY * extraY

    withTransform({
        translate(left = tx, top = ty)
        scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero)
    }) {
        drawImage(
            image = bmp.asImageBitmap(),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(bmp.width, bmp.height),
            colorFilter = colorFilter
        )
    }
}

private fun loopingStateStateSafe(isLooping: Boolean, decoder: GifFrameDecoder): Boolean {
    return isLooping || decoder.getCurrentFrameIndex() < decoder.frameCount - 1
}

private fun computeTextureTransformMatrix(
    viewWidth: Float,
    viewHeight: Float,
    mediaWidth: Float,
    mediaHeight: Float,
    scaleMode: ScaleMode,
    zoom: Float,
    offsetX: Float,
    offsetY: Float
): Matrix {
    val matrix = Matrix()
    val viewAspect = viewWidth / viewHeight.coerceAtLeast(1f)
    val mediaAspect = mediaWidth / mediaHeight.coerceAtLeast(1f)

    var scaleX = 1f
    var scaleY = 1f

    when (scaleMode) {
        ScaleMode.CROP -> {
            if (mediaAspect > viewAspect) {
                scaleX = mediaAspect / viewAspect
            } else {
                scaleY = viewAspect / mediaAspect
            }
        }
        ScaleMode.FIT -> {
            if (mediaAspect > viewAspect) {
                scaleY = viewAspect / mediaAspect
            } else {
                scaleX = mediaAspect / viewAspect
            }
        }
        ScaleMode.FILL -> {
            scaleX = 1f
            scaleY = 1f
        }
        ScaleMode.CENTER -> {
            val wRatio = mediaWidth / viewWidth
            val hRatio = mediaHeight / viewHeight
            val factor = min(1f, min(wRatio, hRatio))
            if (mediaAspect > viewAspect) {
                scaleX = factor
                scaleY = factor * (viewAspect / mediaAspect)
            } else {
                scaleY = factor
                scaleX = factor * (mediaAspect / viewAspect)
            }
        }
    }

    scaleX *= zoom
    scaleY *= zoom

    val scaledW = viewWidth * scaleX
    val scaledH = viewHeight * scaleY
    val extraX = max(0f, (scaledW - viewWidth) / 2f)
    val extraY = max(0f, (scaledH - viewHeight) / 2f)

    val pivotX = viewWidth / 2f
    val pivotY = viewHeight / 2f
    matrix.setScale(scaleX, scaleY, pivotX, pivotY)
    matrix.postTranslate(offsetX * extraX, offsetY * extraY)
    return matrix
}
