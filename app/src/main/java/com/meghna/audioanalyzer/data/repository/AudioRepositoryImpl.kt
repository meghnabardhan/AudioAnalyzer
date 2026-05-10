package com.meghna.audioanalyzer.data.repository

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import com.meghna.audioanalyzer.data.model.AudioDeviceInfo
import com.meghna.audioanalyzer.data.model.AudioFocusInfo
import com.meghna.audioanalyzer.data.model.AudioStreamInfo
import com.meghna.audioanalyzer.data.model.FocusState
import com.meghna.audioanalyzer.data.model.FocusType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AudioRepositoryImpl @Inject constructor(
    private val audioManager: AudioManager,
    @ApplicationContext private val context: Context
) : AudioRepository {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val FFT_SIZE = 1024
        private const val NUM_BANDS = 32
    }

    // ─── Audio Focus ───────────────────────────────────────────────

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
            } else false
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

    // ─── Streams & Devices ─────────────────────────────────────────

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
        18 -> "Telephony"        // TYPE_TELEPHONY — voice call audio path
        21 -> "Bus"              // TYPE_BUS — internal audio bus, ignore in routing
        24 -> "Built-in Speaker" // TYPE_BUILTIN_SPEAKER_SAFE — treat as speaker
        26 -> "Bluetooth LE"     // TYPE_BLE_HEADSET (API 31+)
        27 -> "Bluetooth LE"     // TYPE_BLE_SPEAKER (API 31+)
        else -> "Internal Audio Path"
    }
}

    // ─── FFT ───────────────────────────────────────────────────────

    override fun observeFftData(): Flow<FloatArray> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuffer, FFT_SIZE * 2)

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        try {
            audioRecord.startRecording()
            val buffer = ShortArray(FFT_SIZE)

            while (currentCoroutineContext().isActive) {
                val read = audioRecord.read(buffer, 0, FFT_SIZE)
                if (read > 0) {
                    val bands = computeBands(buffer, read)
                    emit(bands)
                }
                delay(50L) // ~20fps
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)

    private fun computeBands(samples: ShortArray, count: Int): FloatArray {
        val real = FloatArray(FFT_SIZE)
        val imag = FloatArray(FFT_SIZE)

        // Apply Hann window
        for (i in 0 until minOf(count, FFT_SIZE)) {
            val window = 0.5f * (1f - cos(2.0 * PI * i / (FFT_SIZE - 1)).toFloat())
            real[i] = (samples[i].toFloat() / Short.MAX_VALUE) * window
        }

        fft(real, imag)

        // Group into bands
        val bands = FloatArray(NUM_BANDS)
        val binsPerBand = (FFT_SIZE / 2) / NUM_BANDS

        for (band in 0 until NUM_BANDS) {
            var maxMag = 0f
            for (bin in 0 until binsPerBand) {
                val idx = band * binsPerBand + bin
                val mag = sqrt(
                    (real[idx] * real[idx] + imag[idx] * imag[idx]).toDouble()
                ).toFloat()
                if (mag > maxMag) maxMag = mag
            }
            bands[band] = maxMag
        }

        // Normalize
        val maxVal = bands.maxOrNull() ?: 1f
        if (maxVal > 0f) {
            for (i in bands.indices) bands[i] = (bands[i] / maxVal).coerceIn(0f, 1f)
        }

        return bands
    }

    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var tmp = real[i]; real[i] = real[j]; real[j] = tmp
                tmp = imag[i]; imag[i] = imag[j]; imag[j] = tmp
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wReal = cos(ang).toFloat()
            val wImag = sin(ang).toFloat()
            var i = 0
            while (i < n) {
                var curReal = 1f
                var curImag = 0f
                for (jj in 0 until len / 2) {
                    val uReal = real[i + jj]
                    val uImag = imag[i + jj]
                    val vReal = real[i + jj + len / 2] * curReal - imag[i + jj + len / 2] * curImag
                    val vImag = real[i + jj + len / 2] * curImag + imag[i + jj + len / 2] * curReal
                    real[i + jj] = uReal + vReal
                    imag[i + jj] = uImag + vImag
                    real[i + jj + len / 2] = uReal - vReal
                    imag[i + jj + len / 2] = uImag - vImag
                    val newCurReal = curReal * wReal - curImag * wImag
                    curImag = curReal * wImag + curImag * wReal
                    curReal = newCurReal
                }
                i += len
            }
            len = len shl 1
        }
    }
}