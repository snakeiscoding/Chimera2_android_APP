package com.jht.progression.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jht.enums.ProgramType
import com.jht.data.EventIndex
import com.jht.progression.MmViewModel
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.ui.setup.assessment.DisclaimerScreen
import com.jht.progression.ui.setup.assessment.assessmentNavGraph
import com.jht.progression.ui.setup.journey.journeyNavGraph
import com.jht.progression.userinfo.UserInfoManager

/**
 * Construct the navigation controller between screens
 */
@Composable
fun ProgramSetupScreenNav(
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
        startDestination = if (userInfoManager.isProgramStarted()) {
            ProgramSelectionScreens.Journey.route
        } else {
            ProgramSelectionScreens.ProgramSelect.route
        }
    ) {
        composable(route = ProgramSelectionScreens.ProgramSelect.route) {
            ProgramSelectScreen(
                programs = setupState.programs,
                navigateToSplash = navigateToSplash,
                onStartPressed = {
                    userInfoManager.loadAndUpdateUserInfo()
                    userInfoManager.userInfo.programType = it

                    if (userInfoManager.isProgramStarted()) {
                        navigateToProgramScreen(userInfoManager, navController)
                    } else {
                        navigateToDisclaimer(navController)
                    }
                }
            )
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
            navigateToAssessment = { navigateToAssessment(navController) },
            userInfoManager = userInfoManager,
            useMetric = setupState.useMetric
        )

        composable(route = ProgramSelectionScreens.Disclaimer.route) {
            DisclaimerScreen(
                onBackPressed = onBackPressed,
                onAccept = { navigateToProgramScreen(userInfoManager, navController) },
                onReject = navigateToSplash
            )
        }
    }
}

fun navigateToProgramScreen(userInfoManager: UserInfoManager, navController: NavHostController) {
    if (userInfoManager.isProgramStarted()) {
        navController.navigate(route = ProgramSelectionScreens.Journey.route)
    } else {
        navigateToAssessment(navController)
    }
}

fun navigateToAssessment(navController: NavHostController) {
    navController.navigate(route = ProgramSelectionScreens.Assessment.route)
}

fun navigateToDisclaimer(navController: NavHostController) {
    navController.navigate(route = ProgramSelectionScreens.Disclaimer.route)
}