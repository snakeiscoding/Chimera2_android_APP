package com.jht.landmarks

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.math.MathUtils
import com.jht.data.LandmarkWorkoutItem
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutCatalogModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.theme.ThemedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt

private const val TAG = "LandmarksViewModel"

@HiltViewModel
open class LandmarksViewModel @Inject constructor(
    private val workoutModel: WorkoutModel,
    private val workoutCatalogModel: WorkoutCatalogModel,
    private val uiServiceModel: UIServiceModel
) : ThemedViewModel() {

    private var floorsOffset = 0f
    val totalFloors by lazy { mutableIntStateOf(0) }
    val floorsCompleted by lazy { mutableIntStateOf(0) }
    val isFullScreen by lazy { mutableStateOf(false) }
    val progress by lazy { mutableFloatStateOf(0f) }
    val landmarksData by lazy { mutableStateOf<LandmarkWorkoutItem?>(null) }
    val currentResistance by lazy { mutableIntStateOf(1) }
    val maxResistance by lazy { mutableIntStateOf(20) }
    val isWorkoutPaused by lazy { mutableStateOf(false) }

    override fun start() {
        super.start()
        initializeListeners()
    }

    private fun initializeListeners() {
        uiServiceModel.stateFullScreen.listenAndTrigger { isFullScreen.value = it }

        workoutModel.apply {
            workoutCatalogModel.getLandmarkWorkouts.let { list ->
                //Get the workoutOptionId from WorkoutModel and retrieve the LandmarksData model
                runCatching {
                    landmarksData.value = list.find {it.optionalId == workoutOptionId.value}
                }.onFailure {
                    Log.e(TAG, "Failed to find workout option ${workoutOptionId.value}", it)
                    landmarksData.value = list.firstOrNull()
                }
            }

            //Calculation for the total floors based on the frame configuration and meters of the Landmark
            landmarksData.value?.let {
                totalFloors.intValue = if (workoutCatalogModel.stepsPerFloor == 0) 0 else
                    ((workoutCatalogModel.stepsPerMeter * it.meters) / workoutCatalogModel.stepsPerFloor).roundToInt()
            }

            goalRemaining.listenAndTrigger { goalRemaining ->
                if (goalType.value.isGoalFloor) {
                    val goalProgress = goalTotal.value - goalRemaining
                    floorsOffset = floors.value - goalProgress
                }
            }

            resistance.listenAndTrigger { value ->
                if (value != -1) currentResistance.intValue = value
            }

            this@LandmarksViewModel.maxResistance.intValue = maxResistance.toInt()

            isWorkoutPaused.listenAndTrigger { paused ->
                this@LandmarksViewModel.isWorkoutPaused.value = paused
            }

            floors.listenAndTrigger { floors ->
                val floorsInStage: Float = floors - floorsOffset
                val floorsClamped =
                    MathUtils.clamp(floorsInStage, 0f, totalFloors.intValue.toFloat()).toInt()

                if (floorsCompleted.intValue != floorsClamped) {
                    // Once the floor is changed will calculate the progress to update UI
                    floorsCompleted.intValue = floorsClamped
                    progress.floatValue = floorsCompleted.intValue / totalFloors.intValue.toFloat()
                }
            }

        }
    }

    fun increaseResistance() {
        val next = (currentResistance.intValue + 1).coerceAtMost(maxResistance.intValue)
        workoutModel.updateWorkoutResistance(next)
    }

    fun decreaseResistance() {
        val next = (currentResistance.intValue - 1).coerceAtLeast(1)
        workoutModel.updateWorkoutResistance(next)
    }

    fun setResistance(value: Int) {
        val clamped = value.coerceIn(1, maxResistance.intValue)
        workoutModel.updateWorkoutResistance(clamped)
    }

    fun togglePause() {
        if (isWorkoutPaused.value) {
            workoutModel.resumeWorkout()
        } else {
            workoutModel.pauseWorkout()
        }
    }

    fun onSizeClick() {
        uiServiceModel.toggleFullscreen()
    }

    private fun close() {
        workoutModel.close()
        workoutCatalogModel.close()
        uiServiceModel.close()
    }

    /**
     * Call this to register listeners when activity is paused
     */
    override fun stop() {
        close()
    }
}