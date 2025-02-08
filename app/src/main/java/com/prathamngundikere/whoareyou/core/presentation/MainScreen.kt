package com.prathamngundikere.whoareyou.core.presentation

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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

    var scaleFactor by remember { mutableFloatStateOf(1f) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }

    val mainViewModel = hiltViewModel<MainViewModel>()
    val resultState = mainViewModel.resultState.collectAsState().value

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "WHO ARE YOU?") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Camera Preview
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

            // Face Detection Overlay
            resultState.combinedResult?.let { result ->
                FaceDetectionOverlay(
                    results = result.faceDetections,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    context = context,
                    classifications = result.classifications,
                    onScaleFactorCalculated = {
                        scaleFactor = it
                    }
                )
            }

            // Cropped Face Preview in bottom right corner
            CroppedFacePreview(
                croppedFaces = resultState.croppedFaces,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}


@Composable
fun CroppedFacePreview(
    croppedFaces: List<Bitmap>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.6f))
    ) {
        if (croppedFaces.isNotEmpty()) {
            val bitmap = croppedFaces.first() // Show the first face
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Cropped Face",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}