package com.jht.spinbikeapps.targettraining.model

import com.jht.spinbikeapps.targettraining.utils.WorkoutMode

internal data class TabBarItem(
    val title: Int,
    val selectedIcon: Int,
    val unselectedIcon: Int,
    val route: String,
    val workoutMode: WorkoutMode,
)

internal enum class BottomTab {
    RPM,
    WATTS,
    HEART_RATE,
    CALORIES,
    DISTANCE
}
