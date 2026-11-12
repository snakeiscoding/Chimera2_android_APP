package com.jht.progression.ui.setup.journey

import android.icu.util.ULocale
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.util.LocalePreferences
import com.jht.progression.R
import com.jht.progression.oxaniumStyle
import com.jht.progression.robotoStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.widgets.MmOptionDialog
import com.jht.progression.ui.widgets.MmSetupFrame
import com.jht.progression.ui.widgets.MmThemedButton
import com.jht.progression.ui.widgets.MmTimeOptionDialog
import com.jht.progression.ui.widgets.convertToPx
import com.jht.progression.util.createTintedPainter
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.unit.UnitFormatter
import com.jht.widgets.scaleSize
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@Composable
fun ProgramSummaryScreen(
    viewModel: ProgramSummaryViewModel,
    goalSettingsEnabled: Boolean,
    onBackPressed: (Boolean) -> Unit,
    onResetProgram: () -> Unit,
    onEditGoalTimePressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var showGoalTimeDialog by remember { mutableStateOf(false) }

    ProgramSummaryScreenFrame(
        firstPageData = uiState.firstPageData,
        secondPageData = uiState.secondPageData,
        showEndOfProgram = uiState.isEndOfProgram,
        estimatedTimeChange = uiState.estimatedTimeChange,
        estimatedPaceChange = uiState.estimatedPaceChange,
        currentPaceValue = uiState.currentPaceValue,
        estimatedRaceTime = uiState.estimatedRaceTime,
        goalSettingsEnabled = goalSettingsEnabled,
        goalTime = uiState.goalTime,
        goalTimeAdjusted = uiState.goalTimeAdjusted,
        onBackPressed = { onBackPressed(uiState.isEndOfProgram) },
        onResetProgramBtnClicked = { showResetDialog = true },
        onGoalTimeClicked = {
            if (!uiState.isEndOfProgram && uiState.goalTimeAdjusted) showGoalTimeDialog = true
        },
        onEditGoalTimeClicked = onEditGoalTimePressed
    )

    MmOptionDialog(
        open = showResetDialog,
        icon = createTintedPainter(R.drawable.ic_warning_triangle, MmColors.missedRed),
        titleText = stringResource(id = R.string.reset_program_tc),
        bodyText = stringResource(R.string.reset_program_pg),
        acceptButtonText = stringResource(R.string.reset_uc),
        acceptButtonColor = MmColors.missedRed,
        dismissButtonText = stringResource(id = R.string.cancel_uc),
        onDismiss = { showResetDialog = false },
        onYesClicked = {
            onResetProgram()
            JourneyScreenAnimations.eventsDidAnimation.clear()
        }
    )

    MmTimeOptionDialog(
        open = showGoalTimeDialog,
        icon = painterResource(id = R.drawable.ic_stopwatch_outline),
        titleText = stringResource(id = R.string.adjust_goal_time_tc),
        bodyText = stringResource(id = R.string.goal_time_adjusted_pg),
        timeText = uiState.goalTime,
        dismissButtonText = stringResource(id = R.string.reject_uc),
        acceptButtonText = stringResource(id = R.string.accept_uc),
        dismissOnOutsideTap = false,
        onDismiss = {
            showGoalTimeDialog = false
            viewModel.confirmGoalTimeAdjustment(false)
        },
        onYesClicked = {
            showGoalTimeDialog = false
            viewModel.confirmGoalTimeAdjustment(true)
        },
    )
}

