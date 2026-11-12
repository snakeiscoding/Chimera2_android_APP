package com.jht.progression.userinfo

import android.util.Log
import androidx.core.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.jht.progression.data.EventLabel
import com.jht.progression.data.IEvent
import com.jht.progression.data.NonWorkoutEvent
import com.jht.progression.data.WorkoutEvent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "UserInfoUtil"

private val lock = Any()
private const val PROGRAM_INFO_FOLDER = "mm_user_program_info"
private const val USER_PROGRAM_FILE = "user_program_info.json"

private fun createProgressionGson(): Gson =
    GsonBuilder().registerTypeAdapter(IEvent::class.java, EventTypeAdapter()).create()

fun getUserProgramString(userInfo5KParams: UserInfo) : String {
    return createProgressionGson().toJson(userInfo5KParams)
}

internal fun writeUserProgramInfo(
    userProgramString: String,
    userId: String,
    filesDir: File,
) {
    synchronized(lock) {
        // Write file using atomic file to avoid corruption of the program
        val file = getUserInfoFile(userId, filesDir)
        val atomicFile = AtomicFile(file)
        val fos: FileOutputStream = atomicFile.startWrite()
        try {
            fos.write(userProgramString.toByteArray())
            atomicFile.finishWrite(fos)
        } catch (e: IOException) {
            Log.e(TAG, "Could not write user info file " + file.path, e)
            atomicFile.failWrite(fos)
        }
    }
}

/**
 * Deletes a user's data from Matrix Method. All data will be deleted from the program.
 */
fun deleteMatrixMethodUserData(userId: String, filesDir: File) {
    val userFolder = getUserInfoFile(userId, filesDir)
    if (userFolder.exists()) {
        val successful = userFolder.deleteRecursively()
        if (!successful) {
            Log.e(TAG, "Failed to delete Matrix Method user data for $userId")
        }
    }
}

internal fun readUserProgramInfoFile(userId: String, filesDir: File): String? {
    synchronized(lock) {
        val file = getUserInfoFile(userId, filesDir)
        val atomicFile = AtomicFile(file)
        return if (file.exists()) {
            val fis = atomicFile.openRead()
            fis.reader(Charsets.UTF_8).use { it.readText() }
        } else {
            null
        }
    }
}

fun getUserInfoFromJsonString(programString: String): UserInfo {
    try {
        return createProgressionGson().fromJson(
            programString,
            UserInfo::class.java
        ) ?: UserInfo()
    } catch (e: JsonSyntaxException) {
        Log.e(TAG, "Could not read user info file, returning blank program!", e)
        return UserInfo()
    }
}

/**
 * Helps GSON read and write JSON representations of the program events. Without this GSON will
 * not be able to create the program events correctly.
 *
 * When changing the events we will need to adjust this as well so we can read and store the events
 * correctly.
 */
private class EventTypeAdapter : TypeAdapter<IEvent>() {
    private val indexString = "index"
    private val startTimeString = "startTime"
    private val endTimeString = "endTime"
    private val completedString = "completed"
    private val labelString = "label"
    private val totalDistanceString = "totalDistance"
    private val totalTimeString = "totalTime"
    private val workoutDistanceString = "workoutDistance"
    private val workoutTimeString = "workoutTime"
    private val totalCaloriesString = "totalCalories"
    private val totalAverageSpeedString = "totalAverageSpeed"
    private val totalPeakSpeedString = "totalPeakSpeed"
    private val totalAverageHeartRateString = "totalAverageHeartRate"
    private val totalElevationString = "totalElevation"
    private val baselineString = "baseline"
    private val goalTimeExistedString = "goalTimeExisted"

