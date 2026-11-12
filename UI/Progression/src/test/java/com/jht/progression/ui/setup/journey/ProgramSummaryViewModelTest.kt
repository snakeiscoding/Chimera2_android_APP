package com.jht.progression.ui.setup.journey

import com.jht.enums.ProgramType
import com.jht.interfaces.StringValueListener
import com.jht.interfaces.models.UserModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.progression.data.EventLabel
import com.jht.progression.data.Program
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.data.getProgramOfType
import com.jht.progression.data.secondsToHrMinSecString
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

class ProgramSummaryViewModelTest {
    private val program: Program = getProgramOfType(ProgramType.Program5k)

    private fun buildUserInfoManager(): UserInfoManager {
        val uuid = mockk<StringValueListener>()
        every { uuid.value } returns "test-user"

        val userModel = mockk<UserModel>()
        every { userModel.uuid } returns uuid

        val progressionModel = mockk<ProgressionModel>()
        every { progressionModel.offlineFileLocation } returns null
        every { progressionModel.saveLocally } returns false
        every { progressionModel.progressionJsonString = any() } just Runs

        return UserInfoManager(userModel, progressionModel)
    }

    private fun completedWorkoutEvent(index: Int, distanceKm: Float, calories: Float, timeSec: Int): WorkoutEvent {
        return WorkoutEvent(
            index = index,
            startTime = 0L,
            endTime = 0L,
            completed = true,
            label = EventLabel.WORKOUT_TEMPO,
            totalDistanceKm = distanceKm,
            totalCalories = calories,
            totalTimeSec = timeSec
        )
    }

    @Test
    fun `default state has zero completed workouts and a placeholder goal time when there are no events`() {
        val userInfoManager = buildUserInfoManager()
        val viewModel = ProgramSummaryViewModel(userInfoManager, program, useMetric = true)

        val firstPage = viewModel.uiState.value.firstPageData
        assertEquals(UnitFormatter.formatInt(0), firstPage[3].value)
        assertEquals("--", viewModel.uiState.value.goalTime)
        assertFalse(viewModel.uiState.value.isEndOfProgram)
    }

    @Test
    fun `isEndOfProgram parameter overrides the events-based determination`() {
        val userInfoManager = buildUserInfoManager()

        val forcedTrue = ProgramSummaryViewModel(userInfoManager, program, useMetric = true, isEndOfProgram = true)
        assertTrue(forcedTrue.uiState.value.isEndOfProgram)

        val completedEvent = completedWorkoutEvent(0, distanceKm = 1f, calories = 50f, timeSec = 300)
        userInfoManager.setProgramEvents(ProgramEvents(listOf(completedEvent)))
        val forcedFalse = ProgramSummaryViewModel(userInfoManager, program, useMetric = true, isEndOfProgram = false)
        assertFalse(forcedFalse.uiState.value.isEndOfProgram)
    }

    @Test
    fun `completed workouts accumulate distance calories time and count`() {
        val userInfoManager = buildUserInfoManager()
        val events = listOf(
            completedWorkoutEvent(0, distanceKm = 1.5f, calories = 100f, timeSec = 600),
            completedWorkoutEvent(1, distanceKm = 1.5f, calories = 100f, timeSec = 600)
        )
        userInfoManager.setProgramEvents(ProgramEvents(events))

        val viewModel = ProgramSummaryViewModel(userInfoManager, program, useMetric = true)
        val firstPage = viewModel.uiState.value.firstPageData

        assertEquals(secondsToHrMinSecString(1200), firstPage[0].value)
        assertEquals(UnitFormatter.formatDistance(3.0f, true), firstPage[1].value)
        assertEquals(UnitFormatter.formatCalories(200f), firstPage[2].value)
        assertEquals(UnitFormatter.formatInt(2), firstPage[3].value)
    }

    @Test
    fun `goalTime shows a placeholder when no goal baseline is set`() {
        val userInfoManager = buildUserInfoManager()
        val viewModel = ProgramSummaryViewModel(userInfoManager, program, useMetric = true)

        assertEquals("--", viewModel.uiState.value.goalTime)
        assertFalse(viewModel.uiState.value.goalTimeAdjusted)
    }

    @Test
    fun `goalTime shows a formatted time and is flagged adjusted when it differs from the backup`() {
        val userInfoManager = buildUserInfoManager()
        userInfoManager.setGoalBaseline(7f)
        val expectedGoalTimeSeconds = program.workoutInterface.getRaceMetrics(7f).raceTimeSeconds

        val viewModel = ProgramSummaryViewModel(userInfoManager, program, useMetric = true)

        assertEquals(secondsToHrMinSecString(expectedGoalTimeSeconds), viewModel.uiState.value.goalTime)
        assertTrue(viewModel.uiState.value.goalTimeAdjusted)
    }

    @Test
    fun `confirmGoalTimeAdjustment true backs up the current goal baseline`() {
        val userInfoManager = buildUserInfoManager()
        userInfoManager.setGoalBaseline(7f)
        val viewModel = ProgramSummaryViewModel(userInfoManager, program, useMetric = true)

        viewModel.confirmGoalTimeAdjustment(true)

        assertEquals(7f, userInfoManager.userInfo.backupGoalBaseline)
        assertFalse(viewModel.uiState.value.goalTimeAdjusted)
    }

    @Test
    fun `confirmGoalTimeAdjustment false reverts to the backed up goal and disables auto adjust`() {
        val userInfoManager = buildUserInfoManager()
        userInfoManager.setGoalBaseline(7f)
        userInfoManager.setBackupGoalBaseline(5f)
        userInfoManager.setAutoAdjustGoalBaseline(true)
        val viewModel = ProgramSummaryViewModel(userInfoManager, program, useMetric = true)

        viewModel.confirmGoalTimeAdjustment(false)

        assertEquals(5f, userInfoManager.userInfo.goalBaselineKph)
        assertFalse(userInfoManager.userInfo.autoAdjustGoalBaseline)
        assertFalse(viewModel.uiState.value.goalTimeAdjusted)
    }
}
