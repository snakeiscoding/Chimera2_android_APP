@file:JvmName("HapticMotorItem")
package com.jht.mastboard.lib

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A class representing events for a haptic motor.
 *
 * This class encapsulates different types of haptic feedback events
 * that can be triggered in response to various user interactions.
 * Being a sealed class, it allows for a limited set of subclasses,
 * making it easy to handle all possible haptic events exhaustively.
 */
@Parcelize
sealed class HapticMotorEvent: Parcelable {

    /**
     * Represents a haptic event where a specific panel is buzzed.
     *
     * @property panelSide The side of the panel to be buzzed.
     */
    @Parcelize data class BuzzPanel(val panelSide: PanelSide): HapticMotorEvent()

    /**
     * Represents a haptic event where all panels are buzzed.
     */
    @Parcelize object BuzzAllPanels: HapticMotorEvent()
}