package com.meghna.audioanalyzer.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

// ─── Audio Routing Policy ─────────────────────────────────────────────────────
//
// Determines whether a stream should connect to a device based on
// Android's AudioPolicyManager routing rules.
//
// Uses TYPE-BASED matching (not ID-based) for reliable routing decisions.
//
// Routing rules:
// MUSIC      → BT A2DP > BT LE > USB Headset > Wired > Speaker
// VOICE_CALL → BT SCO > Wired Headset > Telephony > Earpiece (NEVER speaker)
// RING       → Speaker ONLY (must ring even with headset on)
// ALARM      → Speaker ONLY (Android safety policy)
// SYSTEM     → Speaker ONLY (UI feedback sounds)
// NOTIFICATION → Music target + Speaker (dual output)
// ──────────────────────────────────────────────────────────────────────────────

object AudioRoutingPolicy {

    fun shouldConnect(
        streamType: String,
        deviceType: String,
        availableTypes: Set<String>
    ): Boolean = when (streamType) {

        "MUSIC" -> deviceType == musicTarget(availableTypes)

        "NOTIFICATION" -> {
            val target = musicTarget(availableTypes)
            deviceType == target || deviceType == "Built-in Speaker"
        }

        "VOICE_CALL" -> {
            val target = when {
                "Bluetooth SCO"  in availableTypes -> "Bluetooth SCO"
                "Wired Headset"  in availableTypes -> "Wired Headset"
                "Telephony"      in availableTypes -> "Telephony"
                else                               -> "Earpiece"
            }
            deviceType == target
        }

        "RING", "ALARM", "SYSTEM" -> deviceType == "Built-in Speaker"

        else -> deviceType !in setOf("Earpiece", "Telephony", "Bus")
    }

