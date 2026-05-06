package com.meghna.audioanalyzer.data.model

data class AudioStreamInfo(
    val streamType: String,
    val volumeLevel: Int,
    val maxVolume: Int,
    val isActive: Boolean
)
