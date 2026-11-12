package com.jht.sprint8.sprint8workoutsummary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.enums.SummaryStage
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_1
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_2
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.TEXT_COLOR_1
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.widgets.helpers.stringResourceDefault

/**
 * Composable screen that displays detailed workout summary statistics.
 *
 * This screen renders a table or list of [FullStatTableRow] items, showing
 * comprehensive information about the completed workout. The layout adapts
 * to the provided screen height.
 *
 * @param fullStats List of [FullStatTableRow] representing detailed statistics
 * to display.
 * @param screenHeight Screen height used for responsive layout calculations.
 */
@Composable
internal fun WorkoutSummaryDetailScreen(
    fullStats: List<FullStatTableRow>,
    screenHeight: Dp
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SPRINT8_BG_1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            stringResource(R.string.workout_summary_tc),
            fontSize = 32.sp.computeSize(),
            color = SPRINT8_BG_2,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Normal,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = screenHeight * 0.06f)
                .testTag("Sprint8Summary:DetailedTitle:Text")
        )
        Text(
            stringResource(R.string.details_tc),
            fontSize = 22.sp.computeSize(),
            color = SPRINT8_BG_2,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Normal,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp.computeSize(),
                    bottom = 20.dp.computeSize()
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(TEXT_COLOR_1, shape = RoundedCornerShape(12.dp.computeSize()))
                .padding(vertical = screenHeight * 0.01f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.size(22.dp.computeSize()))
            Row(
                modifier = Modifier.fillMaxWidth(0.65f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.warmup_tc),
                    fontSize = 18.sp.computeSize(),
                    color = SPRINT8_BG_2,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.plus_uc),
                    fontSize = 18.sp.computeSize(),
                    color = SPRINT8_BG_2,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.wrapContentSize()
                )

                Text(
                    text = stringResource(R.string.workout_tc),
                    fontSize = 18.sp.computeSize(),
                    color = SPRINT8_BG_2,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.weight(1f)

                )
                Text(
                    text = stringResource(R.string.plus_uc),
                    fontSize = 18.sp.computeSize(),
                    color = SPRINT8_BG_2,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.wrapContentSize()
                )

                Text(
                    text = stringResource(R.string.cooldown_tc),
                    fontSize = 18.sp.computeSize(),
                    color = SPRINT8_BG_2,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "=",
                    fontSize = 18.sp.computeSize(),
                    color = SPRINT8_BG_2,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.wrapContentSize()
                )

                Text(
                    text = stringResource(R.string.total_tc),
                    fontSize = 18.sp.computeSize(),
                    color = SUMMARY_GRAPH_BAR_COLOR_2,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.weight(1f)

                )
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            for (statRow in fullStats) {
                item {
                    val label = stringResourceDefault(statRow.labelId)
                    val unit = stringResourceDefault(statRow.unitId)
                    val rowLabel = label + if (unit.isEmpty()) "" else " ($unit)"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = screenHeight * 0.1f,
                                vertical = screenHeight * 0.016f
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = rowLabel,
                            fontSize = 20.sp.computeSize(),
                            color = SPRINT8_BG_2,
                            textAlign = TextAlign.Start,
                            fontStyle = FontStyle.Normal,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.weight(3f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(0.65f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = statRow[SummaryStage.WARMUP],
                                fontSize = 18.sp.computeSize(),
                                color = SPRINT8_BG_2,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Normal,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.weight(2f)
                            )

                            Text(
                                text = statRow[SummaryStage.WORKOUT],
                                fontSize = 18.sp.computeSize(),
                                color = SPRINT8_BG_2,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Normal,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.weight(2f)
                            )

                            Text(
                                text = statRow[SummaryStage.COOLDOWN],
                                fontSize = 18.sp.computeSize(),
                                color = SPRINT8_BG_2,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Normal,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.weight(2f)

                            )

                            Text(
                                text = statRow[SummaryStage.TOTAL],
                                fontSize = 18.sp.computeSize(),
                                color = SUMMARY_GRAPH_BAR_COLOR_2,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Normal,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.weight(2f).padding(start = 12.dp.computeSize())
                            )
                        }
                    }
                }
                item {
                    HorizontalDivider(
                        thickness = 1.dp.computeSize(),
                        color = Color.DarkGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = screenHeight * 0.1f,
                            )
                    )
                }
            }
        }
    }
}


@AllDevicePreviews
@Composable
private fun Sprint8WorkoutSummaryDetailScreen() {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    WorkoutSummaryDetailScreen(
        fullStats = emptyList(),
        screenHeight = screenHeight,
    )
}