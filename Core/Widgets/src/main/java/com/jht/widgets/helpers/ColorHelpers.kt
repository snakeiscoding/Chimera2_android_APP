package com.jht.widgets.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix


/** ColorFilter to invert colors in an image */
private val invertColorFilter = ColorFilter.colorMatrix(ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )
))

/** returns if given color is above half luminance */
fun isColorLight(color: Color): Boolean {
    // is luminance calculation needed? does it take into account gamma? can we just threshold an average?
    val luminance: Double = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.5
}

/** if color is dark, return inversion filter */
@Composable
fun colorInvertIfDark(color: Color): ColorFilter? = if (isColorLight(color)) null else invertColorFilter
