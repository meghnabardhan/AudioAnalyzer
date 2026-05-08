package com.meghna.audioanalyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meghna.audioanalyzer.data.model.AudioFocusInfo
import com.meghna.audioanalyzer.data.model.AudioStreamInfo
import com.meghna.audioanalyzer.data.model.AudioDeviceInfo
import com.meghna.audioanalyzer.data.model.FocusState
import com.meghna.audioanalyzer.data.model.FocusType
import com.meghna.audioanalyzer.data.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val audioRepository: AudioRepository
) : ViewModel() {

    // ─── Streams ───────────────────────────────────────────────────
    private val _audioStreams = MutableStateFlow<List<AudioStreamInfo>>(emptyList())
    val audioStreams: StateFlow<List<AudioStreamInfo>> = _audioStreams.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<AudioDeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<AudioDeviceInfo>> = _connectedDevices.asStateFlow()

    // ─── Focus ─────────────────────────────────────────────────────
    private val _focusInfo = MutableStateFlow(
        AudioFocusInfo(focusState = FocusState.NONE, focusType = FocusType.NONE)
    )
    val focusInfo: StateFlow<AudioFocusInfo> = _focusInfo.asStateFlow()

    private val _focusHistory = MutableStateFlow<List<AudioFocusInfo>>(emptyList())
    val focusHistory: StateFlow<List<AudioFocusInfo>> = _focusHistory.asStateFlow()

    // ─── FFT ───────────────────────────────────────────────────────
    private val _fftBands = MutableStateFlow(FloatArray(32))
    val fftBands: StateFlow<FloatArray> = _fftBands.asStateFlow()

    private var fftJob: Job? = null

    init {
        startMonitoring()
        observeFocus()
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                _audioStreams.value = audioRepository.getActiveStreams()
                _connectedDevices.value = audioRepository.getConnectedDevices()
                delay(1000)
            }
        }
    }

    private fun observeFocus() {
        viewModelScope.launch {
            audioRepository.observeAudioFocus().collect { info ->
                _focusInfo.value = info
                val current = _focusHistory.value.toMutableList()
                current.add(0, info)
                _focusHistory.value = current.take(10)
            }
        }
    }

    fun requestFocus() { audioRepository.requestAudioFocus() }

    fun abandonFocus() { audioRepository.abandonAudioFocus() }

    fun startFftCapture() {
        if (fftJob?.isActive == true) return
        fftJob = viewModelScope.launch {
            audioRepository.observeFftData().collect { bands ->
                _fftBands.value = bands
            }
        }
    }

    fun stopFftCapture() {
        fftJob?.cancel()
        fftJob = null
        _fftBands.value = FloatArray(32)
    }

    override fun onCleared() {
        super.onCleared()
        audioRepository.abandonAudioFocus()
        stopFftCapture()
    }
}