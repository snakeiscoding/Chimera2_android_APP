package com.jht.chimera.service.workout.stat;


import com.jht.chimera.service.workout.enums.WorkoutGoalType;
import com.jht.chimera.service.workout.enums.WorkoutRunState;
import com.jht.chimera.service.workout.enums.WorkoutState;

public class WorkoutStateData {
    // current running state (run, pause, stop)
    public WorkoutRunState workoutRunState = WorkoutRunState.STOP;
    // current workout state (warmup, workout, cooldown)
    public WorkoutState workoutState = WorkoutState.NONE;
    // current goal type of mode
    public WorkoutGoalType goalType = WorkoutGoalType.NONE;
    // current goal value for workout mode
    public float goalValue;
    // current goal remaining for workout mode
    public float goalRemaining;


    // interval name if one exists
    public int intervalIndex;
    // interval goal type if one exists
    public WorkoutGoalType intervalGoalType = WorkoutGoalType.NONE;
    // interval goal if one exists
    public float intervalGoalValue;
    // interval goal remaining if one exists
    public float intervalGoalRemaining;
    // interval name if one exists
    public String intervalName = new String();

    // workout id read from workout xml
    public int id;

    @Override
    public String toString() {
        return "WorkoutStateData{" +
                "workoutState=" + workoutState +
                ", goalTypeFixed=" + goalType +
                ", goalValueFixed=" + goalValue +
                ", goalRemaining=" + goalRemaining +
                ", intervalIndex=" + intervalIndex +
                ", intervalGoalType=" + intervalGoalType +
                ", intervalGoalValue=" + intervalGoalValue +
                ", intervalGoalRemaining=" + intervalGoalRemaining +
                ", intervalName='" + intervalName + '\'' +
                '}';
    }
}
