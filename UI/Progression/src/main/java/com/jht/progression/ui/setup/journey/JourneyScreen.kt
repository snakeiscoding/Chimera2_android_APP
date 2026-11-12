package com.jht.progression.ui.setup.journey

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.scale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jht.progression.R
import com.jht.progression.data.EventLabel
import com.jht.progression.data.IEvent
import com.jht.progression.data.NonWorkoutEvent
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.widgets.HomeButtonWidget
import com.jht.progression.ui.widgets.MmDoubleArrowsRight
import com.jht.progression.ui.widgets.MmOptionDialog
import com.jht.progression.util.createTintedPainter
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.widgets.AutoSizeText
import com.jht.widgets.scaleSize
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun JourneyScreen(
    viewModel: JourneyViewModel = viewModel(),
    onEventSelected: (IEvent) -> Unit,
    navigateToSplash: () -> Unit,
    onRedoAssessment: () -> Unit,
    onGoalSettings: () -> Unit,
    navigateToProgramSummary: () -> Unit,
    onEraseUserData: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    JourneyScreenContent(
        navigateToSplash = navigateToSplash,
        eventPages = uiState.eventPages,
        currentTime = uiState.currentTime,
        timeUntilWorkout = uiState.timeUntilNextWorkout,
        nextFutureEvent = uiState.nextFutureWorkoutIndex,
        selectedPage = uiState.selectedPage,
        autoDisplayGoalTimeInfoPopup = uiState.autoDisplayGoalTimeInfoPopup,
        goalSettingsEnabled = uiState.goalSettingsEnabled,
        onPageSelected = { viewModel.updateSelectedPage(it) },
        onClickedItem = onEventSelected,
        onResetWorkout = { viewModel.resetToWorkout(it) },
        onRetakeAssessment = onRedoAssessment,
        onGoalSettings = { navigateToGoalSettings, acknowledgeGoalTimeInfoPopup ->
            if (acknowledgeGoalTimeInfoPopup) viewModel.acknowledgeGoalTimeInfoPopup()
            if (navigateToGoalSettings) onGoalSettings()
        },
        onEraseUserData = {
            onEraseUserData()
            JourneyScreenAnimations.eventsDidAnimation.clear()
        },
        navigateToProgramSummary = navigateToProgramSummary
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun JourneyScreenContent(
    eventPages: List<List<IEvent>>,
    currentTime: Long,
    timeUntilWorkout: Long,
    nextFutureEvent: Int,
    selectedPage: Int,
    autoDisplayGoalTimeInfoPopup: Boolean,
    goalSettingsEnabled: Boolean,
    onPageSelected: (Int) -> Unit,
    onClickedItem: (IEvent) -> Unit,
    onResetWorkout: (IEvent) -> Unit,
    onGoalSettings: (Boolean, Boolean) -> Unit,
    onEraseUserData: () -> Unit,
    onRetakeAssessment: () -> Unit,
    navigateToSplash: () -> Unit,
    navigateToProgramSummary: () -> Unit
) {
    var recoverDialogOpen by remember { mutableStateOf(false) }
    var retakeAssessmentDialogOpen by remember { mutableStateOf(false) }
    var eraseDataOpen by remember { mutableStateOf(false) }
    var workoutToReset: IEvent? by remember { mutableStateOf(null) }

    // non-workout event dialog states
    val nonWorkoutDialogOpen = remember { mutableStateOf(false) }
    val dialogIconId = remember { mutableIntStateOf(0) }
    val dialogTitleId = remember { mutableIntStateOf(0) }
    val dialogLabelId = remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFF010F1B))
                    .height(78.dp.scaleSize()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_method_logo),
                    contentDescription = "Matrix Method screen header ",
                    modifier = Modifier
                        .size(206.dp.scaleSize(), 30.dp.scaleSize())
                        .align(Alignment.Center)
                        .testTag("method_logo"),
                    contentScale = ContentScale.FillBounds,
                    colorFilter = ColorFilter.tint(color = Color.White)
                )
                HomeButtonWidget(onHomeButtonPressed = navigateToSplash)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.mm_squares_bg),
                    contentDescription = "Journey Screen",
                    contentScale = ContentScale.Crop
                )
                JourneyProgramInfo(
                    eventPages = eventPages,
                    currentTime = currentTime,
                    timeUntilWorkout = timeUntilWorkout,
                    nextFutureEvent = nextFutureEvent,
                    selectedPage = selectedPage,
                    onPageSelected = onPageSelected,
                    onClickedItem = onClickedItem,
                    onMissedWorkoutClicked = {
                        recoverDialogOpen = true
                        workoutToReset = it
                    },
                    onNonWorkoutEventClicked = { icon, title, label ->
                        dialogIconId.intValue = icon
                        dialogTitleId.intValue = title
                        dialogLabelId.intValue = label
                        nonWorkoutDialogOpen.value = true
                    }
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            SettingsBox(
                goalSettingsEnabled = goalSettingsEnabled,
                onRetakeAssessment = { retakeAssessmentDialogOpen = true },
                onGoalSettings = { onGoalSettings(true, false) },
                onEraseUserData = { eraseDataOpen = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(width = 370.dp.scaleSize(), height = 250.dp.scaleSize())
            )
        }

        // Recovering missed workouts
        MmOptionDialog(
            open = recoverDialogOpen,
            icon = createTintedPainter(R.drawable.ic_time_remaining_white, MmColors.futuristicBlue),
            titleText = stringResource(id = R.string.mm_recover_workout_tc),
            bodyText = stringResource(id = R.string.mm_recover_workout_pg),
            acceptButtonText = stringResource(R.string.yes_uc),
            dismissButtonText = stringResource(R.string.cancel_uc),
            onDismiss = {
                recoverDialogOpen = false
                workoutToReset = null
            },
            onYesClicked = {
                recoverDialogOpen = false
                workoutToReset?.let { onResetWorkout(it) }
            }
        )

        // Delete user data confirmation
        MmOptionDialog(
            open = eraseDataOpen,
            icon = painterResource(R.drawable.ic_erase_program_trash_mm),
            titleText = stringResource(id = R.string.mm_erase_data_tc),
            bodyText = stringResource(id = R.string.mm_erase_data_pg),
            acceptButtonText = stringResource(id = R.string.erase_uc),
            acceptButtonColor = MmColors.missedRed,
            acceptRequiresHold = true,
            dismissButtonText = stringResource(id = R.string.cancel_uc),
            onDismiss = { eraseDataOpen = false },
            onYesClicked = {
                JourneyScreenAnimations.eventsDidAnimation.clear()
                onEraseUserData()
            })

        // Goal settings dialog
        MmOptionDialog(
            open = goalSettingsEnabled && autoDisplayGoalTimeInfoPopup,
            icon = painterResource(id = R.drawable.ic_target_training),
            titleText = stringResource(id = R.string.mm_5k_goal_settings_dialog_tc),
            bodyText = stringResource(id = R.string.mm_goal_settings_dialog_pg),
            dismissButtonText = stringResource(id = R.string.later_uc),
            onDismiss = {
                onGoalSettings(false, true)
            },
            onYesClicked = { onGoalSettings(true, true) }
        )

        // Retake assessment confirmation
        MmOptionDialog(
            open = retakeAssessmentDialogOpen,
            icon = createTintedPainter(R.drawable.ic_refresh_white, MmColors.futuristicBlue),
            titleText = stringResource(id = R.string.mm_retake_assessment_tc),
            bodyText = stringResource(id = R.string.mm_retake_assessment_pg),
            acceptButtonText = stringResource(id = R.string.retake_uc),
            dismissButtonText = stringResource(id = R.string.cancel_uc),
            onDismiss = { retakeAssessmentDialogOpen = false },
            onYesClicked = onRetakeAssessment
        )

        // Program summary icon on the Journey screen
        ProgramSummaryEntry(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 30.dp.scaleSize(), end = 30.dp.scaleSize()),
            navigateToProgramSummary = navigateToProgramSummary
        )
    }

    NonWorkoutEventDialog(
        open = nonWorkoutDialogOpen.value,
        dialogIconId.intValue,
        dialogTitleId.intValue,
        dialogLabelId.intValue
    ) {
        nonWorkoutDialogOpen.value = false
    }
}

