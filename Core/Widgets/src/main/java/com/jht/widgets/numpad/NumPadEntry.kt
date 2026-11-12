package com.jht.widgets.numpad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jht.ui.computeSize
import com.jht.widgets.numpad.internal.NumPadConfirmButton
import com.jht.widgets.numpad.internal.NumPadDeleteButton
import com.jht.widgets.numpad.internal.NumPadDisplayPanel
import com.jht.widgets.numpad.internal.NumPadNumberButton
import com.jht.widgets.numpad.internal.NumPadNumberRow

/**
 * Dumb base composable for the numpad. Renders [NumPadState] with no logic of its own.
 * All input processing belongs in [NumPadHandler]; this composable only fires callbacks.
 */
@Composable
fun NumPadEntry(
    state: NumPadState,
    modifier: Modifier = Modifier,
    config: NumPadConfig = NumPadConfig(),
    style: NumPadStyle = NumPadDefaults.style(),
    onDigitPressed: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    onLongDelete: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .size(width = 400.dp.computeSize(), height = 600.dp.computeSize())
            .background(style.background, style.containerShape),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(Modifier.padding(style.containerPadding)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                NumPadDisplayPanel(
                    state = state,
                    style = style,
                    config = config,
                )
                NumPadDeleteButton(
                    style = style,
                    onDelete = onDelete,
                    onLongDelete = onLongDelete,
                )
            }
            NumPadNumberRow(numbers = listOf("1", "2", "3"), style = style, onDigitPressed = onDigitPressed)
            NumPadNumberRow(numbers = listOf("4", "5", "6"), style = style, onDigitPressed = onDigitPressed)
            NumPadNumberRow(numbers = listOf("7", "8", "9"), style = style, onDigitPressed = onDigitPressed)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                NumPadNumberButton(number = "0", style = style, onDigitPressed = onDigitPressed)
                NumPadConfirmButton(
                    isEnabled = state.isConfirmEnabled,
                    style = style,
                    onConfirm = onConfirm,
                )
            }
        }
    }
}
