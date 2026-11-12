package com.jht.interfaces.models

import com.jht.androidcommonalgorithms.timer.JHTTimer
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.timer.HomeScreenIdleTimer
import com.jht.chimera.lib.common.timer.IdleTimerManagement
import com.jht.interfaces.IStartStop

class MatrixIdleTimerModel : IdleTimerModel() {

    override fun createHomeScreenSystemTimer(isFirstScreen: Boolean): IStartStop {
        // TODO - we should merge ParkerHomeScreenIdleTimer into HomeScreenIdleTimer with market checks to avoid multiple similar classes
        return HomeScreenIdleTimer(ContextUtil.getApplicationContext(), isFirstScreen)
    }


    val idleTimerManagement = IdleTimerManagement() // should we only create instance when needed or just have one always?

    override fun addIdleTimer(timerName: String, timeoutMs: Int, idleCallback: () -> Unit) {
        if (timeoutMs > 0) {
            val newTimer = JHTTimerGeneric(timerName, timeoutMs, idleCallback)
            idleTimerManagement.addTimer(newTimer)
        }
    }
    override fun clearIdleTimers() { idleTimerManagement.clearTimers() }
    override fun startIdleTimers(wakeCallback: ()->Unit) {
        idleTimerManagement.wakeCallback = Runnable(wakeCallback)
        idleTimerManagement.monitorTimers()
    }
    override fun stopIdleTimers() { idleTimerManagement.stopMonitoring() }
    override fun resetIdleTimers() { idleTimerManagement.resetTimers() }

    override fun close() {
        idleTimerManagement.clearTimers()
    }
}


/** Generic timer wrapper for our current system timer implementation */
private class JHTTimerGeneric(val name: String, timeoutMs: Int, callback: () -> Unit) : JHTTimer(timeoutMs, callback), IStartStop {
    override fun start() {
        super.start(name)
    }
}
