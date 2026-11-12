package com.jht.widgets.numpad

import androidx.compose.runtime.Immutable

/**
 * Rendering state for [NumPadEntryV2]. Produced by the handler; consumed by the composable.
 * The composable is dumb, renders whatever is in here without computing anything itself.
 */
@Immutable
data class NumPadState(
    val displayValue: String = "",
    val isAnimating: Boolean = false,
    val isConfirmEnabled: Boolean = true,
    val showPercentageBar: Boolean = false,
    val percentageFraction: Float = 0f,
    val unitLabel: String = "",
    /** Current numeric value, passed to [NumPadStyle] hook overrides for dynamic border/background/bar color. */
    val valueFloat: Float = 0f,
)
