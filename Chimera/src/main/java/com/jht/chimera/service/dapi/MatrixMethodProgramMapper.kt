package com.jht.chimera.service.dapi

import android.util.Log
import com.jht.enums.ProgramType
import com.jht.oauthlib.api.exerciser.MatrixMethodEvent
import com.jht.oauthlib.api.exerciser.MatrixMethodProgram
import com.jht.oauthlib.api.exerciser.MatrixMethodProgramEvents
import com.jht.oauthlib.api.exerciser.MatrixMethodProgramEventsUpdate
import com.jht.oauthlib.api.exerciser.MatrixMethodProgramMetadata
import com.jht.progression.data.EventLabel
import com.jht.progression.data.IEvent
import com.jht.progression.data.NonWorkoutEvent
import com.jht.progression.data.ProgramEvents
import com.jht.progression.data.WorkoutEvent
import com.jht.progression.userinfo.UserInfo
import java.time.Instant
import java.time.format.DateTimeParseException

// Converts between JUMP's Matrix Method data classes (oauthlib) and the console's UserInfo
// (UI/Progression). API: https://orion.jfit.co/docs/#/Matrix%20Method
private const val TAG = "MatrixMethodProgramMapper"

private const val MAX_SAFE_INTEGER = 9007199254740991L

/** A plan with no events and no baseline is a shell, so it must not be applied over real progress. */
fun isActiveJumpPlan(program: MatrixMethodProgram): Boolean =
    program.programEvents.events.isNotEmpty() || program.baselineKph > 0f

/** Maps a JUMP program to the console's [UserInfo], or null if its programId is not one this console knows. */
fun jumpProgramToUserInfo(program: MatrixMethodProgram, nowMs: Long): UserInfo? {
    val programType = ProgramType.entries.firstOrNull { it.id == program.programId }
    if (programType == null) {
        Log.w(TAG, "Unrecognized programId ${program.programId}, cannot map program")
        return null
    }
    return UserInfo(
        baselineKph = program.baselineKph,
        assessmentBaselineKph = program.assessmentBaselineKph,
        // JUMP uses 0 for "not set", so map it back to null to keep local != null checks working
        goalBaselineKph = program.goalBaselineKph.takeIf { it != 0f },
        goalIncrementKph = program.goalIncrementKph.takeIf { it != 0f },
        backupGoalBaseline = program.backupGoalBaseline.takeIf { it != 0f },
        autoAdjustGoalBaseline = program.autoAdjustGoalBaseline,
        programEvents = ProgramEvents(program.programEvents.events.map(::toIEvent)),
        goalTimeInfoAcknowledged = program.goalTimeAcknowledged,
        lastChangedTime = jumpUpdatedAtMs(program.updatedAt) ?: nowMs,
        programType = programType,
    )
}

/** Server change time from the undocumented `updatedAt`. Compared against console time, so clock skew matters. */
private fun jumpUpdatedAtMs(updatedAt: String?): Long? {
    if (updatedAt.isNullOrBlank()) return null
    return try {
        Instant.parse(updatedAt).toEpochMilli()
    } catch (e: DateTimeParseException) {
        Log.w(TAG, "Could not parse updatedAt '$updatedAt', falling back to current time")
        null
    }
}

/** Body for POST /program, which needs a client supplied id and does accept programEvents. */
fun userInfoToJumpProgram(userInfo: UserInfo, programId: String): MatrixMethodProgram = MatrixMethodProgram(
    id = programId,
    programId = userInfo.programType.id,
    baselineKph = userInfo.baselineKph,
    assessmentBaselineKph = userInfo.assessmentBaselineKph,
    // JUMP 400s on null for these three; 0 is its "not set" value
    goalBaselineKph = userInfo.goalBaselineKph ?: 0f,
    goalIncrementKph = userInfo.goalIncrementKph ?: 0f,
    backupGoalBaseline = userInfo.backupGoalBaseline ?: 0f,
    goalTimeAcknowledged = userInfo.goalTimeInfoAcknowledged,
    autoAdjustGoalBaseline = userInfo.autoAdjustGoalBaseline,
    programEvents = MatrixMethodProgramEvents(userInfo.programEvents.events.map(::toJumpEvent)),
)

