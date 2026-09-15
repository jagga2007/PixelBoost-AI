package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary

@Composable
fun ZoomableImageViewer(
    bitmap: Bitmap,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.2f) {
                            scale = 1.0f
                            offset = Offset.Zero
                        } else {
                            scale = 3.0f
                            offset = Offset(
                                (size.width / 2f - tapOffset.x) * 2f,
                                (size.height / 2f - tapOffset.y) * 2f
                            )
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1.0f, 10.0f)
                    if (scale == 1.0f) {
                        offset = Offset.Zero
                    } else {
                        val maxOffsetX = (size.width * (scale - 1f)) / 2f
                        val maxOffsetY = (size.height * (scale - 1f)) / 2f
                        offset = Offset(
                            x = (offset.x + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX),
                            y = (offset.y + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    }
                }
            }
            .testTag("zoomable_image_viewer")
    ) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

        Image(
            bitmap = imageBitmap,
            contentDescription = "Zoomable enhanced high-resolution image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )

        // Close Button
        FilledTonalIconButton(
            onClick = onClose,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color(0x99000000),
                contentColor = Color.White
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .testTag("zoom_close_button")
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close zoom viewer")
        }

        // Reset Zoom Button & Scale Badge
        Surface(
            shape = CircleShape,
            color = Color(0x99000000),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(scale * 100).toInt()}% • Double tap to toggle",
                    color = CyanPrimary,
                    fontSize = 13.sp
                )
            }
        }

        if (scale > 1.05f) {
            FilledTonalIconButton(
                onClick = {
                    scale = 1.0f
                    offset = Offset.Zero
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0x99000000),
                    contentColor = CyanPrimary
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset zoom")
            }
        }
    }
}
