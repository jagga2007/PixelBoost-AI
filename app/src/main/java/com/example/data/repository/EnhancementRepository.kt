package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import com.example.data.network.PixelBoostApiService
import com.example.engine.DimensionCalculator
import com.example.engine.EnhancementResult
import com.example.engine.GallerySaver
import com.example.engine.OnDeviceEnhancer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.roundToInt

class ServerUnavailableException(message: String) : Exception(message)

class EnhancementRepository(
    private val context: Context,
    private val historyDao: HistoryDao,
    private val settingsRepository: SettingsRepository
) {
    private val onDeviceEnhancer = OnDeviceEnhancer(context)

    suspend fun enhance(
        sourceUri: Uri,
        quality: String,
        mode: String,
        onProgress: (progress: Float, stage: String) -> Unit,
        forceLocal: Boolean = false
    ): EnhancementResult = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.value
        val shouldTryServer = !forceLocal && when (settings.processingMode) {
            ProcessingMode.SERVER -> true
            ProcessingMode.ON_DEVICE -> false
            ProcessingMode.AUTO -> {
                // For 8K, 12K, 16K, prefer server processing to avoid mobile RAM strain
                quality.equals("8K", true) || quality.equals("12K", true) || quality.equals("16K", true)
            }
        }

        val result: EnhancementResult = if (shouldTryServer) {
            try {
                processViaServer(sourceUri, quality, mode, onProgress, settings)
            } catch (e: Exception) {
                if (settings.processingMode == ProcessingMode.SERVER) {
                    throw ServerUnavailableException("AI server unavailable: ${e.localizedMessage ?: "Connection failed"}")
                } else {
                    // In AUTO mode, gracefully fallback to on-device engine
                    onProgress(0.1f, "Server unreachable, switching to on-device engine...")
                    onDeviceEnhancer.enhance(
                        sourceUri = sourceUri,
                        quality = quality,
                        mode = mode,
                        enableFaceEnhance = settings.faceEnhancement,
                        onProgress = onProgress
                    )
                }
            }
        } else {
            onDeviceEnhancer.enhance(
                sourceUri = sourceUri,
                quality = quality,
                mode = mode,
                enableFaceEnhance = settings.faceEnhancement,
                onProgress = onProgress
            )
        }

        // Save entry into Room Database
        val thumbnailFile = createThumbnailFile(result.previewBitmap)
        val originalCopy = copyUriToInternalFile(sourceUri, "orig_${System.currentTimeMillis()}.jpg")

        val entity = HistoryEntity(
            title = "PixelBoost_${result.quality}_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            inputResolution = "${result.originalWidth} × ${result.originalHeight}",
            outputResolution = "${result.outputWidth} × ${result.outputHeight}",
            qualityTier = result.quality,
            mode = result.mode,
            originalImagePath = originalCopy.absolutePath,
            enhancedImagePath = result.outputFilePath,
            thumbnailPath = thumbnailFile.absolutePath,
            processingTimeSeconds = result.processingTimeSeconds,
            isAiEnhanced = result.isAiEnhanced,
            isServerProcessed = result.isServerProcessed,
            fileSizeFormatted = formatFileSize(result.fileSizeBytes)
        )
        historyDao.insert(entity)

        // Automatically save to Gallery if enabled in Settings
        if (settings.saveToGallery) {
            try {
                GallerySaver.saveImageToGallery(
                    context = context,
                    sourceFilePath = result.outputFilePath,
                    quality = result.quality,
                    format = settings.defaultOutputFormat
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        result
    }

    private suspend fun processViaServer(
        sourceUri: Uri,
        quality: String,
        mode: String,
        onProgress: (progress: Float, stage: String) -> Unit,
        settings: AppSettings
    ): EnhancementResult {
        onProgress(0.15f, "Connecting to AI Server...")
        val apiService = PixelBoostApiService.create(settings.serverUrl)

        // Verify server health
        try {
            val health = apiService.checkHealth()
            if (!health.isSuccessful) {
                throw ServerUnavailableException("AI Server returned HTTP ${health.code()}")
            }
        } catch (e: Exception) {
            throw ServerUnavailableException("Could not connect to ${settings.serverUrl}: ${e.message}")
        }

        onProgress(0.30f, "Uploading image to AI engine...")
        val tempUpload = copyUriToInternalFile(sourceUri, "upload_${System.currentTimeMillis()}.jpg")
        val fileReq = tempUpload.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val multipartBody = MultipartBody.Part.createFormData("file", tempUpload.name, fileReq)

        val qualityReq = quality.toRequestBody("text/plain".toMediaTypeOrNull())
        val modeReq = mode.lowercase().toRequestBody("text/plain".toMediaTypeOrNull())
        val formatReq = settings.defaultOutputFormat.toRequestBody("text/plain".toMediaTypeOrNull())
        val jpegQualityReq = settings.jpegQuality.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val faceReq = settings.faceEnhancement.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        onProgress(0.60f, "AI is reconstructing details (Tiled Super-Resolution)...")
        val startTime = System.currentTimeMillis()
        val response = apiService.enhanceImage(
            file = multipartBody,
            quality = qualityReq,
            mode = modeReq,
            outputFormat = formatReq,
            jpegQuality = jpegQualityReq,
            enableFaceEnhance = faceReq
        )

        if (!response.isSuccessful || response.body() == null) {
            throw ServerUnavailableException("Enhancement failed with status ${response.code()}")
        }

        onProgress(0.85f, "Downloading enhanced image...")
        val outputDir = File(context.cacheDir, "pixelboost_server").apply { mkdirs() }
        val ext = if (settings.defaultOutputFormat.equals("PNG", true)) ".png" else ".jpg"
        val outputFile = File(outputDir, "PixelBoost_${quality}_${System.currentTimeMillis()}$ext")

        response.body()!!.byteStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }

        // Read metadata from response headers
        val outWidth = response.headers()["X-Output-Width"]?.toIntOrNull()
            ?: DimensionCalculator.getMaxDimensionForQuality(quality)
        val outHeight = response.headers()["X-Output-Height"]?.toIntOrNull()
            ?: (outWidth * 9 / 16)
        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0

        // Create downsampled preview bitmap for display
        val previewBitmap = decodeSubsampledBitmap(outputFile.absolutePath, 2560)
            ?: throw IllegalStateException("Failed to decode enhanced server response.")

        onProgress(1.0f, "Completed")

        return EnhancementResult(
            outputFilePath = outputFile.absolutePath,
            previewBitmap = previewBitmap,
            originalWidth = outWidth / 4,
            originalHeight = outHeight / 4,
            outputWidth = outWidth,
            outputHeight = outHeight,
            processingTimeSeconds = elapsed,
            mode = mode,
            quality = quality,
            isAiEnhanced = true,
            isServerProcessed = true,
            fileSizeBytes = outputFile.length()
        )
    }

    private fun decodeSubsampledBitmap(path: String, maxDimension: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOptions)

        var sampleSize = 1
        val maxBound = max(boundsOptions.outWidth, boundsOptions.outHeight)
        while (maxBound / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(path, decodeOptions)
    }

    private fun copyUriToInternalFile(uri: Uri, filename: String): File {
        val file = File(context.cacheDir, filename)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun createThumbnailFile(bitmap: Bitmap): File {
        val file = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
        val scale = 300f / max(bitmap.width, bitmap.height).toFloat()
        val tw = max((bitmap.width * scale).roundToInt(), 100)
        val th = max((bitmap.height * scale).roundToInt(), 100)
        val thumb = Bitmap.createScaledBitmap(bitmap, tw, th, true)
        FileOutputStream(file).use { out ->
            thumb.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        return file
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
