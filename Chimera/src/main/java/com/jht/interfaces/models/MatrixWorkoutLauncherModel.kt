package com.jht.interfaces.models

import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.Sprint8Keys
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables
import com.jht.chimera.lib.workout.WorkoutInput
import com.jht.chimera.lib.workout.WorkoutManager
import com.jht.chimera.lib.workout.workoutlist.workoutmap.WorkoutMap
import com.jht.chimera.service.ui.panels.DigitalSafetyPinDialog
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.WorkoutId
import com.jht.interfaces.CallbackManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class MatrixWorkoutLauncherModel(
) : WorkoutLauncherModel() {

    private val manager: CallbackManager = CallbackManager()
    private var safetyPrompt: DigitalSafetyPinDialog? = null

    override fun checkLaunchWorkoutPin(nextAction: (Boolean) -> Unit) {
        // safety PIN feature implementation could use clean up in general
        // Should we just integrate this into the launch methods?
        // If so, keep dialog system side as part of launch methods here, or UI side by just providing a PIN if there needs to be checked by UI?
        // the internal workout launch will still block and show system's dialog but user will have re-start if we do not pre-check
        if (DigitalSafetyPinDialog.needsToShow()) {
            if (safetyPrompt == null) safetyPrompt = DigitalSafetyPinDialog(ContextUtil.getApplicationContext())
            safetyPrompt?.checkIfNeeded({ nextAction(true) }, { nextAction(false) })
        } else {
            nextAction(true)
        }
    }

    override fun launchCustomWorkout(
        workoutId: WorkoutId,
        appId: String?,
        workoutScript: String,
        initialConditions: Map<String, Float>
    ) : Boolean {
        val workoutManager = createWorkoutManager()
        workoutManager.loadCustomWorkout(workoutId.id, appId, workoutScript, null)

        // We don't need to check the callback since we don't care about the requested inputs
        val workoutInputs = ArrayList<WorkoutInput>()
        for (input in initialConditions) {
            val workoutInput = WorkoutInput(input.key, input.value)
            workoutInputs.add(workoutInput)
        }

        return workoutManager.startLoadedWorkout(workoutInputs, false)
    }

    override suspend fun launchExistingWorkout(
        workoutId: WorkoutId,
        initialConditions: Map<String, Float>,
        options: WorkoutLaunchOptions?,
    ) : Boolean = suspendCoroutine { cont ->
        // some workouts need special setup (do we need to create dedicated launch methods for these or just use this method with options? or make options into initial conditions?)
        when(workoutId) {
            WorkoutId.VIRTUAL_ACTIVE -> {
                options?.optionalId?.let {id ->
                    DataMatrix.putString(VaKeys.DATA_SELECTED_WORKOUT_UNIQUEID_STRING, id)
                }
            }
            WorkoutId.SPRINT8 -> {
                options?.let {
                    it.sectionCount?.let {sprints -> DataMatrix.putInt(Sprint8Keys.DATA_SPRINT8_NUM_SPRINTS_INT, sprints)}
                }
            }
            else -> {}
        }
        // load and start workout
        val workoutManager = createWorkoutManager()
        workoutManager.loadWorkoutWithId(workoutId.id) { workoutInputs ->
            for (input in workoutInputs) {
                val default = initialConditions[input.identifier()]
                if (default != null) {
                    input.value(default.toDouble())
                }
            }

            val started = workoutManager.startLoadedWorkout(workoutInputs, false)
            cont.resume(started)
        }
    }

    override fun launchSelectedWorkout(
        initialConditions: Map<String, Float>
    ) : Boolean {
        val workoutManager = createWorkoutManager()
        val oldWorkoutInputList = arrayListOf<WorkoutInput>() // old WorkoutInput from back end, NOT WorkoutInput on interface (yet)

        for (item in initialConditions) {
            oldWorkoutInputList.add(WorkoutInput(item.key, item.value))
        }

        return workoutManager.startLoadedWorkout(oldWorkoutInputList, false)
    }

    private fun createWorkoutManager(): WorkoutManager {
        val workoutMap = WorkoutMap.createWorkoutMap()
        return WorkoutManager(workoutMap)
    }

    override fun launchQuickStartWorkout(appId: String?): Boolean {
        // Both commercial and retail use the same code for quick start. Retail just adds option for pre-launching an app with the workout.
        val initialConditions = listOf(
            WorkoutInput(ConsoleWorkoutVariables.TIME_GOAL_START, DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT).toFloat()),
            WorkoutInput(ConsoleWorkoutVariables.START_SPEED, DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT)),
            WorkoutInput(ConsoleWorkoutVariables.START_INCLINE,FrameConfigManager.getInstance().frameConfig.incline.min),
            WorkoutInput(ConsoleWorkoutVariables.START_RESISTANCE, DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT).toFloat()),
        )
        val workoutManager = WorkoutManager(WorkoutMap.createWorkoutMap())
        if (appId == null) workoutManager.loadQuickStartWorkout(null) else workoutManager.loadQuickStartWorkout(appId, null)
        return workoutManager.startLoadedWorkout(initialConditions, false)
    }

    override fun launchFeaturedApp(uuid: String) {
        LaunchAppUtil.initialLaunchApp(ContextUtil.getApplicationContext(), uuid, null)
    }


    override fun close() {
        manager.cancelAllListeners()
        safetyPrompt?.dismiss()
    }
}