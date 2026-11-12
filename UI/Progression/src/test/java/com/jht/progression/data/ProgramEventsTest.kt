package com.jht.progression.data

import com.jht.progression.ui.setup.journey.EventStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgramEventsTest {
    private fun workoutEvent(index: Int, startTime: Long, endTime: Long, completed: Boolean): WorkoutEvent {
        return WorkoutEvent(index = index, startTime = startTime, endTime = endTime, completed = completed, label = EventLabel.WORKOUT_TEMPO)
    }

    @Test
    fun `completed event is always COMPLETED regardless of timing`() {
        val event = workoutEvent(0, startTime = 1000, endTime = 2000, completed = true)
        assertEquals(EventStatus.COMPLETED, event.getEventStatus(currentTime = 5000))
    }

    @Test
    fun `event with a start time after now is FUTURE`() {
        val event = workoutEvent(0, startTime = 5000, endTime = 6000, completed = false)
        assertEquals(EventStatus.FUTURE, event.getEventStatus(currentTime = 1000))
    }

    @Test
    fun `event spanning current time is CURRENT`() {
        val event = workoutEvent(0, startTime = 1000, endTime = 2000, completed = false)
        assertEquals(EventStatus.CURRENT, event.getEventStatus(currentTime = 1500))
    }

    @Test
    fun `incomplete workout event that already ended is MISSED`() {
        val event = workoutEvent(0, startTime = 1000, endTime = 2000, completed = false)
        assertEquals(EventStatus.MISSED, event.getEventStatus(currentTime = 3000))
    }

    @Test
    fun `incomplete non-workout event that already ended is treated as completed`() {
        val event = NonWorkoutEvent(index = 0, startTime = 1000, endTime = 2000, completed = false, label = EventLabel.REST)
        assertEquals(EventStatus.COMPLETED, event.getEventStatus(currentTime = 3000))
    }

    @Test
    fun `getDayNum returns the index modulo 7`() {
        assertEquals(0, workoutEvent(0, 0, 0, false).getDayNum())
        assertEquals(6, workoutEvent(6, 0, 0, false).getDayNum())
        assertEquals(0, workoutEvent(7, 0, 0, false).getDayNum())
        assertEquals(3, workoutEvent(10, 0, 0, false).getDayNum())
    }
}
