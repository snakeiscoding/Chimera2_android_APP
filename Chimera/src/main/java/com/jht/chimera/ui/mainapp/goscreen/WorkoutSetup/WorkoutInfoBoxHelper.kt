package com.jht.chimera.ui.mainapp.goscreen.WorkoutSetup

import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.androidcommonalgorithms.type.JHTCommonTypes.MachineClassificationType
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.formatting.BasicNumberFormat
import com.jht.chimera.lib.common.formatting.SpeedFormat
import com.jht.datamatrixlib.DataMatrix
import com.jht.workoutcontrollerlib.model.DataModel
import com.jht.workoutcontrollerlib.model.WorkoutModel
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutScriptVariables


class WorkoutStatInfo(var name: String, var average: String, var max: String)


// Helpers for getting level info for the little box that shows on the workout selection screen
// moving them out of WorkoutSelectionFragment so I can have an easier time getting it into the API
fun getWorkoutInfo(stat: String, stageChanges: List<Float>): WorkoutStatInfo? {
    var statInfo: WorkoutStatInfo? = null
    if (stageChanges.size > 0) {
        val max = getMaxOfList(stageChanges)
        if (max > 0) {
            statInfo =
                createWorkoutInfo(stat, averageList(stageChanges), getMaxOfList(stageChanges))
        }
    }
    return statInfo
}

private fun createWorkoutInfo(stat: String, average: Float, max: Float): WorkoutStatInfo? {
    val averageString: String
    val maxString: String
    return when (stat) {
        WorkoutScriptVariables.RESISTANCE -> {
            averageString = BasicNumberFormat.format(average, 0)
            maxString = BasicNumberFormat.format(max, 0)
            var resistanceString = ContextUtil.getString(R.string.resistance_tc)
            if (FrameConfigManager.getInstance().frameConfig.machineClassType == MachineClassificationType.Climbmill) {
                resistanceString = ContextUtil.getString(R.string.level_tc)
            }
            WorkoutStatInfo(resistanceString, averageString, maxString)
        }

        WorkoutScriptVariables.SPEED -> {
            val unitString =
                if (DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)) ContextUtil.getString(
                    R.string.kph_lc
                ) else ContextUtil.getString(R.string.mph_lc)
            averageString = SpeedFormat.formatSpeedToUserSetting(average) + " " + unitString
            maxString = SpeedFormat.formatSpeedToUserSetting(max) + " " + unitString
            WorkoutStatInfo(ContextUtil.getString(R.string.speed_tc), averageString, maxString)
        }

        WorkoutScriptVariables.INCLINE -> {
            val config = FrameConfigManager.getInstance().frameConfig
            if (config.machineClassType == JHTCommonTypes.MachineClassificationType.Ascent) {
                averageString = BasicNumberFormat.format(average, 0) + "%"
                maxString = BasicNumberFormat.format(max, 0) + "%"
            } else {
                averageString = BasicNumberFormat.format(average, 1) + "%"
                maxString = BasicNumberFormat.format(max, 1) + "%"
            }
            WorkoutStatInfo(ContextUtil.getString(R.string.incline_tc), averageString, maxString)
        }

        else ->                 // Do nothing
            null
    }
}

private fun averageList(floatList: List<Float>): Float {
    var sum = 0f
    for (value in floatList) {
        sum += value
    }
    return sum / floatList.size
}

private fun getMaxOfList(floatList: List<Float>): Float {
    var max = 0f
    for (value in floatList) {
        if (value > max) max = value
    }
    return max
}

fun getStatChangesForWorkout(level: Int, stat: String, workoutModel: WorkoutModel): List<Float> {
    val workoutStages = workoutModel.stages
    val valueChanges: MutableList<Float> = ArrayList()
    for (stageModel in workoutStages) {
        val stageName = stageModel.name

        // We don't care about warmup or cooldown stages
        if (stageName == "warmup" || stageName == "cooldown") {
            continue
        }
        valueChanges.addAll(getChangesForStat(stat, stageModel.defaultValues))
        if (stageModel.levels.size > level) {
            val levelModel = stageModel.levels[level]
            valueChanges.addAll(getChangesForStat(stat, levelModel.defaultValues))
            for (intervalModel in levelModel.intervals) {
                valueChanges.addAll(getChangesForStat(stat, intervalModel.defaultValues))
            }
        }
    }
    return valueChanges
}

private fun getChangesForStat(stat: String, dataList: List<DataModel>): List<Float> {
    val changes: MutableList<Float> = ArrayList()
    for (data in dataList) {
        if (data.parameter == stat) {
            changes.add(data.value)
        }
    }
    return changes
}

