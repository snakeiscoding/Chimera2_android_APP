package com.jht.spinbikeapps.intervals

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jht.spinbikeapps.targettraining.ui.popup.ConfirmationPopUp
import com.jht.widgets.RestringThemedActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpinBikeIntervalsActivity : RestringThemedActivity() {

    override val themedViewModel: IntervalsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by themedViewModel.uiState.collectAsState()

            Box(modifier = Modifier.fillMaxSize()) {
                IntervalsWorkoutScreen(
                    completionProgress = state.completionProgress,
                    intervalGoal = state.intervalGoal,
                    mainText = state.mainText,
                    subText = state.subText,
                    currentIntervalIndex = state.currentIntervalIndex,
                    currentPhase = state.currentPhase,
                    previousIntervalsData = state.previousIntervals,
                    isMetric = state.isMetric,
                    totalDistanceText = state.totalDistanceText,
                    totalTimeText = state.totalTimeText,
                )

                if (state.showTargetReachedPopup) {
                    ConfirmationPopUp(
                        modifier = Modifier.align(Alignment.Center),
                        onClickContinue = {themedViewModel.onContinueWorkoutClicked()},
                        onClickEndWorkout = themedViewModel::onEndWorkoutClicked,
                    )
                }
            }
        }
    }
}