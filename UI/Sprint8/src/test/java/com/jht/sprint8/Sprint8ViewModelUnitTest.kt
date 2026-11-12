package com.jht.sprint8

import android.content.Context
import com.jht.enums.WorkoutStage
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutCatalogModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import com.jht.sprint8.assessment.data.datastore.AssessmentDataStoreManager
import com.jht.sprint8.model.Sprint8UiState
import com.jht.sprint8.sprint8classic.components.SprintStateType
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
class Sprint8ViewModelUnitTest {
    private lateinit var uiBaseModel: MockUIBaseModel
    private val workoutModel = spyk(MockWorkoutModel())
    private val uiServiceModel = mockk<UIServiceModel>(relaxed = true)
    private val navigationModel = mockk<NavigationModel>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val catalogModel = mockk<WorkoutCatalogModel>(relaxed = true)
    private val systemModel = mockk<SystemModel>(relaxed = true)
    private val dataStoreManager = mockk<AssessmentDataStoreManager>(relaxed = true)
    private lateinit var viewModel: Sprint8ViewModel
    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        uiBaseModel = MockUIBaseModel()
        viewModel = Sprint8ViewModel(
            workoutModel = workoutModel,
            uiBaseModel = uiBaseModel,
            uiServiceModel = uiServiceModel,
            navigationModel = navigationModel,
            context = context,
            catalogModel = catalogModel,
            systemModel = systemModel,
            dataStoreManager = dataStoreManager
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fullScreen should trigger toggleFullscreen`() {
        // When
        viewModel.fullScreen()
        // Then
        verify { uiServiceModel.toggleFullscreen() }
    }

    @Test
    fun `onHome should navigate to splash`() {
        // When
        viewModel.onHome()
        //Then
        verify { navigationModel.launchSplashScreen() }
    }

    @Test
    fun `skipInterval should call action when workout is running`() {
        // Given
        setRunningState(true)
        // When
        viewModel.skipInterval()
        // Then
        verify { workoutModel.actionSkipInterval() }
    }

    @Test
    fun `skipInterval should NOT call action when workout is not running`() {
        // Given
        setRunningState(false)
        // When
        viewModel.skipInterval()
        // Then
        verify(exactly = 0) { workoutModel.actionSkipInterval() }
    }

    @Test
    fun `isCadenceVisible should reflect cadenceDisplayAllowed on start`() {
        // Given
        every { workoutModel.cadenceDisplayAllowed } returns true
        // When
        viewModel.start()
        // Then
        assertTrue(viewModel.isCadenceVisible.value)
    }

    @Test
    fun `isCadenceVisible should be false when cadence is not supported`() {
        // Given
        every { workoutModel.cadenceDisplayAllowed } returns false
        // When
        viewModel.start()
        // Then
        assertFalse(viewModel.isCadenceVisible.value)
    }

    @Test
    fun `spm should update from workoutModel after start`() {
        // Given
        viewModel.start()
        // When
        workoutModel.spm.emit(162)
        // Then
        assertEquals(162, viewModel.spm.intValue)
    }

    @Test
    fun `spm should ignore sentinel -1 value`() {
        // Given
        viewModel.start()
        workoutModel.spm.emit(162)
        // When
        workoutModel.spm.emit(-1)
        // Then
        assertEquals(162, viewModel.spm.intValue)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `onReset should reset uiState to default`() {
        // Given
        val field = viewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val state = field.get(viewModel) as MutableStateFlow<Sprint8UiState>
        state.value = state.value.copy(isRunning = true)
        // When
        viewModel.onReset()
        val result = viewModel.uiState.value
        // Then (check key fields only)
        assertFalse(result.isRunning)
        assertFalse(result.isSprinting)
        assertEquals(0, result.heartRate)
        assertEquals(0, result.rpm)
    }

    @Test
    fun `onPlayPauseButtonClick should pause when running`() {
        // Given
        setRunningState(true)
        // When
        viewModel.onPlayPauseButtonClick()
        // Then
        verify { workoutModel.pauseWorkout() }
    }

    @Test
    fun `onPlayPauseButtonClick should resume when not running`() {
        // Given
        setRunningState(false)
        // When
        viewModel.onPlayPauseButtonClick()
        // Then
        verify { workoutModel.resumeWorkout() }
    }

    @Test
    fun `progressStyle should be WarmUp when stage is WARM_UP`() {
        setWorkoutStage(WorkoutStage.WARM_UP)

        val result = viewModel.uiState.value.progressStyle

        assertEquals(SprintStateType.WarmUp, result)
    }

    @Test
    fun `progressStyle should be Recovery when stage is WORKOUT`() {
        setWorkoutStage(WorkoutStage.WORKOUT)

        val result = viewModel.uiState.value.progressStyle

        assertEquals(SprintStateType.Recovery, result)
    }

    @Test
    fun `progressStyle should be Cooldown when stage is COOL_DOWN`() {
        setWorkoutStage(WorkoutStage.COOL_DOWN)

        val result = viewModel.uiState.value.progressStyle

        assertEquals(SprintStateType.Cooldown, result)
    }

    @Test
    fun `remainingTime should exist and be initialized`() {
        val time = viewModel.uiState.value.remainingTime

        assertTrue(time >= 0)
    }

    // -------------------- Sprint8 Assessment Unit Test --------------------//

    @Test
    fun `should show all buttons when level is between 1 and 20`() {
        // Given
        setWorkoutLevel(10)
        // When
        val result = viewModel.assessmentDialogButtonConfig.value
        // Then
        assertEquals(3, result.size)
    }

    @Test
    fun `should not show level down when level is 1`() {
        // Given
        setWorkoutLevel(1)
        // When
        val result = viewModel.assessmentDialogButtonConfig.value
        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `should not show level up when level is max`() {
        // Given
        setWorkoutLevel(20)
        // When
        val result = viewModel.assessmentDialogButtonConfig.value
        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `stay here button should trigger selection`() {
        // Given
        every { workoutModel.workoutLevel.value } returns 5
        val buttons = viewModel.assessmentDialogButtonConfig.value
        val stayButton = buttons.last()
        // When
        stayButton.onButtonClick.invoke()
        // Then
        verify { workoutModel.workoutLevel.value }
    }

    @Test
    fun `level up button should update level correctly`() {
        // Given
        setWorkoutLevel(10)
        every { workoutModel.workoutLevel.value } returns 10
        val buttons = viewModel.assessmentDialogButtonConfig.value
        val levelUpButton = buttons.first() // LEVEL_UP
        // When
        levelUpButton.onButtonClick.invoke()
        // Then
        verify {
            workoutModel.hotLevelUpdateSprint8(10)
        }
    }

    @Test
    fun `level down button should decrease level correctly`() {
        // Given
        setWorkoutLevel(10)
        every { workoutModel.workoutLevel.value } returns 10
        val buttons = viewModel.assessmentDialogButtonConfig.value
        val levelDownButton = buttons[1] // LEVEL_DOWN
        // When
        levelDownButton.onButtonClick.invoke()
        // Then
        verify {
            workoutModel.hotLevelUpdateSprint8(8) // 10 - 2
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setRunningState(isRunning: Boolean) {
        val field = viewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val state = field.get(viewModel) as MutableStateFlow<Sprint8UiState>
        state.value = state.value.copy(isRunning = isRunning)
    }

    @Suppress("SameParameterValue")
    private fun setCurrentIncline(value: Float) {
        val field = viewModel::class.java.getDeclaredField("currentIncline\$delegate")
        field.isAccessible = true
        val lazy = field.get(viewModel) as Lazy<*>
        val state = lazy.value as androidx.compose.runtime.MutableFloatState
        state.floatValue = value
    }

    @Suppress("SameParameterValue")
    private fun setCurrentResistance(value: Int) {
        val field = viewModel::class.java.getDeclaredField("currentResistance\$delegate")
        field.isAccessible = true
        val lazy = field.get(viewModel) as Lazy<*>
        val state = lazy.value as androidx.compose.runtime.MutableIntState
        state.intValue = value
    }

    private fun setWorkoutLevel(level: Int) {
        val field = viewModel::class.java.getDeclaredField("workoutLevelState\$delegate")
        field.isAccessible = true
        val lazy = field.get(viewModel) as Lazy<*>
        val state = lazy.value as androidx.compose.runtime.MutableIntState
        state.intValue = level
    }

    @Suppress("UNCHECKED_CAST")
    private fun setWorkoutStage(stage: WorkoutStage) {
        val field = viewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val state = field.get(viewModel) as MutableStateFlow<Sprint8UiState>
        state.value = state.value.copy(workoutStage = stage)
        val method =
            viewModel::class.java.getDeclaredMethod("getProgressBarStyle", WorkoutStage::class.java)
        method.isAccessible = true
        val result = method.invoke(viewModel, stage) as SprintStateType
        state.value = state.value.copy(progressStyle = result)
    }
}
