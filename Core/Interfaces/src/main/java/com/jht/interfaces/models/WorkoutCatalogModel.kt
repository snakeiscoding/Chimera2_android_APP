package com.jht.interfaces.models

import android.util.SparseArray
import androidx.compose.runtime.Composable
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.CustomWorkout
import com.jht.data.CustomWorkouts
import com.jht.data.LandmarkWorkoutItem
import com.jht.data.WorkoutInput
import com.jht.data.WorkoutInputLevelInfo
import com.jht.data.WorkoutItem
import com.jht.interfaces.BaseModel
import com.jht.interfaces.BoolValueListener
import com.jht.interfaces.DataVariableListener
import com.jht.data.sprint8.ISprint8WorkoutData
import com.jht.interfaces.EventListener

/**
 * Provides a model for getting the list of available workouts
 */
abstract class WorkoutCatalogModel : BaseModel {

    /** Indicates if the concierge setting is enabled */
    abstract val getWorkoutList: SparseArray<WorkoutItem>
    abstract val getVAWorkouts: List<WorkoutItem>
    abstract val getLandmarkWorkouts: List<LandmarkWorkoutItem>
    abstract val getFeaturedWorkouts: List<WorkoutItem>
    abstract val getFeatureWorkoutPlugins: List<BaseFeaturedWorkoutPlugin>
    abstract val getCarouselWorkouts: List<WorkoutItem>
    abstract val getTrainingCarouselWorkouts: List<WorkoutItem>
    abstract fun setFeaturedWorkouts()
    abstract fun getSprint8WorkoutData(callback : (List<ISprint8WorkoutData>) -> Unit)

    /**
     * The custom Sprint8 workout this user last started, empty when not available.
     * Values come back in the display units the UI handed over to [startCustomSprint8Workout],
     * the first entry is speed or resistance and the optional second entry is incline.
     */
    abstract fun loadSprint8CustomWorkoutData(): List<ISprint8WorkoutData>

    abstract suspend fun getWorkoutInputs(workoutId: Int, optionalId: String = ""): List<WorkoutInput>

    /** Indicates if workout lists are ready for use */
    abstract val workoutListReady: BoolValueListener // TODO - find better way

    /** Indicates if VA workouts has changed (ie workout setup is loaded before VA workout has been loaded) */
    abstract val onVAWorkoutsChange: EventListener

    abstract val stepsPerMeter: Float
    abstract val stepsPerFloor: Int

    /**
     * for the little black level box stats on the go screen.
     * separate from the method below, as the callback to grab the correct workoutmodel
     * needed to do these calculations felt too slow to do everytime the level was updated.
     * Now the workoutmodel is set when a user selects a Workout Item on the go screen, and
     * this method here is free to be called by the ui to update the level box frequently
     * without the cost performance of a suspend coroutine callback conversion, as that
     * workoutmodel is cached on the api interface.
     */
    abstract fun getWorkoutInputLevelInfo(level: Int): List<WorkoutInputLevelInfo>
    /**
     * for the little black level box stats on the go screen.
     * This MUST be called before getWorkoutInputLevelInfo(level) as this grabs the correct
     * workoutmodel from the backend to cache on the api from the currently loaded workout
     * so that the above method can be called by the ui to update the level box info
     */
    abstract suspend fun setWorkoutControllerModel()

    /** Provides a means to get and set the custom workouts for the current user */
    abstract val customWorkouts: DataVariableListener<CustomWorkouts>

    /** Starts a give custom workout item */
    abstract fun startCustomWorkout(customWorkout: CustomWorkout)
    abstract fun startCustomSprint8Workout(speedOrResistanceData: List<Float>, inclineData: List<Float>)
}
