package com.jht.trackapp

import android.content.Intent
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.theme.ThemedViewModel
import com.jht.trackapp.common.models.LapDistanceEnum
import com.jht.trackapp.common.utils.Extensions.getLapData
import com.jht.widgets.helpers.StatFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.absoluteValue

private const val TAG = "TrackAppViewModel"

/** Modes for the track app, option is passed as string name via intent with [TrackAppModeKey] */
enum class TrackAppMode {
    /** The stock track app */
    Normal,
    /** Mile high club displays info at bottom instead of usual data */
    MileHighClub,
}
const val TrackAppModeKey = "com.jht.trackapp.mode"

/** Resolves the mode stored under [TrackAppModeKey], defaulting to [TrackAppMode.Normal]. */
private fun SavedStateHandle.toTrackAppMode(): TrackAppMode =
    get<String>(TrackAppModeKey)?.let { modeString ->
        TrackAppMode.entries.find { it.name == modeString }
    } ?: TrackAppMode.Normal

@HiltViewModel
class TrackAppViewModel @Inject constructor(
    private val uiBaseModel: UIBaseModel,
    private val workoutModel: WorkoutModel,
    private val savedStateHandle: SavedStateHandle, // seeded with the launch intent extras
) : ThemedViewModel() {
    val mode = mutableStateOf(savedStateHandle.toTrackAppMode())
    val currentLapDistance by lazy { mutableStateOf(LapDistanceEnum.DISTANCE_400) }
    val lapProgress by lazy { mutableFloatStateOf(0.0f) }
    val lapTime by lazy { mutableIntStateOf(0) }
    val lapList by lazy { mutableStateOf(emptyList<Int>()) }
    val heartRate by lazy { mutableIntStateOf(0) }
    val incline by lazy { mutableFloatStateOf(0f) }
    val speedKph by lazy { mutableFloatStateOf(0f) }
    val spm by lazy { mutableStateOf("0") }
    val strideLength by lazy { mutableStateOf("0.00") }
    val cadenceEnabled: Boolean = workoutModel.cadenceDisplayAllowed
    private val statFormatter by lazy {
        StatFormatter(
            frameType = uiBaseModel.frameType,
            isMetric = workoutModel.isMetric.value,
            is24HourFormat = workoutModel.is24HourFormat,
            precision = workoutModel.distancePrecision,
        )
    }
    // Time elapsed reading for internal calculation
    private val timeElapsed by lazy { mutableIntStateOf(0) }
    // Lap data based on lap distance
    private val laps100m by lazy { mutableStateOf(emptyList<Int>()) } // 100m generic lap data
    private val laps200m by lazy { mutableStateOf(emptyList<Int>()) }
    private val laps400m by lazy { mutableStateOf(emptyList<Int>()) }
    private val laps800m by lazy { mutableStateOf(emptyList<Int>()) }
    private val laps1600m by lazy { mutableStateOf(emptyList<Int>()) }
    // Distance
    private val distanceKm by lazy { mutableStateOf(0f) }

    /**
     * Called by the activity for every launch intent it receives, including re-launches delivered
     * to `onNewIntent`. An intent without [TrackAppModeKey] resets the app to [TrackAppMode.Normal].
     *
     * The resolved mode is written back to [savedStateHandle] so it is saved with the instance
     * state: unlike `Activity.setIntent()`, that survives process death, and [mode] is seeded from
     * it when this view model is rebuilt.
     */
    fun onLaunchIntent(intent: Intent?) {
        savedStateHandle[TrackAppModeKey] = intent?.getStringExtra(TrackAppModeKey)
        mode.value = savedStateHandle.toTrackAppMode()
    }

    override fun create() {
        // stats run continuously from creation to destruction, they do not stop when UI hides
        initStat()
    }

    // Update lap time followed by laps done based on the updated track distance i.e. distance selected by the user
    fun updateLapDistance(trackDistance: LapDistanceEnum) {
        currentLapDistance.value = trackDistance
        lapList.value = when (trackDistance) {
            LapDistanceEnum.DISTANCE_100 -> laps100m.value
            LapDistanceEnum.DISTANCE_200 -> laps200m.value
            LapDistanceEnum.DISTANCE_400 -> laps400m.value
            LapDistanceEnum.DISTANCE_800 -> laps800m.value
            LapDistanceEnum.DISTANCE_1600 -> laps1600m.value
        }
        val lapDistance = when (trackDistance) {
            LapDistanceEnum.DISTANCE_100 -> laps100m.value.sum()
            LapDistanceEnum.DISTANCE_200 -> laps200m.value.sum()
            LapDistanceEnum.DISTANCE_400 -> laps400m.value.sum()
            LapDistanceEnum.DISTANCE_800 -> laps800m.value.sum()
            LapDistanceEnum.DISTANCE_1600 -> laps1600m.value.sum()
        }
        lapTime.intValue = abs(timeElapsed.intValue - lapDistance.absoluteValue)
        lapProgress.floatValue = ((distanceKm.value % (trackDistance.distance.toFloat() / 1000)) / (trackDistance.distance.toFloat() / 1000))
    }

    private fun initStat() {
        workoutModel.let {
            it.distanceKm.listenAndTrigger { distance ->
                distanceKm.value = distance
                lapProgress.floatValue = ((distance % (currentLapDistance.value.distance.toFloat() / 1000f)) / (currentLapDistance.value.distance.toFloat() / 1000f))
            }
            // Capture time elapsed to handle track length switch
            it.timeElapsedMs.listenAndTrigger { time ->
                timeElapsed.intValue = time
                val lapDistance = when (currentLapDistance.value) {
                    LapDistanceEnum.DISTANCE_100 -> laps100m.value.sum()
                    LapDistanceEnum.DISTANCE_200 -> laps200m.value.sum()
                    LapDistanceEnum.DISTANCE_400 -> laps400m.value.sum()
                    LapDistanceEnum.DISTANCE_800 -> laps800m.value.sum()
                    LapDistanceEnum.DISTANCE_1600 -> laps1600m.value.sum()
                }
                lapTime.intValue = abs(time - lapDistance.absoluteValue)
            }

            it.trackDataParcelable.listenAndTrigger { trackInfo ->
                // Update 100m track data and compute data based on it
                laps100m.value = trackInfo.trackData
                computeTrackLengthData()
            }
            it.heartRate.listenAndTrigger { value ->
                heartRate.intValue = value
            }

            it.speedKph.listenAndTrigger { value ->
                speedKph.floatValue = value
            }

            it.incline.listenAndTrigger { value ->
                incline.floatValue = value
            }

            it.spm.listenAndTrigger { value ->
                spm.value = statFormatter.formatInt(value)
            }

            it.strideLengthMeters.listenAndTrigger { value ->
                strideLength.value = statFormatter.formatStrideLength(value, 2)
            }
        }
    }

    private fun computeTrackLengthData() {
        lapList.value = when (currentLapDistance.value) {
            LapDistanceEnum.DISTANCE_100 -> laps100m.value
            LapDistanceEnum.DISTANCE_200 -> laps200m.value
            LapDistanceEnum.DISTANCE_400 -> laps400m.value
            LapDistanceEnum.DISTANCE_800 -> laps800m.value
            LapDistanceEnum.DISTANCE_1600 -> laps1600m.value
        }
        laps100m.value.let { lapData ->
            laps200m.value = lapData.getLapData(2)
            laps400m.value = lapData.getLapData(4)
            laps800m.value = lapData.getLapData(8)
            laps1600m.value = lapData.getLapData(16)
        }
    }

    override fun destroy() {
        workoutModel.close()
    }
}
