package com.jht.sprint8.sprint8classic.components


import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.enums.FrameType
import com.jht.sprint8.R
import com.jht.sprint8.utils.Constants.COOLDOWN_TIME
import com.jht.sprint8.utils.Constants.PREVIEW_DIVISOR
import com.jht.sprint8.utils.Constants.RECOVERY_TIME
import com.jht.sprint8.utils.Constants.SECONDS_30
import com.jht.sprint8.utils.Constants.THRESHOLD
import com.jht.sprint8.utils.Constants.WARMUP_TIME
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_BAR_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_CLOCK_ICON_COLOR
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_LIST_ITEM_COLOR
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_10
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_BAR_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.TEXT_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_BAR_COLOR_2
import com.jht.ui.computeSize

private val defaultMaxHeight = 300.dp

@Composable
internal fun PreviewBarGraphComponent(
    workoutData: List<Float>,
    frameType: FrameType
) {
    PreviewBarGraphChart(
        values = workoutData,
        frameType = frameType
    )
}
/**
 * Displays a preview bar graph chart for a given set of values.
 * @param modifier Modifier applied to the root chart composable for layout
 * and styling.
 * @param values List of float values to be represented as bars in the chart.
 * @param maxHeight Maximum height of the bars, used to scale the chart visually.
 * Defaults to a computed size suitable for previews.
 * @param frameType Defines the visual frame or border style applied to the chart.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PreviewBarGraphChart(
    modifier: Modifier = Modifier,
    values: List<Float>,
    maxHeight: Dp = defaultMaxHeight.computeSize(),
    frameType: FrameType
) {
    Row(
        modifier = modifier
            .then(
                Modifier
                    .graphicsLayer(scaleX = 0.9f)
                    .fillMaxWidth()
                    .height(maxHeight)
            )
            .semantics {
                testTagsAsResourceId = true
            },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, item ->
            val (timeLabel, color) = when {
                index == 0 -> WARMUP_TIME to Brush.verticalGradient(
                    listOf(
                        WARMUP_BAR_COLOR_1,
                        WARMUP_BAR_COLOR_2
                    )
                )

                index == values.size - 1 -> COOLDOWN_TIME to Brush.verticalGradient(
                    listOf(
                        COOLDOWN_CLOCK_ICON_COLOR,
                        COOLDOWN_BAR_COLOR_2
                    )
                )

                index and 1 == 0 -> RECOVERY_TIME to Brush.verticalGradient(
                    listOf(
                        RECOVERY_ICON_BORDER_COLOR,
                        SPRINT8_BG_10
                    )
                )

                else -> SECONDS_30 to Brush.verticalGradient(listOf(SPRINT_BAR_COLOR_1, SPRINT_BAR_COLOR_2))
            }
            Bar(
                value = item,
                timeLabel = timeLabel,
                color = color,
                maxHeight = maxHeight,
                width = 52.dp.computeSize(),
                indexTag = index.toString(),
                frameType = frameType
            )
        }
    }
}


@Composable
private fun Bar(
    value: Float,
    timeLabel: String?,
    color: Brush,
    maxHeight: Dp,
    width: Dp,
    indexTag: String,
    frameType: FrameType,
) {
    val componentTag = "PreviewGraph"
    val configuration = LocalConfiguration.current
    val barSize = when (configuration.screenHeightDp) {
        1000 -> 5.5f
        in 696 .. 734 -> 4.3f
        else -> 5.5f
    }


    val itemHeight = remember(value, frameType, maxHeight) {
        val threshold = THRESHOLD
        if (value <= threshold) {
            barSize
        } else {
            val divisor = PREVIEW_DIVISOR
            value * maxHeight.value / divisor
        }
    }
    var animated by remember { mutableStateOf(false) }

    val animatedHeight by animateFloatAsState(
        targetValue = if (animated) itemHeight else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "barHeightAnimation"
    )

    LaunchedEffect(Unit) {
        animated = true
    }

    Column(modifier = Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .height(animatedHeight.dp * 12)
                .width(width)
                .background(
                    color,
                    shape = RoundedCornerShape(
                        topStart = 8.dp.computeSize(),
                        topEnd = 8.dp.computeSize()
                    )
                ),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_speedometer),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 10.dp.computeSize())
                    .testTag("$componentTag:SpeedometerImage:Image"),
            )
            Text(
                text = "%.1f".format(value),
                fontSize = 20.sp.computeSize(),
                color = Color.White,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Normal,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .padding(bottom = 10.dp.computeSize())
                    .wrapContentSize()
                    .testTag("$componentTag:Value:$indexTag:Text"),

                )
        }

        if (timeLabel != null) {
            Text(
                text = timeLabel,
                fontSize = 16.sp.computeSize(),
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Normal,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .padding(bottom = 10.dp.computeSize())
                    .width(64.dp.computeSize())
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                TEXT_COLOR_1,
                                CUSTOM_WORKOUT_LIST_ITEM_COLOR
                            )
                        )
                    )
                    .testTag("$componentTag:TimeLabel:$indexTag:Text")
            )
        }
    }
}