package com.jht.progression.ui.setup.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jht.progression.data.Program
import com.jht.progression.data.convertRaceTimeToSeconds
import com.jht.progression.data.secondsToHrMinSecString
import com.jht.progression.userinfo.UserInfoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


/**
 * ViewModel for managing the goal settings screen.
 *
 * @property userInfoManager Provides user information needed for computations.
 * @property program The workout program actively in use.
 */
class GoalSettingsViewModel(
    private val userInfoManager: UserInfoManager,
    private val program: Program
) : ViewModel() {
    val uiState: StateFlow<MmGoalSettingsState>
    private var _uiState: MutableStateFlow<MmGoalSettingsState>

    init {
        val goalTime = getGoalTime()
        val estimatedRaceTime = getEstimatedRaceTime()
        val maxRaceTimeReduction = getMaxRaceTimeReduction(estimatedRaceTime)
        val isValidGoalTime = isValidGoalTime(goalTime, estimatedRaceTime)

        _uiState = MutableStateFlow(
            MmGoalSettingsState(
                goalTime = goalTime,
                estimatedRaceTime = estimatedRaceTime,
                isValidGoalTime = isValidGoalTime,
                maxRaceTimeReduction = maxRaceTimeReduction
            )
        )

        uiState = _uiState.asStateFlow()
    }

    /**
     * Handler for when the user changes the goal time input. Updates the UI state wih the newly
     * entered goal time and goal time validation.
     *
     * @param newGoalTime The new goal time entered by the user.
     */
    fun onGoalTimeChanged(newGoalTime: String) {
        _uiState.update { currentState ->
            currentState.copy(
                goalTime = newGoalTime,
                isValidGoalTime = isValidGoalTime(newGoalTime, currentState.estimatedRaceTime),
            )
        }
    }

    /**
     * Computes the goal time baseline for the provided goal time.
     *
     * @param goalTime The goal time in HHMMSS format.
     * @return The computed goal time baseline in kph.
     */
    fun computeGoalBaseline(goalTime: String): Float {
        return program.workoutInterface.computeBaselineFromWorkoutTime(
            convertRaceTimeToSeconds(goalTime)
        )
    }

    /**
     * Retrieves the goal time from the user's saved data.
     *
     * @return The goal time in HHMMSS format.
     */
    private fun getGoalTime(): String {
        return userInfoManager.userInfo.goalBaselineKph?.let {
            val (_, _, goalTimeSeconds) = program.workoutInterface.getRaceMetrics(it)
            secondsToHrMinSecString(goalTimeSeconds, "").trimStart { char -> char == '0' }
        } ?: ""
    }

    /**
     * Retrieves the user's estimated race time from their saved data.
     *
     * @return The estimated race time in HH:MM:SS format.
     */
    private fun getEstimatedRaceTime(): String {
        val baseline = userInfoManager.userInfo.baselineKph
        val (_, _, estimatedRaceTimeInSeconds) = program.workoutInterface.getRaceMetrics(baseline)
        return secondsToHrMinSecString(estimatedRaceTimeInSeconds)
    }

    /**
     * Validates the goal time against the estimated race time.
     *
     * @param goalTime The goal time in HHMMSS format.
     * @param estimatedRaceTime The estimated race time in HH:MM:SS format.
     * @return True if the goal time is valid, false otherwise.
     */
    private fun isValidGoalTime(
        goalTime: String,
        estimatedRaceTime: String,
    ): Boolean {
        val estimatedRaceTimeInSeconds = convertRaceTimeToSeconds(estimatedRaceTime)
        val goalTimeInSeconds = convertRaceTimeToSeconds(goalTime)
        val maxRaceTimeReductionSeconds = getMaxRaceTimeReduction(estimatedRaceTime) * 60
        val maxGoalTimeInSeconds = estimatedRaceTimeInSeconds - 1
        val minGoalTimeInSeconds = estimatedRaceTimeInSeconds - maxRaceTimeReductionSeconds
        return goalTimeInSeconds in minGoalTimeInSeconds..maxGoalTimeInSeconds
    }

    /**
     * Calculates the maximum race time reduction based on the estimated race time.
     *
     * The number of minutes will change depending on whether the estimated
     * time is in the 10's/20's/30's/40's/50's/60's/70's/80's/90's:
     * 13-19 minute 5k = “…must be within 1 minute faster”;
     * 20-29 minute 5k = “…must be within 2 minute faster”;
     * 30-39 minute 5k = “…must be within 3 minute faster”;
     * 40-49 minute 5k = “…must be within 4 minute faster”;
     * 50-59 minute 5k = “…must be within 5 minute faster";
     * 60-69 minute 5k = “…must be within 6 minute faster”;
     * 70-79 minute 5k = “…must be within 7 minute faster”;
     * 80-89 minute 5k = “…must be within 8 minute faster";
     * 90-99 minute 5k = “…must be within 9 minute faster";
     *
     * @param estimatedRaceTime The estimated race time in HH:MM:SS format.
     * @return The maximum race time reduction in minutes.
     */
    private fun getMaxRaceTimeReduction(estimatedRaceTime: String): Int {
        val estimatedRaceTimeInSeconds = convertRaceTimeToSeconds(estimatedRaceTime)
        val minutes = (estimatedRaceTimeInSeconds / 60) / 10
        return minutes.coerceIn(1, 10)
    }

    companion object {
        /**
         * Provides a factory to create an instance of [GoalSettingsViewModel].
         *
         * @param userInfoManager Provides user information needed for computations.
         * @param program Contains the workout program and its associated data.
         * @return A [ViewModelProvider.Factory] to create an instance of [GoalSettingsViewModel].
         */
        fun provideFactory(
            userInfoManager: UserInfoManager,
            program: Program
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GoalSettingsViewModel(userInfoManager, program) as T
            }
        }
    }
}

/**
 * Data class representing the state of the goal settings UI.
 *
 * @property goalTime The goal time set by the user in HHMMSS format.
 * @property estimatedRaceTime The estimated race time based on user's baseline in HH:MM:SS format.
 * @property isValidGoalTime Indicates if the goal time is valid.
 * @property maxRaceTimeReduction The maximum race time reduction allowed in minutes.
 */
data class MmGoalSettingsState(
    val goalTime: String,
    val estimatedRaceTime: String,
    val isValidGoalTime: Boolean,
    val maxRaceTimeReduction: Int
)