@Composable
private fun JourneyProgramInfo(
    eventPages: List<List<IEvent>>,
    currentTime: Long,
    timeUntilWorkout: Long,
    nextFutureEvent: Int,
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    onClickedItem: (IEvent) -> Unit,
    onMissedWorkoutClicked: (IEvent) -> Unit,
    onNonWorkoutEventClicked: (icon: Int, title: Int, label: Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            WeekBar(
                numPages = eventPages.size,
                selectedPage = selectedPage,
                onPageSelected = onPageSelected,
                modifier = Modifier
                    .padding(top = 15.dp.scaleSize(), bottom = 50.dp.scaleSize())
                    .fillMaxWidth()
                    .height(52.dp.scaleSize())
                    .testTag("week_bar_container"),
            )

            NextWorkoutText(
                timeUntilWorkout = timeUntilWorkout,
                modifier = Modifier
                    .padding(bottom = 64.dp.scaleSize())
                    .fillMaxWidth(),
                selectedPage = selectedPage
            )

            EventPager(
                pagedEvents = eventPages,
                currentTime = currentTime,
                nextFutureEvent = nextFutureEvent,
                timeUntilWorkout = timeUntilWorkout,
                selectedPage = selectedPage,
                onPageSelected = onPageSelected,
                onClickedItem = onClickedItem,
                onMissedWorkoutClicked = onMissedWorkoutClicked,
                onNonWorkoutEventClicked = onNonWorkoutEventClicked,
                modifier = Modifier
                    .padding(bottom = 75.dp.scaleSize())
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProgramSummaryEntry(modifier: Modifier, navigateToProgramSummary: () -> Unit) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Image(
        painter = painterResource(id = R.drawable.ic_program_summary),
        contentDescription = "program summary",
        alpha = if (isPressed) 0.5f else 1.0f,
        modifier = modifier
            .size(40.dp.scaleSize())
            .testTag("program_summary_button")
            .clickableNoEffect(interactionSource) { navigateToProgramSummary() }
    )
}

@Composable
private fun SettingsBox(
    goalSettingsEnabled: Boolean,
    onRetakeAssessment: () -> Unit,
    onGoalSettings: () -> Unit,
    onEraseUserData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val bgColor = if (open) Color(0x99000000) else Color.Transparent

    Box(modifier = modifier.background(bgColor)) {
        if (!open) {
            val interactionSource: MutableInteractionSource =
                remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            Image(
                modifier = Modifier
                    .padding(end = 20.dp.scaleSize(), top = 24.dp.scaleSize())
                    .size(30.dp.scaleSize())
                    .align(Alignment.TopEnd)
                    .testTag("settings_button")
                    .clickableNoEffect(interactionSource) { open = true },
                painter = painterResource(id = R.drawable.ic_settings_gear),
                alpha = if (isPressed) 0.5f else 1.0f,
                contentDescription = "Settings button",
                contentScale = ContentScale.FillBounds
            )
        } else {
            SettingsContent(
                goalSettingsEnabled = goalSettingsEnabled,
                onRetakeAssessment = onRetakeAssessment,
                onGoalSettings = onGoalSettings,
                onEraseUserData = onEraseUserData,
                onCloseSettings = { open = false }
            )
        }
    }
}

@Composable
private fun SettingsContent(
    goalSettingsEnabled: Boolean,
    onRetakeAssessment: () -> Unit,
    onGoalSettings: () -> Unit,
    onEraseUserData: () -> Unit,
    onCloseSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        val closeButtonSource: MutableInteractionSource = remember { MutableInteractionSource() }
        val isClosePressed by closeButtonSource.collectIsPressedAsState()
        Image(
            modifier = Modifier
                .padding(end = 20.dp.scaleSize(), top = 20.dp.scaleSize())
                .size(30.dp.scaleSize())
                .clickableNoEffect(closeButtonSource) { onCloseSettings() }
                .align(Alignment.TopEnd)
                .testTag("settings_close_button"),
            painter = painterResource(id = R.drawable.ic_close),
            alpha = if (isClosePressed) 0.5f else 1.0f,
            contentDescription = "Settings close button",
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier.padding(all = 20.dp.scaleSize()),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(id = R.string.settings_tc),
                textAlign = TextAlign.Left,
                fontSize = 24.sp.scaleSize(),
                color = Color.White,
                style = oxaniumStyle,
                modifier = Modifier
                    .testTag("settings_string")
                    .padding(bottom = 15.dp.scaleSize())
            )

            val interactionSourceGoalSettings: MutableInteractionSource =
                remember { MutableInteractionSource() }
            val isGoalSettingsPressed by interactionSourceGoalSettings.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickableNoEffect(interactionSourceGoalSettings) { if (goalSettingsEnabled) onGoalSettings() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(id = R.string.mm_goal_settings_tc),
                    textAlign = TextAlign.Left,
                    fontSize = 21.sp.scaleSize(),
                    color = when {
                        !goalSettingsEnabled -> Color.Gray
                        isGoalSettingsPressed -> Color.White.copy(alpha = 0.5f)
                        else -> Color.White
                    },
                    style = oxaniumStyle,
                    modifier = Modifier.testTag("goal_settings_button")
                )
            }

            val retakeSource: MutableInteractionSource = remember { MutableInteractionSource() }
            val isRetakePressed by retakeSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickableNoEffect(retakeSource) { onRetakeAssessment() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(id = R.string.retake_assessment_tc),
                    textAlign = TextAlign.Left,
                    fontSize = 21.sp.scaleSize(),
                    color = if (isRetakePressed) Color.White.copy(alpha = 0.5f) else Color.White,
                    style = oxaniumStyle,
                    modifier = Modifier.testTag("retake_assessment_button")
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 5.dp.scaleSize()),
                color = MmColors.futureGray
            )

            val eraseSource: MutableInteractionSource = remember { MutableInteractionSource() }
            val isErasePressed by eraseSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickableNoEffect(eraseSource) {
                        onEraseUserData()
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(id = R.string.erase_data_tc),
                    textAlign = TextAlign.Left,
                    fontSize = 21.sp.scaleSize(),
                    color = if (isErasePressed) MmColors.missedRed.copy(alpha = 0.5f) else MmColors.missedRed,
                    style = oxaniumStyle,
                    modifier = Modifier.testTag("erase_program_button")
                )
            }
        }
    }
}

