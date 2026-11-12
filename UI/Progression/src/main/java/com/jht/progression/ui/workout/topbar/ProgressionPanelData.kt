package com.jht.progression.ui.workout.topbar

import com.jht.interfaces.ui.BasePanelData
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProgressionPanelData(
    val isProgressionAppVisible: Boolean = false
) : BasePanelData(MM_TOP_BAR_ID)