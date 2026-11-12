package com.jht.widgets.numpad

import com.jht.unit.UnitFormatter
import kotlin.math.roundToInt

private const val MAX_TIME_SECONDS_FLOAT = 359999f
private const val MAX_USER_ENTRY_FLOAT = 9999999f

enum class NumPadDisplay {
    Passcode,
    Number,
    TimeValueSeconds,
    TimeValueMinutes,
    ; // -----

    fun isTime() = (this == TimeValueMinutes || this == TimeValueSeconds)

    internal fun format(value: Float, decimalPlaces: Int = 0, forceHours: Boolean = false): String = when (this) {
        TimeValueSeconds -> UnitFormatter.formatTimeSeconds(value.roundToInt(), forceHours)
        TimeValueMinutes -> UnitFormatter.formatTimeSeconds((value * 60).roundToInt(), forceHours)
        else -> UnitFormatter.formatDecimal(decimalPlaces, value)
    }

    internal fun needHours(value: Float): Boolean = when (this) {
        TimeValueSeconds -> value >= 3600f
        TimeValueMinutes -> value >= 60f
        else -> false
    }

    internal fun getMax(): Float = when (this) {
        TimeValueSeconds -> MAX_TIME_SECONDS_FLOAT
        TimeValueMinutes -> MAX_TIME_SECONDS_FLOAT / 60
        else -> MAX_USER_ENTRY_FLOAT
    }
}
