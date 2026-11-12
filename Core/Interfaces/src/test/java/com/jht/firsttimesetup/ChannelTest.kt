package com.jht.firsttimesetup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChannelTest {

    @Test
    // CGC-22432
    fun `generateUuid returns same value for same inputs`() {
        val uuid1 = Channel.generateUuid("2", "ESPN")
        val uuid2 = Channel.generateUuid("2", "ESPN")
        assertEquals(uuid1, uuid2)
    }

    @Test
    // CGC-22432
    fun `generateUuid returns different values for different channels`() {
        val espn = Channel.generateUuid("2", "ESPN")
        val cnn  = Channel.generateUuid("3", "CNN")
        assertNotEquals(espn, cnn)
    }

    @Test
    // CGC-22432
    fun `generateUuid distinguishes channels with same name but different number`() {
        val a = Channel.generateUuid("1", "ESPN")
        val b = Channel.generateUuid("2", "ESPN")
        assertNotEquals(a, b)
    }
}
