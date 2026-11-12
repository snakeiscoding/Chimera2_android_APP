package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.WorkoutInput;
import com.jht.chimera.lib.workout.WorkoutManager;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.WorkoutMap;
import com.jht.chimera.lib.workoutapps.AppInfo;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.chimera.service.workout.WorkoutSettingOverrides;
import com.jht.data.WorkoutItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.workoutdefinition.statements.SettingValue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This is used to start a workout.  In general we just need a goal type (time, distance, or calories)
 * and a goal value.
 */
public class StartWorkoutCommand extends RequestBase {
    public enum GOAL_TYPE {
        TIME,
        DISTANCE,
        CALORIE,
        INVALID;

        public static GOAL_TYPE fromString(String value) {
            try {
                return GOAL_TYPE.valueOf(value.toUpperCase(Locale.ENGLISH));
            }
            catch(IllegalArgumentException ignored) {
                return INVALID;
            }
        }
    }
    private final GOAL_TYPE goalType;
    private final String goalTypeName;
    private final int workoutTimeSeconds;
    private final double workoutDistanceKm;
    private final int workoutCalories;
    private final boolean autoPause;

    /**
     * Create a basic command for testing.
     * @param goalType              The goal type. (time, distance, or calories.
     * @param workoutTimeSeconds    The workout time.  Only matters if goal type is time.
     * @param workoutDistanceKm     The workout distance.  Only matters if goal type is distance.
     * @param workoutCalories       The workout calories.  Only matters if goal type is calories.
     * @param autoPause             True if we can auto pause workout.  Only useful for retail currently.
     */
    @VisibleForTesting
    public StartWorkoutCommand(GOAL_TYPE goalType, int workoutTimeSeconds, double workoutDistanceKm, int workoutCalories, boolean autoPause) {
        super("startworkout", 1);
        this.goalType = goalType;
        goalTypeName = goalType.toString().toLowerCase(Locale.ENGLISH);
        this.workoutTimeSeconds = workoutTimeSeconds;
        this.workoutDistanceKm = workoutDistanceKm;
        this.workoutCalories = workoutCalories;
        this.autoPause = autoPause;
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public StartWorkoutCommand(JSONObject object) throws JSONException {
        super(object);
        goalTypeName = object.optString("goalType", "invalid");
        goalType = GOAL_TYPE.fromString(goalTypeName);
        workoutTimeSeconds = object.optInt("time", 0);
        workoutDistanceKm = object.optDouble("distance", 0);
        workoutCalories = object.optInt("calorie", 0);
        autoPause = object.optBoolean("autoPause", false);
    }

    /**
     * Start the workout if it is safe to do so and the goal type / value are valid.  Send an
     * error message if we cannot start the workout.
     *
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        switch (goalType) {
            case TIME:
                if (workoutTimeSeconds > FrameConfig.THIRD_PARTY_MAX_WORKOUT_TIME_SECONDS || workoutTimeSeconds <= 0) {
                    sendErrorResponse("invalid time", socket);
                    return;
                }
                break;
            case DISTANCE:
                if (workoutDistanceKm > FrameConfig.THIRD_PARTY_MAX_WORKOUT_DISTANCE || workoutDistanceKm <= 0) {
                    sendErrorResponse("invalid distance", socket);
                    return;
                }
                break;
            case CALORIE:
                if (workoutCalories > frameConfig.workoutCalories.getMax() || workoutCalories <= frameConfig.workoutCalories.getMin()) {
                    sendErrorResponse("invalid calories", socket);
                    return;
                }
                break;
            default:
                sendErrorResponse("Unrecognized goalType: '" + goalTypeName + "'", socket);
                return;
        }
        startWorkout(socket);

    }

    /**
     * @return  The command in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("goalType", goalType.toString().toLowerCase(Locale.ENGLISH));
            object.put("time", workoutTimeSeconds);
            object.put("distance", workoutDistanceKm);
            object.put("calorie", workoutCalories);
            object.put("autoPause", autoPause);
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public String toString() {
        return "StartWorkoutCommand: goalType " + goalType.toString() +
                ", time " + workoutTimeSeconds +
                ", distance " + workoutDistanceKm +
                ", time " + workoutCalories +
                ", calories " + workoutTimeSeconds +
                ", autoPause " + autoPause +
                ", " + super.toString();
    }

    /**
     * The goal parameters are all good.  Go ahead and start the workout if it is safe.
     * @param socket        The socket to send any error messages or the result if started.
     */
    private void startWorkout(JSONLocalSocket socket) {
        String goalVar;
        float workoutGoalValue;
        int workoutId; // See https://jhtnaee.atlassian.net/wiki/spaces/DOC/pages/3506251/Console+Program+Table
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        WorkoutSettingOverrides settingOverrides = new WorkoutSettingOverrides();

        switch (goalType) {
            case TIME:
                goalVar = ConsoleWorkoutVariables.TIME_GOAL_START;
                workoutGoalValue = workoutTimeSeconds / 60f; // seconds -> minutes
                workoutId = 0;
                break;
            case DISTANCE:
                if (frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill) ||
                    frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Stepper)) {
                    // convert to floors
                    float distancePerFloor = frameConfig.getDistancePerStep() * frameConfig.STEPS_PER_FLOOR; // km per floor
                    goalVar = ConsoleWorkoutVariables.FLOORS_GOAL_START;
                    workoutGoalValue = (float) (workoutDistanceKm / distancePerFloor); // floors
                    workoutId = 16;
                } else {
                    goalVar = ConsoleWorkoutVariables.DISTANCE_GOAL_START;
                    workoutGoalValue = (float) workoutDistanceKm; // km
                    workoutId = 14;
                }
                break;
            case CALORIE:
                goalVar = ConsoleWorkoutVariables.CALORIE_GOAL_START;
                workoutGoalValue = workoutCalories; // kcal
                workoutId = 15;
                break;
            default:
                sendErrorResponse("Invalid goal type: " + goalType, socket); //NON-NLS
                return;
        }
        WorkoutMap workoutMap = WorkoutMap.Companion.createWorkoutMap();
        WorkoutManager workoutManager = new WorkoutManager(workoutMap);
        WorkoutItem workoutEntry = workoutMap.getWorkout(workoutId);
        if (workoutEntry == null) {
            throw new RuntimeException("Oh no there's no workout entry");
        }

