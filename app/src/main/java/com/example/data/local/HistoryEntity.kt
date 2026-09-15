package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enhancement_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val inputResolution: String,
    val outputResolution: String,
    val qualityTier: String, // 2K, 4K, 8K, 12K, 16K
    val mode: String, // Balanced, Face Enhance, Detail Recovery, Portrait, Restoration
    val originalImagePath: String,
    val enhancedImagePath: String,
    val thumbnailPath: String,
    val processingTimeSeconds: Double,
    val isAiEnhanced: Boolean = true,
    val isServerProcessed: Boolean = false,
    val fileSizeFormatted: String
)
