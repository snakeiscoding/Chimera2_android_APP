package com.jht.widgets.numpad

import androidx.compose.runtime.Immutable

/**
 * Input rules for [NumPadEntryV2]. Set at the call site; controls what values are accepted.
 * Kept separate from [NumPadState] so display state and input config never collide.
 */
@Immutable
data class NumPadConfig(
    val displayType: NumPadDisplay = NumPadDisplay.Number,
    val decimalPlaces: Int = 0,
    val min: Float = 0f,
    val max: Float = NumPadDisplay.Number.getMax(),
    val forceStepValues: Boolean = false,
    val stepValue: Float = 1f,
    val displayZeroForNoInput: Boolean = true,
    val usePercentageBar: Boolean = false,
    val unitLabel: String = "",
)
