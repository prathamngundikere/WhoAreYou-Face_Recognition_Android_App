package com.prathamngundikere.whoareyou.faceClassifier.domain.model

import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

/**
 * Data class wrapping the combined detection and classification results.
 *
 * @param faceDetections List of face bounding boxes detected in the image.
 * @param classifications List of pairs containing the predicted label and its confidence for each face.
 * @param inferenceTime Total processing time (in ms) for this image.
 */
data class CombinedResult(
    val faceDetections: FaceDetectorResult,
    val classifications: List<Pair<String, Float>>,
    val inferenceTime: Long,
    val imageWidth: Int,
    val imageHeight: Int
)