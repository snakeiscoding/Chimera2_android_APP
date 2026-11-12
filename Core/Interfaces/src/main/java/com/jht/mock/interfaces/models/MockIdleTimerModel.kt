package com.jht.mock.interfaces.models

import com.jht.interfaces.IStartStop
import com.jht.interfaces.models.IdleTimerModel

class MockIdleTimerModel : IdleTimerModel() {

    // just ignore the calls or do we need to build any kind of timer simulation?
    var genericTimer: IStartStop = object: IStartStop {
        override fun start() {}
        override fun stop() {}
    }

    override fun createHomeScreenSystemTimer(isFirstScreen: Boolean): IStartStop { return genericTimer }
    /** Callbacks registered with [addIdleTimer], keyed by timer name, so tests can fire a timeout */
    val idleCallbacks = mutableMapOf<String, () -> Unit>()

    override fun addIdleTimer(timerName: String, timeoutMs: Int, idleCallback: () -> Unit) {
        idleCallbacks[timerName] = idleCallback
    }
    override fun clearIdleTimers() {
        idleCallbacks.clear()
    }
    override fun startIdleTimers(wakeCallback: ()->Unit) {}
    override fun stopIdleTimers() {}
    override fun resetIdleTimers() {}

    override fun close() {
        // do nothing
    }
}
