package com.meghna.audioanalyzer.data.repository

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.meghna.audioanalyzer.data.model.AudioDeviceInfo
import com.meghna.audioanalyzer.data.model.AudioFocusInfo
import com.meghna.audioanalyzer.data.model.AudioStreamInfo
import com.meghna.audioanalyzer.data.model.FocusState
import com.meghna.audioanalyzer.data.model.FocusType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class AudioRepositoryImpl @Inject constructor(
    private val audioManager: AudioManager,
    @ApplicationContext private val context: Context
) : AudioRepository {

    private val _focusState = MutableStateFlow(
        AudioFocusInfo(focusState = FocusState.NONE, focusType = FocusType.NONE)
    )

    private var focusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        val info = when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN ->
                AudioFocusInfo(FocusState.GAIN, FocusType.GAIN)
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT ->
                AudioFocusInfo(FocusState.GAIN, FocusType.GAIN_TRANSIENT)
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK ->
                AudioFocusInfo(FocusState.GAIN, FocusType.GAIN_TRANSIENT_MAY_DUCK)
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE ->
                AudioFocusInfo(FocusState.GAIN, FocusType.GAIN_TRANSIENT_EXCLUSIVE)
            AudioManager.AUDIOFOCUS_LOSS ->
                AudioFocusInfo(FocusState.LOSS, FocusType.LOSS)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                AudioFocusInfo(FocusState.LOSS, FocusType.LOSS_TRANSIENT)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                AudioFocusInfo(FocusState.LOSS, FocusType.LOSS_TRANSIENT_CAN_DUCK)
            else ->
                AudioFocusInfo(FocusState.NONE, FocusType.NONE)
        }
        _focusState.value = info
    }

    override fun observeAudioFocus(): Flow<AudioFocusInfo> = _focusState.asStateFlow()

    override fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            val result = audioManager.requestAudioFocus(focusRequest!!)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                _focusState.value = AudioFocusInfo(FocusState.GAIN, FocusType.GAIN)
                true
            } else {
                false
            }
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    override fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
        _focusState.value = AudioFocusInfo(FocusState.NONE, FocusType.NONE)
    }

    override fun getActiveStreams(): List<AudioStreamInfo> {
        val streams = listOf(
            Pair(AudioManager.STREAM_MUSIC, "MUSIC"),
            Pair(AudioManager.STREAM_ALARM, "ALARM"),
            Pair(AudioManager.STREAM_NOTIFICATION, "NOTIFICATION"),
            Pair(AudioManager.STREAM_VOICE_CALL, "VOICE_CALL"),
            Pair(AudioManager.STREAM_RING, "RING"),
            Pair(AudioManager.STREAM_SYSTEM, "SYSTEM")
        )

        return streams.map { (streamType, name) ->
            val volume = audioManager.getStreamVolume(streamType)
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            AudioStreamInfo(
                streamType = name,
                volumeLevel = volume,
                maxVolume = maxVolume,
                isActive = volume > 0
            )
        }
    }

    override fun getConnectedDevices(): List<AudioDeviceInfo> {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
        return devices.map { device ->
            AudioDeviceInfo(
                id = device.id,
                name = device.productName.toString(),
                type = getDeviceTypeName(device.type),
                isOutput = device.isSink
            )
        }
    }

    private fun getDeviceTypeName(type: Int): String {
        return when (type) {
            android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in Speaker"
            android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
            android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
            android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
            android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
            android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
            android.media.AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
            android.media.AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
            else -> "Unknown Device"
        }
    }
}