@Composable
private fun WeekBar(
    modifier: Modifier = Modifier,
    numPages: Int,
    selectedPage: Int,
    onPageSelected: (Int) -> Unit
) {
    val pagerState = rememberPagerState(selectedPage) { numPages }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect {
            onPageSelected(it)
        }
    }

    LaunchedEffect(selectedPage) {
        pagerState.animateScrollToPage(selectedPage)
    }

    Row(modifier = modifier.drawWithCache {
        val lineBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                MmColors.futuristicBlue.copy(alpha = 0.6f),
                Color.Transparent
            )
        )
        onDrawBehind {
            drawLine(
                brush = lineBrush,
                start = Offset(0f, 0f),
                end = Offset(this.size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )

            drawLine(
                brush = lineBrush,
                start = Offset(0f, this.size.height),
                end = Offset(this.size.width, this.size.height),
                strokeWidth = 1.dp.toPx()
            )
        }
    }) {
        Spacer(modifier = Modifier.weight(5f))

        //Left Arrow
        MmDoubleArrowsRight(
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .weight(1f)
                .rotate(180f)
                .alpha(0.6f)
                .align(Alignment.CenterVertically)
                .testTag("week_bar_left_arrow")
        ) {
            onPageSelected(selectedPage - 1)
        }

        HorizontalPager(
            modifier = Modifier.weight(7f),
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 170.dp.scaleSize())
        ) { page ->
            val isLastPage = page == numPages - 1
            val labelText = if (isLastPage) {
                stringResource(id = R.string.event_uc)
            } else {
                stringResource(id = R.string.week_uc) + " " + (page + 1)
            }

            WeekLabel(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onPageSelected(page) }
                    .graphicsLayer {
                        val pageOffset = ((pagerState.currentPage - page)
                                + pagerState.currentPageOffsetFraction).absoluteValue

                        alpha = lerp(
                            start = 0.4f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                    }
                    .testTag("week_label"),
                selected = selectedPage == page,
                labelText = labelText
            )
        }

        //Right Arrow
        MmDoubleArrowsRight(
            modifier = Modifier
                .fillMaxHeight(0.6f)
                .weight(1f)
                .alpha(0.6f)
                .align(Alignment.CenterVertically)
                .testTag("week_bar_right_arrow")
        ) {
            onPageSelected(selectedPage + 1)
        }

        Spacer(modifier = Modifier.weight(5f))
    }
}

@Composable
private fun WeekLabel(modifier: Modifier = Modifier, selected: Boolean, labelText: String) {
    val bgColor = if (selected) MmColors.futuristicBlue.copy(alpha = 0.25f) else Color.Transparent

    Box(modifier = modifier.background(bgColor)) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            text = labelText,
            color = Color.White,
            fontSize = 21.sp.scaleSize(),
            textAlign = TextAlign.Center,
            style = oxaniumStyle
        )


        if (selected) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter),
                onDraw = {
                    drawLine(
                        color = MmColors.futuristicBlue,
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                })

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.BottomCenter),
                onDraw = {
                    drawLine(
                        color = MmColors.futuristicBlue,
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                })
        }
    }
}

@Composable
private fun EventPager(
    modifier: Modifier = Modifier,
    pagedEvents: List<List<IEvent>>,
    currentTime: Long,
    nextFutureEvent: Int,
    timeUntilWorkout: Long,
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    onClickedItem: (IEvent) -> Unit,
    onMissedWorkoutClicked: (IEvent) -> Unit,
    onNonWorkoutEventClicked: (icon: Int, title: Int, label: Int) -> Unit
) {
    val pagerState = rememberPagerState(selectedPage) { pagedEvents.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect {
            onPageSelected(it)
        }
    }

    LaunchedEffect(selectedPage) {
        pagerState.animateScrollToPage(selectedPage)
    }

    HorizontalPager(
        modifier = modifier,
        state = pagerState
    )
    { page ->
        val previousStatus = getPreviousPageStatus(page, pagedEvents, currentTime)
        WeekView(
            weekEvents = pagedEvents[page],
            currentTime = currentTime,
            previousStatus = previousStatus,
            nextFutureEvent = nextFutureEvent,
            timeUntilWorkout = timeUntilWorkout,
            lastWeek = page == pagedEvents.size - 1,
            onClickedItem = onClickedItem,
            onMissedWorkoutClicked = onMissedWorkoutClicked,
            onNonWorkoutEventClicked = onNonWorkoutEventClicked,
        )
    }
}

