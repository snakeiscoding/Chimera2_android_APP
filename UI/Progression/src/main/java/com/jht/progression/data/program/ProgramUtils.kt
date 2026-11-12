package com.jht.progression.data.program

import com.jht.progression.data.EventLabel
import com.jht.progression.data.IEvent
import com.jht.progression.data.NonWorkoutEvent
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import java.time.LocalDate
import java.time.ZoneId


/** Set if you want to see the workouts actually advancing */
private const val DEBUG_TIMING = false
val dayInMs = if (DEBUG_TIMING) 10000L else 86400000L

/**
 * Skips the current event and sets the next event as currently active. This will shift
 * subsequent events so that they are sooner by one day.
 */
fun skipOverEvent(
    event: IEvent,
    programEvents: ProgramEvents,
    skipRace: Boolean = false
): ProgramEvents {
    val events = programEvents.events
    val mutableEvents = events.toMutableList()

    val startTime = getWorkoutStartTime()
    for ((day, i) in (event.index until events.size).withIndex()) {
        mutableEvents[i] = updateEventTime(
            event = mutableEvents[i],
            startTime = startTime + ((day - 1) * dayInMs),
            allowSkipRace = skipRace
        )
    }

    return programEvents.copy(events = mutableEvents.toList())
}

/**
 * Shifts all events to happen one day sooner. This means an event will almost certainly be skipped
 * when running this. ONLY FOR DEBUGGING.
 */
fun debugShiftEvents(programEvents: ProgramEvents): ProgramEvents {
    val events = programEvents.events
    val mutableEvents = events.toMutableList()

    for (index in events.indices) {
        mutableEvents[index] = updateEventTime(
            event = mutableEvents[index],
            startTime = mutableEvents[index].startTime - dayInMs,
            allowSkipRace = false
        )
    }

    return programEvents.copy(events = mutableEvents.toList())
}

/**
 * Changes the selected event to be currently active. Changes prior events that would intersect
 * to start and end before the current start time. Later events are rescheduled to be after
 * the event we skip to.
 */
fun skipToEvent(event: IEvent, programEvents: ProgramEvents): ProgramEvents {
    val events = programEvents.events
    val mutableEvents = events.toMutableList()

    val startTime = getWorkoutStartTime()
    val eventIndex = event.index

    events.forEachIndexed { index, programEvent ->
        // If this is before the event we are skipping to
        if (index < eventIndex) {
            // If the end time is past the current start time, reset the start and end times of the event to before the current start time
            if (programEvent.endTime > startTime) {
                val updatedEvent = updateEventTime(
                    event = programEvent,
                    startTime = startTime - dayInMs, // Put this event in the past an arbitrary amount of time
                    endTime = startTime - dayInMs,
                    resetCompletion = false
                )

                mutableEvents[index] = updatedEvent
            }
        } else { // If this is the event we are skipping to or later
            val day = index - eventIndex
            val updatedEvent = updateEventTime(
                programEvent,
                startTime + (day * dayInMs)
            )

            mutableEvents[index] = updatedEvent
        }
    }

    return programEvents.copy(events = mutableEvents.toList())
}

/**
 * Changes the selected event to be currently active, resets all progress for events after
 * the selected event, and schedules all subsequent events after the selected event.
 */
fun redoEvent(event: IEvent, programEvents: ProgramEvents): ProgramEvents {
    val events = programEvents.events
    val mutableEvents = events.toMutableList()

    val startTime = getWorkoutStartTime()
    for ((day, i) in (event.index until events.size).withIndex()) {
        mutableEvents[i] = updateEventTime(
            mutableEvents[i],
            startTime = startTime + (day * dayInMs),
            resetCompletion = true
        )
    }

    return programEvents.copy(events = mutableEvents.toList())
}

private fun updateEventTime(
    event: IEvent,
    startTime: Long,
    endTime: Long = startTime + dayInMs,
    resetCompletion: Boolean = false,
    allowSkipRace: Boolean = false
): IEvent {
    // If this is a race event it's always the last event, and should never end unless we specifically want it to
    val actualEndTime = if (event.label == EventLabel.RACE && !allowSkipRace) {
        Long.MAX_VALUE
    } else {
        endTime
    }

    return when (event) {
        is WorkoutEvent -> event.copy(
            startTime = startTime,
            endTime = actualEndTime,
            completed = if (resetCompletion) false else event.completed
        )

        is NonWorkoutEvent -> event.copy(
            startTime = startTime,
            endTime = actualEndTime,
            completed = if (resetCompletion) false else event.completed
        )
    }
}

/**
 * Gets the start time of a workout event in the local time.
 */
fun getWorkoutStartTime(dayOffset: Int = 0): Long {
    if (DEBUG_TIMING) {
        return System.currentTimeMillis()
    }

    val startTime = LocalDate.now().atTime(0, 0).plusDays(dayOffset.toLong())
    return startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/**
 * Checks whether the program events are valid or whether they need to be migrated to a newer version
 */
fun programEventsNeedMigration(programEvents: ProgramEvents): Boolean {
    // If there's no race event we need to update the program events to have one
    if (programEvents.events.isNotEmpty()) {
        val lastEvent = programEvents.events.last()
        return lastEvent.label != EventLabel.RACE
    }

    return false
}

/**
 * Migrates the program events and passes back an updated program events to save for a user
 */
fun migrateProgramEvents(programEvents: ProgramEvents): ProgramEvents {
    return if (programEventsNeedMigration(programEvents)) {
        val newProgramEvents = addRaceEvent(programEvents)
        newProgramEvents
    } else {
        programEvents
    }
}

/**
 * Early versions of method didn't include races, this adds a race event to the end of the program
 */
private fun addRaceEvent(programEvents: ProgramEvents) : ProgramEvents {
    val mutableEvents = programEvents.events.toMutableList()
    val lastEvent = programEvents.events.last()
    mutableEvents.add(
        createWorkoutEvent(
            label = EventLabel.RACE,
            startTime = lastEvent.startTime + dayInMs,
            endTime = Long.MAX_VALUE,
            index = lastEvent.index + 1,
            complete = false
        )
    )

    return programEvents.copy(events = mutableEvents.toList())
}

/**
 * Creates a workout event that can be put into a program
 */
fun createWorkoutEvent(
    label: EventLabel,
    index: Int,
    startTime: Long,
    endTime: Long = startTime + dayInMs,
    complete: Boolean = false
): WorkoutEvent {
    return WorkoutEvent(
        index = index,
        startTime = startTime,
        endTime = endTime,
        completed = complete,
        label = label
    )
}

/**
 * Creates a reset event that can be put into a program
 */
fun createRestEvent(index: Int, startTime: Long): NonWorkoutEvent {
    return NonWorkoutEvent(
        index = index,
        startTime = startTime,
        endTime = startTime + dayInMs,
        completed = false,
        label = EventLabel.REST
    )
}