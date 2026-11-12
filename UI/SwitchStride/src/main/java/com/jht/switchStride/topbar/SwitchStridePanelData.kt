package com.jht.switchStride.topbar

import com.jht.interfaces.ui.BasePanelData
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class SwitchStridePanelData (
    val isSwitchStrideTopBarVisible: Boolean
): BasePanelData(SWITCH_STRIDE_UUID)