private fun getPreviousPageStatus(
    page: Int,
    pageList: List<List<IEvent>>,
    currentTime: Long
): EventStatus? {
    return if (page == 0) {
        null
    } else {
        pageList[page - 1][6].getEventStatus(currentTime)
    }
}

private const val LINE_WEIGHT_ACTIVE = 2f
private const val weightAnimationTime = 1000
private const val lineAnimationTime = 1000

object JourneyScreenAnimations {
    var workoutItemAnimated by mutableStateOf(false)
    var circleItemAnimated by mutableStateOf(false)
    var lineAnimated by mutableStateOf(false)
    var currentWorkoutIndex by mutableIntStateOf(0)
    val eventsDidAnimation = mutableSetOf<Int>()
}

@Composable
private fun WeekView(
    weekEvents: List<IEvent>,
    currentTime: Long,
    previousStatus: EventStatus?,
    nextFutureEvent: Int,
    timeUntilWorkout: Long,
    lastWeek: Boolean,
    onClickedItem: (IEvent) -> Unit,
    onMissedWorkoutClicked: (IEvent) -> Unit,
    onNonWorkoutEventClicked: (icon: Int, title: Int, label: Int) -> Unit,
) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        val activeIndex = remember(currentTime) { getActiveEventIndex(weekEvents, currentTime) }

        var lineProgress by remember { mutableFloatStateOf(0f) }
        val animatedLineProgress by animateFloatAsState(
            targetValue = lineProgress,
            animationSpec = tween(durationMillis = lineAnimationTime), label = ""
        )

        var lineWeightActive by remember { mutableFloatStateOf(1f) }
        val animatedLineWeight by animateFloatAsState(
            targetValue = lineWeightActive,
            animationSpec = tween(durationMillis = weightAnimationTime), label = ""
        )

        var circleWidth by remember { mutableStateOf(75.dp) }
        val animatedCircleWidth by animateDpAsState(
            targetValue = circleWidth,
            animationSpec = tween(durationMillis = 500), label = ""
        )

        if (activeIndex >= 0) {
            LaunchedEffect(Unit) {
                if (!JourneyScreenAnimations.lineAnimated) {
                    lineWeightActive = LINE_WEIGHT_ACTIVE
                    delay(weightAnimationTime.toLong())
                    lineProgress = 1f
                    delay(lineAnimationTime.toLong())
                    circleWidth = 110.dp
                    JourneyScreenAnimations.lineAnimated = true
                }
            }
        }

        // Initial line from start
        EventDividerLine(
            modifier = Modifier.weight(
                getCurrentLineWeight(
                    -1,
                    activeIndex,
                    if (JourneyScreenAnimations.lineAnimated) LINE_WEIGHT_ACTIVE else animatedLineWeight
                )
            ),
            status = previousStatus,
            statusProgress = if (activeIndex == 0) animatedLineProgress else 1f,
            selectLineColor = previousStatus == null
        )


        weekEvents.forEachIndexed { index, event ->
            val status = event.getEventStatus(currentTime)
            val interactionSource: MutableInteractionSource =
                remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            if (index == activeIndex) {
                JourneyScreenAnimations.currentWorkoutIndex = event.index
            }

            // Event views
            when (event) {
                is WorkoutEvent -> {
                    WorkoutItem(
                        modifier = Modifier
                            .clickableNoEffect(
                                interactionSource = interactionSource,
                                debounceTimeMs = 500
                            ) {
                                if (status == EventStatus.MISSED) onMissedWorkoutClicked(event)
                                else onClickedItem(event)
                                JourneyScreenAnimations.lineAnimated = true
                                JourneyScreenAnimations.workoutItemAnimated = true
                            },
                        headerString = getEventHeader(event),
                        baseIcon = getEventIcon(event.label),
                        status = getJourneyEventStatus(event, nextFutureEvent, currentTime),
                        timeUntilWorkout = timeUntilWorkout,
                        eventLabel = event.label,
                        isPressed = isPressed,
                        animateBackground = activeIndex == index
                    )
                }

                is NonWorkoutEvent -> {
                    val circleItemScale = if (activeIndex == index) {
                        if (JourneyScreenAnimations.lineAnimated && JourneyScreenAnimations.eventsDidAnimation.contains(
                                JourneyScreenAnimations.currentWorkoutIndex
                            )
                        ) {
                            110.dp.scaleSize()
                        } else {
                            animatedCircleWidth.scaleSize()
                        }
                    } else {
                        75.dp.scaleSize()
                    }

                    if (status == EventStatus.COMPLETED) {
                        JourneyScreenAnimations.circleItemAnimated = false
                    }

                    val eventLabel = getEventLabel(event.label)
                    val eventIcon = getEventIcon(event.label)

                    CircleItem(
                        modifier = Modifier
                            .width(circleItemScale)
                            .fillMaxHeight()
                            .clickableNoEffect(
                                interactionSource = interactionSource,
                                debounceTimeMs = 500
                            ) {
                                if (status == EventStatus.CURRENT) {
                                    onClickedItem(event)
                                } else {
                                    onNonWorkoutEventClicked(
                                        eventIcon,
                                        eventLabel,
                                        R.string.rest_day_explanation_pg
                                    )
                                }
                            }, // Only current non-workout item is clickable
                        eventHeader = getEventHeader(event),
                        icon = getEventIcon(event.label),
                        status = status,
                        isPressed = isPressed,
                        animateBackground = activeIndex == index
                    )
                }
            }

            EventDividerLine(
                modifier = Modifier.weight(
                    getCurrentLineWeight(
                        index,
                        activeIndex,
                        if (JourneyScreenAnimations.lineAnimated) LINE_WEIGHT_ACTIVE else animatedLineWeight
                    )
                ),
                status = if (lastWeek && weekEvents.last() == event) null else status,
                statusProgress = if (activeIndex == index + 1) animatedLineProgress else 1f, // animate if the next element is the active event
                selectLineColor = activeIndex == index,
            )
        }
    }
}

