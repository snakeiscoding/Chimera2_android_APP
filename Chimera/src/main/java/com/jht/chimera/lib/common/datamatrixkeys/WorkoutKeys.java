package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.ParcelableDataDefault;
import com.jht.workoutcontrollerlib.WorkoutController;
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutControllerCardio;

import java.util.LinkedList;
import java.util.List;

/**
 * The keys for controlling the workout and determining the state of the workout.
 * <p>
 * The WorkoutController also tracks the splits that are made from the track app.
 */
public enum WorkoutKeys {
    // Setting the file path will load the selected script and allow for starting the workout
    ACTION_LOAD_WORKOUT_ENTRY_PARCELABLE,       // Used to send workout entry to WorkoutControllerService
    ACTION_LOAD_WORKOUT_XML_PARCELABLE,         // Used to send a custom xml workout to WorkoutControllerService
    ACTION_SET_SPRINT8_WORKOUT_INFO_PARCELABLE, // Used to send BaseSprint8Config workout to WorkoutControllerService
    ACTION_GET_MODEL,                           // Loads the model so we can graph Sprint8

    // Used as a callback from WorkoutControllerService to WorkoutManager to signal a workout has been loaded
    EVENT_LOADED_WORKOUT_INPUTS_PARCELABLE, // StringInputList.class
    // Stores the maximum level allowable for the program
    DATA_NUM_LEVELS_INT(new DataDefault<>(0)),


    // Contains workout model information for Sprint8 workouts
    EVENT_MODEL_INFORMATION_RESPONSE_PARCELABLE, // WorkoutModel.class
    // When replaying a recorded workout, it goes here
    DATA_CURRENT_RECORDED_WORKOUT_PARCELABLE(new ParcelableDataDefault()), // RecordedWorkout.class

    // This will be updated by the WorkoutController when the workout state changes
    DATA_RUN_STATE_INT(new DataDefault<>(Constants.RUN_STATE_STOP)),

    // This will be updated by the service to reflect if it is in the process of starting/switching a workout
    DATA_LAUNCH_STATE_BOOL(new DataDefault<>(false)),

    // The ID of a loaded workout
    DATA_WORKOUT_ID_INT(new DataDefault<>(0)),

    // This will be used to pass extra data if required
    DATA_WORKOUT_OPTION_ID_STRING(new DataDefault<>("")),

    //last value actions for speed/resistance/incline
    ACTION_LAST_SPEED,
    ACTION_LAST_RESISTANCE,
    ACTION_LAST_INCLINE,
    DATA_LAST_VALUE_FLASH_OK_BOOL(new DataDefault<>(false)),

    DATA_WORKOUT_APP_UUID_STRING(new DataDefault<>("")),
    DATA_WORKOUT_APP_SETUP_PARCELABLE(new ParcelableDataDefault()), // Bundle for any extras needed by workout app (could we just pass with start intent to avoid this key?)
    DATA_CUSTOM_WORKOUT_UUID_STRING(new DataDefault<>("")),
    // programUuid sent in the last 701; ProgramUuidResolver makes a new one per call, so read this instead
    DATA_LAST_PROGRAM_UUID_STRING(new DataDefault<>("")),

    // Workout launching
    ACTION_LAUNCH_WORKOUT_PARCELABLE, // WorkoutLaunchData.class

    // These will start and stop the currently loaded workout
    ACTION_STOP_WORKOUT,
    ACTION_RESUME_WORKOUT,
    ACTION_PAUSE_WORKOUT,
    ACTION_ABORT_WORKOUT,

    // Signal when a passive workout is starting (true) or stopping (false), for example in the case of ifit strength workout video
    ACTION_PASSIVE_WORKOUT_BOOL,
    // Used by the UiService to increment or decrement the goal, automatically updates by the correct value
    ACTION_INCREASE_WORKOUT_GOAL,
    ACTION_DECREASE_WORKOUT_GOAL,
    ACTION_SET_TIME_GOAL_SECONDS_INT,
    ACTION_UPDATE_WORKOUT_GOAL_FLOAT,

    // Notifies the completion of a workout cycle
    ACTION_WORKOUT_STATS_UPDATED,

    // The workout stage currently underway
    DATA_WORKOUT_STAGE_INT(new DataDefault<>(0)),

