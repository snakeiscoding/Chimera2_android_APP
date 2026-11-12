package com.jht.enums

/**
 * Defines the type of interval in a progression workout. This controls the starting speed of an
 * interval and the label that displays during the interval.
 */
enum class IntervalType(val id: Int) {
    WARMUP(0),
    COOLDOWN(1),
    ASSESSMENT(2),
    ZONE_1(3),
    ZONE_2(4),
    ZONE_3(5),
    ZONE_4(6),
    ZONE_5(7),
    RACE(8)
}

/**
 * Defines the goal type of an interval. The interval goal will be in the units of this goal type.
 */
enum class IntervalGoalType {
    /** interval goal will be in milliseconds **/
    TIME,

    /** interval goal will be in kilometers */
    DISTANCE
}

enum class ProgramType(val id: Int) {
    Program5k(0),
    Program10k(1)
}