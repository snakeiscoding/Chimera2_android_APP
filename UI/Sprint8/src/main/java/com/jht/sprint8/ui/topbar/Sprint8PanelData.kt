package com.jht.sprint8.ui.topbar

import com.jht.interfaces.ui.BasePanelData
import kotlinx.android.parcel.Parcelize

/**
 * Data class representing the visibility state of the Sprint8 panel components.
 *
 * @param isSprint8TopBarVisible Boolean indicating whether the top bar of the
 * Sprint8 screen should be visible.
 */
@Parcelize
data class Sprint8PanelData(
    val isSprint8TopBarVisible: Boolean
) : BasePanelData(SPRINT8_UUID)