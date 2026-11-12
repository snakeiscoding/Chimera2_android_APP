package com.jht.sprint8.assessment.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_NEW_DARK_GREY
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_LIST_ITEM_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_SCORE_COLOR
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import java.util.Locale

/**
 * Assessment Summary Component
 * Displays a single assessment summary item, including its title,
 * value, and corresponding unit.
 * @param summaryStatsTitle The label describing the summary statistic (e.g., "Average Speed",
 * "Average Incline", "Peak Speed").
 * @param summaryStatsValue The numeric value associated with the summary statistic.
 * @param summaryStatsUnit The unit of measurement for the summary value
 * (e.g., "mph", "%").
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AssessmentSummaryComp(
    screenWidth: Dp, screenHeight: Dp,
    summaryStatsTitle: String,
    summaryStatsValue: Float,
    summaryStatsUnit: String
) {
    val componentTag = "AssessmentSummaryScreenComp"

    Box(
        modifier = Modifier
            .width(screenWidth)
            .height(screenHeight)
            .semantics {
                testTagsAsResourceId = true
            }
            .background(
                CUSTOM_WORKOUT_LIST_ITEM_COLOR,
                shape = RoundedCornerShape(32.dp.computeSize())
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp.computeSize())
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                summaryStatsTitle,
                fontSize = 18.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                color = SUMMARY_SCORE_COLOR,
                modifier = Modifier.testTag("$componentTag:Peak title:Text")
            )
            Text(
                text = String.format(Locale.getDefault(), "%.1f", summaryStatsValue),
                fontSize = 54.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                color = ASSESSMENT_NEW_DARK_GREY,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("$componentTag:Peak subtitle:Text")
            )
            Text(
                summaryStatsUnit,
                fontSize = 18.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                color = SUMMARY_SCORE_COLOR
            )
        }
    }
}

@AllDevicePreviews
@Composable
fun AssessmentSummaryCompPreview() {
    AssessmentSummaryComp(300.dp, 200.dp, "", 0f, "")
}