package com.jht.sprint8.assessment.ui


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jht.sprint8.R
import com.jht.sprint8.assessment.components.AssessmentSummaryComp
import com.jht.sprint8.sprint8workoutsummary.ui.Sprint8WorkoutSummaryViewModel
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_GREY
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_WHITE
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_SCORE_COLOR
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Assessment Summary Screen
 * Displays a summary of the user's assessment results, including
 * the assessment level, recommended category, and
 * workout statistics.
 * @param assessmentLevel The assessment level of the user, ranging from 1 to 20.
 * @param assessmentRecommendedCategory The recommended category based on the user's assessment level
 * (e.g., Beginner, Intermediate, Advanced, Elite).
 * @param assessmentLevelBgColor Background and border color used to visually represent the
 * assessment level.
 * @param sprint8WorkoutSummaryViewModel ViewModel responsible for providing workout summary data
 * and handling business logic for the summary screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@AllDevicePreviews
@Composable
internal fun Sprint8WorkoutAssessmentScreen(
    assessmentLevel: String = "",
    assessmentRecommendedCategory: String = "",
    assessmentLevelBgColor: Color = Color.Transparent,
    sprint8WorkoutSummaryViewModel: Sprint8WorkoutSummaryViewModel = hiltViewModel()
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val baseTag = "AssessmentSummaryScreen"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SPRINT8_NEW_GREY)
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(top = 60.dp.computeSize())
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.workout_summary_tc),
                fontSize = 32.sp.computeSize(),
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = SPRINT8_NEW_WHITE,
                modifier = Modifier.testTag("$baseTag:Heading:Text")
            )
            Text(
                stringResource(R.string.assessment_results_tc),
                fontSize = 22.sp.computeSize(),
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = SUMMARY_SCORE_COLOR,
                modifier = Modifier.testTag("$baseTag:Subheading:Text")

            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp.computeSize())
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(screenWidth * 0.45f)
                    .height(screenHeight * 0.504f)
                    .padding(horizontal = 70.dp.computeSize())
                    .border(
                        border = BorderStroke(
                            width = 1.dp.computeSize(),
                            color = assessmentLevelBgColor
                        ), shape = RoundedCornerShape(32.dp.computeSize())
                    )
                    .background(
                        color =
                            assessmentLevelBgColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(32.dp.computeSize())
                    )
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = assessmentRecommendedCategory,
                        fontSize = 80.sp.computeSize(),
                        color = assessmentLevelBgColor,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("$baseTag:Beginner:Text")

                    )
                    Text(
                        stringResource(R.string.level_tc) + " " + assessmentLevel,
                        fontSize = 50.sp.computeSize(),
                        color = assessmentLevelBgColor,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("$baseTag:Level:Text")

                    )
                }
            }
            Column(
                modifier = Modifier.wrapContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyVerticalGrid(
                    modifier = Modifier.padding(end = 70.dp.computeSize()),
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(32.dp.computeSize()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp.computeSize())
                ) {
                    items(sprint8WorkoutSummaryViewModel.summaryStatsList.value) { stats ->
                        AssessmentSummaryComp(
                            screenWidth = screenWidth * 0.45f,
                            screenHeight = screenHeight * 0.2f,
                            summaryStatsTitle = stats.title,
                            summaryStatsValue = stats.value,
                            summaryStatsUnit = stats.unit
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(top = 20.dp.computeSize()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.swipe_to_view_your_sweat_score_stats_tc),
                        fontSize = 20.sp.computeSize(),
                        color = SUMMARY_GRAPH_BAR_COLOR_2,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_double_right),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 20.dp.computeSize())
                            .size(32.dp.computeSize())
                    )
                }
            }
        }
    }
}