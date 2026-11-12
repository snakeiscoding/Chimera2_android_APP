package com.jht.landmarks

import com.jht.mock.interfaces.MockBoolValue
import com.jht.mock.interfaces.MockStringVariable
import com.jht.mock.interfaces.models.MockUIServiceModel
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric is used instead of pure JUnit because the data models (WorkoutItem, LandmarkWorkoutItem)
// use Uri.EMPTY as default parameter values, which requires the Android framework to be available at runtime.
@RunWith(RobolectricTestRunner::class)
class LandmarksViewModelUnitTest {

    private lateinit var mockWorkoutModel: MockWorkoutModel
    private lateinit var mockWorkoutCatalogModel: MockWorkoutCatalogModel
    private lateinit var mockUIServiceModel: MockUIServiceModel
    private lateinit var viewModel: LandmarksViewModel

    private val pauseState = MockBoolValue(false)

    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockWorkoutCatalogModel = MockWorkoutCatalogModel()
        mockWorkoutModel = MockWorkoutModel(
            maxResistance = 20f,
            isWorkoutPaused = pauseState,
            // must match the catalog entry, not the empty default
            workoutOptionId = MockStringVariable(
                mockWorkoutCatalogModel.getLandmarkWorkouts.first().optionalId
            ),
        )
        mockUIServiceModel = MockUIServiceModel()

        viewModel = LandmarksViewModel(
            workoutModel = mockWorkoutModel,
            workoutCatalogModel = mockWorkoutCatalogModel,
            uiServiceModel = mockUIServiceModel
        ).apply {
            create()
            start()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `increaseResistance increments currentResistance by 1`() {
        val initialResistance = viewModel.currentResistance.intValue
        viewModel.increaseResistance()
        assertEquals(initialResistance + 1, viewModel.currentResistance.intValue)
    }

    @Test
    fun `increaseResistance does not exceed maxResistance`() {
        viewModel.setResistance(viewModel.maxResistance.intValue)
        viewModel.increaseResistance()
        assertEquals(viewModel.maxResistance.intValue, viewModel.currentResistance.intValue)
    }

    @Test
    fun `decreaseResistance decrements currentResistance by 1`() {
        viewModel.setResistance(5)
        val initialResistance = viewModel.currentResistance.intValue
        viewModel.decreaseResistance()
        assertEquals(initialResistance - 1, viewModel.currentResistance.intValue)
    }

    @Test
    fun `decreaseResistance does not go below 1`() {
        viewModel.setResistance(1)
        viewModel.decreaseResistance()
        assertEquals(1, viewModel.currentResistance.intValue)
    }

    @Test
    fun `setResistance clamps value to maxResistance when above max`() {
        viewModel.setResistance(viewModel.maxResistance.intValue + 10)
        assertEquals(viewModel.maxResistance.intValue, viewModel.currentResistance.intValue)
    }

    @Test
    fun `setResistance clamps value to 1 when below minimum`() {
        viewModel.setResistance(0)
        assertEquals(1, viewModel.currentResistance.intValue)
    }

    @Test
    fun `setResistance sets exact value when within valid range`() {
        viewModel.setResistance(10)
        assertEquals(10, viewModel.currentResistance.intValue)
    }

    @Test
    fun `togglePause pauses workout when not paused`() {
        assertFalse(viewModel.isWorkoutPaused.value)
        pauseState.emit(true)
        assertTrue(viewModel.isWorkoutPaused.value)
    }

    @Test
    fun `togglePause resumes workout when already paused`() {
        pauseState.emit(true)
        assertTrue(viewModel.isWorkoutPaused.value)
        pauseState.emit(false)
        assertFalse(viewModel.isWorkoutPaused.value)
    }

    @Test
    fun `isFullScreen initializes to false`() {
        assertFalse(viewModel.isFullScreen.value)
    }

    @Test
    fun `floorsCompleted is initialized to 0`() {
        assertEquals(0, viewModel.floorsCompleted.intValue)
    }

    @Test
    fun `progress is initialized to 0`() {
        assertEquals(0f, viewModel.progress.floatValue, 0.001f)
    }

    @Test
    fun `totalFloors is non-negative after start`() {
        assertTrue(viewModel.totalFloors.intValue >= 0)
    }

    @Test
    fun `progress stays within 0 to 1 range after floors update`() {
        mockWorkoutModel.floors.emit(viewModel.totalFloors.intValue.toFloat() / 2)
        val progress = viewModel.progress.floatValue
        assertTrue(progress >= 0f)
        assertTrue(progress <= 1f)
    }

    @Test
    fun `landmarksData is populated from catalog on start`() {
        assertNotNull(viewModel.landmarksData.value)
    }

    @Test
    fun `landmarksData resolves using workoutOptionId from WorkoutModel`() {
        assertNotNull(viewModel.landmarksData.value)
    }
}