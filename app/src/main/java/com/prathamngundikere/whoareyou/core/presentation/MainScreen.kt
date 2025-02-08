package com.prathamngundikere.whoareyou.core.presentation

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.prathamngundikere.whoareyou.faceClassifier.presentation.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    context: Context
) {

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // State variables for scaling and image dimensions.
    var scaleFactor by remember { mutableFloatStateOf(1f) }
    var imageWidth by remember {mutableIntStateOf(0)}
    var imageHeight by remember {mutableIntStateOf(0)}

    val mainViewModel = hiltViewModel<MainViewModel>()
    val resultState = mainViewModel.resultState.collectAsState().value
    val combinedResult = resultState.combinedResult

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "WHO ARE YOU?") }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CameraPreview(
                cameraProviderFuture = cameraProviderFuture,
                lifecycleOwner = lifecycleOwner
            ) { imageProxy ->
                imageWidth = imageProxy.width
                imageHeight = imageProxy.height
                mainViewModel.processImage(
                    imageProxy = imageProxy,
                    scaleFactor = scaleFactor
                )
            }
            combinedResult.let { result ->
                FaceDetectionOverlay(
                    results = result?.faceDetections,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    context = context,
                    classifications = result?.classifications,
                    onScaleFactorCalculated =  {
                        scaleFactor = it
                    }
                )
            }
        }
    }
}