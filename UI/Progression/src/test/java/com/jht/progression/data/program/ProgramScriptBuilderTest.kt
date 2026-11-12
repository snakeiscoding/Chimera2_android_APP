package com.jht.progression.data.program

import com.jht.enums.IntervalType
import com.jht.progression.data.program.ProgramScriptBuilder.ScriptInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramScriptBuilderTest {
    private val builder = ProgramScriptBuilder()

    @Test
    fun `script includes settings warmup workout and cooldown stages in order`() {
        val script = builder.getScriptString(listOf(ScriptInterval(60000, 5f, IntervalType.ZONE_3)), zone1Speed = 2f)

        assertTrue(script.startsWith("settings\n"))
        val warmupIndex = script.indexOf("stage warmup")
        val workoutIndex = script.indexOf("stage workout")
        val cooldownIndex = script.indexOf("stage cooldown")
        assertTrue(warmupIndex in 0 until workoutIndex)
        assertTrue(workoutIndex < cooldownIndex)
    }

    @Test
    fun `warmup and cooldown stages use zone1 speed and a 5 minute goal`() {
        val script = builder.getScriptString(listOf(ScriptInterval(60000, 5f, IntervalType.ZONE_3)), zone1Speed = 2f)
        val warmupSection = script.substringAfter("stage warmup").substringBefore("stage workout")
        val cooldownSection = script.substringAfter("stage cooldown")

        assertTrue(warmupSection.contains("GOAL = 300000"))
        assertTrue(warmupSection.contains("SPEED = 2.0"))
        assertTrue(cooldownSection.contains("GOAL = 300000"))
        assertTrue(cooldownSection.contains("SPEED = 2.0"))
    }

    @Test
    fun `workout stage goal is the sum of interval times`() {
        val intervals = listOf(
            ScriptInterval(60000, 5f, IntervalType.ZONE_3),
            ScriptInterval(30000, 8f, IntervalType.ZONE_4)
        )
        val script = builder.getScriptString(intervals, zone1Speed = 2f)
        val workoutSection = script.substringAfter("stage workout").substringBefore("stage cooldown")

        assertTrue(workoutSection.contains("GOAL = 90000"))
        assertEquals(2, Regex("INTERVAL_GOAL = ").findAll(workoutSection).count())
        assertTrue(workoutSection.contains("SPEED = 5.0"))
        assertTrue(workoutSection.contains("SPEED = 8.0"))
    }

    @Test
    fun `empty interval list produces no workout stage`() {
        val script = builder.getScriptString(emptyList(), zone1Speed = 2f)
        assertFalse(script.contains("stage workout"))
    }
}
