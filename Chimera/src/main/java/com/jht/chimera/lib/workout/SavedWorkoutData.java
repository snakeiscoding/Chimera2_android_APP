package com.jht.chimera.lib.workout;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.stats.WorkoutSummary;

import java.util.Date;

/** Data that is saved locally about a workout */
public class SavedWorkoutData {
    public @Keep Date endTime;
    public @Keep @Nullable WorkoutSummary totalSummary;
    public @Keep @Nullable RecordedWorkout recordedWorkout;
    public @Keep @Nullable WorkoutStartupParams startupParams;
}
