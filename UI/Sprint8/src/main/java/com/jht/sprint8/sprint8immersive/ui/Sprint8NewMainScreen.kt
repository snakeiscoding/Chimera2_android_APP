package com.jht.sprint8.sprint8immersive.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.data.FrameControl
import com.jht.data.FrameControlCollection
import com.jht.data.FrameControlLocation
import com.jht.data.MockFrameControl
import com.jht.data.RangeInfo
import com.jht.enums.WorkoutStage
import com.jht.sprint8.R
import com.jht.sprint8.model.AssessmentButtonConfig
import com.jht.sprint8.model.GetReadyConfig
import com.jht.sprint8.model.Sprint8DataConfig
import com.jht.sprint8.model.Sprint8UiState
import com.jht.sprint8.sprint8classic.components.FullScreenButton
import com.jht.sprint8.sprint8classic.components.Sprint8KeyPadButton
import com.jht.sprint8.sprint8classic.components.Sprint8PillboxButton
import com.jht.sprint8.sprint8immersive.components.AssessmentDialog
import com.jht.sprint8.sprint8immersive.components.AssessmentFollowUp
import com.jht.sprint8.sprint8immersive.components.CurrentSweatScoreComp
import com.jht.sprint8.sprint8immersive.components.Sprint8CadenceModal
import com.jht.sprint8.sprint8immersive.components.NewGetReadyComp
import com.jht.sprint8.sprint8immersive.components.NextLevelAttributes
import com.jht.sprint8.sprint8immersive.components.Sprint8NewForwardMotionCompGL
import com.jht.sprint8.sprint8immersive.components.Sprint8NewRpmComponent
import com.jht.sprint8.sprint8immersive.components.Sprint8NewScoreComp
import com.jht.sprint8.sprint8immersive.components.Sprint8NewSkipButton
import com.jht.sprint8.sprint8immersive.components.Sprint8NewTimer
import com.jht.sprint8.sprint8immersive.components.UpcomingSprintIndicatorComp
import com.jht.sprint8.utils.Constants.MAX_RPM
import com.jht.sprint8.utils.Constants.NUM_LANES
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_ICON_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT_PROGRESSION_BAR_COLOR
import com.jht.sprint8.keypad.Sprint8NumPadEntry
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.widgets.PopupFullscreen
import kotlin.math.round

private const val MAX_INTERVALS = 8

