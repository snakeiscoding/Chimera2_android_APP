package com.jht.chimera.service.ui.panels

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.ChimeraEnums
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.UiPanelType


/**
 * Gets the [WindowManager.LayoutParams] to define placement and size for a specific [UiPanelType].
 */
fun getPanelTypeLayoutParams(context: Context, type: UiPanelType): WindowManager.LayoutParams {
    return when (type) {
        UiPanelType.TopPanel, UiPanelType.ValueUpdatePanelOverride -> getTopBarPanelParams(context)
    }
}

/**
 * Placement for a standard top bar in the system. This will be sized differently depending on
 * whether the screen is fullscreen or not.
 */
private fun getTopBarPanelParams(context: Context): WindowManager.LayoutParams {
    val isFullscreen = DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT) == UiServiceKeys.WorkoutPanelState.FULLSCREEN

    val height = context.resources.getDimension(R.dimen.top_bar_height).toInt()
    val width = if (isFullscreen) {
        WindowManager.LayoutParams.MATCH_PARENT
    } else {
        context.resources.getDimension(R.dimen.app_container_width).toInt()
    }

    val viewParams = WindowManager.LayoutParams(
        width, height,
        ChimeraEnums.TYPE_SYSTEM_ERROR,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT
    )
    viewParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
    viewParams.x = 0
    viewParams.y = 0
    return viewParams
}