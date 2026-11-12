package com.jht.progression.ui.workout

import android.util.Log
import com.jht.data.EventIndex
import com.jht.data.MmIntervalList
import com.jht.data.ProgressionInterval
import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.enums.ProgramType
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.DataVariableListener
import com.jht.interfaces.IntValueListener
import com.jht.interfaces.models.WorkoutModel
import com.jht.progression.interfaces.ProgressionModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Listener stub that records subscribers so a test can push new values through them. */
private open class FakeValueListener<T>(override var value: T) : DataValueListener<T> {
    private val listeners = mutableListOf<(T) -> Unit>()

    override fun listen(onChange: (T) -> Unit) {
        listeners.add(onChange)
    }

    override fun listenAndTrigger(onChange: (T) -> Unit) {
        listeners.add(onChange)
        onChange(value)
    }

    fun emit(newValue: T) {
        value = newValue
        listeners.forEach { it(newValue) }
    }
}

private class FakeIntValueListener(value: Int) : FakeValueListener<Int>(value), IntValueListener

class WorkoutViewModelTest {
    private val zone1Interval = ProgressionInterval(IntervalType.ZONE_1, IntervalGoalType.TIME, 60f)
    private val zone3Interval = ProgressionInterval(IntervalType.ZONE_3, IntervalGoalType.TIME, 60f)
    private val raceInterval = ProgressionInterval(IntervalType.RACE, IntervalGoalType.TIME, 60f)

    private lateinit var intervalNumberListener: FakeIntValueListener
    private lateinit var workoutIntervalListListener: FakeValueListener<MmIntervalList>
    private lateinit var viewModel: WorkoutViewModel

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun buildViewModel(
        intervals: List<ProgressionInterval>,
        intervalNumber: Int,
        cadenceDisplayAllowed: Boolean = false,
    ): FakeIntValueListener {
        intervalNumberListener = FakeIntValueListener(intervalNumber)
        workoutIntervalListListener = FakeValueListener(MmIntervalList(intervals))

        val isMetricListener = mockk<DataVariableListener<Boolean>>(relaxed = true)
        every { isMetricListener.value } returns true

        val spmListener = FakeIntValueListener(0)

        val workoutModel = mockk<WorkoutModel>(relaxed = true)
        every { workoutModel.intervalNumber } returns intervalNumberListener
        every { workoutModel.isMetric } returns isMetricListener
        every { workoutModel.spm } returns spmListener
        every { workoutModel.cadenceDisplayAllowed } returns cadenceDisplayAllowed

        val eventIndexListener = mockk<DataValueListener<EventIndex>>(relaxed = true)
        every { eventIndexListener.value } returns EventIndex(ProgramType.Program5k, 0)

        val progressionModel = mockk<ProgressionModel>(relaxed = true)
        every { progressionModel.workoutIntervalList } returns workoutIntervalListListener
        every { progressionModel.eventIndex } returns eventIndexListener

        viewModel = WorkoutViewModel(progressionModel, workoutModel)
        return spmListener
    }

    @Test
    fun `currentInterval is clamped when a shorter interval list arrives`() {
        buildViewModel(listOf(zone1Interval, zone3Interval, raceInterval), 0)

        // 3 intervals, sitting on the last one
        intervalNumberListener.emit(2)
        assertEquals(2, viewModel.uiState.value.currentInterval)

        // A new workout launches with fewer intervals; WorkoutScreen indexes by currentInterval
        workoutIntervalListListener.emit(MmIntervalList(listOf(zone1Interval, raceInterval)))

        val state = viewModel.uiState.value
        assertEquals(1, state.currentInterval)
        assertEquals(state.intervalList.intervalList[state.currentInterval].intervalType, state.intervalType)
    }

    @Test
    fun `interval number past the end of the list is clamped`() {
        buildViewModel(listOf(zone1Interval, zone3Interval), 0)

        intervalNumberListener.emit(5)

        val state = viewModel.uiState.value
        assertEquals(1, state.currentInterval)
        assertEquals(IntervalType.ZONE_3, state.intervalType)
    }

    @Test
    fun `an empty interval list falls back to a single zone 1 interval`() {
        buildViewModel(listOf(zone1Interval, zone3Interval), 1)

        workoutIntervalListListener.emit(MmIntervalList(emptyList()))

        val state = viewModel.uiState.value
        assertEquals(1, state.intervalList.intervalList.size)
        assertEquals(0, state.currentInterval)
        assertEquals(IntervalType.ZONE_1, state.intervalType)
    }

    @Test
    fun `spm updates from workoutModel and ignores the sentinel -1 value`() {
        val spmListener = buildViewModel(listOf(zone1Interval), 0, cadenceDisplayAllowed = true)

        assertEquals(0, viewModel.uiState.value.spm)

        spmListener.emit(170)
        assertEquals(170, viewModel.uiState.value.spm)

        spmListener.emit(-1)
        assertEquals(170, viewModel.uiState.value.spm)
    }

    @Test
    fun `isCadenceVisible reflects workoutModel cadenceDisplayAllowed`() {
        buildViewModel(listOf(zone1Interval), 0, cadenceDisplayAllowed = true)
        assertEquals(true, viewModel.uiState.value.isCadenceVisible)
    }
}