/**
 * Main screen composable for the Sprint 8 Plus/New.
 * @param rpmValue Current RPM value to be displayed.
 * @param spmValue Current cadence value (steps per minute) to be displayed.
 * @param keyPadConfigList List of keypad configurations used for user input.
 * @param sprint8DataConfig Configuration object for Sprint 8 data presentation.
 * @param getReadyConfig Configuration for the "Get Ready" state and visuals.
 * @param uiState Current UI state of the Sprint 8 screen.
 * @param isRpmVisible Whether the RPM value should be visible on the UI.
 * @param isCadenceVisible Whether the cadence modal should be shown instead of RPM. Cadence and
 * RPM are mutually exclusive and share the same UI slot; this takes precedence over [isRpmVisible].
 * @param onSkipClick Callback invoked when the user taps the skip button.
 * @param onFullscreenClick Callback invoked when fullscreen mode is toggled.
 * @param onPlayPauseButtonClick Callback invoked when the play/pause button is pressed.
 * @param assessmentButtonConfig Configuration list for assessment action buttons.
 * @param isAssessmentVisible Whether assessment buttons should be visible.
 * @param getLevelName Function that returns a level name for a given level index.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8NewMainScreen(
    rpmValue: Int,
    spmValue: Int = 0,
    frameControls: FrameControlCollection = FrameControlCollection(),
    sprint8DataConfig: Sprint8DataConfig = Sprint8DataConfig(),
    getReadyConfig: GetReadyConfig = GetReadyConfig(),
    uiState: Sprint8UiState,
    isRpmVisible: Boolean,
    isCadenceVisible: Boolean = false,
    onSkipClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onPlayPauseButtonClick: () -> Unit,
    assessmentButtonConfig: List<AssessmentButtonConfig>,
    isAssessmentVisible: Boolean,
    getLevelName: (Int) -> String
) {
    val baseTag = "Sprint8"
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val visibleKeypadConfig = remember { mutableStateOf<FrameControl?>(null) }

    // should this be in VM? or confined to the composable that actually needs it?
    val isAssessmentWindow = isAssessmentVisible && (uiState.workoutStage == WorkoutStage.WORKOUT &&
            !uiState.isSprinting &&
            uiState.remainingTime >= 60_000f)
    val shouldShowAssessmentDialog = isAssessmentWindow && !uiState.hasAssessmentBeenAnswered
    val shouldShowAssessmentFollowUp = isAssessmentVisible && uiState.showAssessmentFollowUp
    val nextLevelAttributes = NextLevelAttributes(
        nextLevelName = getLevelName(uiState.workoutLevel),
        attributes = frameControls.controls.map { it.nextValue.value to it.keypadUnit.value }
    )
    val shouldShowControls = if(isAssessmentVisible) !(shouldShowAssessmentDialog || shouldShowAssessmentFollowUp) else true
    var showKeypad by remember { mutableStateOf(false) }
    var isCadenceModalDismissed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            //.preferredFrameRate(24f)
            .fillMaxSize()
            .background(Color.Black)
            .semantics {
                testTagsAsResourceId = true
            }) {
        // TOP HALF SCREEN
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
        ) {
            Image(
                painter = painterResource(sprint8DataConfig.sprint8NewImage),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("$baseTag:NewSprint8Image:Image"),
                contentScale = ContentScale.Crop,
            )
            if (isCadenceVisible) {
                Sprint8CadenceModal(
                    spm = spmValue,
                    isHidden = isCadenceModalDismissed,
                    onDismiss = { isCadenceModalDismissed = true },
                    onReopen = { isCadenceModalDismissed = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp.computeSize())
                )
            } else if (isRpmVisible) {
                Sprint8NewRpmComponent(
                    rpm = rpmValue,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp.computeSize()),
                    screenWidth,
                    screenHeight,
                    maxRpm = MAX_RPM
                )
            }
            if (uiState.isSprinting) {
                CurrentSweatScoreComp(
                    sweatScore = uiState.sweatScore,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp.computeSize(), start = 20.dp.computeSize())
                )
                Sprint8NewSkipButton(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onSkipClick = onSkipClick
                )
            }

            // sweat tracking block
            SweatScoreStatBlock(
                sweatScores = uiState.sweatScoreParcel.sweatScores,
                numberOfSprints = uiState.numberOfSprints,
                sprintCount = uiState.sprintCount,
                isRunning = uiState.isRunning,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                modifier = Modifier.align(Alignment.BottomStart),
            )

            // center UI block - stage / time / pause
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                NewGetReadyComp(
                    getReadyConfig = getReadyConfig,
                    screenWidth = screenWidth,
                )
                Sprint8NewTimer(
                    modifier = Modifier.padding(top = 12.dp.computeSize()),
                    title = sprint8DataConfig.workoutTitle,
                    color = sprint8DataConfig.sprint8NewColors,
                    remainingTime = uiState.remainingTime,
                    isFullScreen = uiState.isFullScreen
                )
                    Icon(
                        painter = painterResource(id = if (uiState.isRunning) R.drawable.pause_new_filled else R.drawable.baseline_play_arrow_24),
                        contentDescription = if (uiState.isRunning) "Pause" else "Play",
                        modifier = Modifier
                            .size(42.dp.computeSize())
                            .aspectRatio(1F)
                            .testTag("${baseTag}:PlayPause:Button")
                            .clickable { onPlayPauseButtonClick.invoke() },
                        tint = sprint8DataConfig.sprint8NewColors
                    )
            }
        }
        // BOTTOM HALF SCREEN
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomStart)
        ) {
            Sprint8NewForwardMotionCompGL(
                modifier = Modifier.align(Alignment.Center),
                sprint8NewColor = sprint8DataConfig.sprint8NewColors,
                numLanes = NUM_LANES,
                runningSpeed = sprint8DataConfig.sprint8NewForwardMotionSpeed,
                isRunning = uiState.isRunning
            )

            frameControls.controls.forEachIndexed { index, control ->
                val imageAlignment = if (control.location == FrameControlLocation.Left) Alignment.TopEnd else Alignment.TopStart
                val newAlignment = if (control.location == FrameControlLocation.Left) Alignment.TopStart else Alignment.TopEnd

                @Composable
                fun PillboxButton() {
                    if (uiState.isFullScreen) Sprint8PillboxButton(
                        modifier = Modifier,
                        screenHeight = screenHeight * 0.9f,
                        screenWidth = screenWidth * 0.9f,
                        title = control.label.value,
                        value = control.currentValue.value,
                        unit = control.unit.value,
                        onPressIncrement = control::onIncrementPress,
                        onPressDecrement = control::onDecrementPress,
                        titleSize = with(LocalDensity.current) { (screenHeight * 19 / 1080f).toSp() },
                        valueSize = with(LocalDensity.current) { (screenHeight * 50 / 1080f).toSp() },
                        imageAlignment = imageAlignment,
                        dataSectionColor = Color.Transparent.copy(alpha = 0.5f),
                        buttonSectionColor = Color.Transparent.copy(alpha = 0.05f),
                        valueColor = Color.White,
                        titleColor = Color.White,
                        arrowIconColor = sprint8DataConfig.sprint8NewColors,
                        dotIconColor = Color.White.copy(alpha = 0.5f),
                        borderColor = Color.White.copy(alpha = 0.15f),
                        borderThickness = 2.dp.computeSize(),
                        onClick = { showKeypad = true; visibleKeypadConfig.value = control })
                }

                @Composable
                fun KeypadButton() {
                    if (sprint8DataConfig.isSprint8KeyPadBtnVisible) {
                        Sprint8KeyPadButton(
                            modifier = Modifier,
                            nextValue = control.nextValue.value,
                            keypadTitle = control.nextValueLabel.value,
                            keyPadUnit = control.unit.value,
                            screenHeight = screenHeight * 0.7f,
                            screenWidth = screenWidth * 0.6f,
                            borderStroke = BorderStroke(
                                width = 0.8.dp.computeSize(), color = SPRINT_ICON_BORDER_COLOR
                            ),
                            color = SPRINT_PROGRESSION_BAR_COLOR,
                            textColor = SPRINT_ICON_BORDER_COLOR,
                            imageAlignment = imageAlignment,
                            keyPadTitleSize = with(LocalDensity.current) { (screenHeight * 16 / 913f).toSp() },
                            nextValueSize = with(LocalDensity.current) { (screenHeight * 40 / 913f).toSp() })
                    }
                }

                Row(
                    modifier = Modifier
                        .align(newAlignment)
                        .padding(horizontal = 20.dp.computeSizeHd())
                        .offset(y = if (!uiState.isFullScreen) 12.dp.computeSizeHd() else -4.dp.computeSizeHd())
                ) {
                    if (newAlignment == Alignment.TopStart) {
                        PillboxButton()
                        Spacer(modifier = Modifier.size(20.dp.computeSizeHd()))
                        //show only after assessment dialog and assessment follow up
                        if (shouldShowControls) {
                            KeypadButton()
                        }
                    } else {
                        //show only after assessment dialog and assessment follow up
                        if (shouldShowControls) {
                            KeypadButton()
                            Spacer(modifier = Modifier.size(20.dp.computeSizeHd()))
                        }

                        //show only for starting 30 seconds of active recovery (active recovery time is 90,000 ms) or until the user has interacted with AssessmentDialog
                        if (shouldShowAssessmentDialog) {
                            AssessmentDialog(
                                modifier = Modifier
                                    .offset(y = if (!uiState.isFullScreen) -109.dp.computeSizeHd() else 5.dp.computeSizeHd()),
                                buttonConfig = assessmentButtonConfig,
                                baseTestTag = baseTag
                            )
                            Spacer(modifier = Modifier.size((if (uiState.isFullScreen) 32 else 20).dp.computeSizeHd()))
                        }

                        //show for only 5 seconds where user has interacted with AssessmentDialog
                        if (shouldShowAssessmentFollowUp) {
                            AssessmentFollowUp(
                                modifier = Modifier
                                    .padding(top = (if (!uiState.isFullScreen) 77 else 5).dp.computeSizeHd()),
                                levelDirection = uiState.levelDirection,
                                nextLevelAttributes = nextLevelAttributes,
                                baseTestTag = baseTag
                            )
                            Spacer(modifier = Modifier.size(24.dp.computeSizeHd()))
                        }
                        PillboxButton()
                    }
                }
            }
            FullScreenButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                screenWidth,
                screenHeight,
                uiState.isFullScreen,
                onFullscreenClick
            )
        }

        if (showKeypad) {
            PopupFullscreen(onDismissRequest = { showKeypad = false }) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickableNoEffect(onClick = { showKeypad = false })
                ) {
                    visibleKeypadConfig.value?.let { control ->
                        val keypadAlignment = if (control.location == FrameControlLocation.Left) Alignment.CenterStart else Alignment.CenterEnd
                        Sprint8NumPadEntry(
                            numPadValue = control.getValueFloat(),
                            min = control.range.value.min,
                            max = control.range.value.max,
                            modifier = Modifier
                                .padding(horizontal = 264.dp.computeSizeHd())
                                .size(
                                    width = 400.dp.computeSizeHd(),
                                    height = 702.dp.computeSizeHd()
                                )
                                .align(keypadAlignment),
                            stepValue = control.range.value.stepSize,
                            forceOnlyStepValues = true,
                            usePercentageBar = true,
                            unitLabel = control.keypadUnit.value,
                            enterBackground = sprint8DataConfig.keyPadConfirmBtnColor,
                            valueBar = sprint8DataConfig.keyPadConfirmBtnColor,
                            onDismiss = { showKeypad = false },
                            onValueCommit = {
                                control.onKeypadCommit(it)
                                showKeypad = false
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun SweatScoreStatBlock(
    sweatScores: ArrayList<Float>,
    numberOfSprints: Int,
    sprintCount: Int,
    isRunning: Boolean,
    screenWidth: Dp,
    screenHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val totalSweatScore = remember(sweatScores) {
        sweatScores.sumOf { round(it).toInt() }.toString()
    }
    val bestSweatScore = remember(sweatScores) {
        sweatScores.maxOrNull()?.let { round(it).toInt().toString() }
    }
    val sweatScoreList: List<Float?> = remember(sweatScores) {
        sweatScores?.let {
            it + List(MAX_INTERVALS - it.size) { null }
        } ?: List(MAX_INTERVALS) { null }
    }

    Column(
        modifier = modifier
            .wrapContentSize()
    ) {
        LazyRow(
            modifier = Modifier.padding(horizontal = 20.dp.computeSize()),
            horizontalArrangement = Arrangement.spacedBy(4.dp.computeSize())
        ) {
            items(numberOfSprints) { index ->
                val score = sweatScoreList[index]
                val label = (index + 1).toString()
                val isReceived = score != null
                val isSprintingThisIndex = index == sprintCount
                UpcomingSprintIndicatorComp(
                    score = score,
                    isReceived = isReceived,
                    label = label,
                    isSprinting = isSprintingThisIndex,
                    isRunning = isRunning
                )
            }
        }
        Sprint8NewScoreComp(
            totalSweatScore = totalSweatScore,
            personalBestScore = bestSweatScore ?: "0",
            modifier = Modifier,
            screenWidth,
            screenHeight
        )
    }
}


@AllDevicePreviews
@Composable
private fun Sprint8NewPreview() {
    val leftControl = MockFrameControl(location = FrameControlLocation.Left).apply {
        nextValue.value = "5.0"
        nextValueLabel.value = stringResource(R.string.next_incline_tc)
        unit.value = stringResource(R.string.percent_lc)
        label.value = stringResource(R.string.incline_tc)
        currentValue.value = "4.0"
        range.value = RangeInfo().apply { min = 0f; stepSize = 1f; max = 15.0f }
    }
    val rightControl = MockFrameControl(location = FrameControlLocation.Right).apply {
        nextValue.value = "7.0"
        nextValueLabel.value = stringResource(R.string.next_speed_tc)
        unit.value = stringResource(R.string.mph_lc)
        label.value = stringResource(R.string.speed_tc)
        currentValue.value = "6.5"
        range.value = RangeInfo().apply { min = 0f; stepSize = 1f; max = 15.0f }
    }
    Sprint8NewMainScreen(
        rpmValue = 30,
        frameControls = FrameControlCollection(listOf(leftControl, rightControl)),
        sprint8DataConfig = Sprint8DataConfig(isSprint8KeyPadBtnVisible = true),
        uiState = Sprint8UiState(isFullScreen = true, isSprinting = true, sweatScore = 40),
        isRpmVisible = true,
        onSkipClick = { },
        onFullscreenClick = { },
        onPlayPauseButtonClick = {},
        assessmentButtonConfig = emptyList(),
        isAssessmentVisible = true,
        getLevelName = { _ -> "Advanced 11"}
    )
}

@AllDevicePreviews
@Composable
private fun Sprint8CadencePreview() {
    Sprint8NewMainScreen(
        rpmValue = 0,
        spmValue = 170,
        sprint8DataConfig = Sprint8DataConfig(isSprint8KeyPadBtnVisible = true),
        uiState = Sprint8UiState(isFullScreen = true, isSprinting = false, sweatScore = 40),
        isRpmVisible = false,
        isCadenceVisible = true,
        onSkipClick = { },
        onFullscreenClick = { },
        onPlayPauseButtonClick = {},
        assessmentButtonConfig = emptyList(),
        isAssessmentVisible = false,
        getLevelName = { _ -> "Advanced 11"}
    )
}