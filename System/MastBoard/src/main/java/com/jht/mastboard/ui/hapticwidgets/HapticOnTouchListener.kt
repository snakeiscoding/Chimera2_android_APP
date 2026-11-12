@file:JvmName("HapticOnTouchListener")
package com.jht.mastboard.ui.hapticwidgets

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.jht.mastboard.interfaces.MastboardModel
import com.jht.mastboard.lib.HapticMotorEvent
import com.jht.mastboard.lib.PanelSide

class HapticOnTouchListener(private val mastboardModel: MastboardModel, private val side: PanelSide): OnTouchListener {
    private val listeners = mutableListOf<OnTouchListener>()

    /**
     * This is the preferred constructor
     */
    constructor(mastboardModel: MastboardModel, side: PanelSide, listener: OnTouchListener): this(mastboardModel, side) {
        addOnTouchListener(listener)
    }
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        var returnVal = false
        listeners.forEach { returnVal = returnVal or it.onTouch(v, event) }
        return returnVal
    }

    @SuppressLint("ClickableViewAccessibility")
    fun addOnTouchListener(listener: OnTouchListener) {
        listeners.add(listener)
        listeners.add(OnTouchListener { v, event ->
            // Creative wants haptics only on the down action
            if (event.action == MotionEvent.ACTION_DOWN) {
                mastboardModel.sendHapticEvent(HapticMotorEvent.BuzzPanel(side))
            }
            false
        })
    }
}