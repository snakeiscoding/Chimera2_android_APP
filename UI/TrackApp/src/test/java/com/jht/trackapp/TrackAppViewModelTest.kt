package com.jht.trackapp

import androidx.lifecycle.SavedStateHandle
import com.jht.mock.interfaces.MockDataValue
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockWorkoutModel
import com.jht.data.TrackInfo
import com.jht.trackapp.common.models.LapDistanceEnum
import com.jht.trackapp.common.utils.Extensions.getLapData
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * @note: Lap distance in parameters are as follows:
 * 1 -> 100m
 * 2 -> 200m
 * 4 -> 400m
 * 8 -> 800m
 * 16 -> 1600m
 *
 * There is a helper function present in this class which deals with data translation to Enum for direct interaction with viewmodel functions
 */
@RunWith(JUnitParamsRunner::class)
class TrackAppViewModelTest {
    private lateinit var randomList: List<Int>
    private lateinit var mockWorkoutModel: MockWorkoutModel
    private lateinit var mockUIBaseModel: MockUIBaseModel
    private lateinit var viewModel: TrackAppViewModel

    @Before
    fun setUp() {
        mockWorkoutModel = MockWorkoutModel()
        mockUIBaseModel = MockUIBaseModel()
        viewModel = TrackAppViewModel(workoutModel = mockWorkoutModel, uiBaseModel = mockUIBaseModel, savedStateHandle = SavedStateHandle())
        randomList = randomListGenerator()
        viewModel.create()
        viewModel.start()
    }

    // Quite basic test case, but need to verify initial value that variables holds
    @Test
    fun `test initialVariableState`() {
        assertEquals(emptyList<Int>(), viewModel.lapList.value)
        assertEquals(LapDistanceEnum.DISTANCE_400, viewModel.currentLapDistance.value) // Default distance should be 400 i.e 2nd ordinal
        assertEquals(0.0f, viewModel.lapProgress.floatValue)
        assertEquals(0, viewModel.lapTime.intValue)
    }

    // Test to check lap if lap distance is updating based on user's input
    @Test
    @Parameters(
        "1",
        "2",
        "4",
        "8",
        "16"
    )
    fun `test updateLapDistance`(newDistance: Int) {
        val distanceToEnum = translateDistanceToEnum(newDistance)
        viewModel.updateLapDistance(distanceToEnum)
        assertEquals(distanceToEnum, viewModel.currentLapDistance.value)
    }

    // Test to validate lap progress value which is sent to View based on different track length and distance
    @Test
    @Parameters(
        "1, 100.05f",
        "2, 500.10f",
        "4, 200.28f",
        "8, 349.50f",
        "16, 8012.20f",
        "4, 10000.34f",
        "8, 60000.50f"
    )
    fun `test lapProgressWRTDistance`(distance: Int, distanceKm: Float) {
        val distanceToEnum = translateDistanceToEnum(distance)
        mockWorkoutModel.distanceKm.emit(distanceKm) // Emit distance and it will trigger the listener to update distance value in viewmodel
        viewModel.updateLapDistance(distanceToEnum)
        val expectedProgress = calculateExpectedLapProgress(distanceToEnum, distanceKm)
        assertEquals(expectedProgress, viewModel.lapProgress.floatValue)
    }

    @Test
    @Parameters(
        "1",
        "2",
        "4",
        "8",
        "16"
    )
    fun `test lapListSizeWRTDistance`(distance: Int) {
        val distanceToEnum = translateDistanceToEnum(distance)
        // Data Emitting
        (mockWorkoutModel.trackDataParcelable as MockDataValue<TrackInfo>).emit(TrackInfo(randomList))
        // Invoking Listeners
        viewModel.updateLapDistance(distanceToEnum)
        val expectedResult = randomList.size / distance
        assertEquals(expectedResult, viewModel.lapList.value.size)
    }

    /**
     * LapList will sum trackDataParcelable list depending on the windowSize
     * e.g.
     * If TrackList -> [1, 3, 1, 4, 6, 8, 2], then
     * : For track length 200m -> windowSize = 2
     *      List will transform to [1+3, 1+4, 6+8] = [4, 5, 14]
     * : For track length 400m -> windowSize = 4
     *      List will transform to [1+3+1+4] = [9]
     * : For track length 800m & 1600m it shall return empty list -> windowSize = 8 & 16
     *
     * NOTE:
     * 1. If list size is less than windowSize then all values will be ignored.
     * 2. If list size >= windowSize then each window shift needs to contain exact windowSize items in it else elements will be ignored
     */
    @Test
    @Parameters(
        "1",
        "2",
        "4",
        "8",
        "16"
    )
    fun `test validateLapListData`(distance: Int) {
        val distanceToEnum = translateDistanceToEnum(distance)
        (mockWorkoutModel.trackDataParcelable as MockDataValue<TrackInfo>).emit(TrackInfo(randomList))
        viewModel.updateLapDistance(distanceToEnum)
        val expectedResult = randomList.getLapData(distance)
        assertEquals(expectedResult, viewModel.lapList.value)
    }

    // Test to validate lapTime shown in the UI
    @Test
    @Parameters(
        "1, 100000",
        "2, 200000",
        "4, 100000",
        "8, 67000",
        "16, 346378"
    )
    fun `test validateRunningLapTimeWRTDistance`(distance: Int, timeElapsed: Int) {
        val distanceToEnum = translateDistanceToEnum(distance)
        mockWorkoutModel.timeElapsedMs.emit(timeElapsed)
        (mockWorkoutModel.trackDataParcelable as MockDataValue<TrackInfo>).emit(TrackInfo(randomList))
        viewModel.updateLapDistance(distanceToEnum)
        val expectedResult = calculateLapTime(timeElapsed, viewModel.lapList.value.sum())
        assertEquals(expectedResult, viewModel.lapTime.intValue)
    }

    // Helper function
    private fun translateDistanceToEnum(distance: Int): LapDistanceEnum {
        return when (distance) {
            1 -> LapDistanceEnum.DISTANCE_100
            2 -> LapDistanceEnum.DISTANCE_200
            4 -> LapDistanceEnum.DISTANCE_400
            8 -> LapDistanceEnum.DISTANCE_800
            16 -> LapDistanceEnum.DISTANCE_1600
            else -> LapDistanceEnum.DISTANCE_100
        }
    }

    private fun calculateExpectedLapProgress(distance: LapDistanceEnum, distanceKm: Float) =
        ((distanceKm % (distance.distance.toFloat() / 1000)) / (distance.distance.toFloat() / 1000))

    // Generates a list of size 80 which contains random number from 1 to 100
    private fun randomListGenerator() =
        (1..80).map { (1..100).random() }

    private fun calculateLapTime(time: Int, lapData: Int) =
        abs(time - lapData)
}