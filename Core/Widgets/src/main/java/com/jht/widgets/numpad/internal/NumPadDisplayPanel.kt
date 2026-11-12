package com.jht.widgets.numpad.internal

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.ui.computeSize
import com.jht.widgets.numpad.NumPadConfig
import com.jht.widgets.numpad.NumPadDisplay
import com.jht.widgets.numpad.NumPadState
import com.jht.widgets.numpad.NumPadStyle

@Composable
internal fun RowScope.EnteredTextPanel(
    userEnteredFloat: Float,
    decimalPlaces: Int,
    min: Float,
    max: Float,
    blockUserEntryForAnimation: Boolean,
    displayType: NumPadDisplay,
    useHours: Boolean,
    usePercentageBar: Boolean,
    unitLabel: String = "",
    style: NumPadStyle,
    ableToSubmitValue: Boolean,
) {
    val borderColor = style.borderColor(
        userEnteredFloat = userEnteredFloat,
        ableToSubmitValue = ableToSubmitValue,
        enterTextPanelBorderColorDefault = style.enterBackground
    )
    val backgroundColor = style.backgroundColor(
        userEnteredFloat = userEnteredFloat,
        ableToSubmitValue = ableToSubmitValue,
    )
    val percentageBarColor = style.percentageBarColor(
        ableToSubmitValue = ableToSubmitValue,
        percentageBarColorDefault = style.valueBar
    )

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val flashAnimation by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = ANIMATION_TIME
                0f at ANIMATION_TIME / 2 using FastOutSlowInEasing
                1f at ANIMATION_TIME using FastOutSlowInEasing
            },
        ), label = "numpad_autocorrect_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp.computeSize())
            .then(
                Modifier.border(
                    width = 1.dp.computeSize(),
                    color = borderColor,
                    shape = RoundedCornerShape(6.dp.computeSize())
                )
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(style.valueTextPanelStroke)
            )
            .weight(2f),
        contentAlignment = Alignment.Center
    ) {
        if (usePercentageBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(style.percentageBarWidth)
                    .padding(1.dp.computeSize())
                    .fillMaxHeight((userEnteredFloat - min) / (max - min))
                    .background(percentageBarColor)
            )
            Text(
                text = getDisplayStringFromFloat(min, displayType, useHours, decimalPlaces),
                style = style.valueTextStyle,
                modifier = Modifier
                    .testTag("NumPadMinValue")
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp.computeSize()),
                fontSize = 15.sp.computeSize()
            )
            Text(
                text = getDisplayStringFromFloat(max, displayType, useHours, decimalPlaces),
                style = style.valueTextStyle,
                modifier = Modifier
                    .testTag("NumPadMaxValue")
                    .align(Alignment.TopStart)
                    .padding(start = 18.dp.computeSize()),
                fontSize = 15.sp.computeSize()
            )
        }
        Text(
            text = unitLabel,
            style = style.valueTextStyle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 15.dp.computeSize(), bottom = 5.dp.computeSize()),
            fontSize = 18.sp.computeSize()
        )
        BasicTextField(
            value = getDisplayStringFromFloat(userEnteredFloat, displayType, useHours, decimalPlaces),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            visualTransformation = if (displayType == NumPadDisplay.Passcode) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = style.valueTextStyle.copy(
                fontSize = style.valueTextStyle.fontSize,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .testTag("NumPadValue")
                .graphicsLayer { alpha = if (blockUserEntryForAnimation) flashAnimation else 1f }
        )
    }
}

internal fun getDisplayStringFromFloat(
    floatToDisplay: Float,
    displayType: NumPadDisplay,
    useHours: Boolean,
    decimalPlaces: Int,
): String {
    if (floatToDisplay < 0) return ""
    return displayType.format(floatToDisplay, decimalPlaces, useHours)
}

/** State-driven display panel for [com.jht.widgets.numpad.NumPadEntryV2]. Renders [NumPadState] with no logic. */
@Composable
internal fun RowScope.NumPadDisplayPanel(
    state: NumPadState,
    style: NumPadStyle,
    config: NumPadConfig,
) {
    val useHours = config.displayType.needHours(config.max)
    val borderColor = style.borderColor(
        userEnteredFloat = state.valueFloat,
        ableToSubmitValue = state.isConfirmEnabled,
        enterTextPanelBorderColorDefault = style.enterBackground,
    )
    val backgroundColor = style.backgroundColor(
        userEnteredFloat = state.valueFloat,
        ableToSubmitValue = state.isConfirmEnabled,
    )
    val percentageBarColor = style.percentageBarColor(
        ableToSubmitValue = state.isConfirmEnabled,
        percentageBarColorDefault = style.valueBar,
    )

    val infiniteTransition = rememberInfiniteTransition(label = "numpad_autocorrect")
    val flashAnimation by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = ANIMATION_TIME
                0f at ANIMATION_TIME / 2 using FastOutSlowInEasing
                1f at ANIMATION_TIME using FastOutSlowInEasing
            },
        ), label = "numpad_autocorrect_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp.computeSize())
            .border(
                width = 1.dp.computeSize(),
                color = borderColor,
                shape = RoundedCornerShape(6.dp.computeSize()),
            )
            .background(backgroundColor, style.buttonShape)
            .weight(2f),
        contentAlignment = Alignment.Center,
    ) {
        if (state.showPercentageBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(style.percentageBarWidth)
                    .padding(1.dp.computeSize())
                    .fillMaxHeight(state.percentageFraction)
                    .background(percentageBarColor)
            )
            Text(
                text = getDisplayStringFromFloat(config.min, config.displayType, useHours, config.decimalPlaces),
                style = style.valueTextStyle,
                modifier = Modifier
                    .testTag("NumPadMinValue")
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp.computeSize()),
                fontSize = 15.sp.computeSize(),
            )
            Text(
                text = getDisplayStringFromFloat(config.max, config.displayType, useHours, config.decimalPlaces),
                style = style.valueTextStyle,
                modifier = Modifier
                    .testTag("NumPadMaxValue")
                    .align(Alignment.TopStart)
                    .padding(start = 18.dp.computeSize()),
                fontSize = 15.sp.computeSize(),
            )
        }
        Text(
            text = state.unitLabel,
            style = style.valueTextStyle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 15.dp.computeSize(), bottom = 5.dp.computeSize()),
            fontSize = 18.sp.computeSize(),
        )
        BasicTextField(
            value = state.displayValue,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            visualTransformation = if (config.displayType == NumPadDisplay.Passcode) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            textStyle = style.valueTextStyle.copy(
                fontSize = style.valueTextStyle.fontSize,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .testTag("NumPadValue")
                .graphicsLayer { alpha = if (state.isAnimating) flashAnimation else 1f },
        )
    }
}
