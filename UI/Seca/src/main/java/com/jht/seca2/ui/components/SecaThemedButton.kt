package com.jht.seca2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.seca.R
import com.jht.seca2.secaUiTheme
import com.jht.seca2.ui.SecaColors
import com.jht.theme.DynamicTheme
import com.jht.ui.clickableNoEffect
import com.jht.widgets.AutoSizeText

/**
 * Full red button widget for seca
 */
@Composable
internal fun SecaButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    width: Dp? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = ButtonColors(
        SecaColors.primaryRed,
        SecaColors.primaryRed,
        SecaColors.white
    )

    val pressColors = ButtonColors(
        SecaColors.white,
        SecaColors.primaryRed,
        SecaColors.primaryRed
    )

    val disabledColors = ButtonColors(
        SecaColors.primaryRed.copy(alpha = 0.3f),
        SecaColors.primaryRed.copy(alpha = 0.3f),
        SecaColors.white.copy(alpha = 0.3f),
    )

    SecaThemedButton(
        modifier = modifier,
        buttonColors = colors,
        buttonOnPressColors = pressColors,
        buttonDisabledColors = disabledColors,
        text = text,
        height = height ?: dimensionResource(R.dimen.seca_button_height),
        width = width ?: dimensionResource(R.dimen.seca_button_width),
        enabled = enabled,
        onClick = onClick
    )
}

/**
 * Outlined button widget for seca
 */
@Composable
internal fun SecaOutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    width: Dp? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = ButtonColors(
        SecaColors.neutralGray0,
        SecaColors.primaryRed,
        SecaColors.primaryRed
    )

    val pressColors = ButtonColors(
        SecaColors.primaryRed,
        SecaColors.primaryRed,
        SecaColors.white
    )

    val disabledColors = ButtonColors(
        SecaColors.neutralGray0.copy(alpha = 0.3f),
        SecaColors.primaryRed.copy(alpha = 0.3f),
        SecaColors.primaryRed.copy(alpha = 0.3f),
    )

    SecaThemedButton(
        modifier = modifier,
        buttonColors = colors,
        buttonOnPressColors = pressColors,
        buttonDisabledColors = disabledColors,
        text = text,
        height = height ?: dimensionResource(R.dimen.seca_button_height),
        width = width ?: dimensionResource(R.dimen.seca_button_width),
        enabled = enabled,
        onClick = onClick
    )
}

/**
 * Blank background button, same size as other buttons
 */
@Composable
internal fun SecaBlankButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp? = null,
    width: Dp? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = ButtonColors(
        SecaColors.transparent,
        SecaColors.transparent,
        SecaColors.neutralGray15
    )

    val pressColors = ButtonColors(
        SecaColors.transparent,
        SecaColors.transparent,
        SecaColors.neutralGray15.copy(alpha = 0.5f)
    )

    val disabledColors = ButtonColors(
        SecaColors.transparent,
        SecaColors.transparent,
        SecaColors.neutralGray15.copy(alpha = 0.3f),
    )

    SecaThemedButton(
        modifier = modifier,
        buttonColors = colors,
        buttonOnPressColors = pressColors,
        buttonDisabledColors = disabledColors,
        text = text,
        height = height ?: dimensionResource(R.dimen.seca_button_height),
        width = width ?: dimensionResource(R.dimen.seca_button_width),
        enabled = enabled,
        onClick = onClick
    )
}

private data class ButtonColors(
    val backgroundColor: Color,
    val borderColor: Color,
    val textColor: Color
)

@Composable
private fun SecaThemedButton(
    text: String,
    buttonColors: ButtonColors,
    buttonOnPressColors: ButtonColors,
    buttonDisabledColors: ButtonColors,
    height: Dp,
    width: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
) {
    val isPressed by interactionSource.collectIsPressedAsState()

    val intendedColor = if (!enabled) {
        buttonDisabledColors
    } else if (isPressed) {
        buttonOnPressColors
    } else {
        buttonColors
    }

    val bgColor by animateColorAsState(
        targetValue = intendedColor.backgroundColor,
        label = "bg color"
    )

    val borderColor by animateColorAsState(
        targetValue = intendedColor.borderColor,
        label = "border color"
    )

    val textColor by animateColorAsState(
        targetValue = intendedColor.textColor,
        label = "text color"
    )

    Box(
        modifier = modifier
            .size(width, height)
            .clickableNoEffect(
                interactionSource = interactionSource,
                enabled = enabled,
                onClick = onClick
            )
    ) {

        val cornerRadius: Dp = 10.dp
        val strokeWidth: Dp = 2.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val radiusPx = cornerRadius.toPx()
            val strokePx = strokeWidth.toPx()

            drawRoundRect(
                color = bgColor,
                size = size,
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )

            drawRoundRect(
                color = borderColor,
                size = size,
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = strokePx)
            )
        }

        AutoSizeText(
            color = textColor,
            text = text,
            style = MaterialTheme.typography.titleSmall,
            maxFontSize = MaterialTheme.typography.titleSmall.fontSize,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

//region PREVIEWS
@Preview(widthDp = 200, heightDp = 60)
@Composable
private fun SecaButtonPreview() {
    DynamicTheme(secaUiTheme) {
        SecaButton(
            text = stringResource(R.string.accept_tc),
            onClick = {}
        )
    }
}

@Preview(widthDp = 200, heightDp = 60)
@Composable
private fun SecaOutlineButtonPreview() {
    DynamicTheme(secaUiTheme) {
        SecaOutlineButton(
            text = "Testing outline!",
            onClick = {}
        )
    }
}

@Preview(widthDp = 200, heightDp = 60)
@Composable
private fun SecaOutlineButtonDisabledPreview() {
    DynamicTheme(secaUiTheme) {
        SecaOutlineButton(
            text = "Testing outline!",
            enabled = false,
            onClick = {}
        )
    }
}