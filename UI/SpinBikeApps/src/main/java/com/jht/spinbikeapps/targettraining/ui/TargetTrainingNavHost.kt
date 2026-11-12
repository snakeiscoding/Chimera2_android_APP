package com.jht.spinbikeapps.targettraining.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jht.data.WorkoutInputUi
import com.jht.spinbikeapps.targettraining.TargetTrainingViewModel
import com.jht.spinbikeapps.targettraining.common.TargetTrainingTopBar
import com.jht.spinbikeapps.targettraining.common.UserFtpMode
import com.jht.spinbikeapps.targettraining.ui.setup.GenericTargetSetupScreen
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import com.jht.ui.computeSizeHd
import com.jht.widgets.RFIDIndicator

@Composable
fun TargetTrainingNavHost(
    navController: NavHostController,
    modifier: Modifier,
    title: String,
    imageUri: Uri,
    description: String,
    workoutInputList: List<WorkoutInputUi>,
    startSelectedWorkout: () -> Unit,
    isRfidIconVisible: Boolean = false,
    content: @Composable (
        startSelectedWorkout: () -> Unit,
    ) -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val targetTrainingViewModel: TargetTrainingViewModel = hiltViewModel()
    var ftpMode by rememberSaveable {
        mutableStateOf(UserFtpMode.UserFtpKnown)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (currentRoute in ttTopBarBannerScreens) {
                TargetTrainingTopBar(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    navController = navController,
                    onHome = targetTrainingViewModel::onHome
                )
            }
            NavHost(
                navController = navController,
                startDestination = TargetTrainingScreen.TargetWorkoutSelectionScreen.route
            ) {
                composable(route = TargetTrainingScreen.TargetWorkoutSelectionScreen.route) {
                    TargetTrainingWorkoutSelectionModalContent(
                        modifier = modifier,
                        title = title,
                        description = description,
                        workoutInputList = workoutInputList,
                        startSelectedWorkout = startSelectedWorkout,
                        imageUri = imageUri,
                        content = content
                    )
                }
                composable(TargetTrainingScreen.TargetSetupScreen.route) {
                    TargetTrainingMainSetupScreen(
                        navController,
                        targetTrainingSetupData = targetTrainingViewModel.targetTrainingSetupData
                    )
                }
                composable(TargetTrainingScreen.RpmScreen.route) {
                    GenericTargetSetupScreen(
                        targetTrainingViewModel.rpmData(),
                        navController,
                        onClick = { targetTrainingViewModel.updateSelectedMode(WorkoutMode.Rpm); startSelectedWorkout() })
                }

                composable(TargetTrainingScreen.WattsScreen.route) {
                    GenericTargetSetupScreen(
                        targetTrainingViewModel.wattsData(),
                        navController,
                        onClick = {
                            targetTrainingViewModel.updateSelectedMode(
                                WorkoutMode.Watts,
                                ftpMode = ftpMode
                            ); startSelectedWorkout()
                        },
                        fitnessLevel = targetTrainingViewModel.fitnessLevel.value,
                        gender = targetTrainingViewModel.gender.value,
                        onValueFitnessLevelChange = { targetTrainingViewModel.setFitnessLevel(it) },
                        onValueGenderChange = { targetTrainingViewModel.setGenderData(it) },
                        ftpMode = ftpMode,
                        onModeChanged = {
                            ftpMode = it
                        }
                    )
                }

                composable(TargetTrainingScreen.HeartRateScreen.route) {
                    GenericTargetSetupScreen(
                        targetTrainingViewModel.heartRateData(),
                        navController,
                        onClick = { targetTrainingViewModel.updateSelectedMode(WorkoutMode.HeartRate); startSelectedWorkout() },
                        ftpMode = ftpMode,
                        onModeChanged = {
                            ftpMode = it
                        })
                }

                composable(TargetTrainingScreen.CaloriesScreen.route) {
                    GenericTargetSetupScreen(
                        targetTrainingViewModel.caloriesData(),
                        navController,
                        onClick = { targetTrainingViewModel.updateSelectedMode(WorkoutMode.Calories); startSelectedWorkout() })
                }

                composable(TargetTrainingScreen.DistanceScreen.route) {
                    GenericTargetSetupScreen(
                        targetTrainingViewModel.distanceData(),
                        navController,
                        onClick = { targetTrainingViewModel.updateSelectedMode(WorkoutMode.Distance); startSelectedWorkout() })
                }
            }
        }

            RFIDIndicator(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 44.dp.computeSizeHd())
                    .zIndex(1f),
                isVisible = currentRoute in ttTopBarBannerScreens && isRfidIconVisible
            )
    }
}
