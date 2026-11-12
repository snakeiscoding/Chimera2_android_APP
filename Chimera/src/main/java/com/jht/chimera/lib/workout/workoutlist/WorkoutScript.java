package com.jht.chimera.lib.workout.workoutlist;

/**
 * A usable workout script
 */
public class WorkoutScript {
    public int workoutId;
    public int maxValue;
    public String scriptString;

    public WorkoutScript(int workoutId, int maxValue, String scriptString) {
        this.workoutId = workoutId;
        this.maxValue = maxValue;
        this.scriptString = scriptString;
    }
}
