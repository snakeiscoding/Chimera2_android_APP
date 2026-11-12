package com.jht.interfaces.models

import android.util.Log
import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.formatting.FloorFormat
import com.jht.chimera.lib.common.stats.SummaryData
import com.jht.chimera.lib.common.stats.WorkoutSummary
import com.jht.chimera.lib.common.stats.WorkoutSummaryList
import com.jht.chimera.lib.integrations.myzone.TimeInZones
import com.jht.chimera.lib.workout.FitnessTestResultHelper
import com.jht.datamatrixlib.DataMatrix
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
import com.jht.enums.WorkoutId
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.MatrixStringValueListener
import com.jht.unit.UnitFormatter
import com.jht.unit.kphUnit
import com.jht.unit.secondsPerKmPaceUnit
import kotlin.math.roundToInt

class MatrixWorkoutSummaryModel(
) : WorkoutSummaryModel() {

    private val manager: CallbackManager = CallbackManager()

    override val lastProgramUuid = MatrixStringValueListener(manager, WorkoutKeys.DATA_LAST_PROGRAM_UUID_STRING)

    override fun hadWorkoutStage(stage: SummaryStage): Boolean {
        val summaryData = DataMatrix.getParcelable(
            WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE,
            SummaryData::class.java
        ) ?: return false
        val summary = getSummaryStage(summaryData, stage)
        return summary.statsAccumulated.secondsStateRun > 0
    }

    override fun getWorkoutStat(stat: SummaryStat, stage: SummaryStage): Float {
        val summaryData = DataMatrix.getParcelable(
            WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE,
            SummaryData::class.java
        ) ?: return 0f

        val summary = getSummaryStage(summaryData, stage)
        return getStatFromSummary(summary, stat)
    }

    private fun getStatFromSummary(summary: WorkoutSummary, stat: SummaryStat): Float =
        when (stat) {
            TIME_ELAPSED -> summary.statsAccumulated.secondsStateRun
            CALORIES -> summary.statsAccumulated.calories
            ACTIVE_CALORIES -> summary.statsAccumulated.activeCalories
            DISTANCE -> summary.statsAccumulated.distanceKm
            STEPS -> summary.statsAccumulated.steps
            FLOORS -> summary.statsAccumulated.floors
            ELEVATION -> summary.statsAccumulated.elevationMeters
            AVERAGE_WATTS -> summary.statsAverages.averageWatts
            AVERAGE_METS -> summary.statsAverages.averageMets
            AVERAGE_HEART_RATE -> summary.statsAverages.averageHeartRate
            AVERAGE_RESISTANCE -> summary.statsAverages.averageResistance
            AVERAGE_SPM -> summary.statsAverages.averageStepsPerMinute
            AVERAGE_RPM -> summary.statsAverages.averageRpm
            AVERAGE_SPEED -> summary.statsAverages.averageSpeedKph
            AVERAGE_PACE -> summary.statsAverages.averageSpeedKph.kphUnit.toSecondsPerKilometerPace
            AVERAGE_INCLINE -> summary.statsAverages.averageIncline
            PEAK_SPEED_KPH -> summary.statsPeaks.peakSpeedKph
            PEAK_WATTS -> summary.statsPeaks.peakWatts
            PEAK_HEART_RATE -> summary.statsPeaks.peakHeartRate.toFloat()
            PEAK_INCLINE -> summary.statsPeaks.peakIncline
            PEAK_RESISTANCE -> summary.statsPeaks.peakResistance.toFloat()
            PEAK_METS -> summary.statsPeaks.peakMets
            PEAK_RPM -> summary.statsPeaks.peakRpm.toFloat()
            PEAK_SPM -> summary.statsPeaks.peakStepsPerMinute.toFloat()
        }

    override fun getWorkoutSplits(stage: SummaryStage): WorkoutSplits? {
        val frameClass = FrameConfigManager.getInstance().frameConfig.machineClassType
        if (frameClass == JHTCommonTypes.MachineClassificationType.Climbmill || frameClass == JHTCommonTypes.MachineClassificationType.Stepper) {
            return null
        }
        val summaryData = DataMatrix.getParcelable(
            WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE,
            SummaryData::class.java
        )

        val splits = if (summaryData == null) listOf() else getSummaryStage(
            summaryData,
            stage
        ).statsSplits.workoutSplits.map { WorkoutSplit(it.timeMs, it.distanceKm) }
        val totalLaps = DataMatrix.getInt(WorkoutKeys.DATA_LAP_COUNT_INT)
        val bestTime = DataMatrix.getInt(WorkoutKeys.DATA_LAP_BEST_TIME_MS_INT)
        return WorkoutSplits(splits, totalLaps, bestTime)
    }

    private fun getSummaryStage(summaryData: SummaryData, stage: SummaryStage): WorkoutSummary {
        return when (stage) {
            SummaryStage.WARMUP -> summaryData.warmupSummary
            SummaryStage.WORKOUT -> summaryData.workoutSummary
            SummaryStage.COOLDOWN -> summaryData.cooldownSummary
            SummaryStage.TOTAL -> summaryData.totalSummary
        }
    }

    override fun close() {
        manager.cancelAllListeners()
    }

    override fun getFitnessTestResult(): FitnessTestResult? {
        val summaryData = DataMatrix.getParcelable(
            WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE,
            SummaryData::class.java
        ) ?: return null
        val context = ContextUtil.getApplicationContext() ?: return null
        val helper = FitnessTestResultHelper(context, summaryData)
        val workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT)
        // roll results and test specific stats into a string for UI to present
        return when (workoutId) {
            WorkoutId.COOPER_FITNESS_TEST.id -> {
                val vo2 = FloorFormat.format(DataMatrix.getFloat(WorkoutKeys.DATA_VO2_FLOAT), 2)
                val vo2p = FloorFormat.format(
                    DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FLOAT),
                    0
                )
                FitnessTestResult(
                    rating = context.getString(R.string.rating_tc) + ": " + helper.getCooperRating(),
                    info = listOf(
                        context.getString(R.string.last_stage_completed_tc) + ": " + helper.getCooperStagesCompleted(),
                    ),
                    stats = listOf(
                        Pair(context.getString(R.string.vo2_tc), vo2),
                        Pair(context.getString(R.string.vo2_percentile_tc), vo2p),
                    )
                )
            }

            WorkoutId.GERKIN_FITNESS_TEST.id -> {
                val max = FloorFormat.format(DataMatrix.getFloat(WorkoutKeys.DATA_VO2_FLOAT), 0)
                FitnessTestResult(
                    rating = helper.getGerkinRating(),
                    stats = listOf(
                        Pair(context.getString(R.string.max_vo2_tc), max)
                    )
                )
            }

            WorkoutId.SUBMAX_FITNESS_TEST.id -> {
                val max = FloorFormat.format(DataMatrix.getFloat(WorkoutKeys.DATA_VO2_FLOAT), 0)
                FitnessTestResult(
                    rating = helper.getSubmaxRating(),
                    stats = listOf(
                        Pair(context.getString(R.string.max_vo2_tc), max)
                    )
                )
            }

            WorkoutId.MARINES_FITNESS_TEST.id, WorkoutId.ARMY_FITNESS_TEST.id -> {
                FitnessTestResult(rating = helper.getScoredFitnessTestResult(100))
            }

            WorkoutId.PEB_FITNESS_TEST.id -> {
                FitnessTestResult(rating = helper.getScoredFitnessTestResult(95))
            }

            WorkoutId.CPAT_FITNESS_TEST.id -> {
                FitnessTestResult(rating = helper.getCPATTestResult(100))
            }

            WorkoutId.AIR_FORCE_FITNESS_TEST.id -> {
                FitnessTestResult(rating = helper.getAirForceTestResult())
            }

            WorkoutId.NAVY_FITNESS_TEST.id -> {
                FitnessTestResult(rating = helper.getNavyTestResult())
            }

            WorkoutId.WFI_FITNESS_TEST.id -> {
                FitnessTestResult(
                    info = listOf(helper.getWfiStatus())
                )
            }

            WorkoutId.FTP_TEST_20.id, WorkoutId.FTP_TEST_8.id -> {
                FitnessTestResult(rating = helper.getFtpScore())
            }

            WorkoutId.MAX_HR_TEST.id -> {
                FitnessTestResult(rating = helper.getMaxHRScore())
            }

            WorkoutId.STEPPER_FITNESS_TEST.id -> {
                FitnessTestResult(rating = helper.getStepperRating())
            }

            else -> null
        }
    }

    override fun getSweatScore(stage: SummaryStage): SweatScoreList? {
        val summaryData = DataMatrix.getParcelable(
            WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE,
            SummaryData::class.java
        )

        val splits = if (summaryData == null) listOf() else getSummaryStage(
            summaryData,
            stage
        ).statsSprint8.sweatScores

        return SweatScoreList(splits)
    }

    override fun getWorkoutIntervalStat(stat: SummaryStat): List<Float> {
        val summaryList = DataMatrix.getParcelable(
            WorkoutKeys.DATA_INTERVAL_LIST_PARCELABLE,
            WorkoutSummaryList::class.java
        ) ?: return listOf()
        return summaryList.workoutSummaries.map { getStatFromSummary(it, stat) }
    }

    override val myZoneTimeInZones = object : DataValueListener<MyZoneTimeInZones?> {
        // DATA_HAVE_MYZONE_TIME_IN_ZONES_BOOL is set when the data becomes valid or invalid
        // DATA_MYZONE_TIME_IN_ZONES_PARCELABLE is the last reported data
        override val value: MyZoneTimeInZones?
            get() = getMyZoneTimeInZones()

        override fun listen(onChange: (MyZoneTimeInZones?) -> Unit) {
            manager.addBoolListener(HALKeys.DATA_HAVE_MYZONE_TIME_IN_ZONES_BOOL, {
                onChange(getMyZoneTimeInZones())
            }, false)
        }

        override fun listenAndTrigger(onChange: (MyZoneTimeInZones?) -> Unit) {
            manager.addBoolListener(HALKeys.DATA_HAVE_MYZONE_TIME_IN_ZONES_BOOL, {
                onChange(getMyZoneTimeInZones())
            }, true)
        }

    }

    private fun createSummaryStatValues(summary: SummaryData, stat: SummaryStat): SummaryStatValues = SummaryStatValues(
        statType = stat,
        warmupValue = getStatFromSummary(summary.warmupSummary, stat),
        workoutValue = getStatFromSummary(summary.workoutSummary, stat),
        cooldownValue = getStatFromSummary(summary.cooldownSummary, stat),
        totalValue = getStatFromSummary(summary.totalSummary, stat)
    )

    override fun getSummaryStats(stats: List<SummaryStat>): List<SummaryStatValues> {
        val summary = DataMatrix.getParcelable(
            WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE,
            SummaryData::class.java
        )
        if (summary == null) {
            Log.e("MatrixWorkoutSummaryModel", "Could not find a workout summary to use!")
            return listOf()
        }

        val statValueList = mutableListOf<SummaryStatValues>()
        stats.forEach { stat ->
            if (stat == CALORIES) {
                val activeCalories = getStatFromSummary(summary.totalSummary, ACTIVE_CALORIES)
                if (activeCalories > 0) statValueList.add(createSummaryStatValues(summary, ACTIVE_CALORIES))
            }
            statValueList.add(createSummaryStatValues(summary, stat))
        }

        return statValueList
    }

    override fun getDefaultDisplayedQuickStats(): List<SummaryStat> {
        val frameConfig = FrameConfigManager.getInstance().frameConfig
        val name: String = frameConfig.machineClassType.name
        val frameType = FrameType.fromString(name)
        val showWatts = !frameConfig.WATTS_INACCURATE

        return when (frameType) {
            FrameType.Treadmill -> listOf(
                TIME_ELAPSED,
                DISTANCE,
                AVERAGE_SPEED,
                AVERAGE_INCLINE,
                CALORIES
            )

            FrameType.Ascent -> listOf(
                TIME_ELAPSED,
                DISTANCE,
                CALORIES,
                AVERAGE_INCLINE,
                AVERAGE_WATTS
            )

            FrameType.Bike, FrameType.SpinBike, FrameType.Elliptical -> listOfNotNull(
                TIME_ELAPSED,
                DISTANCE,
                CALORIES,
                AVERAGE_WATTS.takeIf { showWatts })

            FrameType.Climbmill, FrameType.Stepper -> listOf(TIME_ELAPSED, CALORIES, STEPS, FLOORS)
        }
    }

    override fun getDefaultDisplayedFullStats(): List<SummaryStat> {
        val frameConfig = FrameConfigManager.getInstance().frameConfig
        val name: String = frameConfig.machineClassType.name
        val frameType = FrameType.fromString(name)
        val showWatts = !frameConfig.WATTS_INACCURATE

        return when (frameType) {
            FrameType.Treadmill -> listOfNotNull(
                TIME_ELAPSED,
                DISTANCE,
                AVERAGE_SPEED,
                AVERAGE_INCLINE,
                CALORIES,
                AVERAGE_PACE,
                AVERAGE_WATTS.takeIf { showWatts },
                AVERAGE_METS,
                AVERAGE_HEART_RATE,
                PEAK_SPEED_KPH,
                PEAK_WATTS.takeIf { showWatts },
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
                AVERAGE_WATTS.takeIf { showWatts },
                AVERAGE_PACE,
                AVERAGE_SPEED,
                AVERAGE_METS,
                AVERAGE_RPM,
                AVERAGE_HEART_RATE,
                AVERAGE_RESISTANCE,
                PEAK_SPEED_KPH,
                PEAK_WATTS.takeIf { showWatts },
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
                AVERAGE_WATTS.takeIf { showWatts },
                AVERAGE_PACE,
                AVERAGE_SPEED,
                AVERAGE_METS,
                AVERAGE_RPM,
                AVERAGE_HEART_RATE,
                AVERAGE_RESISTANCE,
                PEAK_SPEED_KPH,
                PEAK_WATTS.takeIf { showWatts },
                PEAK_METS,
                PEAK_RPM,
                PEAK_HEART_RATE,
                PEAK_RESISTANCE
            )

            FrameType.Climbmill, FrameType.Stepper -> listOfNotNull(
                TIME_ELAPSED, CALORIES, STEPS, FLOORS,
                AVERAGE_SPM, AVERAGE_WATTS.takeIf { showWatts }, AVERAGE_METS, AVERAGE_HEART_RATE,
                PEAK_SPM, PEAK_WATTS.takeIf { showWatts }, PEAK_METS, PEAK_HEART_RATE
            )
        }
    }

    override fun getQuickStats(): List<QuickStatData> {
        val quickStatsToShow = getDefaultDisplayedQuickStats()
        val statValues = getSummaryStats(quickStatsToShow)
        val quickStats = mutableListOf<QuickStatData>()
        val useMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)
        statValues.forEach { statValues ->
            val quickStatData = getDefaultQuickStatData(statValues, useMetric)
            quickStatData?.let { quickStats.add(it) }
        }
        return quickStats
    }

    private fun getDefaultQuickStatData(
        statValues: SummaryStatValues,
        isMetric: Boolean
    ): QuickStatData? {
        return when (statValues.statType) {
            TIME_ELAPSED -> QuickStatData(
                com.jht.workoutsummary.R.drawable.ic_time_total_white,
                com.jht.workoutsummary.R.string.total_duration_tc,
                UnitFormatter.formatTimeSeconds(statValues.totalValue.roundToInt()),
                com.jht.workoutsummary.R.string.minutes_lc
            )

            DISTANCE -> QuickStatData(
                com.jht.workoutsummary.R.drawable.ic_distance_white,
                com.jht.workoutsummary.R.string.total_distance_tc,
                UnitFormatter.formatDistance(statValues.totalValue, isMetric),
                if (isMetric) com.jht.workoutsummary.R.string.km_lc else com.jht.workoutsummary.R.string.miles_lc
            )

            AVERAGE_SPEED -> QuickStatData(
                com.jht.workoutsummary.R.drawable.ic_speed_white,
                com.jht.workoutsummary.R.string.avg_speed_tc,
                UnitFormatter.formatSpeed(statValues.totalValue, isMetric),
                if (isMetric) com.jht.workoutsummary.R.string.kph_lc else com.jht.workoutsummary.R.string.mph_lc
            )

            AVERAGE_INCLINE -> QuickStatData(
                com.jht.workoutsummary.R.drawable.ic_incline_white,
                com.jht.workoutsummary.R.string.avg_incline_tc,
                UnitFormatter.formatFloors(statValues.totalValue),
                com.jht.workoutsummary.R.string.percent_lc
            )

            CALORIES -> QuickStatData(
                com.jht.workoutsummary.R.drawable.ic_calories_white,
                com.jht.workoutsummary.R.string.total_calories_tc,
                UnitFormatter.formatCalories(statValues.totalValue),
                com.jht.workoutsummary.R.string.kcal_lc
            )

            AVERAGE_WATTS -> QuickStatData(
                com.jht.workoutsummary.R.drawable.ic_watts_white,
                com.jht.workoutsummary.R.string.power_tc,
                UnitFormatter.formatWatts(statValues.totalValue),
                com.jht.workoutsummary.R.string.avg_watts_tc // TODO - lc is in main to-be-translated right now
            )

            STEPS -> QuickStatData(
                com.jht.workoutsummary.R.drawable.ic_steps_total_white,
                com.jht.workoutsummary.R.string.steps_tc,
                UnitFormatter.formatFloors(statValues.totalValue),
                0 //R.string.dashes_tc
            )

            FLOORS -> QuickStatData(
                com.jht.workoutsummary.R.drawable.ic_floors_white,
                com.jht.workoutsummary.R.string.floors_tc,
                UnitFormatter.formatFloors(statValues.totalValue),
                0 //R.string.dashes_tc
            )

            else -> null
        }
    }

    override fun getFullStats(): List<FullStatData> {
        val statsToShow = getDefaultDisplayedFullStats()
        val statValues = getSummaryStats(statsToShow)
        val stats = mutableListOf<FullStatData>()
        val useMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)
        statValues.forEach { statValues ->
            val statData = getDefaultFullStatData(statValues, useMetric)
            statData?.let { stats.add(it) }
        }
        return stats
    }

    private fun getDefaultFullStatData(
        statValues: SummaryStatValues,
        isMetric: Boolean
    ): FullStatData? {
        fun createStat(labelId: Int, unitId: Int, formatter: (Float) -> String): FullStatData {
            return FullStatData(
                labelId = labelId,
                unitId = unitId,
                warmupText = formatter(statValues.warmupValue),
                workoutText = formatter(statValues.workoutValue),
                cooldownText = formatter(statValues.cooldownValue),
                totalText = formatter(statValues.totalValue),
            )
        }

        val frameType by lazy { FrameConfigManager.getInstance().frameConfig.machineClassType }
        val hasActiveCalories by lazy {
            getWorkoutStat(
                SummaryStat.ACTIVE_CALORIES,
                SummaryStage.TOTAL
            ) > 0f
        }

        return when (statValues.statType) {
            TIME_ELAPSED -> createStat(
                com.jht.workoutsummary.R.string.time_elapsed_tc, 0,
            ) {
                UnitFormatter.formatTimeSeconds(it.roundToInt())
            }

            DISTANCE -> createStat(
                com.jht.workoutsummary.R.string.distance_tc,
                if (isMetric) R.string.km_lc else R.string.miles_lc
            ) {
                UnitFormatter.formatDistance(it, isMetric)
            }

            ELEVATION -> createStat(
                com.jht.workoutsummary.R.string.elevation_tc,
                if (isMetric) R.string.m_lc else R.string.ft_lc
            ) {
                UnitFormatter.formatElevation(it, isMetric)
            }

            STEPS -> createStat(
                com.jht.workoutsummary.R.string.steps_tc, 0
            ) {
                UnitFormatter.formatInt(it.roundToInt())
            }

            FLOORS -> createStat(
                com.jht.workoutsummary.R.string.floors_tc, 0
            ) {
                UnitFormatter.formatFloors(it)
            }

            AVERAGE_SPM -> createStat(
                com.jht.workoutsummary.R.string.average_spm_tc, 0
            ) {
                UnitFormatter.formatInt(it.roundToInt())
            }

            PEAK_SPM -> createStat(
                com.jht.workoutsummary.R.string.peak_spm_tc, 0
            ) {
                UnitFormatter.formatInt(it.roundToInt())
            }

            AVERAGE_SPEED -> createStat(
                com.jht.workoutsummary.R.string.average_speed_tc,
                if (isMetric) R.string.kph_lc else R.string.mph_lc
            ) {
                UnitFormatter.formatSpeed(it, isMetric)
            }

            PEAK_SPEED_KPH -> createStat(
                com.jht.workoutsummary.R.string.peak_speed_tc,
                if (isMetric) R.string.kph_lc else R.string.mph_lc
            ) {
                UnitFormatter.formatSpeed(it, isMetric)
            }

            AVERAGE_PACE -> createStat(
                com.jht.workoutsummary.R.string.average_pace_tc, 0
            ) {
                val secsPerDist =
                    if (isMetric) it.secondsPerKmPaceUnit.toSecondsPerKilometerPace.roundToInt()
                    else it.secondsPerKmPaceUnit.toSecondsPerMilePace.roundToInt()
                UnitFormatter.formatPace(secsPerDist)
            }

            AVERAGE_RPM -> createStat(
                com.jht.workoutsummary.R.string.average_rpm_tc, 0
            ) {
                UnitFormatter.formatInt(it.roundToInt())
            }

            PEAK_RPM -> createStat(
                com.jht.workoutsummary.R.string.peak_rpm_tc, 0
            ) {
                UnitFormatter.formatInt(it.roundToInt())
            }

            AVERAGE_RESISTANCE -> createStat(
                com.jht.workoutsummary.R.string.average_resistance_tc, 0
            ) {
                UnitFormatter.formatInt(it.roundToInt())
            }

            PEAK_RESISTANCE -> createStat(
                com.jht.workoutsummary.R.string.peak_resistance_tc, 0
            ) {
                UnitFormatter.formatInt(it.roundToInt())
            }

            AVERAGE_INCLINE -> createStat(
                com.jht.workoutsummary.R.string.average_incline_tc, 0
            ) {
                UnitFormatter.formatIncline(
                    it,
                    frameType == JHTCommonTypes.MachineClassificationType.Treadmill
                )
            }

            PEAK_INCLINE -> createStat(
                com.jht.workoutsummary.R.string.peak_incline_tc, 0
            ) {
                UnitFormatter.formatIncline(
                    it,
                    frameType == JHTCommonTypes.MachineClassificationType.Treadmill
                )
            }

            AVERAGE_WATTS -> createStat(
                com.jht.workoutsummary.R.string.average_watts_tc, 0
            ) {
                UnitFormatter.formatWatts(it)
            }

            PEAK_WATTS -> createStat(
                com.jht.workoutsummary.R.string.peak_watts_tc, 0
            ) {
                UnitFormatter.formatWatts(it)
            }

            AVERAGE_METS -> createStat(
                com.jht.workoutsummary.R.string.average_mets_tc, 0
            ) {
                UnitFormatter.formatMets(it)
            }

            PEAK_METS -> createStat(
                com.jht.workoutsummary.R.string.peak_mets_tc, 0
            ) {
                UnitFormatter.formatMets(it)
            }

            AVERAGE_HEART_RATE -> createStat(
                com.jht.workoutsummary.R.string.average_heart_rate_tc, 0
            ) {
                UnitFormatter.formatHeartRate(it)
            }

            PEAK_HEART_RATE -> createStat(
                com.jht.workoutsummary.R.string.peak_heart_rate_tc, 0
            ) {
                UnitFormatter.formatHeartRate(it)
            }

            ACTIVE_CALORIES -> createStat(
                com.jht.workoutsummary.R.string.active_calories_tc, 0
            ) {
                UnitFormatter.formatCalories(it)
            }

            CALORIES -> createStat(
                if (hasActiveCalories) com.jht.workoutsummary.R.string.total_calories_tc else com.jht.workoutsummary.R.string.calories_tc,
                0
            ) {
                UnitFormatter.formatCalories(it)
            }

            else -> null
        }
    }

    private fun getMyZoneTimeInZones(): MyZoneTimeInZones? {
        val timeInZones = DataMatrix.getParcelable(
            HALKeys.DATA_MYZONE_TIME_IN_ZONES_PARCELABLE,
            TimeInZones::class.java
        ) ?: return null
        if (!DataMatrix.getBoolean(HALKeys.DATA_HAVE_MYZONE_TIME_IN_ZONES_BOOL)) return null
        // seems to passed as a pair of arrays, to be safe only pair up to least common size
        val minSize = minOf(timeInZones.timeInZone().size, timeInZones.mepsInZone().size)
        val list = mutableListOf<MyZoneInfoPerZone>()
        for (i in 0 until minSize) {
            list.add(MyZoneInfoPerZone(timeInZones.timeInZone()[i], timeInZones.mepsInZone()[i]))
        }
        return MyZoneTimeInZones(list)
    }

}
