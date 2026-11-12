package com.jht.customhiit.ui.topbar

import com.jht.customhiit.common.models.HIITWorkoutMap
import com.jht.interfaces.ui.BasePanelData
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CustomHIITPanelData(
    val isCustomHIITVisible: Boolean,
    val map: HIITWorkoutMap,
): BasePanelData(CUSTOM_HII_TOP_BAR_PANEL_ID)
