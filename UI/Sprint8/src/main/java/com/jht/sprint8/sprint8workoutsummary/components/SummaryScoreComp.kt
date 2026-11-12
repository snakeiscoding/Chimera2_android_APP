package com.jht.sprint8.sprint8workoutsummary.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
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
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_LIST_ITEM_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_SCORE_COLOR
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Composable function to display a summary score card.
 * @param modifier Modifier to be applied to the summary score component for layout, padding, or styling.
 * @param screenWidth The width of the screen in Dp, used for responsive layout calculations.
 * @param screenHeight The height of the screen in Dp, used for responsive layout calculations.
 * @param sweatScore The score value to display, typically as a String (e.g., "85").
 * @param scoreTitle The title or label of the score being displayed (e.g., "Best", "Total", "Average").
 */

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SummaryScoreComp(
    modifier: Modifier,
    screenWidth: Dp,
    screenHeight: Dp,
    sweatScore: String,
    scoreTitle: String
) {
    val componentTag = "SummaryScoreComp"
    Box(
        modifier = modifier
            .width(screenWidth * 0.286f)
            .height(screenHeight * 0.256f)
            .background(
                color = CUSTOM_WORKOUT_LIST_ITEM_COLOR,
                shape = RoundedCornerShape(12.dp.computeSize())
            )
            .semantics {
                testTagsAsResourceId = true
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = scoreTitle,
                fontSize = 20.sp.computeSize(),
                color = SUMMARY_SCORE_COLOR,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Normal,
                fontFamily = FontFamily.SansSerif,
                modifier = modifier
                    .wrapContentSize()
                    .testTag("$componentTag:$scoreTitle:Title")
            )
            Row(
                modifier =modifier.wrapContentSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sweatScore,
                    fontSize = 54.sp.computeSize(),
                    color = SUMMARY_GRAPH_BAR_COLOR_1,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.SansSerif,
                    modifier = modifier
                        .wrapContentSize()
                        .testTag("$componentTag:$scoreTitle:Value")

                )
                Image(
                    painter = painterResource(R.drawable.ic_trophy),
                    contentDescription = null,
                    modifier = modifier
                        .size(40.dp.computeSize())
                        .padding(start = 8.dp.computeSize())
                        .testTag("$componentTag:TrophyIcon:Image"),
                    colorFilter = ColorFilter.tint(SUMMARY_GRAPH_BAR_COLOR_1)
                )
            }
            Text(
                text = stringResource(R.string.new_best_tc),
                fontSize = 16.sp.computeSize(),
                color = SUMMARY_GRAPH_BAR_COLOR_1,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.SansSerif,
                modifier = modifier
                    .wrapContentSize()
                    .testTag("$componentTag:NewBestLabel:Text")
            )
        }

    }
}

@AllDevicePreviews
@Composable
fun SummaryScoreCompPreview(){
    SummaryScoreComp(
        modifier = Modifier,
        screenWidth = 1000.dp,
        screenHeight = 1000.dp,
        sweatScore = "9",
        scoreTitle = stringResource(R.string.best_tc)
    )
}