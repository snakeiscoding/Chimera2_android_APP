package com.jht.sprint8.sprint8immersive.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_LIGHT_BLUE
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_2
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Composable function to display the user's sweat score during a Sprint8 workout.
 *
 * @param sweatScore
 * The current sweat score value to display. Defaults to 0.
 * @param modifier
 * Modifier applied to the component for layout, styling,
 * or positioning.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CurrentSweatScoreComp(
    sweatScore: Int = 0,
    modifier: Modifier
) {
    val componentTag = "CurrentSweatScoreComp"
    Column(modifier = Modifier.semantics {
        testTagsAsResourceId = true
    }) {
        Text(
            text = sweatScore.toString(),
            fontSize = 72.sp.computeSize(),
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            color = SUMMARY_GRAPH_BAR_COLOR_2,
            modifier = modifier.testTag("$componentTag:CurrentSweatScore:Text")
        )
        Text(
            text = stringResource(R.string.current_sweat_score_tc),
            fontSize = 17.sp.computeSize(),
            fontFamily = FontFamily.SansSerif,
            fontStyle = FontStyle.Normal,
            fontWeight = FontWeight.Bold,
            color = SPRINT8_NEW_LIGHT_BLUE,
            modifier = modifier
                .offset(y = -20.dp.computeSize())
                .testTag("$componentTag:CurrentSweatScoreTitle:Text")
        )
    }
}

@AllDevicePreviews
@Composable
fun CurrentSweatScoreCompPreview(){
    CurrentSweatScoreComp(
        sweatScore = 9,
        modifier = Modifier
    )
}