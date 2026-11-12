package com.jht.chimera.lib.workout;

import com.jht.workoutcontrollerlib.workoutcardio.WorkoutScriptVariables;

import org.jetbrains.annotations.NonNls;

/**
 * Defines system variables that can be used in workout scripts. This is a single possible
 * implementation.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConsoleWorkoutVariables extends WorkoutScriptVariables {

    @NonNls public static final String INTERVAL_NUMBER = "INTERVAL_NUMBER";
    @NonNls public static final String ERG_MODE = "ERG_MODE";
    // I am going to depricate the next line, just need it to hang around for a little bit yet as we transition to CURRENT_AVG_TARGET_SPEED
    @NonNls public static final String PACE_WORKOUT_TARGET_SPEED = "PACE_WORKOUT_TARGET_SPEED";  // Remove this line in future
    @NonNls public static final String CURRENT_AVG_TARGET_SPEED = "CURRENT_AVG_TARGET_SPEED";

    // Output variable that calculates the closest value of the resistance to the given watts and
    // attempts to set resistance to that level.
    @NonNls public static final String TARGET_WATTS_RESISTANCE = "TARGET_WATTS_RESISTANCE";

    // Whether sprint 8 is currently ramping for display on the UI
    @NonNls public static final String IS_RAMPING = "IS_RAMPING";
    // The time of the current interval. This should be the value that is counted down from on the sprint8 app
    @NonNls public static final String SPRINT8_INTERVAL_TIME = "TIME_SWITCH";
    @NonNls public static final String NEXT_SPEED = "NEXT_SPEED";
    @NonNls public static final String NEXT_INCLINE = "NEXT_INCLINE";
    @NonNls public static final String NEXT_RESISTANCE = "NEXT_RESISTANCE";
    @NonNls public static final String IS_SPRINTING = "IS_SPRINTING";

    // Fitness test keys
    @NonNls public static final String TEST_SCORE = "TEST_SCORE";
    @NonNls public static final String TEST_SCORE_MALE = "TEST_SCORE_MALE";
    @NonNls public static final String TEST_SCORE_FEMALE = "TEST_SCORE_FEMALE";

    @NonNls public static final String VO2 = "VO2";
    @NonNls public static final String COOPER_RPM_WARNING = "COOPER_RPM_WARNING"; // Warn rpm is not in correct spot
    @NonNls public static final String HEART_RATE_WARNING = "HEART_RATE_WARNING"; // Warn that heart rate is necessary for this workout
    @NonNls public static final String SUBMAX_OUTPUT = "SUBMAX_OUTPUT";


    /******* Variables that are inputs from the user *******/

    // Inputs that are used for starting values for speed, resistance, or incline for workouts
    @NonNls public static final String START_SPEED = "START_SPEED";
    @NonNls public static final String START_INCLINE = "START_INCLINE";
    @NonNls public static final String START_RESISTANCE = "START_RESISTANCE";

    // The values that are sent to the script as the user selected starting goal
    @NonNls public static final String TIME_GOAL_START = "TIME_GOAL_START";
    @NonNls public static final String CALORIE_GOAL_START = "CALORIE_GOAL_START";
    @NonNls public static final String DISTANCE_GOAL_START = "DISTANCE_GOAL_START";
    @NonNls public static final String FLOORS_GOAL_START = "FLOORS_GOAL_START";
    @NonNls public static final String ELEVATION_GOAL_START = "ELEVATION_GOAL_START";

    // Per-interval goals for the spin bike interval workouts
    @NonNls public static final String NUM_INTERVALS = "NUM_INTERVALS";
    @NonNls public static final String INTERVAL_TIME_GOAL = "INTERVAL_TIME_GOAL";
    @NonNls public static final String INTERVAL_DISTANCE_GOAL = "INTERVAL_DISTANCE_GOAL";
    @NonNls public static final String REST_TIME_GOAL = "REST_TIME_GOAL";

    // Inputs that can be used to set the current goal type and goal for goal agnostic workout scripts
    @NonNls public static final String START_GOAL_TYPE = "START_GOAL_TYPE";
    @NonNls public static final String START_GOAL = "START_GOAL";

    // Used as a pair for the minimum and maximum values in a workout. Should adjust based on one another
    @NonNls public static final String MIN_RESISTANCE = "MIN_RESISTANCE";
    @NonNls public static final String MAX_RESISTANCE = "MAX_RESISTANCE";

    // Used as a pair for the minimum and maximum values in a workout. Should adjust based on one another
    @NonNls public static final String MIN_TARGET_WATTS_PAIR = "MIN_TARGET_WATTS";
    @NonNls public static final String MAX_TARGET_WATTS_PAIR = "MAX_TARGET_WATTS";

    // Used as a pair, for the minimum and maximum values in a workout. Should adjust based on one another
    @NonNls public static final String MIN_SPEED = "MIN_SPEED";
    @NonNls public static final String MAX_SPEED = "MAX_SPEED";

    @NonNls public static final String TARGET_AVG_SPEED = "TARGET_AVG_SPEED";

    @NonNls public static final String WEIGHT = "WEIGHT";
    @NonNls public static final String AGE = "AGE";
    @NonNls public static final String LEVEL = "LEVEL";
    @NonNls public static final String MAXIMUM_INCLINE = "MAXIMUM_INCLINE";
    @NonNls public static final String MAXIMUM_SPEED = "MAXIMUM_SPEED";
    @NonNls public static final String MAXIMUM_RESISTANCE = "MAXIMUM_RESISTANCE";
    @NonNls public static final String PERCENT_MAX_HEART_RATE = "PERCENT_MAX_HEART_RATE";
    @NonNls public static final String GENDER = "GENDER"; // Used for several fitness tests
    @NonNls public static final String HEIGHT = "HEIGHT"; // Used for WFI fitness test


    /******* Inputs that are passed in by the system. These are either calculated, settings, or workout data. *******/

    // The input values for speed/incline/resistance in workout scripts. These values are initialized
    // before the workout by the system to initial values and are then updated by the script and user
    // interaction with the console
    @NonNls public static final String SPEED_IN = "SPEED_IN";
    @NonNls public static final String INCLINE_IN = "INCLINE_IN";
    @NonNls public static final String RESISTANCE_IN = "RESISTANCE_IN";

    // Workout values that can be retrieved by the system. These can only be inputs and cannot be directly controlled
    @NonNls public static final String HEART_RATE = "HEART_RATE";
    @NonNls public static final String PEAK_HEART_RATE = "PEAK_HEART_RATE";
    @NonNls public static final String CALORIES_PER_HOUR = "CALORIES_PER_HOUR";
    @NonNls public static final String WATTS = "WATTS";
    @NonNls public static final String METS = "METS";
    @NonNls public static final String PACE = "PACE";
    @NonNls public static final String AVERAGE_PACE = "AVERAGE_PACE";
    @NonNls public static final String RPM = "RPM";
    @NonNls public static final String SPM = "SPM";
    @NonNls public static final String AVERAGE_SPM = "AVERAGE_SPM";

    @NonNls public static final String STRIDE_LENGTH_METERS = "STRIDE_LENGTH_METERS";
    @NonNls public static final String AVERAGE_STRIDE_LENGTH_METERS = "AVERAGE_STRIDE_LENGTH_METERS";

    // Values for default time of cooldown and warmup
    @NonNls public static final String COOLDOWN_TIME_START = "COOLDOWN_TIME_START";
    @NonNls public static final String WARMUP_TIME_START = "WARMUP_TIME_START";

    // Can be updated during the running of the script
    @NonNls public static final String TARGET_WATTS_START = "TARGET_WATTS_START";
    @NonNls public static final String TARGET_WATTS = "TARGET_WATTS";
    @NonNls public static final String TARGET_HEART_RATE = "TARGET_HEART_RATE";

    // Whether the workout is currently shown in metric. This will change based on what distance parameter is selected
    @NonNls public static final String IS_METRIC = "IS_METRIC";

    // SPRINT8 VARIABLES
    // Ramp rates for determining how long the frame will take to adjust the speed/incline/resistance
    @NonNls public static final String RAMP_RATE_SPEED = "RAMP_RATE_SPEED";
    @NonNls public static final String RAMP_RATE_INCLINE = "RAMP_RATE_INCLINE";
    @NonNls public static final String RAMP_RATE_RESISTANCE = "RAMP_RATE_RESISTANCE";

    // Constant values for elevation levels for navy test
    @NonNls public static final String FRAME_ELEVATION = "FRAME_ELEVATION";

    @NonNls public static final String MIN_HEART_RATE = "MIN_HEART_RATE";
    @NonNls public static final String MAX_HEART_RATE = "MAX_HEART_RATE";

    // Some workouts started from MatrixComm may wish to disable auto pause as they have portions done off the machine (RETAIL ONLY)
    @NonNls public static final String ALLOW_AUTO_PAUSE = "ALLOW_AUTO_PAUSE";

    /* ***** CONSTANT VALUES *******/
    public static final int COOPER_WARNING_NONE = 0;
    public static final int COOPER_WARNING_TOO_SLOW = 1;
    public static final int COOPER_WARNING_TOO_FAST = 2;

    public static final int SUBMAX_OUTPUT_TEST_COMPLETE = 0;
    public static final int SUBMAX_OUTPUT_HR_DID_NOT_INCREASE = 1;
    public static final int SUBMAX_OUTPUT_HR_TOO_LOW = 2;

    public static final int WFI_TEST_SUCCESSFUL = 0;
    public static final int WFI_TEST_FAILED_HR_TOO_LOW = 1;
    public static final int WFI_TEST_NO_HR_INCREASE = 2;
    public static final int WFI_TEST_STOPPED = 3;
    public static final int WFI_NO_HR_FOUND = 4;
}

