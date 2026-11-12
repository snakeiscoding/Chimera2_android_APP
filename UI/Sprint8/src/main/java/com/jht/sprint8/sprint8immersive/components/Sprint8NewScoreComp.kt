package com.jht.sprint8.sprint8immersive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_2
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Composable function to display sweat score information for the Sprint8 workout.
 * Shows the total sweat score and the personal best score.
 * @param totalSweatScore
 * The total sweat score achieved in the workout. Defaults to an empty string.
 * @param personalBestScore
 * The user's personal best sweat score. Defaults to an empty string.
 * @param modifier
 * Modifier applied to the score component for layout, styling,
 * or positioning.
 * @param screenWidth
 * Width of the screen in Dp, used for responsive layout calculations.
 * @param screenHeight
 * Height of the screen in Dp, used for responsive layout calculations.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8NewScoreComp(
    totalSweatScore: String = "",
    personalBestScore: String = "",
    modifier: Modifier,
    screenWidth: Dp,
    screenHeight: Dp
) {
    val componentTag = "Sprint8NewScoreComp"
    Box(
        modifier = modifier
            .padding(
                start = 20.dp.computeSize(),
                bottom = 16.dp.computeSize(),
                top = 8.dp.computeSize()
            )
            .height(screenHeight * 0.12f)
            .width(screenWidth * 0.25f)
            .background(color = Color.Black, shape = RoundedCornerShape(12.dp.computeSize())) .semantics {
                testTagsAsResourceId = true
            }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp.computeSize()),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.total_score_tc),
                    fontSize = 22.sp.computeSize(),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = modifier.testTag("$componentTag:TotalScoreTitle:Text")
                )
                Text(
                    totalSweatScore,
                    fontSize = 32.sp.computeSize(),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = SUMMARY_GRAPH_BAR_COLOR_2,
                    modifier = modifier.testTag("$componentTag:TotalScoreTitle:Text")
                )
            }
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                     stringResource(R.string.personal_best_tc),
                    fontSize = 18.sp.computeSize(),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = modifier.testTag("$componentTag:PersonalBestTitle:Text")
                )
                Text(
                    personalBestScore,
                    fontSize = 18.sp.computeSize(),
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = modifier.testTag("$componentTag:PersonalBest:Text")
                )
            }

        }
    }
}

@AllDevicePreviews
@Composable
fun Sprint8NewScoreCompPreview(){
    Sprint8NewScoreComp(
        totalSweatScore = "100",
        personalBestScore = "50",
        modifier = Modifier,
        screenWidth = 1000.dp,
        screenHeight = 1000.dp
    )
}