package com.jht.firsttimesetup.viewmodels

import com.jht.firsttimesetup.FirstTimeSetupModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MachineCalibrationViewModel @Inject constructor(
    private val ftsuModel: FirstTimeSetupModel
) : ThemedViewModel() {

    private val _uiState = MutableStateFlow(MachineCalibrationUi())

    val uiState = _uiState.asStateFlow()

    init {
        val isTreadmill = ftsuModel.isTreadmill()
        _uiState.value = _uiState.value.copy(
            calibrationType = CalibrationType.Treadmill.takeIf { isTreadmill } ?: CalibrationType.Ascent
        )

        ftsuModel.isCalibrating.listen { calibrationState ->
            val state = intToCalibrationState(calibrationState)
            _uiState.value = _uiState.value.copy(
                state = state,
                calibrationType = CalibrationType.Treadmill.takeIf { isTreadmill } ?: CalibrationType.Ascent
            )
        }
    }


    /**
     * Starts calibration if not already calibrating
     */
    fun startCalibration() {
        ftsuModel.startCalibration()
    }


    /**
     * Test function that sets the machine calibration state to calibrating
     */
    fun triggerMachineCalibration() {
        _uiState.value = _uiState.value.copy(
            state = MachineCalibrationState.CALIBRATING
        )
    }

    /**
     * Test function that sets the machine calibration state to success
     */
    fun triggerMachineCalibrationSuccess() {
        _uiState.value = _uiState.value.copy(
            state = MachineCalibrationState.SUCCESS
        )
    }

    /**
     * Test function that sets the machine calibration state to calibrating
     */
    fun triggerMachineCalibrationFailure() {
        _uiState.value = _uiState.value.copy(
            state = MachineCalibrationState.FAIL
        )
    }

    /**
     * Test function that sets the machine calibration state to start
     */
    fun onRestartDemo() {
        _uiState.value = _uiState.value.copy(
            state = MachineCalibrationState.START
        )
    }

    fun onToggleMachineCalibrationResult() {
        if (_uiState.value.state == MachineCalibrationState.SUCCESS) {
            _uiState.value = _uiState.value.copy(
                state = MachineCalibrationState.FAIL
            )
        } else if (_uiState.value.state == MachineCalibrationState.FAIL) {
            _uiState.value = _uiState.value.copy(
                state = MachineCalibrationState.SUCCESS
            )
        }
    }
}

/**
 * Type of calibration supported
 */
enum class CalibrationType {
    Ascent,
    Treadmill,
    NotConfigured
}


/**
 * Machine calibration Ui state
 */

data class MachineCalibrationUi(
    val state: MachineCalibrationState = MachineCalibrationState.START,
    val calibrationType: CalibrationType = CalibrationType.NotConfigured
)

/**
 * States for machine calibration Ui
 */
enum class MachineCalibrationState {
    START,
    CALIBRATING,
    SUCCESS,
    FAIL
}

fun MachineCalibrationState.toInt() : Int {
    return when(this) {
        MachineCalibrationState.START -> 0
        MachineCalibrationState.CALIBRATING -> 1
        MachineCalibrationState.SUCCESS -> 2
        MachineCalibrationState.FAIL -> 3
    }
}

/**
 * Since we don't have access to
 */
fun intToCalibrationState(intState: Int) : MachineCalibrationState {
    return when(intState) {
        0 -> MachineCalibrationState.START
        1 -> MachineCalibrationState.CALIBRATING
        2 -> MachineCalibrationState.SUCCESS
        3 -> MachineCalibrationState.FAIL
        else -> MachineCalibrationState.START
    }
}