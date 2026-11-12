package com.jht.progression.data

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(JUnitParamsRunner::class)
class SharedCalculationsTest {
    @Test
    @Parameters(
        "0, 00:00:00",
        "1, 00:00:01",
        "30, 00:00:30",
        "45, 00:00:45",
        "60, 00:01:00",
        "123, 00:02:03",
        "900, 00:15:00",
        "935, 00:15:35",
        "13845, 03:50:45",
        "43845, 12:10:45"
    )
    fun `test secondsToHrMinSecString`(seconds: Int, expected: String) {
        assertEquals(
            "Failed to convert seconds to HH:MM:SS format",
            expected,
            secondsToHrMinSecString(seconds)
        )
    }

    @Test
    @Parameters(
        "00:00:00, 0",
        "000000, 0",
        "00:00:01, 1",
        "000001, 1",
        "00:00:15, 15",
        "000015, 15",
        "00:01:00, 60",
        "000100, 60",
        "12:34:56, 45296",
        "123456, 45296",
        "02:04:06, 7446"
    )
    fun `test convertRaceTimeToSeconds`(raceTime: String, expectedSeconds: Int) {
        assertEquals(
            "Failed to convert race time to seconds",
            expectedSeconds,
            convertRaceTimeToSeconds(raceTime)
        )
    }
}