package com.jht.sprint8.sprint8classic.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.data.FrameControl
import com.jht.data.FrameControlCollection
import com.jht.data.FrameControlLocation
import com.jht.data.MockFrameControl
import com.jht.data.RangeInfo
import com.jht.enums.WorkoutStage
import com.jht.sprint8.R
import com.jht.sprint8.model.GetReadyConfig
import com.jht.sprint8.model.Sprint8DataConfig
import com.jht.sprint8.model.Sprint8UiState
import com.jht.sprint8.sprint8classic.components.ClassicWorkoutStatComp
import com.jht.sprint8.sprint8classic.components.CountdownTimer
import com.jht.sprint8.sprint8classic.components.FullScreenButton
import com.jht.sprint8.sprint8classic.components.GetReadyComp
import com.jht.sprint8.sprint8classic.components.ProgressionBarComp
import com.jht.sprint8.sprint8classic.components.SkipButton
import com.jht.sprint8.sprint8classic.components.Sprint8KeyPadButton
import com.jht.sprint8.sprint8classic.components.Sprint8PillboxButton
import com.jht.sprint8.sprint8classic.components.SprintStateType
import com.jht.sprint8.sprint8classic.components.SweatScoreComp
import com.jht.sprint8.sprint8classic.components.TimerStyle
import com.jht.sprint8.utils.AnimationDurations
import com.jht.sprint8.utils.Sprint8Colors.BORDER_GRADIENT
import com.jht.sprint8.keypad.Sprint8NumPadEntry
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.widgets.PopupFullscreen

/**
 * Composable screen representing the main Sprint8 workout interface.
 *
 * This screen displays heart rate, RPM (optional), keypads for input,
 * workout data, and controls such as play/pause, skip, and fullscreen.
 * It adapts its layout based on the provided UI state and configuration data.
 *
 * @param heartRateValue Current heart rate value in BPM.
 * @param rpmValue Current RPM value, if applicable.
 * @param spmValue Current cadence value in steps per minute, if applicable.
 * @param keyPadConfigList List of [KeyPadConfig] defining keypad inputs. Defaults to empty.
 * @param sprint8DataConfig Configuration data for the Sprint8 workout display.
 * @param getReadyConfig Configuration for the "Get Ready" pre-workout UI.
 * @param uiState Current [Sprint8UiState] representing the workout state and progress.
 * @param isRpmVisible Boolean indicating whether the RPM value should be displayed. Defaults to false.
 * @param isCadenceVisible Boolean indicating whether the cadence value should be displayed instead
 * of RPM. Cadence and RPM are mutually exclusive and share the same UI slot; this takes precedence
 * over [isRpmVisible] when true. Defaults to false.
 * @param onPlayPauseButtonClick Callback invoked when the play/pause button is clicked.
 * @param onSkipClick Callback invoked when the skip button is clicked.
 * @param onFullscreenClick Callback invoked when the fullscreen button is clicked.
 */
