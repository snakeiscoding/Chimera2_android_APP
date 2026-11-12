package com.jht.interfaces.models

import android.graphics.Rect
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.MatrixBoolStateValueListener
import com.jht.interfaces.MatrixParcelableValueListener
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelHolder

class MatrixUIServiceModel(
) : UIServiceModel() {

    private val manager: CallbackManager = CallbackManager()

    override val stateFullScreen: BoolValueListener = buildStateValueListener(UiServiceKeys.WorkoutPanelState.FULLSCREEN)
    override val stateIdle: BoolValueListener = buildStateValueListener(UiServiceKeys.WorkoutPanelState.IDLE)
    override val stateSummary: BoolValueListener = buildStateValueListener(UiServiceKeys.WorkoutPanelState.WORKOUT_SUMMARY)

    override val overlayViewportPixels by lazy {
        MatrixParcelableValueListener(
            manager,
            UiServiceKeys.DATA_UI_OVERLAY_VIEWPORT_PARCELABLE,
            Rect(),
            Rect::class.java
        )
    }

    override fun toggleFullscreen(showFullscreenOverride: Boolean?) {
        val goFullscreen = showFullscreenOverride ?: !stateFullScreen.value
        if (goFullscreen) {
            DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.FULLSCREEN)
        } else {
            DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR)
        }
    }

    override fun addUiPanel(panelHolder: InnerPanelHolder) {
        DataMatrix.putParcelable(UiServiceKeys.ACTION_ADD_UI_PANEL_PARCELABLE, panelHolder)
    }

    override fun sendUiPanelData(panelData: BasePanelData) {
        DataMatrix.putParcelable(UiServiceKeys.ACTION_SEND_PANEL_DATA_PARCELABLE, panelData)
    }

    override fun close() {
        manager.cancelAllListeners()
    }

    private fun buildStateValueListener(state: Int) : BoolValueListener {
        val key = UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT
        return MatrixBoolStateValueListener(manager, key, state)
    }

    override fun sendMessageCenterMessage(messageCenterMessage: com.jht.interfaces.ui.MessageCenterMessage) {
        val message = MessageCenterMessage(messageCenterMessage.messageHeader, messageCenterMessage.messageText, messageCenterMessage.priority)
        message.hideTime = messageCenterMessage.hideTime
        message.setHideOnSplash(messageCenterMessage.isHiddenOnSplash)
        message.setTag(messageCenterMessage.tag)
        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message)
    }

    override fun removeMessageCenterMessageByTag(tag: String) {
        DataMatrix.putString(UiServiceKeys.ACTION_REMOVE_MESSAGE_BY_TAG_STRING, tag)
    }
}