package com.meghna.audioanalyzer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_sessions")
data class AudioSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val streamType: String,
    val volumeLevel: Int,
    val deviceName: String,
    val timestamp: Long = System.currentTimeMillis()
)
