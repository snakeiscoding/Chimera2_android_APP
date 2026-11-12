package com.jht.spinbikeapps.intervals

import com.jht.enums.WorkoutId
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.theme.ThemedViewModel
import com.jht.unit.UnitFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
open class IntervalsViewModel @Inject constructor(
    private val uiBaseModel: UIBaseModel,
    private val workoutModel: WorkoutModel,
    private val navigationModel: NavigationModel
) : ThemedViewModel() {

    private val _uiState = MutableStateFlow(IntervalsWorkoutState(
        isMetric = workoutModel.isMetric.value
    ))

    val uiState: StateFlow<IntervalsWorkoutState> = _uiState.asStateFlow()

    // Running history of STRICTLY COMPLETED intervals, oldest first.
    private var history = emptyList<IntervalData>()
    private var lastPhase = IntervalPhase.Other
    private var lastIntervalIndexFromMachine = -1

    // Work/Rest setup goals, sourced live from the console
    private var workTimeGoalMs = 0f
    private var workDistGoalKm = 0f
    private var restTimeGoalMs = 0f

    // Total number of Work intervals configured on the pillbox (NUM_INTERVALS). Used to detect
    // when the whole workout's target has been reached, to trigger the "Target Reached!" popup.
    private var numIntervalsGoal = 0

    private var intervalStartDistKm = 0f
    private var intervalStartTimeMs = 0f
    private var hasSeenWork = false

    // Set before resetKeysAtWorkoutEnd() zeroes the script keys, which used to flash a Rest interval.
    private var workoutEnded = false

    override fun start() {
        observeUnitAndWorkoutId()
        workoutModel.onStartWorkout.listen { resetForNewWorkout() }
        observeWorkoutEnd()
        observeLiveTelemetry()
        observeSetupGoals()
    }

    private fun observeWorkoutEnd() {
        workoutModel.apply {
            isWorkoutStopped.listenAndTrigger { if (it) workoutEnded = true }
            isWorkoutSwitching.listenAndTrigger { if (it) workoutEnded = true }
        }
    }

    /** Applies [update] only while the workout is live, so end-of-workout key resets are ignored. */
    private inline fun whileWorkoutLive(update: () -> Unit) {
        if (!workoutEnded) update()
    }

    private fun observeUnitAndWorkoutId() {
        workoutModel.isMetric.listen {
            _uiState.update { currentState -> currentState.copy(isMetric = it) }
        }

        workoutModel.workoutId.listenAndTrigger { id ->
            _uiState.update {
                it.copy(
                    workoutId = id,
                    intervalGoal = if (id == WorkoutId.SPIN_BIKE_INTERVALS_DISTANCE) IntervalGoal.Distance else IntervalGoal.Time
                )
            }
        }
    }

    /** Resets all per-workout tracking state for a fresh run. */
    private fun resetForNewWorkout() {
        history = emptyList()
        lastPhase = IntervalPhase.Other
        lastIntervalIndexFromMachine = -1
        hasSeenWork = false
        workoutEnded = false
        intervalStartDistKm = workoutModel.distanceKm.value
        intervalStartTimeMs = workoutModel.timeElapsedMs.value.toFloat()
        _uiState.update { it.copy(showTargetReachedPopup = false) }
    }

    private fun observeLiveTelemetry() {
        workoutModel.apply {
            intervalGoalRemaining.listenAndTrigger { whileWorkoutLive { refreshDial() } }

            distanceKm.listenAndTrigger { dist ->
                whileWorkoutLive {
                    val formattedDist = formatDistance(dist)
                    _uiState.update { state ->
                        state.copy(
                            subText = formattedDist,
                            totalDistanceText = formattedDist
                        )
                    }
                    refreshDial()
                }
            }

            timeElapsedMs.listenAndTrigger { ms ->
                whileWorkoutLive {
                    _uiState.update { state -> state.copy(totalTimeText = formatTime(ms.toFloat())) }
                    refreshDial()
                }
            }

            intervalNumber.listenAndTrigger { num ->
                whileWorkoutLive {
                    _uiState.update { it.copy(currentIntervalIndexFromMachine = num) }
                    refreshDial(machineIntervalIndex = num)
                }
            }

            scriptIsSprinting.listenAndTrigger { isSprinting ->
                whileWorkoutLive {
                    val newPhase = resolvePhase(isSprinting)
                    _uiState.update { it.copy(currentPhase = newPhase) }
                    refreshDial(phase = newPhase)
                }
            }
        }
    }

    /**
     * scriptIsSprinting reports its default `false` for a moment before the script sets it
     *  `true` for the very first work interval. These workouts always begin with Work, so a
     *  `false` reported before any real `true` is treated as still-Work rather than a genuine
     *  Rest phase.
     *  */
    private fun resolvePhase(isSprinting: Boolean): IntervalPhase {
        if (isSprinting) hasSeenWork = true
        return when {
            isSprinting -> IntervalPhase.Work
            hasSeenWork -> IntervalPhase.Rest
            else -> IntervalPhase.Work
        }
    }

    private fun observeSetupGoals() {
        workoutModel.apply {
            intervalWorkTimeGoal.listenAndTrigger {
                if (it > 0) workTimeGoalMs = it
                refreshDial()
            }
            intervalWorkDistanceGoal.listenAndTrigger {
                if (it > 0) workDistGoalKm = it
                refreshDial()
            }
            intervalRestTimeGoal.listenAndTrigger {
                if (it > 0) restTimeGoalMs = it
                refreshDial()
            }
            numberOfIntervals.listenAndTrigger {
                if (it > 0) numIntervalsGoal = it
            }
        }
    }

    /**
     * Re-runs the dial/history update using the current phase and machine interval index,
     *  sourcing `remaining` fresh from the console. Used by every listener whose change should
     *  refresh the dial but doesn't itself carry a new phase or machine index.
     */
    private fun refreshDial(
        phase: IntervalPhase = uiState.value.currentPhase,
        machineIntervalIndex: Int = uiState.value.currentIntervalIndexFromMachine,
    ) {
        updateDialAndHistory(
            remaining = workoutModel.intervalGoalRemaining.value,
            phase = phase,
            machineIntervalIndex = machineIntervalIndex,
        )
    }

    private fun updateDialAndHistory(remaining: Float, phase: IntervalPhase, machineIntervalIndex: Int) {
        if (phase == IntervalPhase.Other) return

        if (isNewTransition(phase, machineIntervalIndex)) {
            recordCompletedInterval()
            beginNewInterval(phase, machineIntervalIndex)
        }

        updateDialText(remaining, phase)
        updateHistoryUI()
    }

    private fun isNewTransition(phase: IntervalPhase, machineIntervalIndex: Int): Boolean {
        return phase != lastPhase || (phase == IntervalPhase.Work && machineIntervalIndex != lastIntervalIndexFromMachine)
    }

    /**
     * Pushes the just-finished interval into history and checks whether the workout's
     *  configured target has just been reached.
     */
    private fun recordCompletedInterval() {
        if (lastPhase == IntervalPhase.Other || workoutModel.timeElapsedMs.value <= 1000) return

        val actualTimeMs = workoutModel.timeElapsedMs.value - intervalStartTimeMs
        val actualDistKm = workoutModel.distanceKm.value - intervalStartDistKm
        history = history + createIntervalData(lastPhase, actualTimeMs, actualDistKm)

        // If the interval that just completed was Work, and it's the last one configured on the
        // pillbox, the whole workout's target has been reached.
        val workCount = history.count { it.intervalType == IntervalPhase.Work }
        if (lastPhase == IntervalPhase.Work && numIntervalsGoal > 0 && workCount == numIntervalsGoal) {
            _uiState.update { it.copy(showTargetReachedPopup = true) }
        }
    }

    private fun beginNewInterval(phase: IntervalPhase, machineIntervalIndex: Int) {
        lastPhase = phase
        lastIntervalIndexFromMachine = machineIntervalIndex
        intervalStartDistKm = workoutModel.distanceKm.value
        intervalStartTimeMs = workoutModel.timeElapsedMs.value.toFloat()
    }

    private fun updateDialText(remaining: Float, phase: IntervalPhase) {
        val dialShowsTime = (phase == IntervalPhase.Rest) || (uiState.value.intervalGoal == IntervalGoal.Time)
        val effectiveGoal = resolveEffectiveGoal(phase, dialShowsTime)
        val coveredKm = distanceCoveredInPhaseKm()
        val displayRemaining = if (dialShowsTime) resolveTimeRemainingMs(remaining, phase)
                               else (effectiveGoal - coveredKm).coerceAtLeast(0f)

        _uiState.update { state ->
            state.copy(
                completionProgress = calculateProgress(effectiveGoal, displayRemaining),
                mainText = if (dialShowsTime) formatTime(displayRemaining)
                           else formatDistanceRemaining(effectiveGoal, coveredKm)
            )
        }
    }

    /**
     * The goal the completion ring measures progress against.
     * if that's not yet available (reports 0), falls back to our own setup goal for phase.
     */
    private fun resolveEffectiveGoal(phase: IntervalPhase, dialShowsTime: Boolean): Float {
        val goalValue = workoutModel.intervalGoal.value
        if (goalValue > 0) return goalValue
        if (!dialShowsTime) return workDistGoalKm

        return if (phase == IntervalPhase.Rest) restTimeGoalMs else workTimeGoalMs
    }

    private fun calculateProgress(effectiveGoal: Float, displayRemaining: Float): Float {
        if (effectiveGoal <= 0) return 0f
        return (effectiveGoal - displayRemaining).coerceAtLeast(0f) / effectiveGoal
    }

    /**
     * Manual countdown fallback if the machine reports 0 or a very small value.
     */
    private fun resolveTimeRemainingMs(remaining: Float, phase: IntervalPhase): Float {
        if (remaining > 0.005f) return remaining

        val goalMs = if (phase == IntervalPhase.Rest) restTimeGoalMs else workTimeGoalMs
        return (goalMs - (workoutModel.timeElapsedMs.value - intervalStartTimeMs)).coerceAtLeast(0f)
    }

    /**
     * Rebuilds the completed-intervals table and the top "Interval N" label. The in-progress
     *  interval is represented only by the dial — it is never included as a table row.
     */
    private fun updateHistoryUI() {
        val workCount = history.count { it.intervalType == IntervalPhase.Work }
        val currentDisplayIndex = if (uiState.value.currentPhase == IntervalPhase.Work) workCount + 1 else workCount

        _uiState.update { it.copy(
            previousIntervals = history.asReversed(),
            currentIntervalIndex = currentDisplayIndex
        )}
    }

    /**
     * Builds a completed-interval table row, with all display formatting already applied
     */
    private fun createIntervalData(phase: IntervalPhase, actualTimeMs: Float, actualDistKm: Float): IntervalData {
        val isWork = phase == IntervalPhase.Work
        val isDistanceGoalWorkout = uiState.value.intervalGoal == IntervalGoal.Distance

        val timeMs = when {
            !isWork -> restTimeGoalMs // Rest is always time-based, in both workout types
            isDistanceGoalWorkout -> actualTimeMs // no time goal for Work in a Distance workout
            else -> workTimeGoalMs // Work's configured time goal in a Time workout
        }
        val distKm = when {
            isWork && isDistanceGoalWorkout -> workDistGoalKm // Work's configured distance goal
            else -> actualDistKm // no distance goal for Work-in-Time-workout or for Rest at all
        }

        // Goals round like goals elsewhere, actual distance covered floors like the stat bar
        val distanceText = if (isWork && isDistanceGoalWorkout) formatDistanceGoal(distKm)
                           else formatDistance(distKm)

        return IntervalData(
            intervalTime = formatTime(timeMs),
            intervalDistance = distanceText,
            intervalType = phase,
        )
    }

    private fun formatTime(ms: Float): String {
        val totalSecs = (ms / 1000f).roundToInt()
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return if (mins >= 60) {
            String.format(Locale.ENGLISH, "%d:%02d:%02d", mins / 60, mins % 60, secs)
        } else {
            String.format(Locale.ENGLISH, "%d:%02d", mins, secs)
        }
    }

    /** Accumulated distance. Floors like the console stat bar and workout summary so they agree. */
    private fun formatDistance(kilometers: Float): String {
        return UnitFormatter.formatDistance(kilometers, uiState.value.isMetric, workoutModel.distancePrecision)
    }

    /** Goal / remaining distance, rounded the way goal values are shown elsewhere. */
    private fun formatDistanceGoal(kilometers: Float): String {
        return UnitFormatter.formatDistanceGoal(kilometers, uiState.value.isMetric, workoutModel.distancePrecision)
    }

    /** Subtracts the two displayed values so the dial and distance covered always sum to the goal. */
    private fun formatDistanceRemaining(goalKm: Float, coveredKm: Float): String {
        val goal = BigDecimal(formatDistanceGoal(goalKm))
        val covered = BigDecimal(formatDistance(coveredKm))
        return (goal - covered)
            .coerceAtLeast(BigDecimal.ZERO)
            .setScale(workoutModel.distancePrecision)
            .toPlainString()
    }

    private fun distanceCoveredInPhaseKm(): Float {
        return (workoutModel.distanceKm.value - intervalStartDistKm).coerceAtLeast(0f)
    }

    /**
     * "Continue" on the target-reached popup — dismiss it and let the workout keep running.
     * Further work in CGC-25433
     */
    fun onContinueWorkoutClicked() {
        _uiState.update { it.copy(showTargetReachedPopup = false) }
        navigationModel.launchWorkoutScreen()
    }

    /**
     * "End Workout" on the target-reached popup — dismiss it and stop the workout.
     */
    fun onEndWorkoutClicked() {
        _uiState.update { it.copy(showTargetReachedPopup = false) }
        workoutModel.stopWorkout()

    }

    override fun stop() {
        uiBaseModel.close()
        workoutModel.close()
    }
}

data class IntervalsWorkoutState(
    val workoutId: WorkoutId = WorkoutId.CUSTOM_WORKOUT,
    val isMetric: Boolean = true,
    val previousIntervals: List<IntervalData> = emptyList(),
    val intervalGoal: IntervalGoal = IntervalGoal.Time,
    val completionProgress: Float = 0f,
    val mainText: String = "",
    val subText: String = "",
    val currentIntervalIndex: Int = 0,
    val currentIntervalIndexFromMachine: Int = 0,
    val currentPhase: IntervalPhase = IntervalPhase.Other,
    val totalDistanceText: String = "0.00",
    val totalTimeText: String = "0:00",
    val showTargetReachedPopup: Boolean = false,
)