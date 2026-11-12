package com.jht.chimera.lib.common.log

import android.util.Log
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.formatting.TimeFormat
import com.jht.datamatrixlib.DataMatrix

object ConsoleEvent {
    private const val TAG = "ConsoleEvent"

    @JvmStatic
    fun logEvent(log: String) {
        var message = log
        val runState = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT)
        if (runState != WorkoutKeys.Constants.RUN_STATE_STOP) {
            val elapsedMs = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT)
            message += " at elapsed time ${TimeFormat.formatTime(elapsedMs)}"
        }
        Log.d(TAG, message)
    }
}
