package com.prathamngundikere.whoareyou.faceClassifier.presentation

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathamngundikere.whoareyou.faceClassifier.data.repository.FaceClassifierHelper
import com.prathamngundikere.whoareyou.faceClassifier.domain.repository.FaceClassifier
import com.prathamngundikere.whoareyou.faceClassifier.util.FaceDetectionState
import com.prathamngundikere.whoareyou.faceClassifier.util.ResultState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val faceClassifier: FaceClassifier
): ViewModel() {

    private val _resultState = MutableStateFlow(FaceDetectionState())
    val resultState = _resultState.asStateFlow()

    fun processImage(
        imageProxy: ImageProxy,
        scaleFactor: Float
    ) {
        viewModelScope.launch {
            try {
                val result = faceClassifier.processImage(imageProxy, scaleFactor)
                _resultState.value = FaceDetectionState(
                    combinedResult = result,
                    croppedFaces = (faceClassifier as FaceClassifierHelper).getLastCroppedFaces()
                )
            } finally {
                imageProxy.close()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            faceClassifier.release()
        }
    }
}