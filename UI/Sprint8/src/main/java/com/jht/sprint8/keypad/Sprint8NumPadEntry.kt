package com.jht.sprint8.keypad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jht.sprint8.KeypadType
import com.jht.sprint8.utils.Sprint8NumPadStyle
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.numpad.NumPadConfig
import com.jht.widgets.numpad.NumPadEntry

/**
 * Sprint8 feature composable for numpad input. Wraps [NumPadEntry] with a
 * [Sprint8NumPadHandler] and [Sprint8NumPadStyle], replacing the legacy [com.jht.widgets.NumPadEntry]
 * call sites in Sprint8Screen and Sprint8NewMainScreen.
 *
 * Decimal places are derived from [stepValue]: < 1.0 → 1 decimal place, else integer.
 * [KeypadType] is inferred from [stepValue] so callers don't need machine-type knowledge.
 */
@Composable
fun Sprint8NumPadEntry(
    numPadValue: Float,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    stepValue: Float = 1f,
    forceOnlyStepValues: Boolean = false,
    usePercentageBar: Boolean = false,
    unitLabel: String = "",
    enterBackground: Color = DynamicTheme.colors.primary,
    valueBar: Color = DynamicTheme.colors.primary,
    onDismiss: () -> Unit = {},
    onValueCommit: (Float) -> Unit = {},
) {
    val decimalPlaces = if (stepValue < 1f) 1 else 0
    val keypadType = if (stepValue < 1f) KeypadType.SPEED else KeypadType.RESISTANCE
    val config = NumPadConfig(
        decimalPlaces = decimalPlaces,
        min = min,
        max = max,
        stepValue = stepValue,
        forceStepValues = forceOnlyStepValues,
        usePercentageBar = usePercentageBar,
        unitLabel = unitLabel,
    )
    val scope = rememberCoroutineScope()
    val handler = remember(numPadValue, min, max, stepValue) {
        Sprint8NumPadHandler(
            scope = scope,
            config = config,
            initialValue = numPadValue,
            keypadType = keypadType,
            onDismiss = onDismiss,
        )
    }
    val state by handler.state.collectAsState()
    val style = Sprint8NumPadStyle.Sprint8NumPadPopUpDefaults.style(
        enterBackground = enterBackground,
        valueBar = valueBar,
    )
    NumPadEntry(
        state = state,
        modifier = modifier,
        config = config,
        style = style,
        onDigitPressed = handler::onDigit,
        onDelete = handler::onDelete,
        onLongDelete = handler::onLongDelete,
        onConfirm = {
            val committed = handler.currentFloat()
            onValueCommit(committed)
            handler.reset(committed)
        },
        onDismiss = onDismiss,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewSprint8NumPadEntrySpeed() {
    DynamicTheme {
        Sprint8NumPadEntry(
            numPadValue = 5.5f,
            min = 0.5f,
            max = 15f,
            stepValue = 0.5f,
            forceOnlyStepValues = true,
            usePercentageBar = true,
            unitLabel = "mph",
        )
    }
}

@AllDevicePreviews
@Composable
private fun PreviewSprint8NumPadEntryResistance() {
    DynamicTheme {
        Sprint8NumPadEntry(
            numPadValue = 8f,
            min = 1f,
            max = 20f,
            stepValue = 1f,
            forceOnlyStepValues = true,
            usePercentageBar = true,
        )
    }
}
