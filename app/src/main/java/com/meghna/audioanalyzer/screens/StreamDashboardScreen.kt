package com.meghna.audioanalyzer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meghna.audioanalyzer.AudioViewModel
import com.meghna.audioanalyzer.data.model.AudioStreamInfo
import com.meghna.audioanalyzer.data.model.AudioDeviceInfo

@Composable
fun StreamDashboardScreen(
    viewModel: AudioViewModel = hiltViewModel()
) {
    val audioStreams by viewModel.audioStreams.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Audio Streams",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(audioStreams) { stream ->
            AudioStreamCard(stream = stream)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connected Devices",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(connectedDevices) { device ->
            AudioDeviceCard(device = device)
        }
    }
}

@Composable
fun AudioStreamCard(stream: AudioStreamInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (stream.isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stream.streamType,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Volume: ${stream.volumeLevel} / ${stream.maxVolume}")
            Text(text = if (stream.isActive) "● Active" else "○ Silent")
        }
    }
}

@Composable
fun AudioDeviceCard(device: AudioDeviceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = device.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(text = "Type: ${device.type}")
            Text(text = if (device.isOutput) "Output Device" else "Input Device")
        }
    }
}
