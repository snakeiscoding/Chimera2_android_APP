package com.jht.spinbikeapps.targettraining.ui

sealed class TargetTrainingScreen(val route: String) {
    data object TargetSetupScreen: TargetTrainingScreen("targetSetup")
    data object RpmScreen: TargetTrainingScreen("rpm")
    data object WattsScreen: TargetTrainingScreen("watts")
    data object HeartRateScreen: TargetTrainingScreen("heartRate")
    data object CaloriesScreen: TargetTrainingScreen("calories")
    data object DistanceScreen: TargetTrainingScreen("distance")
    data object TargetWorkoutSelectionScreen: TargetTrainingScreen("target_workout_selection")
}

internal val ttTopBarBannerScreens = listOf<String>(
    TargetTrainingScreen.RpmScreen.route,
    TargetTrainingScreen.WattsScreen.route,
    TargetTrainingScreen.HeartRateScreen.route,
    TargetTrainingScreen.CaloriesScreen.route,
    TargetTrainingScreen.DistanceScreen.route,
    TargetTrainingScreen.TargetSetupScreen.route,

)