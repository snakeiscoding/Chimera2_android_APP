package com.jht.chimera

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test Suite to verify all Cpp libraries created by Chimera Drivers are able to be loaded.
 */
@RunWith(AndroidJUnit4::class)
class NativeLoadTest {
    @Test
    fun loads_ChimeraMIXCtl() {
        System.loadLibrary("ChimeraMIXCtl")
        assert(true);
    }

    @Test
    fun loads_ChimeraAudio() {
        System.loadLibrary("ChimeraAudio")
        assert(true);
    }

    @Test
    fun loads_mfi_usb() {
        System.loadLibrary("mfi-usb")
    }

    @Test
    fun loads_mfi_i2c() {
        System.loadLibrary("mfi-i2c")
    }

    @Test
    fun loads_hdmiin() {
        System.loadLibrary("hdmiin")
    }

    @Test
    fun loads_driver() {
        System.loadLibrary("driver")
    }

    @Test
    fun loads_nativeyuv() {
        System.loadLibrary("nativeyuv")
    }

    @Test
    fun loads_yuv() {
        System.loadLibrary("yuv")
    }
}
