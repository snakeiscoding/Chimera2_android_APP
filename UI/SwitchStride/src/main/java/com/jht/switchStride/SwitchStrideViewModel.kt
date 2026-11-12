package com.jht.switchStride

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.switchStride.model.ButtonType
import com.jht.switchStride.model.ButtonConfig
import com.jht.switchStride.model.SwitchStrideUIState
import com.jht.switchStride.topbar.SwitchStridePanelData
import com.jht.switchStride.topbar.SwitchStrideTopbarHolder
import com.jht.switchStride.utils.pauseButtonColor
import com.jht.switchStride.utils.resumeButtonColor
import com.jht.switchStride.utils.runScreenForwardBlue
import com.jht.switchStride.utils.runScreenReversePurple
import com.jht.switchStride.utils.stopButtonColor
import com.jht.theme.ThemedViewModel
import com.jht.widgets.helpers.StatInfo
import com.jht.widgets.helpers.StatType.CALORIES
import com.jht.widgets.helpers.StatType.DISTANCE
import com.jht.widgets.helpers.StatType.HEART_RATE
import com.jht.widgets.helpers.StatType.PACE
import com.jht.widgets.helpers.StatType.TIME_ELAPSED
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class SwitchStrideViewModel @Inject constructor(
    private val uiBaseModel: UIBaseModel,
    private val deviceModel: DeviceModel,
    val workoutModel: WorkoutModel,
    private val uiServiceModel: UIServiceModel,
) : ThemedViewModel() {

    private val panelHolder by lazy { mutableStateOf<SwitchStrideTopbarHolder?>(null) }
    private fun sendPanelUpdate(isVisible: Boolean) {
        val panelData = SwitchStridePanelData(isVisible)
        uiServiceModel.sendUiPanelData((panelData))
    }

    /**
     * Initializes and adds the top bar panel when entering the App Library,
     * only if it hasn’t been created yet.
     */
    private fun handleTopBarVisibility() {
        if (panelHolder.value == null) {
            panelHolder.value = SwitchStrideTopbarHolder()
            panelHolder.value?.let { innerPanelHolder ->
                uiServiceModel.addUiPanel(innerPanelHolder)
            }
        }
    }

    private val _uiState = MutableStateFlow(SwitchStrideUIState())
    val uiState: StateFlow<SwitchStrideUIState> = _uiState
    private val isWorkoutRunning by lazy { mutableStateOf(false) }

    val caloriesStat = StatInfo(CALORIES)
    val heartRateStat = StatInfo(HEART_RATE)
    val timeElapsedStat = StatInfo(TIME_ELAPSED)
    val distanceStat = StatInfo(DISTANCE)
    val paceStat = StatInfo(PACE)

    private val statListOfList = listOf(
        listOf(caloriesStat, heartRateStat),  // First card: Calories / Heart Rate pager
        listOf(timeElapsedStat),              // Second card: Time Elapsed
        listOf(distanceStat, paceStat)        // Third card: Distance / Pace pager
    )


    private fun close() {
        sendPanelUpdate(false)
        uiServiceModel.close()
        uiBaseModel.close()
        deviceModel.close()
        workoutModel.close()
    }

    /**
     * Call this to register listeners when activity is resumed
     */
    override fun start() {
        handleTopBarVisibility()
        sendPanelUpdate(true)
        initializeListeners()
    }

    private fun initializeListeners() {
        workoutModel.let { workoutModel ->
            workoutModel.isWorkoutRunning.listenAndTrigger {
                isWorkoutRunning.value = it
            }
            workoutModel.isBeltInReverse.listenAndTrigger { updateUi { copy(isBeltInReverse = it) } }
            workoutModel.onStartWorkout.listen { updateUi { copy(hasWorkoutStarted = true) } }
            workoutModel.pauseTimerRemaining.listenAndTrigger { updateUi { copy(pauseTimer = it) } }
            workoutModel.isWorkoutPaused.listenAndTrigger { updateUi { copy(isWorkoutPaused = it) } }
        }
        StatInfo.registerListener(statListOfList, uiBaseModel, workoutModel)
    }

    fun onPauseTapped() {
        workoutModel.pauseWorkout()
    }

    fun onResumeTapped() {
        workoutModel.resumeWorkout()
    }

    fun onStopTapped() {
        workoutModel.stopWorkout()
    }

    fun onReverseTapped() {
        if (!uiState.value.isWorkoutPaused) {
            updateUi { copy(showReverseBeltDialog = true) }
        }
    }

    /**
     * Button *types* that should be shown on the Run screen.
     *
     * Note: This is intentionally just types (no lambdas) so it can be safely derived from
     * current workout state, and the UI can render it deterministically.
     */
    val controlButtonsTypes: List<ButtonType> by derivedStateOf {
        listOf(
            ButtonType.REVERSE,
            ButtonType.STOP,
            if (isWorkoutRunning.value) ButtonType.PAUSE else ButtonType.RESUME
        )
    }

    fun buttonConfig(type: ButtonType): ButtonConfig {
        return when (type) {
            ButtonType.REVERSE -> ButtonConfig(
                buttonIcon = R.drawable.ic_reverse,
                buttonName = R.string.reverse_belt_tc,
                buttonColor = if (_uiState.value.isBeltInReverse) runScreenForwardBlue else runScreenReversePurple,
                onClick = ::onReverseTapped
            )

            ButtonType.STOP -> ButtonConfig(
                buttonIcon = R.drawable.ic_stop,
                buttonName = R.string.stop_tc,
                buttonColor = stopButtonColor,
                onClick = ::onStopTapped
            )

            ButtonType.PAUSE -> ButtonConfig(
                buttonIcon = R.drawable.ic_pause,
                buttonName = R.string.pause_tc,
                buttonColor = pauseButtonColor,
                onClick = ::onPauseTapped
            )

            ButtonType.RESUME -> ButtonConfig(
                buttonIcon = R.drawable.ic_resume,
                buttonName = R.string.resume_tc,
                buttonColor = resumeButtonColor,
                onClick = ::onResumeTapped
            )
        }
    }

    /**
     * Call this to register listeners when activity is paused
     */
    override fun stop() {
        close()
    }
    private inline fun updateUi(update: SwitchStrideUIState.() -> SwitchStrideUIState) =
        _uiState.update(update)

    val showPauseTimer by derivedStateOf {
        controlButtonsTypes.lastOrNull() == ButtonType.RESUME
    }

    fun onReverseConfirmed() {
        viewModelScope.launch {
            workoutModel.setBeltInReverse(!_uiState.value.isBeltInReverse)
            delay(3000) // 3-second delay added; logic not present yet.
            updateUi {
                copy(
                    showReverseBeltDialog = false
                )
            }
        }
    }
    fun onReverseDialogCancelled() {
        updateUi {
            copy(
                showReverseBeltDialog = false
            )
        }
    }
}