/** Metadata only for PATCH /program. It rejects programEvents and userId, but requires programId despite the schema. */
fun userInfoToJumpMetadata(userInfo: UserInfo, programId: String): MatrixMethodProgramMetadata = MatrixMethodProgramMetadata(
    id = programId,
    programId = userInfo.programType.id,
    baselineKph = userInfo.baselineKph,
    assessmentBaselineKph = userInfo.assessmentBaselineKph,
    goalBaselineKph = userInfo.goalBaselineKph ?: 0f,
    goalIncrementKph = userInfo.goalIncrementKph ?: 0f,
    backupGoalBaseline = userInfo.backupGoalBaseline ?: 0f,
    goalTimeAcknowledged = userInfo.goalTimeInfoAcknowledged,
    autoAdjustGoalBaseline = userInfo.autoAdjustGoalBaseline,
)

/** Events keyed by index, limited to those differing from [previous]; indices dropped from [previous] send null to delete them on JUMP. Null previous sends all; returns null if nothing changed. */
fun userInfoToJumpEventsUpdate(userInfo: UserInfo, programId: String, previous: UserInfo? = null): MatrixMethodProgramEventsUpdate? {
    val previousByIndex = previous?.programEvents?.events?.associateBy { it.index } ?: emptyMap()
    val currentByIndex = userInfo.programEvents.events.associateBy { it.index }
    val eventsToUpdate: Map<String, MatrixMethodEvent?> = buildMap {
        currentByIndex.forEach { (index, event) ->
            if (previousByIndex[index] != event) put(index.toString(), toJumpEvent(event))
        }
        (previousByIndex.keys - currentByIndex.keys).forEach { index -> put(index.toString(), null) }
    }
    return if (eventsToUpdate.isEmpty()) null else MatrixMethodProgramEventsUpdate(programId, eventsToUpdate)
}

/** Clamps out of range timestamps, since Long.MAX_VALUE end times make JUMP reject the whole request. */
private fun toJumpEvent(event: IEvent): MatrixMethodEvent {
    val startTime = event.startTime.coerceAtMost(MAX_SAFE_INTEGER)
    val endTime = event.endTime.coerceAtMost(MAX_SAFE_INTEGER)
    return when (event) {
        is WorkoutEvent -> MatrixMethodEvent(
            index = event.index,
            startTime = startTime,
            endTime = endTime,
            completed = event.completed,
            label = event.label.id,
            workoutDistance = event.workoutDistanceKm,
            workoutTime = event.workoutTimeMs,
            totalDistance = event.totalDistanceKm,
            totalTime = event.totalTimeSec,
            totalCalories = event.totalCalories,
            totalAverageSpeed = event.totalAverageSpeedKph,
            totalPeakSpeed = event.totalPeakSpeedKph,
            totalAverageHeartRate = event.totalAverageHR,
            totalElevation = event.totalElevationMeters,
            baseline = event.baselineKph,
            goalTimeExisted = event.goalTimeExisted,
        )
        is NonWorkoutEvent -> MatrixMethodEvent(
            index = event.index,
            startTime = startTime,
            endTime = endTime,
            completed = event.completed,
            label = event.label.id,
        )
    }
}

private fun toIEvent(event: MatrixMethodEvent): IEvent {
    val label = EventLabel.entries.firstOrNull { it.id == event.label } ?: EventLabel.REST
    return if (isWorkoutLabel(label)) {
        WorkoutEvent(
            index = event.index,
            startTime = event.startTime,
            endTime = event.endTime,
            completed = event.completed,
            label = label,
            workoutDistanceKm = event.workoutDistance,
            workoutTimeMs = event.workoutTime,
            totalDistanceKm = event.totalDistance,
            totalTimeSec = event.totalTime,
            totalCalories = event.totalCalories,
            totalAverageSpeedKph = event.totalAverageSpeed,
            totalPeakSpeedKph = event.totalPeakSpeed,
            totalAverageHR = event.totalAverageHeartRate,
            totalElevationMeters = event.totalElevation,
            baselineKph = event.baseline,
            goalTimeExisted = event.goalTimeExisted,
        )
    } else {
        NonWorkoutEvent(
            index = event.index,
            startTime = event.startTime,
            endTime = event.endTime,
            completed = event.completed,
            label = label,
        )
    }
}

private fun isWorkoutLabel(label: EventLabel): Boolean = when (label) {
    EventLabel.WORKOUT_LONG_SLOW_DISTANCE, EventLabel.WORKOUT_INTERVAL, EventLabel.WORKOUT_TEMPO, EventLabel.RACE -> true
    EventLabel.REST, EventLabel.CROSS, EventLabel.ACTIVE_RECOVERY -> false
}
