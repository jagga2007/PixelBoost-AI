package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GallerySaver {
    private var saveCounter = 1

    suspend fun saveImageToGallery(
        context: Context,
        sourceFilePath: String,
        quality: String = "16K",
        format: String = "JPG"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(IllegalStateException("Source image file does not exist."))
            }

            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
            val dateStr = dateFormat.format(Date())
            val counterStr = String.format(Locale.US, "%03d", saveCounter++)
            val extension = if (format.equals("PNG", true)) "png" else if (format.equals("WEBP", true)) "webp" else "jpg"
            val mimeType = if (format.equals("PNG", true)) "image/png" else if (format.equals("WEBP", true)) "image/webp" else "image/jpeg"
            val filename = "PixelBoost_${quality}_${dateStr}_${counterStr}.${extension}"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "PixelBoost AI")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry."))

            resolver.openOutputStream(itemUri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Failed to open output stream."))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            Result.success(itemUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
