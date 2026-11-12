package com.jht.progression.ui.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jht.data.MmIntervalList
import com.jht.data.ProgressionInterval
import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.progression.R
import com.jht.progression.data.program.getSpeedForIntervalType
import com.jht.progression.oxaniumStyle
import com.jht.progression.robotoStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.getIntervalTypeColor
import com.jht.progression.ui.widgets.BlackBackgroundScreen
import com.jht.progression.ui.widgets.ColoredWorkoutBar
import com.jht.progression.ui.widgets.ProgressionCadenceModal
import com.jht.progression.ui.widgets.TwoOppositeTriangles
import com.jht.resources.oxaniumFonts
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.unit.UnitFormatter
import com.jht.widgets.scaleSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WorkoutScreen(
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = viewModel(),
    baselineKph: Float,
    isWorkoutLaunched: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (isWorkoutLaunched){
            delay(1000.milliseconds) //Showing the workout screen after 1 second
            isVisible = true
        }
    }

    //Using the black background to hide the visible UI flashes when launching a workout from
    //another screen. This black background will just show for 1 second, which matches the
    //countdown time logic at starting of a workout and matches a workout ending transition
    if (isWorkoutLaunched && isVisible && !uiState.isWorkoutStopped){
        WorkoutScreenContent(
            modifier = modifier,
            currentInterval = uiState.currentInterval,
            intervalType = uiState.intervalType,
            intervalList = uiState.intervalList,
            baselineKph = baselineKph,
            useMetric = uiState.useMetric,
            goalRemaining = uiState.goalRemaining,
            elapsedTime = uiState.timeElapsedMs,
            displayIntervalList = uiState.displayIntervalList,
            displayTips = uiState.displayTips,
            tipText = uiState.tipText,
            isTipBoxEnabled = uiState.isTipBoxEnabled,
            isTipBoxVisible = uiState.isTipBoxVisible,
            spmValue = uiState.spm,
            isCadenceVisible = uiState.isCadenceVisible,
            onDismissTipBox = { viewModel.onDismissTipBox() },
            setTipBoxState = { viewModel.setTipBoxState(it) },
            clampSpeed = { viewModel.clampSpeed(it) },
        )
    }else{
       BlackBackgroundScreen()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WorkoutScreenContent(
    modifier: Modifier = Modifier,
    currentInterval: Int,
    intervalType: IntervalType,
    intervalList: MmIntervalList,
    baselineKph: Float,
    useMetric: Boolean,
    goalRemaining: Float,
    elapsedTime: Int,
    displayIntervalList: Boolean,
    displayTips: Boolean,
    tipText: Int,
    isTipBoxEnabled: Boolean,
    isTipBoxVisible: Boolean,
    spmValue: Int,
    isCadenceVisible: Boolean,
    onDismissTipBox: () -> Unit,
    setTipBoxState: (Boolean) -> Unit,
    clampSpeed: (Float) -> Float,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .semantics { testTagsAsResourceId = true }
    ) {
        // This will make sure the goal is set correctly when we are initially starting a workout
        val correctedGoalRemaining = if (elapsedTime > 0) {
            goalRemaining
        } else {
            intervalList.intervalList[currentInterval].goalValue
        }
        var isCadenceModalDismissed by remember { mutableStateOf(false) }

        Image(
            painterResource(id = R.drawable.mm_squares_bg),
            contentDescription = "Workout Screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        CurrentIntervalLayout(
            intervalType = intervalType,
            intervalList = intervalList,
            currentInterval = currentInterval,
            goalRemaining = correctedGoalRemaining,
            useMetric = useMetric
        )

        // "Up Next" display and the cadence modal stack together, top-aligned at the same left
        // edge, 24dp apart vertically.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 100.dp.scaleSize()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp.scaleSize(1920f))
        ) {
            // Check that there is a next interval to display
            if (displayIntervalList) {
                AnimatedNextIntervalDisplay(
                    intervalList = intervalList,
                    currentInterval = currentInterval,
                    baselineKph = baselineKph,
                    useMetric = useMetric,
                    clampSpeed = clampSpeed
                )
            }
            if (isCadenceVisible) {
                ProgressionCadenceModal(
                    spm = spmValue,
                    isHidden = isCadenceModalDismissed,
                    onDismiss = { isCadenceModalDismissed = true },
                    onReopen = { isCadenceModalDismissed = false },
                )
            }
        }

        if (displayTips) {
            TipsBoxDisplay(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 100.dp),
                tipText = tipText,
                isTipBoxEnabled = isTipBoxEnabled,
                isTipBoxVisible = isTipBoxVisible,
                onDismissTipBox = onDismissTipBox,
                setTipBoxState = setTipBoxState
            )
        }


        // workout progress bar
        if (displayIntervalList) {
            WorkoutProgress(
                intervalList,
                elapsedTime,
                currentInterval = currentInterval,
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp.scaleSize())
                    .width(700.dp.scaleSize()),
            )
        }
    }
}

