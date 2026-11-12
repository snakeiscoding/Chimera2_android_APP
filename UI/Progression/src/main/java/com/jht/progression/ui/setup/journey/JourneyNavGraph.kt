package com.jht.progression.ui.setup.journey

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.jht.data.EventIndex
import com.jht.progression.data.IEvent
import com.jht.progression.data.NonWorkoutEvent
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.data.getProgramOfType
import com.jht.progression.data.program.skipOverEvent
import com.jht.progression.ui.ProgramSelectionScreens
import com.jht.progression.ui.setup.SetupScreens
import com.jht.progression.ui.widgets.BlackBackgroundScreen
import com.jht.progression.userinfo.UserInfoManager

private const val TAG = "JourneyNavGraph"
private const val PREVIEW_EVENT_INDEX_ARG: String = "eventIndex"
private const val JOURNEY_START_ROUTE = "JourneyStart"

fun NavGraphBuilder.journeyNavGraph(
    navController: NavHostController,
    onBackPressed: () -> Unit,
    launchWorkout: (workout: WorkoutInfo, eventIndex: EventIndex) -> Unit,
    launchVaWorkout: () -> Unit,
    navigateToSplash: () -> Unit,
    navigateToAssessment: () -> Unit,
    userInfoManager: UserInfoManager,
    useMetric: Boolean,
) {
    val program = getProgramOfType(userInfoManager.userInfo.programType)

    navigation(
        startDestination = JOURNEY_START_ROUTE,
        route = ProgramSelectionScreens.Journey.route
    ) {
        composable(JOURNEY_START_ROUTE) {
            val programCompleted = isLastEventCompletedOrMissed(
                userInfoManager.userInfo.programEvents,
                System.currentTimeMillis()
            )
            LaunchedEffect(Unit) {
                if (programCompleted) {
                    navigateToProgramSummary(navController)
                } else {
                    navigateToJourney(navController)
                }
            }

            BlackBackgroundScreen()
        }

        composable(SetupScreens.WorkoutJourneyScreen.route) {
            val journeyViewModel: JourneyViewModel = viewModel(
                factory = JourneyViewModel.provideFactory(
                    userInfoManager = userInfoManager
                )
            )

            JourneyScreen(
                onEventSelected = { navigateToPreview(navController, it) },
                viewModel = journeyViewModel,
                navigateToSplash = navigateToSplash,
                onRedoAssessment = navigateToAssessment,
                onGoalSettings = { navigateToGoalSettings(navController) },
                navigateToProgramSummary = { navigateToProgramSummary(navController) },
                onEraseUserData = {
                    userInfoManager.resetProgram()
                    // don't update time since we want to overwrite with blank data
                    userInfoManager.writeUserInfoToFile(false)
                    navigateToSplash()
                }
            )
        }

        composable(
            route = SetupScreens.WorkoutPreviewScreen.route + "/{$PREVIEW_EVENT_INDEX_ARG}",
            arguments = listOf(navArgument(PREVIEW_EVENT_INDEX_ARG) { type = NavType.IntType })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt(PREVIEW_EVENT_INDEX_ARG) ?: 0
            val previewViewModel: PreviewViewModel = viewModel(
                factory = PreviewViewModel.provideFactory(
                    eventSelected = index,
                    userInfoManager = userInfoManager,
                    useMetric = useMetric
                )
            )

            if (previewViewModel.isRaceEvent()) {
                RacePreviewScreen(
                    viewModel = previewViewModel,
                    // Don't use normal back button behavior since this might need to reload the program if user skipped on that screen
                    onBackPressed = { navigateToJourney(navController) },
                    onCompleteProgram = {
                        Log.d(TAG, "Skipping the race event")
                        previewViewModel.skipRaceEvent()
                        navigateToProgramSummary(navController)
                    },
                    launchWorkout = launchWorkout
                )
            } else {
                WorkoutPreviewScreen(
                    viewModel = previewViewModel,
                    // Don't use normal back button behavior since this might need to reload the program if user skipped on that screen
                    onBackPressed = { navigateToJourney(navController) },
                    launchWorkout = launchWorkout
                )
            }
        }

        composable(
            route = SetupScreens.NonWorkoutPreviewScreen.route + "/{$PREVIEW_EVENT_INDEX_ARG}",
            arguments = listOf(navArgument(PREVIEW_EVENT_INDEX_ARG) { type = NavType.IntType })
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt(PREVIEW_EVENT_INDEX_ARG) ?: 0
            val programEvents = userInfoManager.userInfo.programEvents
            val event = programEvents.events[index]

            NonWorkoutScreen(
                event, onBackPressed = onBackPressed, onSkipEvent = {
                    val updatedEvents = skipOverEvent(event, programEvents)
                    userInfoManager.setProgramEvents(updatedEvents)
                    userInfoManager.writeUserInfoToFile()
                    navController.navigate(route = SetupScreens.WorkoutJourneyScreen.route)
                }, onLaunchVaWorkout = launchVaWorkout
            )
        }

        composable(
            route = SetupScreens.ProgramSummaryScreen.route
        ) {
            val programSummaryViewModel: ProgramSummaryViewModel = viewModel(
                factory = ProgramSummaryViewModel.provideFactory(
                    userInfoManager = userInfoManager,
                    useMetric = useMetric,
                    program = program
                )
            )

            ProgramSummaryScreen(
                viewModel = programSummaryViewModel,
                goalSettingsEnabled = program.workoutInterface.isGoalTimeChangePermitted(userInfoManager),
                onBackPressed = { isEndOfProgram ->
                    if (!isEndOfProgram) onBackPressed()
                    else navigateToSplash()
                },
                onResetProgram = {
                    userInfoManager.resetProgram()
                    userInfoManager.writeUserInfoToFile()
                    navigateToSplash()
                },
                onEditGoalTimePressed = { navigateToGoalSettings(navController) }
            )
        }

        composable(
            route = SetupScreens.GoalSettingsScreen.route
        ) {
            val goalSettingsViewModel: GoalSettingsViewModel = viewModel(
                factory = GoalSettingsViewModel.provideFactory(
                    userInfoManager = userInfoManager,
                    program = program
                )
            )

            GoalSettingsScreen(
                viewModel = goalSettingsViewModel,
                onBackPressed = onBackPressed,
                onSetGoalTime = {
                    userInfoManager.setGoalBaseline(it)
                    userInfoManager.setBackupGoalBaseline(it)
                    userInfoManager.setAutoAdjustGoalBaseline(true)
                    userInfoManager.writeUserInfoToFile()

                    val previousRoute = navController.previousBackStackEntry?.destination?.route
                    if (previousRoute == SetupScreens.ProgramSummaryScreen.route) {
                        navigateToProgramSummary(navController)
                    } else {
                        navController.navigate(SetupScreens.WorkoutJourneyScreen.route) {
                            popUpTo(SetupScreens.WorkoutJourneyScreen.route) {
                                inclusive = true
                            }
                        }
                    }
                }
            )
        }
    }
}

fun navigateToPreview(navController: NavHostController, event: IEvent) {
    when (event) {
        is WorkoutEvent -> navController.navigate(SetupScreens.WorkoutPreviewScreen.route + "/${event.index}")
        is NonWorkoutEvent -> navController.navigate(SetupScreens.NonWorkoutPreviewScreen.route + "/${event.index}")
    }
}

fun navigateToJourney(navController: NavHostController) {
    navController.navigate(SetupScreens.WorkoutJourneyScreen.route) {
        popUpTo(SetupScreens.WorkoutJourneyScreen.route) {
            inclusive = true
        }
    }
}

fun navigateToProgramSummary(navController: NavHostController) {
    navController.navigate(SetupScreens.ProgramSummaryScreen.route) {
        popUpTo(SetupScreens.ProgramSummaryScreen.route) {
            inclusive = true
        }
    }
}

fun navigateToGoalSettings(navController: NavHostController) {
    navController.navigate(SetupScreens.GoalSettingsScreen.route) {
        popUpTo(SetupScreens.GoalSettingsScreen.route) {
            inclusive = true
        }
    }
}
