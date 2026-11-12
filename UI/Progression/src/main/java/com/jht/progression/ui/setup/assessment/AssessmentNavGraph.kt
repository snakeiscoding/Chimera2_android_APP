package com.jht.progression.ui.setup.assessment

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.jht.enums.ProgramType
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.data.getProgramOfType
import com.jht.progression.ui.ProgramSelectionScreens
import com.jht.progression.ui.setup.SetupScreens
import com.jht.progression.userinfo.UserInfoManager

fun NavGraphBuilder.assessmentNavGraph(
    navController: NavHostController,
    onBackPressed: () -> Unit,
    launchAssessmentWorkout: (workout: WorkoutInfo) -> Unit,
    initializeProgram: (ProgramType, dayOffset: Int) -> Unit,
    userInfoManager: UserInfoManager
) {
    navigation(
        // If we have not started the program before show assessment instead of phases screen
        startDestination = SetupScreens.AssessmentScreen.route,
        route = ProgramSelectionScreens.Assessment.route
    ) {
        composable(route = SetupScreens.AssessmentScreen.route) {
            AssessmentSetupScreen(onBackPressed = onBackPressed,
                onSkipAssessment = {
                    if (!userInfoManager.isProgramStarted()) {
                        initializeProgram(userInfoManager.userInfo.programType, 0)
                    }

                    navController.navigate(route = SetupScreens.ManualTimeEntry.route)
                },
                onStartAssessment = {
                    if (!userInfoManager.isProgramStarted()) {
                        // Set the program to begin the next day since we're doing an assessment now
                        initializeProgram(userInfoManager.userInfo.programType, 1)
                    }
                    val program = getProgramOfType(userInfoManager.userInfo.programType)
                    userInfoManager.userInfo.programType
                    launchAssessmentWorkout(program.baselineInterface.getBaselineWorkout())
                })
        }

        composable(SetupScreens.ManualTimeEntry.route) {
            val program = getProgramOfType(userInfoManager.userInfo.programType)
            val manualTimeEntryViewModel: ManualTimeEntryViewModel = viewModel(
                factory = ManualTimeEntryViewModel.provideFactory(program)
            )
            ManualTimeEntryScreen(
                onPaceSelected = { paceValue ->
                    userInfoManager.userInfo.baselineKph = paceValue
                    if (userInfoManager.userInfo.assessmentBaselineKph == 0f) {
                        userInfoManager.userInfo.assessmentBaselineKph = paceValue
                    }
                    userInfoManager.userInfo.goalBaselineKph = null
                    userInfoManager.userInfo.goalIncrementKph = null
                    userInfoManager.userInfo.backupGoalBaseline = null
                    userInfoManager.userInfo.autoAdjustGoalBaseline = false
                    userInfoManager.writeUserInfoToFile()

                    navController.navigate(SetupScreens.WorkoutJourneyScreen.route) {
                        popUpTo(SetupScreens.AssessmentScreen.route) {
                            inclusive = true
                        }
                    }
                },
                onBackPressed = onBackPressed,
                viewModel = manualTimeEntryViewModel
            )
        }
    }
}


