package com.jht.test.util

fun simpleSleep(ms: Long) {
    catchAll { Thread.sleep(ms) }
}