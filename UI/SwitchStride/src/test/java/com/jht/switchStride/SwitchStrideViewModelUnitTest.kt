package com.jht.switchStride

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.jht.interfaces.models.DeviceModel
import com.jht.mock.interfaces.models.MockDeviceModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockUIServiceModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import com.jht.switchStride.model.ButtonType
import com.jht.switchStride.utils.pauseButtonColor
import com.jht.switchStride.utils.resumeButtonColor
import com.jht.switchStride.utils.runScreenReversePurple
import com.jht.switchStride.utils.stopButtonColor
import com.jht.widgets.helpers.StatInfo
import com.jht.widgets.helpers.StatType
import junitparams.JUnitParamsRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class SwitchStrideViewModelUnitTest {
    private lateinit var deviceModel: DeviceModel
    private lateinit var mockWorkoutModel: MockWorkoutModel
    private lateinit var mockUIServiceModel: MockUIServiceModel
    private lateinit var mockUIBaseModel: MockUIBaseModel
    private lateinit var viewModel: SwitchStrideViewModel
    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        mockWorkoutModel = MockWorkoutModel()
        mockUIServiceModel = MockUIServiceModel()
        mockUIBaseModel = MockUIBaseModel()
        deviceModel = MockDeviceModel()
        viewModel = SwitchStrideViewModel(
            deviceModel = deviceModel,
            workoutModel = mockWorkoutModel,
            uiServiceModel = mockUIServiceModel,
            uiBaseModel = mockUIBaseModel
        ).apply {
            create()
            start()
        }
        Dispatchers.setMain(testDispatcher)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test onPauseTapped`() {
        mockWorkoutModel.pauseWorkout()
    }

    @Test
    fun `test onResumeTapped`() {
        mockWorkoutModel.resumeWorkout()
    }

    @Test
    fun `test onStopTapped`() {
        mockWorkoutModel.stopWorkout()
    }

    @Test
    fun `test oReverseTapped`() {
        // WHEN
        val result = viewModel.uiState.value.isWorkoutPaused
        // THEN
        assertEquals(true,!result)
    }


    @Test
    fun `buttonConfig returns REVERSE config when belt is in reverse`() {
        // When
        val result = viewModel.buttonConfig(ButtonType.REVERSE)
        // Then
        assertEquals(R.drawable.ic_reverse, result.buttonIcon)
        assertEquals(R.string.reverse_belt_tc, result.buttonName)
        assertEquals(runScreenReversePurple, result.buttonColor)
    }

    @Test
    fun `buttonConfig returns STOP config when belt is in STOP`() {
        // When
        val result = viewModel.buttonConfig(ButtonType.STOP)
        // Then
        assertEquals(R.drawable.ic_stop, result.buttonIcon)
        assertEquals(R.string.stop_tc, result.buttonName)
        assertEquals(stopButtonColor, result.buttonColor)
    }

    @Test
    fun `buttonConfig returns PAUSE config when belt is in PAUSE`() {
        // When
        val result = viewModel.buttonConfig(ButtonType.PAUSE)
        // Then
        assertEquals(R.drawable.ic_pause, result.buttonIcon)
        assertEquals(R.string.pause_tc, result.buttonName)
        assertEquals(pauseButtonColor, result.buttonColor)
    }

    @Test
    fun `buttonConfig returns RESUME config when belt is in RESUME`() {
        // When
        val result = viewModel.buttonConfig(ButtonType.RESUME)
        // Then
        assertEquals(R.drawable.ic_resume, result.buttonIcon)
        assertEquals(R.string.resume_tc, result.buttonName)
        assertEquals(resumeButtonColor, result.buttonColor)
    }

    @Test
    fun `returns false when onReverse Dialog is Cancelled`() {
        // When
        viewModel.onReverseDialogCancelled()
        // Then
        assertFalse(viewModel.uiState.value.showReverseBeltDialog)
    }

    @Test
    fun `onReverseConfirmed should toggle belt and hide dialog after delay`() = runTest {
        // WHEN
        viewModel.onReverseConfirmed()
        // THEN
        assertEquals(false,viewModel.uiState.value.isBeltInReverse)
        assertFalse(viewModel.uiState.value.showReverseBeltDialog)
    }

    @Test
    fun `stat info should be created with correct types`() {
        val caloriesStat = StatInfo(StatType.CALORIES)
        val heartRateStat = StatInfo(StatType.HEART_RATE)
        val timeElapsedStat = StatInfo(StatType.TIME_ELAPSED)
        val distanceStat = StatInfo(StatType.DISTANCE)
        val paceStat = StatInfo(StatType.PACE)

        assertEquals(StatType.CALORIES, caloriesStat.type)
        assertEquals(StatType.HEART_RATE, heartRateStat.type)
        assertEquals(StatType.TIME_ELAPSED, timeElapsedStat.type)
        assertEquals(StatType.DISTANCE, distanceStat.type)
        assertEquals(StatType.PACE, paceStat.type)
    }

    @Test
    fun `controlButtonsTypes should show RESUME when workout is not running`() {
        val result = viewModel.controlButtonsTypes
        assertEquals(
            listOf(
                ButtonType.REVERSE,
                ButtonType.STOP,
                if(mockWorkoutModel.isWorkoutRunning.value){ButtonType.PAUSE}else{ButtonType.RESUME}
            ),
            result
        )
    }

    @Test
    fun `showPauseTimer should be true when last button is RESUME`() {
        mockWorkoutModel.isWorkoutRunning.value
        val result = viewModel.showPauseTimer
        assertTrue(result)
    }
}

