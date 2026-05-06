package com.meghna.audioanalyzer.data.model

data class AudioDeviceInfo(
    val id: Int,
    val name: String,
    val type: String,
    val isOutput: Boolean
)
