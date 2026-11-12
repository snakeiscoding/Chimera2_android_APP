package com.jht.workoutselection.chameleon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jht.data.WorkoutItem
import com.jht.workoutselection.WorkoutSelectionViewModel

@Composable
fun WorkoutSelectionRoute(
    workoutSelectionViewModel: WorkoutSelectionViewModel,
) {
    val theme = remember { workoutSelectionViewModel.theme }
    //don't remember handlers
    val rfidHandler = workoutSelectionViewModel.rfidHandler
    val selectedTabKey = remember { workoutSelectionViewModel.selectedTabKey }
    val ptpHandler = workoutSelectionViewModel.engage360Handler

    WorkoutSelectionScreenChameleon(
        isRetail = workoutSelectionViewModel.uiBaseModel.isRetail,
        theme = theme.value,
        uiState = workoutSelectionViewModel.uiState,
        rfidHandler = rfidHandler,
        selectedTabKey = selectedTabKey.value,
        onSelectTabKey = { key -> workoutSelectionViewModel.onSelectTabKey(key) },
        onUserButton = workoutSelectionViewModel::onUserButton,
        onQuickStart = workoutSelectionViewModel::launchQuickStartWorkout,
        workoutInputs = workoutSelectionViewModel.currentWorkoutInputs,
        selectedWorkoutItem = workoutSelectionViewModel.selectedWorkoutItem.value,
        onSelectWorkoutItem = { workoutItem -> workoutItem?.let { workoutSelectionViewModel.selectWorkoutItem(it) } },
        onStartSelectedWorkout = { workoutSelectionViewModel.startSelectedItemWorkout() },
        workoutInputLevelInfoList = workoutSelectionViewModel.workoutInputLevelInfoList.value,
        workoutCategories = workoutSelectionViewModel.workoutCategories,
        virtualActiveWorkoutItems = workoutSelectionViewModel.virtualActiveItems,
        landmarkWorkoutItems = workoutSelectionViewModel.landmarksItems,
        featuredWorkoutItems = workoutSelectionViewModel.featuredItems,
        featuredPagerWorkoutItems = workoutSelectionViewModel.carouselItems.mapNotNull { it.tag as? WorkoutItem },
        customWorkoutHandler = workoutSelectionViewModel.customWorkoutHandler,
        onStartCustomWorkout = workoutSelectionViewModel::startSelectedCustomWorkout,
        isLoadingWorkout = workoutSelectionViewModel.isLoadingWorkout.value,
        showLanguageButton = workoutSelectionViewModel.isHomeScreen,
        onHome = workoutSelectionViewModel::onHomeButton,
        //onLoadFeaturedWorkouts = { workoutSelectionViewModel.updateWorkoutItems() }, // not sure this is needed? consider removing
        ptpHandler = ptpHandler,
        onPtpShow = { workoutSelectionViewModel.showPtp() },
        blockUi = workoutSelectionViewModel.isLaunchingWorkout,
        isPTPUser = workoutSelectionViewModel.userModel.isPTP.value && workoutSelectionViewModel.checkSecaTiersEnabled(),
        isSecaUser = workoutSelectionViewModel.userModel.isSecaUser && workoutSelectionViewModel.checkSecaTiersEnabled(),
        isGuest = workoutSelectionViewModel.userModel.isGuest,
        showDebugDialog = { workoutSelectionViewModel.showDebugDialog() },

        //Workout Setup Modals
        showWorkoutSetupModal = workoutSelectionViewModel.showDefaultWorkoutModal,
        showVirtualActiveModal = workoutSelectionViewModel.showVirtualActiveModal,
        showLandmarksModal = workoutSelectionViewModel.showLandmarksModal,
        activeFeaturePlugin = workoutSelectionViewModel.activeFeaturePlugin,
        showFeaturePluginModal = workoutSelectionViewModel.showFeaturePluginModal,
    )
}
