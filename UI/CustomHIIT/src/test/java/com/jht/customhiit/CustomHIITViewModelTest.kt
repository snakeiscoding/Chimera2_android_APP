package com.jht.customhiit

import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.jht.customhiit.common.HiitUtils
import com.jht.customhiit.common.models.HIITIntervalEnum
import com.jht.customhiit.common.models.HIITWorkoutMap
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.enums.FrameType
import com.jht.interfaces.models.DeviceModel
import com.jht.mock.interfaces.MockParcelableVariable
import com.jht.mock.interfaces.models.MockDeviceModel
import com.jht.mock.interfaces.models.MockSystemModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockUIServiceModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class) // Bundle mocking needs RobolectricTestRunner
internal class CustomHIITViewModelTest {
    private lateinit var treadmillIntervalData: HIITWorkoutMap
    private lateinit var ascentIntervalData: HIITWorkoutMap
    private lateinit var mockSystemModel: MockSystemModel
    private lateinit var deviceModel: DeviceModel
    private lateinit var mockWorkoutModel: MockWorkoutModel
    private lateinit var mockUIServiceModel: MockUIServiceModel
    private lateinit var mockUIBaseModel: MockUIBaseModel
    private lateinit var viewModel: CustomHIITViewModel

    @Before
    fun setup() {
        mockWorkoutModel = MockWorkoutModel()
        mockUIServiceModel = MockUIServiceModel()
        mockUIBaseModel = MockUIBaseModel()
        mockSystemModel = MockSystemModel()
        deviceModel = MockDeviceModel()

        // Initialize with treadmill interval data:
        // EASY_INTERVAL -> speed = 0.5, incline = 1.0
        // MEDIUM_INTERVAL -> speed = 0.2, incline = 3.0
        // HARD_INTERVAL -> speed = 1.6, incline = 7.5
        treadmillIntervalData = initializeInterval(
            firstString = ConsoleWorkoutVariables.START_SPEED,
            data = listOf(0.5f to 1.0f, 0.2f to 3.0f, 1.6f to 7.5f)
        )
        //likewise initialize for ascent. (resistance, Incline%)
        ascentIntervalData = initializeInterval(
            firstString = ConsoleWorkoutVariables.START_RESISTANCE,
            data = listOf(3f to 10f, 7f to 30f, 13f to 80f)
        )
        emitHIITData(treadmillIntervalData)

        viewModel = CustomHIITViewModel(
            systemModel = mockSystemModel,
            deviceModel = deviceModel,
            workoutModel = mockWorkoutModel,
            uiServiceModel = mockUIServiceModel,
            uiBaseModel = mockUIBaseModel
        ).apply {
            create()
            start()
        }
    }

    /**
     * Test to validate the update in the interval if user is selecting any interval.
     */
    @Test
    fun `test validateIntervalUpdate updates state correctly`() {
        listOf(
            HIITIntervalEnum.EASY_INTERVAL,
            HIITIntervalEnum.MEDIUM_INTERVAL,
            HIITIntervalEnum.HARD_INTERVAL,
            HIITIntervalEnum.MEDIUM_INTERVAL,
            HIITIntervalEnum.EASY_INTERVAL,
            HIITIntervalEnum.HARD_INTERVAL,
        ).forEach { interval ->
            viewModel.updateInterval(interval)

            assertEquals(
                "ViewModel should update selectedInterval",
                interval.ordinal,
                viewModel.selectedInterval.value
            )

            assertEquals(
                "WorkoutMap should sync selectedInterval",
                interval.ordinal,
                viewModel.hiitWorkoutMapData.value.selectedInterval
            )
        }
    }


    /**
     * Test to validate that the treadmill interval data is correctly set.
     * EASY_INTERVAL should have speed = 0.5 and incline = 1.0.
     */
    @Test
    fun `test validateTreadmillIndicatorValues`() {
        val easyIntervalData = viewModel.hiitWorkoutMapData.value.intervalMap[HIITIntervalEnum.EASY_INTERVAL]
        assertEquals(0.5f, easyIntervalData?.get(ConsoleWorkoutVariables.START_SPEED)?.value)
        assertEquals(1.0f, easyIntervalData?.get(ConsoleWorkoutVariables.START_INCLINE)?.value)
    }

