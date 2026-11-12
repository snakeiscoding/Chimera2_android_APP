package com.jht.progression.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jht.enums.ProgramType
import com.jht.data.EventIndex
import com.jht.progression.MmViewModel
import com.jht.progression.R
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.data.getProgramOfType
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.navigateToAssessment
import com.jht.progression.ui.navigateToProgramScreen
import com.jht.progression.ui.setup.SetupScreens
import com.jht.progression.ui.setup.assessment.assessmentNavGraph
import com.jht.progression.ui.setup.journey.JourneyScreenAnimations
import com.jht.progression.ui.setup.journey.JourneyScreenAnimations.currentWorkoutIndex
import com.jht.progression.ui.setup.journey.ProgramSummaryScreen
import com.jht.progression.ui.setup.journey.ProgramSummaryViewModel
import com.jht.progression.ui.setup.journey.journeyNavGraph
import com.jht.progression.ui.setup.journey.navigateToGoalSettings
import com.jht.progression.ui.widgets.HomeButtonWidget
import com.jht.progression.ui.widgets.MmThemedButton
import com.jht.progression.ui.widgets.SeparatedText
import com.jht.progression.ui.widgets.StatBoxWidget
import com.jht.progression.ui.widgets.drawStatBorder
import com.jht.progression.userinfo.UserInfoManager
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.AutoSizeText
import com.jht.widgets.scaleSize

