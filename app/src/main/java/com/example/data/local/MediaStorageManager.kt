package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.domain.model.MediaType
import com.example.domain.model.StorageSummary
import com.example.domain.model.WallpaperItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

data class ImportedMediaMetadata(
    val title: String,
    val filePath: String,
    val thumbnailPath: String,
    val mediaType: MediaType,
    val mimeType: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int
)

class MediaStorageManager(private val context: Context) {

    private val wallpapersDir: File
        get() = File(context.filesDir, "wallpapers").apply { if (!exists()) mkdirs() }

    private val thumbnailsDir: File
        get() = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }

    suspend fun importMediaFromUri(
        uri: Uri,
        forcedType: MediaType? = null
    ): Result<ImportedMediaMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val contentResolver = context.contentResolver
            val detectedMime = contentResolver.getType(uri).orEmpty()
            val displayName = queryDisplayName(uri) ?: "wallpaper_${System.currentTimeMillis()}"
            val cleanTitle = displayName.substringBeforeLast('.').ifBlank { "Live Wallpaper" }

            val rawExt = displayName.substringAfterLast('.', "").lowercase()
                .ifBlank { MimeTypeMap.getSingleton().getExtensionFromMimeType(detectedMime).orEmpty() }

            val uuid = UUID.randomUUID().toString()
            val tempFile = File(wallpapersDir, "$uuid.tmp")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            } ?: throw IllegalStateException("Unable to read selected file stream")

            if (tempFile.length() == 0L) {
                tempFile.delete()
                throw IllegalStateException("Selected file is empty")
            }

            val isGifHeader = checkIsGifHeader(tempFile)
            val isImageMime = detectedMime.startsWith("image/", ignoreCase = true) &&
                !detectedMime.equals("image/gif", ignoreCase = true)
            val isImageExt = rawExt in setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp")

            val resolvedType = when {
                isGifHeader || detectedMime.equals("image/gif", ignoreCase = true) || rawExt == "gif" -> MediaType.GIF
                forcedType == MediaType.IMAGE || (forcedType != MediaType.VIDEO && (isImageMime || isImageExt)) -> MediaType.IMAGE
                else -> MediaType.VIDEO
            }

            val finalExt = when {
                resolvedType == MediaType.GIF -> "gif"
                resolvedType == MediaType.IMAGE -> rawExt.ifBlank { "jpg" }
                rawExt.isNotBlank() -> rawExt
                else -> "mp4"
            }

            val finalFile = File(wallpapersDir, "$uuid.$finalExt")
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            val resolvedMime = when {
                resolvedType == MediaType.GIF -> "image/gif"
                detectedMime.isNotBlank() -> detectedMime
                else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(finalExt)
                    ?: if (resolvedType == MediaType.IMAGE) "image/jpeg" else "video/mp4"
            }

            val thumbFile = File(thumbnailsDir, "$uuid.jpg")
            var width = 1080
            var height = 1920
            var durationMs = 0L

            when (resolvedType) {
                MediaType.VIDEO -> {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(finalFile.absolutePath)
                        durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull() ?: 0L
                        val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            ?.toIntOrNull() ?: 1080
                        val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ?.toIntOrNull() ?: 1920
                        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                            ?.toIntOrNull() ?: 0
                        if (rotation == 90 || rotation == 270) {
                            width = rawH
                            height = rawW
                        } else {
                            width = rawW
                            height = rawH
                        }

                        val frameTimeUs = if (durationMs > 1500L) 1_000_000L else 0L
                        val frame = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.getFrameAtTime(0L)
                        if (frame != null) {
                            saveScaledThumbnail(frame, thumbFile)
                            frame.recycle()
                        }
                    } finally {
                        runCatching { retriever.release() }
                    }
                }
                MediaType.GIF -> {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(finalFile.absolutePath, bounds)
                    if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                        width = bounds.outWidth
                        height = bounds.outHeight
                    }
                    durationMs = scanGifDurationMs(finalFile)
                    val sampleSize = calculateInSampleSize(width, height, 640, 640)
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val bmp = BitmapFactory.decodeFile(finalFile.absolutePath, decodeOpts)
                    if (bmp != null) {
                        saveScaledThumbnail(bmp, thumbFile)
                        bmp.recycle()
                    }
                }
                MediaType.IMAGE -> {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(finalFile.absolutePath, bounds)
                    if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                        width = bounds.outWidth
                        height = bounds.outHeight
                    }
                    val sampleSize = calculateInSampleSize(width, height, 640, 640)
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    val bmp = BitmapFactory.decodeFile(finalFile.absolutePath, decodeOpts)
                    if (bmp != null) {
                        saveScaledThumbnail(bmp, thumbFile)
                        bmp.recycle()
                    }
                }
            }

            ImportedMediaMetadata(
                title = cleanTitle,
                filePath = finalFile.absolutePath,
                thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else "",
                mediaType = resolvedType,
                mimeType = resolvedMime,
                fileSizeBytes = finalFile.length(),
                durationMs = durationMs,
                width = width,
                height = height
            )
        }
    }

    suspend fun calculateStorageSummary(items: List<WallpaperItem>): StorageSummary =
        withContext(Dispatchers.IO) {
            var videoBytes = 0L
            var gifBytes = 0L
            var imageBytes = 0L
            items.forEach { item ->
                val f = File(item.filePath)
                val size = if (f.exists()) f.length() else item.fileSizeBytes
                when (item.mediaType) {
                    MediaType.VIDEO -> videoBytes += size
                    MediaType.GIF -> gifBytes += size
                    MediaType.IMAGE -> imageBytes += size
                }
            }
            val thumbBytes = thumbnailsDir.listFiles()?.sumOf { it.length() } ?: 0L
            StorageSummary(
                videoBytes = videoBytes,
                gifBytes = gifBytes,
                imageBytes = imageBytes,
                thumbnailBytes = thumbBytes,
                totalBytes = videoBytes + gifBytes + imageBytes + thumbBytes,
                wallpaperCount = items.size
            )
        }

    suspend fun clearOrphanFiles(validItems: List<WallpaperItem>): Int = withContext(Dispatchers.IO) {
        val validMediaPaths = validItems.map { it.filePath }.toSet()
        val validThumbPaths = validItems.map { it.thumbnailPath }.toSet()
        var deletedCount = 0

        wallpapersDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in validMediaPaths && file.delete()) {
                deletedCount++
            }
        }
        thumbnailsDir.listFiles()?.forEach { file ->
            if (file.absolutePath !in validThumbPaths && file.delete()) {
                deletedCount++
            }
        }
        deletedCount
    }

    suspend fun deleteMediaFiles(filePath: String, thumbnailPath: String) = withContext(Dispatchers.IO) {
        if (filePath.isNotBlank()) {
            runCatching { File(filePath).delete() }
        }
        if (thumbnailPath.isNotBlank()) {
            runCatching { File(thumbnailPath).delete() }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.lastPathSegment
        }
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else {
                        null
                    }
                }
        }.getOrNull()
    }

    private fun checkIsGifHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 6) return false
        return runCatching {
            FileInputStream(file).use { fis ->
                val header = ByteArray(6)
                val count = fis.read(header)
                if (count < 6) return@use false
                val sig = String(header, Charsets.US_ASCII)
                sig == "GIF87a" || sig == "GIF89a"
            }
        }.getOrDefault(false)
    }

    private fun saveScaledThumbnail(source: Bitmap, targetFile: File) {
        val maxDim = 640
        val ratio = minOf(
            maxDim.toFloat() / source.width.coerceAtLeast(1),
            maxDim.toFloat() / source.height.coerceAtLeast(1),
            1.0f
        )
        val targetW = (source.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (source.height * ratio).toInt().coerceAtLeast(1)
        val scaled = if (ratio < 1.0f) {
            Bitmap.createScaledBitmap(source, targetW, targetH, true)
        } else {
            source
        }
        FileOutputStream(targetFile).use { fos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            fos.flush()
        }
        if (scaled !== source) {
            scaled.recycle()
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun scanGifDurationMs(file: File): Long {
        return runCatching {
            BufferedInputStream(FileInputStream(file)).use { input ->
                val header = ByteArray(6)
                if (input.read(header) != 6) return@use 0L
                @Suppress("DEPRECATION")
                val movie = android.graphics.Movie.decodeStream(FileInputStream(file))
                movie?.duration()?.toLong() ?: 0L
            }
        }.getOrDefault(0L)
    }
}
