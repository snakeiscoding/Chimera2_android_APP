package com.jht.customhiit.plugin

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import com.jht.customhiit.common.HiitUtils
import com.jht.customhiit.common.models.HIITIntervalEnum
import com.jht.customhiit.common.models.HIITWorkoutMap
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.enums.UnitMeasurement
import com.jht.interfaces.models.WorkoutLauncherModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.unit.mphUnit
import com.jht.widgets.helpers.getInjectionEntryPoint

/** Featured workout plugin for Custom HIIT workout app */
class CustomHIITFeaturePlugin(
    appContext: Context,
    workoutItem: WorkoutItem,
) : BaseFeaturedWorkoutPlugin(appContext, workoutItem) {
    private val mapInputForHIIT = mutableStateOf(HIITWorkoutMap(emptyMap()))
    private val workoutModel: WorkoutModel
    private val workoutLauncherModel: WorkoutLauncherModel

    init {
        val injection = getInjectionEntryPoint(appContext)
        workoutModel = injection.workout()
        workoutLauncherModel = injection.workoutLauncher()
    }

    @Composable
    override fun SetupScreen(
        workoutInputs: List<WorkoutInputUi>,
        onDismissRequest: () -> Unit,
    ) {
        // we could stub in start/stop as side effects? or make hosting view model call some lifecycle methods?
        // HIIT seems to use a 2nd background for this dialog
        val imageUri = workoutItem.background.getOrNull(1) ?: workoutItem.background.firstOrNull() ?: Uri.EMPTY
        CustomHIITSetupModal(
            imageUri = imageUri,
            workoutInputList = workoutInputs,
            inputModelForHIIT = mapInputForHIIT.value,
            startSelectedWorkout = { startWorkout(workoutInputs) },
        )
    }

    override fun startWorkout(workoutInputs: List<WorkoutInputUi>) {
        //If starting a custom HIIT workout, send the easy/medium/hard values to the UI
        val localMapCopy = mapInputForHIIT.value.deepCopy() // disconnect from UI before unit conversion
        for (interval in localMapCopy.intervalMap.values) {
            for (parameter in interval.entries) {
                //Conversion for imperial speeds to metric if necessary
                if (parameter.key == ConsoleWorkoutVariables.START_SPEED && workoutInputs.any { it.unitMeasurement == UnitMeasurement.MilePerHour }) {
                    parameter.value.value = parameter.value.value.mphUnit.toKph
                }
            }
        }
        HiitUtils.insertBundle(appSetupBundle, localMapCopy)
        // do we pass along the setup bundle or have the caller deal with it? or make setup bundle part of the workout launch itself?
        workoutModel.workoutAppSetupBundle.value = appSetupBundle
        // do we callback to system or just start workout through interfacing?
        workoutLauncherModel.launchSelectedWorkoutWithSafetyCheck(workoutInputs)
    }

    override fun onLoad(workoutInputs: List<WorkoutInputUi>) {
        //Loads extra values for the custom HIIT setup
        val hiitIntervalMap = hashMapOf<HIITIntervalEnum, Map<String, MutableState<Float>>>()
        val hiitDefaults = HiitUtils.getHIITDefaults(workoutModel.isMetric.value)
        val (inputParameters, weightInput) = workoutInputs.partition { it.identifier != ConsoleWorkoutVariables.WEIGHT }

        //Get HIIT values for easy, medium, and hard (first 3 indexes in the enum)
        HIITIntervalEnum.entries.subList(0, 3).forEachIndexed { index, enum ->
            val map = hashMapOf<String, MutableState<Float>>()
            for (workoutInput in inputParameters) {
                map.put(workoutInput.identifier, mutableFloatStateOf(hiitDefaults.get(workoutInput.identifier)
                    ?.get(index) ?: 0f)
                )
            }
            hiitIntervalMap.put(enum, map)
        }
        //Weight is the additional field
        weightInput.getOrNull(0)?.let {
            hiitIntervalMap.put(HIITIntervalEnum.ADDITIONAL_FIELD, mapOf(it.identifier to it.currentValue))
        }
        mapInputForHIIT.value = HIITWorkoutMap(intervalMap = hiitIntervalMap)
    }
    override fun onUnload() {
        workoutLauncherModel.close()
        workoutModel.close()
    }
}
