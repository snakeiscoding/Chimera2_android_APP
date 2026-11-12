package com.jht.mock.interfaces.models

import com.jht.enums.SummaryStage
import com.jht.enums.SummaryStat
import com.jht.interfaces.models.FitnessTestResult
import com.jht.interfaces.models.FullStatData
import com.jht.interfaces.models.MyZoneTimeInZones
import com.jht.interfaces.models.QuickStatData
import com.jht.interfaces.models.SummaryStatValues
import com.jht.interfaces.models.SweatScoreList
import com.jht.interfaces.models.WorkoutSplits
import com.jht.interfaces.models.WorkoutSummaryModel
import com.jht.mock.interfaces.MockDataValue
import com.jht.mock.interfaces.MockStringValue

class MockWorkoutSummaryModel(
    override val lastProgramUuid: MockStringValue = MockStringValue(),
) : WorkoutSummaryModel() {
    var workoutSplitsData: WorkoutSplits? = null
    var fitnessTestResultData: FitnessTestResult? = null
    var myZoneTimeInZonesData: MyZoneTimeInZones? = null
    var sweatScoreList: SweatScoreList? =null

    override fun hadWorkoutStage(stage: SummaryStage): Boolean {
        return true
    }

    override fun getWorkoutStat(stat: SummaryStat, stage: SummaryStage): Float {
        return 0f
    }

    override fun getWorkoutSplits(stage: SummaryStage): WorkoutSplits? {
        return workoutSplitsData
    }

    override fun getFitnessTestResult(): FitnessTestResult? {
        return fitnessTestResultData
    }

    override fun getWorkoutIntervalStat(stat: SummaryStat): List<Float> = listOf()

    override val myZoneTimeInZones = MockDataValue(myZoneTimeInZonesData)

    override fun getSummaryStats(stats: List<SummaryStat>): List<SummaryStatValues> {
        return listOf()
    }

    override fun getDefaultDisplayedQuickStats(): List<SummaryStat> {
        return listOf()
    }

    override fun getDefaultDisplayedFullStats(): List<SummaryStat> {
        return listOf()
    }

    override fun getQuickStats(): List<QuickStatData> {
        return listOf()
    }

    override fun getFullStats(): List<FullStatData> {
        return listOf()
    }

    override fun close() {
        // do nothing
    }

    override fun getSweatScore(stage: SummaryStage): SweatScoreList? {
        return sweatScoreList
    }
}