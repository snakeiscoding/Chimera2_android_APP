package com.jht.sprint8.sprint8workoutsummary.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
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
import com.jht.interfaces.models.SweatScoreList
import com.jht.sprint8.R
import com.jht.sprint8.utils.Constants.SUMMARY_BAR_GRAPH_HEIGHT_MAX
import com.jht.sprint8.utils.Constants.SUMMARY_BAR_GRAPH_HEIGHT_MIN
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_1
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_2
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_2
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlin.math.round

private val defaultMaxHeight = 350.dp

/**
 * Composable function to display the summary bar graph.
 *
 * @param sweatScoreList
 * Optional list containing sweat score data for each sprint.
 * If null, the bar graph will handle the absence of data gracefully.
 * @param numberOfSprints
 * Total number of sprints completed in the workout, used to
 * determine the number of bars displayed.
 */
@Composable
internal fun SummaryBarGraphScreen(sweatScoreList: SweatScoreList?, numberOfSprints: Int) {
    val barChartInputsPercent = sweatScoreList?.sweatScoreList?.let {
        it + List(numberOfSprints - it.size) { null }
    } ?: List(numberOfSprints) { null }

    val maxSweatScore = barChartInputsPercent.filterNotNull().maxOrNull() ?: 0f
    val lastMaxIndex = sweatScoreList?.sweatScoreList?.indexOfLast { it == maxSweatScore }

    SummaryBarChart(
        modifier = Modifier.padding(horizontal = 20.dp.computeSize(), vertical = 10.dp.computeSize()),
        values = barChartInputsPercent,
        sweatScoreList = sweatScoreList,
        maxSweatScore = maxSweatScore,
        lastMaxIndex = lastMaxIndex,
        numberOfSprints = numberOfSprints
    )
}

/**
 * Composable function to display a summary bar chart for Sprint8 workout data.
 *
 * Renders a bar chart representing sweat score values across multiple sprints.
 * The chart highlights peak values and supports optional data for handling
 * incomplete or missing sprint results. It is used in Sprint8 summary screen to visualize performance trends.
 *
 * @param modifier
 * Modifier applied to the bar chart for layout, styling, or positioning.
 *
 * @param values
 * List of sweat score values for each sprint. Nullable values represent
 * unavailable data.
 *
 * @param maxHeight
 * Maximum height of the bar chart. Defaults to a computed size based on screen dimensions.
 *
 * @param sweatScoreList
 * Optional detailed sweat score data used for enhanced visualization
 * or annotations in the chart.
 *
 * @param maxSweatScore
 * The highest sweat score value used to normalize bar heights.
 *
 * @param lastMaxIndex
 * Index of the last sprint that achieved the maximum sweat score,
 * used for highlighting the peak sprint. Can be null if no peak exists.
 *
 * @param numberOfSprints
 * Total number of sprints in the workout, used to calculate chart spacing
 * and alignment.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SummaryBarChart(
    modifier: Modifier = Modifier,
    values: List<Float?>?,
    maxHeight: Dp = defaultMaxHeight.computeSize(),
    sweatScoreList: SweatScoreList?,
    maxSweatScore: Float,
    lastMaxIndex: Int?,
    numberOfSprints: Int
) {
    Row(
        modifier = modifier.then(
            Modifier
                .fillMaxWidth(0.8f)
                .height(maxHeight)
                .semantics {
                    testTagsAsResourceId = true
                },
        ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..numberOfSprints) {
            val item = values?.getOrNull(i - 1)
            val color = when (item) {
                null -> Color.Transparent
                maxSweatScore -> SUMMARY_GRAPH_BAR_COLOR_1
                else -> SUMMARY_GRAPH_BAR_COLOR_2
            }
            Bar(
                value = item ?: 0f,
                color = color,
                maxHeight = maxHeight,
                sweatScoreList = sweatScoreList,
                sprintNumber = i,
                maxSweatScore = maxSweatScore,
                lastMaxIndex = lastMaxIndex
            )
        }
    }
}

/**
 * Composable function to render a single bar within a summary bar chart.
 *
 * This function is used inside a [Row] to display an individual bar
 * representing a sprint's sweat score. The bar height is calculated
 * relative to the maximum sweat score and can be visually highlighted
 * when it represents the peak sprint.
 *
 * @param value
 * The sweat score value for the sprint, used to determine the bar height.
 *
 * @param color
 * The color of the bar.
 *
 * @param maxHeight
 * The maximum height the bar can reach, used for normalizing values.
 *
 * @param sweatScoreList
 * Optional detailed sweat score data used for additional context
 * or annotations.
 *
 * @param sprintNumber
 * The sprint number associated with this bar (1-based index).
 *
 * @param maxSweatScore
 * The highest sweat score across all sprints, used to scale the bar height.
 *
 * @param lastMaxIndex
 * Index of the last sprint that achieved the maximum sweat score,
 * used to highlight the peak bar. Can be null if no peak exists.
 */
