package com.jht.sprint8.sprint8immersive.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_LIGHT_BLUE
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_2
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize


/**
 * Composable function to display the RPM (Revolutions Per Minute) indicator.
 * Shows the current RPM value during the Sprint8 workout
 * @param rpm
 * The current RPM value to display. Defaults to 0.
 * @param modifier
 * Modifier applied to the RPM component for layout, styling,
 * or positioning.
 * @param screenWidth
 * Width of the screen in Dp, used for responsive layout calculations.
 * @param screenHeight
 * Height of the screen in Dp, used for responsive layout calculations.
 * @param maxRpm
 * The maximum RPM value used to scale the visual indicator.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8NewRpmComponent(
    rpm: Int = 0,
    modifier: Modifier,
    screenWidth: Dp,
    screenHeight: Dp,
    maxRpm: Float
) {
    val componentTag = "Sprint8NewRpmComponent"
    Column(modifier = modifier
        .wrapContentSize()
        .semantics {
            testTagsAsResourceId = true
        }, horizontalAlignment = Alignment.End) {
        LinearProgressIndicator(
            progress = { rpm.toFloat() / maxRpm },
            modifier = Modifier
                .height(screenHeight * 0.03f)
                .width(screenWidth * 0.180f)
                .align(Alignment.End),
            color = SUMMARY_GRAPH_BAR_COLOR_2,
            trackColor = SUMMARY_GRAPH_BAR_COLOR_2.copy(alpha = 0.5f),
        )
        Text(
            text = rpm.toString(),
            fontSize = 72.sp.computeSize(),
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            color = SUMMARY_GRAPH_BAR_COLOR_2,
            modifier = Modifier.testTag("$componentTag:Rpm:Text")
        )
        Text(
            stringResource(R.string.rpm_uc),
            fontSize = 17.sp.computeSize(),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            color = SPRINT8_NEW_LIGHT_BLUE,
            modifier = Modifier
                .offset(y = -10.dp.computeSize())
                .padding(end = 10.dp.computeSize())
                .testTag("$componentTag:RpmTitle:Text")
        )
    }
}

@AllDevicePreviews
@Composable
fun Sprint8NewRmpCompPreview(){
    Sprint8NewRpmComponent(
        rpm = 9,
        modifier = Modifier,
        screenWidth = 500.dp,
        screenHeight = 300.dp,
        maxRpm = 100f
    )
}