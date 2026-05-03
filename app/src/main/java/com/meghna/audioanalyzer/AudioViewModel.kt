package com.meghna.audioanalyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioViewModel : ViewModel() {

    // Private mutable state - only ViewModel can change this
    private val _secondsElapsed = MutableStateFlow(0)

    // Public read-only state - UI can only read this
    val secondsElapsed: StateFlow<Int> = _secondsElapsed.asStateFlow()

    init {
        // This runs automatically when ViewModel is created
        startCounting()
    }

    private fun startCounting() {
        viewModelScope.launch {
            while (true) {
                delay(1000) // wait 1 second
                _secondsElapsed.value += 1 // increment counter
            }
        }
    }
}