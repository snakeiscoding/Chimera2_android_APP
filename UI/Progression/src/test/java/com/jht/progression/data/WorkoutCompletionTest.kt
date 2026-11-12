package com.jht.progression.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers marking a program event complete, with no ViewModel or DataMatrix plumbing involved. */
class WorkoutCompletionTest {

    private fun workoutEvent(index: Int, completed: Boolean = false) = WorkoutEvent(
        index = index, startTime = 0L, endTime = 0L, completed = completed, label = EventLabel.WORKOUT_TEMPO
    )

    private fun restEvent(index: Int, completed: Boolean = false) = NonWorkoutEvent(
        index = index, startTime = 0L, endTime = 0L, completed = completed, label = EventLabel.REST
    )

    private val stats = CompletedWorkoutStats(
        workoutDistanceKm = 5.2f,
        workoutTimeMs = 1_800_000,
        totalTimeSec = 2100,
        totalDistanceKm = 6.1f,
        totalCalories = 320f,
        totalAverageSpeedKph = 10.4f,
        totalPeakSpeedKph = 13.7f,
        totalAverageHR = 148,
        totalElevationMeters = 42f,
        baselineKph = 9.5f,
        goalTimeExisted = true,
    )

    private fun applied(result: WorkoutCompletionResult): ProgramEvents {
        assertTrue("expected Applied but was $result", result is WorkoutCompletionResult.Applied)
        return (result as WorkoutCompletionResult.Applied).events
    }

    @Test
    fun `records every stat on the completed workout event`() {
        val events = ProgramEvents(listOf(workoutEvent(0), workoutEvent(1)))

        val updated = applied(applyWorkoutCompletion(1, events, stats)).events[1] as WorkoutEvent

        assertTrue(updated.completed)
        assertEquals(5.2f, updated.workoutDistanceKm)
        assertEquals(1_800_000, updated.workoutTimeMs)
        assertEquals(2100, updated.totalTimeSec)
        assertEquals(6.1f, updated.totalDistanceKm)
        assertEquals(320f, updated.totalCalories)
        assertEquals(10.4f, updated.totalAverageSpeedKph)
        assertEquals(13.7f, updated.totalPeakSpeedKph)
        assertEquals(148, updated.totalAverageHR)
        assertEquals(42f, updated.totalElevationMeters)
        assertEquals(9.5f, updated.baselineKph)
        assertTrue(updated.goalTimeExisted)
    }

    @Test
    fun `completes a non workout event without recording workout stats`() {
        val events = ProgramEvents(listOf(restEvent(0)))

        val updated = applied(applyWorkoutCompletion(0, events, stats)).events[0]

        assertTrue(updated is NonWorkoutEvent)
        assertTrue(updated.completed)
    }

    @Test
    fun `leaves the other events untouched`() {
        val events = ProgramEvents(listOf(workoutEvent(0), workoutEvent(1), restEvent(2)))

        val result = applied(applyWorkoutCompletion(1, events, stats)).events

        assertEquals(3, result.size)
        assertEquals(events.events[0], result[0])
        assertEquals(events.events[2], result[2])
    }

    @Test
    fun `does not mutate the events it was given`() {
        val original = listOf(workoutEvent(0), workoutEvent(1))
        val events = ProgramEvents(original)

        applyWorkoutCompletion(1, events, stats)

        assertEquals(original, events.events)
        assertTrue(events.events.none { it.completed })
    }

    @Test
    fun `reports out of range when the program is empty`() {
        // The crash case: a session reset cleared the program but not the event index
        val result = applyWorkoutCompletion(4, ProgramEvents(emptyList()), stats)

        assertEquals(WorkoutCompletionResult.IndexOutOfRange(index = 4, eventCount = 0), result)
    }

    @Test
    fun `reports out of range when the index is past the last event`() {
        val events = ProgramEvents(listOf(workoutEvent(0), workoutEvent(1)))

        assertEquals(
            WorkoutCompletionResult.IndexOutOfRange(index = 2, eventCount = 2),
            applyWorkoutCompletion(2, events, stats)
        )
    }

    @Test
    fun `reports out of range for a negative index`() {
        val events = ProgramEvents(listOf(workoutEvent(0)))

        assertEquals(
            WorkoutCompletionResult.IndexOutOfRange(index = -1, eventCount = 1),
            applyWorkoutCompletion(-1, events, stats)
        )
    }

    @Test
    fun `completing an already completed event is idempotent`() {
        val events = ProgramEvents(listOf(workoutEvent(0, completed = true)))

        val updated = applied(applyWorkoutCompletion(0, events, stats)).events[0] as WorkoutEvent

        assertTrue(updated.completed)
        assertEquals(5.2f, updated.workoutDistanceKm)
    }
}