@Composable
private fun getJourneyEventStatus(
    event: IEvent,
    nextEventIndex: Int,
    currentTime: Long
): JourneyEventStatus {
    val eventStatus = event.getEventStatus(currentTime)
    return when (eventStatus) {
        EventStatus.MISSED -> JourneyEventStatus.MISSED
        EventStatus.CURRENT -> JourneyEventStatus.CURRENT
        EventStatus.COMPLETED -> JourneyEventStatus.COMPLETED
        else -> {
            if (event.index == nextEventIndex) JourneyEventStatus.NEXT else JourneyEventStatus.FUTURE
        }
    }
}

private fun getActiveEventIndex(weekEvents: List<IEvent>, currentTime: Long): Int {
    weekEvents.forEachIndexed { index, event ->
        if (currentTime in event.startTime..event.endTime) return index
    }
    return -1
}

/**
 * Lines next to a past event should start longer and animate shorter. Lines next to the current
 * event should animate longer.
 */
private fun getCurrentLineWeight(
    currentLineIndex: Int,
    activeIndex: Int,
    animatedWeight: Float
): Float {
    if (activeIndex == -1) return 1f

    return if (activeIndex == currentLineIndex) {
        animatedWeight // The line after the current event
    } else if (activeIndex - 1 == currentLineIndex) {
        LINE_WEIGHT_ACTIVE // This is the line between the past and current event
    } else if (activeIndex - 2 == currentLineIndex) {
        1 + LINE_WEIGHT_ACTIVE - animatedWeight // The line before the past event
    } else {
        1f // Lines that are not animated
    }
}

@Composable
private fun EventDividerLine(
    modifier: Modifier = Modifier,
    status: EventStatus?,
    statusProgress: Float = 1f,
    selectLineColor: Boolean
) {
    if (status != null) {
        val baseColor = MmColors.futureGray
        val topColor = getEventLineColor(status)
        val selectedColor = if (selectLineColor) baseColor else topColor

        Canvas(modifier.height(1.dp.scaleSize())) {
            if (statusProgress < 1f && !JourneyScreenAnimations.lineAnimated) {
                drawLine(baseColor, Offset(size.width * statusProgress, 0f), Offset(size.width, 0f))
            } else {
                drawLine(topColor, Offset(size.width * statusProgress, 0f), Offset(size.width, 0f))
            }
            drawLine(selectedColor, Offset.Zero, Offset(size.width * statusProgress, 0f))
        }
    } else {
        Spacer(modifier = modifier)
    }
}

@Composable
private fun getEventLineColor(eventStatusFrom: EventStatus): Color {
    return when (eventStatusFrom) {
        EventStatus.COMPLETED -> MmColors.lightGreen
        EventStatus.MISSED -> MmColors.missedRed
        EventStatus.CURRENT, EventStatus.FUTURE -> MmColors.futureGray
    }
}

@Composable
private fun getEventLabel(label: EventLabel): Int {
    return when (label) {
        EventLabel.WORKOUT_TEMPO -> R.string.tempo_uc
        EventLabel.WORKOUT_INTERVAL -> R.string.intervals_uc
        EventLabel.WORKOUT_LONG_SLOW_DISTANCE -> R.string.distance_uc
        EventLabel.REST -> R.string.rest_tc
        EventLabel.CROSS -> R.string.cross_uc
        EventLabel.ACTIVE_RECOVERY -> R.string.active_recovery_uc
        EventLabel.RACE -> R.string.virtual_competition_tc
    }
}

@Composable
private fun getEventHeader(event: IEvent): String {
    return when (event.label) {
        EventLabel.RACE -> stringResource(R.string.race_day_tc)
        else -> getEventHeaderString(event.getDayNum() + 1)
    }
}

@Composable
private fun getEventIcon(label: EventLabel): Int {
    return when (label) {
        EventLabel.WORKOUT_TEMPO, EventLabel.WORKOUT_INTERVAL, EventLabel.WORKOUT_LONG_SLOW_DISTANCE -> R.drawable.ic_running_man_mm
        EventLabel.REST -> R.drawable.ic_rest_mm
        EventLabel.CROSS, EventLabel.ACTIVE_RECOVERY -> R.drawable.ic_cross_weight_mm
        EventLabel.RACE -> R.drawable.ic_race_flag
    }
}

private val workoutItemWidth = 200.dp
private val workoutItemHeight = 350.dp

