package com.jht.progression.ui.setup.assessment

import app.cash.turbine.test
import com.jht.enums.ProgramType
import com.jht.progression.data.IBaseline
import com.jht.progression.data.IWorkoutInterface
import com.jht.progression.data.Program
import com.jht.progression.ui.MmColors
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.pow

@RunWith(JUnitParamsRunner::class)
class ManualTimeEntryViewModelTest {
    @MockK private lateinit var workoutInterface: IWorkoutInterface
    @MockK private lateinit var baselineInterface: IBaseline
    private lateinit var program: Program
    private lateinit var viewModel: ManualTimeEntryViewModel

    /*
        This method initializes any needed variables or functionality before each unit test is ran.
     */
    @Before
    fun setUp() {
        // Initialize any variables annotated with @MockK
        MockKAnnotations.init(this)

        // Mock calls to workoutInterface.computeBaselineFromWorkoutTime
        every { workoutInterface.computeBaselineFromWorkoutTime(any()) } answers { computeMockWorkoutBaseline(firstArg<Int>()) }
        every { workoutInterface.minRaceTimeInSeconds } answers { 16 * 60 }
        every { workoutInterface.maxRaceTimeInSeconds } answers { 5 * 3600 }

        // ManualTimeEntryViewModel is system under test and is dependent upon program, hence we mock it here.
        program = Program(
            programType = ProgramType.Program5k,
            nameId = 0,
            workoutInterface = workoutInterface,
            baselineInterface = baselineInterface,
            color = MmColors.futuristicBlue
        )

        // Initialize view model for each test case
        viewModel = ManualTimeEntryViewModel(program)
    }


    @Test
    fun `test default uiState`() = runTest {
        viewModel.uiState.test {
            val defaultUiState = awaitItem()
            assertEquals("Manual time entry should be empty by default" ,"", defaultUiState.raceTime)
            assertEquals("Manual time entry should be invalid by default", false, defaultUiState.isValidRaceTime)
            assertEquals("Manual time entry should provide min race time", workoutInterface.minRaceTimeInSeconds, defaultUiState.minRaceTimeInSeconds)
            assertEquals("Manual time entry should provide max race time", workoutInterface.maxRaceTimeInSeconds, defaultUiState.maxRaceTimeInSeconds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @Parameters(
        "5030, 5030, true",
        "500, 500, false",
        "50000, 50000, true",
        "50001, 50001, false",
        "1600, 1600, true",
        "1559, 1559, false",
        "123456, 123456, false"
    )
    fun `test onRaceTimeChanged`(newRaceTime: String, expectedRaceTimeState: String, expectedIsValidState: Boolean) = runTest {
        viewModel.uiState.test {
            val initialUiState = awaitItem()
            viewModel.onRaceTimeChanged(newRaceTime)
            val updatedUiState = awaitItem()
            assertEquals("Manual time entry state should be updated", true, initialUiState != updatedUiState)
            assertEquals("Manual time entry should contain the updated race time", expectedRaceTimeState, updatedUiState.raceTime)
            assertEquals("Manual time entry should appear as valid", expectedIsValidState, updatedUiState.isValidRaceTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @Parameters(
        "5030, 3030",
        "50000, 18000",
        "1600, 960",
        "30, 30",
        "123456, 45296"
    )
    fun `test computeBaseline`(raceTime: String, raceTimeSeconds: Int) = runTest {
        val expectedBaseline = computeMockWorkoutBaseline(raceTimeSeconds)
        assertEquals("Unexpected baseline from manual time entry", expectedBaseline, viewModel.computeBaseline(raceTime))
    }

    /*
        Different from 5k workout baseline computation to ensure that view model is using the
        workout interface to calculate baseline. Helps verify ManualTimeEntryViewModel remains
        decoupled from the 5k workout program.
     */
    private fun computeMockWorkoutBaseline(totalSeconds: Int): Float {
        return (4*((4956.1562/totalSeconds).pow((1/1.0604)))).toFloat()
    }
}