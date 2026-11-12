package com.jht.sprint8.keypad

import com.jht.sprint8.KeypadType
import com.jht.widgets.numpad.NumPadConfig
import com.jht.widgets.numpad.NumPadHandler
import kotlinx.coroutines.CoroutineScope

/**
 * Sprint8-specific numpad handler. Extends [NumPadHandler] with [KeypadType] awareness,
 * enabling per-type unit tests and future Sprint8-specific overrides.
 *
 * Decimal places are driven by [NumPadConfig.decimalPlaces]:
 * - SPEED and INCLINE use 1 decimal place (step < 1.0)
 * - RESISTANCE uses 0 decimal places (step >= 1.0)
 */
open class Sprint8NumPadHandler(
    scope: CoroutineScope,
    config: NumPadConfig,
    initialValue: Float = 0f,
    val keypadType: KeypadType,
    onDismiss: () -> Unit = {},
) : NumPadHandler(scope, config, initialValue, onDismiss)
