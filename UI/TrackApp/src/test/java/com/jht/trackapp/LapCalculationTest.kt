package com.jht.trackapp

import com.jht.trackapp.common.utils.Extensions.getLapData
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lap calculation test
 */
@RunWith(JUnitParamsRunner::class)
class LapCalculationTest {

    /**
     * trackDistance will be passed as prefix of track distance
     * e.g 100 -> 1, so 2, 4, 8, 16 for latter distances
     */

    // Base Distance -> 100m
    @Test
    @Parameters(
        "1, 1, 1",
        "5, 4, 1",
        "3, 8, 0",
        "12, 16, 0",
        "32, 16, 2",
        "84, 8, 10",
        "96, 4, 24"
    )
    fun `test conversionFromBaseDistance`(lapCount: Int, trackDistance: Int, expected: Int) {
        assertEquals(expected, lapCount / trackDistance)
    }

    // This function tests the list size depending on the track distance
    @Test
    @Parameters(
        "1",
        "2",
        "4",
        "8",
        "16"
    )
    fun getValidLapCountFromListTest(trackDistance: Int) {
        val dummyLapList : List<Int>  = (1..40).map { (1..40).random() }
        assertEquals(dummyLapList.size / trackDistance, dummyLapList.getLapData(trackDistance).size)
    }

}