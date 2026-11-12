package com.jht.widgets.helpers

import androidx.compose.ui.graphics.Color
import com.jht.widgets.R

/*
    Trying to centralize MyZone specific UI stuff so we can keep better track of it
    We seem to have different "zone" colors being used in different places (bottom bar / workout summary), is there a reason?
*/

// When in the red zone the red icon is washed out, so switch to black
fun getMyZoneIcon(hrPercent: Int): Int =
    if (hrPercent < 90) {
        R.drawable.logo_myzone
    } else { // MyZone red
        R.drawable.logo_myzone_black
    }

// These colors/zones are off the MyZone website https://www.myzone.org/status-rankings
fun getMyZoneTextColor(hrPercent: Int): Color =
    if (hrPercent < 50) { // MyZone light gray
        Color(0xffed2224) // Same as red color according to MyZone
    } else if (hrPercent < 60) { // MyZone gray
        Color.White
    } else if (hrPercent < 70) { // MyZone blue
        Color.White
    } else if (hrPercent < 80) { // MyZone green
        Color.White
    } else if (hrPercent < 90) { // MyZone yellow
        Color.Black // Black to increase contrast
    } else { // MyZone red
        Color.White
    }

// These colors/zones are off the MyZone website https://www.myzone.org/status-rankings
fun getMyZoneBackgroundColor(hrPercent: Int): Color =
    if (hrPercent < 50) { // MyZone light gray
        Color(0xffdddddd)
    } else if (hrPercent < 60) { // MyZone gray
        Color(0xff757679)
    } else if (hrPercent < 70) { // MyZone blue
        Color(0xff0031f9)
    } else if (hrPercent < 80) { // MyZone green
        Color(0xff058b44)
    } else if (hrPercent < 90) { // MyZone yellow
        Color(0xfffff200)
    } else { // MyZone red
        Color(0xffed2224)
    }

// just copied this from old workout summary screen UI
fun getMyZoneColor(index: Int, defaultColor: Color = Color.Unspecified): Color =
    when (index) {
        0 -> Color(0xFFDDDDDD)
        1 -> Color(0xFF6E6E6F)
        2 -> Color(0xFF335AF9)
        3 -> Color(0xFF33A169)
        4 -> Color(0xFFEFE84E)
        5 -> Color(0xFFFE3339)
        else -> defaultColor
    }
