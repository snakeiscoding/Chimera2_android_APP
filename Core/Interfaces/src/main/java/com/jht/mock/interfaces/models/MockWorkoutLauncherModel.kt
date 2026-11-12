package com.jht.mock.interfaces.models

import com.jht.enums.WorkoutId
import com.jht.interfaces.models.WorkoutLaunchOptions
import com.jht.interfaces.models.WorkoutLauncherModel

class MockWorkoutLauncherModel() : WorkoutLauncherModel() {
    override fun checkLaunchWorkoutPin(nextAction: (Boolean) -> Unit) {
        nextAction(true)
    }

    override fun launchCustomWorkout(
        workoutId: WorkoutId,
        appId: String?,
        workoutScript: String,
        initialConditions: Map<String, Float>
    ) : Boolean {
        // do nothing
        return true
    }

    override suspend fun launchExistingWorkout(
        workoutId: WorkoutId,
        initialConditions: Map<String, Float>,
        options: WorkoutLaunchOptions?,
    ) : Boolean {
        // do nothing
        return true
    }

    override fun launchSelectedWorkout(initialConditions: Map<String, Float>) : Boolean {
        // do nothing
        return true
    }

    override fun close() {
        // do nothing
    }

    override fun launchQuickStartWorkout(appId: String?): Boolean {
        // do nothing
        return true
    }

    override fun launchFeaturedApp(uuid: String) {
        // do nothing
    }
}