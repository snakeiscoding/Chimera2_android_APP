package com.jht.chimera.lib.parker

import com.jht.enums.FrameType
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * This is for testing combinations of the correct LCB type with specific frame model and
 * frame serial number on retail frames
 */
class ParkerFramesTest {

    private lateinit var systemModel: SystemModel
    private lateinit var uiBaseModel: UIBaseModel

    @Before
    fun setup(){
        // Create a new mock instance with strict behavior
        systemModel = mockk(relaxed = false)
        uiBaseModel = mockk(relaxed = false)
    }

    @After
    fun cleanup(){
        clearAllMocks()  // Clear all mock interactions
    }

    /**Test the correct lcb type with the specific frame model and frame serial
     * number combinations on Ascent frames
     * */
    @Test
    fun testAscentFrames(){
        val ascentConfig1 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("A30_02", "FEP701"),
                FrameEntity("A30_03", "FEP711"),
                FrameEntity("A30_04", "FEP729")
            ),
            lcbTypes = listOf("0x0B", "0xB6"),
            frameType = FrameType.Ascent
        )
        runFrameTests(ascentConfig1)

        val ascentConfig2 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("A50", "FEP702"),
                FrameEntity("A50_03", "FEP712"),
                FrameEntity("A50_04", "FEP727"),
            ),
            lcbTypes = listOf("0x15", "0xB9"),
            frameType = FrameType.Ascent
        )
        runFrameTests(ascentConfig2)
    }

    /**Test the correct lcb type with the specific frame model and frame serial number
     * combinations on Elliptical frames
     * */
    @Test
    fun testEllipticalFrames(){
        val ellipticalConfig1 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("E30", "FEP597"),
                FrameEntity("E30_02", "FEP730"),
                FrameEntity("E30_03", "FEP299B"),
            ),
            lcbTypes = listOf("0x12"),
            frameType = FrameType.Elliptical
        )
        runFrameTests(ellipticalConfig1)

        val ellipticalConfig2 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("E50", "FEP598"),
                FrameEntity("E50_02", "FEP728"),
                FrameEntity("E50_03", "FEP300B"),
            ),
            lcbTypes = listOf("0x0C", "0xB7", "0xB9"),
            frameType = FrameType.Elliptical
        )
        runFrameTests(ellipticalConfig2)

    }

    /**Test the correct lcb type with the specific frame model and frame serial
     * number combinations on Clibmill frames
     * */
    @Test
    fun testClimbmillFrames(){
        val climbmillConfig = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("C50", "FCS31"),
                FrameEntity("C50_02", "FCS38")
            ),
            lcbTypes = listOf("0x19"),
            frameType = FrameType.Climbmill
        )
        runFrameTests(climbmillConfig)
    }

    /**Test the correct lcb type with the specific frame model and frame serial
     * number combinations on Bike frames
     * */
    @Test
    fun testBikeFrames(){
        val bikeConfig1 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("R30_03", "FRB207"),
                FrameEntity("R30_04", "FRB213"),
                FrameEntity("U30_02", "FCB204"),
                FrameEntity("U30_03", "FCB208")
            ),
            lcbTypes = listOf("0x12"),
            frameType = FrameType.Bike
        )
        runFrameTests(bikeConfig1)

        val bikeConfig2 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("R50", "FRB208"),
                FrameEntity("R50_02", "FRB212"),
                FrameEntity("U50", "FCB205"),
                FrameEntity("U50_02", "FCB210")
            ),
            lcbTypes = listOf("0x0C", "0xB7", "0xB9"),
            frameType = FrameType.Bike
        )
        runFrameTests(bikeConfig2)
    }

    /**Test the correct lcb type with the specific frame model and frame serial
     * number combinations on Treadmill frames
     * */
    @Test
    fun testTreadmillFrames(){
        val treadmillConfig1 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("ST90", "FTM755")
            ),
            lcbTypes = listOf("0xE0", "0xE1"),
            frameType = FrameType.Treadmill
        )
        runFrameTests(treadmillConfig1)

        val treadmillConfig2 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("T30", "FTM729")
            ),
            lcbTypes = listOf("0x06", "0xD0"),
            frameType = FrameType.Treadmill
        )
        runFrameTests(treadmillConfig2)

        val treadmillConfig3 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("T50_02", "FTM695"),
                FrameEntity("T50_03", "FTM767"),
                FrameEntity("TF30", "FTM693"),
                FrameEntity("TF30_02", "FTM693B"),
                FrameEntity("TF50", "FTM694"),
                FrameEntity("TF50_02", "FTM694B"),
                FrameEntity("TF50_03", "FTM768")
            ),
            lcbTypes = listOf("0x06", "0x0E", "0xD0", "0xD1"),
            frameType = FrameType.Treadmill
        )
        runFrameTests(treadmillConfig3)

        val treadmillConfig4 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("T70_02", "FTM703"),
                FrameEntity("T70_03", "FTM727"),
                FrameEntity("T70_04", "FTM703D"),
                FrameEntity("T75", "FTM703B"),
                FrameEntity("T75_02", "FTM703C")
            ),
            lcbTypes = listOf("0xC3", "0xA3", "0xC7"),
            frameType = FrameType.Treadmill
        )
        runFrameTests(treadmillConfig4)

        val treadmillConfig5 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("T70_05", "FTM783"),
                FrameEntity("T70_06", "FTM783B"),
                FrameEntity("T75_04", "FTM703E")
            ),
            lcbTypes = listOf("0xA3", "0xC7"),
            frameType = FrameType.Treadmill
        )
        runFrameTests(treadmillConfig5)

        val treadmillConfig6 = FrameTestConfig(
            frameEntities = listOf(
                FrameEntity("T70_02_LCB_C1", "FTM703"),
                FrameEntity("T75_LCB_C1", "FTM703B")
            ),
            lcbTypes = listOf("0xC1"),
            frameType = FrameType.Treadmill
        )
        runFrameTests(treadmillConfig6)

    }

    /**Runs assertions on each test case*/
    private fun runFrameTests(config: FrameTestConfig) {
        val testCases = generateTestCases(config)

        testCases.forEach { testCase ->
            val lcbType = Integer.decode(testCase.lcbType)
            every { uiBaseModel.frameType } returns testCase.frameType
            every { systemModel.lcbType } returns lcbType
            every { systemModel.serialNumberFrame } returns testCase.frameSN
            assertTrue("Frame model", config.frameEntities.any { it.frameModel == testCase.frameModel })
            assertEquals("Frame serial number", testCase.frameSN, systemModel.serialNumberFrame)
            assertEquals("LCB type", lcbType, systemModel.lcbType)
            assertEquals("Frame type", testCase.frameType, uiBaseModel.frameType)
            logTestExecution(testCase)
        }
    }

    /**Creates test combinations from the frame entities and LCB types*/
    private fun generateTestCases(config: FrameTestConfig): List<FrameDataCombination> {
        return config.lcbTypes.flatMap { lcbType ->
            config.frameEntities.map { frameConfig ->
                FrameDataCombination(
                    frameModel = frameConfig.frameModel,
                    frameSN = frameConfig.frameSerialNumber,
                    lcbType = lcbType,
                    frameType = config.frameType
                )
            }
        }
    }

    /**Print each combination of the frame data*/
    private fun logTestExecution(frameData: FrameDataCombination) {
        println("""
            ${frameData.frameType} Frame Test:
            - Model: ${frameData.frameModel}
            - Frame Serial Number: ${frameData.frameSN}
            - LCB Type: ${frameData.lcbType}
            - Frame Type: ${frameData.frameType}
        """.trimIndent())
    }
}

data class FrameEntity(
    val frameModel: String,
    val frameSerialNumber: String
)

data class FrameTestConfig(
    val frameEntities: List<FrameEntity>,
    val lcbTypes: List<String>,
    val frameType: FrameType
)

data class FrameDataCombination(
    val frameModel: String,
    val frameSN: String,
    val lcbType: String,
    val frameType: FrameType
)