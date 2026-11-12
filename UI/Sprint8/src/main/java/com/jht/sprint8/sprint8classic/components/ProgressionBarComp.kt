package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import kotlin.math.round

/**
 * Represents the current phase of the Sprint8 program.
 */
enum class SprintStateType {
    WarmUp, Sprint, Recovery, Cooldown
}

/**
 * Displays the full Sprint8 progression bar with steps and scores.
 *
 * @param modifier Modifier to be applied to the ProgressionBar container.
 * @param currentStep The current index of progress in the Sprint8 routine.
 * @param sprintStateType The current workout state (e.g., Sprint, WarmUp).
 * @param sweatScoreList List of sweat scores collected during sprint intervals.
 * @param totalSprints Total number of steps shown in the progression bar..
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ProgressionBarComp(
    modifier: Modifier,
    currentStep: Int,
    sprintStateType: SprintStateType,
    sweatScoreList: List<Float>,
    totalSprints: Int,
) {
    val testTagPrefix = "Sprint8"
    val totalSteps = (totalSprints * 2) + 1
    Surface(
        modifier
            .padding(10.dp.computeSize())
            .wrapContentHeight()
            .wrapContentWidth()
            .semantics { testTagsAsResourceId = true },
        shape = RoundedCornerShape(15.dp.computeSize()),
        tonalElevation = 10.dp,
        shadowElevation = 10.dp,
    ) {
        LazyRow {
            items(totalSteps) { index ->
                val isCurrentSprintStarted = index < currentStep
                val isCurrentSprintCompleted = (index + 1) < currentStep
                //Computes sprint text value based on position
                val displayValue = if (index % 2 != 0 && index / 2 < sweatScoreList.size) {
                    round(sweatScoreList[index / 2]).toInt()
                } else {
                    0
                }

                TextAndDotItem(
                    isCurrentSprintStarted,
                    isCurrentSprintCompleted,
                    index,
                    index % 2 == 0,
                    displayValue,
                    totalSteps,
                    sprintStateType, testTagPrefix
                )
            }
        }
    }
}

//Item UI for sprint progress
@Composable
internal fun TextAndDotItem(
    isCurrentSprintStarted: Boolean,
    isCurrentSprintCompleted: Boolean,
    index: Int,
    isEven: Boolean,
    displayValue: Int,
    totalSteps: Int,
    sprintStateType: SprintStateType,
    componentTag: String
) {
    Box(
        modifier = Modifier
            .size(54.dp.computeSize())
            .background(
                when (sprintStateType) {
                    SprintStateType.WarmUp -> if (isCurrentSprintStarted) DynamicTheme.colors.progressPast else DynamicTheme.colors.sprint8border2
                    SprintStateType.Sprint -> if (isCurrentSprintStarted) DynamicTheme.colors.sprintActive else DynamicTheme.colors.sprintInactive
                    SprintStateType.Recovery -> if (isCurrentSprintStarted) DynamicTheme.colors.recoveryActive else DynamicTheme.colors.recoveryInactive
                    SprintStateType.Cooldown -> if (isCurrentSprintStarted) DynamicTheme.colors.cooldownActive else DynamicTheme.colors.cooldownInactive
                }
            ),
        contentAlignment = Alignment.Center,

        ) {
        if (isEven) {
            Row {
                if (index == 0 || index == (totalSteps - 1)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp.computeSize())
                            .clip(CircleShape)
                            .background(
                                if (isCurrentSprintStarted) DynamicTheme.colors.progressTextDefault else DynamicTheme.colors.progressTextDefault.copy(
                                    alpha = 0.3f
                                )
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp.computeSize())
                        .clip(CircleShape)
                        .background(
                            if (isCurrentSprintStarted) DynamicTheme.colors.progressTextDefault else DynamicTheme.colors.progressTextDefault.copy(
                                alpha = 0.3f
                            )
                        )
                )
            }
        } else {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.ic_sweat_score),
                    contentDescription = "Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp.computeSize())
                        .alpha(if (isCurrentSprintCompleted) 1f else 0f)
                        .testTag("$componentTag:SweatScoreBg:Image"),
                )
                Text(
                    modifier = Modifier.testTag("$componentTag:SweatScore$index:Text"),
                    text = displayValue.toString(),
                    color = if (isCurrentSprintCompleted) DynamicTheme.colors.textSweatScore else if (isCurrentSprintStarted) DynamicTheme.colors.progressTextDefault else DynamicTheme.colors.progressTextDefault.copy(
                        alpha = 0.3f
                    ),
                    fontSize = if (isCurrentSprintCompleted) 16.sp.computeSize() else 26.sp.computeSize(),
                    fontFamily = FontFamily.SansSerif,
                )
            }
        }
    }
}

@Composable
@AllDevicePreviews
private fun ProgressbarCompPreview(){
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
        ProgressionBarComp(
            modifier = Modifier,
            currentStep = 7,
            sprintStateType = SprintStateType.WarmUp,
            sweatScoreList = listOf(110f, 120f, 130f, 140f),
            totalSprints = 8
        )
    }
}




