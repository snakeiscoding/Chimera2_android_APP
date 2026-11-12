package com.jht.enums

import org.jetbrains.annotations.NonNls

/**
 * copied from ConsoleWorkoutVariables and WorkoutScriptVariables
 */
object ConsoleWorkoutVariables {
    @NonNls const val INTERVAL_NUMBER = "INTERVAL_NUMBER"
    @NonNls const val ERG_MODE = "ERG_MODE"
    // I am going to depricate the next line, just need it to hang around for a little bit yet as we transition to CURRENT_AVG_TARGET_SPEED
    @NonNls const val PACE_WORKOUT_TARGET_SPEED = "PACE_WORKOUT_TARGET_SPEED" // Remove this line in future
    @NonNls const val CURRENT_AVG_TARGET_SPEED = "CURRENT_AVG_TARGET_SPEED"

    // Output variable that calculates the closest value of the resistance to the given watts and
    // attempts to set resistance to that level.
    @NonNls const val TARGET_WATTS_RESISTANCE = "TARGET_WATTS_RESISTANCE"

    // Whether sprint 8 is currently ramping for display on the UI
    @NonNls const val IS_RAMPING = "IS_RAMPING"

    // The time of the current interval. This should be the value that is counted down from on the sprint8 app
    @NonNls const val SPRINT8_INTERVAL_TIME = "TIME_SWITCH"
    @NonNls const val NEXT_SPEED = "NEXT_SPEED"
    @NonNls const val NEXT_INCLINE = "NEXT_INCLINE"
    @NonNls const val NEXT_RESISTANCE = "NEXT_RESISTANCE"
    @NonNls const val IS_SPRINTING = "IS_SPRINTING"

    // Fitness test keys
    @NonNls const val TEST_SCORE = "TEST_SCORE"
    @NonNls const val TEST_SCORE_MALE = "TEST_SCORE_MALE"
    @NonNls const val TEST_SCORE_FEMALE = "TEST_SCORE_FEMALE"

    @NonNls const val VO2 = "VO2"

    @NonNls const val COOPER_RPM_WARNING = "COOPER_RPM_WARNING" // Warn rpm is not in correct spot
    @NonNls const val HEART_RATE_WARNING = "HEART_RATE_WARNING" // Warn that heart rate is necessary for this workout

    @NonNls const val SUBMAX_OUTPUT = "SUBMAX_OUTPUT"

    /******* Variables that are inputs from the user  */

    // Inputs that are used for starting values for speed, resistance, or incline for workouts
    @NonNls const val START_SPEED = "START_SPEED"
    @NonNls const val START_INCLINE = "START_INCLINE"
    @NonNls const val START_RESISTANCE = "START_RESISTANCE"

    // The values that are sent to the script as the user selected starting goal
    @NonNls const val TIME_GOAL_START = "TIME_GOAL_START"
    @NonNls const val CALORIE_GOAL_START = "CALORIE_GOAL_START"
    @NonNls const val DISTANCE_GOAL_START = "DISTANCE_GOAL_START"
    @NonNls const val FLOORS_GOAL_START = "FLOORS_GOAL_START"
    @NonNls const val ELEVATION_GOAL_START = "ELEVATION_GOAL_START"

    // Per-interval goals for the spin bike interval workouts
    @NonNls const val NUM_INTERVALS = "NUM_INTERVALS"
    @NonNls const val INTERVAL_TIME_GOAL = "INTERVAL_TIME_GOAL"
    @NonNls const val INTERVAL_DISTANCE_GOAL = "INTERVAL_DISTANCE_GOAL"
    @NonNls const val REST_TIME_GOAL = "REST_TIME_GOAL"

    // Inputs that can be used to set the current goal type and goal for goal agnostic workout scripts
    @NonNls const val START_GOAL_TYPE = "START_GOAL_TYPE"
    @NonNls const val START_GOAL = "START_GOAL"

    // Used as a pair for the minimum and maximum values in a workout. Should adjust based on one another
    @NonNls const val MIN_RESISTANCE = "MIN_RESISTANCE"
    @NonNls const val MAX_RESISTANCE = "MAX_RESISTANCE"

    // Used as a pair for the minimum and maximum values in a workout. Should adjust based on one another
    @NonNls const val MIN_TARGET_WATTS_PAIR = "MIN_TARGET_WATTS"
    @NonNls const val MAX_TARGET_WATTS_PAIR = "MAX_TARGET_WATTS"

    // Used as a pair, for the minimum and maximum values in a workout. Should adjust based on one another
    @NonNls const val MIN_SPEED = "MIN_SPEED"
    @NonNls const val MAX_SPEED = "MAX_SPEED"

    @NonNls const val TARGET_AVG_SPEED = "TARGET_AVG_SPEED"

    @NonNls const val WEIGHT = "WEIGHT"
    @NonNls const val AGE = "AGE"
    @NonNls const val LEVEL = "LEVEL"

