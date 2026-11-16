package com.example.engine

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import com.example.domain.model.ScaleMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * OpenGL ES 2.0 renderer for ExoPlayer video live wallpapers.
 * Supports ScaleMode (CROP, FIT, FILL, CENTER), zoom, pan offsets, brightness, and FPS pacing.
 */
class WallpaperGlRenderer(
    private val surfaceHolder: SurfaceHolder,
    private val onVideoSurfaceReady: (Surface) -> Unit
) {
    private val glThread = HandlerThread("RonyGlWallpaperThread").apply { start() }
    private val glHandler = Handler(glThread.looper)

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var programId = 0
    private var textureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var videoSurface: Surface? = null

    private var aPositionLoc = -1
    private var aTexCoordLoc = -1
    private var uMvpMatrixLoc = -1
    private var uStMatrixLoc = -1
    private var uBrightnessLoc = -1

    private val stMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }
    private val mvpMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) }

    @Volatile private var surfaceWidth = 1080
    @Volatile private var surfaceHeight = 1920
    @Volatile private var videoWidth = 1080
    @Volatile private var videoHeight = 1920
    @Volatile private var scaleMode = ScaleMode.CROP
    @Volatile private var zoom = 1.0f
    @Volatile private var offsetX = 0f
    @Volatile private var offsetY = 0f
    @Volatile private var brightness = 1.0f
    @Volatile private var targetFps = 60
    @Volatile private var isVisible = true
    @Volatile private var isReleased = false

    private var lastRenderNs = 0L

    private val vertexCoords = floatArrayOf(
        -1f, -1f, 0f, 0f,
         1f, -1f, 1f, 0f,
        -1f,  1f, 0f, 1f,
         1f,  1f, 1f, 1f
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertexCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(vertexCoords)
        .apply { position(0) }

    fun init() {
        glHandler.post {
            if (isReleased) return@post
            setupEgl()
            setupGl()
            val st = SurfaceTexture(textureId)
            surfaceTexture = st
            st.setOnFrameAvailableListener({
                glHandler.post { drawFrame() }
            }, glHandler)
            val s = Surface(st)
            videoSurface = s
            onVideoSurfaceReady(s)
        }
    }

    fun updateSurfaceSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        surfaceWidth = width
        surfaceHeight = height
        glHandler.post {
            if (isReleased || eglDisplay == EGL14.EGL_NO_DISPLAY) return@post
            GLES20.glViewport(0, 0, width, height)
            recalculateMvpMatrix()
            drawFrame(force = true)
        }
    }

    fun updateVideoSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        videoWidth = width
        videoHeight = height
        glHandler.post {
            if (isReleased) return@post
            recalculateMvpMatrix()
            drawFrame(force = true)
        }
    }

    fun updateParameters(
        scaleMode: ScaleMode,
        zoom: Float,
        offsetX: Float,
        offsetY: Float,
        brightness: Float,
        targetFps: Int
    ) {
        this.scaleMode = scaleMode
        this.zoom = zoom.coerceIn(1.0f, 3.0f)
        this.offsetX = offsetX.coerceIn(-1f, 1f)
        this.offsetY = offsetY.coerceIn(-1f, 1f)
        this.brightness = brightness.coerceIn(0.3f, 1.2f)
        this.targetFps = targetFps.coerceIn(15, 60)
        glHandler.post {
            if (isReleased) return@post
            recalculateMvpMatrix()
            drawFrame(force = true)
        }
    }

    fun setVisible(visible: Boolean) {
        isVisible = visible
        if (visible) {
            glHandler.post { drawFrame(force = true) }
        }
    }

    private fun drawFrame(force: Boolean = false) {
        if (isReleased || eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE) return
        val st = surfaceTexture ?: return

        try {
            st.updateTexImage()
            st.getTransformMatrix(stMatrix)
        } catch (_: Throwable) {
            return
        }

        if (!isVisible && !force) return

        val now = System.nanoTime()
        val minIntervalNs = 1_000_000_000L / targetFps.coerceAtLeast(15)
        if (!force && now - lastRenderNs < minIntervalNs) {
            return
        }
        lastRenderNs = now

        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(programId)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aPositionLoc)

        vertexBuffer.position(2)
        GLES20.glVertexAttribPointer(aTexCoordLoc, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glUniformMatrix4fv(uMvpMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uStMatrixLoc, 1, false, stMatrix, 0)
        GLES20.glUniform1f(uBrightnessLoc, brightness)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun recalculateMvpMatrix() {
        Matrix.setIdentityM(mvpMatrix, 0)
        val viewAspect = surfaceWidth.toFloat() / surfaceHeight.coerceAtLeast(1).toFloat()
        val videoAspect = videoWidth.toFloat() / videoHeight.coerceAtLeast(1).toFloat()

        var scaleX = 1f
        var scaleY = 1f

        when (scaleMode) {
            ScaleMode.CROP -> {
                if (videoAspect > viewAspect) {
                    scaleX = videoAspect / viewAspect
                } else {
                    scaleY = viewAspect / videoAspect
                }
            }
            ScaleMode.FIT -> {
                if (videoAspect > viewAspect) {
                    scaleY = viewAspect / videoAspect
                } else {
                    scaleX = videoAspect / viewAspect
                }
            }
            ScaleMode.FILL -> {
                scaleX = 1f
                scaleY = 1f
            }
            ScaleMode.CENTER -> {
                val wRatio = videoWidth.toFloat() / surfaceWidth.coerceAtLeast(1).toFloat()
                val hRatio = videoHeight.toFloat() / surfaceHeight.coerceAtLeast(1).toFloat()
                val factor = min(1f, min(wRatio, hRatio))
                if (videoAspect > viewAspect) {
                    scaleX = factor
                    scaleY = factor * (viewAspect / videoAspect)
                } else {
                    scaleY = factor
                    scaleX = factor * (videoAspect / viewAspect)
                }
            }
        }

        scaleX *= zoom
        scaleY *= zoom

        val maxTransX = max(0f, scaleX - 1f)
        val maxTransY = max(0f, scaleY - 1f)
        val transX = offsetX * maxTransX
        val transY = -offsetY * maxTransY

        Matrix.translateM(mvpMatrix, 0, transX, transY, 0f)
        Matrix.scaleM(mvpMatrix, 0, scaleX, scaleY, 1f)
    }

    private fun setupEgl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0)

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surfaceHolder.surface, surfaceAttribs, 0)
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    private fun setupGl() {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        programId = GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
        }
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "aTexCoord")
        uMvpMatrixLoc = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
        uStMatrixLoc = GLES20.glGetUniformLocation(programId, "uStMatrix")
        uBrightnessLoc = GLES20.glGetUniformLocation(programId, "uBrightness")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        glHandler.post {
            runCatching { surfaceTexture?.setOnFrameAvailableListener(null) }
            runCatching { videoSurface?.release() }
            runCatching { surfaceTexture?.release() }
            videoSurface = null
            surfaceTexture = null

            if (textureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                textureId = 0
            }
            if (programId != 0) {
                GLES20.glDeleteProgram(programId)
                programId = 0
            }
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                }
                EGL14.eglTerminate(eglDisplay)
            }
            eglSurface = EGL14.EGL_NO_SURFACE
            eglContext = EGL14.EGL_NO_CONTEXT
            eglDisplay = EGL14.EGL_NO_DISPLAY
            glThread.quitSafely()
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvpMatrix;
            uniform mat4 uStMatrix;
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMvpMatrix * aPosition;
                vTexCoord = (uStMatrix * aTexCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES sTexture;
            uniform float uBrightness;
            void main() {
                vec4 color = texture2D(sTexture, vTexCoord);
                gl_FragColor = vec4(clamp(color.rgb * uBrightness, 0.0, 1.0), 1.0);
            }
        """
    }
}