@Composable
private fun WorkoutItem(
    modifier: Modifier = Modifier,
    headerString: String,
    baseIcon: Int,
    status: JourneyEventStatus,
    timeUntilWorkout: Long,
    eventLabel: EventLabel,
    isPressed: Boolean,
    animateBackground: Boolean
) {
    //setup the expanded animation for current workout
    val currentStatus =
        status == JourneyEventStatus.COMPLETED || status == JourneyEventStatus.CURRENT
    val currentWorkoutAnimation = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (!JourneyScreenAnimations.workoutItemAnimated && !JourneyScreenAnimations.eventsDidAnimation.contains(
                JourneyScreenAnimations.currentWorkoutIndex
            )
        ) {
            if (currentStatus) {
                delay((weightAnimationTime + lineAnimationTime).toLong())
                currentWorkoutAnimation.animateTo(
                    targetValue = if (animateBackground) 1.05f else 1f,
                    animationSpec = tween(100)
                )
                currentWorkoutAnimation.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(100)
                )
                JourneyScreenAnimations.workoutItemAnimated = true
            }
            if (status == JourneyEventStatus.COMPLETED) {
                JourneyScreenAnimations.eventsDidAnimation.add(JourneyScreenAnimations.currentWorkoutIndex)
            }
        }
    }

    // setup the transition from a workout item to another workout item
    var currentWorkoutItemTransition by remember {
        mutableStateOf(if (status == JourneyEventStatus.CURRENT) JourneyEventStatus.FUTURE else JourneyEventStatus.CURRENT)
    }

    Column(
        modifier = modifier
            .width(workoutItemWidth.scaleSize())
            .testTag("workout_item_container")
    ) {
        AutoSizeText(
            modifier = Modifier
                .padding(bottom = 14.dp.scaleSize())
                .fillMaxWidth()
                .height(30.dp.scaleSize())
                .testTag("workout_item_header_label"),
            text = headerString,
            textAlign = TextAlign.Center,
            color = Color.White,
            maxFontSize = 21.sp.scaleSize(),
            style = oxaniumStyle,
            fontWeight = FontWeight.Bold,
        )

        Box(
            modifier = Modifier
                .height(workoutItemHeight.scaleSize())
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = currentWorkoutAnimation.value,
                    scaleY = currentWorkoutAnimation.value
                )
        ) {
            if (animateBackground && !JourneyScreenAnimations.eventsDidAnimation.contains(
                    JourneyScreenAnimations.currentWorkoutIndex
                )
            ) {
                LaunchedEffect(Unit) {
                    if (currentStatus && !JourneyScreenAnimations.workoutItemAnimated) {
                        delay((weightAnimationTime + lineAnimationTime).toLong())
                        currentWorkoutItemTransition = status
                        JourneyScreenAnimations.workoutItemAnimated = true
                    } else {
                        currentWorkoutItemTransition = status
                    }
                }
                WorkoutItemBackground(
                    modifier = modifier,
                    status = currentWorkoutItemTransition,
                    isPressed = isPressed
                )
            } else {
                WorkoutItemBackground(
                    modifier = modifier,
                    status = status,
                    isPressed = isPressed
                )
            }

            Column(
                Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                WorkoutLabel(
                    modifier = Modifier
                        .padding(horizontal = 25.dp.scaleSize())
                        .testTag("workout_item_label"),
                    eventStatus = if (animateBackground && !JourneyScreenAnimations.eventsDidAnimation.contains(
                            JourneyScreenAnimations.currentWorkoutIndex
                        )
                    ) currentWorkoutItemTransition else status,
                    timeUntilWorkout = timeUntilWorkout,
                    isPressed = isPressed,
                    eventLabel = eventLabel
                )

                Spacer(modifier = Modifier.weight(2f))

                WorkoutIcon(
                    modifier = Modifier
                        .weight(2.6f)
                        .width(60.dp.scaleSize())
                        .testTag("workout_item_Icon"),
                    eventStatus = if (animateBackground && !JourneyScreenAnimations.eventsDidAnimation.contains(
                            JourneyScreenAnimations.currentWorkoutIndex
                        )
                    ) currentWorkoutItemTransition else status,
                    baseIcon = baseIcon,
                    isPressed = isPressed,
                    eventLabel = eventLabel
                )
                Spacer(modifier = Modifier.weight(1.8f))

                Spacer(Modifier.weight(3f))
                WorkoutSubText(
                    modifier = Modifier
                        .padding(horizontal = 10.dp.scaleSize())
                        .testTag("workout_item_sub_label"),
                    eventStatus = if (animateBackground && !JourneyScreenAnimations.eventsDidAnimation.contains(
                            JourneyScreenAnimations.currentWorkoutIndex
                        )
                    ) currentWorkoutItemTransition else status,
                    eventLabel = eventLabel,
                    isPressed = isPressed
                )
                Spacer(Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(30.dp.scaleSize()))
    }
}

@Composable
private fun getEventHeaderString(dayNum: Int): String {
    return String.format(stringResource(id = R.string.day_num_tc), dayNum)
}

