package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
 * Renders still photo wallpapers onto WallpaperService's SurfaceHolder with ScaleMode,
 * zoom, pan offset, brightness, and memory-safe downsampling.
 */
class ImageSurfaceRenderer(
    private val surfaceHolder: SurfaceHolder
) {
    private val thread = HandlerThread("RonyImageWallpaperThread").apply { start() }
    private val handler = Handler(thread.looper)

    private var bitmap: Bitmap? = null
    private var currentPath: String? = null
    @Volatile private var isReleased = false

    @Volatile private var scaleMode = ScaleMode.CROP
    @Volatile private var zoom = 1.0f
    @Volatile private var offsetX = 0f
    @Volatile private var offsetY = 0f
    @Volatile private var brightness = 1.0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val drawMatrix = Matrix()

    fun loadAndDraw(
        filePath: String,
        scaleMode: ScaleMode,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        brightness: Float
    ) {
        updateParameters(scaleMode, zoom, offsetX, offsetY, brightness)
        handler.post {
            if (isReleased) return@post
            if (currentPath != filePath || bitmap == null) {
                bitmap?.recycle()
                bitmap = decodeSampledBitmap(File(filePath), maxDim = 2160)
                currentPath = filePath
            }
            bitmap?.let { drawBitmap(it) }
        }
    }

    fun updateParameters(
        scaleMode: ScaleMode,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        brightness: Float
    ) {
        this.scaleMode = scaleMode
        this.zoom = zoom.coerceIn(1.0f, 3.0f)
        this.offsetX = offsetX.coerceIn(-1f, 1f)
        this.offsetY = offsetY.coerceIn(-1f, 1f)
        this.brightness = brightness.coerceIn(0.3f, 1.2f)

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

        handler.post {
            if (!isReleased) {
                bitmap?.let { drawBitmap(it) }
            }
        }
    }

    fun redraw() {
        handler.post {
            if (!isReleased) {
                bitmap?.let { drawBitmap(it) }
            }
        }
    }

    private fun drawBitmap(bmp: Bitmap) {
        val surface = surfaceHolder.surface
        if (surface == null || !surface.isValid) return

        val canvas = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                surfaceHolder.lockHardwareCanvas()
            } else {
                surfaceHolder.lockCanvas()
            }
        } catch (_: Throwable) {
            null
        } ?: return

        try {
            val cw = canvas.width.toFloat()
            val ch = canvas.height.toFloat()
            val bw = bmp.width.toFloat().coerceAtLeast(1f)
            val bh = bmp.height.toFloat().coerceAtLeast(1f)

            canvas.drawColor(Color.BLACK)
            drawMatrix.reset()

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

            drawMatrix.postScale(sx, sy)
            drawMatrix.postTranslate(tx, ty)
            canvas.drawBitmap(bmp, drawMatrix, paint)
        } finally {
            runCatching { surfaceHolder.unlockCanvasAndPost(canvas) }
        }
    }

    private fun decodeSampledBitmap(file: File, maxDim: Int): Bitmap? {
        if (!file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        }.getOrNull()
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        handler.removeCallbacksAndMessages(null)
        handler.post {
            bitmap?.recycle()
            bitmap = null
            thread.quitSafely()
        }
    }
}