@Composable
private fun CurrentIntervalLayout(
    intervalType: IntervalType,
    intervalList: MmIntervalList,
    currentInterval: Int,
    goalRemaining: Float,
    useMetric: Boolean
) {
    val activeInterval = intervalList.intervalList[currentInterval]
    val goalType = activeInterval.goalType
    val goalValue = activeInterval.goalValue

    // Progress bar animation values
    val targetProgress: Float = when (goalType) {
        // Target progress should be one second in the future to line up with the animation
        IntervalGoalType.TIME -> ((goalRemaining - 1000f) / goalValue).coerceAtLeast(0f)

        /*
         * I don't want to try to forecast distance, so this will just be whatever the user has
         * achieved. Distance goals count up from 0.
         */
        IntervalGoalType.DISTANCE -> (goalValue - goalRemaining) / goalValue
    }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(targetProgress) {
        val countsUp = when (goalType) {
            IntervalGoalType.TIME -> false
            IntervalGoalType.DISTANCE -> true
        }

        if (!countsUp && targetProgress > progress.targetValue) {
            // Reset animation and begin animation again
            progress.animateTo(
                targetValue = targetProgress,
                animationSpec = tween(800, easing = EaseInOut)
            )
        } else {
            // Animate each second
            progress.animateTo(
                targetValue = targetProgress,
                animationSpec = tween(1000, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedCircularProgressBar(
            modifier = Modifier
                .size(430.dp.scaleSize())
                .testTag("workout_center_progress_circle"),
            progress = progress.value,
            intervalType = intervalType
        )

        // the two triangles on the above image
        TwoOppositeTriangles(color = getIntervalTypeColor(intervalType))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize()
        ) {
            // The back time countdown
            Text(
                text = getFormattedGoalRemainingText(
                    goalType,
                    goalRemaining,
                    useMetric
                ),
                color = Color.White,
                fontSize = 100.sp.scaleSize(),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .alpha(0.2f)
                    .offset(y = 5.dp.scaleSize())
                    .testTag("workout_back_countdown_value")
                    .size(281.dp.scaleSize(), 100.dp.scaleSize()),
                style = oxaniumStyle.copy(lineHeight = 100.sp.scaleSize(), textAlign = TextAlign.Center),
            )

            CurrentIntervalDisplay(
                modifier = Modifier.offset(y = (-20).dp.scaleSize()),
                goalValue = goalValue,
                goalRemaining = goalRemaining,
                goalType = goalType,
                useMetric = useMetric,
                intervalColor = getIntervalTypeColor(intervalType),
                intervalType = intervalType,
                currentInterval = currentInterval
            )
        }
    }
}

@Composable
private fun AnimatedCircularProgressBar(
    modifier: Modifier = Modifier,
    progress: Float,
    intervalType: IntervalType
) {
    val intervalColor = getIntervalTypeColor(intervalType)

    val infiniteAnimation = rememberInfiniteTransition(label = "centerCircleRotation")
    val rotationAngle by infiniteAnimation.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2160000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    Canvas(modifier = modifier, onDraw = {
        drawDottedCircle(
            diameter = size.width,
            numCircles = 80,
            intervalColor,
            dotRadius = size.width * 0.004f,
            rotationRadians = rotationAngle
        )

        drawDottedCircle(
            diameter = size.width * 0.85f,
            numCircles = 30,
            intervalColor,
            dotRadius = size.width * 0.006f,
            rotationRadians = (2f * Math.PI.toFloat()) - rotationAngle
        )

        // Progress variables
        val progressBarDiameter = size.width * 0.93f
        val progressStrokeWidth = size.width * 0.022f
        val offset = (size.width - progressBarDiameter) / 2f
        val sweepAngle = progress.coerceIn(0f, 1f) * 360f

        // variables for drawing the completed progress line in the correct spot
        val remainderStrokeWidth = progressStrokeWidth / 3.5f
        val remainderDiameter = progressBarDiameter - remainderStrokeWidth * 3
        val remainderOffset = (size.width - remainderDiameter) / 2f
        val startAngle = 270f + sweepAngle

        // Draw arc for completed progress (the inner circle)
        drawArc(
            color = intervalColor,
            startAngle = startAngle, // This starts the arc directly up
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(remainderOffset, remainderOffset),
            style = Stroke(remainderStrokeWidth),
            size = Size(remainderDiameter, remainderDiameter),
        )

        // Draw arc for remaining progress
        drawArc(
            color = intervalColor,
            startAngle = 270f, // This starts the arc directly up
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(offset, offset),
            style = Stroke(progressStrokeWidth),
            size = Size(progressBarDiameter, progressBarDiameter),
        )
    })
}

/**
 * Draws a circle of dots with a rotation and color
 */
private fun DrawScope.drawDottedCircle(
    diameter: Float,
    numCircles: Int,
    color: Color,
    dotRadius: Float,
    rotationRadians: Float
) {
    val centerCoordinate = size.width / 2f
    val eachDotRadians = 2f * Math.PI.toFloat() / numCircles.toFloat()
    val dotCircleRadius = diameter / 2f - dotRadius

    for (i in 0 until numCircles) {
        val dotRadian = eachDotRadians * i + rotationRadians
        val dotXPosition = centerCoordinate + sin(dotRadian) * dotCircleRadius
        val dotYPosition = centerCoordinate + cos(dotRadian) * dotCircleRadius
        drawCircle(
            color = color,
            radius = dotRadius,
            center = Offset(dotXPosition, dotYPosition)
        )
    }
}

@Composable
private fun AnimatedNextIntervalDisplay(
    modifier: Modifier = Modifier,
    intervalList: MmIntervalList,
    currentInterval: Int,
    baselineKph: Float,
    useMetric: Boolean,
    clampSpeed: (Float) -> Float
) {
    var displayedInterval by remember {
        val hasNextInterval = intervalList.intervalList.size > (currentInterval + 1)
        if (hasNextInterval) {
            mutableStateOf(intervalList.intervalList[currentInterval + 1])
        } else {
            mutableStateOf(intervalList.intervalList[currentInterval])
        }
    }
    var nextIntervalVisible by remember { mutableStateOf(false) }

    // When the interval changes start the animation for hiding the current next interval and showing the new one
    LaunchedEffect(currentInterval) {
        val hasNextInterval = intervalList.intervalList.size > (currentInterval + 1)

        if (currentInterval == 0 && hasNextInterval) {
            nextIntervalVisible = true
        } else if (hasNextInterval) {
            // We just switched, we want to hide the display, switch the displayed interval, then show it again
            nextIntervalVisible = false
            delay(2000.milliseconds)
            displayedInterval = intervalList.intervalList[currentInterval + 1]
            nextIntervalVisible = true
        } else {
            nextIntervalVisible = false
        }
    }

    // graphicsLayer only affects drawing, never layout, so this content's measured size stays
    // constant regardless of nextIntervalVisible - the Column stacking this with the cadence
    // modal needs a stable slot here.
    var contentWidthPx by remember { mutableStateOf(0) }
    val slideProgress by animateFloatAsState(
        targetValue = if (nextIntervalVisible) 0f else 1f,
        label = "nextIntervalSlideProgress"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (nextIntervalVisible) 1f else 0f,
        label = "nextIntervalAlpha"
    )

    NextIntervalDisplay(
        modifier = modifier
            .onSizeChanged { contentWidthPx = it.width }
            .graphicsLayer {
                alpha = contentAlpha
                translationX = slideProgress * contentWidthPx
            },
        nextInterval = displayedInterval,
        baselineKph = baselineKph,
        useMetric = useMetric,
        clampSpeed = clampSpeed,
        currentInterval = currentInterval
    )
}

@Composable
private fun NextIntervalDisplay(
    modifier: Modifier = Modifier,
    nextInterval: ProgressionInterval,
    baselineKph: Float,
    useMetric: Boolean,
    clampSpeed: (Float) -> Float,
    currentInterval: Int
) {
    val intervalColor = getIntervalTypeColor(nextInterval.intervalType)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .padding(bottom = 3.dp.scaleSize())
                .testTag("up_next_string"),
            text = stringResource(id = R.string.up_next_uc),
            fontSize = 22.sp.scaleSize(),
            fontWeight = FontWeight.Bold,
            color = intervalColor,
            style = oxaniumStyle,
        )

        Column(
            modifier = Modifier
                .background(intervalColor.copy(alpha = 0.2f))
                .padding(10.dp.scaleSize()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getIntervalTypeName(intervalType = nextInterval.intervalType, currentInterval = currentInterval),
                fontSize = 18.sp.scaleSize(),
                color = Color.White,
                style = oxaniumStyle,
                modifier = Modifier.testTag("up_next_interval_name")
            )

            val kphUnit = stringResource(R.string.kph_uc)
            val mphUnit = stringResource(R.string.mph_uc)
            val speedString = remember(nextInterval, useMetric) {
                val intervalSpeed =
                    getSpeedForIntervalType(nextInterval.intervalType, baselineKph, clampSpeed)
                val intervalSpeedFormatted = UnitFormatter.formatSpeed(intervalSpeed, useMetric)
                intervalSpeedFormatted
            }

            val speedUnit = if (useMetric) {
                kphUnit
            } else {
                mphUnit
            }

            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 10.dp.scaleSize())
                    .widthIn(min = 150.dp.scaleSize())
                    .testTag("up_next_interval_speed"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    modifier = Modifier
                        .padding(end = 5.dp.scaleSize())
                        .alignByBaseline(),
                    text = speedString,
                    fontSize = 30.sp.scaleSize(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = oxaniumStyle,
                )
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = speedUnit,
                    fontSize = 20.sp.scaleSize(),
                    color = Color.White,
                    style = oxaniumStyle,
                )
            }

        }
    }
}

