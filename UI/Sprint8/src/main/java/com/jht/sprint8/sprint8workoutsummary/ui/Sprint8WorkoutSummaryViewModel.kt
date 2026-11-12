package com.jht.sprint8.sprint8workoutsummary.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.jht.enums.FrameType
import com.jht.enums.SummaryStage
import com.jht.enums.SummaryStat
import com.jht.enums.SummaryStat.ACTIVE_CALORIES
import com.jht.enums.SummaryStat.AVERAGE_HEART_RATE
import com.jht.enums.SummaryStat.AVERAGE_INCLINE
import com.jht.enums.SummaryStat.AVERAGE_METS
import com.jht.enums.SummaryStat.AVERAGE_PACE
import com.jht.enums.SummaryStat.AVERAGE_RESISTANCE
import com.jht.enums.SummaryStat.AVERAGE_RPM
import com.jht.enums.SummaryStat.AVERAGE_SPEED
import com.jht.enums.SummaryStat.AVERAGE_SPM
import com.jht.enums.SummaryStat.AVERAGE_WATTS
import com.jht.enums.SummaryStat.CALORIES
import com.jht.enums.SummaryStat.DISTANCE
import com.jht.enums.SummaryStat.ELEVATION
import com.jht.enums.SummaryStat.FLOORS
import com.jht.enums.SummaryStat.PEAK_HEART_RATE
import com.jht.enums.SummaryStat.PEAK_INCLINE
import com.jht.enums.SummaryStat.PEAK_METS
import com.jht.enums.SummaryStat.PEAK_RESISTANCE
import com.jht.enums.SummaryStat.PEAK_RPM
import com.jht.enums.SummaryStat.PEAK_SPEED_KPH
import com.jht.enums.SummaryStat.PEAK_SPM
import com.jht.enums.SummaryStat.PEAK_WATTS
import com.jht.enums.SummaryStat.STEPS
import com.jht.enums.SummaryStat.TIME_ELAPSED
import com.jht.enums.UiEvent
import com.jht.interfaces.models.IdleTimerModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.interfaces.models.WorkoutSummaryModel
import com.jht.sprint8.R
import com.jht.sprint8.assessment.data.datastore.AssessmentDataStoreManager
import com.jht.sprint8.model.AssessmentWorkoutSummaryStat
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_ELITE_COLOR
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_INTERMEDIATE_COLOR
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_START_WORKOUT_BTN_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_RED
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import com.jht.unit.UnitFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.round
import kotlin.math.roundToInt


private const val TAG = "Sprint8WorkoutSummary"

