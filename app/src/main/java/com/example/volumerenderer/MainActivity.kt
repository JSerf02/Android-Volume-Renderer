package com.example.volumerenderer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.volumerenderer.ui.theme.VolumeRendererTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Setup the Android activity
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get the ViewModel which manages the UI state and calls the code that runs the volume
        // renderer in the backend
        val viewModel: RendererViewModel by viewModels()

        // Display the UI on the screen using Jetpack Compose
        setContent {
            VolumeRendererTheme {
                ComposeRoot(viewModel, applicationContext)
            }
        }
    }
}