package com.meghna.audioanalyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.meghna.audioanalyzer.ui.theme.AudioAnalyzerTheme

class MainActivity : ComponentActivity() {

    // Get the ViewModel
    private val viewModel: AudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioAnalyzerTheme {

                // Collect the StateFlow as Compose state
                val seconds by viewModel.secondsElapsed.collectAsState()

                // Display it on screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Seconds: $seconds",
                        fontSize = 32.sp
                    )
                }
            }
        }
    }
}