    @NonNls const val MAXIMUM_INCLINE = "MAXIMUM_INCLINE"
    @NonNls const val MAXIMUM_SPEED = "MAXIMUM_SPEED"
    @NonNls const val MAXIMUM_RESISTANCE = "MAXIMUM_RESISTANCE"
    @NonNls const val PERCENT_MAX_HEART_RATE = "PERCENT_MAX_HEART_RATE"
    @NonNls const val GENDER = "GENDER" // Used for several fitness tests
    @NonNls const val HEIGHT = "HEIGHT" // Used for WFI fitness test

    /******* Inputs that are passed in by the system. These are either calculated, settings, or workout data.  */ // The input values for speed/incline/resistance in workout scripts. These values are initialized
    // before the workout by the system to initial values and are then updated by the script and user
    // interaction with the console
    @NonNls const val SPEED_IN = "SPEED_IN"
    @NonNls const val INCLINE_IN = "INCLINE_IN"
    @NonNls const val RESISTANCE_IN = "RESISTANCE_IN"

    // Workout values that can be retrieved by the system. These can only be inputs and cannot be directly controlled
    @NonNls const val HEART_RATE = "HEART_RATE"
    @NonNls const val PEAK_HEART_RATE = "PEAK_HEART_RATE"
    @NonNls const val CALORIES_PER_HOUR = "CALORIES_PER_HOUR"
    @NonNls const val WATTS = "WATTS"
    @NonNls const val METS = "METS"
    @NonNls const val PACE = "PACE"
    @NonNls const val AVERAGE_PACE = "AVERAGE_PACE"
    @NonNls const val RPM = "RPM"
    @NonNls const val SPM = "SPM"
    @NonNls const val AVERAGE_SPM = "AVERAGE_SPM"

    // Values for default time of cooldown and warmup
    @NonNls const val COOLDOWN_TIME_START = "COOLDOWN_TIME_START"
    @NonNls const val WARMUP_TIME_START = "WARMUP_TIME_START"

    // Can be updated during the running of the script
    @NonNls const val TARGET_WATTS_START = "TARGET_WATTS_START"
    @NonNls const val TARGET_WATTS = "TARGET_WATTS"
    @NonNls const val TARGET_HEART_RATE = "TARGET_HEART_RATE"

    // Whether the workout is currently shown in metric. This will change based on what distance parameter is selected
    @NonNls const val IS_METRIC = "IS_METRIC"

    // SPRINT8 VARIABLES
    // Ramp rates for determining how long the frame will take to adjust the speed/incline/resistance
    @NonNls const val RAMP_RATE_SPEED = "RAMP_RATE_SPEED"
    @NonNls const val RAMP_RATE_INCLINE = "RAMP_RATE_INCLINE"
    @NonNls const val RAMP_RATE_RESISTANCE = "RAMP_RATE_RESISTANCE"

    // Constant values for elevation levels for navy test
    @NonNls const val FRAME_ELEVATION = "FRAME_ELEVATION"
    @NonNls const val MIN_HEART_RATE = "MIN_HEART_RATE"
    @NonNls const val MAX_HEART_RATE = "MAX_HEART_RATE"

    // Some workouts started from MatrixComm may wish to disable auto pause as they have portions done off the machine (RETAIL ONLY)
    @NonNls const val ALLOW_AUTO_PAUSE = "ALLOW_AUTO_PAUSE"

    /* ***** CONSTANT VALUES *******/
    const val COOPER_WARNING_NONE = 0
    const val COOPER_WARNING_TOO_SLOW = 1
    const val COOPER_WARNING_TOO_FAST = 2
    const val SUBMAX_OUTPUT_TEST_COMPLETE = 0
    const val SUBMAX_OUTPUT_HR_DID_NOT_INCREASE = 1
    const val SUBMAX_OUTPUT_HR_TOO_LOW = 2
    const val WFI_TEST_SUCCESSFUL = 0
    const val WFI_TEST_FAILED_HR_TOO_LOW = 1
    const val WFI_TEST_NO_HR_INCREASE = 2
    const val WFI_TEST_STOPPED = 3
    const val WFI_NO_HR_FOUND = 4
}