@Composable
fun ProgramSummaryScreenFrame(
    firstPageData: List<ProgramSummaryViewModel.ProgramSummaryLine>,
    secondPageData: List<ProgramSummaryViewModel.ProgramSummaryLine>,
    showEndOfProgram: Boolean,
    estimatedTimeChange: Int,
    estimatedPaceChange: Int,
    currentPaceValue: String,
    estimatedRaceTime: String,
    goalSettingsEnabled: Boolean,
    goalTime: String,
    goalTimeAdjusted: Boolean,
    onBackPressed: () -> Unit,
    onResetProgramBtnClicked: () -> Unit,
    onGoalTimeClicked: () -> Unit,
    onEditGoalTimeClicked: () -> Unit
) {
    val backIcon = if (showEndOfProgram) {
        ImageVector.vectorResource(id = R.drawable.ic_home)
    } else {
        Icons.AutoMirrored.Filled.ArrowBack
    }

    MmSetupFrame(
        onBackPressed = onBackPressed,
        backIcon = backIcon
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("Program Summary Screen"),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Program summary title
            Box(
                modifier = Modifier
                    .weight(.2f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val titleText =
                    if (showEndOfProgram) stringResource(id = R.string.program_complete_tc)
                    else stringResource(id = R.string.program_summary_tc)
                Text(
                    text = titleText,
                    style = oxaniumStyle.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 54.sp.scaleSize(),
                        fontSize = 48.sp.scaleSize(),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.testTag(titleText.lowercase())
                )
            }

            // Program summary content
            Box(
                modifier = Modifier
                    .weight(.8f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                ProgramSummaryContent(
                    firstPageData = firstPageData,
                    secondPageData = secondPageData,
                    showEndOfProgram = showEndOfProgram,
                    estimatedTimeChange = estimatedTimeChange,
                    estimatedPaceChange = estimatedPaceChange,
                    currentPaceValue = currentPaceValue,
                    estimatedRaceTime = estimatedRaceTime,
                    goalSettingsEnabled = goalSettingsEnabled,
                    goalTime = goalTime,
                    goalTimeAdjusted = goalTimeAdjusted,
                    onGoalTimeClicked = onGoalTimeClicked,
                    onEditGoalTimeClicked = onEditGoalTimeClicked
                )
            }
        }

        if (showEndOfProgram) {
            MmThemedButton(
                modifier = Modifier
                    .padding(end = 40.dp.scaleSize(), bottom = 40.dp.scaleSize())
                    .size(257.dp.scaleSize(), 75.dp.scaleSize())
                    .align(Alignment.BottomEnd)
                    .testTag("reset_program_button"),
                text = stringResource(id = R.string.reset_program_uc),
                color = MmColors.futuristicBlue,
                onClick = onResetProgramBtnClicked,
                textStyle = oxaniumStyle.copy(fontSize = 24.sp.scaleSize(), fontWeight = FontWeight.Bold, lineHeight = 24.sp.scaleSize())
            )
        }
    }
}

@Composable
private fun ProgramSummaryContent(
    firstPageData: List<ProgramSummaryViewModel.ProgramSummaryLine>,
    secondPageData: List<ProgramSummaryViewModel.ProgramSummaryLine>,
    showEndOfProgram: Boolean,
    estimatedTimeChange: Int,
    estimatedPaceChange: Int,
    currentPaceValue: String,
    estimatedRaceTime: String,
    goalSettingsEnabled: Boolean,
    goalTime: String,
    goalTimeAdjusted: Boolean,
    onGoalTimeClicked: () -> Unit,
    onEditGoalTimeClicked: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // the two content pages of program summary
        Box(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        )
        {
            HorizontalPager(state = pagerState) { pageIndex ->
                currentPage = pageIndex
                if (currentPage == 0) {
                    ProgramSummaryFirstPage(
                        firstPageData = firstPageData,
                        currentPaceValue = currentPaceValue,
                        showEndOfProgram = showEndOfProgram,
                        estimatedTimeChange = estimatedTimeChange,
                        estimatedPaceChange = estimatedPaceChange,
                        estimatedRaceTime = estimatedRaceTime,
                        goalSettingsEnabled = goalSettingsEnabled,
                        goalTime = goalTime,
                        goalTimeAdjusted = goalTimeAdjusted,
                        onGoalTimeClicked = onGoalTimeClicked,
                        onEditGoalTime = onEditGoalTimeClicked,
                    )
                } else {
                    ProgramSummarySecondPage(
                        secondPageData = secondPageData
                    )
                }
            }
        }

        // the two page indicator dots
        Box(
            modifier = Modifier
                .weight(.2f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageIndicatorDot(currentPage == 0) {
                    currentPage = 0
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = currentPage,
                            animationSpec = tween(0)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp.scaleSize()))
                PageIndicatorDot(currentPage == 1) {
                    currentPage = 1
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = currentPage,
                            animationSpec = tween(0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicatorDot(isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val color = if (isSelected) MmColors.futuristicBlue else Color.White
    Box(
        modifier = Modifier
            .size(22.dp.scaleSize())
            .background(color = if (isSelected) color else Color.Transparent, shape = CircleShape)
            .clickableNoEffect(interactionSource) { onClick() }
            .drawBehind {
                drawCircle(
                    color = color,
                    radius = this.size.width / 2,
                    style = Stroke((-4).dp.toPx())
                )
            }
            .testTag("page_indicator_dot")
    )
}

@Composable
private fun ProgramSummaryFirstPage(
    firstPageData: List<ProgramSummaryViewModel.ProgramSummaryLine>,
    currentPaceValue: String,
    showEndOfProgram: Boolean,
    estimatedTimeChange: Int,
    estimatedPaceChange: Int,
    estimatedRaceTime: String,
    goalSettingsEnabled: Boolean,
    goalTime: String,
    goalTimeAdjusted: Boolean,
    onGoalTimeClicked: () -> Unit,
    onEditGoalTime: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(.5f)
                .fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Implement 4 items into the row
                firstPageData.forEach { (title, value, unit) ->
                    val itemUnit = if (unit != null) stringResource(unit) else "HH:MM:SS"
                    FirstPageColumnLayoutMode(
                        itemTitle = stringResource(id = title),
                        itemValue = value,
                        itemUnit = itemUnit
                    )

                }

            }
        }

        Box(
            Modifier
                .weight(.5f)
                .fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.performance_evaluation_tc),
                    color = Color.White,
                    fontSize = 25.sp.scaleSize(),
                    modifier = Modifier
                        .padding(bottom = 6.dp.scaleSize())
                        .testTag("performance_evaluation"),
                    style = oxaniumStyle.copy(textAlign = TextAlign.Center)
                )
                HorizontalDivider(
                    Modifier
                        .background(Color.White)
                        .size(600.dp.scaleSize(), 2.dp.scaleSize())
                )

                val paceString = if (showEndOfProgram) R.string.finishing_pace_uc
                else R.string.current_pace_uc
                Row(
                    modifier = Modifier
                        .padding(top = 25.dp.scaleSize())
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    // Current pace
                    Row(
                        modifier = Modifier
                            .weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = paceString),
                                modifier = Modifier
                                    .padding(bottom = 2.dp.scaleSize())
                                    .testTag(stringResource(paceString).lowercase())
                                    .align(Alignment.CenterHorizontally),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 25.sp.scaleSize(),
                                    color = Color.White,
                                )
                            )

                            Row(
                                modifier = Modifier.width(400.dp.scaleSize()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = currentPaceValue,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        color = MmColors.futuristicBlue,
                                        fontSize = 40.sp.scaleSize(),
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    modifier = Modifier.testTag(stringResource(paceString).lowercase() + " value")
                                )
                                EstimatedTimeChangeDisplay(
                                    estimatedTimeChange = estimatedPaceChange, modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp.scaleSize())
                                )
                            }
                        }
                    }

                    // Goal race time
                    Row(horizontalArrangement = Arrangement.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.clickableNoEffect { onGoalTimeClicked() }
                        ) {
                            Text(
                                // TODO: this will need to be updated to be program-independent once other programs are added back in.
                                text = stringResource(R.string.goal_5k_time_uc),
                                style = oxaniumStyle.copy(
                                    fontSize = 25.sp.scaleSize(),
                                    color = Color.White,
                                ),
                                modifier = Modifier
                                    .padding(bottom = 2.dp.scaleSize())
                                    .testTag("goal_5k_time"),
                            )
                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .width(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f)) {
                                    if (goalSettingsEnabled && goalTimeAdjusted) {
                                        Icon(
                                            painterResource(id = R.drawable.ic_warning_triangle),
                                            tint = Color(0XFFF0B323),
                                            contentDescription = "goal time adjusted warning icon",
                                            modifier = Modifier
                                                .size(20.dp.scaleSize())
                                                .testTag("goal_5k_adjustment_warning")
                                        )
                                        Spacer(modifier = Modifier.width(8.dp.scaleSize()))
                                    }
                                }

                                Text(
                                    text = goalTime,
                                    style = oxaniumStyle.copy(
                                        color = MmColors.futuristicBlue,
                                        fontSize = 40.sp.scaleSize(),
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    modifier = Modifier.testTag("goal_5k_time_value")
                                )
                                if (goalSettingsEnabled) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable(onClick = onEditGoalTime),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Spacer(modifier = Modifier.width(12.dp.scaleSize()))

                                        Image(
                                            painter = painterResource(id = R.drawable.ic_edit_pencil),
                                            modifier = Modifier.size(20.dp.scaleSize()),
                                            contentDescription = "Edit button",
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Estimated race time
                    Row(
                        modifier = Modifier
                            .weight(1f),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // TODO: this will need to be updated to be program-independent once other programs are added back in
                        //If we only want to show the estimated time on The Program Summary screen, we will need to change the implementations of this column.
                        val raceTime =
                            if (showEndOfProgram) R.string.final_five_k_time_tc else R.string.estimated_five_k_time_tc
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = raceTime).uppercase(),
                                style = oxaniumStyle.copy(
                                    fontSize = 25.sp.scaleSize(),
                                    color = Color.White,
                                ),
                                modifier = Modifier
                                    .padding(bottom = 2.dp.scaleSize())
                                    .testTag(if (showEndOfProgram) "final_5k_time" else "estimated_5k_time"),
                            )
                            Row(
                                modifier = Modifier.width(400.dp.scaleSize()),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = estimatedRaceTime,
                                    color = MmColors.futuristicBlue,
                                    fontSize = 40.sp.scaleSize(),
                                    fontWeight = FontWeight.Bold,
                                    style = oxaniumStyle,
                                    modifier = Modifier.testTag(if (showEndOfProgram) "final_5k_time_value" else "estimated_5k_time_value")
                                )

                                EstimatedTimeChangeDisplay(
                                    estimatedTimeChange = estimatedTimeChange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp.scaleSize())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EstimatedTimeChangeDisplay(estimatedTimeChange: Int, modifier: Modifier) {
    val trianglePath = trianglePath(estimatedTimeChange)
    val colorChange = when {
        estimatedTimeChange < 0 -> MmColors.lightGreen
        estimatedTimeChange > 0 -> MmColors.missedRed
        else -> Color.Transparent
    }

    Row(modifier = modifier) {
        Canvas(
            modifier = Modifier.testTag("estimated_time_change_triangle")
        ) {
            drawPath(
                path = trianglePath,
                color = colorChange
            )

        }
        Spacer(modifier = Modifier.width(22.dp.scaleSize()))
        Text(
            text = formatEstimatedTimeChange(estimatedTimeChange),
            color = colorChange,
            style = oxaniumStyle.copy(fontWeight = FontWeight.SemiBold),
            fontSize = 18.sp.scaleSize(),
            maxLines = 1,
            modifier = Modifier.testTag("estimated_time_change")
        )
    }
}

@Composable
private fun formatEstimatedTimeChange(estimatedTimeChange: Int): String {
    return if (estimatedTimeChange > 0) {
        "+${UnitFormatter.formatPace(estimatedTimeChange)}"
    } else if (estimatedTimeChange < 0) {
        "-${UnitFormatter.formatPace(abs(estimatedTimeChange))}"
    } else {
        ""
    }
}

@Composable
private fun trianglePath(estimatedTimeChange: Int): Path {
    return if (estimatedTimeChange > 0) {
        Path().let {
            it.moveTo(convertToPx(0f), convertToPx(15f))
            it.lineTo(convertToPx(10f), convertToPx(3f))
            it.lineTo(convertToPx(20f), convertToPx(15f))
            it.close()
            it
        }
    } else if (estimatedTimeChange < 0) {
        Path().let {
            it.moveTo(convertToPx(0f), convertToPx(3f))
            it.lineTo(convertToPx(10f), convertToPx(15f))
            it.lineTo(convertToPx(20f), convertToPx(3f))
            it.close()
            it
        }
    } else {
        Path()
    }
}

@Composable
private fun FirstPageColumnLayoutMode(
    itemTitle: String,
    itemValue: String,
    itemUnit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = itemTitle,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 25.sp.scaleSize(),
                color = Color.White,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.testTag(itemTitle.lowercase())
        )
        Text(
            text = itemValue,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 40.sp.scaleSize(),
                color = MmColors.futuristicBlue,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier
                .testTag("${itemTitle.lowercase()} value")
                .padding(top = 5.dp.scaleSize(), bottom = 5.dp.scaleSize())
        )
        Text(
            text = itemUnit,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 20.sp.scaleSize(),
                color = Color.White,
            ),
            modifier = Modifier.testTag("${itemTitle.lowercase()} unit")
        )
    }
}

