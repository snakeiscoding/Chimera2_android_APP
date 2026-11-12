package com.jht.progression.ui.setup.journey

import com.jht.interfaces.StringValueListener
import com.jht.interfaces.models.UserModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.progression.data.EventLabel
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.userinfo.UserInfoManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyViewModelTest {
    private lateinit var userInfoManager: UserInfoManager
    private lateinit var progressionModel: ProgressionModel
    private lateinit var viewModel: JourneyViewModel

    private fun buildUserInfoManager(): UserInfoManager {
        val uuid = mockk<StringValueListener>()
        every { uuid.value } returns "test-user"

        val userModel = mockk<UserModel>()
        every { userModel.uuid } returns uuid

        progressionModel = mockk()
        every { progressionModel.offlineFileLocation } returns null
        every { progressionModel.saveLocally } returns false
        every { progressionModel.progressionJsonString = any() } just Runs

        return UserInfoManager(userModel, progressionModel)
    }

    private fun buildViewModel(programEvents: ProgramEvents): JourneyViewModel {
        userInfoManager = buildUserInfoManager()
        userInfoManager.setProgramEvents(programEvents)
        viewModel = JourneyViewModel(userInfoManager)
        return viewModel
    }

    private fun workoutEvent(
        index: Int,
        startTime: Long,
        endTime: Long,
        completed: Boolean = false
    ): WorkoutEvent {
        return WorkoutEvent(index = index, startTime = startTime, endTime = endTime, completed = completed, label = EventLabel.WORKOUT_TEMPO)
    }

    // JourneyViewModel schedules a real, non-daemon java.util.Timer in init. It must be
    // cancelled after every test or the Gradle test JVM will hang waiting on that thread.
    @After
    fun tearDown() {
        val onCleared = JourneyViewModel::class.java.getDeclaredMethod("onCleared")
        onCleared.isAccessible = true
        onCleared.invoke(viewModel)
    }

    @Test
    fun `initial state pages events into groups of seven`() {
        val now = System.currentTimeMillis()
        val events = (0 until 10).map { i -> workoutEvent(i, now + (i + 1) * 200_000L, now + (i + 2) * 200_000L) }
        val viewModel = buildViewModel(ProgramEvents(events))

        assertEquals(2, viewModel.uiState.value.eventPages.size)
        assertEquals(7, viewModel.uiState.value.eventPages[0].size)
        assertEquals(3, viewModel.uiState.value.eventPages[1].size)
    }

    @Test
    fun `initial state selects the page containing the current event`() {
        val now = System.currentTimeMillis()
        val events = (0 until 10).map { i ->
            if (i == 8) workoutEvent(i, now - 1000L, now + 100_000L) else workoutEvent(i, now + (i + 1) * 200_000L, now + (i + 2) * 200_000L)
        }
        val viewModel = buildViewModel(ProgramEvents(events))

        assertEquals(1, viewModel.uiState.value.selectedPage)
    }

    @Test
    fun `nextFutureWorkoutIndex is -1 when a workout is currently active`() {
        val now = System.currentTimeMillis()
        val events = listOf(
            workoutEvent(0, now - 1000L, now + 100_000L), // current
            workoutEvent(1, now + 200_000L, now + 300_000L) // future
        )
        val viewModel = buildViewModel(ProgramEvents(events))

        assertEquals(-1, viewModel.uiState.value.nextFutureWorkoutIndex)
    }

    @Test
    fun `nextFutureWorkoutIndex skips completed and missed events to find the first future one`() {
        val now = System.currentTimeMillis()
        val events = listOf(
            workoutEvent(0, now - 200_000L, now - 100_000L, completed = true), // completed
            workoutEvent(1, now - 200_000L, now - 100_000L), // missed
            workoutEvent(2, now + 100_000L, now + 200_000L) // future
        )
        val viewModel = buildViewModel(ProgramEvents(events))

        assertEquals(2, viewModel.uiState.value.nextFutureWorkoutIndex)
    }

    @Test
    fun `updateSelectedPage updates the page when within bounds`() {
        val now = System.currentTimeMillis()
        val events = (0 until 10).map { i -> workoutEvent(i, now + (i + 1) * 200_000L, now + (i + 2) * 200_000L) }
        val viewModel = buildViewModel(ProgramEvents(events))

        viewModel.updateSelectedPage(1)

        assertEquals(1, viewModel.uiState.value.selectedPage)
    }

    @Test
    fun `updateSelectedPage ignores out of bounds values`() {
        val now = System.currentTimeMillis()
        val events = (0 until 10).map { i -> workoutEvent(i, now + (i + 1) * 200_000L, now + (i + 2) * 200_000L) }
        val viewModel = buildViewModel(ProgramEvents(events))
        val initialPage = viewModel.uiState.value.selectedPage

        viewModel.updateSelectedPage(-1)
        viewModel.updateSelectedPage(5)

        assertEquals(initialPage, viewModel.uiState.value.selectedPage)
    }

    @Test
    fun `resetToWorkout resets completion and persists the updated events`() {
        val now = System.currentTimeMillis()
        val events = listOf(
            workoutEvent(0, now - 300_000L, now - 200_000L, completed = true),
            workoutEvent(1, now - 200_000L, now - 100_000L, completed = true),
            workoutEvent(2, now - 100_000L, now, completed = false)
        )
        val viewModel = buildViewModel(ProgramEvents(events))

        viewModel.resetToWorkout(events[1])

        assertFalse(userInfoManager.userInfo.programEvents.events[1].completed)
        assertFalse(viewModel.uiState.value.eventPages.flatten()[1].completed)
        verify { progressionModel.progressionJsonString = any() }
    }

    @Test
    fun `deleteUserData delegates to the user info manager`() {
        userInfoManager = spyk(buildUserInfoManager())
        userInfoManager.setProgramEvents(ProgramEvents(emptyList()))
        viewModel = JourneyViewModel(userInfoManager)

        viewModel.deleteUserData()

        verify { userInfoManager.deleteUserData() }
    }

    @Test
    fun `acknowledgeGoalTimeInfoPopup marks it acknowledged and hides the popup`() {
        val now = System.currentTimeMillis()
        val events = listOf(workoutEvent(0, now - 100_000L, now + 100_000L))
        val viewModel = buildViewModel(ProgramEvents(events))
        assertTrue(viewModel.uiState.value.autoDisplayGoalTimeInfoPopup)

        viewModel.acknowledgeGoalTimeInfoPopup()

        assertFalse(viewModel.uiState.value.autoDisplayGoalTimeInfoPopup)
        assertTrue(userInfoManager.userInfo.goalTimeInfoAcknowledged)
        verify { progressionModel.progressionJsonString = any() }
    }

    @Test
    fun `autoDisplayGoalTimeInfoPopup is false before the first workout has started`() {
        val now = System.currentTimeMillis()
        val events = listOf(workoutEvent(0, now + 100_000L, now + 200_000L))
        val viewModel = buildViewModel(ProgramEvents(events))

        assertFalse(viewModel.uiState.value.autoDisplayGoalTimeInfoPopup)
    }
}
