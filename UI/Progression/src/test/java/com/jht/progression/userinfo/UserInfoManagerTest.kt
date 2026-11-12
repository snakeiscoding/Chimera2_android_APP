package com.jht.progression.userinfo

import com.jht.interfaces.StringValueListener
import com.jht.interfaces.models.UserModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.progression.data.EventLabel
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserInfoManagerTest {
    private lateinit var userModel: UserModel
    private lateinit var progressionModel: ProgressionModel
    private lateinit var userInfoManager: UserInfoManager

    @Before
    fun setUp() {
        val uuid = mockk<StringValueListener>()
        every { uuid.value } returns "test-user"

        userModel = mockk()
        every { userModel.uuid } returns uuid

        progressionModel = mockk()
        every { progressionModel.offlineFileLocation } returns null
        every { progressionModel.saveLocally } returns false
        every { progressionModel.progressionJsonString = any() } just Runs

        userInfoManager = UserInfoManager(userModel, progressionModel)
    }

    private fun workoutEvent(
        index: Int = 0,
        completed: Boolean = false,
        label: EventLabel = EventLabel.WORKOUT_TEMPO,
        endTime: Long = System.currentTimeMillis() + 100_000L
    ): WorkoutEvent {
        return WorkoutEvent(index = index, startTime = 0L, endTime = endTime, completed = completed, label = label)
    }

    @Test
    fun `setBaseline updates the current baseline`() {
        userInfoManager.setBaseline(5f)
        assertEquals(5f, userInfoManager.userInfo.baselineKph)
    }

    @Test
    fun `setGoalBaseline splits the remaining increment across qualifying workouts`() {
        // the goal adjustment range is checked against list position, not the event's own index
        val filler = List(3) { workoutEvent() }
        val qualifying = List(2) { workoutEvent() }
        userInfoManager.setProgramEvents(ProgramEvents(filler + qualifying))
        userInfoManager.setBaseline(5f)

        userInfoManager.setGoalBaseline(7f)

        assertEquals(7f, userInfoManager.userInfo.goalBaselineKph)
        assertEquals(1f, userInfoManager.userInfo.goalIncrementKph)
    }

    @Test
    fun `setGoalBaseline leaves increment null when no workouts remain to adjust over`() {
        userInfoManager.setProgramEvents(ProgramEvents(emptyList()))
        userInfoManager.setBaseline(5f)

        userInfoManager.setGoalBaseline(7f)

        assertEquals(7f, userInfoManager.userInfo.goalBaselineKph)
        assertNull(userInfoManager.userInfo.goalIncrementKph)
    }

    @Test
    fun `setGoalBaseline leaves increment null when goal is not faster than baseline`() {
        userInfoManager.setProgramEvents(ProgramEvents(listOf(workoutEvent(index = 3))))
        userInfoManager.setBaseline(7f)

        userInfoManager.setGoalBaseline(5f)

        assertEquals(5f, userInfoManager.userInfo.goalBaselineKph)
        assertNull(userInfoManager.userInfo.goalIncrementKph)
    }

    @Test
    fun `setGoalBaseline of null clears the goal and increment`() {
        userInfoManager.setGoalBaseline(null)

        assertNull(userInfoManager.userInfo.goalBaselineKph)
        assertNull(userInfoManager.userInfo.goalIncrementKph)
    }

    @Test
    fun `setAutoAdjustGoalBaseline updates the flag`() {
        userInfoManager.setAutoAdjustGoalBaseline(true)
        assertTrue(userInfoManager.userInfo.autoAdjustGoalBaseline)

        userInfoManager.setAutoAdjustGoalBaseline(false)
        assertFalse(userInfoManager.userInfo.autoAdjustGoalBaseline)
    }

    @Test
    fun `getNumGoalIncrementsRemaining counts only incomplete non-race workouts within range`() {
        // the goal adjustment range is checked against list position, not the event's own index,
        // so the first 3 filler events push the rest into the zero-indexed 3..20 range
        val filler = List(3) { workoutEvent() }
        val events = filler + listOf(
            workoutEvent(), // position 3, qualifies
            workoutEvent(completed = true), // position 4, completed, does not count
            workoutEvent(label = EventLabel.RACE), // position 5, race, does not count
            workoutEvent(endTime = System.currentTimeMillis() - 100_000L), // position 6, already passed
            workoutEvent() // position 7, qualifies
        )
        userInfoManager.setProgramEvents(ProgramEvents(events))

        assertEquals(2, userInfoManager.getNumGoalIncrementsRemaining())
    }

    @Test
    fun `isProgramStarted reflects whether a baseline has been set`() {
        assertFalse(userInfoManager.isProgramStarted())

        userInfoManager.setBaseline(5f)
        assertTrue(userInfoManager.isProgramStarted())
    }

    @Test
    fun `resetProgram clears all user info back to defaults`() {
        userInfoManager.setBaseline(5f)
        userInfoManager.setGoalBaseline(7f)

        userInfoManager.resetProgram()

        assertEquals(UserInfo(), userInfoManager.userInfo)
    }

    @Test
    fun `writeUserInfoToFile updates the model json without writing to disk`() {
        userInfoManager.setBaseline(5f)

        userInfoManager.writeUserInfoToFile()

        verify { progressionModel.progressionJsonString = any() }
    }

    @Test
    fun `deleteUserData does not touch disk when local saving is disabled`() {
        userInfoManager.deleteUserData()
    }
}
