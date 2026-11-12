package com.jht.sprint8.utils

import android.os.Build

/**
 * Represents the workout modes in Sprint8.
 * - [Classic]: The traditional workout mode with classic UI and behavior.
 * - [Immersive]: The Sprint8 New/Plus workout mode with new UI and features.
 */
internal enum class WorkoutMode {
    Classic,
    Immersive
}

internal object Sprint8TypeData {
    //Default Sprint 8 to New
    var selectedMode: WorkoutMode = WorkoutMode.Immersive
    var allowSelection: Boolean = true

    init {
        // CGC-22991 Temporarily restrict to classic on pre-vortex era consoles (vortex runs android 14)
        // If not temporary, might consider a console tech level integer in models, or some system to configure sprint8 specifically from system
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            selectedMode = WorkoutMode.Immersive
            allowSelection = true
        } else {
            selectedMode = WorkoutMode.Classic
            allowSelection = false
        }
    }
}