package com.jht.spinbikeapps.intervals

import com.jht.enums.WorkoutId
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.MockBoolVariable
import com.jht.mock.interfaces.MockDataValue
import com.jht.mock.interfaces.MockEventListener
import com.jht.mock.interfaces.MockFloatValue
import com.jht.mock.interfaces.MockIntValue
import com.jht.mock.interfaces.models.MockNavigationModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import com.jht.unit.UnitFormatter
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class IntervalsViewModelTest {

    // Direct references to each listener so tests can drive them via .emit(...) and assert
    // against what the ViewModel produced in response.
    private lateinit var isMetric: MockBoolVariable
    private lateinit var workoutId: MockDataValue<WorkoutId>
    private lateinit var onStartWorkout: MockEventListener
    private lateinit var intervalGoalRemaining: MockFloatValue
    private lateinit var intervalGoal: MockFloatValue
    private lateinit var distanceKm: MockFloatValue
    private lateinit var timeElapsedMs: MockIntValue
    private lateinit var intervalNumber: MockIntValue
    private lateinit var scriptIsSprinting: MockBoolValue
    private lateinit var intervalWorkTimeGoal: MockFloatValue
    private lateinit var intervalWorkDistanceGoal: MockFloatValue
    private lateinit var intervalRestTimeGoal: MockFloatValue
    private lateinit var numberOfIntervals: MockIntValue
    private lateinit var isWorkoutStopped: MockBoolValue
    private lateinit var isWorkoutSwitching: MockBoolValue

    private lateinit var workoutModel: WorkoutModel
    private lateinit var navigationModel: NavigationModel
    private lateinit var uiBaseModel: UIBaseModel
    private lateinit var viewModel: IntervalsViewModel

    /**
     * Builds and starts a fresh ViewModel against fresh Mock* models. Call after seeding any
     *  initial values we want present before start()/onStartWorkout fire.
     */
    private fun createAndStart(
        initialWorkoutId: WorkoutId = WorkoutId.SPIN_BIKE_INTERVALS_DISTANCE,
        metric: Boolean = true,
    ) {
        isMetric = MockBoolVariable(metric)
        workoutId = MockDataValue(initialWorkoutId)
        onStartWorkout = MockEventListener()
        intervalGoalRemaining = MockFloatValue()
        intervalGoal = MockFloatValue()
        distanceKm = MockFloatValue()
        timeElapsedMs = MockIntValue()
        intervalNumber = MockIntValue()
        scriptIsSprinting = MockBoolValue()
        intervalWorkTimeGoal = MockFloatValue()
        intervalWorkDistanceGoal = MockFloatValue()
        intervalRestTimeGoal = MockFloatValue()
        numberOfIntervals = MockIntValue()
        isWorkoutStopped = MockBoolValue()
        isWorkoutSwitching = MockBoolValue()

        // spyk(): a REAL MockWorkoutModel underneath (so all the untouched properties behave
        // exactly as MockWorkoutModel defines them), but MockK still records calls so
        // verify { workoutModel.stopWorkout() } works.
        workoutModel = spyk(
            MockWorkoutModel(
                isMetric = isMetric,
                workoutId = workoutId,
                onStartWorkout = onStartWorkout,
                intervalGoalRemaining = intervalGoalRemaining,
                intervalGoal = intervalGoal,
                distanceKm = distanceKm,
                timeElapsedMs = timeElapsedMs,
                intervalNumber = intervalNumber,
                scriptIsSprinting = scriptIsSprinting,
                intervalWorkTimeGoal = intervalWorkTimeGoal,
                intervalWorkDistanceGoal = intervalWorkDistanceGoal,
                intervalRestTimeGoal = intervalRestTimeGoal,
                numberOfIntervals = numberOfIntervals,
                isWorkoutStopped = isWorkoutStopped,
                isWorkoutSwitching = isWorkoutSwitching,
            )
        )

        navigationModel = spyk(MockNavigationModel())
        every { navigationModel.launchWorkoutScreen() } just Runs

        uiBaseModel = MockUIBaseModel()

        viewModel = IntervalsViewModel(
            uiBaseModel = uiBaseModel,
            workoutModel = workoutModel,
            navigationModel = navigationModel,
        )
        viewModel.start()
    }

    @Test
    fun distanceWorkout_resolvesIntervalGoalToDistance() {
        createAndStart(WorkoutId.SPIN_BIKE_INTERVALS_DISTANCE)
        assertEquals(IntervalGoal.Distance, viewModel.uiState.value.intervalGoal)
    }

    @Test
    fun timeWorkout_resolvesIntervalGoalToTime() {
        createAndStart(WorkoutId.SPIN_BIKE_INTERVALS_TIME)
        assertEquals(IntervalGoal.Time, viewModel.uiState.value.intervalGoal)
    }

    @Test
    fun goalsLoadedBeforeOnStartWorkout_surviveTheStartEvent() {
        createAndStart()

        numberOfIntervals.emit(5)
        intervalWorkDistanceGoal.emit(0.177f)
        intervalRestTimeGoal.emit(30000f)

        onStartWorkout.emit() // confirm MockEventListener's actual trigger method name

        repeat(5) { driveOneWorkRestCycle() }
        assertTrue(viewModel.uiState.value.showTargetReachedPopup)
    }

    @Test
    fun workTimeGoal_msValue_formatsDirectlyWithNoRescaling() {
        createAndStart(WorkoutId.SPIN_BIKE_INTERVALS_TIME)
        onStartWorkout.emit()

        intervalWorkTimeGoal.emit(40000f)
        scriptIsSprinting.emit(true)
        intervalGoalRemaining.emit(40000f)

        assertEquals("0:40", viewModel.uiState.value.mainText)
    }

    @Test
    fun zeroEmission_doesNotOverwriteAlreadyLoadedGoal() {
        createAndStart(WorkoutId.SPIN_BIKE_INTERVALS_TIME)
        onStartWorkout.emit()

        intervalWorkTimeGoal.emit(40000f)
        intervalWorkTimeGoal.emit(0f)

        scriptIsSprinting.emit(true)
        intervalGoalRemaining.emit(40000f)

        assertEquals("0:40", viewModel.uiState.value.mainText)
    }

    @Test
    fun firstScriptIsSprintingFalse_beforeAnyTrue_resolvesToWork() {
        createAndStart()
        onStartWorkout.emit()

        scriptIsSprinting.emit(false)

        assertEquals(IntervalPhase.Work, viewModel.uiState.value.currentPhase)
    }

    @Test
    fun scriptIsSprintingFalse_afterARealTrue_resolvesToRest() {
        createAndStart()
        onStartWorkout.emit()

        scriptIsSprinting.emit(true)
        scriptIsSprinting.emit(false)

        assertEquals(IntervalPhase.Rest, viewModel.uiState.value.currentPhase)
    }

    @Test
    fun popup_notShown_beforeConfiguredIntervalsComplete() {
        createAndStart()
        onStartWorkout.emit()
        numberOfIntervals.emit(3)

        repeat(2) { driveOneWorkRestCycle() }

        assertFalse(viewModel.uiState.value.showTargetReachedPopup)
    }

    @Test
    fun popup_shown_exactlyWhenConfiguredIntervalsComplete() {
        createAndStart()
        onStartWorkout.emit()
        numberOfIntervals.emit(3)

        repeat(3) { driveOneWorkRestCycle() }

        assertTrue(viewModel.uiState.value.showTargetReachedPopup)
    }

    @Test
    fun onContinueWorkoutClicked_dismissesPopupWithoutStoppingWorkout() {
        createAndStart()
        onStartWorkout.emit()
        numberOfIntervals.emit(1)
        driveOneWorkRestCycle()
        assertTrue(viewModel.uiState.value.showTargetReachedPopup)

        viewModel.onContinueWorkoutClicked()

        assertFalse(viewModel.uiState.value.showTargetReachedPopup)
        verify(exactly = 0) { workoutModel.stopWorkout() }
        verify(exactly = 1) { navigationModel.launchWorkoutScreen() }
    }

    @Test
    fun onEndWorkoutClicked_dismissesPopupAndStopsWorkout() {
        createAndStart()
        onStartWorkout.emit()
        numberOfIntervals.emit(1)
        driveOneWorkRestCycle()

        viewModel.onEndWorkoutClicked()

        assertFalse(viewModel.uiState.value.showTargetReachedPopup)
        verify(exactly = 0) { navigationModel.launchWorkoutScreen() }
        verify(exactly = 1) { workoutModel.stopWorkout() }
    }

    @Test
    fun totalDistance_imperial_matchesBottomBarFormatting() {
        createAndStart(metric = false)
        onStartWorkout.emit()

        distanceKm.emit(0.048f)

        assertEquals(
            UnitFormatter.formatDistance(0.048f, false, workoutModel.distancePrecision),
            viewModel.uiState.value.totalDistanceText
        )
    }

    @Test
    fun totalDistance_metric_floorsInsteadOfRounding() {
        createAndStart(metric = true)
        onStartWorkout.emit()

        distanceKm.emit(0.0299f)

        assertEquals("0.02", viewModel.uiState.value.totalDistanceText)
    }

    @Test
    fun distanceRemaining_imperial_plusDistanceCovered_equalsTheGoal() {
        createAndStart(metric = false)
        onStartWorkout.emit()

        intervalWorkDistanceGoal.emit(5f)
        scriptIsSprinting.emit(true)
        distanceKm.emit(0.048f)

        // goal 5 km shows as 3.11 mi, 0.048 km shows as 0.02 mi
        assertEquals("0.02", viewModel.uiState.value.totalDistanceText)
        assertEquals("3.09", viewModel.uiState.value.mainText)
    }

    @Test
    fun distanceRemaining_metric_plusDistanceCovered_equalsTheGoal() {
        createAndStart(metric = true)
        onStartWorkout.emit()

        intervalWorkDistanceGoal.emit(5f)
        scriptIsSprinting.emit(true)
        distanceKm.emit(0.0299f)

        assertEquals("0.02", viewModel.uiState.value.totalDistanceText)
        assertEquals("4.98", viewModel.uiState.value.mainText)
    }

    @Test
    fun distanceRemaining_neverGoesNegative_pastTheGoal() {
        createAndStart(metric = true)
        onStartWorkout.emit()

        intervalWorkDistanceGoal.emit(1f)
        scriptIsSprinting.emit(true)
        distanceKm.emit(1.5f)

        assertEquals("0.00", viewModel.uiState.value.mainText)
    }

    @Test
    fun distanceRemaining_onlyEverChangesWhenTheBottomNumberChanges() {
        createAndStart(metric = false)
        onStartWorkout.emit()

        intervalWorkDistanceGoal.emit(5f)
        scriptIsSprinting.emit(true)

        var lastCovered = viewModel.uiState.value.totalDistanceText
        var lastRemaining = viewModel.uiState.value.mainText
        var tickedTogether = 0

        // creep forward in 5 m steps, well under one display step of either number
        repeat(200) { step ->
            distanceKm.emit(0.005f * (step + 1))
            // machine-reported remaining deliberately lags, it must not drive the dial
            intervalGoalRemaining.emit(5f - 0.004f * (step + 1))

            val covered = viewModel.uiState.value.totalDistanceText
            val remaining = viewModel.uiState.value.mainText
            assertEquals(
                "dial and bottom number must change on the same tick",
                covered != lastCovered,
                remaining != lastRemaining
            )
            if (covered != lastCovered) tickedTogether++
            lastCovered = covered
            lastRemaining = remaining
        }

        assertTrue("expected the display to have stepped at least once", tickedTogether > 0)
    }

    @Test
    fun workoutEnd_scriptKeyResets_doNotFlipTheScreenToRest() {
        createAndStart()
        onStartWorkout.emit()

        intervalWorkDistanceGoal.emit(5f)
        scriptIsSprinting.emit(true)
        timeElapsedMs.emit(20000)
        distanceKm.emit(1f)

        val phaseBeforeEnd = viewModel.uiState.value.currentPhase
        val mainTextBeforeEnd = viewModel.uiState.value.mainText

        // WorkoutControllerService sets run state STOP, then resetKeysAtWorkoutEnd() zeroes these
        isWorkoutStopped.emit(true)
        scriptIsSprinting.emit(false)
        intervalNumber.emit(0)
        intervalGoalRemaining.emit(0f)

        assertEquals(IntervalPhase.Work, phaseBeforeEnd)
        assertEquals(phaseBeforeEnd, viewModel.uiState.value.currentPhase)
        assertEquals(mainTextBeforeEnd, viewModel.uiState.value.mainText)
    }

    @Test
    fun workoutEnd_doesNotAppendAPhantomIntervalRow() {
        createAndStart()
        onStartWorkout.emit()

        intervalWorkDistanceGoal.emit(5f)
        scriptIsSprinting.emit(true)
        timeElapsedMs.emit(20000)
        distanceKm.emit(1f)

        val rowsBeforeEnd = viewModel.uiState.value.previousIntervals.size

        isWorkoutStopped.emit(true)
        scriptIsSprinting.emit(false)

        assertEquals(rowsBeforeEnd, viewModel.uiState.value.previousIntervals.size)
    }

    @Test
    fun workoutSwitching_alsoFreezesTheScreen() {
        createAndStart()
        onStartWorkout.emit()

        scriptIsSprinting.emit(true)
        timeElapsedMs.emit(20000)

        isWorkoutSwitching.emit(true)
        scriptIsSprinting.emit(false)

        assertEquals(IntervalPhase.Work, viewModel.uiState.value.currentPhase)
    }

    @Test
    fun restPhaseStillShowsDuringALiveWorkout() {
        createAndStart()
        onStartWorkout.emit()

        scriptIsSprinting.emit(true)
        timeElapsedMs.emit(20000)
        scriptIsSprinting.emit(false)

        assertEquals(IntervalPhase.Rest, viewModel.uiState.value.currentPhase)
    }

    private fun driveOneWorkRestCycle() {
        scriptIsSprinting.emit(true)
        timeElapsedMs.emit(timeElapsedMs.value + 2000)
        distanceKm.emit(distanceKm.value + 0.1f)

        scriptIsSprinting.emit(false)
        timeElapsedMs.emit(timeElapsedMs.value + 30000)
    }
}