@Composable
private fun getIntervalTypeName(intervalType: IntervalType, currentInterval: Int): String {
    val zone1 = if (currentInterval == 0) R.string.warmup_tc else R.string.cooldown_tc
    return when (intervalType) {
        IntervalType.WARMUP -> stringResource(R.string.warmup_tc)
        IntervalType.ASSESSMENT -> stringResource(R.string.work_tc)
        IntervalType.COOLDOWN -> stringResource(R.string.cooldown_tc)
        IntervalType.ZONE_1 -> stringResource(zone1)
        IntervalType.ZONE_2 -> stringResource(R.string.recovery_tc)
        IntervalType.ZONE_3 -> stringResource(R.string.moderate_tc)
        IntervalType.ZONE_4 -> stringResource(R.string.intense_tc)
        IntervalType.ZONE_5 -> stringResource(R.string.max_effort_tc)
        IntervalType.RACE -> stringResource(R.string.race_tc)
    }
}

@Composable
private fun TipsBoxDisplay(
    modifier: Modifier = Modifier,
    tipText: Int,
    isTipBoxVisible: Boolean,
    isTipBoxEnabled: Boolean,
    onDismissTipBox: () -> Unit,
    setTipBoxState: (Boolean) -> Unit
) {
    TipBox(
        modifier = modifier,
        tipText = tipText,
        isTipBoxVisible = isTipBoxVisible,
        isTipBoxEnabled = isTipBoxEnabled,
        onDismissTipBox = onDismissTipBox
    )

    TipIcon(
        modifier = modifier,
        isTipBoxVisible = isTipBoxVisible,
        setTipBoxState = setTipBoxState
    )

}

