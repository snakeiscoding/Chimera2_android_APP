@file:JvmName("HapticOnClickListener")
package com.jht.mastboard.ui.hapticwidgets

import android.view.View
import android.view.View.OnClickListener
import com.jht.mastboard.lib.HapticMotorEvent
import com.jht.mastboard.lib.PanelSide
import com.jht.mastboard.interfaces.MastboardModel

class HapticOnClickListener(private val mastboardModel: MastboardModel, private val side: PanelSide) : OnClickListener {
    private val listeners = mutableListOf<OnClickListener>()

    /**
     * This constructor should simplify instantiation.
     */
    constructor(mastboardModel: MastboardModel, side: PanelSide, listener: OnClickListener): this(mastboardModel, side) {
        addOnClickListener(listener)
    }

    fun addOnClickListener(listener: OnClickListener) {
        listeners.add(listener)
        listeners.add {
            mastboardModel.sendHapticEvent(HapticMotorEvent.BuzzPanel(side))
        }
    }

    override fun onClick(v: View) {
        listeners.forEach { it.onClick(v) }
    }
}