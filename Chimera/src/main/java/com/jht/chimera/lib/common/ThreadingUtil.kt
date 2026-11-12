@file:JvmName("ThreadingUtil")
package com.jht.chimera.lib.common

import android.os.Handler
import android.os.Looper


/** Provides some helper functions wrapping around [Handler] */
class UiThreadHandler : Handler(Looper.getMainLooper()) {

    /** Basically a recreation of the same function from Activity */
    fun runOnUiThread(action: Runnable) {
        if (looper.isCurrentThread) {
            action.run()
        } else {
            post(action)
        }
    }
}
