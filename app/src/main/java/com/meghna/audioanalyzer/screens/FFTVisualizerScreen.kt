package com.meghna.audioanalyzer.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.meghna.audioanalyzer.AudioViewModel

@Composable
fun FFTVisualizerScreen(viewModel: AudioViewModel) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    if (hasPermission) {
        // Start capturing when permission is granted
        LaunchedEffect(Unit) {
            viewModel.startFftCapture()
        }
        // Stop capturing when leaving screen
        DisposableEffect(Unit) {
            onDispose { viewModel.stopFftCapture() }
        }
        FftContent(viewModel = viewModel)
    } else {
        PermissionRequestUI(
            onRequestPermission = {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
    }
}

@Composable
fun FftContent(viewModel: AudioViewModel) {
    val fftBands by viewModel.fftBands.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "FFT Spectrum Analyzer",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Real-time microphone frequency analysis",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Frequency band labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Bass", "Low-Mid", "Mid", "Treble").forEach { label ->
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // FFT Bar Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                val numBands = fftBands.size
                if (numBands == 0) return@Canvas

                val barWidth = (size.width / numBands) * 0.8f
                val gap = (size.width / numBands) * 0.2f

                fftBands.forEachIndexed { index, magnitude ->
                    val barHeight = (size.height * magnitude).coerceAtLeast(4f)
                    val x = index * (barWidth + gap)
                    val y = size.height - barHeight

                    // Color based on frequency band
                    val color = when {
                        index < 8 -> Color(0xFF2196F3)   // Bass — Blue
                        index < 16 -> Color(0xFF4CAF50)  // Low-Mid — Green
                        index < 24 -> Color(0xFFFFEB3B)  // Mid — Yellow
                        else -> Color(0xFFF44336)         // Treble — Red
                    }

                    // Draw bar
                    drawRect(
                        color = color.copy(alpha = 0.9f),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )

                    // Draw peak glow at top
                    if (magnitude > 0.1f) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.6f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, 3f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live stats
        FftStatsBar(fftBands = fftBands)
    }
}

@Composable
fun FftStatsBar(fftBands: FloatArray) {
    val bassAvg = if (fftBands.size >= 8)
        fftBands.take(8).average().toFloat() else 0f
    val midAvg = if (fftBands.size >= 24)
        fftBands.drop(8).take(16).average().toFloat() else 0f
    val trebleAvg = if (fftBands.size >= 32)
        fftBands.drop(24).take(8).average().toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FftStatItem(
                value = "${(bassAvg * 100).toInt()}%",
                label = "Bass",
                color = Color(0xFF2196F3)
            )
            FftStatItem(
                value = "${(midAvg * 100).toInt()}%",
                label = "Mid",
                color = Color(0xFF4CAF50)
            )
            FftStatItem(
                value = "${(trebleAvg * 100).toInt()}%",
                label = "Treble",
                color = Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun FftStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun PermissionRequestUI(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🎤",
                fontSize = 48.sp
            )
            Text(
                text = "Microphone Permission Required",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "The FFT Visualizer needs microphone access to analyze real-time audio frequencies.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("Grant Permission")
            }
        }
    }
}