@Composable
private fun TipIcon(
    modifier: Modifier = Modifier,
    isTipBoxVisible: Boolean,
    setTipBoxState: (Boolean) -> Unit
) {
    val enabledColor = MmColors.futuristicBlue
    val disabledColor = MmColors.futureGray
    var isPressing by remember { mutableStateOf(false) }
    val currentIconColor = if (isPressing) disabledColor else enabledColor
    val tipIconStatus =
        if (isPressing) R.string.disabled_uc else R.string.enabled_uc
    var isShrinkAndExpand by remember { mutableStateOf(false) }
    val iconSize by animateDpAsState(
        targetValue = if (isShrinkAndExpand) 40.dp.scaleSize() else 50.dp.scaleSize(),
        label = "Tip icon size animated"
    )
    val scope = rememberCoroutineScope()
    var isTipIconActive by remember { mutableStateOf(false) }

    //this is for setting the tip box state and running the tip box in TipBox()
    setTipBoxState(currentIconColor == enabledColor)

    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .fillMaxWidth(0.4f)
            .padding(5.dp.scaleSize())
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.ic_icon_hint),
                contentDescription = "hint icon",
                colorFilter = ColorFilter.tint(color = currentIconColor.copy(alpha = if (isShrinkAndExpand) 0.8f else 1f)),
                modifier = Modifier
                    .height(iconSize)
                    .padding(
                        start = 10.dp.scaleSize(),
                        end = 20.dp.scaleSize(),
                        top = 10.dp.scaleSize()
                    )
                    .clickableNoEffect {
                        //the actions are working when the tip box doesn't present
                        if (!isTipBoxVisible) {
                            //This is for running the icon and text animations and also handle the consistent click issue
                            if (!isTipIconActive) {
                                isTipIconActive = true
                                isPressing = !isPressing
                                isShrinkAndExpand = true
                            }
                            scope.launch {
                                delay(200.milliseconds)
                                isShrinkAndExpand = false
                                delay(2000.milliseconds)
                                isTipIconActive = false
                            }
                        }
                    }
                    .testTag("workout_tip_icon")
            )
        }

        AnimatedVisibility(
            visible = isTipIconActive,
            enter = slideInHorizontally { -it / 2 } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp.scaleSize())
                    .testTag("tip_box_status_text"),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.advice_uc),
                        fontSize = 16.sp.scaleSize(),
                        color = Color.White,
                        fontStyle = FontStyle.Italic,
                        style = oxaniumStyle,
                    )
                    Text(
                        text = stringResource(id = tipIconStatus),
                        fontSize = 16.sp.scaleSize(),
                        color = Color.White,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        style = oxaniumStyle,
                    )
                }
            }
        }
    }
}