object WorkoutScriptVariables {
    const val WORKOUT_TIME_MS = "WORKOUT_TIME_MS"
    const val STAGE_TIME_MS = "STAGE_TIME_MS"
    const val INTERVAL_TIME_MS = "INTERVAL_TIME_MS"
    const val WORKOUT_DISTANCE = "WORKOUT_DISTANCE"
    const val STAGE_DISTANCE = "STAGE_DISTANCE"
    const val INTERVAL_DISTANCE = "INTERVAL_DISTANCE"
    const val WORKOUT_FLOORS = "WORKOUT_FLOORS"
    const val STAGE_FLOORS = "STAGE_FLOORS"
    const val INTERVAL_FLOORS = "INTERVAL_FLOORS"
    const val WORKOUT_CALORIES = "WORKOUT_CALORIES"
    const val STAGE_CALORIES = "STAGE_CALORIES"
    const val INTERVAL_CALORIES = "INTERVAL_CALORIES"
    const val WORKOUT_ELEVATION = "WORKOUT_ELEVATION"
    const val STAGE_ELEVATION = "STAGE_ELEVATION"
    const val INTERVAL_ELEVATION = "INTERVAL_ELEVATION"
    const val WORKOUT_STEPS = "WORKOUT_STEPS"
    const val STAGE_STEPS = "STAGE_STEPS"
    const val INTERVAL_STEPS = "INTERVAL_STEPS"
    const val GOAL = "GOAL"
    const val INTERVAL_GOAL = "INTERVAL_GOAL"
    const val INTERVAL_GOAL_TYPE = "INTERVAL_GOAL_TYPE"
    const val GOAL_TYPE = "GOAL_TYPE"
    const val SPEED = "SPEED"
    const val INCLINE = "INCLINE"
    const val RESISTANCE = "RESISTANCE"
    const val PREVENT_USER_GOAL_CHANGES = "PREVENT_USER_GOAL_CHANGES"
    const val PREVENT_USER_CHANGES = "PREVENT_USER_CHANGES"
    const val ALLOW_PAUSE = "ALLOW_PAUSE"
    const val PREVENT_INCLINE_CHANGES = "PREVENT_INCLINE_CHANGES"
    const val CALORIES = "CALORIES"
    const val DISTANCE = "DISTANCE"
    const val STEPS = "STEPS"
    const val FLOORS = "FLOORS"
    const val ELEVATION = "ELEVATION"
    const val GOAL_TYPE_TIME_CONSTANT = "GOAL_TYPE_TIME_CONSTANT"
    const val GOAL_TYPE_DISTANCE_CONSTANT = "GOAL_TYPE_DISTANCE_CONSTANT"
    const val GOAL_TYPE_CALORIES_CONSTANT = "GOAL_TYPE_CALORIES_CONSTANT"
    const val GOAL_TYPE_FLOORS_CONSTANT = "GOAL_TYPE_FLOORS_CONSTANT"
    const val GOAL_TYPE_ELEVATION_CONSTANT = "GOAL_TYPE_ELEVATION_CONSTANT"
    const val GOAL_TYPE_STEPS_CONSTANT = "GOAL_TYPE_STEPS_CONSTANT"
    const val GOAL_TYPE_NONE_CONSTANT = "GOAL_TYPE_NONE_CONSTANT"
    const val GOAL_TYPE_TIME_STAGE_CONSTANT = "GOAL_TYPE_TIME_STAGE_CONSTANT"
    const val GOAL_TYPE_DISTANCE_STAGE_CONSTANT = "GOAL_TYPE_DISTANCE_STAGE_CONSTANT"
    const val GOAL_TYPE_CALORIES_STAGE_CONSTANT = "GOAL_TYPE_CALORIES_STAGE_CONSTANT"
    const val GOAL_TYPE_FLOORS_STAGE_CONSTANT = "GOAL_TYPE_FLOORS_STAGE_CONSTANT"
    const val GOAL_TYPE_ELEVATION_STAGE_CONSTANT = "GOAL_TYPE_ELEVATION_STAGE_CONSTANT"
    const val GOAL_TYPE_STEPS_STAGE_CONSTANT = "GOAL_TYPE_STEPS_STAGE_CONSTANT"
    const val GENDER_MALE = "GENDER_MALE"
    const val GENDER_FEMALE = "GENDER_FEMALE"
    const val GENDER_OTHER = "GENDER_OTHER"
    const val FRAME_ELEVATION_LOW = "FRAME_ELEVATION_LOW"
    const val FRAME_ELEVATION_HIGH = "FRAME_ELEVATION_HIGH"
    const val GOAL_TYPE_NONE = 0
    const val GOAL_TYPE_TIME = 1
    const val GOAL_TYPE_DISTANCE = 2
    const val GOAL_TYPE_CALORIES = 3
    const val GOAL_TYPE_FLOORS = 4
    const val GOAL_TYPE_ELEVATION = 5
    const val GOAL_TYPE_STEPS = 6
    const val GOAL_TYPE_TIME_STAGE = 7
    const val GOAL_TYPE_DISTANCE_STAGE = 8
    const val GOAL_TYPE_CALORIES_STAGE = 9
    const val GOAL_TYPE_FLOORS_STAGE = 10
    const val GOAL_TYPE_ELEVATION_STAGE = 11
    const val GOAL_TYPE_STEPS_STAGE = 12
    const val GENDER_MALE_CONSTANT = 0
    const val GENDER_FEMALE_CONSTANT = 1
    const val GENDER_OTHER_CONSTANT = 2
    const val FRAME_ELEVATION_VAL_LOW = 0
    const val FRAME_ELEVATION_VAL_HIGH = 1
    const val WORKOUT_STATE_STOPPED = 0
    const val WORKOUT_STATE_RUNNING = 1
    const val WORKOUT_STATE_PAUSED = 2
    const val WORKOUT_STAGE_WARMUP = 0
    const val WORKOUT_STAGE_WORKOUT = 1
    const val WORKOUT_STAGE_COOLDOWN = 2
}
