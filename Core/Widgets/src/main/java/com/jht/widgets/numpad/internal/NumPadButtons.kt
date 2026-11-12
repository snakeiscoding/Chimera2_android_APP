package com.jht.widgets.numpad.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jht.ui.computeSize
import com.jht.widgets.numpad.NumPadStyle
import com.jht.widgets.R

@Composable
internal fun ColumnScope.NumberRow(
    numbers: List<String>,
    isBlocked: Boolean,
    style: NumPadStyle,
    onDigit: (String) -> Unit,
) {
    require(numbers.size >= 3) { "NumberRow requires at least 3 numbers" }
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
    ) {
        numbers.take(3).forEach { number ->
            NumberButton(
                number = number,
                isBlocked = isBlocked,
                style = style,
                onClick = { onDigit(number) },
            )
        }
    }
}

@Composable
internal fun RowScope.NumberButton(
    number: String,
    isBlocked: Boolean,
    style: NumPadStyle,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag("NumPad$number")
            .fillMaxSize()
            .padding(2.dp.computeSize())
            .weight(1f)
            .background(
                color = style.buttonBackground,
                shape = RoundedCornerShape(style.numberButtonStroke)
            )
            .clickable(onClick = { if (!isBlocked) onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Text(text = number, style = style.buttonTextStyle)
    }
}

@Composable
internal fun RowScope.EnterButton(
    isEnabled: Boolean,
    style: NumPadStyle,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag("NumPadEnterButton")
            .fillMaxSize()
            .padding(2.dp.computeSize())
            .background(
                if (!isEnabled) style.enterDisabledBackground else style.enterBackground,
                shape = RoundedCornerShape(style.enterButtonStroke)
            )
            .clickable(onClick = { if (isEnabled) onClick() })
            .weight(2f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_checkmark),
            contentDescription = "enterText",
            tint = if (!isEnabled) style.enterDisabledForeground else style.enterForeground,
            modifier = Modifier.size(40.dp.computeSize())
        )
    }
}

@Composable
internal fun RowScope.DeleteButton(
    style: NumPadStyle,
    onDelete: () -> Unit,
    onClear: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag("NumPadDeleteButton")
            .fillMaxSize()
            .padding(2.dp.computeSize())
            .background(
                color = style.deleteButtonBackground,
                shape = RoundedCornerShape(style.deleteButtonStroke)
            )
            .combinedClickable(
                onClick = onDelete,
                onLongClick = onClear,
            )
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = style.deleteIcon),
            contentDescription = "deleteButton",
            tint = style.valueTextStyle.color,
            modifier = Modifier.size(40.dp.computeSize())
        )
    }
}

// Dumb sub-composables for numpad/NumPadEntry.kt, no blocking logic, no timers.
// All input gating is handled by NumPadHandler; these composables only fire callbacks.

@Composable
internal fun ColumnScope.NumPadNumberRow(
    numbers: List<String>,
    style: NumPadStyle,
    onDigitPressed: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
    ) {
        numbers.forEach { digit ->
            NumPadNumberButton(number = digit, style = style, onDigitPressed = onDigitPressed)
        }
    }
}

@Composable
internal fun RowScope.NumPadNumberButton(
    number: String,
    style: NumPadStyle,
    onDigitPressed: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag("NumPad$number")
            .fillMaxSize()
            .padding(2.dp.computeSize())
            .weight(1f)
            .background(style.buttonBackground, style.buttonShape)
            .border(style.buttonBorderStroke, style.buttonShape)
            .clickable { onDigitPressed(number) },
        contentAlignment = Alignment.Center
    ) {
        Text(text = number, style = style.buttonTextStyle)
    }
}

@Composable
internal fun RowScope.NumPadConfirmButton(
    isEnabled: Boolean,
    style: NumPadStyle,
    onConfirm: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag("NumPadEnterButton")
            .fillMaxSize()
            .padding(2.dp.computeSize())
            .background(
                if (isEnabled) style.enterBackground else style.enterDisabledBackground,
                style.buttonShape,
            )
            .border(style.buttonBorderStroke, style.buttonShape)
            .clickable(enabled = isEnabled, onClick = onConfirm)
            .weight(2f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_checkmark),
            contentDescription = "confirm",
            tint = if (isEnabled) style.enterForeground else style.enterDisabledForeground,
            modifier = Modifier.size(40.dp.computeSize())
        )
    }
}

@Composable
internal fun RowScope.NumPadDeleteButton(
    style: NumPadStyle,
    onDelete: () -> Unit,
    onLongDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag("NumPadDeleteButton")
            .fillMaxSize()
            .padding(2.dp.computeSize())
            .background(style.deleteButtonBackground, style.buttonShape)
            .border(style.buttonBorderStroke, style.buttonShape)
            .combinedClickable(onClick = onDelete, onLongClick = onLongDelete)
            .weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = style.deleteIcon),
            contentDescription = "delete",
            tint = style.valueTextStyle.color,
            modifier = Modifier.size(40.dp.computeSize())
        )
    }
}
