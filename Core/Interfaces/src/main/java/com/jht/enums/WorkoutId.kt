package com.jht.enums

enum class WorkoutId(val id: Int) {
    MANUAL(0),
    ROLLING_HILLS(9),
    DISTANCE_GOAL(14),
    CALORIES_GOAL(15),
    FLOORS_GOAL(16),
    SPRINT8(18),
    CONSTANT_WATTS(20),
    GERKIN_FITNESS_TEST(30),
    ARMY_FITNESS_TEST(33),
    NAVY_FITNESS_TEST(34),
    AIR_FORCE_FITNESS_TEST(35),
    MARINES_FITNESS_TEST(36),
    PEB_FITNESS_TEST(37),
    COOPER_FITNESS_TEST(38),
    INTERVALS(40),
    THR_SPEED(44),
    THR_INCLINE(45),
    THR_RESISTANCE(46),
    VIRTUAL_ACTIVE(47),
    FAT_BURN(49),
    GLUTES(50),
    CPAT_FITNESS_TEST(51),
    WFI_FITNESS_TEST(52),
    SUBMAX_FITNESS_TEST(53),
    LANDMARKS(55),
    STEPPER_FITNESS_TEST(56),
    INTERVALS_SPEED(57),
    MIGROS_POTENTIAL(58),
    MIGROS_EXHAUSTION(59),
    MIGROS_FATIGUE(60),
    FEATURED(71),
    FTP_TEST_8(72),
    PACE_TIME_GOAL(73),
    PACE_DISTANCE_GOAL(74),
    FTP_TEST_20(75),
    MAX_HR_TEST(76),
    MARATHON(77),
    HIIT(78),
    EGYM(79),
    TARGET_TRAINING(89),
    CUSTOM_WORKOUT(90),
    TIME_GOAL(93),
    TIME_GOAL_MANUAL_COOLDOWN(94),
    IFIT(95),
    YMCA_360(96),
    MATRIX_TEST_APP(97),
    CARDIO_SCAN(98),
    PROGRESSION_WORKOUT(99),
    SECA(117),
    HYBRID_TD_SWITCH_STRIDE(67),
    SPIN_BIKE_INTERVALS_DISTANCE(88),
    SPIN_BIKE_INTERVALS_TIME(118);

    companion object {
        private val map = WorkoutId.values().associateBy(WorkoutId::id)
        // TODO we need to eventually refactor code to use same enum type
        // if the enum is not found, it will return MANUAL
        fun fromId(id: Int) = map[id] ?: MANUAL
    }

    val isTargetHeartRate: Boolean get() = this == THR_SPEED || this == THR_INCLINE || this == THR_RESISTANCE
    val isMigros: Boolean get() = this == MIGROS_POTENTIAL || this == MIGROS_EXHAUSTION || this == MIGROS_FATIGUE
    val isFitnessTest: Boolean get() = fitnessTestWorkoutIds.contains(this)
}

// Temporary lists to help sort the scrolling view on the new (but still same old design) go screen
val trainingWorkoutIds = listOf(
    WorkoutId.MANUAL,
    WorkoutId.CUSTOM_WORKOUT,
    WorkoutId.TARGET_TRAINING,
    WorkoutId.ROLLING_HILLS,
    WorkoutId.FAT_BURN,
    WorkoutId.INTERVALS,
    WorkoutId.INTERVALS_SPEED,
    WorkoutId.PACE_TIME_GOAL,
    WorkoutId.PACE_DISTANCE_GOAL,
    WorkoutId.CONSTANT_WATTS,
    WorkoutId.GLUTES,
    WorkoutId.SPIN_BIKE_INTERVALS_DISTANCE,
    WorkoutId.SPIN_BIKE_INTERVALS_TIME
)

val goalWorkoutIds = listOf(
    WorkoutId.TIME_GOAL,
    WorkoutId.DISTANCE_GOAL,
    WorkoutId.FLOORS_GOAL,
    WorkoutId.CALORIES_GOAL
)

val marathonWorkoutIds = listOf(WorkoutId.MARATHON)

val targetHeartRateWorkoutIds =
    listOf(WorkoutId.THR_INCLINE, WorkoutId.THR_SPEED, WorkoutId.THR_RESISTANCE)

val fitnessTestWorkoutIds = listOf(
    WorkoutId.ARMY_FITNESS_TEST,
    WorkoutId.MARINES_FITNESS_TEST,
    WorkoutId.AIR_FORCE_FITNESS_TEST,
    WorkoutId.NAVY_FITNESS_TEST,
    WorkoutId.PEB_FITNESS_TEST,
    WorkoutId.GERKIN_FITNESS_TEST,
    WorkoutId.CPAT_FITNESS_TEST,
    WorkoutId.SUBMAX_FITNESS_TEST,
    WorkoutId.WFI_FITNESS_TEST,
    WorkoutId.STEPPER_FITNESS_TEST,
    WorkoutId.FTP_TEST_20,
    WorkoutId.FTP_TEST_8,
    WorkoutId.COOPER_FITNESS_TEST,
    WorkoutId.MAX_HR_TEST
)