package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.SurfaceHolder
import com.example.domain.model.ScaleMode
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Renders animated GIFs onto WallpaperService's SurfaceHolder with exact 0.25x-3.0x speed,
 * ScaleMode (CROP, FIT, FILL, CENTER), zoom, pan offset, brightness, and FPS capping.
 */
class GifSurfaceRenderer(
    private val surfaceHolder: SurfaceHolder
) {
    private val thread = HandlerThread("RonyGifWallpaperThread").apply { start() }
    private val handler = Handler(thread.looper)

    private var decoder: GifFrameDecoder? = null
    private var currentPath: String? = null

    @Volatile private var isVisible = true
    @Volatile private var isReleased = false
    @Volatile private var speed = 1.0f
    @Volatile private var isLooping = true
    @Volatile private var scaleMode = ScaleMode.CROP
    @Volatile private var zoom = 1.0f
    @Volatile private var offsetX = 0f
    @Volatile private var offsetY = 0f
    @Volatile private var brightness = 1.0f
    @Volatile private var targetFps = 60

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val drawMatrix = Matrix()

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (isReleased || !isVisible) return
            val dec = decoder ?: return
            val delayRaw = dec.advance()
            val bmp = dec.getNextFrameBitmap()
            if (bmp != null) {
                drawBitmapToSurface(bmp)
            }
            if (!isLooping && dec.getCurrentFrameIndex() >= dec.frameCount - 1) {
                return
            }
            val effectiveDelay = (delayRaw / speed.coerceIn(0.25f, 3.0f)).toLong()
            val minFrameMs = (1000L / targetFps.coerceIn(15, 60))
            val scheduledDelay = max(effectiveDelay, minFrameMs)
            if (!isReleased && isVisible) {
                handler.postDelayed(this, scheduledDelay)
            }
        }
    }

    fun loadAndStart(
        filePath: String,
        speed: Float,
        isLooping: Boolean,
        scaleMode: ScaleMode,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        brightness: Float,
        targetFps: Int
    ) {
        updateParameters(speed, isLooping, scaleMode, zoom, offsetX, offsetY, brightness, targetFps)
        handler.post {
            if (isReleased) return@post
            if (currentPath != filePath || decoder == null) {
                decoder?.release()
                decoder = GifFrameDecoder.fromFile(File(filePath), maxDimension = 1080)
                currentPath = filePath
            }
            handler.removeCallbacks(frameRunnable)
            if (isVisible && decoder != null) {
                handler.post(frameRunnable)
            }
        }
    }

    fun updateParameters(
        speed: Float,
        isLooping: Boolean,
        scaleMode: ScaleMode,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        brightness: Float,
        targetFps: Int
    ) {
        this.speed = speed.coerceIn(0.25f, 3.0f)
        this.isLooping = isLooping
        this.scaleMode = scaleMode
        this.zoom = zoom.coerceIn(1.0f, 3.0f)
        this.offsetX = offsetX.coerceIn(-1f, 1f)
        this.offsetY = offsetY.coerceIn(-1f, 1f)
        this.brightness = brightness.coerceIn(0.3f, 1.2f)
        this.targetFps = targetFps.coerceIn(15, 60)

        paint.colorFilter = if (kotlin.math.abs(this.brightness - 1.0f) > 0.01f) {
            val b = this.brightness
            ColorMatrixColorFilter(
                ColorMatrix(
                    floatArrayOf(
                        b, 0f, 0f, 0f, 0f,
                        0f, b, 0f, 0f, 0f,
                        0f, 0f, b, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        } else {
            null
        }
    }

    fun setVisible(visible: Boolean) {
        isVisible = visible
        handler.removeCallbacks(frameRunnable)
        if (visible && !isReleased && decoder != null) {
            handler.post(frameRunnable)
        }
    }

    fun redrawCurrent() {
        handler.post {
            val bmp = decoder?.getNextFrameBitmap()
            if (bmp != null) drawBitmapToSurface(bmp)
        }
    }

    private fun drawBitmapToSurface(bitmap: Bitmap) {
        val holder = surfaceHolder
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
            val cw = canvas.width.toFloat()
            val ch = canvas.height.toFloat()
            val bw = bitmap.width.toFloat().coerceAtLeast(1f)
            val bh = bitmap.height.toFloat().coerceAtLeast(1f)

            canvas.drawColor(Color.BLACK)
            drawMatrix.reset()

            var sx = 1f
            var sy = 1f
            when (scaleMode) {
                ScaleMode.CROP -> {
                    val s = max(cw / bw, ch / bh)
                    sx = s
                    sy = s
                }
                ScaleMode.FIT -> {
                    val s = min(cw / bw, ch / bh)
                    sx = s
                    sy = s
                }
                ScaleMode.FILL -> {
                    sx = cw / bw
                    sy = ch / bh
                }
                ScaleMode.CENTER -> {
                    val s = min(1f, min(cw / bw, ch / bh))
                    sx = s
                    sy = s
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

            drawMatrix.postScale(sx, sy)
            drawMatrix.postTranslate(tx, ty)
            canvas.drawBitmap(bitmap, drawMatrix, paint)
        } finally {
            runCatching { holder.unlockCanvasAndPost(canvas) }
        }
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        handler.removeCallbacksAndMessages(null)
        handler.post {
            decoder?.release()
            decoder = null
            thread.quitSafely()
        }
    }
}