    override fun read(reader: JsonReader?): IEvent {
        if (reader == null) {
            Log.e(TAG, "Reader is null, failed to read json")
            return NonWorkoutEvent(0, 0, 0, false, EventLabel.REST)
        }

        reader.beginObject()
        var index = 0
        var startTime: Long = 0
        var endTime: Long = 0
        var completed = false
        var label: EventLabel = EventLabel.REST

        // Data for workout history
        var workoutTime = 0
        var workoutDistance = 0f
        var totalDistance = 0f
        var totalTime = 0
        var totalCalories = 0f
        var totalAverageSpeed = 0f
        var totalPeakSpeed = 0f
        var totalAverageHR = 0
        var totalElevation = 0f
        var baseline = 0f
        var goalTimeExisted = false

        while (reader.hasNext()) {
            val token = reader.peek()
            if (token.equals(JsonToken.NAME)) {
                when (reader.nextName()) {
                    indexString -> index = reader.nextInt()
                    startTimeString -> startTime = reader.nextLong()
                    endTimeString -> endTime = reader.nextLong()
                    completedString -> completed = reader.nextBoolean()
                    labelString -> label = getEventLabel(reader.nextInt())
                    workoutDistanceString -> workoutDistance = reader.nextDouble().toFloat()
                    workoutTimeString -> workoutTime = reader.nextInt()
                    totalDistanceString -> totalDistance = reader.nextDouble().toFloat()
                    totalTimeString -> totalTime = reader.nextInt()
                    totalCaloriesString -> totalCalories = reader.nextDouble().toFloat()
                    totalAverageSpeedString -> totalAverageSpeed = reader.nextDouble().toFloat()
                    totalPeakSpeedString -> totalPeakSpeed = reader.nextDouble().toFloat()
                    totalAverageHeartRateString -> totalAverageHR = reader.nextInt()
                    totalElevationString -> totalElevation = reader.nextDouble().toFloat()
                    baselineString -> baseline = reader.nextDouble().toFloat()
                    goalTimeExistedString -> goalTimeExisted = reader.nextBoolean()
                }
            }
        }
        reader.endObject()

        return if (isWorkoutEvent(label)) {
            WorkoutEvent(
                index = index,
                startTime = startTime,
                endTime = endTime,
                completed = completed,
                label = label,
                workoutDistanceKm = workoutDistance,
                workoutTimeMs = workoutTime,
                totalDistanceKm = totalDistance,
                totalTimeSec = totalTime,
                totalCalories = totalCalories,
                totalAverageSpeedKph = totalAverageSpeed,
                totalPeakSpeedKph = totalPeakSpeed,
                totalAverageHR = totalAverageHR,
                totalElevationMeters = totalElevation,
                baselineKph = baseline,
                goalTimeExisted = goalTimeExisted
            )
        } else {
            NonWorkoutEvent(
                index = index,
                startTime = startTime,
                endTime = endTime,
                completed = completed,
                label = label
            )
        }
    }

    private fun getEventLabel(id: Int): EventLabel {
        return when (id) {
            EventLabel.WORKOUT_LONG_SLOW_DISTANCE.id -> EventLabel.WORKOUT_LONG_SLOW_DISTANCE
            EventLabel.WORKOUT_INTERVAL.id -> EventLabel.WORKOUT_INTERVAL
            EventLabel.WORKOUT_TEMPO.id -> EventLabel.WORKOUT_TEMPO
            EventLabel.RACE.id -> EventLabel.RACE
            EventLabel.REST.id -> EventLabel.REST
            EventLabel.CROSS.id -> EventLabel.CROSS
            EventLabel.ACTIVE_RECOVERY.id -> EventLabel.ACTIVE_RECOVERY
            else -> {
                Log.e(TAG, "Incorrect id $id")
                EventLabel.REST
            }
        }
    }

    private fun isWorkoutEvent(label: EventLabel): Boolean {
        return when (label) {
            // Workout events
            EventLabel.WORKOUT_LONG_SLOW_DISTANCE, EventLabel.WORKOUT_INTERVAL, EventLabel.WORKOUT_TEMPO, EventLabel.RACE -> true

            // Non workout events
            EventLabel.REST, EventLabel.CROSS, EventLabel.ACTIVE_RECOVERY -> false
        }
    }

    override fun write(writer: JsonWriter?, event: IEvent?) {
        if (writer == null || event == null) {
            Log.e(TAG, "Error writing file, null writer or event")
            return
        }

        writer.beginObject()
        writer.name(indexString)
        writer.value(event.index)
        writer.name(startTimeString)
        writer.value(event.startTime)
        writer.name(endTimeString)
        writer.value(event.endTime)
        writer.name(completedString)
        writer.value(event.completed)
        writer.name(labelString)
        writer.value(event.label.id)
        if (event is WorkoutEvent) {
            writer.name(workoutDistanceString)
            writer.value(event.workoutDistanceKm)
            writer.name(workoutTimeString)
            writer.value(event.workoutTimeMs)
            writer.name(totalTimeString)
            writer.value(event.totalTimeSec)
            writer.name(totalDistanceString)
            writer.value(event.totalDistanceKm)
            writer.name(totalCaloriesString)
            writer.value(event.totalCalories)
            writer.name(totalAverageSpeedString)
            writer.value(event.totalAverageSpeedKph)
            writer.name(totalPeakSpeedString)
            writer.value(event.totalPeakSpeedKph)
            writer.name(totalAverageHeartRateString)
            writer.value(event.totalAverageHR)
            writer.name(totalElevationString)
            writer.value(event.totalElevationMeters)
            writer.name(baselineString)
            writer.value(event.baselineKph)
            writer.name(goalTimeExistedString)
            writer.value(event.goalTimeExisted)
        }
        writer.endObject()
    }
}

private fun getUserInfoFile(userId: String, filesDir: File): File {
    val userDir = File(filesDir, "$PROGRAM_INFO_FOLDER/$userId/0") // keep 0 at end for compat with already ongoing programs
    if (!userDir.exists()) userDir.mkdirs()
    return File(userDir, USER_PROGRAM_FILE)
}