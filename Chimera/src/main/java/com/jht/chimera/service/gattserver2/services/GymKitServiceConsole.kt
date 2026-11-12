package com.jht.chimera.service.gattserver2.services

import com.jht.androidcommonalgorithms.type.JHTCommonTypes
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.log.ConsoleEvent
import com.jht.datamatrixlib.DataMatrix
import com.jht.gattserver2.AppleWatch
import com.jht.gattserver2.IGattServer
import com.jht.gattserver2.WatchState
import com.jht.gattserver2.services.GymKitService

class GymKitServiceConsole(server: IGattServer, appleWatch: AppleWatch) : GymKitService(server, appleWatch) {
    private val dataMatrix = DataMatrix()
    override fun start() {
        super.start()
        if (FrameConfigManager.getInstance().frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            dataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT) { kph: Float -> updateInstantaneousSpeed(kph) }
        } else {
            dataMatrix.createFloatAdapter(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT) { kph: Float -> updateInstantaneousSpeed(kph) }
        }
        dataMatrix.createFloatAdapter(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT) { kph: Float -> updateAverageSpeed(kph) }
    }

    override fun stop() {
        dataMatrix.finish()
        super.stop()
    }

    override fun onAppleWatchConnecting() {}

    override fun onAppleWatchDisconnecting() {}

    override fun onAppleWatchState(state: WatchState) {
        when (state) {
            WatchState.Connecting -> {
                val current = DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT).toDouble()
                onActiveCalories(current)
                onTotalCalories(current)
                //Temp workaround to the calories key organization.
                // We should review the external parameter pathways during the workout controller / stats review
                DataMatrix.putFloat(WorkoutKeys.DATA_ACTIVE_CALORIES_FLOAT, current.toFloat())
            }
            WatchState.Disconnected -> {
                //Only reset the watch values on a full disconnection. This check is to prevent resetting if the watch encounters a temporary disconnect
                onHeartRate(0)
                //Calories clearing is a negative number, as 0 will override the summary screen
                onActiveCalories(-1.0)
                onTotalCalories(-1.0)
                onAverageHeartRate(0)
            }
            else -> {}
        }
    }

    override fun onActiveCalories(activeCalories: Double) {
        ConsoleEvent.logEvent("GymKit Active Calories: $activeCalories")
        DataMatrix.putInt(HALKeys.DATA_GYM_KIT_CALORIES_ACTIVE_INT, activeCalories.toInt())
    }

    override fun onTotalCalories(totalCalories: Double) {
        ConsoleEvent.logEvent("GymKit Total Calories: $totalCalories")
        DataMatrix.putInt(HALKeys.DATA_GYM_KIT_CALORIES_TOTAL_INT, totalCalories.toInt())
    }

    override fun onHeartRate(heartRate: Int) {
        ConsoleEvent.logEvent("GymKit Heart Rate: $heartRate")
        DataMatrix.putInt(HALKeys.ACTION_UPDATE_GYM_KIT_HEART_RATE_INT, heartRate)
    }

    override fun onAverageHeartRate(averageHeartRate: Int) {
        ConsoleEvent.logEvent("GymKit Average Heart Rate: $averageHeartRate")
        DataMatrix.putInt(HALKeys.DATA_GYM_KIT_AVE_HEART_RATE_INT, averageHeartRate)
    }
}
