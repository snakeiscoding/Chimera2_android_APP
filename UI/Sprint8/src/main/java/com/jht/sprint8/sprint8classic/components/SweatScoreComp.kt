package com.jht.sprint8.sprint8classic.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Displays the Sweat Score component based on sprint state.
 * @param screenWidth The width of the screen to scale layout properly.
 * @param sweatScore The current sweat score value to display.
 * @param sprintStateType The current sprint state (WarmUp, Sprint, Recovery, Cooldown).
 * @param sweatScoreList List of sweat scores for progress bar or visualization.
 */
@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun SweatScoreComp(
    modifier: Modifier = Modifier,
    screenWidth: Dp,
    sweatScore: Int,
    sweatScoreList: List<Float>
) {
    val cornerRadius = 12.dp.computeSize()
    val markerWidth = 4.dp.computeSize()
    val progressBarHeight = 20.dp.computeSize()
    val outerPadding = 12.dp.computeSize()
    val innerPadding = 8.dp.computeSize()
    val testTagPrefix = "Sprint8"
    val targetScore = sweatScoreList.lastOrNull()?.toInt() ?: 0
    val effectiveTarget = sweatScoreList.lastOrNull()?.toInt()?.takeIf { it > 0 } ?: 70
    val barMax =
        effectiveTarget / 0.7f // Dynamically calculate bar max.Target sweat score is always 70% of the max bar

    BoxWithConstraints(
        modifier
            .width(screenWidth * 788 / 1600f)
            .wrapContentHeight()
            .semantics { testTagsAsResourceId = true }
            .background(
                DynamicTheme.colors.sprintInactive, shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        val progressBarWidth = maxWidth

        Column(modifier = Modifier.padding(20.dp.computeSize(), outerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        modifier = Modifier.testTag("$testTagPrefix:SweatIcon:Image"),
                        painter = painterResource(id = R.drawable.ic_sweat_front_diamond_inverted),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(5.dp.computeSize()))
                    Text(
                        modifier = Modifier.testTag("$testTagPrefix:SweatScoreTitle:Text"),
                        text = stringResource(R.string.sweat_score_tc),
                        color = DynamicTheme.colors.progressTextDefault,
                        style = TextStyle(fontSize = 16.sp.computeSize()),
                        fontFamily = FontFamily.SansSerif
                    )
                }
                if (targetScore != 0) Text(
                    modifier = Modifier.testTag("$testTagPrefix:TargetScore:Text"),
                    text = "${stringResource(R.string.target_score_tc)} $targetScore",
                    color = DynamicTheme.colors.progressTextDefault,
                    style = TextStyle(fontSize = 16.sp.computeSize()),
                    fontFamily = FontFamily.SansSerif
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = innerPadding)
                    .fillMaxWidth()
                    .height(progressBarHeight)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(
                        DynamicTheme.colors.sprintActive.copy(alpha = 0.6f)
                    )
            ) {
                //Progress Fill
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(progressBarWidth * (sweatScore / barMax).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(DynamicTheme.colors.progressBarFilled)
                ) {
                    if (sweatScore >= 1) {
                        Text(
                            text = sweatScore.toString(),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = innerPadding)
                                .testTag("$testTagPrefix:ProgressPercentage:Text"),
                            color = DynamicTheme.colors.text2,
                            style = TextStyle(fontSize = 14.sp.computeSize()),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
                //Target progress
                if (targetScore != 0) Box(
                    modifier = Modifier
                        .offset(x = progressBarWidth * 0.7f - markerWidth / 2)
                        .width(markerWidth)
                        .fillMaxHeight()
                        .background(
                            DynamicTheme.colors.sprintInactive
                        )
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun SweatScoreCompPreview() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter)
    {
        DynamicTheme {
            SweatScoreComp(
                modifier = Modifier,
                screenWidth = screenWidth,
                sweatScore = 17,
                sweatScoreList = listOf(28f, 31f, 34f)
            )
        }
    }
}

