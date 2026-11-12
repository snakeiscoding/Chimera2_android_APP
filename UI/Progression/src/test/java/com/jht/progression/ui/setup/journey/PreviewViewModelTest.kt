package com.jht.progression.ui.setup.journey

import com.jht.interfaces.StringValueListener
import com.jht.interfaces.models.UserModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.progression.R
import com.jht.progression.data.EventLabel
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.userinfo.UserInfoManager
import com.jht.unit.UnitFormatter
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewViewModelTest {
    private fun buildUserInfoManager(baseline: Float, events: List<WorkoutEvent>): UserInfoManager {
        val uuid = mockk<StringValueListener>()
        every { uuid.value } returns "test-user"

        val userModel = mockk<UserModel>()
        every { userModel.uuid } returns uuid

        val progressionModel = mockk<ProgressionModel>()
        every { progressionModel.offlineFileLocation } returns null
        every { progressionModel.saveLocally } returns false
        every { progressionModel.progressionJsonString = any() } just Runs

        val userInfoManager = UserInfoManager(userModel, progressionModel)
        userInfoManager.setBaseline(baseline)
        userInfoManager.setProgramEvents(ProgramEvents(events))
        return userInfoManager
    }

    private fun workoutEvent(
        index: Int,
        label: EventLabel = EventLabel.WORKOUT_TEMPO,
        startTime: Long,
        endTime: Long,
        completed: Boolean = false,
        totalTimeSec: Int = 0
    ): WorkoutEvent {
        return WorkoutEvent(index = index, startTime = startTime, endTime = endTime, completed = completed, label = label, totalTimeSec = totalTimeSec)
    }

    @Test
    fun `future incomplete workout shows the future preview state and title`() {
        val now = System.currentTimeMillis()
        val event = workoutEvent(0, EventLabel.WORKOUT_LONG_SLOW_DISTANCE, now + 100_000L, now + 200_000L)
        val userInfoManager = buildUserInfoManager(8f, listOf(event))

        val viewModel = PreviewViewModel(0, userInfoManager, useMetric = true)

        assertEquals(PreviewState.STATE_FUTURE, viewModel.uiState.value.previewState)
        assertEquals(R.string.long_distance_workout_tc, viewModel.uiState.value.workoutPreviewTitle)
        assertTrue(viewModel.uiState.value.intervals.isNotEmpty())
    }

    @Test
    fun `active workout shows the active preview state`() {
        val now = System.currentTimeMillis()
        val event = workoutEvent(0, EventLabel.WORKOUT_LONG_SLOW_DISTANCE, now - 1000L, now + 100_000L)
        val userInfoManager = buildUserInfoManager(8f, listOf(event))

        val viewModel = PreviewViewModel(0, userInfoManager, useMetric = true)

        assertEquals(PreviewState.STATE_ACTIVE, viewModel.uiState.value.previewState)
    }

    @Test
    fun `completed workout shows the completed preview state and actual total time`() {
        val now = System.currentTimeMillis()
        val event = workoutEvent(0, EventLabel.WORKOUT_LONG_SLOW_DISTANCE, now - 200_000L, now - 100_000L, completed = true, totalTimeSec = 1200)
        val userInfoManager = buildUserInfoManager(8f, listOf(event))

        val viewModel = PreviewViewModel(0, userInfoManager, useMetric = true)

        assertEquals(PreviewState.STATE_COMPLETED, viewModel.uiState.value.previewState)
        assertEquals(UnitFormatter.formatTimeMilliseconds(1200 * 1000), viewModel.uiState.value.totalTime)
    }

    @Test
    fun `rest day has no workout intervals and uses the rest title`() {
        val now = System.currentTimeMillis()
        val events = listOf(
            workoutEvent(0, EventLabel.WORKOUT_LONG_SLOW_DISTANCE, now + 100_000L, now + 200_000L),
            workoutEvent(1, EventLabel.REST, now + 200_000L, now + 300_000L)
        )
        val userInfoManager = buildUserInfoManager(8f, events)

        val viewModel = PreviewViewModel(1, userInfoManager, useMetric = true)

        assertTrue(viewModel.uiState.value.intervals.isEmpty())
        assertEquals(R.string.rest_uc, viewModel.uiState.value.workoutPreviewTitle)
    }

    @Test
    fun `race event is identified and uses the race title`() {
        val now = System.currentTimeMillis()
        val events = (0..55).map { i -> workoutEvent(i, EventLabel.WORKOUT_TEMPO, now + 1000L, now + 2000L) } +
            workoutEvent(56, EventLabel.RACE, now + 100_000L, Long.MAX_VALUE)
        val userInfoManager = buildUserInfoManager(8f, events)

        val viewModel = PreviewViewModel(56, userInfoManager, useMetric = true)

        assertTrue(viewModel.isRaceEvent())
        assertEquals(R.string.race_uc, viewModel.uiState.value.workoutPreviewTitle)
        assertEquals(3, viewModel.uiState.value.intervals.size)
    }

    @Test
    fun `isRaceEvent is false for a normal workout`() {
        val now = System.currentTimeMillis()
        val event = workoutEvent(0, EventLabel.WORKOUT_LONG_SLOW_DISTANCE, now + 100_000L, now + 200_000L)
        val userInfoManager = buildUserInfoManager(8f, listOf(event))

        val viewModel = PreviewViewModel(0, userInfoManager, useMetric = true)

        assertFalse(viewModel.isRaceEvent())
    }

    @Test
    fun `skipToWorkout persists progress and marks the state active`() {
        val now = System.currentTimeMillis()
        val event = workoutEvent(0, EventLabel.WORKOUT_LONG_SLOW_DISTANCE, now + 100_000L, now + 200_000L)
        val userInfoManager = buildUserInfoManager(8f, listOf(event))
        val viewModel = PreviewViewModel(0, userInfoManager, useMetric = true)

        viewModel.skipToWorkout()

        assertEquals(PreviewState.STATE_ACTIVE, viewModel.uiState.value.previewState)
    }

    @Test
    fun `startWorkout launches the workout with the expected event index`() {
        val now = System.currentTimeMillis()
        val event = workoutEvent(0, EventLabel.WORKOUT_LONG_SLOW_DISTANCE, now + 100_000L, now + 200_000L)
        val userInfoManager = buildUserInfoManager(8f, listOf(event))
        val viewModel = PreviewViewModel(0, userInfoManager, useMetric = true)

        var launchedIndex: Int? = null
        viewModel.startWorkout { _, eventIndex -> launchedIndex = eventIndex.eventIndex }

        assertEquals(0, launchedIndex)
    }
}
