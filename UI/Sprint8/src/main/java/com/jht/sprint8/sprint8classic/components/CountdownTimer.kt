package com.jht.sprint8.sprint8classic.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_ACTIVE_BAR_COLOR
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_BACKGROUND_COLOR
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_CLOCK_ICON_COLOR
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_PAUSE_ICON_COLOR
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_PROGRESSION_BAR_COLOR
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_ACTIVE_BAR_COLOR
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_BACKGROUND_COLOR
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_PAUSE_ICON_COLOR
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_PROGRESSION_BAR_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_ACTIVE_BAR_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_BACKGROUND_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_PAUSE_ICON_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_PROGRESSION_BAR_COLOR
import com.jht.sprint8.utils.Sprint8Colors.TEXT_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_ACTIVE_BAR_COLOR
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_BACKGROUND_COLOR
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_PAUSE_ICON_COLOR
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_PROGRESSION_BAR_COLOR
import com.jht.ui.AllDevicePreviews
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt


internal fun formatTime(milliseconds: Float): String {
    val seconds = (milliseconds / 1000f).roundToInt()
    return String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60)
}

sealed class TimerStyle(
    val backgroundColor: Color,      // Inner circle
    val activeBarColor: Color,       // Outer circle
    val progressionBarColor: Color,  // Progress arc
    val clockIconColor: Color,       // Clock icon
    val pauseIconColor: Color        // Pause icon or play icon when avl
) {
    data object Warmup : TimerStyle(
        backgroundColor = WARMUP_BACKGROUND_COLOR,
        activeBarColor = WARMUP_ACTIVE_BAR_COLOR,
        progressionBarColor = WARMUP_PROGRESSION_BAR_COLOR,
        clockIconColor = WARMUP_ICON_BORDER_COLOR,
        pauseIconColor = WARMUP_PAUSE_ICON_COLOR
    )

    data object Sprint : TimerStyle(
        backgroundColor = SPRINT_BACKGROUND_COLOR,
        activeBarColor = SPRINT_ACTIVE_BAR_COLOR,
        progressionBarColor = SPRINT_PROGRESSION_BAR_COLOR,
        clockIconColor = SPRINT_ICON_BORDER_COLOR,
        pauseIconColor = SPRINT_PAUSE_ICON_COLOR
    )

    data object Recovery : TimerStyle(
        backgroundColor = RECOVERY_BACKGROUND_COLOR,
        activeBarColor = RECOVERY_ACTIVE_BAR_COLOR,
        progressionBarColor = RECOVERY_PROGRESSION_BAR_COLOR,
        clockIconColor = RECOVERY_ICON_BORDER_COLOR,
        pauseIconColor = RECOVERY_PAUSE_ICON_COLOR
    )

    data object Cooldown : TimerStyle(
        backgroundColor = COOLDOWN_BACKGROUND_COLOR,
        activeBarColor = COOLDOWN_ACTIVE_BAR_COLOR,
        progressionBarColor = COOLDOWN_PROGRESSION_BAR_COLOR,
        clockIconColor = COOLDOWN_CLOCK_ICON_COLOR,
        pauseIconColor = COOLDOWN_PAUSE_ICON_COLOR
    )
}

