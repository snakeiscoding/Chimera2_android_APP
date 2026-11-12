package com.jht.spinbikeapps.intervals

import androidx.annotation.FloatRange
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors
import com.jht.theme.DynamicTheme
import com.jht.theme.darkDynamicTheme
import com.jht.ui.Device22inPreviews

@Composable
internal fun IntervalsCountdownTimer(
    dialLabel: String = "",
    /** How much of the interval is done. Drives how far the bright ring sweeps. */
    @FloatRange(from = 0.0, to = 1.0)
    completionProgress: Float = 0f,
    /** Pre-formatted value shown in the middle of the dial */
    mainText: String = "",
    accentColor: Color = TargetTrainingColors.BTN_PRIMARY,
) {
    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        val animatedCompletion by animateFloatAsState(
            targetValue = if (completionProgress.isNaN()) 0f else completionProgress.coerceIn(0f, 1f),
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
        )
        Canvas(
            modifier = Modifier
                .size(648.dp)
                .align(Alignment.Center)
        ) {
            val sweepAngle = 360f * animatedCompletion
            drawArc(
                color = if (accentColor == TargetTrainingColors.GREEN_1) TargetTrainingColors.REST_ROW_BACKGROUND else TargetTrainingColors.TIME_SUB_COLOR,
                startAngle = 270f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(25f, 25f),
                size = Size(
                    width = size.width - 50f,
                    height = size.height - 50f
                ),
                style = Stroke(
                    width = 50f,
                )
            )
            drawArc(
                color = accentColor,
                startAngle = 270f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(25f, 25f),
                size = Size(
                    width = size.width - 50f,
                    height = size.height - 50f
                ),
                style = Stroke(
                    width = 50f,
                )
            )


        }
        TimeRemainingDial(
            timerTextLabel = dialLabel,
            mainText = mainText,
            accentColor = accentColor,
        )
    }
}

//region Previews
@Composable
@Device22inPreviews
private fun PreviewIntervalsCountdownTime() {
    DynamicTheme(darkDynamicTheme) {
        IntervalsCountdownTimer(
            dialLabel = stringResource(R.string.time_remaining_tc),
            completionProgress = .01f,
            mainText = "0:30",
        )
    }
}

@Composable
@Device22inPreviews
private fun PreviewIntervalsCountdownDistance() {
    DynamicTheme(darkDynamicTheme) {
        IntervalsCountdownTimer(
            dialLabel = stringResource(R.string.miles_remaining_tc),
            completionProgress = .99f,
            mainText = "0.01",
        )
    }
}
