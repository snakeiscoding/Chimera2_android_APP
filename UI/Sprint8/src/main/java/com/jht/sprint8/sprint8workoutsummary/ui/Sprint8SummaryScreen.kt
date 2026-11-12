package com.jht.sprint8.sprint8workoutsummary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.interfaces.models.SweatScoreList
import com.jht.sprint8.R
import com.jht.sprint8.assessment.ui.Sprint8WorkoutAssessmentScreen
import com.jht.sprint8.sprint8classic.components.Sprint8TopBannerComp
import com.jht.sprint8.sprint8workoutsummary.components.SummaryBarGraphScreen
import com.jht.sprint8.sprint8workoutsummary.components.SummaryScoreComp
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_1
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_2
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_SCORE_COLOR
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize


enum class PagerScreen {
    SCREEN_ASSESSMENT, SCREEN_SUMMARY_GRAPH, SCREEN_SUMMARY_DETAIL
}

/**
 * Composable function to display the Sprint8 workout summary screen.
 *
 * Shows a detailed summary of the workout, including full statistics,
 * sweat scores, sprint count, and assessment recommendations.
 * This screen is typically displayed after completing a Sprint8 workout
 * or an assessment session.
 *
 * @param fullStats
 * A list of full statistics to display in a tabular format, represented by [FullStatTableRow].
 * @param sweatScoreList
 * Optional list of individual sweat scores for each sprint, represented by [SweatScoreList].
 * @param totalSweatScore
 * The total sweat score accumulated during the workout, displayed as a string.
 * @param peakSweatScore
 * The highest sweat score recorded in a single sprint, displayed as a string.
 * @param averageSweatScore
 * The average sweat score across all sprints, displayed as a string.
 * @param numberOfSprints
 * The total number of sprints completed during the workout.
 * @param isAssessmentVisible
 * Flag indicating whether assessment-related information should be displayed.
 * @param recommendedAssessmentLevel
 * The recommended assessment level based on the user's performance.
 * @param assessmentRecommendedCategory
 * The assessment category corresponding to the recommended level
 * (e.g., Beginner, Intermediate, Advanced, Elite).
 * @param assessmentLevelBgColor
 * Background color used to visually highlight the recommended assessment level.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8SummaryScreen(
    fullStats: List<FullStatTableRow>,
    sweatScoreList: SweatScoreList?,
    totalSweatScore: String,
    peakSweatScore: String,
    averageSweatScore: String,
    numberOfSprints: Int,
    isAssessmentVisible: Boolean,
    recommendedAssessmentLevel: String,
    assessmentRecommendedCategory: String,
    assessmentLevelBgColor: Color
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val baseTag = "Sprint8SummaryScreen"
    val summaryScreensVisible = remember(isAssessmentVisible) {
        if (isAssessmentVisible) {
            listOf(
                PagerScreen.SCREEN_ASSESSMENT,
                PagerScreen.SCREEN_SUMMARY_GRAPH,
                PagerScreen.SCREEN_SUMMARY_DETAIL
            )
        } else {
            listOf(PagerScreen.SCREEN_SUMMARY_GRAPH, PagerScreen.SCREEN_SUMMARY_DETAIL)
        }
    }

    val pagerState = rememberPagerState(
        pageCount = { summaryScreensVisible.size },
        initialPage = 0
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SPRINT8_BG_1)
            .semantics {
                testTagsAsResourceId = true
            }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Sprint8TopBannerComp(
                modifier = Modifier.align(Alignment.CenterHorizontally), isInfoIconVisible = false
            )
            HorizontalPager(
                state = pagerState, modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (summaryScreensVisible[page]) {
                    PagerScreen.SCREEN_ASSESSMENT -> Sprint8WorkoutAssessmentScreen(
                        assessmentLevel = recommendedAssessmentLevel,
                        assessmentRecommendedCategory = assessmentRecommendedCategory,
                        assessmentLevelBgColor
                    )
                    PagerScreen.SCREEN_SUMMARY_GRAPH -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                stringResource(R.string.workout_summary_tc),
                                fontSize = 32.sp.computeSize(),
                                color = SPRINT8_BG_2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = screenHeight * 0.04f)
                                    .testTag("$baseTag:SummaryLabel:Text")
                            )
                            Text(
                                stringResource(R.string.sweat_score_tc),
                                fontSize = 22.sp.computeSize(),
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = SUMMARY_SCORE_COLOR,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp.computeSize())
                                    .testTag("$baseTag:SweatScoreLabel:Text")
                            )
                            SummaryBarGraphScreen(
                                sweatScoreList = sweatScoreList, numberOfSprints = numberOfSprints
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SummaryScoreComp(
                                    modifier = Modifier,
                                    screenWidth = screenWidth,
                                    screenHeight = screenHeight,
                                    sweatScore = peakSweatScore,
                                    scoreTitle = stringResource(R.string.best_tc)
                                )
                                SummaryScoreComp(
                                    modifier = Modifier,
                                    screenWidth = screenWidth,
                                    screenHeight = screenHeight,
                                    sweatScore = totalSweatScore,
                                    scoreTitle = stringResource(R.string.total_tc)

                                )
                                SummaryScoreComp(
                                    modifier = Modifier,
                                    screenWidth = screenWidth,
                                    screenHeight = screenHeight,
                                    sweatScore = averageSweatScore,
                                    scoreTitle = stringResource(R.string.average_tc)
                                )
                            }
                        }
                    }

                    PagerScreen.SCREEN_SUMMARY_DETAIL -> WorkoutSummaryDetailScreen(
                        fullStats,
                        screenHeight
                    )
                }
            }
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(vertical = 16.dp.computeSize()),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp.computeSize())
                            .size(10.dp.computeSize())
                            .then(
                                if (isSelected) {
                                    Modifier.background(SUMMARY_GRAPH_BAR_COLOR_1, shape = CircleShape)
                                } else {
                                    Modifier
                                        .background(Color.DarkGray, shape = CircleShape)
                                        .border(
                                            width = 1.dp.computeSize(),
                                            color = SUMMARY_GRAPH_BAR_COLOR_1,
                                            shape = CircleShape
                                        )
                                }
                            )
                    )
                }
            }
        }
    }
}


@AllDevicePreviews
@Composable
fun Sprint8SummaryScreenPreview(){
    Sprint8SummaryScreen(
        fullStats = listOf(),
        sweatScoreList = SweatScoreList(),
        totalSweatScore = "100",
        peakSweatScore = "50",
        averageSweatScore = "20",
        numberOfSprints = 5,
        isAssessmentVisible = false,
        recommendedAssessmentLevel = "3",
        assessmentRecommendedCategory = stringResource(R.string.beginner_tc),
        assessmentLevelBgColor = Color.Green
    )
}