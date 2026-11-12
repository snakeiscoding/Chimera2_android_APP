package com.jht.interfaces.models

import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.keypress.KeyPress
import com.jht.concierge.service.ConciergeEvent
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.CallbackManager
import com.jht.interfaces.MatrixBoolValueListener
import com.jht.interfaces.MatrixEventListener
import com.jht.interfaces.MatrixIntValueListener
import com.jht.interfaces.MatrixParcelableListener
import com.jht.interfaces.MatrixParcelableValueListener
import com.jht.interfaces.MatrixStringValueListener
import com.jht.mastboard.data.LEDItem
import com.jht.mastboard.data.UiEventKeys
import com.jht.mastboard.interfaces.MastboardModel
import com.jht.mastboard.lib.HapticMotorEvent

class MatrixMastBoardModel : MastboardModel() {
    private val manager: CallbackManager = CallbackManager()
    override fun close() {
        manager.cancelAllListeners()
    }

    override val isGoAllowed = DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_PHYSICAL_GO_BTN_BOOL)

    override val ledDefaultColor = MatrixIntValueListener(manager, SettingKeys.Experimental.DATA_LED_DEFAULT_COLOR_HEX_INT)
    override val ledWorkoutRunningColor = MatrixIntValueListener(manager, SettingKeys.Experimental.DATA_LED_WORKOUT_RUNNING_COLOR_HEX_INT)
    override val ledWorkoutPausedColor = MatrixIntValueListener(manager, SettingKeys.Experimental.DATA_LED_WORKOUT_PAUSED_COLOR_HEX_INT)
    override val ledWorkoutSummaryColor = MatrixIntValueListener(manager, SettingKeys.Experimental.DATA_LED_WORKOUT_SUMMARY_COLOR_HEX_INT)
    override val ledConciergeColor = MatrixIntValueListener(manager, SettingKeys.Experimental.DATA_LED_CONCIERGE_COLOR_HEX_INT)
    override val userLedStyle = MatrixIntValueListener(manager, DAPIKeys.DATA_USER_LED_STYLE_INT)
    override val ledOverride = MatrixBoolValueListener(manager, SettingKeys.ProjectOne.DATA_OVERRIDE_LED_COLOR_BOOL)

    override val uiEvent = MatrixStringValueListener(manager, UiEventKeys.Key.DATA_UI_EVENT_STRING)
    override val conciergeEvent = MatrixParcelableValueListener(manager, MainAppKeys.DATA_CONCIERGE_EVENT_PARCELABLE, ConciergeEvent.RequestClosed, ConciergeEvent::class.java)

    override fun setLedColor(ledItem: LEDItem) {
        DataMatrix.putParcelable(SettingKeys.ProjectOne.ACTION_LED_COLOR_HEX_PARCELABLE, ledItem)
    }
    override val onLedColor = MatrixParcelableListener(manager, SettingKeys.ProjectOne.ACTION_LED_COLOR_HEX_PARCELABLE, LEDItem::class.java)
    override val ledBrightness = MatrixIntValueListener(manager, SettingKeys.ProjectOne.DATA_LED_BRIGHTNESS_INT)
    override val lcdBrightness = MatrixIntValueListener(manager, SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT)

    override fun onScreenActivity() {
        DataMatrix.sendEvent(HALKeys.EVENT_SCREEN_TOUCH)
    }

    override fun sendHapticEvent(event: HapticMotorEvent) {
        DataMatrix.putParcelable(SettingKeys.ProjectOne.ACTION_HAPTIC_MOTOR_EVENT_PARCELABLE, event)
    }
    override val onHapticEvent = MatrixParcelableListener(manager, SettingKeys.ProjectOne.ACTION_HAPTIC_MOTOR_EVENT_PARCELABLE, HapticMotorEvent::class.java)
    override val hapticMotorEffect = MatrixIntValueListener(manager, SettingKeys.ProjectOne.DATA_HAPTIC_MOTOR_EFFECT_ID_INT)
    override val userHapticsEnabled = MatrixBoolValueListener(manager, DAPIKeys.DATA_USER_HAPTICS_ON_BOOL)
    override val systemHapticsEnabled = MatrixBoolValueListener(manager, SettingKeys.ProjectOne.DATA_HAPTIC_MOTOR_ENABLED_ON_SYSTEM_BOOL)

    override val onLcdDebugLayoutEvent = MatrixEventListener(manager, SettingKeys.ProjectOne.EVENT_SETUP_LCD_DEBUG_LAYOUT)
    override val lcdDebugPanels = MatrixBoolValueListener(manager, SettingKeys.ProjectOne.DATA_P1_ENABLE_DEBUG_VIEW_BOOL)

    // These could be replaced by passing a control model like compose panels use?
    override fun inclineIncrement(touched: Boolean) = emitKeyEvent(KeyPress.KeyCode.KEY_INCLINE_UP, touched)
    override fun inclineDecrement(touched: Boolean) = emitKeyEvent(KeyPress.KeyCode.KEY_INCLINE_DOWN, touched)
    override fun speedIncrement(touched: Boolean) = emitKeyEvent(KeyPress.KeyCode.KEY_SPEED_UP, touched)
    override fun speedDecrement(touched: Boolean) = emitKeyEvent(KeyPress.KeyCode.KEY_SPEED_DOWN, touched)
    override fun resistanceIncrement(touched: Boolean) = emitKeyEvent(KeyPress.KeyCode.KEY_RESISTANCE_UP, touched)
    override fun resistanceDecrement(touched: Boolean) = emitKeyEvent(KeyPress.KeyCode.KEY_RESISTANCE_DOWN, touched)
    override fun emulateGoPress() {
        emitKeyEvent(KeyPress.KeyCode.KEY_GO, true)
        emitKeyEvent(KeyPress.KeyCode.KEY_GO, false)
    }
    private fun emitKeyEvent(keyCode: KeyPress.KeyCode, touched: Boolean) {
        val keyState = if (touched) KeyPress.KeyState.KEY_DOWN else KeyPress.KeyState.KEY_UP
        DataMatrix.putInt(
            HALKeys.EVENT_KEYPRESS_BITMAP_INT,
            KeyPress.createBitmapKey(keyCode, keyState)
        )
    }
}