@Composable
private fun ProgramSummarySecondPage(
    secondPageData: List<ProgramSummaryViewModel.ProgramSummaryLine>
) {
    val leftBoxData = secondPageData.subList(0, 7)
    val rightBoxData = secondPageData.subList(7, secondPageData.size)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 45.dp.scaleSize()),
        verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.Center
    ) {
        //Implement 14 items into the second page
        SecondPageBoxLayoutMode(itemsData = leftBoxData)
        Spacer(modifier = Modifier.width(24.dp.scaleSize()))
        SecondPageBoxLayoutMode(itemsData = rightBoxData)
    }
}

@Composable
fun SecondPageBoxLayoutMode(itemsData: List<ProgramSummaryViewModel.ProgramSummaryLine>) {
    Box(
        modifier = Modifier.size(450.dp.scaleSize(), 330.dp.scaleSize()),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            VerticalDivider(
                modifier = Modifier
                    .padding(bottom = 16.dp.scaleSize())
                    .fillMaxHeight()
                    .width(5.dp.scaleSize())
                    .testTag("the_vertical_line"),
                color = MmColors.futuristicBlue
            )
            Column(modifier = Modifier.fillMaxSize()) {
                itemsData.forEach { (title, value, unit) ->
                    val state = value + if (unit != null) " " + stringResource(unit) else ""
                    SecondPageRowLayoutMode(
                        textStart = formatTheSecondPageTitle(title),
                        textEnd = state
                    )
                }
            }
        }
    }
}

