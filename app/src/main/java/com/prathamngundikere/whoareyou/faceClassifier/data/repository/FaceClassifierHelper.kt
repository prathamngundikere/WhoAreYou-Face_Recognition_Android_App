package com.prathamngundikere.whoareyou.faceClassifier.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.prathamngundikere.whoareyou.faceClassifier.domain.model.CombinedResult
import com.prathamngundikere.whoareyou.faceClassifier.domain.repository.FaceClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject

/**
 * A combined helper class that:
 *  1. Takes an MPImage (from the live camera stream),
 *  2. Runs MediaPipe face detection in LIVE_STREAM mode,
 *  3. For each detected face (using its bounding box) crops out that region from the image,
 *  4. Runs a second TFLite model (classification) on the cropped face,
 *  5. Returns a CombinedResult containing the list of bounding boxes, the list of (label, confidence)
 *     pairs, and the overall inference time.
 *
 * **Note:** You must implement [mpImageToBitmap] for your particular MPImage subclass.
 */
class FaceClassifierHelper @Inject constructor(
    private val context: Context
): FaceClassifier {

    companion object {
        private const val FACE_MODEL = "blaze_face_short_range.tflite"
        private const val CLASSIFIER_MODEL = "model.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val MIN_DETECTION_CONFIDENCE = 0.5f
        private const val TAG = "CombinedFaceProcessor"
    }

    // Add this property to store the last cropped faces
    private var lastCroppedFaces: List<Bitmap> = emptyList()

    // Add this function to access the last cropped faces
    fun getLastCroppedFaces(): List<Bitmap> = lastCroppedFaces

    private val faceDetectionFlow = MutableSharedFlow<FaceDetectorResult>(replay = 1)

    // Create and store the detector when the helper is created.
    private val faceDetector: FaceDetector by lazy {
        val baseOptions = BaseOptions.builder()
            .setDelegate(Delegate.CPU)
            .setModelAssetPath(FACE_MODEL)
            .build()
        val options = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinDetectionConfidence(MIN_DETECTION_CONFIDENCE)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                faceDetectionFlow.tryEmit(result)
            }
            .setErrorListener { error ->
                Log.e(TAG, "Face Detection error: ${error.message}", error)
            }
            .build()
        FaceDetector.createFromOptions(context, options)
    }

    /**
     * Processes a live camera frame (as an [MPImage]) by detecting faces and classifying them.
     *
     * @param imageProxy The live stream image.
     * @return A [CombinedResult] containing detected face bounding boxes, classification results,
     *         and the total inference time.
     */
    override suspend fun processImage(imageProxy: ImageProxy, scaleFactor: Float): CombinedResult = withContext(Dispatchers.Default) {
        Log.i(TAG, "In the process Image")
        val startTime = SystemClock.uptimeMillis()

        // Convert the MPImage to a Bitmap (needed for cropping).
        val bitmap = mpImageToBitmap(imageProxy)

        val mpImage = BitmapImageBuilder(bitmap).build()

        faceDetector.detectAsync(mpImage, startTime)

        // Run face detection asynchronously (using LIVE_STREAM mode).
        val faceDetectionResult = faceDetectionFlow.first()

        val croppedBitmap = cropFaces(
            bitmap = bitmap,
            result = faceDetectionResult,
            scaleFactor = scaleFactor
        )
        val classificationResults = mutableListOf<Pair<String, Float>>()

        croppedBitmap.forEach {
            classificationResults.add(classifyFace(it))
        }

        val totalTime = SystemClock.uptimeMillis() - startTime
        CombinedResult(
            faceDetections = faceDetectionResult,
            classifications = classificationResults,
            inferenceTime = totalTime,
            imageWidth = mpImage.width,
            imageHeight = mpImage.height
        )
    }

    override suspend fun release() {
        withContext(Dispatchers.IO) {
            faceDetector.close()
        }
    }

    // Classifies the given face Bitmap using a TFLite model.
    private suspend fun classifyFace(faceBitmap: Bitmap): Pair<String, Float> =
        withContext(Dispatchers.IO) {
            Log.i(TAG, "In the classifyFace")
            // Load the TFLite model.
            val interpreter = Interpreter(loadModelFile())
            val labels = loadLabels()

            // Get input dimensions (expected shape: [1, height, width, 3]).
            val inputShape = interpreter.getInputTensor(0).shape()
            val inputHeight = inputShape[1]
            val inputWidth = inputShape[2]

            // Resize the face Bitmap to match the model’s expected input size.
            val resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, inputWidth, inputHeight, true)

            // Prepare the input ByteBuffer (using 4 bytes per float).
            val inputBuffer = ByteBuffer.allocateDirect(1 * inputHeight * inputWidth * 3 * 4)
            inputBuffer.order(ByteOrder.nativeOrder())
            val intValues = IntArray(inputWidth * inputHeight)
            resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)
            for (pixel in intValues) {
                val r = ((pixel shr 16) and 0xFF).toFloat()
                val g = ((pixel shr 8) and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()
                // Normalize the pixel values.
                inputBuffer.putFloat((r - 127f) / 128f)
                inputBuffer.putFloat((g - 127f) / 128f)
                inputBuffer.putFloat((b - 127f) / 128f)
            }

            // Prepare the output buffer.
            val outputShape = interpreter.getOutputTensor(0).shape() // [1, numClasses]
            val numClasses = outputShape[1]
            val outputBuffer = ByteBuffer.allocateDirect(numClasses * 4)
            outputBuffer.order(ByteOrder.nativeOrder())

            // Run inference.
            interpreter.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()

            // Extract the scores.
            val scores = FloatArray(numClasses)
            for (i in 0 until numClasses) {
                scores[i] = outputBuffer.float
            }

            // Find the index of the maximum score.
            val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: -1
            val confidence = if (maxIndex != -1) scores[maxIndex] else 0f
            val label = if (maxIndex in labels.indices) labels[maxIndex] else "Unknown"

            interpreter.close()
            Pair(label, confidence)
        }

    // Loads a TFLite model file from the assets folder.
    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(CLASSIFIER_MODEL)
        FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }

    // Loads the label list from the assets folder.
    private fun loadLabels(): List<String> {
        return context.assets.open(LABELS_FILE).bufferedReader().readLines()
    }

    // Converts an [MPImage] to a [Bitmap].
    // You must implement this conversion based on your MPImage subclass.
    private fun mpImageToBitmap(imageProxy: ImageProxy): Bitmap {
        // Extract image dimensions and rotation.
        val width = imageProxy.width
        val height = imageProxy.height
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        // Get the pixel buffer from the first plane.
        val buffer = imageProxy.planes[0].buffer

        // Copy the buffer into a byte array.
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes) // This advances the buffer position to the end.

        // Create a new ByteBuffer from the byte array.
        val byteBuffer = ByteBuffer.wrap(bytes)

        // Create a Bitmap to hold the image.
        val bitmapBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmapBuffer.copyPixelsFromBuffer(byteBuffer)

        // Now that we've extracted all the needed data, close the imageProxy.
        //imageProxy.close()

        // Rotate the bitmap if needed.
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(bitmapBuffer, 0, 0, width, height, matrix, true)
    }

    private fun cropFaces(
        bitmap: Bitmap,
        result: FaceDetectorResult,
        scaleFactor: Float
    ): List<Bitmap> {
        Log.i(TAG, "In cropped faces")
        val croppedFaces = mutableListOf<Bitmap>()

        result.detections().forEach { detection ->
            val boundingBox = detection.boundingBox()

            var left = (boundingBox.left * scaleFactor).toInt().coerceIn(0, bitmap.width)
            var top = (boundingBox.top * scaleFactor).toInt().coerceIn(0, bitmap.height)
            var right = (boundingBox.right * scaleFactor).toInt().coerceIn(0, bitmap.width)
            var bottom = (boundingBox.bottom * scaleFactor).toInt().coerceIn(0, bitmap.height)

            val width = (right - left)
            val height = (bottom - top)

            val maxSide = maxOf(width, height)

            val centerX = boundingBox.centerX().toInt()
            val centerY = boundingBox.centerY().toInt()
            var newLeft = 0
            var newTop = 0
            if ((bitmap.width - maxSide) > 0 && (bitmap.height - maxSide) > 0) {
                newLeft = (centerX - maxSide / 2).coerceIn(0, bitmap.width - maxSide)
                newTop = (centerY - maxSide / 2).coerceIn(0, bitmap.height - maxSide)
            }
            try {
                val faceBitmap = Bitmap.createBitmap(bitmap, newLeft, newTop, maxSide, maxSide)
                croppedFaces.add(faceBitmap)
            } catch (e: Exception) {
                Log.e("FaceCrop", "Error Cropping the image: ${e.message}")
                e.printStackTrace()
            }
        }

        // Store the cropped faces
        lastCroppedFaces = croppedFaces
        return croppedFaces
    }
}