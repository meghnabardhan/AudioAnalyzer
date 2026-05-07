package com.meghna.audioanalyzer.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghna.audioanalyzer.AudioViewModel
import com.meghna.audioanalyzer.data.model.AudioDeviceInfo
import com.meghna.audioanalyzer.data.model.AudioStreamInfo

@Composable
fun RoutingGraphScreen(viewModel: AudioViewModel) {
    val audioStreams by viewModel.audioStreams.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()

    val outputDevices = connectedDevices.filter { it.isOutput }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Audio Routing Graph",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Shows how audio streams route to output devices",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LegendItem(color = Color(0xFF4CAF50), label = "Active")
            LegendItem(color = Color(0xFF546E7A), label = "Silent")
            LegendItem(color = Color(0xFF2196F3), label = "Output Device")
        }

        // Graph
        RoutingGraph(
            streams = audioStreams,
            devices = outputDevices,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stats bar at bottom
        StatsBar(streams = audioStreams, devices = outputDevices)
    }
}

@Composable
fun RoutingGraph(
    streams: List<AudioStreamInfo>,
    devices: List<AudioDeviceInfo>,
    modifier: Modifier = Modifier
) {
    val displayDevices = if (devices.isEmpty()) {
        listOf(AudioDeviceInfo(0, "Speaker", "Built-in Speaker", true))
    } else {
        devices
    }

    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val boxWidth = constraints.maxWidth.toFloat()
        val boxHeight = constraints.maxHeight.toFloat()

        val nodeWidthPx = with(density) { 130.dp.toPx() }
        val nodeHeightPx = with(density) { 64.dp.toPx() }

        // Calculate Y centers for streams
        val streamSpacing = if (streams.isNotEmpty())
            boxHeight / streams.size else boxHeight

        val streamCenters = streams.mapIndexed { index, _ ->
            Offset(
                x = nodeWidthPx,
                y = streamSpacing * index + streamSpacing / 2f
            )
        }

        // Calculate Y centers for devices
        val deviceSpacing = if (displayDevices.isNotEmpty())
            boxHeight / displayDevices.size else boxHeight

        val deviceCenters = displayDevices.mapIndexed { index, _ ->
            Offset(
                x = boxWidth - nodeWidthPx,
                y = deviceSpacing * index + deviceSpacing / 2f
            )
        }

        // Draw lines on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            streams.forEachIndexed { streamIndex, stream ->
                val start = streamCenters.getOrNull(streamIndex) ?: return@forEachIndexed
                val color = if (stream.isActive)
                    Color(0xFF4CAF50) else Color(0xFF546E7A)
                val alpha = if (stream.isActive) 0.8f else 0.3f
                val strokeWidth = if (stream.isActive) 3f else 1f

                deviceCenters.forEach { end ->
                    val midX = (start.x + end.x) / 2f
                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = start,
                        end = Offset(midX, start.y),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = Offset(midX, start.y),
                        end = Offset(midX, end.y),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = Offset(midX, end.y),
                        end = end,
                        strokeWidth = strokeWidth
                    )
                }
            }
        }

        // Stream nodes LEFT
        streams.forEachIndexed { index, stream ->
            val centerY = streamCenters.getOrNull(index)?.y ?: return@forEachIndexed
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(64.dp)
                    .offset(
                        x = 0.dp,
                        y = with(density) { (centerY - nodeHeightPx / 2f).toDp() }
                    )
            ) {
                StreamNode(stream = stream, modifier = Modifier.fillMaxSize())
            }
        }

        // Device nodes RIGHT
        displayDevices.forEachIndexed { index, device ->
            val centerY = deviceCenters.getOrNull(index)?.y ?: return@forEachIndexed
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(64.dp)
                    .offset(
                        x = with(density) { (boxWidth - nodeWidthPx).toDp() },
                        y = with(density) { (centerY - nodeHeightPx / 2f).toDp() }
                    )
            ) {
                DeviceNode(
                    name = device.name,
                    type = device.type,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun StreamNode(
    stream: AudioStreamInfo,
    modifier: Modifier = Modifier
) {
    val bgColor = if (stream.isActive) Color(0xFF1B5E20) else Color(0xFF37474F)
    val volumePercent = if (stream.maxVolume > 0)
        (stream.volumeLevel.toFloat() / stream.maxVolume * 100).toInt() else 0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stream.streamType,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (stream.isActive) "Vol: $volumePercent%" else "Silent",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DeviceNode(
    name: String,
    type: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = type,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = name.ifEmpty { "Device" },
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun StatsBar(
    streams: List<AudioStreamInfo>,
    devices: List<AudioDeviceInfo>
) {
    val activeCount = streams.count { it.isActive }
    val totalCount = streams.size
    val deviceCount = devices.size

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
            StatItem(value = "$activeCount", label = "Active Streams")
            StatItem(value = "${totalCount - activeCount}", label = "Silent Streams")
            StatItem(value = "$deviceCount", label = "Output Devices")
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}