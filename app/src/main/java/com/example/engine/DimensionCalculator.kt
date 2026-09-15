package com.example.engine

import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ImageDimensions(
    val width: Int,
    val height: Int
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 1.0f

    val formatted: String
        get() = "${width} × ${height}"

    val totalPixels: Long
        get() = width.toLong() * height.toLong()
}

object DimensionCalculator {
    private const val MAX_2K = 2560
    private const val MAX_4K = 3840
    private const val MAX_8K = 7680
    private const val MAX_12K = 11520
    private const val MAX_16K = 15360

    // Maximum pixel canvas bound (~16K 16:9 standard canvas)
    private const val MAX_CANVAS_PIXELS = 15360L * 8640L

    fun getMaxDimensionForQuality(quality: String): Int {
        return when (quality.uppercase().trim()) {
            "2K" -> MAX_2K
            "4K" -> MAX_4K
            "8K" -> MAX_8K
            "12K" -> MAX_12K
            "16K" -> MAX_16K
            else -> MAX_4K
        }
    }

    /**
     * Calculates the exact target dimensions preserving original aspect ratio.
     * Does NOT force into 16:9.
     * Keeps 16:9 -> ~15360x8640, 4:3 -> ~15360x11520 (or proportional), 1:1 -> proportional square.
     */
    fun calculateTargetDimensions(
        origWidth: Int,
        origHeight: Int,
        quality: String
    ): ImageDimensions {
        if (origWidth <= 0 || origHeight <= 0) {
            val maxD = getMaxDimensionForQuality(quality)
            return ImageDimensions(maxD, (maxD * 9f / 16f).roundToInt())
        }

        val maxDim = getMaxDimensionForQuality(quality).toFloat()
        val aspect = origWidth.toFloat() / origHeight.toFloat()

        var targetW: Float
        var targetH: Float

        if (origWidth >= origHeight) {
            targetW = maxDim
            targetH = targetW / aspect
        } else {
            targetH = maxDim
            targetW = targetH * aspect
        }

        // Check if total pixels exceeds canvas safety bound
        val totalPix = (targetW * targetH).toLong()
        if (totalPix > MAX_CANVAS_PIXELS) {
            val scale = sqrt(MAX_CANVAS_PIXELS.toDouble() / totalPix.toDouble()).toFloat()
            targetW *= scale
            targetH *= scale
        }

        var finalW = targetW.roundToInt()
        var finalH = targetH.roundToInt()

        // Ensure even dimensions
        if (finalW % 2 != 0) finalW += 1
        if (finalH % 2 != 0) finalH += 1

        return ImageDimensions(finalW, finalH)
    }
}
