package com.prathamngundikere.whoareyou.faceClassifier.domain.repository

import androidx.camera.core.ImageProxy
import com.prathamngundikere.whoareyou.faceClassifier.domain.model.CombinedResult

interface FaceClassifier {
    suspend fun processImage(imageProxy: ImageProxy, scaleFactor: Float): CombinedResult
    suspend fun release()
}