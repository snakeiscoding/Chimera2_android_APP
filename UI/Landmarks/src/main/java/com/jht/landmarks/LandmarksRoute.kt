package com.jht.landmarks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun LandmarksRoute(landmarkViewModel: LandmarksViewModel) {

    val currLocation = landmarkViewModel.landmarksData.value

    currLocation?.let {
        LandmarksScreen(
            landmarkItem = it,
            progress = landmarkViewModel.progress.floatValue,
            currentFloor = landmarkViewModel.floorsCompleted.intValue,
            maxFloors = landmarkViewModel.totalFloors.intValue,
            isFullScreen = landmarkViewModel.isFullScreen.value,
            currentResistanceLevel = landmarkViewModel.currentResistance.intValue,
            maxResistance = landmarkViewModel.maxResistance.intValue,
            isPaused = landmarkViewModel.isWorkoutPaused.value,
            onSizeClick = landmarkViewModel::onSizeClick,
            onLevelChange = { landmarkViewModel.setResistance(it) },
            onLevelUp = { landmarkViewModel.increaseResistance() },
            onLevelDown = { landmarkViewModel.decreaseResistance() },
            onPauseToggle = { landmarkViewModel.togglePause() },
        )
    }
}