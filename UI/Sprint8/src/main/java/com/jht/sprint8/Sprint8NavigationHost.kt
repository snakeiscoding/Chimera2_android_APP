package com.jht.sprint8

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.navigation
import com.jht.data.WorkoutInput
import com.jht.data.WorkoutInputUi
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.sprint8.assessment.ui.AssessmentInformationScreen
import com.jht.sprint8.assessment.ui.AssessmentLevelSelectionScreen
import com.jht.sprint8.sprint8classic.components.Sprint8TopBannerComp
import com.jht.sprint8.sprint8classic.ui.ExperienceSelectionScreen
import com.jht.sprint8.sprint8classic.ui.Sprint8InfoScreen
import com.jht.sprint8.sprint8classic.ui.Sprint8PreviewScreen
import com.jht.sprint8.sprint8classic.ui.Sprint8WorkoutSelectionModalContent
import com.jht.ui.computeSizeHd
import com.jht.widgets.RFIDIndicator

/**
 * Composable function to host navigation and display workout-related content in Sprint8.
 *
 * @param navController The [NavHostController] responsible for managing navigation between screens.
 * @param modifier Modifier applied to the navigation host for layout, styling, or positioning.
 * @param title The title displayed at the top of the navigation host.
 * @param imageUri URI of the image to display alongside the title and description.
 * @param description A descriptive text providing context for the workout or screen.
 * @param workoutInputList List of [WorkoutInput] objects representing the configurable workout parameters.
 * Used to track real-time changes to input values.
 * @param showingModal Mutable state indicating whether a modal is currently visible.
 * @param startSelectedWorkout Callback invoked when the user starts a selected workout.
 * @param onCustomSprint8Start Callback invoked when a custom workout is started, providing lists of speed/resistance and incline values.
 * Defaults to a no-op lambda.
 * @param content Composable content displayed inside the navigation host. Receives [startSelectedWorkout] as a parameter
 * to allow nested components to start the workout.
 */
@Composable
fun Sprint8NavigationHost(
    navController: NavHostController,
    modifier: Modifier,
    title: String,
    imageUri: Uri,
    description: String,
    workoutInputList: List<WorkoutInputUi>,
    onDismiss: () -> Unit = {},
    startSelectedWorkout: () -> Unit,
    onCustomSprint8Start: (speedOrResistanceData: List<Float>, inclineData: List<Float>) -> Unit = { _, _ -> },
    onShowCloseButton: (Boolean) -> Unit = {},
    isRfidIconVisible: Boolean = false,
    content: @Composable (
        startSelectedWorkout: () -> Unit
    ) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val previousBackStackEntry = navController.previousBackStackEntry
    val currentRoute = navBackStackEntry?.destination?.route
    val previousRoute = previousBackStackEntry?.destination?.route
    val sprint8ViewModel: Sprint8ViewModel = hiltViewModel()

    LaunchedEffect(currentRoute) {
        currentRoute ?: return@LaunchedEffect
        onShowCloseButton(currentRoute == Screen.Sprint8WorkoutSelectionScreen.route)
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (currentRoute in topBarBannerScreens) {
                Sprint8TopBannerComp(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    navController = navController
                )
            }
            NavHost(
                navController = navController, startDestination =
                    Screen.Sprint8NavGraph.route,
                enterTransition = {
                    fadeIn(tween(700)) + slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(700)
                    )
                },
                exitTransition = {
                    fadeOut(tween(700)) + slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(700)
                    )
                },
                popEnterTransition = {
                    fadeIn(tween(700)) + slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(700)
                    )
                },
                popExitTransition = {
                    fadeOut(tween(700)) + slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(700)
                    )
                }
            ) {
                navigation(
                    startDestination = Screen.Sprint8WorkoutSelectionScreen.route,
                    route = Screen.Sprint8NavGraph.route
                ) {
                    composable(route = Screen.Sprint8WorkoutSelectionScreen.route) {
                        Sprint8WorkoutSelectionModalContent(
                            navController = navController,
                            modifier = modifier,
                            title = title,
                            description = description,
                            workoutInputList = workoutInputList,
                            onDismiss = onDismiss,
                            startSelectedWorkout = startSelectedWorkout,
                            imageUri = imageUri,
                            content = content
                        )
                    }
                    dialog(
                        route = Screen.Sprint8InformationScreen.route,
                        dialogProperties = DialogProperties(
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        Sprint8InfoScreen(navController, previousRoute = previousRoute)
                    }
                    composable(route = Screen.Sprint8ExperienceScreen.route) {
                        ExperienceSelectionScreen(
                            navController = navController,
                            viewModel = sprint8ViewModel
                        )
                    }
                    composable(route = Screen.AssessmentLevelSelectionScreen.route) {
                        AssessmentLevelSelectionScreen(
                            navController = navController,
                            startSelectedWorkout = {
                                workoutInputList.find { it.identifier == ConsoleWorkoutVariables.LEVEL }?.currentValue?.value =
                                    sprint8ViewModel.getSelectedWorkoutLevel().toFloat()
                                startSelectedWorkout()
                            },
                            sprint8ViewModel = sprint8ViewModel
                        )
                    }
                    composable(route = Screen.AssessmentInformationScreen.route) {
                        AssessmentInformationScreen(navController = navController)
                    }
                    composable(route = Screen.Sprint8PreviewScreen.route) {
                        Sprint8PreviewScreen(
                            navController = navController,
                            startSelectedWorkout = {
                                workoutInputList.find { it.identifier == ConsoleWorkoutVariables.LEVEL }?.currentValue?.value =
                                    sprint8ViewModel.getSelectedWorkoutLevel().toFloat()
                                startSelectedWorkout()
                            },
                            onCustomWorkoutStart = onCustomSprint8Start,
                            viewModel = sprint8ViewModel
                        )
                    }
                }
            }
        }
            RFIDIndicator(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 44.dp.computeSizeHd())
                    .zIndex(1f),
                isVisible = currentRoute in topBarBannerScreens && isRfidIconVisible
            )
    }
}