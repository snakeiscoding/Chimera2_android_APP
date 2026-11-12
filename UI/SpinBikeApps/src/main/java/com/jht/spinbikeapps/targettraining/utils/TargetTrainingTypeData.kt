package com.jht.spinbikeapps.targettraining.utils

enum class WorkoutMode {
    Rpm,
    Watts,
    HeartRate,
    Calories,
    Distance,
    None;

    companion object {
        fun fromTTType(type: Int): WorkoutMode? = when (type) {
            0 -> Watts
            1 -> Rpm
            2 -> HeartRate
            3 -> Distance
            4 -> Calories
            else -> null
        }
        internal fun WorkoutMode.toTTType(): Int = when (this) {
            Watts -> 0
            Rpm -> 1
            HeartRate -> 2
            Distance -> 3
            Calories -> 4
            None -> 0 // default value from DataMatrix value
        }
    }
}

internal data class WattsTarget(
    val targetZone: String,
    val watts: Float
)

internal data class HeartRateTarget(
    val targetZone: String,
    val maxHr: Float
)

internal object TargetTrainingTypeData {
    var selectedMode: WorkoutMode = WorkoutMode.None
    var rpmTarget: Float = 0f
    var caloriesTarget: Float? = null
    var heartRateTarget: HeartRateTarget? = null
    var wattsTarget: WattsTarget? = null
    var distanceTarget: Float = 0f
    var timeTarget: Int = 0
}