@Composable
private fun WorkoutItemBackground(
    modifier: Modifier = Modifier,
    status: JourneyEventStatus,
    isPressed: Boolean
) {
    if (status == JourneyEventStatus.CURRENT) {
        Image(
            modifier = modifier
                .wrapContentSize(align = Alignment.Center, unbounded = true)
                .testTag("workout_item_background"),
            painter = getScaledCurrentBg(resourceId = getWorkoutItemBackground(status, isPressed)),
            contentDescription = "workout bg",
            contentScale = ContentScale.FillBounds
        )
    } else {
        Image(
            modifier = modifier
                .fillMaxSize()
                .testTag("workout_item_background"),
            painter = painterResource(id = getWorkoutItemBackground(status, isPressed)),
            contentDescription = "workout bg",
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun getScaledCurrentBg(resourceId: Int): BitmapPainter {
    val resources = LocalContext.current.resources
    val density = LocalDensity.current
    val baseWidth = with(density) { workoutItemWidth.scaleSize().toPx() }
    val baseHeight = with(density) { workoutItemHeight.scaleSize().toPx() }

    return remember(resourceId) {
        val yScaleFactor = 1.12f
        val xScaleFactor = 1.18f

        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        val scaledBitmap = bitmap.scale(
            width = (baseWidth * xScaleFactor).toInt(),
            height = (baseHeight * yScaleFactor).toInt()
        )
        BitmapPainter(scaledBitmap.asImageBitmap())
    }
}

@Composable
private fun WorkoutSubText(
    modifier: Modifier = Modifier,
    eventStatus: JourneyEventStatus,
    eventLabel: EventLabel,
    isPressed: Boolean
) {
    val textId: Int
    val color: Color

    when (eventStatus) {
        JourneyEventStatus.COMPLETED -> {
            textId = R.string.view_summary_tc
            color = if (isPressed) MmColors.deepBlue else MmColors.lightGreen
        }

        JourneyEventStatus.MISSED -> {
            textId = R.string.recover_workout_tc
            color = if (isPressed) MmColors.deepBlue else MmColors.missedRed
        }

        JourneyEventStatus.CURRENT -> {
            textId = when (eventLabel) {
                EventLabel.RACE -> R.string.tap_to_start_5k_race_tc
                else -> R.string.start_workout_tc
            }
            color = if (isPressed) MmColors.deepBlue else MmColors.futuristicBlue
        }

        JourneyEventStatus.FUTURE -> {
            textId = when(eventLabel){
                EventLabel.RACE -> R.string.tap_to_preview_tc
                else -> R.string.preview_workout_tc
            }

            color = if (isPressed) MmColors.deepBlue.copy(alpha = 0.5f) else MmColors.futureGray
        }

        JourneyEventStatus.NEXT -> {
            textId = R.string.preview_workout_tc
            color = if (isPressed) MmColors.deepBlue.copy(alpha = 0.5f) else Color.White
        }
    }

    Text(
        modifier = modifier,
        text = stringResource(id = textId),
        color = color,
        textAlign = TextAlign.Center,
        fontSize = 16.sp.scaleSize(),
    )
}

private fun getWorkoutItemBackground(eventStatus: JourneyEventStatus, isPressed: Boolean): Int {
    return when (eventStatus) {
        JourneyEventStatus.COMPLETED -> if (isPressed) R.drawable.mm_workout_complete_on_press_bg else R.drawable.mm_workout_complete_bg
        JourneyEventStatus.MISSED -> if (isPressed) R.drawable.mm_workout_missed_on_press_bg else R.drawable.mm_workout_missed_bg
        JourneyEventStatus.CURRENT -> if (isPressed) R.drawable.mm_workout_current_on_press_bg else R.drawable.mm_workout_current_bg
        JourneyEventStatus.FUTURE -> if (isPressed) R.drawable.mm_workout_future_on_press_bg else R.drawable.mm_workout_future_bg
        JourneyEventStatus.NEXT -> if (isPressed) R.drawable.mm_workout_next_on_press_bg else R.drawable.mm_workout_next_bg
    }
}

@Composable
private fun WorkoutIcon(
    modifier: Modifier = Modifier,
    eventStatus: JourneyEventStatus,
    baseIcon: Int,
    isPressed: Boolean,
    eventLabel: EventLabel
) {
    val icon = getWorkoutEventIconSettings(baseIcon, eventStatus, eventLabel)

    Image(
        modifier = modifier,
        painter = painterResource(id = icon.iconId),
        colorFilter = ColorFilter.tint(if (isPressed) icon.pressedTint else icon.tint),
        contentDescription = "workout icon",
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center,
    )
}

// info for displaying an event icon
private data class EventIconSettings(
    val iconId: Int,
    val tint: Color,
    val pressedTint: Color,
)

@Composable
private fun getWorkoutEventIconSettings(
    baseIcon: Int,
    eventStatus: JourneyEventStatus,
    eventLabel: EventLabel,
): EventIconSettings {
    val icon: Int
    val tint: Color
    val pressedTint: Color

    when (eventStatus) {
        JourneyEventStatus.COMPLETED -> {
            icon = R.drawable.ic_star_mm
            tint = MmColors.lightGreen
            pressedTint = MmColors.lightGreen
        }
        JourneyEventStatus.MISSED -> {
            icon = R.drawable.ic_skipped_mm
            tint = MmColors.missedRed
            pressedTint = MmColors.missedRed
        }
        JourneyEventStatus.CURRENT -> {
            icon = baseIcon
            tint = MmColors.futuristicBlue
            pressedTint = MmColors.deepBlue
        }
        JourneyEventStatus.FUTURE -> {
            icon = when(eventLabel){
                EventLabel.RACE -> R.drawable.ic_race_flag
                else -> R.drawable.ic_lock_mm
            }
            tint = Color.White.copy(alpha = 0.5f)
            pressedTint = Color.White.copy(alpha = 0.5f)
        }
        JourneyEventStatus.NEXT -> {
            icon = R.drawable.ic_lock_mm
            tint = Color.White
            pressedTint = Color.White
        }
    }

    return EventIconSettings(icon, tint, pressedTint)
}

@Composable
private fun WorkoutLabel(
    modifier: Modifier = Modifier,
    eventStatus: JourneyEventStatus,
    timeUntilWorkout: Long,
    isPressed: Boolean,
    eventLabel: EventLabel
) {
    val settings = getWorkoutLabelSettings(eventStatus, eventLabel)

    Column(modifier = modifier) {
        // Show when the workout will be eg Today's Workout
        Text(
            modifier = Modifier,
            text = stringResource(id = settings.textId),
            color = if (isPressed) MmColors.deepBlue else settings.color,
            textAlign = TextAlign.Center,
            fontSize = 21.sp.scaleSize(),
            style = oxaniumStyle,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp.scaleSize()
        )

        // Show timer only for next upcoming workout
        if (eventStatus == JourneyEventStatus.NEXT) {
            val formattedTime = formatTimeRemaining(timeUntilWorkout)
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp.scaleSize())
                    .height(24.dp.scaleSize()),
                text = formattedTime,
                color = MmColors.futuristicBlue,
                fontSize = 24.sp.scaleSize(),
                textAlign = TextAlign.Center,
                style = oxaniumStyle,
                fontWeight = FontWeight.Bold
            )
        }
        else {
            Spacer(modifier = Modifier.height(24.dp.scaleSize()))
        }
    }
}

private data class WorkoutLabelSettings(
    val textId: Int,
    val color: Color,
)

@Composable
private fun getWorkoutLabelSettings(
    eventStatus: JourneyEventStatus,
    eventLabel: EventLabel,
): WorkoutLabelSettings {
    val textId: Int
    val color: Color
    when (eventStatus) {
        JourneyEventStatus.COMPLETED -> {
            textId = R.string.workout_complete_exclamation_tc
            color = MmColors.lightGreen
        }

        JourneyEventStatus.MISSED -> {
            textId = R.string.missed_workout_tc
            color = MmColors.missedRed
        }

        JourneyEventStatus.CURRENT -> {
            textId = R.string.todays_workout_tc
            color = MmColors.futuristicBlue
        }

        JourneyEventStatus.FUTURE -> {
            textId = when (eventLabel) {
                EventLabel.RACE -> R.string.virtual_competition_tc
                else -> R.string.future_workout_tc
            }
            color = Color.White.copy(alpha = 0.5f)
        }

        JourneyEventStatus.NEXT -> {
            textId = R.string.next_workout_in_tc
            color = Color.White
        }
    }

    return WorkoutLabelSettings(textId, color)
}

@Composable
fun CircleItem(
    modifier: Modifier = Modifier,
    eventHeader: String,
    icon: Int,
    status: EventStatus,
    isPressed: Boolean,
    animateBackground: Boolean
) {
    var currentCircleItemTransition by remember {
        mutableStateOf(if (status == EventStatus.CURRENT) EventStatus.FUTURE else EventStatus.CURRENT)
    }

    //set the color transition of the circle item and a time to implement them
    if (animateBackground && !JourneyScreenAnimations.eventsDidAnimation.contains(
            JourneyScreenAnimations.currentWorkoutIndex
        )
    ) {
        LaunchedEffect(Unit) {
            if (!JourneyScreenAnimations.circleItemAnimated) {
                delay((weightAnimationTime + lineAnimationTime).toLong())
                currentCircleItemTransition = status
                JourneyScreenAnimations.circleItemAnimated = true
                JourneyScreenAnimations.eventsDidAnimation.add(JourneyScreenAnimations.currentWorkoutIndex)
            } else {
                currentCircleItemTransition = status
            }
        }
    } else {
        currentCircleItemTransition = status
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .testTag("rest_day_header_label")
        ) {
            AutoSizeText(
                modifier = Modifier
                    .padding(bottom = 12.dp.scaleSize())
                    .align(Alignment.BottomCenter),
                text = eventHeader,
                color = Color.White,
                maxFontSize = 21.sp.scaleSize(),
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = oxaniumStyle,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .drawBehind {

                    // Background
                    if (status == EventStatus.COMPLETED) {
                        drawCircle(
                            color = getCircleItemColor(status).copy(alpha = 0.25f),
                            radius = this.size.width / 2,
                        )
                    } else if (status == EventStatus.CURRENT && isPressed) {
                        drawCircle(
                            color = getCircleItemColor(status).copy(alpha = 0.6f),
                            radius = this.size.width / 2,
                        )
                    }

                    // Border
                    drawCircle(
                        color = getCircleItemColor(currentCircleItemTransition),
                        radius = this.size.width / 2,
                        style = Stroke(1.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Center Icon
            val iconToUse = if (status != EventStatus.COMPLETED) icon else R.drawable.ic_star_mm
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .testTag("rest_day_circle_icon"),
                painter = painterResource(id = iconToUse),
                contentDescription = "circle item icon",
                colorFilter = ColorFilter.tint(
                    if (isPressed) MmColors.deepBlue else getCircleItemColor(
                        currentCircleItemTransition
                    )
                ),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun getCircleItemColor(eventStatus: EventStatus): Color {
    return when (eventStatus) {
        EventStatus.COMPLETED, EventStatus.MISSED -> MmColors.lightGreen
        EventStatus.CURRENT -> MmColors.futuristicBlue
        EventStatus.FUTURE -> MmColors.futureGray
    }
}

enum class EventStatus {
    COMPLETED, CURRENT, FUTURE, MISSED
}

enum class JourneyEventStatus {
    COMPLETED, CURRENT, NEXT, FUTURE, MISSED
}

@Composable
private fun NextWorkoutText(
    modifier: Modifier = Modifier,
    timeUntilWorkout: Long,
    selectedPage: Int
) {
    val displayedText = getTimeNotificationText(timeUntilWorkout, selectedPage)

    Text(
        modifier = modifier
            .testTag("back_next_workout_available_text"),
        text = displayedText,
        color = if (selectedPage == 8) MmColors.futuristicBlue else Color.White,
        fontSize = 36.sp.scaleSize(),
        textAlign = TextAlign.Center,
        style = oxaniumStyle,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun getTimeNotificationText(timeUntilWorkout: Long, selectedPage: Int): String {
    return if (timeUntilWorkout == 0L) {
        when (selectedPage){
            8 -> stringResource(R.string.race_day_notification_text_tc)//The race day event page
            else ->  stringResource(R.string.next_workout_is_available_tc)
        }
    } else {
        val millisInDay = 86_400_000L // 1000 ms * 60 sec * 60 min * 24 hr
        val days = timeUntilWorkout / millisInDay + 1
        if (days == 1L) {
            stringResource(R.string.come_back_tomorrow_tc)
        } else {
            val comeBackString = stringResource(R.string.come_back_in_x_days_tc)
            String.format(comeBackString, days)
        }
    }
}

/**
 * Formats the time remaining until the next workout into hh:mm:ss
 *
 * @param milliseconds The number of milliseconds
 * @return The time string
 */
@Composable
private fun formatTimeRemaining(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
}

@AllDevicePreviews
@Composable
private fun JourneyPreview() {
    val programEvents: List<List<IEvent>> = createTestData()
    val testTime = 350L

    JourneyScreenContent(
        eventPages = programEvents,
        currentTime = testTime,
        timeUntilWorkout = 1000000,
        nextFutureEvent = 4,
        selectedPage = 0,
        autoDisplayGoalTimeInfoPopup = true,
        goalSettingsEnabled = false,
        onPageSelected = {},
        onClickedItem = {},
        onResetWorkout = {},
        onGoalSettings = { _, _ -> },
        onEraseUserData = {},
        onRetakeAssessment = {},
        navigateToSplash = {},
        navigateToProgramSummary = {}
    )
}

@Preview(widthDp = 370, heightDp = 200)
@Composable
private fun SettingsPreview() {
    SettingsContent(
        goalSettingsEnabled = true,
        onRetakeAssessment = { /*TODO*/ },
        onGoalSettings = { /*TODO*/ },
        onEraseUserData = { /*TODO*/ },
        onCloseSettings = { /*TODO*/ }
    )
}

private fun createTestData(): List<List<IEvent>> {
    val eventList = ArrayList<List<IEvent>>()
    eventList.add(getSingleWeekTestDataList(0))
    eventList.add(getSingleWeekTestDataList(7))
    eventList.add(getSingleWeekTestDataList(14))
    eventList.add(getSingleWeekTestDataList(21))
    eventList.add(getSingleWeekTestDataList(28))
    return eventList
}

private fun getSingleWeekTestDataList(index: Int): List<IEvent> {
    val eventList = ArrayList<IEvent>()
    var i = index
    eventList.add(
        WorkoutEvent(
            startTime = i * 100L,
            endTime = i * 100L + 100L,
            index = i++,
            completed = true,
            label = EventLabel.WORKOUT_TEMPO
        )
    )

    eventList.add(
        NonWorkoutEvent(
            startTime = i * 100L,
            endTime = i * 100L + 100L,
            index = i++,
            completed = false,
            label = EventLabel.CROSS
        )
    )

    eventList.add(
        WorkoutEvent(
            startTime = i * 100L,
            endTime = i * 100L + 100L,
            index = i++,
            completed = false,
            label = EventLabel.WORKOUT_TEMPO
        )
    )

    eventList.add(
        NonWorkoutEvent(
            startTime = i * 100L,
            endTime = i * 100L + 100L,
            index = i++,
            completed = false,
            label = EventLabel.REST
        )
    )

    eventList.add(
        WorkoutEvent(
            startTime = i * 100L,
            endTime = i * 100L + 100L,
            index = i++,
            completed = false,
            label = EventLabel.WORKOUT_TEMPO
        )
    )

    eventList.add(
        NonWorkoutEvent(
            startTime = i * 100L,
            endTime = i * 100L + 100L,
            index = i++,
            completed = false,
            label = EventLabel.REST
        )
    )

    eventList.add(
        NonWorkoutEvent(
            startTime = i * 100L,
            endTime = i * 100L + 100L,
            index = i,
            completed = false,
            label = EventLabel.CROSS
        )
    )

    return eventList
}
