package com.jht.progression.ui.summary

import android.util.Log
import com.jht.data.EventIndex
import com.jht.enums.ProgramType
import com.jht.enums.SummaryStage
import com.jht.enums.SummaryStat
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.DataVariableListener
import com.jht.interfaces.StringValueListener
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UserModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.interfaces.models.WorkoutSummaryModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.progression.R
import com.jht.progression.data.EventLabel
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.userinfo.UserInfo
import com.jht.progression.userinfo.getUserProgramString
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SummaryViewModelTest {
    private lateinit var progressionModel: ProgressionModel
    private lateinit var navigationModel: NavigationModel

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
        userInfo: UserInfo,
        eventIndex: EventIndex,
        workoutStats: Map<Pair<SummaryStat, SummaryStage>, Float> = emptyMap()
    ): SummaryViewModel {
        val summaryModel = mockk<WorkoutSummaryModel>(relaxed = true)
        workoutStats.forEach { (key, value) -> every { summaryModel.getWorkoutStat(key.first, key.second) } returns value }

        val uuid = mockk<StringValueListener>()
        every { uuid.value } returns "test-user"
        val userModel = mockk<UserModel>()
        every { userModel.uuid } returns uuid

        val eventIndexListener = mockk<DataValueListener<EventIndex>>()
        every { eventIndexListener.value } returns eventIndex

        progressionModel = mockk()
        every { progressionModel.offlineFileLocation } returns null
        every { progressionModel.saveLocally } returns false
        every { progressionModel.progressionJsonString } returns getUserProgramString(userInfo)
        every { progressionModel.progressionJsonString = any() } just Runs
        every { progressionModel.eventIndex } returns eventIndexListener

        navigationModel = mockk(relaxed = true)

        val isMetricListener = mockk<DataVariableListener<Boolean>>()
        every { isMetricListener.value } returns true
        val workoutModel = mockk<WorkoutModel>()
        every { workoutModel.isMetric } returns isMetricListener

        return SummaryViewModel(summaryModel, progressionModel, userModel, navigationModel, workoutModel)
    }

    private fun workoutEvent(index: Int, completed: Boolean = false): WorkoutEvent {
        return WorkoutEvent(index = index, startTime = 0L, endTime = 0L, completed = completed, label = EventLabel.WORKOUT_TEMPO)
    }

    @Test
    fun `completed assessment shows the congratulations screen and saves the baseline`() {
        val userInfo = UserInfo(programType = ProgramType.Program5k)
        val viewModel = buildViewModel(
            userInfo = userInfo,
            eventIndex = EventIndex(ProgramType.Program5k, -1),
            workoutStats = mapOf(
                (SummaryStat.TIME_ELAPSED to SummaryStage.WORKOUT) to 900f,
                (SummaryStat.AVERAGE_SPEED to SummaryStage.WORKOUT) to 10f
            )
        )

        assertTrue(viewModel.isAssessment())
        assertFalse(viewModel.assessmentIncomplete)
        assertEquals(R.string.congratulations_uc, viewModel.titleTextId)
        verify { progressionModel.progressionJsonString = any() }
    }

    @Test
    fun `incomplete assessment shows the incomplete screen`() {
        val userInfo = UserInfo(programType = ProgramType.Program5k)
        val viewModel = buildViewModel(
            userInfo = userInfo,
            eventIndex = EventIndex(ProgramType.Program5k, -1),
            workoutStats = mapOf((SummaryStat.AVERAGE_SPEED to SummaryStage.WORKOUT) to 0f)
        )

        assertTrue(viewModel.assessmentIncomplete)
        assertEquals(R.string.assessment_incomplete_uc, viewModel.titleTextId)
        assertEquals(R.string.return_assessment_setup_text1_pg, viewModel.subTextId)
    }

    @Test
    fun `completed workout shows the workout complete screen with full stats`() {
        val userInfo = UserInfo(
            baselineKph = 8f,
            programType = ProgramType.Program5k,
            programEvents = ProgramEvents(listOf(workoutEvent(0)))
        )
        val viewModel = buildViewModel(userInfo = userInfo, eventIndex = EventIndex(ProgramType.Program5k, 0))

        assertFalse(viewModel.isAssessment())
        assertFalse(viewModel.assessmentIncomplete)
        assertEquals(R.string.workout_complete_exclamation_uc, viewModel.titleTextId)
        assertEquals(0, viewModel.subTextId)
        assertEquals(5, viewModel.stats.size)
        verify { progressionModel.progressionJsonString = any() }
    }

    @Test
    fun `isProgramCompleted is true once the final event index is reached`() {
        val userInfo = UserInfo(
            baselineKph = 8f,
            programType = ProgramType.Program5k,
            programEvents = ProgramEvents(listOf(workoutEvent(0), workoutEvent(1), workoutEvent(2)))
        )
        val viewModel = buildViewModel(userInfo = userInfo, eventIndex = EventIndex(ProgramType.Program5k, 2))

        assertTrue(viewModel.isProgramCompleted())
    }

    @Test
    fun `isProgramCompleted is false before the final event index`() {
        val userInfo = UserInfo(
            baselineKph = 8f,
            programType = ProgramType.Program5k,
            programEvents = ProgramEvents(listOf(workoutEvent(0), workoutEvent(1), workoutEvent(2)))
        )
        val viewModel = buildViewModel(userInfo = userInfo, eventIndex = EventIndex(ProgramType.Program5k, 0))

        assertFalse(viewModel.isProgramCompleted())
    }

    @Test
    fun `completing a workout does not crash when programEvents is out of sync with eventIndex`() {
        // A session reset can clear progression data but not eventIndex, leaving it past the end
        val userInfo = UserInfo(baselineKph = 8f, programType = ProgramType.Program5k, programEvents = ProgramEvents(listOf()))
        val viewModel = buildViewModel(userInfo = userInfo, eventIndex = EventIndex(ProgramType.Program5k, 4))

        assertFalse(viewModel.isAssessment())
        assertEquals(R.string.workout_complete_exclamation_uc, viewModel.titleTextId)
        // Persisting here would overwrite real progress with an empty program
        verify(exactly = 0) { progressionModel.progressionJsonString = any() }
    }

    @Test
    fun `navigateToSplashScreen delegates to the navigation model`() {
        val userInfo = UserInfo(programType = ProgramType.Program5k)
        val viewModel = buildViewModel(
            userInfo = userInfo,
            eventIndex = EventIndex(ProgramType.Program5k, -1),
            workoutStats = mapOf((SummaryStat.AVERAGE_SPEED to SummaryStage.WORKOUT) to 0f)
        )

        viewModel.navigateToSplashScreen()

        verify { navigationModel.launchSplashScreen() }
    }
}