@Composable
fun SummaryScreenNav(
    summaryViewModel: SummaryViewModel = viewModel(),
    navController: NavHostController,
    setupState: MmViewModel.ProgramSetupState,
    launchAssessment: (WorkoutInfo) -> Unit,
    launchWorkout: (WorkoutInfo, EventIndex) -> Unit,
    launchVaWorkout: () -> Unit,
    onBackPressed: () -> Unit,
    navigateToSplash: () -> Unit,
    initializeProgram: (ProgramType, dayOffset: Int) -> Unit,
    userInfoManager: UserInfoManager
) {
    NavHost(
        navController = navController,
        startDestination = SetupScreens.WorkoutSummaryScreen.route
    ) {
        composable(SetupScreens.WorkoutSummaryScreen.route) {
            if (summaryViewModel.isProgramCompleted()) {
                var loadedProgram by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    // We need to load user info since it's set right before this
                    //TODO: This will probably need to be refactored when save data to web
                    userInfoManager.loadUserInfo()
                    loadedProgram = true
                }

                if (loadedProgram && userInfoManager.isProgramStarted()) {
                    val programSummaryViewModel: ProgramSummaryViewModel = viewModel(
                        factory = ProgramSummaryViewModel.provideFactory(
                            userInfoManager = userInfoManager,
                            useMetric = summaryViewModel.isMetric,
                            program = getProgramOfType(userInfoManager.userInfo.programType),
                            isEndOfProgram = true
                        )
                    )

                    val program = getProgramOfType(userInfoManager.userInfo.programType)
                    ProgramSummaryScreen(
                        viewModel = programSummaryViewModel,
                        goalSettingsEnabled = program.workoutInterface.isGoalTimeChangePermitted(userInfoManager),
                        onBackPressed = { summaryViewModel.navigateToSplashScreen() },
                        onResetProgram = {
                            userInfoManager.resetProgram()
                            userInfoManager.writeUserInfoToFile()
                            navigateToSplash()
                        },
                        onEditGoalTimePressed = { navigateToGoalSettings(navController) }
                    )
                }
            } else {
                SummaryScreenContent(
                    titleText = stringResource(summaryViewModel.titleTextId),
                    subText = if (summaryViewModel.subTextId != 0) stringResource(summaryViewModel.subTextId) else null,
                    stats = summaryViewModel.stats,
                    onHomeButtonPressed = { summaryViewModel.navigateToSplashScreen() },
                    navController = navController,
                    userInfoManager = userInfoManager,
                    assessmentIncomplete = summaryViewModel.assessmentIncomplete,
                    isWorkoutComplete = !summaryViewModel.isAssessment()
                )
            }
        }

        assessmentNavGraph(
            navController = navController,
            onBackPressed = onBackPressed,
            launchAssessmentWorkout = launchAssessment,
            userInfoManager = userInfoManager,
            initializeProgram = initializeProgram
        )

        journeyNavGraph(
            navController = navController,
            onBackPressed = onBackPressed,
            launchWorkout = launchWorkout,
            launchVaWorkout = launchVaWorkout,
            navigateToSplash = navigateToSplash,
            userInfoManager = userInfoManager,
            useMetric = setupState.useMetric,
            navigateToAssessment = { navigateToAssessment(navController) }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SummaryScreenContent(
    titleText: String,
    subText: String?,
    stats: Map<Int, SummaryViewModel.StatValue>,
    onHomeButtonPressed: () -> Unit,
    navController: NavHostController,
    userInfoManager: UserInfoManager?,
    assessmentIncomplete: Boolean,
    isWorkoutComplete: Boolean
) {
    val topBarBackground = if (assessmentIncomplete) Color(0xFF191919) else Color(0xFF02211C)
    val backgroundImage = if (assessmentIncomplete) R.drawable.mm_squares_bg_gray else R.drawable.mm_squares_bg_green
    val buttonText = if (assessmentIncomplete) stringResource(id = R.string.elift_return) else stringResource(id = R.string.view_journey_uc)
    val statBorderPrimary = if (assessmentIncomplete) Color(0xFFFFFFFF) else Color(0xFF06C25B)
    val statBorderSecondary = if (assessmentIncomplete) Color(0xFFFFFFFF).copy(alpha = 0.6f) else Color(0xFF06C25B).copy(alpha = 0.6f)

    Box(modifier = Modifier
        .fillMaxSize()
        .semantics {
            testTagsAsResourceId = true
        }) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBackground)
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

                if (isWorkoutComplete || assessmentIncomplete) {
                    HomeButtonWidget(onHomeButtonPressed)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = backgroundImage),
                    contentDescription = if(isWorkoutComplete) "Workout Summary Screen" else "Assessment Summary Screen",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop //Slightly crops the width to fit the 10" screen
                )
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AutoSizeText(
                        text = titleText,
                        maxLines = 1,
                        modifier = Modifier
                            .size(777.dp.scaleSize(), 44.dp.scaleSize())
                            .testTag(titleText.lowercase() + " summary title"),
                        style = oxaniumStyle.copy(
                            fontSize = 36.sp.scaleSize(),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 44.sp.scaleSize(),
                            textAlign = TextAlign.Center
                        ),
                    )
                    Spacer(modifier = Modifier.height(30.dp.scaleSize()))
                    StatBoxWidget(
                        width = 775.dp.scaleSize(),
                        height = if (isWorkoutComplete) 375.dp.scaleSize() else 260.dp.scaleSize(),
                        modifier = Modifier
                            .background(color = Color.Black)
                            .alpha(0.4f),
                        border = {
                            Canvas(
                                modifier = Modifier.testTag("summary_items_container"),
                                onDraw = {
                                    drawStatBorder(
                                        borderPrimary = statBorderPrimary,
                                        borderSecondary = statBorderSecondary
                                    )
                                })
                        })
                    {
                        Column(
                            verticalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 20.dp.scaleSize(), bottom = 20.dp.scaleSize())
                        ) {
                            stats.forEach { (name, stat) ->
                                val statValue = stat.value + if (stat.unit != null) " " + stringResource(stat.unit) else ""
                                SeparatedText(
                                    modifier = Modifier
                                        .padding(horizontal = 50.dp.scaleSize())
                                        .testTag(stringResource(name).lowercase()),
                                    textStart = stringResource(name).uppercase(),
                                    textEnd = statValue,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(25.dp.scaleSize()))
                    if (subText != null) {
                        Text(
                            text = formatSubtext(subText),
                            color = Color.White,
                            fontSize = 18.sp.scaleSize(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (assessmentIncomplete) FontWeight.SemiBold else FontWeight.Normal,
                            style = oxaniumStyle,
                            modifier = Modifier
                                .size(1100.dp.scaleSize(), 52.dp.scaleSize())
                                .testTag("summary_subtext"),
                            lineHeight = 26.sp.scaleSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(if (assessmentIncomplete) 30.dp.scaleSize() else 0.dp.scaleSize()))
                    MmThemedButton(
                        modifier = Modifier
                            .size(
                                if (!assessmentIncomplete) 420.dp.scaleSize() else 300.dp.scaleSize(),
                                120.dp.scaleSize()
                            )
                            .testTag("view_journey_button"),
                        text = buttonText,
                        color = MmColors.futuristicBlue,
                        onClick = {
                            if (userInfoManager != null) {
                                if (!assessmentIncomplete){
                                    userInfoManager.loadUserInfo()
                                }
                                navigateToProgramScreen(userInfoManager, navController)
                                JourneyScreenAnimations.lineAnimated = JourneyScreenAnimations.eventsDidAnimation.contains(currentWorkoutIndex)
                                JourneyScreenAnimations.workoutItemAnimated = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun formatSubtext(subText: String): String{
    return if (subText == stringResource(id = R.string.return_assessment_setup_text1_pg)){
        subText + "\n" + stringResource(id = R.string.return_assessment_setup_text2_pg)
    }else{
        subText
    }
}

@AllDevicePreviews
@Composable
private fun AssessmentSummaryPreview() {
    SummaryScreenContent(
        titleText = stringResource(id = R.string.assessment_incomplete_uc),
        subText = stringResource(id = R.string.return_assessment_setup_text1_pg),
        stats = mapOf(
            R.string.active_distance_uc to SummaryViewModel.StatValue("15"),
            R.string.estimated_mile_time_uc to SummaryViewModel.StatValue("0.1"),
            R.string.estimated_five_k_time_uc to SummaryViewModel.StatValue("3.15"),
        ),
        onHomeButtonPressed = {},
        navController = NavHostController(LocalContext.current),
        userInfoManager = null,
        assessmentIncomplete = true,
        isWorkoutComplete = false
    )
}

@AllDevicePreviews
@Composable
private  fun WorkoutSummaryPreview(){
    SummaryScreenContent(
        titleText = stringResource(id = R.string.workout_complete_exclamation_uc),
        subText = null,
        stats = mapOf(
            R.string.total_duration_tc to SummaryViewModel.StatValue("0:30:22"),
            R.string.total_distance_uc to SummaryViewModel.StatValue("2.5 MI"),
            R.string.average_speed_uc to SummaryViewModel.StatValue("5.6 MPH"),
            R.string.average_incline_uc to SummaryViewModel.StatValue("2.0"),
            R.string.total_calories_uc to SummaryViewModel.StatValue("146")

        ),
        onHomeButtonPressed = {},
        navController = NavHostController(LocalContext.current),
        userInfoManager = null,
        assessmentIncomplete = false,
        isWorkoutComplete = true
    )
}