package com.jht.chimera.lib.common.testing

object InstrumentationTestStatus {
    val isRunningTest : Boolean by lazy {
        try {
            Class.forName("androidx.test.espresso.Espresso")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}