        // just use app ID that was launched (modification of return should be fine since we created a local copy)
        String appId = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING);
        // keep the same app up, or do we need a special value to indicate launcher should skip launching app/activity?
        workoutManager.loadWorkout(workoutId, appId, null);

        List<WorkoutInput> initialConditions = new ArrayList<>();
        initialConditions.add(new WorkoutInput(goalVar, workoutGoalValue));
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_SPEED, DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT)));
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_INCLINE, FrameConfigManager.getInstance().getFrameConfig().incline.getMin()));
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_RESISTANCE, DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT)));

        //Only allow disabling auto pause for retail
        initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.ALLOW_AUTO_PAUSE, (!autoPause && ProductConfig.isRetail() ? 0 : 1)));

        AppInfo appInfo = AppManager.getInstance().getApp(appId);
        if (appInfo != null) {
            if (appInfo.settingOverrides != null) {
                for (SettingValue value : appInfo.settingOverrides) settingOverrides.add(value);
            }
            if (appInfo.workoutInputs != null) {
                for (WorkoutInput value : appInfo.workoutInputs) initialConditions.add(value);
            }
        }

        if (!workoutManager.startLoadedWorkout(initialConditions, false, settingOverrides)) {
            sendErrorResponse("Cannot start workout", socket);
        }
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.WORKOUT.name());
        sendGeneralResponse("ok", socket);
    }

}
