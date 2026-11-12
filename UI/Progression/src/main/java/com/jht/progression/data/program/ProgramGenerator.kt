package com.jht.progression.data.program

import com.jht.progression.data.EventLabel
import com.jht.progression.data.IEvent
import com.jht.progression.data.ProgramEvents

/*
  TODO: This logic should have an interface so that future programs (10k, Marathon, etc) can
    implement their logic for these methods, thus decoupling Matrix Method from the 5k program. If
    this logic is not actually 5k specific, this file should be renamed to reflect that.
*/

/**
 * Initializes the events for a 5k program. The events define how the program is viewed on the
 * journey screen.
 */
fun createInitial5kProgram(dayOffset: Int = 0): ProgramEvents {
    var day = 0
    val startMs = getWorkoutStartTime(dayOffset)
    val eventList = ArrayList<IEvent>()

    for (i in 0..7) {
        eventList.add(
            createWorkoutEvent(
                EventLabel.WORKOUT_LONG_SLOW_DISTANCE,
                day,
                startMs + (dayInMs * day++)
            )
        )
        eventList.add(createRestEvent(day, startMs + (dayInMs * day++)))
        eventList.add(
            createWorkoutEvent(
                EventLabel.WORKOUT_INTERVAL,
                day,
                startMs + (dayInMs * day++)
            )
        )
        eventList.add(createRestEvent(day, startMs + (dayInMs * day++)))
        eventList.add(
            createWorkoutEvent(
                EventLabel.WORKOUT_TEMPO,
                day,
                startMs + (dayInMs * day++)
            )
        )
        eventList.add(createRestEvent(day, startMs + (dayInMs * day++)))
        eventList.add(createRestEvent(day, startMs + (dayInMs * day++)))
    }

    eventList.add(
        createWorkoutEvent(
            label = EventLabel.RACE,
            index = day,
            startTime = startMs + (dayInMs * day),
            endTime = Long.MAX_VALUE
        )
    )

    return ProgramEvents(eventList)
}