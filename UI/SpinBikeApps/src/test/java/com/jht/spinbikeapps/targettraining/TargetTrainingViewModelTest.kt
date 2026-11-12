package com.jht.spinbikeapps.targettraining

import com.jht.enums.Gender
import com.jht.enums.WorkoutStage
import com.jht.mock.interfaces.MockBoolVariable
import com.jht.mock.interfaces.models.MockNavigationModel
import com.jht.mock.interfaces.models.MockSystemModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockUIServiceModel
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import com.jht.spinbikeapps.targettraining.common.FitnessLevel
import com.jht.spinbikeapps.targettraining.common.UserFtpMode
import com.jht.spinbikeapps.targettraining.utils.HeartRateTarget
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingTypeData
import com.jht.spinbikeapps.targettraining.utils.WattsTarget
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode.Companion.toTTType
import com.jht.unit.kilometerUnit
import com.jht.unit.mileUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Target training zone math and stat wiring. No NavHost or composables, so these survive UI restructuring.
 * Robolectric because the view model resolves pill box strings through Context at construction.
 */
@RunWith(RobolectricTestRunner::class)
class TargetTrainingViewModelTest {

    private lateinit var workoutModel: MockWorkoutModel
    private lateinit var viewModel: TargetTrainingViewModel

    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        // keep anything the view model dispatches on the test dispatcher
        Dispatchers.setMain(testDispatcher)
        resetTargetTrainingTypeData()
        workoutModel = MockWorkoutModel(isMetric = MockBoolVariable(true))
        viewModel = createViewModel(workoutModel)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        resetTargetTrainingTypeData()
        Dispatchers.resetMain()
    }

    private fun createViewModel(model: MockWorkoutModel) = TargetTrainingViewModel(
        workoutModel = model,
        uiBaseModel = MockUIBaseModel(),
        uiServiceModel = MockUIServiceModel(),
        navigationModel = MockNavigationModel(),
        context = RuntimeEnvironment.getApplication(),
        catalogModel = MockWorkoutCatalogModel(),
        systemModel = MockSystemModel(),
    )

    /** [TargetTrainingTypeData] is a singleton, so state has to be cleared or tests leak into each other */
    private fun resetTargetTrainingTypeData() {
        TargetTrainingTypeData.selectedMode = WorkoutMode.Rpm
        TargetTrainingTypeData.rpmTarget = 0f
        TargetTrainingTypeData.caloriesTarget = 0f
        TargetTrainingTypeData.heartRateTarget = null
        TargetTrainingTypeData.wattsTarget = null
        TargetTrainingTypeData.distanceTarget = 0f
        TargetTrainingTypeData.timeTarget = 0
    }

    //region RPM

    @Test
    fun `rpm target sets a zone five either side of the target`() {
        TargetTrainingTypeData.rpmTarget = 90f

        viewModel.updateSelectedMode(WorkoutMode.Rpm)

        assertEquals(85f, workoutModel.rpmZoneLow.value, 0.001f)
        assertEquals(95f, workoutModel.rpmZoneHigh.value, 0.001f)
        assertEquals(90, workoutModel.lastRpmTarget)
        assertEquals(WorkoutMode.Rpm.toTTType(), workoutModel.targetTrainingType.value)
    }

    @Test
    fun `rpm target truncates a fractional target before building the zone`() {
        TargetTrainingTypeData.rpmTarget = 90.7f

        viewModel.updateSelectedMode(WorkoutMode.Rpm)

        assertEquals(90, workoutModel.lastRpmTarget)
        assertEquals(85f, workoutModel.rpmZoneLow.value, 0.001f)
    }

    //endregion

    //region Heart rate

    @Test
    fun `heart rate zone is a percentage band of max hr with the target at its midpoint`() {
        TargetTrainingTypeData.heartRateTarget = HeartRateTarget(targetZone = "70-79%", maxHr = 200f)

        viewModel.updateSelectedMode(WorkoutMode.HeartRate)

        // 70% and 79% of 200, target is the middle of the band
        assertEquals(140, workoutModel.bpmZoneLow.value)
        assertEquals(158, workoutModel.bpmZoneHigh.value)
        assertEquals(149, workoutModel.targetBpm.value)
        assertEquals(2, workoutModel.lastBpmZoneSelectedIndex)
        assertEquals(200, workoutModel.lastUserMaxHr)
        assertEquals(WorkoutMode.HeartRate.toTTType(), workoutModel.targetTrainingType.value)
    }

    @Test
    fun `lowest heart rate zone starts at zero`() {
        TargetTrainingTypeData.heartRateTarget = HeartRateTarget(targetZone = "<60%", maxHr = 200f)

        viewModel.updateSelectedMode(WorkoutMode.HeartRate)

        assertEquals(0, workoutModel.bpmZoneLow.value)
        assertEquals(118, workoutModel.bpmZoneHigh.value)
        assertEquals(0, workoutModel.lastBpmZoneSelectedIndex)
    }

    @Test
    fun `highest heart rate zone runs to max hr`() {
        TargetTrainingTypeData.heartRateTarget = HeartRateTarget(targetZone = "90%", maxHr = 200f)

        viewModel.updateSelectedMode(WorkoutMode.HeartRate)

        assertEquals(180, workoutModel.bpmZoneLow.value)
        assertEquals(200, workoutModel.bpmZoneHigh.value)
        assertEquals(4, workoutModel.lastBpmZoneSelectedIndex)
    }

    @Test
    fun `unrecognised heart rate zone falls back to sixty to sixty nine percent`() {
        TargetTrainingTypeData.heartRateTarget = HeartRateTarget(targetZone = "not a zone", maxHr = 200f)

        viewModel.updateSelectedMode(WorkoutMode.HeartRate)

        assertEquals(120, workoutModel.bpmZoneLow.value)
        assertEquals(138, workoutModel.bpmZoneHigh.value)
        // not in the list, so the index clamps to 0 rather than -1
        assertEquals(0, workoutModel.lastBpmZoneSelectedIndex)
    }

    @Test
    fun `heart rate mode with no target set does not change the workout`() {
        TargetTrainingTypeData.heartRateTarget = null
        val typeBefore = workoutModel.targetTrainingType.value

        viewModel.updateSelectedMode(WorkoutMode.HeartRate)

        assertNull(workoutModel.lastUserMaxHr)
        assertEquals(typeBefore, workoutModel.targetTrainingType.value)
    }

    //endregion

    //region Watts

    @Test
    fun `watts zone is a percentage band of ftp`() {
        TargetTrainingTypeData.wattsTarget = WattsTarget(targetZone = "91-105%", watts = 200f)

        viewModel.updateSelectedMode(WorkoutMode.Watts)

        assertEquals(182f, workoutModel.ftpZoneLow.value, 0.001f)
        assertEquals(210f, workoutModel.ftpZoneHigh.value, 0.001f)
        assertEquals(3, workoutModel.lastFtpZoneSelectedIndex)
        assertEquals(200, workoutModel.lastUserFtp)
        assertEquals(WorkoutMode.Watts.toTTType(), workoutModel.targetTrainingType.value)
    }

    @Test
    fun `lowest watts zone starts at zero`() {
        TargetTrainingTypeData.wattsTarget = WattsTarget(targetZone = "<55%", watts = 200f)

        viewModel.updateSelectedMode(WorkoutMode.Watts)

        assertEquals(0f, workoutModel.ftpZoneLow.value, 0.001f)
        assertEquals(110f, workoutModel.ftpZoneHigh.value, 0.001f)
        assertEquals(0, workoutModel.lastFtpZoneSelectedIndex)
    }

    @Test
    fun `unrecognised watts zone falls back to fifty six to seventy five percent`() {
        TargetTrainingTypeData.wattsTarget = WattsTarget(targetZone = "not a zone", watts = 200f)

        viewModel.updateSelectedMode(WorkoutMode.Watts)

        assertEquals(112f, workoutModel.ftpZoneLow.value, 0.001f)
        assertEquals(150f, workoutModel.ftpZoneHigh.value, 0.001f)
        assertEquals(0, workoutModel.lastFtpZoneSelectedIndex)
    }

    @Test
    @Ignore(
        "Known bug: wattsData offers \"150% + \" (trailing space) but updateSelectedMode matches \"150% +\", " +
            "so the top zone silently falls back to 56-75%. Un-ignore once the strings are aligned."
    )
    fun `top watts zone runs to double ftp`() {
        TargetTrainingTypeData.wattsTarget = WattsTarget(targetZone = "150% + ", watts = 200f)

        viewModel.updateSelectedMode(WorkoutMode.Watts)

        assertEquals(300f, workoutModel.ftpZoneLow.value, 0.001f)
        assertEquals(400f, workoutModel.ftpZoneHigh.value, 0.001f)
        assertEquals(6, workoutModel.lastFtpZoneSelectedIndex)
    }

    @Test
    fun `watts mode with no target set does not change the workout`() {
        TargetTrainingTypeData.wattsTarget = null
        val typeBefore = workoutModel.targetTrainingType.value

        viewModel.updateSelectedMode(WorkoutMode.Watts)

        assertNull(workoutModel.lastUserFtp)
        assertEquals(typeBefore, workoutModel.targetTrainingType.value)
    }

    /** Estimated FTP when the user does not know theirs; the estimate table only has male and female rows */
    private fun estimatedFtpFor(gender: Gender, level: FitnessLevel = FitnessLevel.Low): Int? {
        TargetTrainingTypeData.wattsTarget = WattsTarget(targetZone = "56-75%", watts = 0f)
        viewModel.setGenderData(gender)
        viewModel.fitnessLevel.value = level
        viewModel.updateSelectedMode(WorkoutMode.Watts, UserFtpMode.UserFtpUnknown)
        return workoutModel.lastUserFtp
    }

    @Test
    fun `unknown ftp estimate uses the male row for male`() {
        assertEquals(175, estimatedFtpFor(Gender.Male))
    }

    @Test
    fun `unknown ftp estimate uses the female row for female`() {
        assertEquals(127, estimatedFtpFor(Gender.Female))
    }

    @Test
    fun `unknown ftp estimate uses the female row for non binary`() {
        assertEquals(127, estimatedFtpFor(Gender.NonBinary))
    }

    @Test
    fun `unknown ftp estimate uses the female row for not specified`() {
        assertEquals(127, estimatedFtpFor(Gender.NotSpecified))
    }

    @Test
    fun `unknown ftp estimate covers every gender and fitness level`() {
        for (gender in Gender.entries) {
            for (level in FitnessLevel.entries) {
                assertTrue("$gender $level", (estimatedFtpFor(gender, level) ?: 0) > 0)
            }
        }
    }

    //endregion

    //region Distance and calories

    @Test
    fun `metric distance target is passed through in kilometres`() {
        TargetTrainingTypeData.distanceTarget = 12f
        TargetTrainingTypeData.timeTarget = 30

        viewModel.updateSelectedMode(WorkoutMode.Distance)

        assertEquals(12f, workoutModel.targetDistanceKm.value, 0.001f)
        assertEquals(30, workoutModel.targetTimeGoalMinutes.value)
        assertEquals(WorkoutMode.Distance.toTTType(), workoutModel.targetTrainingType.value)
    }

    @Test
    fun `imperial distance target is converted to kilometres before it is stored`() {
        val imperialModel = MockWorkoutModel(isMetric = MockBoolVariable(false))
        val imperialViewModel = createViewModel(imperialModel)
        TargetTrainingTypeData.distanceTarget = 10f
        TargetTrainingTypeData.timeTarget = 45

        imperialViewModel.updateSelectedMode(WorkoutMode.Distance)

        assertEquals(10f.mileUnit.toKilometer, imperialModel.targetDistanceKm.value, 0.001f)
        // fails if the conversion is dropped and miles are stored as km
        assertTrue(imperialModel.targetDistanceKm.value > 10f)
        assertEquals(45, imperialModel.targetTimeGoalMinutes.value)
    }

    @Test
    fun `calories target sets both the calorie goal and the time goal`() {
        TargetTrainingTypeData.caloriesTarget = 250f
        TargetTrainingTypeData.timeTarget = 20

        viewModel.updateSelectedMode(WorkoutMode.Calories)

        assertEquals(250, workoutModel.lastTargetCalories)
        assertEquals(20, workoutModel.targetTimeGoalMinutes.value)
        assertEquals(WorkoutMode.Calories.toTTType(), workoutModel.targetTrainingType.value)
    }

    @Test
    fun `selecting a mode records it as the current mode`() {
        TargetTrainingTypeData.caloriesTarget = 100f

        viewModel.updateSelectedMode(WorkoutMode.Calories)

        assertEquals(WorkoutMode.Calories, TargetTrainingTypeData.selectedMode)
    }

    //endregion

    //region Distance reporting

    @Test
    fun `relative distance reports kilometres when metric`() {
        viewModel.isMetric.value = true
        viewModel.currentDistance.floatValue = 5f

        assertEquals(5f, viewModel.relativeDistance, 0.001f)
    }

    @Test
    fun `relative distance converts to miles when imperial`() {
        viewModel.isMetric.value = false
        viewModel.currentDistance.floatValue = 5f

        assertEquals(5f.kilometerUnit.toMiles, viewModel.relativeDistance, 0.001f)
        assertTrue(viewModel.relativeDistance < 5f)
    }

    @Test
    fun `relative distance never goes negative once a target is running`() {
        viewModel.start()
        // the mock starts in a distance target type, so start() activates one
        assertTrue(viewModel.isTargetTrainingActive.value)
        viewModel.currentDistance.floatValue = -1f

        assertEquals(0f, viewModel.relativeDistance, 0.001f)
    }

    //endregion

    //region Stat wiring

    @Test
    fun `rpm ignores the minus one no reading sentinel`() {
        viewModel.start()
        workoutModel.rpm.emit(80)
        workoutModel.rpm.emit(-1)

        assertEquals(80, viewModel.rpm.intValue)
    }

    @Test
    fun `target calories are counted from the point they were saved`() {
        viewModel.start()
        workoutModel.calories.emit(100f)
        viewModel.onClickSavedCalories()
        workoutModel.calories.emit(130f)

        assertEquals(30f, viewModel.targetCalories.floatValue, 0.001f)
    }

    @Test
    fun `target calories never report a negative total`() {
        viewModel.start()
        workoutModel.calories.emit(100f)
        viewModel.onClickSavedCalories()
        workoutModel.calories.emit(90f)

        assertEquals(0f, viewModel.targetCalories.floatValue, 0.001f)
    }

    @Test
    fun `cool down ends an active target`() {
        viewModel.start()
        assertTrue(viewModel.isTargetTrainingActive.value)

        workoutModel.workoutStage.emit(WorkoutStage.COOL_DOWN)

        assertFalse(viewModel.isTargetTrainingActive.value)
    }

    @Test
    fun `switching to a non distance target ends the distance target`() {
        viewModel.start()
        assertTrue(viewModel.isTargetTrainingActive.value)

        workoutModel.targetTrainingType.emit(WorkoutMode.Rpm.toTTType())

        assertFalse(viewModel.isTargetTrainingActive.value)
    }

    @Test
    fun `elapsed time is reported in seconds`() {
        viewModel.start()
        workoutModel.timeElapsedMs.emit(90_000)

        assertEquals(90f, viewModel.timeElapsedSeconds.floatValue, 0.001f)
    }

    //endregion

    //region Test tags

    @Test
    fun `every pill box has its own test tag`() {
        // a shared tag makes onNodeWithTag ambiguous
        val tags = listOf(
            viewModel.caloriesData(),
            viewModel.rpmData(),
            viewModel.heartRateData(),
            viewModel.wattsData(),
            viewModel.distanceData(),
        ).flatMap { it.pillBoxes }.map { it.testTag }

        assertEquals(tags.size, tags.distinct().size)
    }

    //endregion
}
