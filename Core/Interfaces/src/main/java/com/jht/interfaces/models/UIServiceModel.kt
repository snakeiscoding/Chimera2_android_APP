package com.jht.interfaces.models

import android.graphics.Rect
import com.jht.interfaces.BaseModel
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.ParcelableValueListener
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelHolder
import com.jht.interfaces.ui.MessageCenterMessage

/**
 * Provides a model for exposing and controlling UI service
 */
abstract class UIServiceModel : BaseModel {
    /** Indicates whether the UI is in full screen mode */
    abstract val stateFullScreen: BoolValueListener
    /** Indicates whether the UI is in workout idle mode */
    abstract val stateIdle: BoolValueListener
    /** Indicates whether the UI is in workout summary mode */
    abstract val stateSummary: BoolValueListener

    /** Signals that UI should toggle between regular and full screen modes. If [showFullscreenOverride] is not null, sets fullscreen accordingly. */
    abstract fun toggleFullscreen(showFullscreenOverride: Boolean? = null)

    /** Adds a UI panel to UI Service. */
    abstract fun addUiPanel(panelHolder: InnerPanelHolder)

    /** Sends data to a panel added from a module */
    abstract fun sendUiPanelData(panelData: BasePanelData)

    /** Sends messages to be handled by MessageCenterManager */
    open fun sendMessageCenterMessage(message: MessageCenterMessage) {}

    /** Remove messages from MessageCenterManager with the given tag string */
    open fun removeMessageCenterMessageByTag(tag: String) {}

    /** Provides the usable view port area left by overlay panels, if full area is available (no overlays) returns empty */
    abstract val overlayViewportPixels: ParcelableValueListener<Rect>
}