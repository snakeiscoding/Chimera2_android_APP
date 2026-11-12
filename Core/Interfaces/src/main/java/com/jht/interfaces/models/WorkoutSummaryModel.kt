package com.jht.interfaces.models

import com.jht.enums.SummaryStage
import com.jht.enums.SummaryStat
import com.jht.interfaces.BaseModel
import com.jht.interfaces.DataValueListener
import com.jht.interfaces.StringValueListener

/**
 * Model for getting workout summary information after a workout has been completed.
 */
abstract class WorkoutSummaryModel : BaseModel {
    /** Provides method to determine if workout had a given stage */
    abstract fun hadWorkoutStage(stage: SummaryStage): Boolean

    /** Provides method to retrieve a given workout summary stat for a given summary stage */
    abstract fun getWorkoutStat(stat: SummaryStat, stage: SummaryStage): Float

    /** Provides method to retrieve information on workout splits/laps (null if system does not support split or lap information) */
    abstract fun getWorkoutSplits(stage: SummaryStage): WorkoutSplits?

    /** Provides method to retrieve fitness test results (NULL if none to report) */
    abstract fun getFitnessTestResult(): FitnessTestResult?

    abstract fun getSweatScore(stage: SummaryStage): SweatScoreList?

    /** Provides method to retrieve a given workout summary stat from the interval summary list */
    abstract fun getWorkoutIntervalStat(stat: SummaryStat): List<Float>

    /** Retrieves MyZone time-in-zones data (null if no data). Due to async nature of device reporting, this is a listener. */
    abstract val myZoneTimeInZones: DataValueListener<MyZoneTimeInZones?>

    /** Gets a list of stats values for a single workout. Cuts down on datamatrix calls vs doing it one by one. */
    abstract fun getSummaryStats(stats: List<SummaryStat>): List<SummaryStatValues>

    /** Gets a list of stats that are generally shown as "quick stats" in the summary. Changes depending on the current frame. */
    abstract fun getDefaultDisplayedQuickStats(): List<SummaryStat>

    /** Gets a list of stats that are generally shown in the full stat list in the summary. Changes depending on the current frame. */
    abstract fun getDefaultDisplayedFullStats(): List<SummaryStat>

    /** Gets data needed to show quick stats for a usual implementation of stats */
    abstract fun getQuickStats(): List<QuickStatData>

    /** Gets data needed to show full stats for a usual implementation of stats */
    abstract fun getFullStats(): List<FullStatData>

    /**
     * Program uuid sent with the last workout's completion message, e.g. `manual:<uuid>`
     *
     * Empty until that message is built, which can land after the summary opens, and stays empty
     * for workouts that were never reported (offline). Cleared when the next workout starts.
     */
    abstract val lastProgramUuid: StringValueListener
}

/** Data class to pass values for a single workout stat. Null values mean that no values were recorded for that stage. */
data class SummaryStatValues(
    val statType: SummaryStat,
    val warmupValue: Float,
    val workoutValue: Float,
    val cooldownValue: Float,
    val totalValue: Float,
)

/** Data class to pass values used for "quick stat" implementations that are common in our summaries */
data class QuickStatData(
    val iconId: Int,
    val labelId: Int,
    val value: String,
    val unitId: Int,
)

/** Data class to pass values used for full stat implementations that are common in our summaries */
data class FullStatData(
    val labelId: Int,
    val unitId: Int,
    val warmupText: String,
    val workoutText: String,
    val cooldownText: String,
    val totalText: String,
)



/** Represents Fitness Test Results (Keeps strings separate in case UI needs to format a certain way) */
class FitnessTestResult(
    /** Presents an overall rating or score */
    val rating: String = "",
    /** Lines of additional information */
    val info: List<String> = listOf(),
    /** Extra stats as a pair of label and value */
    val stats: List<Pair<String, String>> = listOf(),
) {
    fun getReport(delimiterLine: String = "\n", delimiterStat: String = ": "): String {
        val sections = listOfNotNull(
            rating.takeIf { rating.isNotEmpty() },
            info.joinToString(delimiterLine).takeIf { info.isNotEmpty() },
            stats.joinToString(delimiterLine) { it.first + delimiterStat + it.second }
                .takeIf { stats.isNotEmpty() },
        )
        return sections.joinToString(delimiterLine)
    }
}

/** Contains collective information for workout splits/laps. Default construction is empty. */
class WorkoutSplits(
    val splits: List<WorkoutSplit> = listOf(),
    val lapsTotal: Int = 0,
    val lapBestTimeMs: Int = 0,
) {
    fun hasSplits() = splits.isNotEmpty()
}

/** Contains information for a workout split */
class WorkoutSplit(
    val timeMs: Int,
    val distanceKm: Float,
)

/** Contains MyZone time-in-zones as seconds-to-MEPs mapping */
class MyZoneTimeInZones(
    val zones: List<MyZoneInfoPerZone>,
) {
    fun getDurationSeconds() = zones.sumOf { it.seconds }
}

/** Contains per-zone stats for MyZone stats */
class MyZoneInfoPerZone(
    val seconds: Int,
    val meps: Int,
)

/** Contains collective information for workout sweat score.Default construction is empty. */
class SweatScoreList(
    val sweatScoreList: List<Float?> = listOf(),
) {
    fun hasSplits() = sweatScoreList.isNotEmpty()
}