@Composable
private fun RowScope.Bar(
    value: Float,
    color: Color,
    maxHeight: Dp,
    sweatScoreList: SweatScoreList?,
    sprintNumber: Int,
    maxSweatScore: Float,
    lastMaxIndex: Int?
) {
    val barHeight = value * maxHeight.value
    val itemHeight = remember(value) {
        when {
            barHeight > 5000 -> barHeight / SUMMARY_BAR_GRAPH_HEIGHT_MAX
            value in 1f..15f -> SUMMARY_BAR_GRAPH_HEIGHT_MIN + value * 0.5f
            else -> value / SUMMARY_BAR_GRAPH_HEIGHT_MAX
        }
    }
    var animated by remember { mutableStateOf(false) }
    val componentTag = "SummaryBarGraphScreen"
    val sweatScore = sweatScoreList?.sweatScoreList?.getOrNull(sprintNumber - 1)
    val sweatBarHeight = if (animated) itemHeight else 0f
    val animatedHeight by animateFloatAsState(
        targetValue = sweatBarHeight,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "barHeightAnimation"
    )

    LaunchedEffect(Unit) {
        animated = true
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 7.dp.computeSize())
            .weight(1f),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            sweatScore?.let {
                Text(
                    text = round(it).toInt().toString(),
                    fontSize = 16.sp.computeSize(),
                    color = SPRINT8_BG_2,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .padding(top = 10.dp.computeSize())
                        .wrapContentSize()
                        .testTag("$componentTag:sweatScore:$sprintNumber")
                )
            }
            Column(
                modifier = Modifier
                    .height(animatedHeight.dp)
                    .fillMaxWidth()
                    .background(color, shape = RoundedCornerShape(18.dp.computeSize())),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (sprintNumber - 1 == lastMaxIndex) {
                    Text(
                        text = stringResource(R.string.new_best_tc),
                        fontSize = 16.sp.computeSize(),
                        color = SPRINT8_BG_1,
                        textAlign = TextAlign.Center,
                        fontStyle = FontStyle.Normal,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .padding(top = 10.dp.computeSize())
                            .wrapContentSize()
                            .testTag("$componentTag:NewBestLabel:Text")
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(
                        if (sweatScore == maxSweatScore) {
                            R.drawable.ic_sweat_yellow
                        } else R.drawable.ic_sweat_blue
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp.computeSize())
                        .padding(bottom = 10.dp.computeSize())
                        .testTag("$componentTag:SweatIcon:Image")
                )
            }
        }
        Text(
            text = sprintNumber.toString(),
            fontSize = 16.sp.computeSize(),
            color = SPRINT8_BG_2,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Normal,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .wrapContentSize()
                .padding(top = 20.dp.computeSize())
                .testTag("$componentTag:sweatScore:Text")
        )
    }
}

@AllDevicePreviews
@Composable
private fun SummaryBarChartPreview() {
    val sweatScoreList = SweatScoreList(
        sweatScoreList = listOf(109f, 22f, 112f, 112f, 115f,115f,115f,115f)
    )
    val numberOfSprints = 8
    val barChartInputsPercent = sweatScoreList?.sweatScoreList?.let {
        it + List(numberOfSprints - it.size) { null }
    } ?: List(numberOfSprints) { null }

    val maxSweatScore = barChartInputsPercent.filterNotNull().maxOrNull() ?: 0f
    val lastMaxIndex = sweatScoreList?.sweatScoreList?.indexOfLast { it == maxSweatScore }


    SummaryBarChart(
        modifier = Modifier.padding(horizontal = 20.dp.computeSize(), vertical = 10.dp.computeSize()),
        values = barChartInputsPercent,
        sweatScoreList = sweatScoreList,
        maxSweatScore = maxSweatScore,
        lastMaxIndex = lastMaxIndex,
        numberOfSprints = numberOfSprints
    )
}