package com.jht.sprint8.sprint8immersive.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Constants.SPRINT_8_DURATION_MILLI
import com.jht.sprint8.utils.Constants.SPRINT_8_TIME_SECONDS
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_PROGRESSION_BAR_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_DARK_GREY
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_PROGRESSION_BAR_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_2
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlin.math.round

/**
 * Composable function to display an indicator for an upcoming sprint in the Sprint8 workout.
 *
 * @param label
 * The label or name of the upcoming sprint (e.g., "Sprint 1").
 * @param isReceived
 * Flag indicating whether this sprint has already been completed or received.
 * @param score
 * Optional score value associated with this sprint. Can be null if not available.
 * @param isSprinting
 * Flag indicating whether the user is currently performing this sprint.
 * Defaults to false.
 * @param isRunning
 * Flag indicating whether the workout session is currently active.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun UpcomingSprintIndicatorComp(
    label: String,
    isReceived: Boolean,
    score: Float?,
    isSprinting: Boolean = false,
    isRunning: Boolean,
) {
    val componentTag = "UpcomingSprintIndicatorComp"
    // ---- Derived state (avoids unnecessary recomposition work)
    val scoreText by remember(isReceived, score, isSprinting, label) {
        derivedStateOf {
            when {
                isReceived && score != null -> round(score).toInt().toString()
                isSprinting -> ""
                else -> label
            }
        }
    }
    val textColor by remember(isReceived) {
        derivedStateOf {
            if (isReceived) Color.White else SPRINT_ICON_BORDER_COLOR
        }
    }
    val backgroundColor by remember(isReceived, isSprinting) {
        derivedStateOf {
            if (isReceived || isSprinting) {
                COOLDOWN_PROGRESSION_BAR_COLOR
            } else {
                SPRINT_PROGRESSION_BAR_COLOR
            }
        }
    }
    val scoreTag = if (isReceived) "Score" else "Sprint"

    // ---- Constants
    val size = 38.dp.computeSize()
    val strokeWidth = 4.dp.computeSize()

    // ---- Animation
    val progress = remember { Animatable(0f) }
    LaunchedEffect(isSprinting, isRunning) {
        if (!isRunning) {
            progress.stop()
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        if (isSprinting) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SPRINT_8_TIME_SECONDS * SPRINT_8_DURATION_MILLI,
                    easing = LinearEasing
                )
            )
        }
    }

    // ---- UI
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(
                BorderStroke(1.dp.computeSize(), SPRINT8_NEW_DARK_GREY),
                CircleShape
            )
            .background(backgroundColor)
            .semantics { testTagsAsResourceId = true }
    ) {
        when {
            isReceived -> {
                Image(
                    painter = painterResource(R.drawable.ic_sweat_score),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("$componentTag:SweatBackground:Image")
                )
            }
            isSprinting -> {
                CircularProgressIndicator(
                    progress = { progress.value },
                    color = SUMMARY_GRAPH_BAR_COLOR_2,
                    strokeWidth = strokeWidth,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(size)
                )
                Image(
                    painter = painterResource(R.drawable.ic_sweat_blue),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("$componentTag:SweatDrop:Image")
                )
            }
        }

        Text(
            text = scoreText,
            fontSize = 18.sp.computeSize(),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier
                .align(Alignment.Center)
                .testTag("$componentTag:$scoreTag:Text")
        )
    }
}

@AllDevicePreviews
@Composable
private fun UpcomingSprintIndicatorCompPreview(){
    UpcomingSprintIndicatorComp(
        label = "Label",
        isReceived = true,
        score = 20f,
        isSprinting = true,
        isRunning = true
    )
}
