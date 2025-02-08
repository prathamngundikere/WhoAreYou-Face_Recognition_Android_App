package com.prathamngundikere.whoareyou

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.prathamngundikere.whoareyou.core.presentation.MainScreen
import com.prathamngundikere.whoareyou.ui.theme.WhoAreYouTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhoAreYouTheme {
                MainScreen(context = applicationContext)
            }
        }
    }
}