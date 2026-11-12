@file:JvmName("PanelType")
package com.jht.mastboard.lib

/**
 * The available panel types.
 *
 * fixme: There will be more types depending on the values that are needed. I'm not actually sure
 *  this is needed here either.
 */
enum class PanelType {
    MatrixLogo,
    WatchConnectionLogo,
    FeaturedWorkouts,
    GoButton,
    WorkoutControls,
    Blank,
    HomeButton
}