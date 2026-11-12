package com.jht.sprint8

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.sprint8.sprint8classic.ui.Sprint8Screen
import com.jht.sprint8.sprint8immersive.ui.Sprint8NewMainScreen
import com.jht.sprint8.utils.WorkoutMode

/**
 * Composable function representing the main route for the Sprint8 workout flow.
 * @param sprint8ViewModel The [Sprint8ViewModel] responsible for managing Sprint8 workout state,
 * including progress, sweat scores, and workout summary.
 * @param sprint8Type The selected workout mode ([WorkoutMode]), which determines the type of
 * Sprint8 workout UI and logic (e.g., Classic or New).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8Route(
    sprint8ViewModel: Sprint8ViewModel,
    sprint8Type: WorkoutMode
) {
    val heartRate by sprint8ViewModel.heartRate
    val rpm by sprint8ViewModel.rpm
    val spm by sprint8ViewModel.spm
    val sprint8DataConfig by sprint8ViewModel.sprint8DataConfig
    val getReadyConfig by sprint8ViewModel.getReadyConfig
    val isRpmVisible by sprint8ViewModel.isRpmVisible
    val isCadenceVisible by sprint8ViewModel.isCadenceVisible
    val uiState by sprint8ViewModel.uiState.collectAsStateWithLifecycle()
    val isAssessmentVisible by sprint8ViewModel.isAssessmentVisible()
        .collectAsStateWithLifecycle(false)

    if (sprint8Type == WorkoutMode.Immersive) {
        Sprint8NewMainScreen(
            rpmValue = rpm,
            spmValue = spm,
            frameControls = sprint8ViewModel.frameControls.value,
            sprint8DataConfig = sprint8DataConfig,
            getReadyConfig = getReadyConfig.getReadyConfig,
            uiState = uiState,
            isRpmVisible = isRpmVisible,
            isCadenceVisible = isCadenceVisible,
            onSkipClick = { sprint8ViewModel.skipInterval() },
            onFullscreenClick = { sprint8ViewModel.fullScreen() },
            assessmentButtonConfig = sprint8ViewModel.assessmentDialogButtonConfig.value,
            onPlayPauseButtonClick = sprint8ViewModel::onPlayPauseButtonClick,
            isAssessmentVisible = isAssessmentVisible,
            getLevelName = sprint8ViewModel::getLevelName
        )
    } else {
        Sprint8Screen(
            heartRateValue = heartRate,
            rpmValue = rpm,
            spmValue = spm,
            frameControls = sprint8ViewModel.frameControls.value,
            sprint8DataConfig = sprint8DataConfig,
            getReadyConfig = getReadyConfig.getReadyConfig,
            uiState = uiState,
            isRpmVisible = isRpmVisible,
            isCadenceVisible = isCadenceVisible,
            onPlayPauseButtonClick = sprint8ViewModel::onPlayPauseButtonClick,
            onSkipClick = sprint8ViewModel::skipInterval,
            onFullscreenClick = { sprint8ViewModel.fullScreen() })
    }
}