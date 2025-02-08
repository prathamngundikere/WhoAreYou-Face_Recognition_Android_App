package com.prathamngundikere.whoareyou.core.presentation

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                mainViewModel.processImage(
                    imageProxy = imageProxy,
                    scaleFactor = scaleFactor
                )
            }

            // Face Detection Overlay
            resultState.combinedResult?.let { result ->
                FaceDetectionOverlay(
                    results = result.faceDetections,
                    imageWidth = result.imageWidth,
                    imageHeight = result.imageHeight,
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
            .size(160.dp)  // Increased size for better visibility
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        if (croppedFaces.isNotEmpty()) {
            val bitmap = croppedFaces.first()
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Cropped Face",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            // Show placeholder when no face is detected
            Text(
                text = "No face detected",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp)
            )
        }
    }
}