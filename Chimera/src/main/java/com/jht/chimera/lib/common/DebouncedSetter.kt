package com.jht.chimera.lib.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sets value after a specified time, reset by setting the value again
 */
class DebouncedSetter<T>(
    private val scope: CoroutineScope,
    private val delayMs: Long,
    private val onHasPending: ((Boolean) -> Unit)? = null,
    private val commit: (T) -> Unit
) {
    private var pending: Job? = null

    fun cancel() { pending?.cancel() }

    fun set(value: T) {
        // send that we have a pending change if there's no current pending change
        if (pending == null || pending?.isCancelled == true || pending?.isCompleted == true) {
            onHasPending?.invoke(true)
        }

        pending?.cancel()

        pending = scope.launch {
            delay(delayMs)
            commit(value)
            onHasPending?.invoke(false)
        }
    }
}