@Composable
private fun formatTheSecondPageTitle(title: Int): String {
    //Format the upper case string to a title case string
    return if (title == R.string.finishing_pace_uc) stringResource(id = title)
        .lowercase()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    else stringResource(id = title)
}

@Composable
private fun SecondPageRowLayoutMode(
    textStart: String,
    textEnd: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp.scaleSize())
            .padding(bottom = 6.dp.scaleSize())
            .background(Color.Black.copy(alpha = 0.5f))
            .testTag(textStart.lowercase()),
        verticalAlignment = Alignment.CenterVertically

    ) {
        Text(
            text = textStart,
            modifier = Modifier.padding(start = 10.dp.scaleSize()),
            fontSize = 16.sp.scaleSize(),
            color = Color.White,
            style = robotoStyle.copy(fontWeight = FontWeight.Medium, lineHeight = 24.sp.scaleSize())
        )
        Spacer(modifier = Modifier.weight(1.0f))
        Text(
            text = textEnd,
            modifier = Modifier.padding(end = 10.dp.scaleSize()),
            fontSize = 16.sp.scaleSize(),
            color = MmColors.futuristicBlue,
            style = robotoStyle.copy(fontWeight = FontWeight.Medium, lineHeight = 24.sp.scaleSize())
        )
    }
}

