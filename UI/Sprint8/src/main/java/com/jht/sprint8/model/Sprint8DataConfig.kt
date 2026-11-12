package com.jht.sprint8.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_WARM_UP_COLOR

/**
 * Configuration data for the Sprint8 workout screen.
 *
 * @property backgroundColor The main background color of the Sprint8 screen. Defaults to [Color.Blue].
 * @property classicHeartRateAndRpmBgColor Background color for classic heart rate and RPM indicators. Defaults to [Color.White].
 * @property getBorderColors A list of colors used for borders in the UI.
 * Defaults to an empty list.
 * @property workoutTitle The title of the workout phase. Defaults to an empty string.
 * @property sprint8NewImage Drawable resource ID for the main image displayed on the screen.
 * Defaults to `R.drawable.ic_mountains_warm_up`.
 * @property sprint8NewColors Primary color used for highlighting the Sprint8 phase.
 * Defaults to [SPRINT8_NEW_WARM_UP_COLOR].
 * @property sprint8NewForwardMotionSpeed Controls the speed of forward motion animation on the screen.
 * Defaults to `2L`.
 * @property getReadyConfig Configuration for the "Get Ready" message, including visibility and blinking.
 * Defaults to a default [GetReadyConfig] instance.
 * @property isSprint8KeyPadBtnVisible Indicates whether the Sprint8 keypad button is visible.
 * Defaults to `false`.
 */
@Immutable
data class Sprint8DataConfig(
    val backgroundColor: Color = Color.Blue,
    val classicHeartRateAndRpmBgColor: Color = Color.White,
    val getBorderColors: List<Color> = emptyList(),
    val workoutTitle: String = "",
    val sprint8NewImage : Int = R.drawable.ic_mountains_warm_up,
    val sprint8NewColors : Color = SPRINT8_NEW_WARM_UP_COLOR,
    val sprint8NewForwardMotionSpeed: Long = 2,
    val getReadyConfig: GetReadyConfig = GetReadyConfig(),
    val isSprint8KeyPadBtnVisible: Boolean = false,
    val keyPadConfirmBtnColor: Color = Color.White,
)
