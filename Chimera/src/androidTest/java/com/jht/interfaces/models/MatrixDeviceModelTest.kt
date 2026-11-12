package com.jht.interfaces.models

import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

class MatrixDeviceModelTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var deviceModel : DeviceModel

    // how to tell we are in a test - logic on splash screen to disable killing?

    @Before
    fun setup() {
        println("setup")
        hiltRule.inject()
    }

    @After
    fun cleanup() {
        println("cleanup")
        deviceModel.close()
    }

}