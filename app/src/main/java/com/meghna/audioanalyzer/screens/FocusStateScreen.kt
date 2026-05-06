package com.meghna.audioanalyzer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meghna.audioanalyzer.AudioViewModel
import com.meghna.audioanalyzer.data.model.AudioFocusInfo
import com.meghna.audioanalyzer.data.model.FocusState
import com.meghna.audioanalyzer.data.model.FocusType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FocusStateScreen(viewModel: AudioViewModel) {
    val focusInfo by viewModel.focusInfo.collectAsState()
    val focusHistory by viewModel.focusHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Audio Focus Monitor",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Current Focus Card
        CurrentFocusCard(focusInfo = focusInfo)

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.requestFocus() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Request Focus")
            }
            Button(
                onClick = { viewModel.abandonFocus() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Text("Abandon Focus")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Focus History
        Text(
            text = "Focus History",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (focusHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No focus events yet.\nTry requesting focus or playing audio.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(focusHistory) { info ->
                    FocusHistoryItem(info = info)
                }
            }
        }
    }
}

@Composable
fun CurrentFocusCard(focusInfo: AudioFocusInfo) {
    val bgColor = when (focusInfo.focusState) {
        FocusState.GAIN -> Color(0xFF1B5E20)
        FocusState.LOSS -> Color(0xFFB71C1C)
        FocusState.NONE -> Color(0xFF37474F)
    }

    val stateLabel = when (focusInfo.focusState) {
        FocusState.GAIN -> "🟢 FOCUS GAINED"
        FocusState.LOSS -> "🔴 FOCUS LOST"
        FocusState.NONE -> "⚪ NO FOCUS"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stateLabel,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Type: ${focusInfo.focusType.name.replace("_", " ")}",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = focusTypeDescription(focusInfo.focusType),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun FocusHistoryItem(info: AudioFocusInfo) {
    val borderColor = when (info.focusState) {
        FocusState.GAIN -> Color(0xFF4CAF50)
        FocusState.LOSS -> Color(0xFFF44336)
        FocusState.NONE -> Color(0xFF90A4AE)
    }

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeString = timeFormat.format(Date(info.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(borderColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.focusType.name.replace("_", " "),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = focusTypeDescription(info.focusType),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = timeString,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

fun focusTypeDescription(type: FocusType): String {
    return when (type) {
        FocusType.GAIN -> "Long-term focus (e.g. music player)"
        FocusType.GAIN_TRANSIENT -> "Brief focus (e.g. navigation)"
        FocusType.GAIN_TRANSIENT_MAY_DUCK -> "Brief, others may duck volume"
        FocusType.GAIN_TRANSIENT_EXCLUSIVE -> "Exclusive focus, no ducking"
        FocusType.LOSS -> "Permanent focus loss — stop playback"
        FocusType.LOSS_TRANSIENT -> "Temporary loss — pause playback"
        FocusType.LOSS_TRANSIENT_CAN_DUCK -> "Lower volume, keep playing"
        FocusType.NONE -> "No audio focus held"
    }
}