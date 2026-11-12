package com.jht.chimera.lib.workout

import android.util.Log
import com.jht.chimera.lib.workout.workoutparameters.MinMaxDictionary
import com.jht.chimera.lib.workout.workoutparameters.WorkoutInputManager
import com.jht.chimera.service.dapi.UserProfileManager
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG = "CustomSprint8File"
private const val FILENAME = "custom_sprint8.json"


/**
 * A user's saved custom Sprint8 workout. Currently used primarily for retail consoles that track user profiles locally.
 * Typically stored as JSON in the local user profile folders.
 *
 * Every property has a default and unknown keys are ignored, so a profile written by an older or
 * newer build still loads with whatever it does have.
 */
@Serializable
data class CustomSprint8File(
    val sprint8ParameterList: List<SavedSprint8WorkoutData> = emptyList(),
    /**
     * False for profiles written by the legacy Sprint8 setup, which stored whatever units the
     * console was displaying at the time. Those are handed back as they were saved, the same as the
     * legacy setup did with them - only profiles saved since carry normalized metric values.
     */
    val savedInMetric: Boolean = false,
) {

    @Serializable
    data class SavedSprint8WorkoutData(
        val identifier: String = "",
        val intervalData: List<List<Float>> = emptyList(),
    )

    /**
     * The saved workout as the rest of the system handles Sprint8 data.
     *
     * Empty when any parameter cannot be resolved: the entries are positional (speed or resistance
     * first, incline second), so a partly readable profile would be worse than none at all.
     */
    fun toSprint8WorkoutData(): List<Sprint8WorkoutData> {
        val workoutData = mutableListOf<Sprint8WorkoutData>()
        for (saved in sprint8ParameterList) {
            val parameter = workoutParameterFor(saved.identifier) ?: return emptyList()
            workoutData.add(Sprint8WorkoutData(parameter, saved.intervalData))
        }
        return workoutData
    }

    companion object {

        /** Captures a workout for saving. Convert the values before calling, this stores them as given. */
        @JvmStatic
        fun from(sprint8WorkoutDataList: List<Sprint8WorkoutData>, savedInMetric: Boolean) = CustomSprint8File(
            sprint8ParameterList = sprint8WorkoutDataList.map { data ->
                SavedSprint8WorkoutData(
                    identifier = data.workoutParameter?.identifier().orEmpty(),
                    intervalData = data.intervalData.orEmpty(),
                )
            },
            savedInMetric = savedInMetric,
        )

        private fun workoutParameterFor(identifier: String): WorkoutParameter? {
            val unit = WorkoutInputManager.getUnitType(identifier) ?: return null
            val minMax = MinMaxDictionary.getMinMaxDictionary(unit) ?: return null
            return WorkoutParameter(
                minMax.min,
                minMax.max,
                minMax.defaultValue,
                minMax.stepSize,
                minMax.precision,
                minMax.captionText,
                minMax.unitString,
                minMax.measurement,
                identifier,
            )
        }

        private val json = Json {
            ignoreUnknownKeys = true // a profile from another build may carry keys this one does not know
            encodeDefaults = true    // savedInMetric has to be written even when it matches the default
        }

        private val file: File
            get() = File(UserProfileManager.getInstance().userFolder, FILENAME)

        /** Load the current user profile custom sprint8 file or return null */
        fun load(): CustomSprint8File? {
            val file = file
            if (!file.exists()) return null
            return try {
                json.decodeFromString(serializer(), file.readText())
            } catch (e: Exception) {
                Log.w(TAG, "Could not read $FILENAME", e)
                null
            }
        }

        /** Save the current user profile custom sprint8 file */
        fun save(customSprint8File: CustomSprint8File) {
            try {
                file.writeText(json.encodeToString(serializer(), customSprint8File))
            } catch (e: Exception) {
                Log.w(TAG, "Could not write $FILENAME", e)
            }
        }
    }
}
