package com.jht.progression.ui

import androidx.compose.ui.graphics.Color
import com.jht.enums.IntervalType

class MmColors {
    companion object {
        val deepBlue = Color(0xFF001423)

        // Colors used on Journey screen
        val futuristicBlue = Color(0xFF00DBFF)
        val futureGray = Color(0xFF7A7979)
        val missedRed = Color(0xFFF23F3E)
        val lightGreen = Color(0xFF06C258)


        val zone1Color = Color(0xFF7169FF)
        val zone2Color = Color(0xFF00DBFF)
        val zone3Color = Color(0xFF33DB95)
        val zone4Color = Color(0xFFFDB323)
        val zone5Color = Color(0xFFF5502B)
    }
}

/**
 * Gets the color associated with an interval type
 */
fun getIntervalTypeColor(intervalType: IntervalType?): Color {
    return when (intervalType) {
        IntervalType.ZONE_1 -> MmColors.zone1Color
        IntervalType.ZONE_2 -> MmColors.zone2Color
        IntervalType.ZONE_3 -> MmColors.zone3Color
        IntervalType.ZONE_4 -> MmColors.zone4Color
        IntervalType.ZONE_5 -> MmColors.zone5Color
        IntervalType.ASSESSMENT -> MmColors.zone4Color
        else -> MmColors.zone1Color
    }
}