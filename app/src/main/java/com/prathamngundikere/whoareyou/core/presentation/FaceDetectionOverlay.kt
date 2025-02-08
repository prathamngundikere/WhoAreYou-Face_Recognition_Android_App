package com.prathamngundikere.whoareyou.core.presentation

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.prathamngundikere.whoareyou.R

@Composable
fun FaceDetectionOverlay(
    results: FaceDetectorResult?,
    imageWidth: Int,
    imageHeight: Int,
    context: Context,
    classifications: List<Pair<String, Float>>?
) {

    val boxColor = remember { Color(ContextCompat.getColor(context, R.color.teal_200)) }

    // Remember canvas size dynamically
    var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { newSize ->
                canvasSize = Size(newSize.width.toFloat(), newSize.height.toFloat())
            }
    ) {

        if (imageWidth == 0 || imageHeight == 0 || canvasSize.width == 0f || canvasSize.height == 0f) return@Canvas

        // Use actual canvas size
        val scaleX = canvasSize.width / imageWidth
        val scaleY = canvasSize.height / imageHeight

        results?.detections()?.forEachIndexed { index, detection ->
            //val paddingBox = 20f
            val boundingBox = detection.boundingBox()

            val left = boundingBox.left * scaleX
            val top = boundingBox.top * scaleY
            val right = boundingBox.right * scaleX
            val bottom = boundingBox.bottom * scaleY

            // Draw the rectangle (bounding box)
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 8f)
            )
            // Draw the label text above the bounding box.
            // For simplicity we use the label from the classification at the same index.
            val label = classifications?.getOrNull(index)?.first ?: "Unknown"
            // Draw the text using the native canvas.
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    label,
                    left,
                    top - 10f, // place the text a bit above the box
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GREEN
                        textSize = 40f
                        // Optionally, set additional text properties (e.g., bold, anti-alias, etc.)
                    }
                )
            }
        }
    }
}