    // Goal can be modified from anywhere and it will be reflected in the Workout Service
    DATA_GOAL_TYPE_INT(new DataDefault<>(7)),
    DATA_GOAL_VALUE_FLOAT(new DataDefault<>(0f)),
    DATA_GOAL_REMAINING_FLOAT(new DataDefault<>(0f)),
    DATA_GOAL_WORKOUT_STAGE_TYPE_INT(new DataDefault<>(0)),        //This is the goal for the workout only.
    DATA_GOAL_WORKOUT_STAGE_FLOAT(new DataDefault<>(0f)),            //This ONLY tracks the goal value from the workout stage.
    DATA_GOAL_WORKOUT_STAGE_REMAINING_FLOAT(new DataDefault<>(0f)),  //This ONLY tracks the goal remaining value from the workout stage.
    DATA_PRE_COOL_DOWN_TIME_ELAPSED_MS_INT(new DataDefault<>(0)),  //This ONLY tracks the time elapsed before Cool-Down
    DATA_COOL_DOWN_VARIABLE_PROGRESS_FLOAT(new DataDefault<>(0f)),   //Used to hold the progress value to retain the last value before cool-down starts.
    DATA_REACHED_FULL_WORKOUT_BOOL(new DataDefault<>(false)),

    DATA_INTERVAL_GOAL_VALUE_FLOAT(new DataDefault<>(0f)),
    DATA_INTERVAL_GOAL_REMAINING_FLOAT(new DataDefault<>(0f)),
    DATA_STAGE_INTERVAL_NUM_INT(new DataDefault<>(0)), // This value is available for all workouts, will be the interval number inside a specific stage
    DATA_INTERVAL_NUM_INT(new DataDefault<>(0)), // This value must be built into the workout, and can be used by apps that need more information
    DATA_INTERVAL_LIST_PARCELABLE(new ParcelableDataDefault()),

    // Workout time elapsed in ms
    DATA_TIME_ELAPSED_MS_INT(new DataDefault<>(0)),

    // Information about lap times
    ACTION_START_SPLIT,
    DATA_LAP_TIME_MS_INT(new DataDefault<>(0)),
    DATA_LAP_COUNT_INT(new DataDefault<>(0)),
    DATA_LAP_BEST_TIME_MS_INT(new DataDefault<>(0)),
    DATA_LAP_DISTANCE_KM_FLOAT(new DataDefault<>(0f)),
    DATA_SPLIT_LIST_PARCELABLE(new ParcelableDataDefault()),
    DATA_CURRENT_SPLIT_PARCELABLE(new ParcelableDataDefault()),
    DATA_TRACK_LAPS_PARCELABLE(new ParcelableDataDefault()),

    // Controls the left bar target heart rate percentage during a target heart rate workout
    DATA_TARGET_HEART_RATE_PERCENT_FLOAT(new DataDefault<>(0f)),

    // Sends a message to HAL to change speed/incline/resistance to a specific value
    ACTION_SET_SPEED_KPH_WORKOUT_FLOAT,
    ACTION_SET_INCLINE_WORKOUT_FLOAT,
    ACTION_SET_RESISTANCE_WORKOUT_INT,

    // scripted values from workout script
    DATA_WORKOUT_SCRIPT_NEXT_SPEED_FLOAT(new DataDefault<>(0f)),
    DATA_WORKOUT_SCRIPT_NEXT_INCLINE_FLOAT(new DataDefault<>(0f)),
    DATA_WORKOUT_SCRIPT_NEXT_RESISTANCE_INT(new DataDefault<>(0)),
    DATA_WORKOUT_SCRIPT_IS_RAMPING_BOOL(new DataDefault<>(false)),
    DATA_WORKOUT_SCRIPT_IS_SPRINTING_BOOL(new DataDefault<>(false)),


    // Contains values for different workout stats
    // Values read from sensors are put into the DataMatrix by HAL
    DATA_DISTANCE_KM_FLOAT(new DataDefault<>(0f)),
    DATA_AVERAGE_SPM_INT(new DataDefault<>(0)),
    DATA_STEPS_INT(new DataDefault<>(0)),
    DATA_FLOORS_FLOAT(new DataDefault<>(0f)),
    DATA_ELEVATION_METERS_FLOAT(new DataDefault<>(0f)),
    DATA_PACE_SEC_PER_KM_INT(new DataDefault<>(0)),
    DATA_AVERAGE_PACE_SEC_PER_KM_INT(new DataDefault<>(0)),
    DATA_WORKOUT_SPEED_KPH_FLOAT(new DataDefault<>(0f)),
    DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT(new DataDefault<>(0f)),
    DATA_CURRENT_AVG_TARGET_SPEED_KPH_FLOAT(new DataDefault<>(0f)),
    DATA_CALORIES_FLOAT(new DataDefault<>(0f)),
    DATA_ACTIVE_CALORIES_FLOAT(new DataDefault<>(0f)),
    DATA_CALORIES_PER_HOUR_INT(new DataDefault<>(0)),
    DATA_PEAK_HEART_RATE_INT(new DataDefault<>(0)),
    DATA_JOULES_FLOAT(new DataDefault<>(0f)),
    DATA_AVERAGE_WATTS_FLOAT(new DataDefault<>(0f)),
    DATA_AVERAGE_RPM_FLOAT(new DataDefault<>(0f)),
    DATA_AVERAGE_HEART_RATE_FLOAT(new DataDefault<>(0f)),
    DATA_AVERAGE_INCLINE_FLOAT(new DataDefault<>(0f)),
    DATA_PREVIOUS_SPEED_FLOAT(new DataDefault<>(0f)),
    DATA_PREVIOUS_INCLINE_FLOAT(new DataDefault<>(0f)),
    DATA_PREVIOUS_RESISTANCE_INT(new DataDefault<>(0)),
    DATA_AVERAGE_RESISTANCE_FLOAT(new DataDefault<>(0f)),
    DATA_STRIDE_COUNT_FLOAT(new DataDefault<>(0f)),

