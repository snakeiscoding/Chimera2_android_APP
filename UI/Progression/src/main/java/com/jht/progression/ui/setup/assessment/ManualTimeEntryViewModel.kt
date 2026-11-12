package com.jht.progression.ui.setup.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jht.progression.data.Program
import com.jht.progression.data.convertRaceTimeToSeconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ManualTimeEntryViewModel(
    private val program: Program
) : ViewModel() {
    private var _uiState = MutableStateFlow(
        MmManualTimeEntryState(
            raceTime = "",
            isValidRaceTime = false,
            minRaceTimeInSeconds = program.workoutInterface.minRaceTimeInSeconds,
            maxRaceTimeInSeconds = program.workoutInterface.maxRaceTimeInSeconds
        )
    )
    val uiState: StateFlow<MmManualTimeEntryState> = _uiState.asStateFlow()

    fun onRaceTimeChanged(newRaceTime: String) {
        _uiState.update { currentState ->
            currentState.copy(
                raceTime = newRaceTime,
                isValidRaceTime = isValidRaceTime(newRaceTime)
            )
        }
    }

    fun computeBaseline(raceTime: String): Float {
        return program.workoutInterface.computeBaselineFromWorkoutTime(
            convertRaceTimeToSeconds(raceTime)
        )
    }

    private fun isValidRaceTime(raceTime: String): Boolean {
        val minRaceTime = program.workoutInterface.minRaceTimeInSeconds
        val maxRaceTime = program.workoutInterface.maxRaceTimeInSeconds
        val raceTimeSeconds = convertRaceTimeToSeconds(raceTime)
        return raceTimeSeconds in minRaceTime..maxRaceTime
    }

    companion object {
        fun provideFactory(
            program: Program
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ManualTimeEntryViewModel(program) as T
            }
        }
    }
}

data class MmManualTimeEntryState(
    val raceTime: String,
    val isValidRaceTime: Boolean,
    val minRaceTimeInSeconds: Int,
    val maxRaceTimeInSeconds: Int
)