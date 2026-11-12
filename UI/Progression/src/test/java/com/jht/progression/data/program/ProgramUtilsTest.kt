package com.jht.progression.data.program

import com.jht.progression.data.EventLabel
import com.jht.progression.data.IEvent
import com.jht.progression.data.NonWorkoutEvent
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramUtilsTest {

    private fun buildEvents(): ProgramEvents {
        val start = getWorkoutStartTime()
        return ProgramEvents(
            listOf(
                WorkoutEvent(0, start, start + dayInMs, completed = true, label = EventLabel.WORKOUT_LONG_SLOW_DISTANCE),
                NonWorkoutEvent(1, start + dayInMs, start + 2 * dayInMs, completed = false, label = EventLabel.REST),
                WorkoutEvent(2, start + 2 * dayInMs, start + 3 * dayInMs, completed = false, label = EventLabel.WORKOUT_INTERVAL),
                NonWorkoutEvent(3, start + 3 * dayInMs, start + 4 * dayInMs, completed = false, label = EventLabel.REST),
                WorkoutEvent(4, start + 4 * dayInMs, Long.MAX_VALUE, completed = false, label = EventLabel.RACE)
            )
        )
    }

    @Test
    fun `skipOverEvent closes out the skipped event and activates the next one`() {
        val start = getWorkoutStartTime()
        val programEvents = buildEvents()

        val result = skipOverEvent(programEvents.events[1], programEvents)

        val skippedEvent = result.events[1]
        assertEquals(start - dayInMs, skippedEvent.startTime)
        assertEquals(start, skippedEvent.endTime)

        val activeEvent = result.events[2]
        assertEquals(start, activeEvent.startTime)
        assertEquals(start + dayInMs, activeEvent.endTime)

        val laterEvent = result.events[3]
        assertEquals(start + dayInMs, laterEvent.startTime)
        assertEquals(start + 2 * dayInMs, laterEvent.endTime)

        assertEquals(programEvents.events[0], result.events[0])
    }

    @Test
    fun `skipOverEvent never closes the race event unless skipRace is set`() {
        val programEvents = buildEvents()
        val raceIndex = programEvents.events.size - 1

        val result = skipOverEvent(programEvents.events[raceIndex - 1], programEvents)

        assertEquals(Long.MAX_VALUE, result.events[raceIndex].endTime)
    }

    @Test
    fun `skipOverEvent with skipRace true allows closing the race event`() {
        val start = getWorkoutStartTime()
        val programEvents = buildEvents()
        val raceIndex = programEvents.events.size - 1

        val result = skipOverEvent(programEvents.events[raceIndex], programEvents, skipRace = true)

        assertEquals(start, result.events[raceIndex].endTime)
    }

    @Test
    fun `debugShiftEvents moves every event one day sooner`() {
        val start = getWorkoutStartTime()
        val programEvents = buildEvents()

        val result = debugShiftEvents(programEvents)

        result.events.forEachIndexed { index, event ->
            assertEquals(programEvents.events[index].startTime - dayInMs, event.startTime)
        }
    }

    @Test
    fun `debugShiftEvents keeps the race event open`() {
        val programEvents = buildEvents()
        val raceIndex = programEvents.events.size - 1

        val result = debugShiftEvents(programEvents)

        assertEquals(Long.MAX_VALUE, result.events[raceIndex].endTime)
    }

    @Test
    fun `skipToEvent resets untouched past events and reschedules the rest`() {
        val start = getWorkoutStartTime()
        val programEvents = ProgramEvents(
            listOf(
                // Already fully in the past, endTime before today, left untouched
                WorkoutEvent(0, start - 3 * dayInMs, start - 2 * dayInMs, completed = true, label = EventLabel.WORKOUT_LONG_SLOW_DISTANCE),
                // Spans across today, gets pushed fully into the past
                NonWorkoutEvent(1, start - dayInMs, start + dayInMs, completed = false, label = EventLabel.REST),
                // The event we skip to, becomes the current event
                WorkoutEvent(2, start + dayInMs, start + 2 * dayInMs, completed = false, label = EventLabel.WORKOUT_INTERVAL),
                // Comes after the skip target, gets rescheduled a day later
                NonWorkoutEvent(3, start + 2 * dayInMs, start + 3 * dayInMs, completed = false, label = EventLabel.REST)
            )
        )

        val result = skipToEvent(programEvents.events[2], programEvents)

        val untouchedEvent = result.events[0]
        assertEquals(programEvents.events[0], untouchedEvent)

        val pushedIntoPast = result.events[1]
        assertEquals(start - dayInMs, pushedIntoPast.startTime)
        assertEquals(start - dayInMs, pushedIntoPast.endTime)

        val activeEvent = result.events[2]
        assertEquals(start, activeEvent.startTime)
        assertEquals(start + dayInMs, activeEvent.endTime)

        val rescheduledEvent = result.events[3]
        assertEquals(start + dayInMs, rescheduledEvent.startTime)
        assertEquals(start + 2 * dayInMs, rescheduledEvent.endTime)
    }

    @Test
    fun `skipToEvent keeps the race event open when skipping to it`() {
        val programEvents = buildEvents()
        val raceIndex = programEvents.events.size - 1

        val result = skipToEvent(programEvents.events[raceIndex], programEvents)

        assertEquals(Long.MAX_VALUE, result.events[raceIndex].endTime)
    }

    @Test
    fun `redoEvent reschedules from the target event and resets completion`() {
        val start = getWorkoutStartTime()
        val programEvents = buildEvents()

        val result = redoEvent(programEvents.events[0], programEvents)

        result.events.forEachIndexed { index, event ->
            assertFalse(event.completed)
            assertEquals(start + index * dayInMs, event.startTime)
        }

        val raceIndex = programEvents.events.size - 1
        assertEquals(Long.MAX_VALUE, result.events[raceIndex].endTime)
    }

    @Test
    fun `programEventsNeedMigration is false when there is no race event required`() {
        assertFalse(programEventsNeedMigration(ProgramEvents(emptyList())))
    }

    @Test
    fun `programEventsNeedMigration is true when the last event is not a race`() {
        val programEvents = ProgramEvents(
            listOf(createRestEvent(0, getWorkoutStartTime()))
        )

        assertTrue(programEventsNeedMigration(programEvents))
    }

    @Test
    fun `programEventsNeedMigration is false when the last event is already a race`() {
        val programEvents = buildEvents()

        assertFalse(programEventsNeedMigration(programEvents))
    }

    @Test
    fun `migrateProgramEvents appends a race event when migration is needed`() {
        val start = getWorkoutStartTime()
        val lastEvent = createRestEvent(0, start)
        val programEvents = ProgramEvents(listOf(lastEvent))

        val result = migrateProgramEvents(programEvents)

        assertEquals(2, result.events.size)
        val raceEvent = result.events[1]
        assertEquals(EventLabel.RACE, raceEvent.label)
        assertEquals(lastEvent.index + 1, raceEvent.index)
        assertEquals(lastEvent.startTime + dayInMs, raceEvent.startTime)
        assertEquals(Long.MAX_VALUE, raceEvent.endTime)
        assertFalse(raceEvent.completed)
    }

    @Test
    fun `migrateProgramEvents returns the same events when no migration is needed`() {
        val programEvents = buildEvents()

        val result = migrateProgramEvents(programEvents)

        assertEquals(programEvents, result)
    }

    @Test
    fun `createWorkoutEvent builds an incomplete workout event with the given fields`() {
        val start = getWorkoutStartTime()

        val event: IEvent = createWorkoutEvent(EventLabel.WORKOUT_TEMPO, index = 5, startTime = start)

        assertEquals(EventLabel.WORKOUT_TEMPO, event.label)
        assertEquals(5, event.index)
        assertEquals(start, event.startTime)
        assertEquals(start + dayInMs, event.endTime)
        assertFalse(event.completed)
    }

    @Test
    fun `createRestEvent builds an incomplete rest event spanning one day`() {
        val start = getWorkoutStartTime()

        val event = createRestEvent(index = 3, startTime = start)

        assertEquals(EventLabel.REST, event.label)
        assertEquals(3, event.index)
        assertEquals(start, event.startTime)
        assertEquals(start + dayInMs, event.endTime)
        assertFalse(event.completed)
    }
}
