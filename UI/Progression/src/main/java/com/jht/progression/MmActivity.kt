package com.jht.progression

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.jht.progression.data.getProgramOfType
import com.jht.progression.ui.LoadingScreen
import com.jht.progression.ui.ProgramSetupScreenNav
import com.jht.progression.ui.summary.SummaryScreenNav
import com.jht.progression.ui.workout.WorkoutScreen
import dagger.hilt.android.AndroidEntryPoint
import com.jht.widgets.RestringThemedActivity

/**
 * The base activity for all Matrix Method (Progression Workouts)
 */
@AndroidEntryPoint
class MmActivity : RestringThemedActivity() {
    override val themedViewModel: MmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by themedViewModel.programSelectState.collectAsState()

            var displayedScreen by remember { mutableStateOf(MmViewModel.MmFlowToDisplay.None) }

            /*
             This is making it so we do not switch to the summary screen if we've loaded other
             screens. Switching to the summary screen saves end of workout data which would get
             done twice without this logic. The workout controller restarts the activity every
             time it starts a workout summary, so this will allow the workout summary to load
             only on first run.
             */
            LaunchedEffect(state.flowToDisplay) {
                when (state.flowToDisplay) {
                    MmViewModel.MmFlowToDisplay.Setup -> displayedScreen = state.flowToDisplay
                    MmViewModel.MmFlowToDisplay.Workout -> displayedScreen = state.flowToDisplay
                    MmViewModel.MmFlowToDisplay.Summary -> {
                        if (displayedScreen == MmViewModel.MmFlowToDisplay.None) {
                            displayedScreen = state.flowToDisplay
                        }
                    }
                    MmViewModel.MmFlowToDisplay.None -> {}
                }
            }

            when (displayedScreen) {
                MmViewModel.MmFlowToDisplay.Setup -> {
                    LaunchedEffect(Unit) {
                        themedViewModel.startIdleTimer()
                    }

                    Setup(state)
                }
                MmViewModel.MmFlowToDisplay.Workout -> {
                    LaunchedEffect(Unit) {
                        themedViewModel.stopIdleTimer()
                    }

                    Workout(state)
                }
                MmViewModel.MmFlowToDisplay.Summary -> {
                    LaunchedEffect(Unit) {
                        themedViewModel.startIdleTimer()
                    }

                    Summary(state)
                }
                MmViewModel.MmFlowToDisplay.None -> {}
            }
        }

        val skipToNextDayReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                themedViewModel.skipToNextEvent()
            }
        }

        val intentFilter = IntentFilter("com.jht.MmActivity.mm_skip_event")
        registerReceiver(skipToNextDayReceiver, intentFilter, RECEIVER_EXPORTED)
    }

    // Need to call setIntent so UiService always receives newest intent used to launch Matrix Method
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    @Composable
    private fun Setup(state: MmViewModel.ProgramSetupState) {
        if (!state.isWorkoutLaunched) {
            if (state.loading) {
                LoadingScreen()
            } else {
                val navController = rememberNavController()
                ProgramSetupScreenNav(
                    navController = navController,
                    setupState = state,
                    launchAssessment = { workoutInfo -> themedViewModel.launchAssessment(workoutInfo) },
                    launchWorkout = { workoutInfo, eventIndex ->
                        val program = getProgramOfType(eventIndex.programType)
                        val showTopBar =
                            program.workoutInterface.shouldDisplayNextInterval(eventIndex.eventIndex)
                        themedViewModel.launchWorkout(
                            workoutInfo,
                            eventIndex,
                            showTopBar
                        )
                    },
                    launchVaWorkout = { themedViewModel.launchVaWorkout() },
                    onBackPressed = { backPressed(navController) },
                    initializeProgram = { programType, dayOffset ->
                        themedViewModel.initializeProgram(programType, dayOffset)
                    },
                    navigateToSplash = { themedViewModel.navigateToSplash() },
                    userInfoManager = themedViewModel.userInfoManager
                )
            }
        }
    }

    @Composable
    private fun Workout(state: MmViewModel.ProgramSetupState) {
        WorkoutScreen(
            baselineKph = themedViewModel.userInfoManager.userInfo.baselineKph,
            isWorkoutLaunched = state.isWorkoutLaunched
        )
    }

    @Composable
    private fun Summary(state: MmViewModel.ProgramSetupState) {
        val navController = rememberNavController()
        if (!state.isWorkoutLaunched) {
            SummaryScreenNav(
                navController = navController,
                setupState = state,
                launchAssessment = { workoutInfo -> themedViewModel.launchAssessment(workoutInfo) },
                launchWorkout = { workoutInfo, eventIndex ->
                    val program = getProgramOfType(eventIndex.programType)
                    val showTopBar =
                        program.workoutInterface.shouldDisplayNextInterval(eventIndex.eventIndex)
                    themedViewModel.launchWorkout(
                        workoutInfo,
                        eventIndex,
                        showTopBar
                    )
                },
                launchVaWorkout = { themedViewModel.launchVaWorkout() },
                onBackPressed = { backPressed(navController = navController) },
                initializeProgram = { programType, dayOffset ->
                    themedViewModel.initializeProgram(programType, dayOffset)
                },
                navigateToSplash = { themedViewModel.navigateToSplash() },
                userInfoManager = themedViewModel.userInfoManager
            )
        }
    }

    private fun backPressed(navController: NavController) {
        if (!navController.popBackStack()) {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}