@Composable
private fun TipBox(
    modifier: Modifier = Modifier,
    tipText: Int,
    isTipBoxVisible: Boolean,
    isTipBoxEnabled: Boolean,
    onDismissTipBox: () -> Unit
) {
    if (isTipBoxEnabled) {
        AnimatedVisibility(
            visible = isTipBoxVisible,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut()
        ) {
            //Each tip will last displaying for one minute
            LaunchedEffect(Unit) {
                delay(60000.milliseconds)
                onDismissTipBox.invoke()
            }
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = modifier
                    .background(Color.Black.copy(alpha = 0.6f))
                    .fillMaxWidth(0.35f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(
                            start = 66.dp.scaleSize(),
                            end = 6.dp.scaleSize(),
                            top = 10.dp.scaleSize(),
                            bottom = 10.dp.scaleSize()
                        )
                ) {
                    Text(
                        text = stringResource(id = R.string.coach_tip_uc),
                        color = Color.White,
                        fontSize = 18.sp.scaleSize(),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(bottom = 5.dp.scaleSize())
                            .testTag("tip_box_title"),
                        style = oxaniumStyle,
                    )

                    Text(
                        text = stringResource(id = tipText),
                        color = Color.White,
                        fontSize = 15.sp.scaleSize(),
                        modifier = Modifier
                            .padding(bottom = 2.dp.scaleSize())
                            .testTag("workout_tips"),
                        style = oxaniumStyle,
                        lineHeight = 24.sp.scaleSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp.scaleSize()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.icon_x),
                        contentDescription = "close icon",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier
                            .fillMaxSize(0.4f)
                            .clickableNoEffect {
                                onDismissTipBox.invoke()
                            }
                            .testTag("tip_box_close_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentIntervalDisplay(
    modifier: Modifier = Modifier,
    goalValue: Float,
    goalRemaining: Float,
    goalType: IntervalGoalType,
    useMetric: Boolean,
    intervalType: IntervalType,
    intervalColor: Color,
    currentInterval: Int
) {
    val intervalLabel = getIntervalTypeName(intervalType, currentInterval)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.wrapContentSize()
    ) {
        // The front time countdown
        Text(
            text = getFormattedGoalRemainingText(goalType, goalRemaining, useMetric),
            color = Color.White,
            fontSize = 48.sp.scaleSize(),
            fontWeight = FontWeight.SemiBold,
            style = oxaniumStyle.copy(lineHeight = 48.sp.scaleSize(), textAlign = TextAlign.Center),
            modifier = Modifier
                .testTag("workout_front_countdown_value")
                .padding(bottom = 10.dp.scaleSize())
                .size(281.dp.scaleSize(), 48.dp.scaleSize())
        )

        // the reminder text and progress bar for the interval time countdown
        ProgressBarReminderText(
            goalValue = goalValue,
            goalRemaining = goalRemaining,
            goalType = goalType,
            intervalType = intervalType,
            intervalColor = intervalColor,
            currentInterval = currentInterval
        )

        ColoredWorkoutBar(barColor = intervalColor)

        Spacer(modifier = Modifier.padding(top = 36.dp.scaleSize()))
        // Interval label
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 281.dp.scaleSize(), height = 36.dp.scaleSize())
        ) {
            Text(
                text = intervalLabel,
                color = intervalColor,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("workout_interval_label"),
                autoSize = TextAutoSize.StepBased(
                    maxFontSize = 200.sp
                ),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontFamily = oxaniumFonts,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    ),
                ),
            )
        }
    }
}

