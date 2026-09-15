package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProcessingMode(val label: String) {
    AUTO("Auto"),
    ON_DEVICE("On Device"),
    SERVER("Server")
}

data class AppSettings(
    val processingMode: ProcessingMode = ProcessingMode.AUTO,
    val serverUrl: String = "http://10.0.2.2:8000",
    val defaultOutputFormat: String = "JPG",
    val jpegQuality: Int = 95,
    val faceEnhancement: Boolean = true,
    val autoEnhance: Boolean = false,
    val saveToGallery: Boolean = true,
    val darkMode: String = "Dark" // "Dark", "Light", "System"
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pixelboost_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val modeStr = prefs.getString("processing_mode", ProcessingMode.AUTO.name) ?: ProcessingMode.AUTO.name
        val mode = try {
            ProcessingMode.valueOf(modeStr)
        } catch (_: Exception) {
            ProcessingMode.AUTO
        }

        return AppSettings(
            processingMode = mode,
            serverUrl = prefs.getString("server_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000",
            defaultOutputFormat = prefs.getString("output_format", "JPG") ?: "JPG",
            jpegQuality = prefs.getInt("jpeg_quality", 95),
            faceEnhancement = prefs.getBoolean("face_enhancement", true),
            autoEnhance = prefs.getBoolean("auto_enhance", false),
            saveToGallery = prefs.getBoolean("save_to_gallery", true),
            darkMode = prefs.getString("dark_mode", "Dark") ?: "Dark"
        )
    }

    fun updateProcessingMode(mode: ProcessingMode) {
        prefs.edit().putString("processing_mode", mode.name).apply()
        _settings.value = _settings.value.copy(processingMode = mode)
    }

    fun updateServerUrl(url: String) {
        val cleanUrl = url.trim().trimEnd('/')
        prefs.edit().putString("server_url", cleanUrl).apply()
        _settings.value = _settings.value.copy(serverUrl = cleanUrl)
    }

    fun updateOutputFormat(format: String) {
        prefs.edit().putString("output_format", format).apply()
        _settings.value = _settings.value.copy(defaultOutputFormat = format)
    }

    fun updateJpegQuality(quality: Int) {
        prefs.edit().putInt("jpeg_quality", quality).apply()
        _settings.value = _settings.value.copy(jpegQuality = quality)
    }

    fun updateFaceEnhancement(enabled: Boolean) {
        prefs.edit().putBoolean("face_enhancement", enabled).apply()
        _settings.value = _settings.value.copy(faceEnhancement = enabled)
    }

    fun updateAutoEnhance(enabled: Boolean) {
        prefs.edit().putBoolean("auto_enhance", enabled).apply()
        _settings.value = _settings.value.copy(autoEnhance = enabled)
    }

    fun updateSaveToGallery(enabled: Boolean) {
        prefs.edit().putBoolean("save_to_gallery", enabled).apply()
        _settings.value = _settings.value.copy(saveToGallery = enabled)
    }

    fun updateDarkMode(mode: String) {
        prefs.edit().putString("dark_mode", mode).apply()
        _settings.value = _settings.value.copy(darkMode = mode)
    }
}
