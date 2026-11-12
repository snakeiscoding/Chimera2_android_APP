package com.jht.progression.data.program

import com.jht.enums.IntervalType
import com.jht.progression.data.program.program5k.Program5k
import com.jht.unit.kphUnit
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(JUnitParamsRunner::class)
class ZoneSpeedTest {


    @Test
    @Parameters(
        "18, 3.5",
        "24, 3.5",
        "28, 3.46",
        "31, 3.34",
        "46, 2.75",
        "60, 2.19",
        "80, 1.40",
        "100, 1.00"
    )
    fun testZone1Speeds(raceTimeMinutes: Float, expectedSpeedMph: Float) {
        val zoneSpeedMph = (getSpeedForIntervalType(IntervalType.ZONE_1, getBaselineFor5kTime(raceTimeMinutes)) { it }).kphUnit.toMph
        assertEquals(expectedSpeedMph, zoneSpeedMph, 0.01f)
    }

    @Test
    @Parameters(
        "18, 7.70",
        "24, 6.17",
        "28, 5.17",
        "31, 4.55",
        "46, 3.19",
        "60, 2.57",
        "80, 2.02",
        "100, 1.66"
    )
    fun testZone2Speeds(raceTimeMinutes: Float, expectedSpeedMph: Float) {
        val zoneSpeedMph = (getSpeedForIntervalType(IntervalType.ZONE_2, getBaselineFor5kTime(raceTimeMinutes)) { it }).kphUnit.toMph
        assertEquals(expectedSpeedMph, zoneSpeedMph, 0.01f)
    }

    @Test
    @Parameters(
        "18, 8.23",
        "24, 6.50",
        "28, 5.63",
        "31, 5.09",
        "46, 3.57",
        "60, 2.82",
        "80, 2.16",
        "100, 1.76"
    )
    fun testZone3Speeds(raceTimeMinutes: Float, expectedSpeedMph: Float) {
        val zoneSpeedMph = (getSpeedForIntervalType(IntervalType.ZONE_3, getBaselineFor5kTime(raceTimeMinutes)) { it }).kphUnit.toMph
        assertEquals(expectedSpeedMph, zoneSpeedMph, 0.01f)
    }

    @Test
    @Parameters(
        "18, 10.36",
        "24, 7.77",
        "28, 6.66",
        "31, 6.01",
        "46, 4.05",
        "60, 3.11",
        "80, 2.33",
        "100, 1.86"
    )
    fun testZone4Speeds(raceTimeMinutes: Float, expectedSpeedMph: Float) {
        val zoneSpeedMph = (getSpeedForIntervalType(IntervalType.ZONE_4, getBaselineFor5kTime(raceTimeMinutes)) { it }).kphUnit.toMph
        assertEquals(expectedSpeedMph, zoneSpeedMph, 0.01f)
    }

    @Test
    @Parameters(
        "18, 15.82",
        "24, 10.26",
        "28, 8.21",
        "31, 7.15",
        "46, 4.35",
        "60, 3.28",
        "80, 2.42",
        "100, 1.92"
    )
    fun testZone5Speeds(raceTimeMinutes: Float, expectedSpeedMph: Float) {
        val zoneSpeedMph = (getSpeedForIntervalType(IntervalType.ZONE_5, getBaselineFor5kTime(raceTimeMinutes)) { it }).kphUnit.toMph
        assertEquals(expectedSpeedMph, zoneSpeedMph, 0.01f)
    }


    private fun getBaselineFor5kTime(raceTimeMinutes: Float) : Float {
        return Program5k().computeBaselineFromWorkoutTime((raceTimeMinutes * 60).toInt())
    }

}