@HiltViewModel
open class Sprint8WorkoutSummaryViewModel @Inject constructor(
    private val dataStoreManager: AssessmentDataStoreManager,
    private val systemModel: SystemModel,
    private val uiBaseModel: UIBaseModel,
    private val navigationModel: NavigationModel,
    private val idleTimerModel: IdleTimerModel,
    @ApplicationContext private val context: Context,
    val workoutModel: WorkoutModel,
    private val summaryModel: WorkoutSummaryModel,
) : ThemedViewModel() {
    val theme by lazy { mutableStateOf(defaultDynamicTheme) }
    private val frameType by lazy { mutableStateOf(uiBaseModel.frameType) }
    internal val fullStatTable = mutableStateOf(
        FullStatTableRow.createTable(
            uiBaseModel.frameType,
            workoutModel.wattsDisplayAllowed,
            workoutModel.isMetric.value,
            summaryModel
        )
    )
    private val speedLabel by lazy { context.getString(if (workoutModel.isMetric.value) R.string.kph_lc else R.string.mph_lc) }
    val sprint8WorkoutSplits = mutableStateOf(summaryModel.getSweatScore(SummaryStage.TOTAL))
    private val _isAssessmentVisible = MutableStateFlow(false)
    val isAssessmentVisible: StateFlow<Boolean> = _isAssessmentVisible.asStateFlow()
    private val _recommendedAssessmentLevel = MutableStateFlow(0)
    val recommendedAssessmentLevel: StateFlow<Int> = _recommendedAssessmentLevel.asStateFlow()
    private val _assessmentLevelBgColor = MutableStateFlow(Color.Transparent)
    val assessmentLevelBgColor: StateFlow<Color> = _assessmentLevelBgColor.asStateFlow()
    private val _categoryRecommended = MutableStateFlow("")
    val categoryRecommended: StateFlow<String> = _categoryRecommended
    private val _summaryStatsList = MutableStateFlow<List<AssessmentWorkoutSummaryStat>>(emptyList())
    val summaryStatsList: StateFlow<List<AssessmentWorkoutSummaryStat>> = _summaryStatsList.asStateFlow()
    private val peakIncline = summaryModel.getWorkoutStat(PEAK_INCLINE, SummaryStage.TOTAL)
    private val peakSpeed = summaryModel.getWorkoutStat(PEAK_SPEED_KPH, SummaryStage.TOTAL)
    private val averageSpeed = summaryModel.getWorkoutStat(AVERAGE_SPEED, SummaryStage.TOTAL)
    private val averageIncline = summaryModel.getWorkoutStat(AVERAGE_INCLINE, SummaryStage.TOTAL)
    private val peakResistance = summaryModel.getWorkoutStat(PEAK_RESISTANCE, SummaryStage.TOTAL)
    private val averageResistance = summaryModel.getWorkoutStat(AVERAGE_RESISTANCE, SummaryStage.TOTAL)

    private val statsList = listOf(
        AssessmentWorkoutSummaryStat(
            title = context.getString(R.string.peak_incline_tc),
            value = peakIncline,
            "%"
        ),
        AssessmentWorkoutSummaryStat(
            title = context.getString(R.string.peak_speed_tc),
            value = UnitFormatter.formatSpeed(peakSpeed, workoutModel.isMetric.value).toFloat(),
            speedLabel
        ),
        AssessmentWorkoutSummaryStat(
            title = context.getString(R.string.average_speed_tc),
            value = UnitFormatter.formatSpeed(averageSpeed, workoutModel.isMetric.value).toFloat(),
            speedLabel
        ),
        AssessmentWorkoutSummaryStat(
            title = context.getString(R.string.average_incline_tc),
            value = averageIncline,
            "%"
        ),
        AssessmentWorkoutSummaryStat(
            title = context.getString(R.string.peak_resistance_tc),
            value = peakResistance,
            ""
        ),
        AssessmentWorkoutSummaryStat(
            title = context.getString(R.string.average_resistance_tc),
            value = averageResistance,
            ""
        )
    )

    init {
        val filteredStatsList = statsList.filter { stat ->
            when (frameType.value) {
                FrameType.Treadmill -> {
                    stat.title.contains(context.getString(R.string.speed_tc)) || stat.title.contains(
                        context.getString(R.string.incline_tc)
                    )
                }

                FrameType.Ascent -> {
                    stat.title.contains(context.getString(R.string.resistance_tc)) || stat.title.contains(
                        context.getString(R.string.incline_tc)
                    )
                }

                else -> {
                    stat.title.contains(context.getString(R.string.resistance_tc))
                }
            }
        }
        _summaryStatsList.value = filteredStatsList
        viewModelScope.launch {
            val isVisible = dataStoreManager.isAssessmentVisible.first()
            val recommendedAssessmentLevel =  dataStoreManager.assessmentRecommendedLevel.first()
            _isAssessmentVisible.value = isVisible
            _recommendedAssessmentLevel.value = recommendedAssessmentLevel
            _categoryRecommended.value = assessmentRecommendedCategory(_recommendedAssessmentLevel.value)
            _assessmentLevelBgColor.value = recommendedLevelBgColor(_categoryRecommended.value)
        }
    }

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
        viewModelScope.launch {
            dataStoreManager.clearAssessmentVisibility()
        }
    }

    /**
     * Returns the recommended category string for a given assessment level.
     *
     * This function maps an integer assessment level to a textual category,
     * such as Beginner, Intermediate, Advanced, or Elite. If the level is outside
     * the defined range, it returns an "out of range" message.
     *
     * @param assessmentLevel Integer representing the assessment level.
     * @return The localized string corresponding to the recommended category.
     */
    private fun assessmentRecommendedCategory(assessmentLevel: Int): String {
        return when (assessmentLevel) {
            in 1 ..5 -> context.getString(R.string.beginner_tc)
            in 6..10 -> context.getString(R.string.intermediate_tc)
            in 11..15 -> context.getString(R.string.advanced_tc)
            in 16..20 -> context.getString(R.string.elite_tc)
            else -> context.getString(R.string.out_of_range_tc)
        }
    }

    /**
     * Returns the recommended level background color for a given assessment level.
     */
    private fun recommendedLevelBgColor(categoryRecommended: String): Color {
        return when (categoryRecommended) {
            context.getString(R.string.beginner_tc) -> {
                SPRINT8_NEW_RED
            }
            context.getString(R.string.intermediate_tc)  -> {
                ASSESSMENT_INTERMEDIATE_COLOR

            }
            context.getString(R.string.advanced_tc)  -> {
                CUSTOM_WORKOUT_START_WORKOUT_BTN_COLOR
            }
            context.getString(R.string.elite_tc)  -> {
                ASSESSMENT_ELITE_COLOR
            }
            else -> {
                Color.Transparent
            }
        }
    }

    fun onHome() {
        navigationModel.launchSplashScreen()
    }

    private val sweatScores: List<Float?>? = sprint8WorkoutSplits.value?.sweatScoreList
    val totalSweatScore: String = sweatScores
        ?.filterNotNull()
        ?.sumOf { round(it).toInt() }
        ?.toString() ?: "0"

    val peakSweatScore: String = sweatScores
        ?.filterNotNull()
        ?.maxOfOrNull { round(it).toInt() }
        ?.toString() ?: "0"

    val averageSweatScore: String = sweatScores
        ?.filterNotNull()
        ?.map { round(it).toInt() }
        ?.let { if (it.isNotEmpty()) (it.sum() / it.size).toString() else "0" } ?: "0"

}

