package com.jht.customhiit.common

import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.BundleCompat
import com.jht.customhiit.common.models.HIITIntervalEnum
import com.jht.customhiit.common.models.HIITWorkoutMap
import com.jht.data.WorkoutInput
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.enums.UnitMeasurement

private const val bundleKey = "mapInputForHIIT"

/** Collection of misc HIIT functions from various places */
internal object HiitUtils {

    /** Inserts HIIT workout app setup data into generic bundle */
    fun insertBundle(bundle: Bundle = Bundle(), map: HIITWorkoutMap): Bundle {
        bundle.putParcelable(bundleKey, map)
        return bundle
    }

    /** Extracts HIIT workout app setup data from generic bundle */
    fun extractBundle(bundle: Bundle?): HIITWorkoutMap? {
        if (bundle == null) return null
        bundle.classLoader = HIITWorkoutMap::class.java.classLoader // DataMatrix IPC passing can strip class loaders
        return BundleCompat.getParcelable(bundle, bundleKey, HIITWorkoutMap::class.java)
    }

    /**
     * Default values for the HIIT workout. They are currently hardcoded
     */
    fun getHIITDefaults(isMetric: Boolean): HashMap<String, List<Float>> {
        val hiitMap = hashMapOf<String, List<Float>>()
        if (isMetric) {
            hiitMap.put(ConsoleWorkoutVariables.START_SPEED, listOf(5f, 6.5f, 8f))
        } else {
            hiitMap.put(ConsoleWorkoutVariables.START_SPEED, listOf(3f, 4f, 5f))
        }
        hiitMap.put(ConsoleWorkoutVariables.START_INCLINE, listOf(0f, 0f, 0f))
        hiitMap.put(ConsoleWorkoutVariables.START_RESISTANCE, listOf(1f, 5f, 10f))
        return hiitMap
    }

    fun getHIITWorkoutMapForPreview(): HIITWorkoutMap {
        val hiitInputMap = hashMapOf<HIITIntervalEnum, Map<String, MutableState<Float>>>().apply {
            HIITIntervalEnum.entries.forEach { interval ->
                if (interval == HIITIntervalEnum.ADDITIONAL_FIELD) {
                    put(interval, hashMapOf<String, MutableState<Float>>().apply {
                        put(ConsoleWorkoutVariables.WEIGHT, mutableStateOf(0.5f))
                    })
                } else {
                    put(interval, hashMapOf<String, MutableState<Float>>().apply {
                        put(ConsoleWorkoutVariables.SPEED_IN, mutableStateOf(0.5f))
                        put(ConsoleWorkoutVariables.INCLINE_IN, mutableStateOf(0.0f))
                    })
                }
            }
        }
        return HIITWorkoutMap(intervalMap = hiitInputMap)
    }

    fun getHIITWorkoutInputsForPreview(): List<WorkoutInput> {
        return listOf(
            WorkoutInput(
                min = 0.5f,
                max = 12f,
                defaultValue = 0.5f,
                stepSize = 0.5f,
                precision = 1,
                label = "Speed",
                unitString = "mph",
                identifier = ConsoleWorkoutVariables.SPEED_IN
            ),
            WorkoutInput(
                min = 0.0f,
                max = 15.0f,
                defaultValue = 0.0f,
                stepSize = 0.5f,
                precision = 1,
                label = "Incline",
                unitString = "%",
                identifier = ConsoleWorkoutVariables.INCLINE_IN
            ),
            WorkoutInput(
                min = 14f,
                max = 110f,
                defaultValue = 25f,
                stepSize = 1f,
                precision = 0,
                label = "Age",
                unitString = "Years",
                identifier = ConsoleWorkoutVariables.AGE
            ),
            WorkoutInput(
                min = 50f,
                max = 500f,
                defaultValue = 150f,
                stepSize = 1f,
                precision = 0,
                label = "Weight",
                unitString = "Kg",
                unitMeasurement = UnitMeasurement.Kilogram,
                identifier = ConsoleWorkoutVariables.WEIGHT
            ),
        )
    }
}