@ExperimentalComposeUiApi
@Composable
internal fun Sprint8Screen(
    heartRateValue: Int,
    rpmValue: Int,
    spmValue: Int = 0,
    frameControls: FrameControlCollection = FrameControlCollection(),
    sprint8DataConfig: Sprint8DataConfig = Sprint8DataConfig(),
    getReadyConfig: GetReadyConfig = GetReadyConfig(),
    uiState: Sprint8UiState,
    isRpmVisible: Boolean = false,
    isCadenceVisible: Boolean = false,
    onPlayPauseButtonClick: () -> Unit,
    onSkipClick: () -> Unit,
    onFullscreenClick: () -> Unit,
) {
    val screenTestTag = "Sprint8"
    val screenHeight = remember { mutableStateOf(0.dp) }
    val screenWidth = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val visibleKeypadConfig = remember { mutableStateOf<FrameControl?>(null) }

    Box(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxSize()
            .onSizeChanged { size ->
                screenWidth.value = with(density) { size.width.toDp() }
                screenHeight.value = with(density) { size.height.toDp() }
            }
            .background(color = sprint8DataConfig.backgroundColor)) {
        Text(
            text = sprint8DataConfig.workoutTitle,
            fontSize = 42.sp.computeSize(1920f),
            fontFamily = FontFamily.SansSerif,
            color = Color.Black,
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.TopCenter)
                .padding(screenHeight.value * 0.136f)
                .testTag("Sprint8:IntervalTitle:Text")
        )
        ClassicWorkoutStatComp(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = screenWidth.value * if (uiState.isFullScreen) 280 / 1920f else 120 / 1600f)
                .fillMaxHeight(300 / if (uiState.isFullScreen) 1080f else 913f),
            title = stringResource(R.string.heart_rate_tc),
            icon = R.drawable.ic_heart,
            backgroundColor = sprint8DataConfig.classicHeartRateAndRpmBgColor,
            iconColor = sprint8DataConfig.backgroundColor,
            workoutStatValue = heartRateValue,
            screenHeight = screenHeight.value,
            screenWidth = screenWidth.value,
            borderColors = sprint8DataConfig.getBorderColors,
            testTagPrefix = screenTestTag
        )
        if (isCadenceVisible) {
            ClassicWorkoutStatComp(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = screenWidth.value * if (uiState.isFullScreen) 280 / 1920f else 120 / 1600f)
                    .fillMaxHeight(300 / if (uiState.isFullScreen) 1080f else 913f),
                title = stringResource(R.string.cadence_tc),
                icon = null,
                unitLabel = stringResource(R.string.spm_uc),
                labelFontWeight = FontWeight.SemiBold,
                backgroundColor = sprint8DataConfig.classicHeartRateAndRpmBgColor,
                iconColor = sprint8DataConfig.backgroundColor,
                workoutStatValue = spmValue,
                screenHeight = screenHeight.value,
                screenWidth = screenWidth.value,
                borderColors = sprint8DataConfig.getBorderColors,
                testTagPrefix = screenTestTag
            )
        } else if (isRpmVisible) {
            ClassicWorkoutStatComp(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = screenWidth.value * if (uiState.isFullScreen) 280 / 1920f else 120 / 1600f)
                    .fillMaxHeight(300 / if (uiState.isFullScreen) 1080f else 913f),
                title = stringResource(R.string.rpm_uc),
                icon = R.drawable.ic_gauge,
                backgroundColor = sprint8DataConfig.classicHeartRateAndRpmBgColor,
                iconColor = sprint8DataConfig.backgroundColor,
                workoutStatValue = rpmValue,
                screenHeight = screenHeight.value,
                screenWidth = screenWidth.value,
                borderColors = sprint8DataConfig.getBorderColors,
                testTagPrefix = screenTestTag
            )
        }

        val infiniteTransition = rememberInfiniteTransition()
        val blinkAlpha by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 0f, animationSpec = infiniteRepeatable(
                animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Reverse
            )
        )
        val fadeAlpha by animateFloatAsState(
            targetValue = if (getReadyConfig.isVisible) 1f else 0f, animationSpec = tween(
                durationMillis = 5000,
            )
        )
        val alpha = when {
            getReadyConfig.isBlinking -> {
                blinkAlpha
            }

            getReadyConfig.isVisible -> {
                fadeAlpha
            }

            else -> {
                0f
            }
        }

        if (uiState.isSprinting) {
            SkipButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                screenHeight.value,
                screenWidth.value,
                onSkipClick
            )
        }

        frameControls.controls.forEachIndexed { index, control ->
            val imageAlignment = if (control.location == FrameControlLocation.Left) Alignment.TopEnd else Alignment.TopStart
            val alignment = if (control.location == FrameControlLocation.Left) Alignment.BottomStart else Alignment.BottomEnd
            if (!uiState.isSprinting && uiState.workoutStage in listOf(WorkoutStage.WARM_UP, WorkoutStage.WORKOUT)) {
                Sprint8KeyPadButton(
                    modifier = Modifier
                        .align(alignment)
                        .padding(
                            bottom = screenHeight.value * 40 / 913f,
                            start = screenWidth.value * if (alignment == Alignment.BottomStart) 134 / 1600f else 20 / 1600f,
                            end = screenWidth.value * if (alignment == Alignment.BottomStart) 20 / 1600f else 134 / 1600f
                        ),
                    nextValue = control.nextValue.value,
                    keypadTitle = control.nextValueLabel.value,
                    keyPadUnit = control.unit.value,
                    screenHeight = screenHeight.value,
                    screenWidth = screenWidth.value,
                    color = Color.White.copy(alpha = 0.7f),
                    imageAlignment = imageAlignment,
                    borderStroke = BorderStroke(
                        screenWidth.value * 3 / 1600f, BORDER_GRADIENT
                    ),
                    keyPadTitleSize = with(LocalDensity.current) { (screenHeight.value * 20 / 913f).toSp() },
                    nextValueSize = with(LocalDensity.current) { (screenHeight.value * 48 / 913f).toSp() },
                    isFullScreen = uiState.isFullScreen,
                )
            }
            var showKeypad by remember { mutableStateOf(false) }

            if (uiState.isFullScreen) {
                Sprint8PillboxButton(
                    modifier = Modifier
                        .align(alignment)
                        .padding(
                            bottom = screenHeight.value * 280 / 1080f,
                            start = screenWidth.value * 60 / 1920f,
                            end = screenWidth.value * 60 / 1920f
                        ),
                    screenWidth = screenWidth.value,
                    screenHeight = screenHeight.value,
                    title = control.label.value,
                    value = control.currentValue.value,
                    unit = control.unit.value,
                    onPressIncrement = control::onIncrementPress,
                    onPressDecrement = control::onDecrementPress,
                    titleSize = with(LocalDensity.current) { (screenHeight.value * 19 / 1080f).toSp() },
                    valueSize = with(LocalDensity.current) { (screenHeight.value * 50 / 1080f).toSp() },
                    imageAlignment = imageAlignment,
                    onClick = { showKeypad = true;visibleKeypadConfig.value = control },
                    componentTag = "Sprint8"
                )

                if (showKeypad) {
                    PopupFullscreen(onDismissRequest = { showKeypad = false }) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clickableNoEffect(onClick = { showKeypad = false })
                        ) {
                            visibleKeypadConfig.value?.let { config ->
                                val keypadAlignment = if (alignment == Alignment.BottomStart) Alignment.CenterStart else Alignment.CenterEnd
                                Sprint8NumPadEntry(
                                    numPadValue = control.getValueFloat(),
                                    min = control.range.value.min,
                                    max = control.range.value.max,
                                    modifier = Modifier.padding(horizontal = 264.dp.computeSize(1920f)).size(width = 400.dp.computeSize(1920f), height = 702.dp.computeSize(1920f)).align(keypadAlignment),
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
        CountdownTimer(
            totalTime = uiState.totalTime,
            timerStyle = if (uiState.isSprinting) TimerStyle.Sprint else uiState.timerStyle,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(484f / if (uiState.isFullScreen) 1920f else 1600f),
            screenTestTag = screenTestTag,
            remainingTime = uiState.remainingTime,
            isRunning = uiState.isRunning,
            onPlayPauseClick = onPlayPauseButtonClick
        )
        ProgressionBarComp(
            modifier = Modifier.align(Alignment.TopCenter),
            uiState.sprint8State,
            if (uiState.isSprinting) SprintStateType.Sprint else uiState.progressStyle,
            uiState.sweatScoreParcel.sweatScores,
            totalSprints = uiState.numberOfSprints
        )
        FullScreenButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            screenWidth.value,
            screenHeight.value,
            uiState.isFullScreen,
            onFullscreenClick
        )
        if (uiState.isSprinting && uiState.remainingTime <= AnimationDurations.SWEAT_SCORE_VISIBLE_DURATION) {
            SweatScoreComp(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = screenHeight.value * 40 / 913f),
                screenWidth.value,
                uiState.sweatScore,
                uiState.sweatScoreParcel.sweatScores
            )
        } else {
            GetReadyComp(
                title = getReadyConfig.title,
                backGroundColor = sprint8DataConfig.classicHeartRateAndRpmBgColor,
                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .alpha(
                        alpha = alpha
                    )
                    .padding(bottom = screenHeight.value * 40 / 913f),
                screenHeight = screenHeight.value,
                screenWidth = screenWidth.value,
                borderColors = sprint8DataConfig.getBorderColors,
                isFullScreen = uiState.isFullScreen
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@AllDevicePreviews
@Composable
private fun Sprint8ScreenPreview() {
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
    Box(Modifier.fillMaxSize()) {
        Sprint8Screen(
            heartRateValue = 0,
            rpmValue = 1,
            frameControls = FrameControlCollection(listOf(leftControl, rightControl)),
            sprint8DataConfig = Sprint8DataConfig(
                workoutTitle = stringResource(R.string.warmup_tc),
                backgroundColor = Color(0xffcece2a),
                classicHeartRateAndRpmBgColor = Color.LightGray,
                getBorderColors = listOf(Color.Green, Color.Red)
            ),
            getReadyConfig = GetReadyConfig(
                title = stringResource(R.string.sprinting_description_pg), isVisible = true, isBlinking = true
            ),
            uiState = Sprint8UiState(
                isSprinting = false, remainingTime = 29000f, isFullScreen = true
            ),
            isRpmVisible = true,
            onPlayPauseButtonClick = {},
            onSkipClick = {},
            onFullscreenClick = { })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@AllDevicePreviews
@Composable
private fun Sprint8ScreenCadencePreview() {
    Box(Modifier.fillMaxSize()) {
        Sprint8Screen(
            heartRateValue = 142,
            rpmValue = 0,
            spmValue = 162,
            sprint8DataConfig = Sprint8DataConfig(
                workoutTitle = stringResource(R.string.warmup_tc),
                backgroundColor = Color(0xffcece2a),
                classicHeartRateAndRpmBgColor = Color.LightGray,
                getBorderColors = listOf(Color.Green, Color.Red)
            ),
            uiState = Sprint8UiState(isSprinting = false, remainingTime = 29000f),
            isRpmVisible = false,
            isCadenceVisible = true,
            onPlayPauseButtonClick = {},
            onSkipClick = {},
            onFullscreenClick = { })
    }
}