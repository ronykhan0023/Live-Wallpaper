package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Memory-efficient streaming GIF87a/89a decoder that keeps only the current and previous
 * pixel buffers in RAM and supports optional downsampling for large GIF files.
 */
class GifFrameDecoder(
    private val data: ByteArray,
    maxDimension: Int = 1080
) {
    data class GifFrame(
        var ix: Int = 0,
        var iy: Int = 0,
        var iw: Int = 0,
        var ih: Int = 0,
        var interlace: Boolean = false,
        var transparency: Boolean = false,
        var dispose: Int = 0,
        var transIndex: Int = 0,
        var delayMs: Int = 100,
        var bufferFrameStart: Int = 0,
        var lct: IntArray? = null
    )

    var width: Int = 0
        private set
    var height: Int = 0
        private set
    var frameCount: Int = 0
        private set
    var loopCount: Int = 0
        private set
    var isValid: Boolean = false
        private set

    private var gctFlag: Boolean = false
    private var gctSize: Int = 0
    private var bgIndex: Int = 0
    private var bgColor: Int = Color.BLACK
    private var gct: IntArray? = null

    private val frames = mutableListOf<GifFrame>()
    private var currentFrameIndex = -1
    private var sampleSize = 1
    private var downsampledWidth = 0
    private var downsampledHeight = 0

    private val rawData: ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    private val block = ByteArray(256)
    private var prefix: ShortArray? = null
    private var suffix: ByteArray? = null
    private var pixelStack: ByteArray? = null
    private var mainPixels: ByteArray = ByteArray(0)
    private var mainScratch: IntArray = IntArray(0)
    private var previousImage: Bitmap? = null
    private var currentBitmap: Bitmap? = null
    private var savePrevious = false

    init {
        parseHeaderAndFrames(maxDimension)
    }

    private fun parseHeaderAndFrames(maxDimension: Int) {
        if (data.size < 14) return
        rawData.rewind()
        val id = buildString {
            repeat(6) { append(readByte().toChar()) }
        }
        if (!id.startsWith("GIF")) return

        width = readShort()
        height = readShort()
        if (width <= 0 || height <= 0) return

        val packed = readByte()
        gctFlag = (packed and 0x80) != 0
        gctSize = 2 shl (packed and 0x07)
        bgIndex = readByte()
        readByte() // pixel aspect

        if (gctFlag) {
            gct = readColorTable(gctSize)
            bgColor = gct?.getOrNull(bgIndex) ?: Color.BLACK
        }

        var done = false
        var currentFrame: GifFrame? = null
        while (!done && rawData.hasRemaining()) {
            when (readByte()) {
                0x2C -> { // Image Descriptor
                    val frame = currentFrame ?: GifFrame()
                    frame.ix = readShort()
                    frame.iy = readShort()
                    frame.iw = readShort()
                    frame.ih = readShort()
                    val packedField = readByte()
                    val lctFlag = (packedField and 0x80) != 0
                    val lctSize = 2 shl (packedField and 0x07)
                    frame.interlace = (packedField and 0x40) != 0
                    frame.lct = if (lctFlag) readColorTable(lctSize) else null
                    frame.bufferFrameStart = rawData.position()
                    skipImageData()
                    frames.add(frame)
                    currentFrame = null
                }
                0x21 -> { // Extension
                    when (readByte()) {
                        0xF9 -> { // Graphic Control Extension
                            val frame = currentFrame ?: GifFrame()
                            readByte() // block size (4)
                            val packedGce = readByte()
                            frame.dispose = (packedGce and 0x1C) shr 2
                            if (frame.dispose == 0) frame.dispose = 1
                            frame.transparency = (packedGce and 0x01) != 0
                            val delayCs = readShort()
                            frame.delayMs = if (delayCs <= 1) 100 else delayCs * 10
                            frame.transIndex = readByte()
                            readByte() // terminator
                            currentFrame = frame
                        }
                        0xFF -> { // Application Extension
                            val blockSize = readByte()
                            val app = ByteArray(blockSize)
                            if (rawData.remaining() >= blockSize) {
                                rawData.get(app)
                                val appStr = String(app, Charsets.US_ASCII)
                                if (appStr == "NETSCAPE2.0") {
                                    readNetscapeExt()
                                } else {
                                    skipSubBlocks()
                                }
                            } else {
                                done = true
                            }
                        }
                        else -> skipSubBlocks()
                    }
                }
                0x3B -> done = true // Trailer
                else -> done = true
            }
        }

        frameCount = frames.size
        if (frameCount > 0) {
            val maxSide = max(width, height)
            var s = 1
            while (maxSide / s > maxDimension) {
                s *= 2
            }
            sampleSize = max(1, s)
            downsampledWidth = max(1, width / sampleSize)
            downsampledHeight = max(1, height / sampleSize)
            mainPixels = ByteArray(width * height)
            mainScratch = IntArray(downsampledWidth * downsampledHeight)
            savePrevious = frames.any { it.dispose == 3 }
            isValid = true
        }
    }

    fun advance(): Int {
        if (frameCount <= 0) return 100
        currentFrameIndex = (currentFrameIndex + 1) % frameCount
        return frames[currentFrameIndex].delayMs.coerceAtLeast(16)
    }

    fun getCurrentFrameIndex(): Int = currentFrameIndex

    fun reset() {
        currentFrameIndex = -1
        mainScratch.fill(Color.TRANSPARENT)
        previousImage?.eraseColor(Color.TRANSPARENT)
    }

    fun getNextFrameBitmap(): Bitmap? {
        if (!isValid || frameCount == 0) return null
        if (currentFrameIndex < 0) currentFrameIndex = 0

        val frame = frames[currentFrameIndex]
        val previousIndex = if (currentFrameIndex == 0) frameCount - 1 else currentFrameIndex - 1
        val prevFrame = if (currentFrameIndex > 0) frames[previousIndex] else null

        val act = frame.lct ?: gct ?: return null
        val savedTransColor = if (frame.transparency && frame.transIndex in act.indices) {
            act[frame.transIndex]
        } else 0
        if (frame.transparency && frame.transIndex in act.indices) {
            act[frame.transIndex] = 0
        }

        val bitmap = setPixels(frame, prevFrame, act)

        if (frame.transparency && frame.transIndex in act.indices) {
            act[frame.transIndex] = savedTransColor
        }
        return bitmap
    }

    private fun setPixels(
        currentFrame: GifFrame,
        previousFrame: GifFrame?,
        activeColorTable: IntArray
    ): Bitmap {
        val dest = mainScratch

        if (previousFrame == null || currentFrameIndex == 0) {
            dest.fill(0)
        } else if (previousFrame.dispose > 0) {
            when (previousFrame.dispose) {
                3 -> { // Restore previous
                    previousImage?.getPixels(
                        dest, 0, downsampledWidth, 0, 0, downsampledWidth, downsampledHeight
                    ) ?: dest.fill(0)
                }
                2 -> { // Restore background
                    val c = if (!currentFrame.transparency) bgColor else 0
                    val top = previousFrame.iy / sampleSize
                    val left = previousFrame.ix / sampleSize
                    val bottom = min(downsampledHeight, (previousFrame.iy + previousFrame.ih) / sampleSize)
                    val right = min(downsampledWidth, (previousFrame.ix + previousFrame.iw) / sampleSize)
                    for (y in top until bottom) {
                        val rowOffset = y * downsampledWidth
                        for (x in left until right) {
                            dest[rowOffset + x] = c
                        }
                    }
                }
            }
        }

        decodeBitmapData(currentFrame)

        val downsampledTop = currentFrame.iy / sampleSize
        val downsampledLeft = currentFrame.ix / sampleSize
        val downsampledW = currentFrame.iw / sampleSize
        val downsampledH = currentFrame.ih / sampleSize

        var pass = 1
        var inc = 8
        var iline = 0
        val isFirstFrame = currentFrameIndex == 0

        for (i in 0 until downsampledH) {
            var line = i
            if (currentFrame.interlace) {
                if (iline >= downsampledH) {
                    pass++
                    when (pass) {
                        2 -> iline = 4
                        3 -> { iline = 2; inc = 4 }
                        4 -> { iline = 1; inc = 2 }
                    }
                }
                line = iline
                iline += inc
            }
            line += downsampledTop
            if (line < downsampledHeight) {
                val k = line * downsampledWidth
                var dx = k + downsampledLeft
                var dlim = dx + downsampledW
                if (k + downsampledWidth < dlim) {
                    dlim = k + downsampledWidth
                }
                var sx = i * sampleSize * currentFrame.iw
                val maxSx = sx + (dlim - dx) * sampleSize
                while (dx < dlim && sx < mainPixels.size && sx < maxSx) {
                    val colorIndex = mainPixels[sx].toInt() and 0xFF
                    val color = if (colorIndex in activeColorTable.indices) activeColorTable[colorIndex] else 0
                    if (color != 0) {
                        dest[dx] = color
                    } else if (isFirstFrame) {
                        dest[dx] = bgColor
                    }
                    dx++
                    sx += sampleSize
                }
            }
        }

        if (savePrevious && (currentFrame.dispose == 0 || currentFrame.dispose == 1)) {
            if (previousImage == null) {
                previousImage = Bitmap.createBitmap(downsampledWidth, downsampledHeight, Bitmap.Config.ARGB_8888)
            }
            previousImage?.setPixels(dest, 0, downsampledWidth, 0, 0, downsampledWidth, downsampledHeight)
        }

        val outBitmap = currentBitmap ?: Bitmap.createBitmap(
            downsampledWidth,
            downsampledHeight,
            Bitmap.Config.ARGB_8888
        ).also { currentBitmap = it }

        outBitmap.setPixels(dest, 0, downsampledWidth, 0, 0, downsampledWidth, downsampledHeight)
        return outBitmap
    }

    private fun decodeBitmapData(frame: GifFrame) {
        rawData.position(frame.bufferFrameStart)
        val npix = frame.iw * frame.ih
        if (mainPixels.size < npix) {
            mainPixels = ByteArray(npix)
        }
        val maxStackSize = 4096
        if (prefix == null) prefix = ShortArray(maxStackSize)
        if (suffix == null) suffix = ByteArray(maxStackSize)
        if (pixelStack == null) pixelStack = ByteArray(maxStackSize + 1)

        val pref = prefix!!
        val suff = suffix!!
        val stack = pixelStack!!

        val dataSize = readByte()
        val clear = 1 shl dataSize
        val endOfInfo = clear + 1
        var available = clear + 2
        var oldCode = -1
        var codeSize = dataSize + 1
        var codeMask = (1 shl codeSize) - 1

        for (code in 0 until clear) {
            pref[code] = 0
            suff[code] = code.toByte()
        }

        var datum = 0
        var bits = 0
        var count = 0
        var first = 0
        var top = 0
        var bi = 0
        var pi = 0
        var i = 0

        while (i < npix) {
            if (bits < codeSize) {
                if (count == 0) {
                    count = readBlock()
                    if (count <= 0) break
                    bi = 0
                }
                datum += (block[bi].toInt() and 0xFF) shl bits
                bits += 8
                bi++
                count--
                continue
            }

            var code = datum and codeMask
            datum = datum shr codeSize
            bits -= codeSize

            if (code == clear) {
                codeSize = dataSize + 1
                codeMask = (1 shl codeSize) - 1
                available = clear + 2
                oldCode = -1
                continue
            }
            if (code > available || code == endOfInfo) break
            if (oldCode == -1) {
                stack[top++] = suff[code]
                oldCode = code
                first = code
                continue
            }

            val inCode = code
            if (code >= available) {
                stack[top++] = first.toByte()
                code = oldCode
            }
            while (code >= clear && top < maxStackSize) {
                stack[top++] = suff[code]
                code = pref[code].toInt() and 0xFFFF
            }
            first = suff[code].toInt() and 0xFF
            stack[top++] = first.toByte()

            if (available < maxStackSize) {
                pref[available] = oldCode.toShort()
                suff[available] = first.toByte()
                available++
                if ((available and codeMask) == 0 && available < maxStackSize) {
                    codeSize++
                    codeMask += available
                }
            }
            oldCode = inCode

            while (top > 0 && pi < npix) {
                top--
                mainPixels[pi++] = stack[top]
                i++
            }
        }
        while (pi < npix) {
            mainPixels[pi++] = 0
        }
    }

    private fun readColorTable(numColors: Int): IntArray {
        val nbytes = 3 * numColors
        val raw = ByteArray(nbytes)
        if (rawData.remaining() >= nbytes) {
            rawData.get(raw)
        }
        val table = IntArray(256)
        var i = 0
        var j = 0
        while (i < numColors && j + 2 < raw.size) {
            val r = raw[j++].toInt() and 0xFF
            val g = raw[j++].toInt() and 0xFF
            val b = raw[j++].toInt() and 0xFF
            table[i++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return table
    }

    private fun readNetscapeExt() {
        do {
            val size = readBlock()
            if (size >= 3 && block[0].toInt() == 1) {
                val b1 = block[1].toInt() and 0xFF
                val b2 = block[2].toInt() and 0xFF
                loopCount = (b2 shl 8) or b1
            }
        } while (size > 0 && rawData.hasRemaining())
    }

    private fun skipImageData() {
        readByte() // LZW minimum code size
        skipSubBlocks()
    }

    private fun skipSubBlocks() {
        var size: Int
        do {
            size = readByte()
            if (size > 0 && rawData.remaining() >= size) {
                rawData.position(rawData.position() + size)
            } else if (size > 0) {
                rawData.position(rawData.limit())
            }
        } while (size > 0 && rawData.hasRemaining())
    }

    private fun readBlock(): Int {
        val size = readByte()
        if (size > 0 && rawData.remaining() > 0) {
            val toRead = min(size, rawData.remaining())
            rawData.get(block, 0, toRead)
            return toRead
        }
        return 0
    }

    private fun readByte(): Int {
        return if (rawData.hasRemaining()) rawData.get().toInt() and 0xFF else 0
    }

    private fun readShort(): Int {
        return if (rawData.remaining() >= 2) rawData.short.toInt() and 0xFFFF else 0
    }

    fun release() {
        currentBitmap?.recycle()
        currentBitmap = null
        previousImage?.recycle()
        previousImage = null
    }

    companion object {
        fun fromFile(file: File, maxDimension: Int = 1080): GifFrameDecoder? {
            if (!file.exists() || !file.canRead()) return null
            return runCatching {
                FileInputStream(file).use { fis ->
                    val bos = ByteArrayOutputStream(file.length().toInt().coerceIn(4096, 32 * 1024 * 1024))
                    val buf = ByteArray(32 * 1024)
                    var read: Int
                    while (fis.read(buf).also { read = it } != -1) {
                        bos.write(buf, 0, read)
                    }
                    GifFrameDecoder(bos.toByteArray(), maxDimension).takeIf { it.isValid }
                }
            }.getOrNull()
        }
    }
}