/**
 * Set up the reminder text animation with the progress bar. When warmup and cool down start, it will show the text
 * "SLOWDOWN" above the bar. When assessment work start, it will show the text "SPEED UP" above the bar.
 * The text "SLOWDOWN" and "SPEED UP" will keep showing for 5 seconds at the beginning of the interval,
 * the text "REMAINING" will keep showing at all other time. The first interval of the workout has no
 * transition to prompt for, so it always shows "REMAINING".
 * */
@Composable
private fun ProgressBarReminderText(
    goalValue: Float,
    goalRemaining: Float,
    goalType: IntervalGoalType,
    intervalType: IntervalType,
    intervalColor: Color,
    currentInterval: Int
) {
    val remaining = stringResource(id = R.string.remaining_uc)
    val slowdown = stringResource(id = R.string.slowdown_uc)
    val speedUp = stringResource(id = R.string.speed_up_uc)

    val reminderText = if (goalType == IntervalGoalType.TIME && currentInterval > 0) {
        val goalProgress = goalValue - goalRemaining
        when {
            goalProgress <= 5000f && intervalType == IntervalType.WARMUP -> slowdown
            goalProgress <= 5000f && intervalType == IntervalType.COOLDOWN -> slowdown
            goalProgress <= 5000f && intervalType == IntervalType.ASSESSMENT -> speedUp
            else -> remaining
        }
    } else {
        remaining
    }

    AnimatedContent(
        targetState = reminderText,
        contentAlignment = Alignment.Center, label = ""
    ) { targetState ->
        Text(
            text = targetState,
            color = Color.White,
            fontSize = 16.sp.scaleSize(),
            modifier = Modifier
                .alpha(0.8f)
                .size(281.dp.scaleSize(), 24.dp.scaleSize())
                .testTag("progress_bar_reminder_text")
                .animateEnterExit(
                    enter = if (targetState == remaining) fadeIn() else slideInVertically(
                        animationSpec = tween(2000)
                    ) { fullHeight -> fullHeight } + fadeIn(),
                    exit = if (targetState == remaining) slideOutVertically(
                        animationSpec = tween(2000)
                    ) { fullHeight -> fullHeight } + fadeOut() else fadeOut()
                ),
            textAlign = TextAlign.Center,
            style = robotoStyle.copy(lineHeight = 24.sp.scaleSize(), fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun getFormattedGoalRemainingText(
    goalType: IntervalGoalType,
    goalRemaining: Float,
    isMetric: Boolean
): String {
    return when (goalType) {
        IntervalGoalType.TIME -> {
            UnitFormatter.formatTimeMilliseconds(goalRemaining)
        }

        IntervalGoalType.DISTANCE -> {
            UnitFormatter.formatDistanceGoal(goalRemaining, isMetric)
        }
    }
}

@AllDevicePreviews
@Composable
fun PreviewWorkoutScreenWithCadence() {
    // "Up Next" and the cadence modal together, stacked under it - the primary case this preview
    // exists to check: same left edge, ~24dp gap.
    WorkoutScreenContent(
        currentInterval = 7,
        intervalType = IntervalType.ZONE_4,
        intervalList = createTestIntervalList(),
        baselineKph = 5f,
        useMetric = false,
        goalRemaining = 85000f,
        elapsedTime = 1000,
        displayIntervalList = true,
        displayTips = false,
        tipText = -1,
        isTipBoxVisible = false,
        isTipBoxEnabled = false,
        spmValue = 170,
        isCadenceVisible = true,
        onDismissTipBox = {},
        setTipBoxState = {},
        clampSpeed = { it },
    )
}

@AllDevicePreviews
@Composable
fun PreviewWorkoutScreen() {
    // Baseline: original preview, now passing cadence off (non-cadence frame).
    val currentInterval = 7
    val intervalList: MmIntervalList = createTestIntervalList()
    val intervalType = intervalList.intervalList[currentInterval].intervalType
    val timeRemainingMs = intervalList.intervalList[currentInterval].goalValue / 2

    WorkoutScreenContent(
        currentInterval = currentInterval,
        intervalType = intervalType,
        intervalList = intervalList,
        baselineKph = 5f,
        useMetric = false,
        goalRemaining = timeRemainingMs,
        elapsedTime = 1000,
        displayIntervalList = true,
        displayTips = true,
        tipText = -1,
        isTipBoxVisible = false,
        isTipBoxEnabled = true,
        spmValue = 0,
        isCadenceVisible = false,
        onDismissTipBox = {},
        setTipBoxState = {},
        clampSpeed = { it },
    )
}

@AllDevicePreviews
@Composable
fun PreviewWorkoutScreenCadenceOnlyNoUpNext() {
    // No "Up Next" (e.g. the final interval) - confirms the cadence modal sits at the top of the
    // stack on its own rather than leaving a gap where "Up Next" would have been.
    val intervalList: MmIntervalList = createTestIntervalList()
    val currentInterval = intervalList.intervalList.lastIndex

    WorkoutScreenContent(
        currentInterval = currentInterval,
        intervalType = intervalList.intervalList[currentInterval].intervalType,
        intervalList = intervalList,
        baselineKph = 5f,
        useMetric = false,
        goalRemaining = intervalList.intervalList[currentInterval].goalValue / 2,
        elapsedTime = 1000,
        displayIntervalList = false,
        displayTips = false,
        tipText = -1,
        isTipBoxVisible = false,
        isTipBoxEnabled = false,
        spmValue = 170,
        isCadenceVisible = true,
        onDismissTipBox = {},
        setTipBoxState = {},
        clampSpeed = { it },
    )
}

private fun createTestIntervalList(): MmIntervalList {
    val intervals = ArrayList<ProgressionInterval>()
    intervals.add(ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, 300000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 150000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, 160000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, 170000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, 180000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 150000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, 160000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, 170000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, 180000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 150000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, 160000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, 170000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, 180000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_2, IntervalGoalType.TIME, 150000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, 160000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_4, IntervalGoalType.TIME, 170000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_5, IntervalGoalType.TIME, 180000f))
    intervals.add(ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, 300000f))
    return MmIntervalList(intervals)
}

