package com.jht.enums

/**
 * List of parameters that can be queried for min/max info
 */
enum class MinMaxParam() {
    Age,
    Calories,
    ConstantWatts,
    MaxWattsPair,
    MinWattsPair,
    Distance,
    Floors,
    IntervalCount,
    IntervalTime,
    IntervalDistance,
    RestTime,
    FTP,
    Elevation,
    Height,
    Incline,
    Level,
    MaxIncline,
    MaxSpeed,
    MaxResistance,
    MaxResistancePair,
    MinResistancePair,
    MaxSpeedPair,
    MinSpeedPair,
    PercentMaxHeartRate,
    Resistance,
    Speed,
    TargetHeartRate,
    TargetSpeed,
    Weight,
    WorkoutTime,
    Gender
}

/**
 * Data for min/max params
 */
data class MinMaxInfo(
    var min: Float = 0f,
    var max: Float = 0f,
    var defaultValue: Float = 0f,
    var stepSize: Float = 0f,
    var precision: Int = 0,
    var captionText: String = "",
    var unitString: String = "",
    var measurement: UnitMeasurement? = null, // null indicates unit is a number, ratio, or has no associated measurement/conversions
)