package com.jht.test.util

/**
 * Use with great care.  This just catches and ignores any exceptions.  This cleans up code
 * where we really don't care if there is an exception.  Example would be an IOException on close.
 */
fun catchAll(call: () -> Unit) {
    try {
        call()
    } catch (ex: Exception) {}
}

fun <T> catchAll(defaultValue: T, call: () -> T) : T {
    try {
        return call()
    } catch (ex: Exception) {}
    return defaultValue
}