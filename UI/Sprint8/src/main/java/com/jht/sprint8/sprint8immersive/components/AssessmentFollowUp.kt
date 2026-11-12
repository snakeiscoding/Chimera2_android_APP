package com.jht.sprint8.sprint8immersive.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_DIALOG_BUTTON_BG_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_DIALOG_BUTTON_BG_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BTN_CONTENT_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.TEXT_COLOR_1
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

private typealias NextValue = String
private typealias ValueUnit = String

internal data class NextLevelAttributes(
    val nextLevelName: String,
    val attributes: List<Pair<NextValue, ValueUnit>>
)
/**
 * Composable function to display the assessment follow-up UI.
 *
 * @param modifier
 * Modifier applied to the follow-up component for layout, styling,
 * or positioning.
 * @param levelDirection
 * Direction indicating how the user's workout level should change,
 * represented by [LevelDirection] (e.g., increase, decrease, or stay).
 * @param nextLevelAttributes
 * Attributes of the next recommended workout level, such as
 * speed, incline, or resistance values.
 * @param baseTestTag
 * Base test tag used for UI testing to uniquely identify
 * follow-up UI elements.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AssessmentFollowUp(
    modifier: Modifier = Modifier,
    levelDirection: LevelDirection,
    nextLevelAttributes: NextLevelAttributes,
    baseTestTag: String
) {
    Column(
        modifier = modifier
            .wrapContentSize()
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(levelDirection.levelText),
            fontSize = 24.sp.computeSize(1920f),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = levelDirection.textColor
        )
        Spacer(
            modifier = Modifier.size(16.dp.computeSizeHd())
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp.computeSizeHd())) {
            nextLevelAttributes.attributes.forEach { attributes ->
                ParameterBox(levelDirection, attributes, baseTestTag)
            }
        }
        Spacer(
            modifier = Modifier.size(10.dp.computeSizeHd())
        )
        Text(
            nextLevelAttributes.nextLevelName,
            fontSize = 18.sp.computeSize(1920f),
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = levelDirection.textColor,
            modifier = Modifier.testTag("$baseTestTag:NextLevelName:Text")
        )
    }
}

@Composable
private fun ParameterBox(
    levelDirection: LevelDirection,
    parameter: Pair<NextValue, ValueUnit>,
    baseTestTag: String
) {
    Box(
        modifier = Modifier
            .size(width = 130.dp.computeSizeHd(), height = 120.dp.computeSizeHd())
            .background(
                color = levelDirection.backgroundColor,
                shape = RoundedCornerShape(corner = CornerSize(12.dp.computeSizeHd()))
            ), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                parameter.first,
                fontSize = 48.sp.computeSize(1920f),
                color = levelDirection.textColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("$baseTestTag:NextValue:Text")
            )
            Text(
                parameter.second,
                fontSize = 18.sp.computeSize(1920f),
                color = levelDirection.textColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("$baseTestTag:NextValueUnit:Text")
            )
        }
    }
}

enum class LevelDirection(
    @StringRes val levelText: Int,
    val textColor: Color,
    val backgroundColor: Color
) {
    LEVEL_UP(
        R.string.leveling_up_tc,
        SUMMARY_GRAPH_BAR_COLOR_1,
        ASSESSMENT_DIALOG_BUTTON_BG_COLOR_1
    ),
    LEVEL_DOWN(
        R.string.leveling_down_tc,
        SUMMARY_GRAPH_BAR_COLOR_2,
        ASSESSMENT_DIALOG_BUTTON_BG_COLOR_2
    ),
    STAY_HERE(
        R.string.you_got_it_tc,
        SPRINT8_BTN_CONTENT_COLOR_2,
        TEXT_COLOR_1
    )
}

@AllDevicePreviews
@Composable
private fun AssessmentFollowUpPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 100.dp.computeSizeHd()),
        contentAlignment = Alignment.Center
    ) {
        AssessmentFollowUp(
            modifier = Modifier.align(alignment = Alignment.TopCenter),
            levelDirection = LevelDirection.LEVEL_DOWN,
            nextLevelAttributes = NextLevelAttributes(
                "Advanced 11",
                listOf("5.0" to "%", "7.1" to "mph")
            ),
            baseTestTag = "Sprint8Plus"
        )
        AssessmentFollowUp(
            modifier = Modifier.align(alignment = Alignment.Center),
            levelDirection = LevelDirection.STAY_HERE,
            nextLevelAttributes = NextLevelAttributes(
                "Advanced 11",
                listOf("5.0" to "%", "7.1" to "mph")
            ),
            baseTestTag = "Sprint8Plus"
        )
        AssessmentFollowUp(
            modifier = Modifier.align(alignment = Alignment.BottomCenter),
            levelDirection = LevelDirection.LEVEL_UP,
            nextLevelAttributes = NextLevelAttributes(
                "Advanced 11",
                listOf("5.0" to "%", "7.1" to "mph")
            ),
            baseTestTag = "Sprint8Plus"
        )
    }
}
