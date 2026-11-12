package com.jht.sprint8.model

import com.jht.sprint8.sprint8classic.ui.CustomIntervalData

internal data class CustomWorkoutUIState(
    val customIntervalDataList: List<CustomIntervalData> = emptyList(),
    val selectedSprint: Int = 0,
    val hideSaveWorkoutUi: Boolean = true //CGC-22110 - Custom save functionality removed for now.
)