/** [FullStatTableRow] consists of label, unit, and a value per stage */
internal class FullStatTableRow(
    val labelId: Int,
    val unitId: Int,
    val valueMap: MutableMap<SummaryStage, String>,
) {
    /** Constructor using a callback to populate values */
    constructor(labelId: Int, unitId: Int, populateValues: (SummaryStage) -> String) : this(
        labelId,
        unitId,
        mutableMapOf()
    ) {
        SummaryStage.values().forEach { stage ->
            valueMap[stage] = populateValues(stage)
        }
    }

    /** null safe accessor of values, returns empty string if not found */
    operator fun get(stage: SummaryStage): String = valueMap[stage] ?: ""

    companion object {
        fun createTable(
            frameType: FrameType,
            wattsDisplayAllowed: Boolean,
            isMetric: Boolean,
            summaryModel: WorkoutSummaryModel
        ): List<FullStatTableRow> {
            val statList = mutableListOf<FullStatTableRow>()
            val showActiveCalories =
                hasActiveCalories(summaryModel) // basically gymkit splits calories into active and total
            // get stats for frame
            val statsNeeded = when (frameType) {
                FrameType.Treadmill -> listOfNotNull(
                    TIME_ELAPSED,
                    DISTANCE,
                    AVERAGE_SPEED,
                    AVERAGE_INCLINE,
                    CALORIES,
                    AVERAGE_PACE,
                    AVERAGE_WATTS.takeIf { wattsDisplayAllowed },
                    AVERAGE_METS,
                    AVERAGE_HEART_RATE,
                    PEAK_SPEED_KPH,
                    PEAK_WATTS.takeIf { wattsDisplayAllowed },
                    PEAK_METS,
                    PEAK_HEART_RATE,
                    PEAK_INCLINE,
                    ELEVATION
                )

                FrameType.Ascent -> listOfNotNull(
                    TIME_ELAPSED,
                    DISTANCE,
                    CALORIES,
                    AVERAGE_INCLINE,
                    AVERAGE_WATTS.takeIf { wattsDisplayAllowed },
                    AVERAGE_PACE,
                    AVERAGE_SPEED,
                    AVERAGE_METS,
                    AVERAGE_RPM,
                    AVERAGE_HEART_RATE,
                    AVERAGE_RESISTANCE,
                    PEAK_SPEED_KPH,
                    PEAK_WATTS.takeIf { wattsDisplayAllowed },
                    PEAK_METS,
                    PEAK_RPM,
                    PEAK_HEART_RATE,
                    PEAK_INCLINE,
                    PEAK_RESISTANCE
                )

                FrameType.Bike, FrameType.SpinBike, FrameType.Elliptical -> listOfNotNull(
                    TIME_ELAPSED,
                    DISTANCE,
                    CALORIES,
                    AVERAGE_WATTS.takeIf { wattsDisplayAllowed },
                    AVERAGE_PACE,
                    AVERAGE_SPEED,
                    AVERAGE_METS,
                    AVERAGE_RPM,
                    AVERAGE_HEART_RATE,
                    AVERAGE_RESISTANCE,
                    PEAK_SPEED_KPH,
                    PEAK_WATTS.takeIf { wattsDisplayAllowed },
                    PEAK_METS,
                    PEAK_RPM,
                    PEAK_HEART_RATE,
                    PEAK_RESISTANCE
                )

                FrameType.Climbmill, FrameType.Stepper -> listOfNotNull(
                    TIME_ELAPSED,
                    CALORIES,
                    STEPS,
                    FLOORS,
                    AVERAGE_SPM,
                    AVERAGE_WATTS.takeIf { wattsDisplayAllowed },
                    AVERAGE_METS,
                    AVERAGE_HEART_RATE,
                    PEAK_SPM,
                    PEAK_WATTS.takeIf { wattsDisplayAllowed },
                    PEAK_METS,
                    PEAK_HEART_RATE
                )
            }
            // generate table rows for each stat
            for (stat in statsNeeded) {
                if (stat == CALORIES && showActiveCalories) createRow(
                    ACTIVE_CALORIES,
                    frameType,
                    isMetric,
                    summaryModel
                )?.let { statList.add(it) }
                createRow(stat, frameType, isMetric, summaryModel)?.let { statList.add(it) }
            }
            // clear out values for unused stages
            SummaryStage.values().forEach { stage ->
                if (!summaryModel.hadWorkoutStage(stage)) {
                    statList.forEach { stat ->
                        stat.valueMap[stage] = "-"
                    }
                }
            }
            return statList
        }

        private fun createRow(
            stat: SummaryStat,
            frameType: FrameType,
            isMetric: Boolean,
            summaryModel: WorkoutSummaryModel
        ): FullStatTableRow? =
            when (stat) {
                TIME_ELAPSED -> FullStatTableRow(
                    R.string.time_elapsed_tc, 0
                ) {
                    UnitFormatter.formatTimeSeconds(
                        summaryModel.getWorkoutStat(stat, it).roundToInt()
                    )
                }

                DISTANCE -> FullStatTableRow(
                    R.string.distance_tc, if (isMetric) R.string.km_lc else R.string.miles_lc
                ) {
                    UnitFormatter.formatDistance(summaryModel.getWorkoutStat(stat, it), isMetric)
                }

                ELEVATION -> FullStatTableRow(
                    R.string.elevation_tc, if (isMetric) R.string.m_lc else R.string.ft_lc
                ) {
                    UnitFormatter.formatElevation(summaryModel.getWorkoutStat(stat, it), isMetric)
                }

                STEPS -> FullStatTableRow(
                    R.string.steps_tc, 0
                ) {
                    UnitFormatter.formatInt(summaryModel.getWorkoutStat(stat, it).roundToInt())
                }

                FLOORS -> FullStatTableRow(
                    R.string.floors_tc, 0
                ) {
                    UnitFormatter.formatFloors(summaryModel.getWorkoutStat(stat, it))
                }

                AVERAGE_SPM -> FullStatTableRow(
                    R.string.average_spm_tc, 0
                ) {
                    UnitFormatter.formatInt(summaryModel.getWorkoutStat(stat, it).roundToInt())
                }

                PEAK_SPM -> FullStatTableRow(
                    R.string.peak_spm_tc, 0
                ) {
                    UnitFormatter.formatInt(summaryModel.getWorkoutStat(stat, it).roundToInt())
                }

                AVERAGE_SPEED -> FullStatTableRow(
                    R.string.average_speed_tc, if (isMetric) R.string.kph_lc else R.string.mph_lc
                ) {
                    UnitFormatter.formatSpeed(summaryModel.getWorkoutStat(stat, it), isMetric)
                }

                PEAK_SPEED_KPH -> FullStatTableRow(
                    R.string.peak_speed_tc, if (isMetric) R.string.kph_lc else R.string.mph_lc
                ) {
                    UnitFormatter.formatSpeed(summaryModel.getWorkoutStat(stat, it), isMetric)
                }

                AVERAGE_PACE -> FullStatTableRow(
                    R.string.average_pace_tc, 0
                ) {
                    UnitFormatter.formatPace(
                        summaryModel.getWorkoutStat(AVERAGE_SPEED, it),
                        isMetric
                    )
                }

                AVERAGE_RPM -> FullStatTableRow(
                    R.string.average_rpm_tc, 0
                ) {
                    UnitFormatter.formatInt(summaryModel.getWorkoutStat(stat, it).roundToInt())
                }

                PEAK_RPM -> FullStatTableRow(
                    R.string.peak_rpm_tc, 0
                ) {
                    UnitFormatter.formatInt(summaryModel.getWorkoutStat(stat, it).roundToInt())
                }

                AVERAGE_RESISTANCE -> FullStatTableRow(
                    R.string.average_resistance_tc, 0
                ) {
                    UnitFormatter.formatInt(summaryModel.getWorkoutStat(stat, it).roundToInt())
                }

                PEAK_RESISTANCE -> FullStatTableRow(
                    R.string.peak_resistance_tc, 0
                ) {
                    UnitFormatter.formatInt(summaryModel.getWorkoutStat(stat, it).roundToInt())
                }

                AVERAGE_INCLINE -> FullStatTableRow(
                    R.string.average_incline_tc, 0
                ) {
                    UnitFormatter.formatIncline(
                        summaryModel.getWorkoutStat(stat, it),
                        frameType.isTreadmill
                    )
                }

                PEAK_INCLINE -> FullStatTableRow(
                    R.string.peak_incline_tc, 0
                ) {
                    UnitFormatter.formatIncline(
                        summaryModel.getWorkoutStat(stat, it),
                        frameType.isTreadmill
                    )
                }

                AVERAGE_WATTS -> FullStatTableRow(
                    R.string.average_watts_tc, 0
                ) {
                    UnitFormatter.formatWatts(summaryModel.getWorkoutStat(stat, it))
                }

                PEAK_WATTS -> FullStatTableRow(
                    R.string.peak_watts_tc, 0
                ) {
                    UnitFormatter.formatWatts(summaryModel.getWorkoutStat(stat, it))
                }

                AVERAGE_METS -> FullStatTableRow(
                    R.string.average_mets_tc, 0
                ) {
                    UnitFormatter.formatMets(summaryModel.getWorkoutStat(stat, it))
                }

                PEAK_METS -> FullStatTableRow(
                    R.string.peak_mets_tc, 0
                ) {
                    UnitFormatter.formatMets(summaryModel.getWorkoutStat(stat, it))
                }

                AVERAGE_HEART_RATE -> FullStatTableRow(
                    R.string.average_heart_rate_tc, 0
                ) {
                    UnitFormatter.formatHeartRate(summaryModel.getWorkoutStat(stat, it))
                }

                PEAK_HEART_RATE -> FullStatTableRow(
                    R.string.peak_heart_rate_tc, 0
                ) {
                    UnitFormatter.formatHeartRate(summaryModel.getWorkoutStat(stat, it))
                }

                ACTIVE_CALORIES -> FullStatTableRow(
                    R.string.active_calories_tc, 0
                ) {
                    UnitFormatter.formatCalories(summaryModel.getWorkoutStat(stat, it))
                }

                CALORIES -> FullStatTableRow(
                    if (hasActiveCalories(summaryModel)) R.string.total_calories_tc else R.string.calories_tc,
                    0
                ) {
                    UnitFormatter.formatCalories(summaryModel.getWorkoutStat(stat, it))
                }

                else -> null
            }
    }
}

private fun hasActiveCalories(summaryModel: WorkoutSummaryModel): Boolean =
    summaryModel.getWorkoutStat(ACTIVE_CALORIES, SummaryStage.TOTAL) > 0
