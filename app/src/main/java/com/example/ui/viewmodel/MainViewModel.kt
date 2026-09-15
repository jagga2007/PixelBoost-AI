package com.example.ui.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HistoryEntity
import com.example.data.local.PixelBoostDatabase
import com.example.data.network.PixelBoostApiService
import com.example.data.repository.AppSettings
import com.example.data.repository.EnhancementRepository
import com.example.data.repository.ProcessingMode
import com.example.data.repository.ServerUnavailableException
import com.example.data.repository.SettingsRepository
import com.example.engine.DimensionCalculator
import com.example.engine.EnhancementResult
import com.example.engine.GallerySaver
import com.example.engine.ImageDimensions
import com.example.engine.ShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PixelBoostDatabase.getDatabase(application)
    val settingsRepository = SettingsRepository(application)
    val enhancementRepository = EnhancementRepository(application, db.historyDao(), settingsRepository)

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    val history: StateFlow<List<HistoryEntity>> = db.historyDao().getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<HistoryEntity>> = db.historyDao().getRecentHistory(4)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Workflow State
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _selectedImageDimensions = MutableStateFlow<ImageDimensions?>(null)
    val selectedImageDimensions: StateFlow<ImageDimensions?> = _selectedImageDimensions.asStateFlow()

    private val _selectedQuality = MutableStateFlow("4K")
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    private val _selectedMode = MutableStateFlow("Balanced")
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    private val _calculatedOutputDimensions = MutableStateFlow(ImageDimensions(3840, 2160))
    val calculatedOutputDimensions: StateFlow<ImageDimensions> = _calculatedOutputDimensions.asStateFlow()

    // Processing State
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()

    private val _processingStage = MutableStateFlow("Analyzing image")
    val processingStage: StateFlow<String> = _processingStage.asStateFlow()

    // Results & Errors
    private val _enhancementResult = MutableStateFlow<EnhancementResult?>(null)
    val enhancementResult: StateFlow<EnhancementResult?> = _enhancementResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _serverFallbackPrompt = MutableStateFlow<String?>(null)
    val serverFallbackPrompt: StateFlow<String?> = _serverFallbackPrompt.asStateFlow()

    fun selectImage(uri: Uri) {
        _selectedImageUri.value = uri
        _enhancementResult.value = null
        _errorMessage.value = null
        _serverFallbackPrompt.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                val w = max(options.outWidth, 1)
                val h = max(options.outHeight, 1)
                val dims = ImageDimensions(w, h)
                _selectedImageDimensions.value = dims
                updateCalculatedDimensions(w, h, _selectedQuality.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setQuality(quality: String) {
        _selectedQuality.value = quality
        val curDims = _selectedImageDimensions.value
        if (curDims != null) {
            updateCalculatedDimensions(curDims.width, curDims.height, quality)
        }
    }

    fun setMode(mode: String) {
        _selectedMode.value = mode
    }

    private fun updateCalculatedDimensions(origW: Int, origH: Int, quality: String) {
        val target = DimensionCalculator.calculateTargetDimensions(origW, origH, quality)
        _calculatedOutputDimensions.value = target
    }

    fun startEnhancement(forceLocalFallback: Boolean = false) {
        val uri = _selectedImageUri.value ?: return
        _isProcessing.value = true
        _processingProgress.value = 0.05f
        _processingStage.value = "Analyzing image"
        _errorMessage.value = null
        _serverFallbackPrompt.value = null

        viewModelScope.launch {
            try {
                val result = enhancementRepository.enhance(
                    sourceUri = uri,
                    quality = _selectedQuality.value,
                    mode = _selectedMode.value,
                    onProgress = { progress, stage ->
                        _processingProgress.value = progress
                        _processingStage.value = stage
                    },
                    forceLocal = forceLocalFallback
                )
                _enhancementResult.value = result
            } catch (e: ServerUnavailableException) {
                _serverFallbackPrompt.value = e.message ?: "AI server unavailable."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Enhancement failed. Please try again."
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun dismissFallbackPrompt() {
        _serverFallbackPrompt.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun resetForNewEnhancement() {
        _selectedImageUri.value = null
        _selectedImageDimensions.value = null
        _enhancementResult.value = null
        _errorMessage.value = null
        _serverFallbackPrompt.value = null
        _isProcessing.value = false
    }

    fun saveResultToGallery(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val res = _enhancementResult.value ?: return
        viewModelScope.launch {
            val saveResult = GallerySaver.saveImageToGallery(
                context = getApplication(),
                sourceFilePath = res.outputFilePath,
                quality = res.quality,
                format = settings.value.defaultOutputFormat
            )
            saveResult.onSuccess {
                onSuccess("Saved successfully to Pictures/PixelBoost AI!")
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Failed to save image to Gallery.")
            }
        }
    }

    fun shareResult() {
        val res = _enhancementResult.value ?: return
        ShareManager.shareImage(
            context = getApplication(),
            imageFilePath = res.outputFilePath,
            title = "PixelBoost AI ${res.quality} Enhancer"
        )
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.historyDao().deleteById(id)
        }
    }

    fun openHistoryItem(entity: HistoryEntity) {
        val enhancedFile = File(entity.enhancedImagePath)
        if (!enhancedFile.exists()) return

        val preview = BitmapFactory.decodeFile(entity.thumbnailPath)
            ?: BitmapFactory.decodeFile(entity.enhancedImagePath)
            ?: return

        val origFile = File(entity.originalImagePath)
        val origUri = if (origFile.exists()) Uri.fromFile(origFile) else null

        val resDims = parseResolution(entity.outputResolution)
        val inDims = parseResolution(entity.inputResolution)

        _selectedImageUri.value = origUri
        _selectedQuality.value = entity.qualityTier
        _selectedMode.value = entity.mode
        _enhancementResult.value = EnhancementResult(
            outputFilePath = entity.enhancedImagePath,
            previewBitmap = preview,
            originalWidth = inDims.first,
            originalHeight = inDims.second,
            outputWidth = resDims.first,
            outputHeight = resDims.second,
            processingTimeSeconds = entity.processingTimeSeconds,
            mode = entity.mode,
            quality = entity.qualityTier,
            isAiEnhanced = entity.isAiEnhanced,
            isServerProcessed = entity.isServerProcessed,
            fileSizeBytes = enhancedFile.length()
        )
    }

    private fun parseResolution(resStr: String): Pair<Int, Int> {
        val parts = resStr.split("×", "x", "*").map { it.trim().toIntOrNull() ?: 0 }
        return if (parts.size >= 2) Pair(parts[0], parts[1]) else Pair(3840, 2160)
    }

    fun testServerConnection(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = PixelBoostApiService.create(url)
                val resp = api.checkHealth()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val msg = "Connected! Device: ${body?.device ?: "ready"}, Model: ${if (body?.modelWeightsLoaded == true) "Real-ESRGAN weights active" else "Neural pipeline ready"}"
                    withContext(Dispatchers.Main) { onResult(true, msg) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Server returned HTTP ${resp.code()}") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Connection error: ${e.localizedMessage}") }
            }
        }
    }
}
