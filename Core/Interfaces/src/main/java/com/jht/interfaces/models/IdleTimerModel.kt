package com.jht.interfaces.models

import com.jht.interfaces.BaseModel
import com.jht.interfaces.IStartStop

/**
 * Model for using system idle timers. These timers monitor various system functions to determine if user is actively using the system.
 */
abstract class IdleTimerModel : BaseModel {

    // Specific idle timers

    /** Creates a specific collection of system event idle timers for use on home screen.
     *  It operates independent of the generic idle timer collection tracked by this model. Model does not manage it beyond creation. */
    abstract fun createHomeScreenSystemTimer(isFirstScreen: Boolean): IStartStop

    // Generic idle timers

    /** Create a new idle timer in the current collection of generic idle timers. Useful if UI needs its own idle timer for specific UI functions. (Ignored if [timeoutMs] <= 0) */
    abstract fun addIdleTimer(timerName: String, timeoutMs: Int, idleCallback: ()->Unit) // should we return instance and provide remove? collection per model instance or global?

    /** Clear the current collection of generic idle timers */
    abstract fun clearIdleTimers()

    /** Start monitoring the current collection of generic idle timers. You can pass a callback if you need to react to wake activity. */
    abstract fun startIdleTimers(wakeCallback: ()->Unit = {})

    /** Stop monitoring the current collection of generic idle timers */
    abstract fun stopIdleTimers()

    /** Manually reset the current collection of generic idle timers */
    abstract fun resetIdleTimers()
}
