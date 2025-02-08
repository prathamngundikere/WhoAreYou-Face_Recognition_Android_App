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
import androidx.compose.ui.unit.dp
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
        modifier = Modifier.fillMaxSize()
    ) {

        val scaleFactor = min(size.width / imageWidth, size.height / imageHeight)

        // Send scaleFactor back to FaceDetectionScreen
        onScaleFactorCalculated(scaleFactor)

        results?.detections()?.forEachIndexed { index, detection ->
            val boundingBox = detection.boundingBox()

            // Add padding to the box (20% of the face size)
            val padX = (boundingBox.width() * 0.2f)
            val padY = (boundingBox.height() * 0.2f)

            // Calculate box dimensions with padding
            val left = (boundingBox.left - padX) * scaleFactor
            val top = (boundingBox.top - padY) * scaleFactor
            val right = (boundingBox.right + padX) * scaleFactor
            val bottom = (boundingBox.bottom + padY) * scaleFactor

            // Draw the rectangle (bounding box)
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4.dp.toPx())  // Made line thicker
            )

            // Get classification info
            val label = classifications?.getOrNull(index)?.first ?: "Unknown"
            val confidence = classifications?.getOrNull(index)?.second
            val displayText = if (confidence != null) {
                "$label (${(confidence * 100).toInt()}%)"
            } else {
                label
            }

            // Draw the label text
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GREEN
                    textSize = 48f  // Increased text size
                    isFakeBoldText = true
                    //setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)  // Added stronger shadow
                    textAlign = android.graphics.Paint.Align.LEFT
                }

                // Calculate text position (moved slightly higher above the box)
                val textX = left
                val textY = top - 20f  // Moved text higher above the box

                // Draw text background for better readability
                val textBounds = android.graphics.Rect()
                paint.getTextBounds(displayText, 0, displayText.length, textBounds)
                val backgroundPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(128, 0, 0, 0)  // Semi-transparent black
                }
                drawRect(
                    textX - 4f,
                    textY - textBounds.height() - 4f,
                    textX + textBounds.width() + 4f,
                    textY + 4f,
                    backgroundPaint
                )

                // Draw the text
                drawText(displayText, textX, textY, paint)
            }
        }
    }
}