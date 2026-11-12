package com.jht.progression.ui.setup.journey

import com.jht.enums.ProgramType
import com.jht.interfaces.StringValueListener
import com.jht.interfaces.models.UserModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.progression.data.Program
import com.jht.progression.data.getProgramOfType
import com.jht.progression.data.program.program5k.Program5k
import com.jht.progression.userinfo.UserInfoManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalSettingsViewModelTest {
    private val program: Program = getProgramOfType(ProgramType.Program5k)

    private fun buildUserInfoManager(baseline: Float, goalBaseline: Float? = null): UserInfoManager {
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
        userInfoManager.setGoalBaseline(goalBaseline)
        return userInfoManager
    }

    @Test
    fun `default state has no goal time and shows the estimated race time from the baseline`() {
        val userInfoManager = buildUserInfoManager(baseline = 8.989657f)

        val viewModel = GoalSettingsViewModel(userInfoManager, program)

        assertEquals("", viewModel.uiState.value.goalTime)
        assertEquals("00:35:01", viewModel.uiState.value.estimatedRaceTime)
        assertEquals(3, viewModel.uiState.value.maxRaceTimeReduction)
        assertFalse(viewModel.uiState.value.isValidGoalTime)
    }

    @Test
    fun `goalTime reflects a saved goal baseline within the allowed reduction`() {
        val goalTimeSeconds = 2050
        val goalBaseline = Program5k().computeBaselineFromWorkoutTime(goalTimeSeconds)
        val userInfoManager = buildUserInfoManager(baseline = 8.989657f, goalBaseline = goalBaseline)

        val viewModel = GoalSettingsViewModel(userInfoManager, program)

        assertEquals("3410", viewModel.uiState.value.goalTime)
        assertTrue(viewModel.uiState.value.isValidGoalTime)
    }

    @Test
    fun `computeGoalBaseline delegates to the workout interface`() {
        val userInfoManager = buildUserInfoManager(baseline = 8.989657f)
        val viewModel = GoalSettingsViewModel(userInfoManager, program)

        val expected = program.workoutInterface.computeBaselineFromWorkoutTime(2050)
        assertEquals(expected, viewModel.computeGoalBaseline("003410"), 0.00001f)
    }

    @Test
    fun `onGoalTimeChanged updates the goal time and revalidates it`() {
        val userInfoManager = buildUserInfoManager(baseline = 8.989657f)
        val viewModel = GoalSettingsViewModel(userInfoManager, program)

        viewModel.onGoalTimeChanged("003410")

        assertEquals("003410", viewModel.uiState.value.goalTime)
        assertTrue(viewModel.uiState.value.isValidGoalTime)
    }

    @Test
    fun `onGoalTimeChanged marks a goal slower than the estimated race time as invalid`() {
        val userInfoManager = buildUserInfoManager(baseline = 8.989657f)
        val viewModel = GoalSettingsViewModel(userInfoManager, program)

        viewModel.onGoalTimeChanged("004000")

        assertFalse(viewModel.uiState.value.isValidGoalTime)
    }

    @Test
    fun `maxRaceTimeReduction scales down with a faster estimated race time`() {
        val fastBaseline = Program5k().computeBaselineFromWorkoutTime(1200)
        val userInfoManager = buildUserInfoManager(baseline = fastBaseline)

        val viewModel = GoalSettingsViewModel(userInfoManager, program)

        assertEquals(2, viewModel.uiState.value.maxRaceTimeReduction)
    }
}
