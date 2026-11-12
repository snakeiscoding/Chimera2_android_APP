package com.jht.chimera.lib.bumpsensorpath

import com.jht.chimera.lib.common.config.ProductConfig
import org.junit.Assert.assertEquals
import org.junit.Test


class BumpSensorPathTest {

    @Test
    fun testBumpSensorPathInProjectOne_1_0(){
        val portMapping = ProductConfig.PortMappings.ProjectOne_1_0
        val bumpSensorPath = portMapping.bumpSensorPath()
        assertEquals("/dev/ttyUSB0", bumpSensorPath)
    }

    @Test
    fun testBumpSensorPathInProjectOne_2_0(){
        val portMapping = ProductConfig.PortMappings.ProjectOne_2_0
        val bumpSensorPath = portMapping.bumpSensorPath()
        assertEquals("/dev/ttyUSB1", bumpSensorPath)
    }

    @Test
    fun testProductsWithoutBumpSensorPath() {
        // It's only a bump sensor path on Onyx currently, other products don't have bump sensor.
        val bumpSensorPath1 = ProductConfig.PortMappings.Chimera_1_0.bumpSensorPath()
        val bumpSensorPath2 = ProductConfig.PortMappings.Parker.bumpSensorPath()
        val bumpSensorPath3 = ProductConfig.PortMappings.LCGUI.bumpSensorPath()
        assertEquals(null, bumpSensorPath1)
        assertEquals(null, bumpSensorPath2)
        assertEquals(null, bumpSensorPath3)
    }
}