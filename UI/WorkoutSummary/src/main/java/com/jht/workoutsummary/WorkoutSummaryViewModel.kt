package com.jht.workoutsummary

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import com.jht.enums.SummaryStage
import com.jht.enums.SummaryStat.ACTIVE_CALORIES
import com.jht.enums.SummaryStat.AVERAGE_HEART_RATE
import com.jht.enums.SummaryStat.PEAK_HEART_RATE
import com.jht.enums.UiEvent
import com.jht.interfaces.models.FitnessTestResult
import com.jht.interfaces.models.IdleTimerModel
import com.jht.interfaces.models.MyZoneTimeInZones
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.interfaces.models.WorkoutSplits
import com.jht.interfaces.models.WorkoutSummaryModel
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import com.jht.unit.UnitFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt

private const val TAG = "WorkoutSummary"

@HiltViewModel
open class WorkoutSummaryViewModel @Inject constructor(
    val systemModel: SystemModel,
    val uiBaseModel: UIBaseModel,
    val navigationModel: NavigationModel,
    val idleTimerModel: IdleTimerModel,
    val workoutModel: WorkoutModel,
    val summaryModel: WorkoutSummaryModel,
    val savedStateHandle: SavedStateHandle, // can provide launch intent extras
) : ThemedViewModel() {
    val theme by lazy { mutableStateOf(defaultDynamicTheme) }
    val quickStatList = mutableStateOf(
        summaryModel.getQuickStats()
    )
    val fullStatTable = mutableStateOf(
        summaryModel.getFullStats()
    )
    val workoutSplits = mutableStateOf(
        SplitTable.create(
            summaryModel.getWorkoutSplits(SummaryStage.WORKOUT),
            workoutModel.isMetric.value,
        )
    )
    val fitnessTestResult = mutableStateOf<FitnessTestResult?>(null)
    val myZoneResults = mutableStateOf<MyZoneResults?>(null)
    /** In-summary survey URL; retail only, since commercial shows the survey before the summary */
    val surveyUrl = if (uiBaseModel.isRetail) systemModel.postWorkoutSurveyUrl else ""

    /**
     * Call this to register listeners when activity is resumed
     */
    override fun start() {
        uiBaseModel.themeUri.listenAndTrigger { theme.value = loadTheme(it) }
        val timeoutSeconds = systemModel.screenTimeoutSeconds
        if (timeoutSeconds > 0) {
            idleTimerModel.addIdleTimer(TAG, timeoutSeconds * 1000, ::onHome)
            idleTimerModel.startIdleTimers()
        }
        /*savedStateHandle.get<Int>(IntentConstants.WorkoutEndWorkoutIdInt)?.let {
            Log.d(TAG, "FOUND jht.workoutEnd.workoutId $it") // TESTING the workout end intent extra to prepare for possible single activity workout apps
        }*/
        fitnessTestResult.value = summaryModel.getFitnessTestResult()
        summaryModel.myZoneTimeInZones.listenAndTrigger {
            myZoneResults.value = MyZoneResults.create(it, summaryModel, workoutModel)
        }
        systemModel.sendUiEvent(UiEvent.WORKOUT_SUMMARY)
    }

    /**
     * Call this to register listeners when activity is paused
     */
    override fun stop() {
        systemModel.close()
        uiBaseModel.close()
        navigationModel.close()
        idleTimerModel.close()
        workoutModel.close()
        summaryModel.close()
    }

    fun onHome() {
        navigationModel.launchSplashScreen()
    }

}


class MyZoneResults(
    val timeInZones: MyZoneTimeInZones,
    val duration: String,
    val effort: String,
    val meps: String,
    val calories: String,
    val averageHr: String,
    val peakHr: String,
) {
    fun zone(index: Int) = if (index < timeInZones.zones.size) timeInZones.zones[index] else null
    companion object {
        fun create(zones: MyZoneTimeInZones?, summaryModel: WorkoutSummaryModel, workoutModel: WorkoutModel): MyZoneResults? {
            if (zones == null) return null
            val durationMinutes = (zones.getDurationSeconds() / 60f).roundToInt()
            return MyZoneResults(
                zones,
                UnitFormatter.formatInt(durationMinutes),
                UnitFormatter.formatInt(workoutModel.myZoneHeartRatePercent.value),
                UnitFormatter.formatInt(workoutModel.myZoneMeps.value),
                UnitFormatter.formatInt(workoutModel.calories.value.roundToInt()),
                UnitFormatter.formatHeartRate(summaryModel.getWorkoutStat(AVERAGE_HEART_RATE, SummaryStage.WORKOUT)),
                UnitFormatter.formatHeartRate(summaryModel.getWorkoutStat(PEAK_HEART_RATE, SummaryStage.WORKOUT)),
            )
        }
    }
}

class SplitRow(
    val number: String,
    val time: String,
    val distance: String,
)
class SplitTable(
    workoutSplits: WorkoutSplits = WorkoutSplits(),
    isMetric: Boolean = false,
) {
    val distanceUnitId: Int
    val splits: List<SplitRow>
    val laps: String
    val lapBestTime: String

    init {
        laps = workoutSplits.lapsTotal.toString()
        lapBestTime = UnitFormatter.formatTimeMilliseconds(workoutSplits.lapBestTimeMs)
        distanceUnitId = if (isMetric) R.string.km_lc else R.string.miles_lc
        splits = workoutSplits.splits.mapIndexed { index, split ->
            SplitRow(
                (index + 1).toString(),
                UnitFormatter.formatTimeMilliseconds(split.timeMs),
                UnitFormatter.formatDistance(split.distanceKm, isMetric),
            )
        }
    }
    fun hasSplits() = splits.isNotEmpty()
    companion object {
        fun create(workoutSplits: WorkoutSplits?, isMetric: Boolean): SplitTable? {
            return workoutSplits?.let { SplitTable(workoutSplits, isMetric) }
        }
    }
}

// should we put something on the WorkoutSummaryModel for this? or pretty self explanatory?
private fun hasActiveCalories(summaryModel: WorkoutSummaryModel): Boolean = summaryModel.getWorkoutStat(ACTIVE_CALORIES, SummaryStage.TOTAL) > 0
