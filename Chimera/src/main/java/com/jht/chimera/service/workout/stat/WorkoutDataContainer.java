package com.jht.chimera.service.workout.stat;

import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.stats.WorkoutSummary;
import com.jht.chimera.lib.common.stats.WorkoutSummaryList;
import com.jht.datamatrixlib.DataMatrix;

import java.util.List;

/**
 * Contains all the stat data for a single workout.
 */
@SuppressWarnings("WeakerAccess")
public class WorkoutDataContainer
{
    public final String TAG = "WorkoutDataContainer";

    private WorkoutDataContainer()
    {
        reset();
    }

    private static class SingletonHolder
    {
        private static final WorkoutDataContainer INSTANCE = new WorkoutDataContainer();
    }

    public static WorkoutDataContainer getInstance()
    {
        return SingletonHolder.INSTANCE;
    }

    public WorkoutStateData workoutStateData;
    public WorkoutSummary workoutSummaryTotal;

    public WorkoutSummary workoutSummaryWarmup;
    public WorkoutSummary workoutSummaryWorkout;
    public WorkoutSummary workoutSummaryCooldown;

    // Tracks stats for individual workout intervals. Intervals are denoted by the workout scripts.
    public WorkoutSummaryList intervalWorkoutSummaries = new WorkoutSummaryList();

    /**
     * Resets and removes all data stored in this object
     */
    public void reset()
    {
        workoutStateData = new WorkoutStateData();
        workoutSummaryWarmup = new WorkoutSummary();
        workoutSummaryWorkout = new WorkoutSummary();
        workoutSummaryCooldown = new WorkoutSummary();
        workoutSummaryTotal = new WorkoutSummary();
        intervalWorkoutSummaries.workoutSummaries.clear();

        DataMatrix.putInt(WorkoutKeys.DATA_LAP_COUNT_INT, 0);
        DataMatrix.putInt(WorkoutKeys.DATA_LAP_BEST_TIME_MS_INT, 0);
    }


    /**
     * Gets the latest workout summary for the current stage the workout is in.
     * @return The workout summary for the current stage
     */
    public WorkoutSummary getCurrentStateWorkoutSummary()
    {
        switch (DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT))
        {
            case WorkoutKeys.Constants.WORKOUT_STAGE_WARMUP:
                return workoutSummaryWarmup;
            case WorkoutKeys.Constants.WORKOUT_STAGE_WORKOUT:
                return workoutSummaryWorkout;
            case WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN:
                return workoutSummaryCooldown;
        }

        return null;
    }

    /**
     * Gets the interval workout information
     * @return The saved workout info for the current intervals
     */
    public List<WorkoutSummary> getIntervalSummaries() {
        return intervalWorkoutSummaries.workoutSummaries;
    }
}
