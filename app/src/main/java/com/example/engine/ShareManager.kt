package com.example.engine

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareManager {
    fun shareImage(context: Context, imageFilePath: String, title: String = "Enhanced Image with PixelBoost AI") {
        try {
            val file = File(imageFilePath)
            if (!file.exists()) return

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Enhanced to ultra-high resolution with PixelBoost AI 16K!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Enhanced Image").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