//region Previews
private val firstPageData = listOf(
    ProgramSummaryViewModel.ProgramSummaryLine(R.string.total_workout_time_uc, "11:11:11"),
    ProgramSummaryViewModel.ProgramSummaryLine(
        R.string.total_distance_uc,
        "8.88",
        R.string.miles_uc
    ),
    ProgramSummaryViewModel.ProgramSummaryLine(
        R.string.total_calories_uc,
        "250",
        R.string.kcal_uc
    ),
    ProgramSummaryViewModel.ProgramSummaryLine(
        R.string.completed_workouts_uc,
        "10",
        R.string.completed_workouts_format_uc
    )
)

private val secondPageData = listOf(
    ProgramSummaryViewModel.ProgramSummaryLine(R.string.workout_completed_tc, "10"),
    ProgramSummaryViewModel.ProgramSummaryLine(R.string.workouts_missed_tc, "1"),
    ProgramSummaryViewModel.ProgramSummaryLine(R.string.total_workout_time_tc, "11:11:11"),
    ProgramSummaryViewModel.ProgramSummaryLine(R.string.total_calories_tc, "250"),
    ProgramSummaryViewModel.ProgramSummaryLine(
        R.string.total_distance_tc,
        "8.88",
        R.string.miles_lc
    ),
    ProgramSummaryViewModel.ProgramSummaryLine(
        R.string.average_speed_tc,
        "6.6",
        R.string.mph_lc
    ),
    ProgramSummaryViewModel.ProgramSummaryLine(R.string.peak_speed_tc, "6.6", R.string.mph_lc),
    ProgramSummaryViewModel.ProgramSummaryLine(R.string.estimated_five_k_time_tc, "88.66"),
    ProgramSummaryViewModel.ProgramSummaryLine(R.string.current_five_k_pace_tc, "8.88"),
    ProgramSummaryViewModel.ProgramSummaryLine(R.string.starting_five_k_pace_tc, "6.66"),
    ProgramSummaryViewModel.ProgramSummaryLine(R.string.best_five_k_pace_tc, "8.88"),
    ProgramSummaryViewModel.ProgramSummaryLine(
        R.string.overall_average_hr_tc,
        "66",
        R.string.bpm_lc
    ),
    ProgramSummaryViewModel.ProgramSummaryLine(
        R.string.highest_average_hr_tc,
        "88",
        R.string.bpm_lc
    ),
    ProgramSummaryViewModel.ProgramSummaryLine(
        R.string.total_elevation_tc,
        "88",
        R.string.feet_lc
    )
)

