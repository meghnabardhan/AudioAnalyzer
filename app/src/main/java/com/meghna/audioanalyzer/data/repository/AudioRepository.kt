package com.meghna.audioanalyzer.data.repository

import com.meghna.audioanalyzer.data.model.AudioStreamInfo
import com.meghna.audioanalyzer.data.model.AudioDeviceInfo
import com.meghna.audioanalyzer.data.model.AudioFocusInfo
import kotlinx.coroutines.flow.Flow

interface AudioRepository {
    fun getActiveStreams(): List<AudioStreamInfo>
    fun getConnectedDevices(): List<AudioDeviceInfo>
    fun observeAudioFocus(): Flow<AudioFocusInfo>
    fun requestAudioFocus(): Boolean
    fun abandonAudioFocus()
    fun observeFftData(): Flow<FloatArray>
}