/**
 * Displays a countdown timer UI with configurable style and controls.
 *
 * @param modifier Modifier applied to the root timer composable for layout
 * and styling.
 * @param totalTime Total duration of the timer, in milliseconds.
 * @param remainingTime Remaining time of the timer, in milliseconds.
 * This value is typically provided by a ViewModel.
 * @param isRunning Indicates whether the timer is currently running or paused.
 * @param timerStyle Defines the visual style of the timer (e.g., circular,
 * classic, minimal).
 * @param strokeWidth Width of the timer progress stroke, used for visual thickness.
 * @param screenTestTag Optional test tag applied to the timer for UI testing.
 * @param onTimerFinish Callback invoked when the countdown reaches zero.
 * @param onPlayPauseClick Callback invoked when the play/pause control is clicked.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CountdownTimer(
    modifier: Modifier = Modifier,
    totalTime: Float,
    remainingTime: Float, // from ViewModel, in ms
    isRunning: Boolean,
    timerStyle: TimerStyle,
    strokeWidth: Float = 32f,
    screenTestTag: String ? = null,
    onTimerFinish: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {}
) {
    val animatedTime = remember { Animatable(remainingTime) }

    LaunchedEffect(remainingTime, isRunning) {
        if (!isRunning) {
            animatedTime.stop()
        } else {
            val delta = abs(animatedTime.value - remainingTime)
            if (delta > 1100f) {
                // Large gap = likely screen navigation away n restore or a jump
                animatedTime.snapTo(remainingTime)
            } else {
                // Small gap = regular timer update via data matrix so animate smoothly
                animatedTime.animateTo(
                    targetValue = remainingTime,
                    animationSpec = tween(
                        durationMillis = 1000,
                        easing = LinearEasing
                    )
                )
            }
        }
    }

    LaunchedEffect(animatedTime.value) {
        if (animatedTime.value <= 0f) onTimerFinish()
    }

    val progress = (animatedTime.value / totalTime).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f) .semantics {
            testTagsAsResourceId = true
        }, contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val fontSize = with(density) { (maxWidth * (130f / 484f)).toSp() }
        val strokeWidthInPx = with(density) { (maxWidth * (strokeWidth / 484f)).toPx() }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val arcDiameter = size.minDimension - strokeWidthInPx
            val arcRadius = arcDiameter / 2f
            val arcOffset = Offset(
                (size.width - arcDiameter) / 2, (size.height - arcDiameter) / 2
            )
            drawCircle(
                color = timerStyle.backgroundColor, radius = arcRadius, center = center
            )
            drawCircle(
                color = timerStyle.activeBarColor,
                radius = arcRadius,
                style = Stroke(width = strokeWidthInPx)
            )
            drawArc(
                color = timerStyle.progressionBarColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = arcOffset,
                size = Size(arcDiameter, arcDiameter),
                style = Stroke(
                    width = strokeWidthInPx
                )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxHeight(297f / 484f)
                .fillMaxWidth(388f / 484f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = R.drawable.clock_new_bold),
                contentDescription = "Clock",
                modifier = Modifier
                    .fillMaxWidth(50 / 388f)
                    .aspectRatio(1F)
                    .testTag("${screenTestTag}:TimerComponent:Icon"),
                tint = timerStyle.clockIconColor
            )
            val totalSeconds = (animatedTime.value / 1000).toInt().coerceAtLeast(0)
            Text(
                text = formatTime(remainingTime),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = TEXT_COLOR_1,
                modifier = Modifier.testTag("${screenTestTag}:TimerComponent:Text")
            )
            Icon(
                painter = painterResource(id = if (isRunning) R.drawable.pause_new_filled else R.drawable.baseline_play_arrow_24),
                contentDescription = if (isRunning) "Pause" else "Play",
                modifier = Modifier
                    .fillMaxWidth(69 / 388f)
                    .aspectRatio(1F)
                    .testTag("${screenTestTag}:TimerComponent:Button")
                    .clickable { onPlayPauseClick.invoke() },
                tint = timerStyle.pauseIconColor)
        }
    }
}

@AllDevicePreviews
@Composable
private fun CountdownTimerPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            CountdownTimer(
                modifier = Modifier.weight(1f), totalTime = 90000F, remainingTime = 30000F, isRunning = true, timerStyle = TimerStyle.Warmup
            )
            CountdownTimer(
                modifier = Modifier.weight(0.8f), totalTime = 90000F, remainingTime = 50000F, isRunning = true, timerStyle = TimerStyle.Sprint
            )
            CountdownTimer(
                modifier = Modifier.weight(0.6f),
                totalTime = 90000F,
                remainingTime = 60000F,
                isRunning = true,
                timerStyle = TimerStyle.Recovery
            )
            CountdownTimer(
                modifier = Modifier.weight(0.4f),
                totalTime = 90000F,
                remainingTime = 45000F,
                isRunning = true,
                timerStyle = TimerStyle.Cooldown
            )
        }
    }
}

