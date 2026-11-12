package com.jht.chimera.service.ui

import android.content.Context
import com.jht.chimera.R
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.language.StringLookup
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage
import com.jht.chimera.service.ui.panels.PanelIds
import com.jht.chimera.service.ui.panels.messagecenter.MessageCenterModalPanel
import com.jht.chimera.service.ui.panels.messagecenter.MessageCenterTopPanel
import com.jht.datamatrixlib.DataMatrix
import com.jht.gattserver2.WatchState
import java.util.PriorityQueue

/**
 * Controls the message center panels. Message center panels are added to the PanelManager and
 * removed after a time specified by the type of message. Messages are displayed in order of priority.
 * Messages in the queue will be displayed after the last one is no longer shown.
 * <p>
 * Sends a message at the start of the workout that says that heart rate may be inaccurate
 */
class MessageCenterManager(context: Context) {
    private val panelManager: PanelManager = PanelManager(context)
    private var mCurrentMessage: MessageCenterMessage? = null
    private var messagePanel = MessageCenterTopPanel(context, PanelIds.MESSAGE_TOP_PANEL.id, panelManager)
    private val modalPanel = MessageCenterModalPanel(context, PanelIds.MESSAGE_FULL_PANEL.id, panelManager)

    // Heart Rate Warning Message variables
    private val heartRateWarningMessage = MessageCenterMessage("", StringLookup.getString(R.string.hr_accuracy_pg, context), 10)
    private var mNeedShowHeartRateMessage = false

    private val mMessageQueue = PriorityQueue(11) { o1: MessageCenterMessage, o2: MessageCenterMessage ->
        // Need to return opposite sign because message center priorities are higher if lower
        val priority1 = o1.priority
        priority1.compareTo(o2.priority)
    }

    /**
     * Handles any changes to the workout panel state from the system and tells the panel manager to
     * update so that our Message panels do not show incorrectly.
     * Needs to hook into an adapter for UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT
     */
    fun processWorkoutPanelState() {
        panelManager.updatePanels()
    }

    /** If panel types changed, z-order may have changed. This should re-add panels */
    fun reAddPanels() {
        panelManager.reAddPanels()
    }

    /**
     * Method to handle cleanup when this object is destroyed
     */
    fun onDestroy() {
        panelManager.disposeAll()
    }

    /** remove any messages with a matching tag string */
    fun removeMessagesByTag(tag: String?) {
        mMessageQueue.removeAll { it.tag == tag }
        if (mCurrentMessage != null && mCurrentMessage?.tag == tag) {
            displayNextMessage()
        }
    }

    /**
     * Handles any direct call to remove messages that contain the corresponding tag.
     */
    fun removeMessagesToHideOnSplash() {
        // remove messages that needs to be hidden
        val messagesToRemove: MutableList<MessageCenterMessage> = ArrayList()
        for (m in mMessageQueue) {
            if (m.isHiddenOnSplash) messagesToRemove.add(m)
        }
        for (m in messagesToRemove) {
            mMessageQueue.remove(m)
        }
        if (mCurrentMessage != null && mCurrentMessage!!.isHiddenOnSplash) {
            displayNextMessage()
        }
    }

    /**
     * Adds a message to the queue of messages to display.
     * Needs to hook into an adapter for UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE
     *
     * @param message The message to display
     */
    fun queueMessage(message: MessageCenterMessage) {
        if (message.isHidden) {
            mMessageQueue.remove(message)
            if (message == mCurrentMessage) {
                displayNextMessage()
            }
        } else if (message != mCurrentMessage && !mMessageQueue.contains(message)) {
            mMessageQueue.add(message)
            // Note we will leave the current message up if there is one until it expires.
            if (mCurrentMessage == null) {
                displayNextMessage()
            }
        }
    }

    private fun displayNextMessage() {
        // showMessage close callback might come from a timer outside of UI thread
        panelManager.runOnUiThread {
            modalPanel.hide()
            mCurrentMessage = mMessageQueue.poll()
            messagePanel.showMessage(mCurrentMessage, { message: MessageCenterMessage? ->
                modalPanel.showMessage(message) { displayNextMessage() }
            }) { displayNextMessage() }
        }
    }

    /**
     * Handles flipping a flag when a workout is started, so that Message Center Manager knows we
     * are in a workout.
     * Needs to hook into an adapter for WorkoutKeys.EVENT_WORKOUT_START
     */
    fun onWorkoutStart() {
        mNeedShowHeartRateMessage = true
    }

    /**
     * Handles displaying an accuracy warning about heart rate to the user when a heart rate that is
     * NOT from a watch is detected.
     * Needs to hook into an adapter for HALKeys.DATA_HEART_RATE_INT
     */
    fun onHeartRate(heartRate: Int) {
        if (heartRate <= 0) return
        val isInWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP
        if (isInWorkout && mNeedShowHeartRateMessage) {
            if (DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT, 0) != HALKeys.STATUS_GYM_KIT_CONNECTED &&
            DataMatrix.getInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal) != WatchState.Connected.ordinal) {
                heartRateWarningMessage.setMessageText(StringLookup.get(R.string.hr_accuracy_pg))
                DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, heartRateWarningMessage)
                mNeedShowHeartRateMessage = false
            }
        }
    }
}