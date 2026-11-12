package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.BORDER_GRADIENT
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_1
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Displays a single button for the Sprint 8 keypad interface, showing a value,
 * title, and unit with customizable styling and layout.
 * @param modifier Modifier applied to the button for layout, styling, and interaction.
 * @param nextValue String representing the main value displayed on the button.
 * @param keypadTitle Title text displayed above or near the value.
 * @param keyPadUnit Unit text displayed alongside the value (e.g., "BPM", "RPM").
 * @param screenHeight Screen height used for responsive layout calculations.
 * @param screenWidth Screen width used for responsive layout calculations.
 * @param color Background color of the button. Defaults to white.
 * @param borderStroke Border stroke configuration for the button. Defaults to no stroke.
 * @param textColor Color used for the keypad title and value text. Defaults to [SPRINT8_BG_1].
 * @param keyPadTitleSize Text size for the keypad title. Defaults to 20.sp.
 * @param nextValueSize Text size for the main value. Defaults to 48.sp.
 * @param imageAlignment Alignment for any image/icon inside the button. Defaults to top-end.
 * @param isFullScreen Boolean indicating whether the button is displayed in fullscreen mode.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8KeyPadButton(
    modifier: Modifier,
    nextValue: String,
    keypadTitle: String,
    keyPadUnit: String,
    screenHeight: Dp,
    screenWidth: Dp,
    color: Color = Color.White,
    borderStroke: BorderStroke = BorderStroke(0.dp, color = Color.White),
    textColor: Color = SPRINT8_BG_1,
    keyPadTitleSize: TextUnit = 20.sp,
    nextValueSize: TextUnit = 48.sp,
    imageAlignment: Alignment = Alignment.TopEnd,
    isFullScreen : Boolean = false
) {
    val testTagPrefix = "Sprint8"
    Box(
        modifier = modifier
            .width(screenWidth * if (isFullScreen) 264 / 1920f else 252 / 1600f)
            .height(screenHeight * 160 / if (isFullScreen) 1080f else 913f)
            .border(borderStroke, shape = RoundedCornerShape(screenWidth * 20 / 1600f))
            .background(
                shape = RoundedCornerShape(screenWidth * 20 / 1600f),
                color = color
            )
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        Image(
            painter = painterResource(R.drawable.ic_dots),
            colorFilter = ColorFilter.tint(textColor),
            contentDescription = null,
            modifier = Modifier
                .align(
                    imageAlignment
                )
                .padding(
                    top = 12.dp.computeSize(),
                    end = 12.dp.computeSize(),
                    start = 12.dp.computeSize()
                )
                .alpha(0f)
                .testTag("$testTagPrefix:DotImage:Image")
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = keypadTitle,
                fontSize = keyPadTitleSize,
                fontFamily = FontFamily.SansSerif,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("$testTagPrefix:KeypadTitle:Text")
            )
            Text(
                text = nextValue, fontSize = nextValueSize,
                fontFamily = FontFamily.SansSerif,
                style = MaterialTheme.typography.labelLarge,
                color = textColor, modifier = Modifier.testTag("$testTagPrefix:${keypadTitle}Value:Text")
            )
            if (keyPadUnit.isNotBlank()) {
                Text(
                    text = keyPadUnit, fontSize = keyPadTitleSize,
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor, modifier = Modifier.testTag("$testTagPrefix:KeypadUnit:Text")
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun Sprint8KeyPadButtonPreview() {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
    {
        DynamicTheme {
            Sprint8KeyPadButton(
                modifier = Modifier,
                nextValue = "5.0",
                keypadTitle = stringResource(R.string.next_incline_tc),
                keyPadUnit = stringResource(R.string.percent_lc),
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                color = Color.White,
                borderStroke = BorderStroke(screenWidth * 3 / 1600f, BORDER_GRADIENT),
                textColor = Color.Black,
                keyPadTitleSize = with(density) { (screenHeight * 20 / 913f).toSp() },
                nextValueSize = with(density) { (screenHeight * 48 / 913f).toSp() }
            )
        }
    }
}