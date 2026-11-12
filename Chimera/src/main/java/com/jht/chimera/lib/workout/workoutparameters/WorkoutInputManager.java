package com.jht.chimera.lib.workout.workoutparameters;

import android.util.Log;

import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.enums.MinMaxParam;

/**
 * Manages getting the units for inputs of workouts from the workout controller.
 * <p>
 * Created by chris.foyer on 3/5/2018.
 */
public class WorkoutInputManager
{
    private static final String TAG = "WorkoutInputManager";

    /**
     * Links the MinMaxDictionary units to the input string type
     *
     * @param inputString The string the workout script is attempting to receive an input for
     * @return The unit or null if there is no unit available
     */
    public static MinMaxParam getUnitType(String inputString)
    {
        switch (inputString)
        {
            case ConsoleWorkoutVariables.START_SPEED:
                return MinMaxParam.Speed;
            case ConsoleWorkoutVariables.START_INCLINE:
                return MinMaxParam.Incline;
            case ConsoleWorkoutVariables.START_RESISTANCE:
                return MinMaxParam.Resistance;
            case ConsoleWorkoutVariables.TIME_GOAL_START:
                return MinMaxParam.WorkoutTime;
            case ConsoleWorkoutVariables.CALORIE_GOAL_START:
                return MinMaxParam.Calories;
            case ConsoleWorkoutVariables.DISTANCE_GOAL_START:
                return MinMaxParam.Distance;
            case ConsoleWorkoutVariables.FLOORS_GOAL_START:
                return MinMaxParam.Floors;
            case ConsoleWorkoutVariables.NUM_INTERVALS:
                return MinMaxParam.IntervalCount;
            case ConsoleWorkoutVariables.INTERVAL_TIME_GOAL:
                return MinMaxParam.IntervalTime;
            case ConsoleWorkoutVariables.INTERVAL_DISTANCE_GOAL:
                return MinMaxParam.IntervalDistance;
            case ConsoleWorkoutVariables.REST_TIME_GOAL:
                return MinMaxParam.RestTime;
            case ConsoleWorkoutVariables.PERCENT_MAX_HEART_RATE:
                return MinMaxParam.PercentMaxHeartRate;
            case ConsoleWorkoutVariables.LEVEL:
                return MinMaxParam.Level;
            case ConsoleWorkoutVariables.TARGET_WATTS_START:
                return MinMaxParam.ConstantWatts;
            case ConsoleWorkoutVariables.MAX_TARGET_WATTS_PAIR:
                return MinMaxParam.MaxWattsPair;
            case ConsoleWorkoutVariables.MIN_TARGET_WATTS_PAIR:
                return MinMaxParam.MinWattsPair;
            case ConsoleWorkoutVariables.WEIGHT:
                return MinMaxParam.Weight;
            case ConsoleWorkoutVariables.AGE:
                return MinMaxParam.Age;
            case ConsoleWorkoutVariables.MAXIMUM_INCLINE:
                return MinMaxParam.MaxIncline;
            case ConsoleWorkoutVariables.MAXIMUM_SPEED:
                return MinMaxParam.MaxSpeed;
            case ConsoleWorkoutVariables.MAXIMUM_RESISTANCE:
                return MinMaxParam.MaxResistance;
            case ConsoleWorkoutVariables.MAX_RESISTANCE:
                return MinMaxParam.MaxResistancePair;
            case ConsoleWorkoutVariables.MIN_RESISTANCE:
                return MinMaxParam.MinResistancePair;
            case ConsoleWorkoutVariables.MAX_SPEED:
                return MinMaxParam.MaxSpeedPair;
            case ConsoleWorkoutVariables.MIN_SPEED:
                return MinMaxParam.MinSpeedPair;
            case ConsoleWorkoutVariables.HEIGHT:
                return MinMaxParam.Height;
            case ConsoleWorkoutVariables.GENDER:
                return MinMaxParam.Gender;
            case ConsoleWorkoutVariables.TARGET_AVG_SPEED:
                return MinMaxParam.TargetSpeed;
            case "FTP_WATTS": //NON-NLS
                return MinMaxParam.FTP;
            default:
                Log.v(TAG, "Cannot create slider for input " + inputString);
                return null;
        }
    }
}
