package com.jht.interfaces.models

import com.jht.data.WorkoutInputUi
import com.jht.enums.WorkoutId
import com.jht.interfaces.BaseModel

/**
 * Model that facilitates launching existing and custom workouts
 */
abstract class WorkoutLauncherModel : BaseModel {
    /** Checks if workout PIN is required. Passes false to callback if action is aborted. If not required, performs action immediately. If required, user is prompted for PIN then action is performed on successful entry. */
    abstract fun checkLaunchWorkoutPin(nextAction: (Boolean)-> Unit) // Should probably circle back to review this mechanism at some point, maybe see if cleaner way to handle it

    /** Launches a customized workout */
    abstract fun launchCustomWorkout(
        workoutId: WorkoutId,
        appId: String? = null,
        workoutScript: String,
        initialConditions: Map<String, Float> = mapOf()
    ) : Boolean

    /** Launches a stock workout by [WorkoutId] and any initial conditions. Suspends
     * so we can get whether the workout actually starts */
    abstract suspend fun launchExistingWorkout(
        workoutId: WorkoutId,
        initialConditions: Map<String, Float> = mapOf(),
        options: WorkoutLaunchOptions? = null,
    ) : Boolean

    /** Launches a workout that has already been loaded by selecting that workout on the go screen,
     *  using the initial conditions set by the user on that screen. Returns true if it can launch workout (but workout might not have started) */
    abstract fun launchSelectedWorkout(initialConditions: Map<String, Float>) : Boolean

    /** Launches a workout that has already been loaded by selecting that workout on the go screen,
     *  it merges given inputs and initial conditions set by the user on that screen. Will check for safety PIN.
     */
    open fun launchSelectedWorkoutWithSafetyCheck(
        inputs: List<WorkoutInputUi>,
        initialConditions: MutableMap<String, Float> = mutableMapOf()
    ) {
        checkLaunchWorkoutPin { proceed ->
            if (!proceed) return@checkLaunchWorkoutPin
            for (workoutInput in inputs) {
                initialConditions[workoutInput.identifier] = workoutInput.convertToMetric()
            }
            launchSelectedWorkout(initialConditions)
        }
    }

    /** Launches a quick start workout. If an app is being launched with the workout, provide the UUID for the app. Returns if workout was able to start. */
    abstract fun launchQuickStartWorkout(appId: String? = null): Boolean

    /** Launches a featured workout app that assumes control of the workout (YMCA, iFit, etc.) */
    abstract fun launchFeaturedApp(uuid: String)
}


/** Generic workout media options */
enum class WorkoutMedia {
    None, Audio, Video
}

/** Optional extra options for launching workout */
class WorkoutLaunchOptions( // should we have some generic properties or do some kind of key/value pairs? can we just use initial conditions for passing these options?
    /** Workouts like Virtual Active and others have course IDs */
    val optionalId: String? = null,
    /** Workout media options, if applicable */
    val media: WorkoutMedia? = null,
    /** Workouts like Sprint8 need a number of sections */
    val sectionCount: Int? = null,
)
