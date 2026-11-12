package com.jht.sprint8.sprint8immersive.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Composable function to display a countdown timer for the Sprint8 workout.
 * @param modifier
 * Modifier applied to the timer component for layout, styling,
 * or positioning.
 * @param title
 * The title displayed above or alongside the timer (e.g., "Sprint Timer").
 * @param color
 * The color used for the timer text or visuals.
 * @param remainingTime
 * The remaining time in milliseconds or seconds (depending on implementation)
 * to display on the timer.
 * @param isFullScreen
 * Flag indicating whether the timer should occupy the full screen.
 * Defaults to false.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8NewTimer(
    modifier: Modifier,
    title: String,
    color: Color,
    remainingTime: Float,
    isFullScreen: Boolean = false
) {
    val componentTag = "Sprint8NewTimer"


    Column(
        modifier = modifier
            .wrapContentSize()
            .semantics {
                testTagsAsResourceId = true
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            fontSize = (if (isFullScreen) 42 else 32).sp.computeSize(),
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = modifier.testTag("$componentTag:Title:Text")
        )
        Text(
            text = formatTime(remainingTime),
            fontSize =  (if (isFullScreen) 144 else 120).sp.computeSize(),
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = modifier.testTag("$componentTag:TimeInterval:Text")
        )
    }

}

internal fun formatTime(milliseconds: Float): String {
    val seconds = (milliseconds / 1000f).roundToInt()
    return String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60)
}

@AllDevicePreviews
@Composable
fun Sprint8NewTimerPreview(){
    Sprint8NewTimer(
        modifier = Modifier,
        title = stringResource(R.string.title_tc),
        color = Color.Yellow,
        remainingTime = 2000f,
        isFullScreen = false
    )
}