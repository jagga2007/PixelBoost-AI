package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CyanPrimary
import kotlin.math.roundToInt

@Composable
fun BeforeAfterSlider(
    beforeModel: Any?, // Uri or Bitmap
    afterBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableFloatStateOf(0.5f) } // 0.0 (all after) to 1.0 (all before)

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    sliderPosition = (offset.x / size.width).coerceIn(0.05f, 0.95f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    sliderPosition = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                }
            }
            .testTag("before_after_slider")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val splitX = widthPx * sliderPosition

        // 1. Bottom Layer: Enhanced AFTER image (full width)
        if (afterBitmap != null) {
            val imgBitmap = remember(afterBitmap) { afterBitmap.asImageBitmap() }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(
                    image = imgBitmap,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                )
            }
        }

        // 2. Top Layer: BEFORE image clipped to the left side [0 .. splitX]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = splitX,
                        bottom = size.height,
                        clipOp = ClipOp.Intersect
                    ) {
                        this@drawWithContent.drawContent()
                    }
                }
        ) {
            AsyncImage(
                model = beforeModel,
                contentDescription = "Original image before enhancement",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        // 3. Vertical Divider Line
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color.White,
                start = Offset(splitX, 0f),
                end = Offset(splitX, heightPx),
                strokeWidth = 3.dp.toPx()
            )
            // Accent glow behind divider
            drawLine(
                color = CyanPrimary.copy(alpha = 0.6f),
                start = Offset(splitX, 0f),
                end = Offset(splitX, heightPx),
                strokeWidth = 6.dp.toPx()
            )
        }

        // 4. Center Drag Handle
        Box(
            modifier = Modifier
                .offset { IntOffset((splitX - 22.dp.toPx()).roundToInt(), (heightPx / 2f - 22.dp.toPx()).roundToInt()) }
                .size(44.dp)
                .clip(CircleShape)
                .background(CyanPrimary)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF090D16),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "Slide to compare",
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 5. "BEFORE" and "AFTER" badges
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "BEFORE",
                color = Color(0xFFE2E8F0),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CyanPrimary.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Text(
                text = "AFTER (AI)",
                color = Color(0xFF041E28),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
