package com.prathamngundikere.whoareyou.core.presentation

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.prathamngundikere.whoareyou.R
import kotlin.math.min

@Composable
fun FaceDetectionOverlay(
    results: FaceDetectorResult?,
    imageWidth: Int,
    imageHeight: Int,
    context: Context,
    classifications: List<Pair<String, Float>>?,
    onScaleFactorCalculated: (Float) -> Unit,
) {
    val boxColor = remember { Color(ContextCompat.getColor(context, R.color.teal_200)) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {

        // Calculate aspect ratios
        val imageAspectRatio = imageWidth.toFloat() / imageHeight
        val canvasAspectRatio = size.width / size.height

        // Calculate actual dimensions maintaining aspect ratio
        val scaledWidth: Float
        val scaledHeight: Float
        val offsetX: Float
        val offsetY: Float

        if (imageAspectRatio > canvasAspectRatio) {
            // Image is wider than canvas
            scaledWidth = size.width
            scaledHeight = size.width / imageAspectRatio
            offsetX = 0f
            offsetY = (size.height - scaledHeight) / 2
        } else {
            // Image is taller than canvas
            scaledHeight = size.height
            scaledWidth = size.height * imageAspectRatio
            offsetX = (size.width - scaledWidth) / 2
            offsetY = 0f
        }

        // Calculate scale factors for both dimensions
        val scaleX = scaledWidth / imageWidth
        val scaleY = scaledHeight / imageHeight

        // Send the scale factor back (use the smaller one to ensure box fits)
        onScaleFactorCalculated(minOf(scaleX, scaleY))

        results?.detections()?.forEachIndexed { index, detection ->
            val boundingBox = detection.boundingBox()

            // Calculate box dimensions with proper scaling and offset
            val left = boundingBox.left * scaleX + offsetX
            val top = boundingBox.top * scaleY + offsetY
            val right = boundingBox.right * scaleX + offsetX
            val bottom = boundingBox.bottom * scaleY + offsetY

            // Draw the rectangle (bounding box)
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 8f)
            )

            // Draw the label text
            val label = classifications?.getOrNull(index)?.first ?: "Unknown"
            val confidence = classifications?.getOrNull(index)?.second
            val displayText = if (confidence != null) {
                "$label (${(confidence * 100).toInt()}%)"
            } else {
                label
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    displayText,
                    left,
                    top - 10f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GREEN
                        textSize = 40f
                        isFakeBoldText = true
                        setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
                    }
                )
            }
        }
    }
}