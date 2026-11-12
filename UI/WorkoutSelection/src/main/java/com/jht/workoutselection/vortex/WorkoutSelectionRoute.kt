package com.jht.workoutselection.vortex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jht.workoutselection.WorkoutSelectionViewModel

@Composable
internal fun WorkoutSelectionRoute(
    workoutSelectionViewModel: WorkoutSelectionViewModel,
) {
    val theme = remember { workoutSelectionViewModel.theme }

    //don't remember handlers
    val rfidHandler = workoutSelectionViewModel.rfidHandler
    val ptpHandler = workoutSelectionViewModel.engage360Handler

    WorkoutSelectionScreenVortex(
        //Theme and UI state
        theme = theme.value,
        uiState = workoutSelectionViewModel.uiState,

        //Start Workouts
        onQuickStart = workoutSelectionViewModel::launchQuickStartWorkout,
        onStartSelectedWorkout = { workoutSelectionViewModel.startSelectedItemWorkout() },

        //Featured Lists
        featuredCarouselItems = workoutSelectionViewModel.carouselItems,
        featuredWorkoutItems = workoutSelectionViewModel.featuredItems,

        //Training Lists
        trainingCarouselItems = workoutSelectionViewModel.trainingCarouselItems,
        trainingWorkoutItems = workoutSelectionViewModel.workoutCategories.flatMap { it.items },

        //Selected Workout Setup
        onSelectWorkoutItem = { workoutItem -> workoutSelectionViewModel.selectWorkoutItem(workoutItem) },
        selectedWorkoutItem = workoutSelectionViewModel.selectedWorkoutItem.value,
        workoutStartingInputs = workoutSelectionViewModel.currentWorkoutInputs,
        workoutInputLevelInfoList = workoutSelectionViewModel.workoutInputLevelInfoList.value,

        //Workout Setup Modals
        showWorkoutSetupModal = workoutSelectionViewModel.showDefaultWorkoutModal,
        showVirtualActiveModal = workoutSelectionViewModel.showVirtualActiveModal,
        showLandmarksModal = workoutSelectionViewModel.showLandmarksModal,
        activeFeaturePlugin = workoutSelectionViewModel.activeFeaturePlugin,
        showFeaturePluginModal = workoutSelectionViewModel.showFeaturePluginModal,

        //Information for special workouts
        virtualActiveWorkoutItems = workoutSelectionViewModel.virtualActiveItems,
        landmarkWorkoutItems = workoutSelectionViewModel.landmarksItems,
        customWorkoutHandler = workoutSelectionViewModel.customWorkoutHandler,
        onStartCustomWorkout = workoutSelectionViewModel::startSelectedCustomWorkout,

        //Engage360 and seca
        engage360Handler = ptpHandler,

        //Single Workout Only
        singleWorkoutItemOnly = workoutSelectionViewModel.singleWorkoutItem,

        //TopBar
        isHomeScreen = workoutSelectionViewModel.isHomeScreen,
        isPTPUser = workoutSelectionViewModel.userModel.isPTP.value && workoutSelectionViewModel.checkSecaTiersEnabled(),
        isSecaUser = workoutSelectionViewModel.userModel.isSecaUser && workoutSelectionViewModel.checkSecaTiersEnabled(),
        onHome = workoutSelectionViewModel::onHomeButton,
        onUserButton = workoutSelectionViewModel::onUserButton,
        onPtpShow = { workoutSelectionViewModel.showPtp() },

        //Top Bar Language if the workout setup is the home screen
        languageList = workoutSelectionViewModel.systemModel.languageList,
        selectedLanguageIndex = workoutSelectionViewModel.systemModel.currentLanguageIndex.value,
        onLanguageSelected = { language -> workoutSelectionViewModel.systemModel.setLanguage(language) },

        //RFID dialogs for login
        rfidHandler = rfidHandler,

        //Tiers Dialog
        showTiersUpgradePrompt = workoutSelectionViewModel.showTiersUpgradePrompt,

        //UI locking for starting workout
        blockUi = workoutSelectionViewModel.isLaunchingWorkout,

        //Extra debugging
        showDebugDialog = { workoutSelectionViewModel.showDebugDialog() },
        isRfidIconVisible = workoutSelectionViewModel.needsRfidIcon(),
    )
}