    /**
     * Test to validate that the Ascent interval data is correctly set.
     * EASY_INTERVAL should have resistance = 1.0 and incline = 1.0.
     */
    @Test
    fun `test validateAscentIndicatorValues`() {
        emitHIITData(ascentIntervalData)

        val easyIntervalData = viewModel.hiitWorkoutMapData.value.intervalMap[HIITIntervalEnum.EASY_INTERVAL]
        assertEquals(3f, easyIntervalData?.get(ConsoleWorkoutVariables.START_RESISTANCE)?.value)
        assertEquals(10f, easyIntervalData?.get(ConsoleWorkoutVariables.START_INCLINE)?.value)
    }

    /**
     * Test workout pause and resume behavior.
     * When paused, selectedInterval should be set to -1.
     * When resumed, it should be -1 for Treadmill.
     */
    @Test
    fun `test workout pause and resume behavior for Treadmill`() {
        verifyPauseResumeBehavior(FrameType.Treadmill, expectedAfterResume = -1)
    }

    /**
     * Test workout pause and resume behavior.
     * When paused, selectedInterval should be set to -1.
     * When resumed, it should be -1 for Climbmill.
     */
    @Test
    fun `test workout pause and resume behavior for Climbmill`() {
        verifyPauseResumeBehavior(FrameType.Climbmill, expectedAfterResume = -1)
    }

    /**
     * Test workout pause and resume behavior.
     * When paused, selectedInterval should be set to -1.
     * When resumed, it should restore the previous interval.
     */
    @Test
    fun `test workout pause and resume behavior for Ascent`() {
        // setup initializes treadmill data by default, we need to emit ascent data to test ascent behavior
        emitHIITData(ascentIntervalData)
        verifyPauseResumeBehavior(FrameType.Ascent, expectedAfterResume = HIITIntervalEnum.EASY_INTERVAL.ordinal)
    }

    private fun verifyPauseResumeBehavior(frame: FrameType, expectedAfterResume: Int) {
        viewModel.frameType.value = frame
        viewModel.selectedInterval.value = HIITIntervalEnum.EASY_INTERVAL.ordinal

        mockWorkoutModel.isWorkoutPaused.emit(true)
        assertEquals("Should deselect on pause for $frame", -1, viewModel.selectedInterval.value)

        mockWorkoutModel.isWorkoutPaused.emit(false)
        assertEquals("Incorrect resume behavior for $frame", expectedAfterResume, viewModel.selectedInterval.value)
    }

    /**
     * Test time elapsed calculation.
     */
    @Test
    fun `test timeElapsed calculation`() {
        // Set selectionTime to a known value
        treadmillIntervalData = treadmillIntervalData.copy(selectionTime = 1000)
        emitHIITData(treadmillIntervalData)
        // Simulate a time update: timer becomes 6000 (i.e., 5000 ms elapsed)
        mockWorkoutModel.timeElapsedMs.emit(6000)
        assertEquals(5000, viewModel.timeElapsed.value)
    }

    // Helper functions

    private fun emitHIITData(data: HIITWorkoutMap) {
        val bundle = HiitUtils.insertBundle(map = data)
        (mockWorkoutModel.workoutAppSetupBundle as MockParcelableVariable<Bundle>).emit(bundle)
    }

    private fun initializeInterval(
        firstString: String,
        secondString: String = ConsoleWorkoutVariables.START_INCLINE,
        data: List<Pair<Float, Float>>
    ): HIITWorkoutMap {
        return HIITWorkoutMap(
            intervalMap = hashMapOf<HIITIntervalEnum, Map<String, MutableState<Float>>>().apply {
                val intervalData = data.map { intervalData ->
                    hashMapOf(
                        firstString to mutableStateOf(intervalData.first),
                        secondString to mutableStateOf(intervalData.second)
                    )
                }

                HIITIntervalEnum.entries.subList(0, 3).forEachIndexed { index, interval ->
                    put(interval, intervalData[index])
                }
            },
            selectedInterval = 0,
            selectionTime = 0
        )
    }
}
