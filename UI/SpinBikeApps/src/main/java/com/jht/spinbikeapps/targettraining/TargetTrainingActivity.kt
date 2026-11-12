package com.jht.spinbikeapps.targettraining

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.jht.spinbikeapps.targettraining.common.UserFtpMode
import com.jht.spinbikeapps.targettraining.ui.TargetTrainingMainScreen
import com.jht.spinbikeapps.targettraining.ui.TargetTrainingScreen
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingTypeData
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import com.jht.widgets.RestringThemedActivity
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "TargetTrainingActivity"

@AndroidEntryPoint
class TargetTrainingActivity : RestringThemedActivity() {
    override val themedViewModel: TargetTrainingViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val targetTimeLabel by remember {
                derivedStateOf { themedViewModel.targetTimeLabel }
            }

            val relativeDistance by remember {
                derivedStateOf { themedViewModel.relativeDistance }
            }
            val selectedMode = TargetTrainingTypeData.selectedMode
            val selectedTab = when (selectedMode) {
                WorkoutMode.Rpm -> TargetTrainingScreen.RpmScreen.route
                WorkoutMode.Watts -> TargetTrainingScreen.WattsScreen.route
                WorkoutMode.HeartRate -> TargetTrainingScreen.HeartRateScreen.route
                WorkoutMode.Calories -> TargetTrainingScreen.CaloriesScreen.route
                WorkoutMode.Distance -> TargetTrainingScreen.DistanceScreen.route
                WorkoutMode.None -> TargetTrainingScreen.RpmScreen.route
            }
            var ftpMode by rememberSaveable {
                mutableStateOf(UserFtpMode.UserFtpKnown)
            }
            val currentCalories = themedViewModel.targetCalories.floatValue.toInt()
            val targetCalories = TargetTrainingTypeData.caloriesTarget?.toInt()
            LaunchedEffect(currentCalories, targetCalories, selectedMode) {
                if (selectedMode == WorkoutMode.Calories) {
                    themedViewModel.onCaloriesTargetReached(currentCalories)
                }
            }

            LaunchedEffect(relativeDistance, selectedMode) {
                if (selectedMode == WorkoutMode.Distance) {
                    themedViewModel.onDistanceTargetReached(relativeDistance)
                }
            }



            TargetTrainingMainScreen(
                selectedTab = selectedTab,
                rpm = themedViewModel.rpm.intValue.toFloat(),
                watts = themedViewModel.watts.floatValue.toInt(),
                averageWatts = themedViewModel.averageWatts.floatValue.toInt(),
                heartRate = themedViewModel.heartRate.intValue.toFloat(),
                maxHeartRate = themedViewModel.maxHeartRate.floatValue,
                userFtp = themedViewModel.userFtp.intValue,
                calories = themedViewModel.targetCalories.floatValue,
                heartRatePercentage = themedViewModel.targetHeartRatePercent.floatValue,
                currentDistance = relativeDistance,
                totalGoalDistance = themedViewModel.totalGoalDistance.floatValue,
                targetDistance = themedViewModel.targetDistance.floatValue.takeIf { it > 0f },
                targetTimeLabel = targetTimeLabel,
                isMetric = themedViewModel.isMetric.value,
                bottomTabs = themedViewModel.bottomTabs,
                totalDistance = themedViewModel.totalDistanceMax,
                rpmData = { themedViewModel.rpmData() },
                wattsData = { themedViewModel.wattsData() },
                heartRateData = { themedViewModel.heartRateData() },
                caloriesData = { themedViewModel.caloriesData() },
                distanceData = { themedViewModel.distanceData() },
                onUpdateSelectedMode = themedViewModel::updateSelectedMode,
                onClickSavedCalories = themedViewModel::onClickSavedCalories,
                fitnessLevel = themedViewModel.fitnessLevel.value,
                gender = themedViewModel.gender.value,
                onValueFitnessLevelChange = {themedViewModel.setFitnessLevel(it)},
                onValueGenderChange = {themedViewModel.setGenderData(it)},
                ftpMode = ftpMode,
                onModeChanged = {
                    ftpMode = it
                },
                onClickEndWorkout = themedViewModel::endWorkout,
                onClickContinue = themedViewModel::onClickContinue,
                isPopUpVisible = themedViewModel.isConfirmationPopUpVisible.value,
                totalDistanceCovered = themedViewModel.totalDistance.floatValue
            )
        }
    }
}