    // The target watts value for the constant watts program
    DATA_TARGET_WATTS_INT(new DataDefault<>(0)),
    DATA_TARGET_HEART_RATE_INT(new DataDefault<>(0)),
    DATA_TARGET_SPEED_KPH_FLOAT(new DataDefault<>(0f)),

    // User Target value for the FTP test
    DATA_TARGET_FTP_FLOAT(new DataDefault<>(0f)),

    // Workout Settings
    DATA_ALLOW_SKIP_WORKOUT_BOOL(new DataDefault<>(true)),
    DATA_ALLOW_SKIP_WARMUP_BOOL(new DataDefault<>(true)),
    DATA_ALLOW_USER_GOAL_CHANGES_BOOL(new DataDefault<>(true)),
    DATA_ALLOW_USER_CHANGES_BOOL(new DataDefault<>(true)),
    DATA_ALLOW_USER_PAUSE_BOOL(new DataDefault<>(true)),
    DATA_ALLOW_COUNTDOWN_BOOL(new DataDefault<>(true)),
    DATA_DISABLE_PEDAL_CHANGES_BOOL(new DataDefault<>(false)),
    DATA_SETTINGS_DIALOG_OPEN_BOOL(new DataDefault<>(false)),

    // Sprint8 Settings
    DATA_SPRINT8_SWEAT_SCORE_FLOAT(new DataDefault<>(0f)),
    DATA_SPRINT8_SWEAT_SCORES_PARCELABLE(new ParcelableDataDefault()),
    DATA_SPRINT8_CURRENT_STATE_INT(new DataDefault<>(0)),
    DATA_STRIDE_LENGTH_METERS_FLOAT(new DataDefault<>(0f)),
    DATA_AVERAGE_STRIDE_LENGTH_METERS_FLOAT(new DataDefault<>(0f)),

    // Cardio Scan Settings
    DATA_CS_THR_ZONE_INT(new DataDefault<>(0)),
    DATA_CS_THR_ZONES_PARCELABLE(new ParcelableDataDefault(null)),

    // Skip actions
    ACTION_SKIP_INTERVAL,
    ACTION_SKIP_TO_COOLDOWN,

    // Workout summary data
    DATA_WORKOUT_SUMMARY_INFO_PARCELABLE(new ParcelableDataDefault()),

    //Parcelable class - com.jht.chimera.lib.common.WorkoutFuture
    DATA_WORKOUT_FUTURE_PARCELABLE(new ParcelableDataDefault()),

    ACTION_SET_WORKOUT_SPEED_MULTIPLIER_FLOAT,

    // Keys specifically used for different fitness tests
    // if nothing is added this will default to 1 in the 701 call, per the documentation for 701 v4
    // TODO - populate this key with data from workouts that use levels that we want to show up in v4 of the 701 call
    DATA_WORKOUT_LEVEL_INT(new DataDefault<>(0)),
    DATA_VO2_FLOAT(new DataDefault<>(0f)),
    DATA_FITNESS_TEST_SCORE_FLOAT(new DataDefault<>(0f)),
    DATA_FITNESS_TEST_SCORE_MALE_FLOAT(new DataDefault<>(0f)),
    DATA_FITNESS_TEST_SCORE_FEMALE_FLOAT(new DataDefault<>(0f)),
    DATA_SUBMAX_OUTPUT_INT(new DataDefault<>(0)),
    DATA_MAX_HEART_RATE_FLOAT(new DataDefault<>(0f)),
    DATA_SUBMAX_MIN_HEART_RATE_FLOAT(new DataDefault<>(0f)),
    DATA_SUBMAX_MAX_HEART_RATE_FLOAT(new DataDefault<>(0f)),

    DATA_WORKOUT_GENDER_INT(new DataDefault<>(0)),

    ACTION_RESET_STATS,

    EVENT_WORKOUT_SWITCHED,
    EVENT_WORKOUT_START,
    DATA_WORKOUT_APP_PAGE_INT(new DataDefault<>(0)),

