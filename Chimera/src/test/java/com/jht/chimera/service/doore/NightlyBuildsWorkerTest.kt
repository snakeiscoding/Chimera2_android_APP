package com.jht.chimera.service.doore

import io.mockk.clearAllMocks
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/** Test that we can retrieve the proper URLs per platform, and that no platform returns an empty
 * list of available versions that can be installed.
 * Please reference the linked Requirements page.
 * @see <a href="https://jhtnaee.atlassian.net/wiki/spaces/CSR/pages/4532305963/DOORE+-+In+Progress">DOORE Requirements</a>
 */
class NightlyBuildsWorkerTest {
    private lateinit var testDooreApiAccess: DooreApi

    @Before
    fun setup(){
        testDooreApiAccess = DooreApi()
    }

    @After
    fun cleanup(){
        clearAllMocks()  // Clear all mock interactions
    }

    @Test
    fun testParkerDooreUrl(): Unit {
        assertEquals(testDooreApiAccess.softwareTypeUrl("parker"),
            "https://7b2r1ruh94.execute-api.us-east-2.amazonaws.com/main/parker")
    }

    @Test
    fun testRetrieveSoftwareOptionsParkerNotEmpty(): Unit {
        assertNotEquals("", testDooreApiAccess.retrieveSoftwareOptions("parker"))
    }

    @Test
    fun testChimeraDooreUrl(): Unit {
        assertEquals(testDooreApiAccess.softwareTypeUrl("chimera"),
            "https://7b2r1ruh94.execute-api.us-east-2.amazonaws.com/main/chimera")
    }

    @Test
    fun testRetrieveSoftwareOptionsChimeraNotEmpty(): Unit {
        assertNotEquals("", testDooreApiAccess.retrieveSoftwareOptions("chimera"))
    }

    @Test
    fun testLowCostGuiDooreUrl(): Unit {
        assertEquals(testDooreApiAccess.softwareTypeUrl("lowcostgui"),
            "https://7b2r1ruh94.execute-api.us-east-2.amazonaws.com/main/lowcostgui")
    }

    @Test
    fun testRetrieveSoftwareOptionsLowCostGuiNotEmpty(): Unit {
        assertNotEquals("", testDooreApiAccess.retrieveSoftwareOptions("lowcostgui"))
    }

    @Test
    fun testProjectOneDooreUrl(): Unit {
        assertEquals(testDooreApiAccess.softwareTypeUrl("projectone"),
            "https://7b2r1ruh94.execute-api.us-east-2.amazonaws.com/main/projectone")
    }

    @Test
    fun testRetrieveSoftwareOptionsProjectOneNotEmpty(): Unit {
        assertNotEquals("", testDooreApiAccess.retrieveSoftwareOptions("projectone"))
    }
}