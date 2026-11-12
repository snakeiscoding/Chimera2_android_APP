package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.BORDER_GRAY_2
import com.jht.sprint8.utils.Sprint8Colors.ICON_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_ICON_BORDER_COLOR
import com.jht.ui.AllDevicePreviews
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Displays a pillbox-style button for the Sprint 8, showing a title,
 * value, and unit, with increment/decrement arrows for controlling
 * speed or resistance.
 * @param modifier Modifier applied to the button for layout, styling, and interaction.
 * @param screenWidth Screen width used for responsive layout calculations.
 * @param screenHeight Screen height used for responsive layout calculations.
 * @param title Title text displayed above the value.
 * @param value Main value displayed in the pillbox.
 * @param unit Unit text displayed alongside the value (e.g., "BPM", "RPM").
 * @param isSpeedControl Boolean indicating whether the pillbox controls speed.
 * @param onArrowUpClick Optional callback invoked when the up arrow is clicked.
 * @param onArrowDownClick Optional callback invoked when the down arrow is clicked.
 * @param onClick Callback invoked when the pillbox itself is clicked.
 * @param componentTag Optional test tag for UI testing. Defaults to "Sprint8PillBox".
 * @param dataSectionColor Background color of the data display section.
 * @param buttonSectionColor Background color of the button section (e.g., arrows).
 * @param valueColor Color of the value text.
 * @param titleColor Color of the title text.
 * @param arrowIconColor Color of the up/down arrow icons.
 * @param dotIconColor Color of any dot or indicator icon.
 * @param borderColor Color of the pillbox border.
 * @param titleSize Text size of the title. Defaults to 20.sp.
 * @param valueSize Text size of the main value. Defaults to 48.sp.
 * @param cornerRadius Corner radius of the pillbox container. Defaults to 12.
 * @param imageAlignment Alignment for any icon or image inside the pillbox. Defaults to top-end.
 * @param borderThickness Thickness of the pillbox border. Defaults to 1.dp.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8PillboxButton(
    modifier: Modifier = Modifier,
    screenWidth: Dp,
    screenHeight: Dp,
    title: String,
    value: String,
    unit: String,
    onPressIncrement: (up : Boolean) -> Unit = {},
    onPressDecrement: (up : Boolean) -> Unit = {},
    onClick: () -> Unit,
    componentTag: String = "Sprint8PillBox",
    dataSectionColor: Color = Color.White.copy(alpha = 0.5f),
    buttonSectionColor: Color = Color.White.copy(alpha = 0.7f),
    valueColor: Color = Color.Black,
    titleColor: Color = Color.Black.copy(alpha = 0.7f),
    arrowIconColor: Color = Color.Black.copy(alpha = 0.75f),
    dotIconColor: Color = ICON_COLOR_1,
    borderColor: Color = BORDER_GRAY_2,
    titleSize: TextUnit = 20.sp,
    valueSize: TextUnit = 48.sp,
    cornerRadius: Int = 12,
    imageAlignment: Alignment = Alignment.TopEnd,
    borderThickness: Dp = 1.dp
) {

    val shape = RoundedCornerShape(screenWidth * cornerRadius / 1920f)
    Column(
        modifier = modifier
            .size(
                width = screenWidth * 168 / 1920f, height = screenHeight * 465 / 1080f
            )
            .clip(shape)
            .border(
                border = BorderStroke(borderThickness, borderColor), shape = shape
            )
            .semantics { testTagsAsResourceId = true }) {

        Box(
            modifier = Modifier
                .testTag("$componentTag:LaunchKeypad:Button")
                .fillMaxWidth()
                .weight(1f)
                .background(color = dataSectionColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = screenWidth * 8 / 1920f)
            ) {
                Text(
                    text = title,
                    fontSize = titleSize,
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.labelMedium,
                    color = titleColor,
                )
                Text(
                    text = value,
                    fontSize = valueSize,
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.labelLarge,
                    color = valueColor,
                    modifier = Modifier.testTag("$componentTag:${title}Value:Text")
                )
                if(unit.isNotBlank()) {
                    Text(
                        text = unit,
                        fontSize = titleSize,
                        fontFamily = FontFamily.SansSerif,
                        style = MaterialTheme.typography.labelMedium,
                        color = titleColor,
                        modifier = Modifier.testTag("$componentTag:PillBoxUnit:Text")
                    )
                }
            }

            val padding = screenWidth * 12 / 1920f
            Icon(
                painter = painterResource(R.drawable.ic_dots),
                contentDescription = "Dot icon for keypad",
                modifier = Modifier
                    .align(
                        imageAlignment
                    )
                    .padding(
                        top = padding, end = padding, start = padding
                    )
                    .size(screenWidth * 15 / 1920f),
                tint = dotIconColor
            )
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(), color = borderColor, thickness = borderThickness
        )

        HoldButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(color = buttonSectionColor)
                .testTag("$componentTag:$title:Increase:Button"),
            onPress = onPressIncrement,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sprint8_up),
                contentDescription = "Arrow Up icon",
                modifier = Modifier
                    .size(screenWidth * 58 / 1920f),
                tint = arrowIconColor
            )
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(), color = borderColor, thickness = borderThickness
        )

        HoldButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(color = buttonSectionColor)
                .testTag("$componentTag:$title:Decrease:Button"),
            onPress = onPressDecrement,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sprint8_down),
                contentDescription = "Arrow down icon",
                modifier = Modifier
                    .size(screenWidth * 58 / 1920f),
                tint = arrowIconColor
            )
        }
    }
}
@Composable
private fun HoldButton(
    modifier: Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onPress: (up : Boolean) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) {
    var wasPressed by remember { mutableStateOf(false) }
    val isPressed by interactionSource.collectIsPressedAsState()

    if (wasPressed != isPressed) {
        onPress(wasPressed)
        wasPressed = !wasPressed
    }
    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}


@AllDevicePreviews
@Composable
private fun Sprint8PillboxButtonPreview() {
    val localConfiguration = LocalConfiguration.current
    val screenWidth = localConfiguration.screenWidthDp.dp
    val screenHeight = localConfiguration.screenHeightDp.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(WARMUP_ICON_BORDER_COLOR.value)),
        contentAlignment = Alignment.Center
    ) {
        Sprint8PillboxButton(
            modifier = Modifier,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            title = stringResource(R.string.incline_tc),
            value = "3",
            unit = stringResource(R.string.percent_lc),
            titleSize = with(LocalDensity.current) { (screenHeight.value * 19 / 1080f).toSp() },
            valueSize = with(LocalDensity.current) { (screenHeight.value * 50 / 1080f).toSp() },
            onClick = {})
    }
}