    // string to hold which 3rd party service starts a workout - needed for the 701 command to dapi
    // if nothing is put here this will default to 'MATRIX' in the 701 call, per the documentation for 701 v4
    // TODO - populate this key with data from 3rd party services starting workouts that we want to show up in v4 of the 701 call
    DATA_WORKOUT_ORIGINATOR_STRING(new DataDefault<>("")),

    // Workout graph data
    EVENT_DISTANCE_MARKERS_PARCELABLE,
    DATA_LCB_CONTROL_MODE_INT(new DataDefault<>(0)),

    // Used in DataMatrixListFactory
    DATA_WORKOUT_SNAPSHOT_PARCELABLE(new ParcelableDataDefault()),
    EVENT_GET_SNAPSHOT_HISTORY,
    ACTION_NUM_SNAPSHOTS,
    ACTION_SNAPSHOT_HISTORY_RESPONSE_PARCELABLE,
    // To keep track of the current selected VA or Landmark Workout Title.  Needed since the normal
    // workout id will be the same for all VA and Landmark respectively.  Used in the 701 call and landmark activity
    DATA_SELECTED_VA_OR_LANDMARK_TITLE_STRING(new DataDefault<>("")),

    ACTION_HOT_SWAP_LEVEL_INT,

    //The four keys are used for the SpinBikeIntervalsActivity
    DATA_WORKOUT_NUMBER_OF_INTERVALS_INT(new DataDefault<>(0)),
    DATA_WORKOUT_INTERVAL_DISTANCE_GOAL_KM_FLOAT(new DataDefault<>(0f)),
    DATA_WORKOUT_INTERVAL_TIME_GOAL_FLOAT(new DataDefault<>(0f)),
    DATA_WORKOUT_REST_TIME_GOAL_FLOAT(new DataDefault<>(0f));


    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (WorkoutKeys s : WorkoutKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    WorkoutKeys(){
        this.DataDefault = null;
    }

    WorkoutKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }

    public class Constants {
        // The different goal types for workouts
        // The goal type determines the type of goal and how goal progress is found
        public static final int GOAL_TYPE_NONE = ConsoleWorkoutVariables.GOAL_TYPE_NONE;
        public static final int GOAL_TYPE_TIME = ConsoleWorkoutVariables.GOAL_TYPE_TIME;
        public static final int GOAL_TYPE_DISTANCE = ConsoleWorkoutVariables.GOAL_TYPE_DISTANCE;
        public static final int GOAL_TYPE_CALORIES = ConsoleWorkoutVariables.GOAL_TYPE_CALORIES;
        public static final int GOAL_TYPE_FLOORS = ConsoleWorkoutVariables.GOAL_TYPE_FLOORS;
        public static final int GOAL_TYPE_STEPS = ConsoleWorkoutVariables.GOAL_TYPE_STEPS;
        public static final int GOAL_TYPE_ELEVATION = ConsoleWorkoutVariables.GOAL_TYPE_ELEVATION;
        public static final int GOAL_TYPE_TIME_STAGE = ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE;
        public static final int GOAL_TYPE_DISTANCE_STAGE = ConsoleWorkoutVariables.GOAL_TYPE_DISTANCE_STAGE;
        public static final int GOAL_TYPE_CALORIES_STAGE = ConsoleWorkoutVariables.GOAL_TYPE_CALORIES_STAGE;
        public static final int GOAL_TYPE_FLOORS_STAGE = ConsoleWorkoutVariables.GOAL_TYPE_FLOORS_STAGE;
        public static final int GOAL_TYPE_STEPS_STAGE = ConsoleWorkoutVariables.GOAL_TYPE_STEPS_STAGE;
        public static final int GOAL_TYPE_ELEVATION_STAGE = ConsoleWorkoutVariables.GOAL_TYPE_ELEVATION_STAGE;

        // Different workout stage constants
        public static final int WORKOUT_STAGE_WARMUP = WorkoutController.WORKOUT_STAGE_WARMUP;
        public static final int WORKOUT_STAGE_WORKOUT = WorkoutController.WORKOUT_STAGE_WORKOUT;
        public static final int WORKOUT_STAGE_COOLDOWN = WorkoutController.WORKOUT_STAGE_COOLDOWN;

        // Workout states
        public static final int RUN_STATE_STOP = WorkoutControllerCardio.WORKOUT_STATE_STOPPED;
        public static final int RUN_STATE_PAUSE = WorkoutControllerCardio.WORKOUT_STATE_PAUSED;
        public static final int RUN_STATE_RUNNING = WorkoutControllerCardio.WORKOUT_STATE_RUNNING;
        public static final int RUN_STATE_SWITCHING = WorkoutControllerCardio.WORKOUT_STATE_SWITCHING;
    }
}
