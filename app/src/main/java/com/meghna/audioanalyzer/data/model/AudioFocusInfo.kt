package com.meghna.audioanalyzer.data.model

data class AudioFocusInfo(
    val focusState: FocusState,
    val focusType: FocusType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class FocusState {
    GAIN, LOSS, NONE
}

enum class FocusType {
    GAIN,
    GAIN_TRANSIENT,
    GAIN_TRANSIENT_MAY_DUCK,
    GAIN_TRANSIENT_EXCLUSIVE,
    LOSS,
    LOSS_TRANSIENT,
    LOSS_TRANSIENT_CAN_DUCK,
    NONE
}