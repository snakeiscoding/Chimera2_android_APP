package com.jht.progression.ui.workout

import android.util.Log
import androidx.lifecycle.ViewModel
import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.interfaces.models.WorkoutModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.data.MmIntervalList
import com.jht.data.ProgressionInterval
import com.jht.progression.data.ProgramManager
import com.jht.progression.data.getProgramOfType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val TAG = "WorkoutViewModel"

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val progressionModel: ProgressionModel,
    private val workoutModel: WorkoutModel,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            WorkoutState(
                intervalList = getSafeIntervals(progressionModel.workoutIntervalList.value),
                currentInterval = workoutModel.intervalNumber.value,
                displayIntervalList = canDisplayIntervalList(),
                displayTips = canDisplayTips(),
                useMetric = workoutModel.isMetric.value,
            )
        )
    val uiState: StateFlow<WorkoutState> = _uiState.asStateFlow()
    private var workoutTips: Map<Int, Int> = mapOf()

    init {
        workoutModel.intervalNumber.listenAndTrigger { intervalNumber ->
            _uiState.update { currentState ->
                currentState.withIntervals(currentState.intervalList, intervalNumber)
            }
        }

        // This only works for time goal types, but that's all we support right now
        workoutModel.intervalGoalRemaining.listenAndTrigger {
            _uiState.update { currentState ->
                currentState.copy(
                    goalRemaining = it
                )
            }
        }

        progressionModel.workoutIntervalList.listen { intervalList ->
            _uiState.update { currentState ->
                currentState.withIntervals(intervalList, currentState.currentInterval)
            }
        }

        workoutModel.isMetric.listen {
            _uiState.update { currentState ->
                currentState.copy(
                    useMetric = it
                )
            }
        }

        workoutModel.isWorkoutStopped.listen { stopped ->
            val workoutIndex = progressionModel.eventIndex.value.eventIndex
            if (stopped) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isWorkoutStopped = true
                    )
                }
            }else{
                workoutTips = mapTipsForWorkout(workoutIndex)
            }
        }

        workoutModel.timeElapsedMs.listenAndTrigger { elapsedTime ->
            val tipResId = workoutTips[elapsedTime]
            tipResId?.let { resId ->
                _uiState.update { currentState ->
                    currentState.copy(
                        tipText = resId,
                        isTipBoxVisible = currentState.isTipBoxEnabled
                    )
                }
            }

            _uiState.update { currentState ->
                currentState.copy(
                    timeElapsedMs = elapsedTime
                )
            }
        }

        workoutModel.spm.listenAndTrigger { value ->
            if (value != -1) {
                _uiState.update { it.copy(spm = value) }
            }
        }

        _uiState.update { it.copy(isCadenceVisible = workoutModel.cadenceDisplayAllowed) }
    }

    /**
     * Keeps [WorkoutState.intervalList], [WorkoutState.currentInterval] and
     * [WorkoutState.intervalType] consistent with each other. WorkoutScreen indexes the list by
     * currentInterval, so an index that falls outside a newly arrived list has to be clamped
     * here rather than at each call site.
     */
    private fun WorkoutState.withIntervals(
        intervalList: MmIntervalList,
        intervalNumber: Int,
    ): WorkoutState {
        val safeList = getSafeIntervals(intervalList)
        val intervals = safeList.intervalList
        // getSafeIntervals never returns an empty list, so indices is a valid coercion range
        val clampedNum = if (intervalNumber in intervals.indices) {
            intervalNumber
        } else {
            Log.e(TAG, "Trying to set invalid interval number $intervalNumber for ${intervals.size} intervals!")
            intervalNumber.coerceIn(intervals.indices)
        }
        return copy(
            intervalList = safeList,
            currentInterval = clampedNum,
            intervalType = intervals[clampedNum].intervalType,
        )
    }

    private fun getSafeIntervals(intervalList: MmIntervalList) : MmIntervalList {
        return if (intervalList.intervalList.isEmpty()) {
            MmIntervalList(listOf(ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, 0f)))
        } else {
            intervalList
        }
    }

    fun onDismissTipBox() {
        _uiState.update { it.copy(isTipBoxVisible = false) }
    }

    fun setTipBoxState(isTipBoxEnabled: Boolean) {
        _uiState.update {
            it.copy(isTipBoxEnabled = isTipBoxEnabled)
        }
    }

    fun clampSpeed(speedKph: Float): Float {
        return workoutModel.clampSpeedToFrameLimits(speedKph)
    }

    private fun canDisplayIntervalList(): Boolean {
        // Will be false during assessment and race event
        val eventIndex = progressionModel.eventIndex
        val program = getProgramOfType(eventIndex.value.programType)

        return eventIndex.value.eventIndex >= 0 &&
                program.workoutInterface.shouldDisplayNextInterval(eventIndex.value.eventIndex)
    }

    private fun canDisplayTips() : Boolean {
        val eventIndex = progressionModel.eventIndex
        val program = getProgramOfType(eventIndex.value.programType)
        return program.workoutInterface.shouldDisplayNextInterval(eventIndex.value.eventIndex)
    }

    override fun onCleared() {
//        dataMatrix.finish()
    }
}

data class WorkoutState(
    val goalRemaining: Float = 0f,
    val intervalType: IntervalType = IntervalType.ZONE_1,
    val tipText: Int = 0,
    val isTipExpanded: Boolean = true,
    val intervalList: MmIntervalList = MmIntervalList(ArrayList()),
    val currentInterval: Int = 0,
    val displayIntervalList: Boolean,
    val displayTips: Boolean,
    val useMetric: Boolean,
    val isTipBoxVisible: Boolean = false,
    val isTipBoxEnabled: Boolean = false,
    val timeElapsedMs: Int = 0,
    val isWorkoutStopped: Boolean = false,
    val spm: Int = 0,
    val isCadenceVisible: Boolean = false,
)