    private fun musicTarget(availableTypes: Set<String>): String = when {
        "Bluetooth A2DP"   in availableTypes -> "Bluetooth A2DP"
        "Bluetooth LE"     in availableTypes -> "Bluetooth LE"
        "USB Headset"      in availableTypes -> "USB Headset"
        "USB Device"       in availableTypes -> "USB Device"
        "Wired Headphones" in availableTypes -> "Wired Headphones"
        "Wired Headset"    in availableTypes -> "Wired Headset"
        else                                 -> "Built-in Speaker"
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RoutingGraphScreen(viewModel: AudioViewModel) {
    val audioStreams by viewModel.audioStreams.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val outputDevices = connectedDevices.filter { it.isOutput }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Audio Routing Graph",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Smart routing based on Android audio policy",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

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

        RoutingGraph(
            streams = audioStreams,
            devices = outputDevices,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        StatsBar(streams = audioStreams, devices = outputDevices)

        Spacer(modifier = Modifier.height(12.dp))
        RoutingDecisionsCard(streams = audioStreams, devices = outputDevices)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── Graph ────────────────────────────────────────────────────────────────────

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
        val nodeHeightPx = with(density) { 58.dp.toPx() }

        val streamSpacing = if (streams.isNotEmpty()) boxHeight / streams.size else boxHeight
        val streamCenters = streams.mapIndexed { index, _ ->
            Offset(x = nodeWidthPx, y = streamSpacing * index + streamSpacing / 2f)
        }

        val deviceSpacing = if (displayDevices.isNotEmpty()) boxHeight / displayDevices.size else boxHeight
        val deviceCenters = displayDevices.mapIndexed { index, _ ->
            Offset(x = boxWidth - nodeWidthPx, y = deviceSpacing * index + deviceSpacing / 2f)
        }

        // Pre-compute connection matrix OUTSIDE Canvas using type-based matching
        val availableDeviceTypes = displayDevices.map { it.type }.toSet()
        val connectionMatrix: List<List<Boolean>> = streams.map { stream ->
            displayDevices.map { device ->
                AudioRoutingPolicy.shouldConnect(
                    streamType = stream.streamType,
                    deviceType = device.type,
                    availableTypes = availableDeviceTypes
                )
            }
        }

        // Draw lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            streams.forEachIndexed { streamIndex, stream ->
                val start = streamCenters.getOrNull(streamIndex) ?: return@forEachIndexed
                val connections = connectionMatrix.getOrNull(streamIndex) ?: return@forEachIndexed

                val color = if (stream.isActive) Color(0xFF4CAF50) else Color(0xFF546E7A)
                val alpha = if (stream.isActive) 0.8f else 0.2f
                val strokeWidth = if (stream.isActive) 3f else 1f

                displayDevices.forEachIndexed { deviceIndex, _ ->
                    if (connections.getOrNull(deviceIndex) != true) return@forEachIndexed

                    val end = deviceCenters.getOrNull(deviceIndex) ?: return@forEachIndexed
                    val midX = (start.x + end.x) / 2f

                    drawLine(color.copy(alpha = alpha), start, Offset(midX, start.y), strokeWidth)
                    drawLine(color.copy(alpha = alpha), Offset(midX, start.y), Offset(midX, end.y), strokeWidth)
                    drawLine(color.copy(alpha = alpha), Offset(midX, end.y), end, strokeWidth)
                }
            }
        }

        // Stream nodes LEFT
        streams.forEachIndexed { index, stream ->
            val centerY = streamCenters.getOrNull(index)?.y ?: return@forEachIndexed
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(58.dp)
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
                    .height(58.dp)
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

// ─── Routing Decisions Card ───────────────────────────────────────────────────

@Composable
fun RoutingDecisionsCard(
    streams: List<AudioStreamInfo>,
    devices: List<AudioDeviceInfo>
) {
    val availTypes = devices.map { it.type }.toSet()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Text(
                text = "Routing Decisions",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            streams.forEachIndexed { index, stream ->
                val targets = devices.filter { device ->
                    AudioRoutingPolicy.shouldConnect(
                        streamType = stream.streamType,
                        deviceType = device.type,
                        availableTypes = availTypes
                    )
                }.map { it.type }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Active indicator dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (stream.isActive) Color(0xFF4CAF50)
                                else Color(0xFF546E7A),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Stream name
                    Text(
                        text = stream.streamType,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (stream.isActive) Color.White else Color.Gray,
                        modifier = Modifier.width(115.dp)
                    )

                    // Arrow
                    Text(
                        text = "→",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    // Target device chips
                    if (targets.isEmpty()) {
                        Text(text = "No route", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            targets.forEach { target ->
                                DeviceChip(label = target)
                            }
                        }
                    }
                }

                if (index < streams.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.07f))
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceChip(label: String) {
    val chipColor = when (label) {
        "Built-in Speaker"                             -> Color(0xFF1565C0)
        "Earpiece"                                     -> Color(0xFF37474F)
        "Telephony"                                    -> Color(0xFF4E342E)
        "Bluetooth A2DP", "Bluetooth LE", "Bluetooth SCO" -> Color(0xFF1A237E)
        "Wired Headphones", "Wired Headset"            -> Color(0xFF4A148C)
        "USB Headset", "USB Device"                    -> Color(0xFF1B5E20)
        else                                           -> Color(0xFF455A64)
    }

    Box(
        modifier = Modifier
            .background(chipColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Nodes ────────────────────────────────────────────────────────────────────

@Composable
fun StreamNode(stream: AudioStreamInfo, modifier: Modifier = Modifier) {
    val bgColor = if (stream.isActive) Color(0xFF1B5E20) else Color(0xFF37474F)
    val volumePercent = if (stream.maxVolume > 0)
        (stream.volumeLevel.toFloat() / stream.maxVolume * 100).toInt() else 0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
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
fun DeviceNode(name: String, type: String, modifier: Modifier = Modifier) {
    val bgColor = when (type) {
        "Bluetooth A2DP", "Bluetooth SCO", "Bluetooth LE" -> Color(0xFF1A237E)
        "Wired Headphones", "Wired Headset"               -> Color(0xFF4A148C)
        "USB Headset", "USB Device"                       -> Color(0xFF1B5E20)
        "Earpiece"                                        -> Color(0xFF37474F)
        "Telephony"                                       -> Color(0xFF4E342E)
        else                                              -> Color(0xFF0D47A1)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = type, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = name.ifEmpty { "Device" }, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

// ─── Supporting Composables ───────────────────────────────────────────────────

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun StatsBar(streams: List<AudioStreamInfo>, devices: List<AudioDeviceInfo>) {
    val activeCount = streams.count { it.isActive }
    val deviceCount = devices.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(value = "$activeCount", label = "Active Streams")
            StatItem(value = "${streams.size - activeCount}", label = "Silent Streams")
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
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
    }
}