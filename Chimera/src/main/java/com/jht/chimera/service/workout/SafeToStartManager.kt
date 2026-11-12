package com.jht.chimera.service.workout

import androidx.annotation.StringRes
import com.jht.chimera.R
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage
import com.jht.datamatrixlib.DataMatrix
import java.util.EnumSet

enum class SafeToStartFlags(val bitValue: Int, @StringRes val stringId: Int) {
    //Show one order
    //TM frame start limited
    TM_MAIN_MOTOR_STATUS(1 shl 1, R.string.start_limited_tm_main_motor_tc),
    TM_INCLINE_MOVING(1 shl 2, R.string.start_limited_tm_incline_moving_tc),
    TM_RPM_ENCODER_STATUS(1 shl 3, R.string.start_limited_tm_rpm_encoder_tc),
    TM_RPM_VALUE_STATUS(1 shl 4, R.string.start_limited_tm_rpm_value_tc),

    ;
    // string retrieved when called, uses locale at time it is called
    fun getMessage(): String = ContextUtil.getString(this.stringId)
    companion object {
        // provide check if any flags are set
        @JvmStatic
        fun isSafe(bitmask: Int): Boolean = (bitmask == 0)
        // to get from DM key integer
        @JvmStatic
        fun fromInt(bitmask: Int): EnumSet<SafeToStartFlags> {
            val flags = EnumSet.noneOf(SafeToStartFlags::class.java)
            SafeToStartFlags.entries.forEach {
                if (it.bitValue and bitmask != 0) flags.add(it)
            }
            return flags
        }
        // to set to DM key integer
        @JvmStatic
        fun toInt(flags: EnumSet<SafeToStartFlags>): Int {
            var bitmask = 0
            flags.forEach { bitmask = bitmask or it.bitValue }
            return bitmask
        }

        /** Checks [bitmask] if safe to start.
         * If not safe, posts notification and returns false.
         * If no bitmask given, reads from DM.
         * @param bitmask
         */
        @JvmStatic
        fun checkSafeToStartWithNotification(bitmask: Int): Boolean {
            if (!SafeToStartFlags.isSafe(bitmask)) {
                val flags = SafeToStartFlags.fromInt(bitmask)
                // this line gets the user message for first flags
                 val stringNotSafeToStart = flags.firstOrNull()?.getMessage()
                 if (!stringNotSafeToStart.isNullOrEmpty()) {
                         val message = MessageCenterMessage("", stringNotSafeToStart, 10)
                         DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message)
                 }
                return false
            }
            return true
        }
    }
}
