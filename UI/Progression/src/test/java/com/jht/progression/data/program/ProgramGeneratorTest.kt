package com.jht.progression.data.program

import com.jht.progression.data.EventLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProgramGeneratorTest {
    private val weekLabels = listOf(
        EventLabel.WORKOUT_LONG_SLOW_DISTANCE,
        EventLabel.REST,
        EventLabel.WORKOUT_INTERVAL,
        EventLabel.REST,
        EventLabel.WORKOUT_TEMPO,
        EventLabel.REST,
        EventLabel.REST
    )

    @Test
    fun `createInitial5kProgram produces 8 weeks of events plus a final race`() {
        val programEvents = createInitial5kProgram()
        assertEquals(8 * 7 + 1, programEvents.events.size)
    }

    @Test
    fun `createInitial5kProgram repeats the same weekly label pattern for every week`() {
        val programEvents = createInitial5kProgram()

        for (week in 0..7) {
            val weekEvents = programEvents.events.subList(week * 7, week * 7 + 7)
            assertEquals(weekLabels, weekEvents.map { it.label })
        }
    }

    @Test
    fun `createInitial5kProgram assigns sequentially increasing indices and start times`() {
        val startMs = getWorkoutStartTime()
        val programEvents = createInitial5kProgram()

        programEvents.events.take(56).forEachIndexed { index, event ->
            assertEquals(index, event.index)
            assertEquals(startMs + index * dayInMs, event.startTime)
            assertEquals(event.startTime + dayInMs, event.endTime)
        }
    }

    @Test
    fun `createInitial5kProgram ends with an open ended race event`() {
        val programEvents = createInitial5kProgram()

        val raceEvent = programEvents.events.last()
        assertEquals(EventLabel.RACE, raceEvent.label)
        assertEquals(56, raceEvent.index)
        assertEquals(Long.MAX_VALUE, raceEvent.endTime)
        assertFalse(raceEvent.completed)
    }
}