@AllDevicePreviews
@Composable
fun ProgramSummaryScreenPreview() {
    ProgramSummaryScreenFrame(
        firstPageData = firstPageData,
        secondPageData = secondPageData,
        showEndOfProgram = false,
        estimatedTimeChange = 30,
        estimatedPaceChange = 5,
        currentPaceValue = "10:00",
        estimatedRaceTime = "65:00",
        goalTime = "50:00",
        goalSettingsEnabled = true,
        goalTimeAdjusted = true,
        onBackPressed = {},
        onResetProgramBtnClicked = {},
        onGoalTimeClicked = {},
        onEditGoalTimeClicked = {}
    )
}

/**
 * Vietnamese Preview
 */
@Preview(name = "15inch", locale = "vi", device = "spec:width=1366dp,height=768dp,dpi=160,orientation=landscape")
@Composable
fun ProgramSummaryVietnamesePreview() {
    ProgramSummaryScreenFrame(
        firstPageData = firstPageData,
        secondPageData = secondPageData,
        showEndOfProgram = false,
        estimatedTimeChange = 30,
        estimatedPaceChange = 5,
        currentPaceValue = "10:00",
        estimatedRaceTime = "65:00",
        goalTime = "50:00",
        goalSettingsEnabled = true,
        goalTimeAdjusted = true,
        onBackPressed = {},
        onResetProgramBtnClicked = {},
        onGoalTimeClicked = {},
        onEditGoalTimeClicked = {}
    )
}

/**
 * Arabic Preview
 */
@Preview(name = "15inch", locale = "ar", device = "spec:width=1366dp,height=768dp,dpi=160,orientation=landscape")
@Composable
fun ProgramSummaryArabicPreview() {
    ProgramSummaryScreenFrame(
        firstPageData = firstPageData,
        secondPageData = secondPageData,
        showEndOfProgram = false,
        estimatedTimeChange = 30,
        estimatedPaceChange = 5,
        currentPaceValue = "10:00",
        estimatedRaceTime = "65:00",
        goalTime = "50:00",
        goalSettingsEnabled = true,
        goalTimeAdjusted = true,
        onBackPressed = {},
        onResetProgramBtnClicked = {},
        onGoalTimeClicked = {},
        onEditGoalTimeClicked = {}
    )
}