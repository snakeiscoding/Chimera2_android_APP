package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to update the goal during a workout.
 */
public class UpdateGoalCommand extends RequestBase {
    private final int workoutTimeSeconds;
    private final double workoutDistanceKm;
    private final int workoutCalories;

    /**
     * Create a command for testing.
     * @param workoutTimeSeconds    New workout time.  Only read if current goal type is time.
     * @param workoutDistanceKm     New workout distance.  Only read if current goal type is distance.
     * @param workoutCalories       New workout calories.  Only read if current goal type is calories.
     */
    @VisibleForTesting
    public UpdateGoalCommand(int workoutTimeSeconds, double workoutDistanceKm, int workoutCalories) {
        super("updategoal", 1);
        this.workoutTimeSeconds = workoutTimeSeconds;
        this.workoutDistanceKm = workoutDistanceKm;
        this.workoutCalories = workoutCalories;
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public UpdateGoalCommand(JSONObject object) throws JSONException {
        super(object);
        workoutTimeSeconds = object.optInt("time", 0);
        workoutDistanceKm = object.optDouble("distance", 0);
        workoutCalories = object.optInt("calorie", 0);
    }

    /**
     * Update the goal if we are in the workout and the goal values are valid.  Send an error otherwise.
     *
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        // Is there any chance of race between starting workout and this state change? Should we just rely on try-catch in workout controller?
        boolean isInWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP;
        if (!isInWorkout) {
            sendErrorResponse("not in workout", socket);
            return;
        }
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        int goalType = DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT);
        switch (goalType) {
            case ConsoleWorkoutVariables.GOAL_TYPE_TIME:
            case ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE:
                if (workoutTimeSeconds > FrameConfig.THIRD_PARTY_MAX_WORKOUT_TIME_SECONDS  || workoutTimeSeconds <= 0) {
                    sendErrorResponse("invalid time", socket);
                    return;
                }
                DataMatrix.putFloat(WorkoutKeys.ACTION_UPDATE_WORKOUT_GOAL_FLOAT, (float) workoutTimeSeconds * 1000);
                break;
            case ConsoleWorkoutVariables.GOAL_TYPE_FLOORS:
            case ConsoleWorkoutVariables.GOAL_TYPE_FLOORS_STAGE:
                if (workoutDistanceKm > FrameConfig.THIRD_PARTY_MAX_WORKOUT_DISTANCE || workoutDistanceKm <= 0) {
                    sendErrorResponse("invalid distance", socket);
                    return;
                }
                float distancePerFloor = frameConfig.getDistancePerStep() * frameConfig.STEPS_PER_FLOOR;
                DataMatrix.putFloat(WorkoutKeys.ACTION_UPDATE_WORKOUT_GOAL_FLOAT, (float) workoutDistanceKm / distancePerFloor);
                break;
            case ConsoleWorkoutVariables.GOAL_TYPE_DISTANCE:
            case ConsoleWorkoutVariables.GOAL_TYPE_DISTANCE_STAGE:
                if (workoutDistanceKm > FrameConfig.THIRD_PARTY_MAX_WORKOUT_DISTANCE || workoutDistanceKm <= 0) {
                    sendErrorResponse("invalid distance", socket);
                    return;
                }
                DataMatrix.putFloat(WorkoutKeys.ACTION_UPDATE_WORKOUT_GOAL_FLOAT, (float) workoutDistanceKm);
                break;
            case ConsoleWorkoutVariables.GOAL_TYPE_CALORIES:
            case ConsoleWorkoutVariables.GOAL_TYPE_CALORIES_STAGE:
                if (workoutCalories > frameConfig.workoutCalories.getMax() || workoutCalories < frameConfig.workoutCalories.getMin()) {
                    sendErrorResponse("invalid calories", socket);
                    return;
                }
                DataMatrix.putFloat(WorkoutKeys.ACTION_UPDATE_WORKOUT_GOAL_FLOAT, (float) workoutCalories);
                break;
            case -1: // not in a workout, acknowledge the command even though we didn't do anything
                break;
            default:
                sendErrorResponse("Mismatched goalType: '" + goalType + "'", socket);
                return;
        }
        sendGeneralResponse("ok", socket);
    }

    /**
     * @return  The command in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("time", workoutTimeSeconds);
            object.put("distance", workoutDistanceKm);
            object.put("calorie", workoutCalories);
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public String toString() {
        return "UpdateGoalCommand: " +
                "time " + workoutTimeSeconds +
                ", distance " + workoutDistanceKm +
                ", time " + workoutCalories +
                ", calories " + workoutTimeSeconds +
                ", " + super.toString();
    }
}
