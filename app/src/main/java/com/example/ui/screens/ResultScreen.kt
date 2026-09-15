package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.components.ZoomableImageViewer
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ResultScreen(
    viewModel: MainViewModel,
    onEnhanceAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val result by viewModel.enhancementResult.collectAsState()
    val origUri by viewModel.selectedImageUri.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showZoomViewer by remember { mutableStateOf(false) }

    if (result == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No result to display", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val res = result!!

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Success Badge Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldSuccess.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Enhanced Successfully",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (res.isAiEnhanced) "AI Reconstruction Active • ${res.quality} Canvas" else "Lanczos High-Fidelity Upscale",
                        fontSize = 12.sp,
                        color = if (res.isAiEnhanced) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Interactive BEFORE / AFTER Comparison Slider
        item {
            BeforeAfterSlider(
                beforeModel = origUri,
                afterBitmap = res.previewBitmap,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            )
        }

        // 3. Primary Actions: Zoom & Save Image & Share
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showZoomViewer = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("zoom_button")
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🔍 Zoom", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        viewModel.saveResultToGallery(
                            onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color(0xFF041E28)
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                        .testTag("save_image_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Image", fontWeight = FontWeight.Black)
                }

                OutlinedButton(
                    onClick = { viewModel.shareResult() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Output Information Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Output Specifications",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    SpecRow("Resolution", "${res.outputWidth} × ${res.outputHeight}")
                    SpecRow("Original", "${res.originalWidth} × ${res.originalHeight}")
                    SpecRow("Format", settings.defaultOutputFormat)
                    SpecRow("Quality", "High (${settings.jpegQuality}%)")
                    SpecRow("Mode", res.mode)
                    SpecRow("Processing Time", "${res.processingTimeSeconds}s")
                    SpecRow("Engine", if (res.isServerProcessed) "AI Cloud Server (PyTorch)" else "On-Device Neural Engine")
                }
            }
        }

        // 5. Enhance Another Button
        item {
            Button(
                onClick = {
                    viewModel.resetForNewEnhancement()
                    onEnhanceAnother()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("enhance_another_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enhance Another", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Fullscreen Zoom Viewer Dialog
    if (showZoomViewer) {
        Dialog(
            onDismissRequest = { showZoomViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ZoomableImageViewer(
                bitmap = res.previewBitmap,
                onClose = { showZoomViewer = false }
            )
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
