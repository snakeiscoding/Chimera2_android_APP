package com.jht.chimera.lib.can

import com.jht.chimera.service.hal.lib.hardware.CanManager
import io.mockk.clearAllMocks
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CanManagerTest {

    private lateinit var canManager: CanManager

    //Mock messages for Get Version Response
    private val getVersionResponseMessageOneNodes: ByteArray   = byteArrayOf(0x00, 0x01, 0x02, 0x01, 0x02, 0x03, 0x00) //1 CAN Node
    private val getVersionResponseMessageTwoNodes: ByteArray   = byteArrayOf(0x00, 0x01, 0x02, 0x01, 0x02, 0x03, 0x01) //2 Can Nodes
    private val getVersionResponseMessageEightBytes: ByteArray = byteArrayOf(0x00, 0x01, 0x02, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01)
    private val getVersionResponseMessageTooShort: ByteArray   = byteArrayOf(0x00, 0x01, 0x02, 0x01) //Only major version shown
    private val getVersionResponseMessageTooLong: ByteArray    = byteArrayOf(0x00, 0x01, 0x02, 0x0A, 0x0B, 0x0C, 0x01, 0x0F, 0x0E, 0x0D, 0x0C, 0x0B, 0x0A)
    private val getVersionResponseMessageNoData: ByteArray     = byteArrayOf(0x00, 0x01, 0x02)
    private val getVersionResponseMessageWrongID: ByteArray    = byteArrayOf(0x50, 0x00, 0x02, 0x01, 0x02, 0x03, 0x00) //Wrong CAN ID (CAN ID 0x050)
    private val getVersionResponseMessageMaxVersionNumber: ByteArray   = byteArrayOf(0x00, 0x01, 0x02,
        0xFF.toByte(),
        0xFF.toByte(),
        0xFF.toByte(), 0x00) //version 255.255.255
    private val getVersionResponseMessageTwoNodesTypeTwo: ByteArray = byteArrayOf(0x00, 0x01, 0x02, 0x01, 0x02, 0x03, 0x02) //Type 2 -> Double
    private val getVersionResponseMessageOneNodeTypeThree: ByteArray = byteArrayOf(0x00, 0x01, 0x02, 0x01, 0x02, 0x03, 0x03) //Type 3 -> Single
    private val getVersionResponseMessageLeft: ByteArray = byteArrayOf(0x00, 0x01, 0x04, 0x02, 0x02, 0x02, 0x01) //Left side, 2.2.2, Double
    private val invalidMessageTooShort: ByteArray = byteArrayOf(0x00, 0x01)

    //swipe deltas
    private val treadmillSpeedSwipeDelta = 1.0f
    private val treadmillInclineSwipeDelta = 5.0f
    private val climbMillResistanceSwipeDelta = 3.0f

    @Before
    fun setup(){
        canManager = CanManager()
    }

    @Test
    fun `parsing version response message for firmware version string`() {
        canManager.firmwareVersion = ""
        canManager.processCanRx(getVersionResponseMessageOneNodes)
        assertEquals("1.2.3", canManager.firmwareVersion)
    }

    @Test
    fun `parsing version response message with one can node`() {
        canManager.canRightHardwareInfo = CanManager.CanConfigEnum.UnknownHardware
        canManager.processCanRx(getVersionResponseMessageOneNodes)
        assertEquals(CanManager.CanConfigEnum.SingleCanNode, canManager.canRightHardwareInfo)
    }

    @Test
    fun `parsing version response message with two can nodes`() {
        canManager.canRightHardwareInfo = CanManager.CanConfigEnum.UnknownHardware
        canManager.processCanRx(getVersionResponseMessageTwoNodes)
        assertEquals(CanManager.CanConfigEnum.DoubleCanNodes, canManager.canRightHardwareInfo)
    }

    @Test
    fun `parse version response message with all 8 bytes of data`() {
        canManager.firmwareVersion = ""
        canManager.processCanRx(getVersionResponseMessageEightBytes)
        assertEquals("1.1.1", canManager.firmwareVersion)
    }

    @Test
    fun `parse version response message only major version`() {
        canManager.firmwareVersion = ""
        canManager.processCanRx(getVersionResponseMessageTooShort)
        assertEquals("unknown", canManager.firmwareVersion)
    }

    @Test
    fun `parse version response message too long for a single frame`() {
        canManager.firmwareVersion = ""
        canManager.processCanRx(getVersionResponseMessageTooLong)
        assertEquals("10.11.12", canManager.firmwareVersion)
    }

    @Test
    fun `parse version response message with no data`() {
        canManager.processCanRx(getVersionResponseMessageNoData)
        assertEquals("unknown", canManager.firmwareVersion)
    }

    @Test
    fun `parse version response message with wrong CAN ID`() {
        canManager.processCanRx(getVersionResponseMessageWrongID)
        assertEquals("1.2.3", canManager.firmwareVersion)
    }

    @Test
    fun `parse version response message max version numbers`() {
        canManager.processCanRx(getVersionResponseMessageMaxVersionNumber)
        assertEquals("255.255.255", canManager.firmwareVersion)
    }

    @Test
    fun `parsing version response message with type 2 hardware`() {
        canManager.canRightHardwareInfo = CanManager.CanConfigEnum.UnknownHardware
        canManager.processCanRx(getVersionResponseMessageTwoNodesTypeTwo)
        assertEquals(CanManager.CanConfigEnum.DoubleCanNodes, canManager.canRightHardwareInfo)
    }

    @Test
    fun `parsing version response message with type 3 hardware`() {
        canManager.canRightHardwareInfo = CanManager.CanConfigEnum.UnknownHardware
        canManager.processCanRx(getVersionResponseMessageOneNodeTypeThree)
        assertEquals(CanManager.CanConfigEnum.SingleCanNode, canManager.canRightHardwareInfo)
    }

    @Test
    fun `parsing version response message for left side updates left info`() {
        canManager.canLeftHardwareInfo = CanManager.CanConfigEnum.UnknownHardware
        canManager.processCanRx(getVersionResponseMessageLeft)
        assertEquals(CanManager.CanConfigEnum.DoubleCanNodes, canManager.canLeftHardwareInfo)
        // Should also check that firmwareVersion (right side) didn't change if it was already set
        // or just verify that it stays what it was.
    }

    @Test
    fun `process invalid can message too short`() {
        canManager.firmwareVersion = "before"
        canManager.processCanRx(invalidMessageTooShort)
        assertEquals("before", canManager.firmwareVersion)
    }

    @Test
    fun `adb screen side all caps LEFT`() {
        val side: CanManager.ScreenSide = CanManager.getAdbScreenSide("LEFT")
        assertEquals(CanManager.ScreenSide.Left, side)
    }

    @Test
    fun `adb screen side all lowercase left`() {
        val side: CanManager.ScreenSide = CanManager.getAdbScreenSide("left")
        assertEquals(CanManager.ScreenSide.Left, side)
    }

    @Test
    fun `adb screen side mixed case Left`() {
        val side: CanManager.ScreenSide = CanManager.getAdbScreenSide("Left")
        assertEquals(CanManager.ScreenSide.Left, side)
    }

    @Test
    fun `adb screen side all caps RIGHT`() {
        val side: CanManager.ScreenSide = CanManager.getAdbScreenSide("RIGHT")
        assertEquals(CanManager.ScreenSide.Right, side)
    }

    @Test
    fun `adb screen side all lowercase right`() {
        val side: CanManager.ScreenSide = CanManager.getAdbScreenSide("right")
        assertEquals(CanManager.ScreenSide.Right, side)
    }

    @Test
    fun `adb screen side mixed case Right`() {
        val side: CanManager.ScreenSide = CanManager.getAdbScreenSide("Right")
        assertEquals(CanManager.ScreenSide.Right, side)
    }

    @Test
    fun `adb screen side empty string`() {
        val side: CanManager.ScreenSide = CanManager.getAdbScreenSide("")
        assertEquals(CanManager.ScreenSide.Right, side)
    }

    @Test
    fun `adb screen side null string`() {
        val side: CanManager.ScreenSide = CanManager.getAdbScreenSide(null)
        assertEquals(CanManager.ScreenSide.Right, side)
    }

    @Test
    fun `adb screen side random string`() {
        val side: CanManager.ScreenSide = CanManager.getAdbScreenSide("48nNh\"De]U6}")
        assertEquals(CanManager.ScreenSide.Right, side)
    }

    @Test
    fun `swipe up treadmill speed from zero kph`() {
        val swipeValue:Float = canManager.getSwipeFloat(
            0.0f,
            treadmillSpeedSwipeDelta,
            true
        )
        assertEquals(1.0f, swipeValue)
    }

    @Test
    fun `swipe down treadmill speed from 30 kph`() {
        val swipeValue:Float = canManager.getSwipeFloat(
            30.0f,
            treadmillSpeedSwipeDelta,
            false
        )
        assertEquals(29.0f, swipeValue)
    }

    @Test
    fun `swipe up treadmill incline from zero point zero`() {
        val swipeValue:Float = canManager.getSwipeFloat(
            0.0f,
            treadmillInclineSwipeDelta,
            true
        )
        assertEquals(5.0f, swipeValue)
    }

    @Test
    fun `swipe up climb mill resistance from 16`() {
        val swipeValue:Float = canManager.getSwipeFloat(
            16.0f,
            climbMillResistanceSwipeDelta,
            true
        )
        assertEquals(19.0f, swipeValue)
    }

    @Test
    fun `swipe down climb mill resistance from 19`() {
        val swipeValue:Float = canManager.getSwipeFloat(
            19.0f,
            climbMillResistanceSwipeDelta,
            false
        )
        assertEquals(16.0f, swipeValue)
    }

    @Test
    fun `swipe up int value`() {
        val swipeValue = canManager.getSwipeInt(10, 5, true)
        assertEquals(15, swipeValue)
    }

    @Test
    fun `swipe down int value`() {
        val swipeValue = canManager.getSwipeInt(20, 3, false)
        assertEquals(17, swipeValue)
    }

    @After
    fun cleanup(){
        clearAllMocks()  // Clear all mock interactions
    }

}