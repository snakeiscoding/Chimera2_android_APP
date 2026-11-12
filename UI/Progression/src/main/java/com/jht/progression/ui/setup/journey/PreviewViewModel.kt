package com.jht.progression.ui.setup.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jht.enums.IntervalType
import com.jht.data.ProgressionInterval
import com.jht.progression.R
import com.jht.data.EventIndex
import com.jht.progression.data.EventLabel
import com.jht.progression.data.IEvent
import com.jht.progression.data.IWorkoutInterface
import com.jht.progression.data.Program
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.data.WorkoutInfo
import com.jht.progression.data.calculateExpectedWorkoutTotals
import com.jht.progression.data.getProgramOfType
import com.jht.progression.data.program.skipOverEvent
import com.jht.progression.data.program.skipToEvent
import com.jht.progression.userinfo.UserInfoManager
import com.jht.unit.UnitFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PreviewViewModel(
    private val eventSelected: Int,
    private val userInfoManager: UserInfoManager,
    useMetric: Boolean
) : ViewModel() {
    private val _uiState: MutableStateFlow<MmPreviewState>
    private val program: Program = getProgramOfType(userInfoManager.userInfo.programType)

    init {
        val workouts = program.workoutInterface
        val event = userInfoManager.userInfo.programEvents.events[eventSelected]
        val baseline = userInfoManager.userInfo.baselineKph
        val goalBaseline = userInfoManager.userInfo.goalBaselineKph ?: -1f
        val workoutInfo = workouts.getWorkout(eventSelected, baseline, goalBaseline)

        val totalTimeMs: Int

        // If event is completed get actual values
        if (event is WorkoutEvent && event.completed) {
            // Get actual values
            totalTimeMs = event.totalTimeSec * 1000
        } else {
            // Get projected values
            val (totalTime, _) = calculateExpectedWorkoutTotals(
                intervals = workoutInfo.intervalList, workouts = workouts, baseline = baseline, isTotalValue = true
            )
            totalTimeMs = totalTime
        }

        _uiState = MutableStateFlow(
            MmPreviewState(
                useMetric = useMetric,
                previewState = getPreviewState(event),
                totalTime = UnitFormatter.formatTimeMilliseconds(totalTimeMs),
                workoutPreviewTitle = previewWorkoutTitle(event),
                intervals =  getIntervalInfo(workoutInfo.intervalList.intervalList, workouts, baseline, useMetric),
            )
        )
    }

    private fun getIntervalInfo(
        intervalList: List<ProgressionInterval>,
        workouts: IWorkoutInterface,
        baseline: Float,
        useMetric: Boolean
    ): List<IntervalState>{
        val intervalSpeeds = mutableListOf<IntervalState>()
        intervalList.forEach { interval->
            val speed = workouts.getIntervalTypeSpeed(interval.intervalType, baseline)
            val intervalData = IntervalState(
                intervalType = interval.intervalType,
                goalValue = interval.goalValue,
                speed = UnitFormatter.formatSpeed(speed, useMetric)
            )
            intervalSpeeds.add(intervalData)
        }
        return intervalSpeeds
    }

    data class IntervalState(
        val intervalType: IntervalType,
        val goalValue: Float = 0f,
        val speed: String
    )

    private fun previewWorkoutTitle(event: IEvent) : Int{
        return when(event.label.id){
            EventLabel.WORKOUT_LONG_SLOW_DISTANCE.id -> R.string.long_distance_workout_tc
            EventLabel.WORKOUT_INTERVAL.id -> R.string.interval_workout_tc
            EventLabel.WORKOUT_TEMPO.id -> R.string.tempo_run_tc
            EventLabel.REST.id -> R.string.rest_uc
            EventLabel.CROSS.id -> R.string.cross_uc
            EventLabel.ACTIVE_RECOVERY.id -> R.string.active_recovery_uc
            EventLabel.RACE.id -> R.string.race_uc
            else -> -1
        }
    }

    private fun getPreviewState(event: IEvent): PreviewState {
        return if (event.completed) {
            PreviewState.STATE_COMPLETED
        } else {
            val currentTime = System.currentTimeMillis()
            if (event.startTime <= currentTime && event.endTime >= currentTime) {
                PreviewState.STATE_ACTIVE
            } else {
                PreviewState.STATE_FUTURE
            }
        }
    }


    val uiState: StateFlow<MmPreviewState> = _uiState.asStateFlow()

    /**
     * Starts a workout for the current event
     */
    fun startWorkout(launchWorkout: (WorkoutInfo, EventIndex) -> Unit) {
        val workout = program.workoutInterface.getWorkout(eventSelected,
            userInfoManager.userInfo.baselineKph,
            userInfoManager.userInfo.goalBaselineKph ?: -1f)
        val eventIndex = EventIndex(program.programType, eventSelected)
        launchWorkout(workout, eventIndex)
    }

    /**
     * Skips to this workout
     */
    fun skipToWorkout() {
        val programEvents = userInfoManager.userInfo.programEvents
        val event = programEvents.events[eventSelected]
        val updatedProgram = skipToEvent(event, programEvents)
        userInfoManager.setProgramEvents(updatedProgram)
        userInfoManager.writeUserInfoToFile()
        _uiState.update { it.copy(previewState = PreviewState.STATE_ACTIVE) }
    }

    /**
     * Completes the program and shows the program summary screen
     */
    fun skipRaceEvent() {
        val programEvents = userInfoManager.userInfo.programEvents
        val event = programEvents.events[eventSelected]
        val updatedProgram = skipOverEvent(event, programEvents, true)
        userInfoManager.setProgramEvents(updatedProgram)
        userInfoManager.writeUserInfoToFile()
    }

    fun isRaceEvent() : Boolean {
        val programEvents = userInfoManager.userInfo.programEvents
        val event = programEvents.events[eventSelected]
        return event.label == EventLabel.RACE
    }

    override fun onCleared() {
    }

    companion object {
        fun provideFactory(
            eventSelected: Int, userInfoManager: UserInfoManager, useMetric: Boolean
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PreviewViewModel(eventSelected, userInfoManager, useMetric) as T
            }
        }
    }
}

enum class PreviewState {
    STATE_COMPLETED, STATE_ACTIVE, STATE_FUTURE
}

data class MmPreviewState(
    val useMetric: Boolean,
    val previewState: PreviewState,
    val totalTime: String,
    val workoutPreviewTitle: Int,
    val intervals: List<PreviewViewModel.IntervalState>,
)