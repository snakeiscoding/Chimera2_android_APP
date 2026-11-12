package com.jht.switchStride.model

data class SwitchStrideUIState(
    val isBeltInReverse: Boolean = false,
    val hasWorkoutStarted: Boolean = false,
    val isWorkoutPaused: Boolean = false,
    val pauseTimer: String= "",
    val showReverseBeltDialog: Boolean = false,
    val isBeltStopping: Boolean = false
)
