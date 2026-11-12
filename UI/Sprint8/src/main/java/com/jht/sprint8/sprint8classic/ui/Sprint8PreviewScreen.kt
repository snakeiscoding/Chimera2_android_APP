package com.jht.sprint8.sprint8classic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jht.enums.FrameType
import com.jht.sprint8.R
import com.jht.sprint8.Sprint8ViewModel
import com.jht.sprint8.formatMinMaxText
import com.jht.sprint8.formatValue
import com.jht.sprint8.sprint8classic.components.PreviewBarGraphComponent
import com.jht.sprint8.sprint8classic.components.PreviewLabelComp
import com.jht.sprint8.sprint8classic.components.Sprint8LevelSelectionButtonGroup
import com.jht.sprint8.utils.Constants.SPRINT8_CUSTOM_WORKOUT
import com.jht.sprint8.utils.Sprint8Colors.COOLDOWN_CLOCK_ICON_COLOR
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_LIST_ITEM_COLOR
import com.jht.sprint8.utils.Sprint8Colors.RECOVERY_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_1
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_12
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.TEXT_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_ICON_BORDER_COLOR
import com.jht.theme.DynamicTheme
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize

/**
 * Composable screen that previews a Sprint8 workout before starting.
 *
 * This screen allows users to view workout parameters, select custom
 * workouts, and start a predefined workout. It interacts with the provided
 * ViewModel for state management.
 *
 * @param startSelectedWorkout Callback invoked when the user starts the selected workout.
 * @param onCustomWorkoutStart Callback invoked when the user starts a custom workout,
 * providing speed/resistance data and incline data. Defaults to a no-op lambda.
 * @param viewModel [Sprint8ViewModel] providing data and state for the preview screen.
 * Defaults to a Hilt-injected ViewModel.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8PreviewScreen(
    navController: NavController,
    startSelectedWorkout: () -> Unit,
    onCustomWorkoutStart: (speedOrResistanceData: List<Float>, inclineData: List<Float>) -> Unit = { _, _ -> },
    viewModel: Sprint8ViewModel = hiltViewModel()
) {

    val frameType = viewModel.frameType.value
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Collect StateFlow values for automatic UI updates
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val currentLevelData by viewModel.currentLevelData.collectAsStateWithLifecycle()
    val workoutStats by viewModel.workoutStats.collectAsStateWithLifecycle()

    // Get available levels for current category
    val availableLevels = viewModel.getAvailableLevels()
    val displayLevel = viewModel.getDisplayLevel()

    val experienceButtonColors = listOf(
        SPRINT8_BG_12,
        COOLDOWN_CLOCK_ICON_COLOR,
        WARMUP_ICON_BORDER_COLOR,
        SPRINT_ICON_BORDER_COLOR,
        RECOVERY_ICON_BORDER_COLOR
    )
    val levelsButtonColors = List(5) { SPRINT8_BG_12 }
    val xAxisPointers = (1 until 9).toList().map { it.toString() }
    val yAxisPoints = if (frameType == FrameType.Treadmill) {
        (15 downTo 3 step 3).toList().map { it.toString() }
    } else {
        (25 downTo 5 step 5).toList().map { it.toString() }
    }
    val levelsList = availableLevels?.map { it.toString() }
    val categoryList = viewModel.categoryList
    val hasFrameControl = viewModel.hasFrameControl.value

    Row(
        modifier = Modifier
            .background(
                SPRINT8_BG_1,
            )
            .pointerInput(Unit) {
                detectTapGestures {}
            }
    ) {
        Row(
            modifier = Modifier
                .clickable { navController.popBackStack() }
                .testTag("Sprint8Preview:Back:Image")
                .wrapContentSize()
                .padding(start = screenWidth * 0.017f, top = screenHeight * 0.062f,)
                .scale(0.95f),

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(painterResource(R.drawable.ic_back), contentDescription = null)
            Text(
                text = stringResource(R.string.back_tc),
                style = MaterialTheme.typography.titleSmall,
                )
        }
        Box(
            modifier = Modifier
                .padding(end = screenWidth * 0.030f)
                .scale(0.95f)
        ) {
            val customStatInfo = remember(frameType) { viewModel.getCustomStatMinMax(frameType) }
            if (!hasFrameControl) {
                ManualResistanceWorkoutScreen(
                    onCustomWorkoutStart = onCustomWorkoutStart
                )
            } else if (selectedCategory == SPRINT8_CUSTOM_WORKOUT) {
                Box(
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clickableNoEffect(onClick = {})
                        .padding(
                            top = screenHeight * 0.032f,
                            end = screenWidth * 0.028f,
                            bottom = screenHeight * 0.032f
                        )
                        .background(
                            color = CUSTOM_WORKOUT_LIST_ITEM_COLOR,
                            shape = RoundedCornerShape(32.dp.computeSize())
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(){
                            Sprint8LevelSelectionButtonGroup(
                                categoryList,
                                modifier = Modifier,
                                buttonColors = experienceButtonColors,
                                screenWidth = screenWidth * 0.160f,
                                screenHeight = screenHeight,
                                selectedIndex = selectedCategory,
                                onLevelSelected = { categoryIndex ->
                                    // temporarily restricting selection of custom workout
                                    viewModel.selectCategory(categoryIndex)
                                }
                            )
                        }

                        CustomWorkoutScreen(
                            frameType = frameType,
                            customStats = customStatInfo,
                            onCustomWorkoutStart = onCustomWorkoutStart
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .fillMaxSize()
                        .background(
                            SPRINT8_BG_1,
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.9f)
                            .padding(
                                top = screenHeight * 0.032f,
                                end = screenWidth * 0.028f,
                                bottom = screenHeight * 0.032f
                            )
                            .background(
                                color = CUSTOM_WORKOUT_LIST_ITEM_COLOR,
                                shape = RoundedCornerShape(32.dp.computeSize())
                            )
                            .align(Alignment.TopCenter)
                    ) {
                        Sprint8LevelSelectionButtonGroup(
                            categoryList,
                            modifier = Modifier.align(
                                Alignment.TopCenter
                            ),
                            buttonColors = experienceButtonColors,
                            screenWidth = screenWidth * 0.160f,
                            screenHeight = screenHeight,
                            selectedIndex = selectedCategory,
                            onLevelSelected = { categoryIndex ->
                                // temporarily restricting selection of custom workout
                                viewModel.selectCategory(categoryIndex)
                            }
                        )
                        if (!levelsList.isNullOrEmpty() && displayLevel != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        TEXT_COLOR_1, shape = RoundedCornerShape(
                                            bottomStart = screenHeight * 0.032f,
                                            bottomEnd = screenHeight * 0.032f
                                        )
                                    )
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = screenHeight * 0.072f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.levels_tc),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp.computeSize()),
                                    color = DynamicTheme.colors.text1,
                                )
                                Sprint8LevelSelectionButtonGroup(
                                    levels = levelsList,
                                    buttonColors = levelsButtonColors,
                                    modifier = Modifier,
                                    screenWidth = screenWidth * 0.120f,
                                    screenHeight = screenHeight,
                                    selectedIndex = displayLevel - 1, // Convert to 0-based index
                                    onLevelSelected = { levelIndex ->
                                        viewModel.setLevelByDisplayIndex(levelIndex + 1) // Convert to 1-based index
                                    }
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(screenHeight * 0.8f)
                                .padding(top = screenHeight * 0.15f)
                                .align(Alignment.TopCenter),
                        ) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally),
                                contentPadding = PaddingValues(horizontal = 32.dp.computeSize()),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(xAxisPointers) {
                                    Text(
                                        text = it,
                                        fontSize = 16.sp.computeSize(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.SansSerif,
                                        color = DynamicTheme.colors.text1,
                                        modifier = Modifier.padding(horizontal = 54.dp.computeSize())
                                    )
                                }
                            }
//                            Temporarily removing the horizontal guidelines and y-axis points for HFA Trade Show
//                            yAxisPoints.forEach { label ->
//                                Sprint8PreviewGraphLine(label, screenWidth, screenHeight, frameType)
//                            }
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(
                                    start = screenWidth * 0.032f,
                                    bottom = screenHeight * 0.024f
                                )
                        ) {
                            currentLevelData?.let {
                                PreviewBarGraphComponent(
                                    workoutData = it,
                                    frameType = frameType
                                )
                            }
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = screenWidth * 0.047f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(bottom = screenHeight * 0.004f)
                                ) {
                                    // Display workout stats dynamically
                                    PreviewLabelComp(
                                        modifier = Modifier.padding(end = screenWidth * 0.008f),
                                        title = stringResource(R.string.warmup_tc),
                                        speedOrResistanceValue = if (frameType == FrameType.Treadmill) {
                                            formatValue(workoutStats.warmup)
                                                .plus(" ${viewModel.speedLabel}")
                                        } else {
                                            stringResource(R.string.resistance_tc) + " " + formatValue(
                                                workoutStats.warmup
                                            )
                                        },
                                        inclineValue = "%.1f%% ${stringResource(R.string.incline_tc)}".format(
                                            workoutStats.warmupIncline
                                        ),
                                        backgroundColor = WARMUP_ICON_BORDER_COLOR,
                                        frameType = frameType
                                    )
                                    PreviewLabelComp(
                                        modifier = Modifier.padding(end = screenWidth * 0.008f),
                                        title = stringResource(R.string.sprint_tc),
                                        speedOrResistanceValue = if (frameType == FrameType.Treadmill) {
                                            formatMinMaxText(
                                                workoutStats.sprintMin, workoutStats.sprintMax
                                            ).plus(" ${viewModel.speedLabel}")
                                        } else {
                                            stringResource(R.string.resistance_tc) + " " + formatMinMaxText(
                                                workoutStats.sprintMin, workoutStats.sprintMax
                                            )
                                        }, inclineValue = formatMinMaxText(
                                            workoutStats.sprintInclineMin, workoutStats.sprintInclineMax
                                        ) + "% ${stringResource(R.string.incline_tc)}",
                                        backgroundColor = SPRINT_ICON_BORDER_COLOR,
                                        frameType = frameType
                                    )
                                    PreviewLabelComp(
                                        modifier = Modifier.padding(end = screenWidth * 0.008f),
                                        title = stringResource(R.string.recovery_tc),
                                        speedOrResistanceValue = if (frameType == FrameType.Treadmill) {
                                            formatMinMaxText(
                                                workoutStats.recoveryMin, workoutStats.recoveryMax
                                            ).plus(" ${viewModel.speedLabel}")
                                        } else {
                                            stringResource(R.string.resistance_tc) + " " + formatMinMaxText(
                                                workoutStats.recoveryMin, workoutStats.recoveryMax
                                            )
                                        },
                                        inclineValue = formatMinMaxText(
                                            workoutStats.recoveryInclineMin, workoutStats.recoveryInclineMax
                                        ) + "% ${stringResource(R.string.incline_tc)}",
                                        backgroundColor = RECOVERY_ICON_BORDER_COLOR,
                                        frameType = frameType
                                    )
                                    PreviewLabelComp(
                                        modifier = Modifier.padding(end = screenWidth * 0.008f),
                                        title = stringResource(R.string.cooldown_tc),
                                        speedOrResistanceValue = if (frameType == FrameType.Treadmill) {
                                            formatValue(workoutStats.cooldown)
                                                .plus(" ${viewModel.speedLabel}")
                                        } else {
                                            stringResource(R.string.resistance_tc) + " " + formatValue(
                                                workoutStats.cooldown
                                            )
                                        },
                                        inclineValue = "%.1f%% ${stringResource(R.string.incline_tc)}".format(
                                            workoutStats.cooldownIncline
                                        ),
                                        backgroundColor = COOLDOWN_CLOCK_ICON_COLOR,
                                        frameType = frameType
                                    )
                                }

                                viewModel.getTotalWorkoutTime()?.let {
                                    Text(
                                        text = it,
                                        fontSize = 20.sp.computeSize(),
                                        color = DynamicTheme.colors.text1,
                                        fontFamily = FontFamily.SansSerif,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(
                                                horizontal = screenWidth * 0.085f
                                            )
                                    )
                                }
                            }

                        }
                    }
                    Row(modifier = Modifier.align(Alignment.BottomCenter)) {
                        StartWorkoutButton(startSelectedWorkout)
                    }
                }
            }
        }
    }
}

@Composable
internal fun StartWorkoutButton(
    onClick: () -> Unit,
) {
    Button(
        onClick = { onClick() },
        shape = RoundedCornerShape(18.dp.computeSize()),
        colors = ButtonDefaults.buttonColors(
            containerColor = WARMUP_ICON_BORDER_COLOR
        ),
        contentPadding = PaddingValues(
            vertical = 14.dp.computeSize(),
            horizontal = 20.dp.computeSize()
        ),
        modifier = Modifier
            .wrapContentWidth()
            .height(54.dp.computeSize())
            .testTag("Sprint8Preview:StartWorkout:Button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.start_workout_tc),
                color = SPRINT8_BG_1,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 24.sp.computeSize(),
                    fontWeight = FontWeight.Bold
                ),
            )
            Spacer(modifier = Modifier.width(4.dp.computeSize()))
            Image(
                painter = painterResource(R.drawable.ic_double_right_arrow),
                contentDescription = null,
                modifier = Modifier.size(32.dp.computeSize())
            )
        }
    }
}