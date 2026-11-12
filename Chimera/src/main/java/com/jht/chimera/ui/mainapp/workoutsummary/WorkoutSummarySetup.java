package com.jht.chimera.ui.mainapp.workoutsummary;

import android.util.Log;

import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the format for the lines in the workout summary
 * <p>
 * Created by chris.foyer on 1/8/2018.
 */
@SuppressWarnings("WeakerAccess")
public class WorkoutSummarySetup {
    private static final String TAG = "WorkoutSummarySetup";

    public WorkoutSummarySetup() {
    }

    /**
     * Gets the workout summary format for the current frame type and workout.
     *
     * @return The format for the workout summary for the current frame and workout
     */
    public List<WorkoutSummaryItem> getWorkoutSummary(boolean showGymKit) {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        switch (config.machineClassType) {
            case Treadmill:
                return getTreadmillSummary(config, showGymKit);
            case Ascent:
                return getAscentSummary(config, showGymKit);
            case Elliptical:
            case RecumbentElliptical:
            case Bike:
            case SpinBike:
                return getEllipticalOrBikeSummary(config, showGymKit);
            case Climbmill:
            case Stepper:
                return getClimbmillOrStepperSummary(config, showGymKit);
            default:
                Log.e(TAG, "Invalid frame type!");
                return getTreadmillSummary(config, showGymKit);
        }
    }

    private List<WorkoutSummaryItem> getTreadmillSummary(FrameConfig config, boolean showGymKit) {
        List<WorkoutSummaryItem> summary = new ArrayList<>();
        addFitnessTestItems(summary);
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.TimeElapsed, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.Distance, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageSpeed, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageIncline, false));
        if (showGymKit) {
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.ActiveCalories, false));
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.TotalCalories, true));
        } else {
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.Calories, true));
        }
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AveragePace, false));
        if (!config.WATTS_INACCURATE) summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageWatts, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageMets, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageHeartRate, true));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakSpeedKph, false));
        if (!config.WATTS_INACCURATE) summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakWatts, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakMets, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakHeartRate, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakIncline, true));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.Elevation, false));
        return summary;
    }

    private List<WorkoutSummaryItem> getAscentSummary(FrameConfig config, boolean showGymKit) {
        List<WorkoutSummaryItem> summary = new ArrayList<>();
        addFitnessTestItems(summary);
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.TimeElapsed, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.Distance, false));
        if (showGymKit) {
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.ActiveCalories, false));
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.TotalCalories, false));
        } else {
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.Calories, false));
        }
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageIncline, false));
        if (!config.WATTS_INACCURATE) summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageWatts, true));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AveragePace, false));    //avg time / mile
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageSpeed, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageMets, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageRpm, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageHeartRate, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageResistance, true));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakSpeedKph, false));
        if (!config.WATTS_INACCURATE) summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakWatts, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakMets, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakRpm, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakHeartRate, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakIncline, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakResistance, true));
        return summary;
    }

    private List<WorkoutSummaryItem> getEllipticalOrBikeSummary(FrameConfig config, boolean showGymKit) {
        List<WorkoutSummaryItem> summary = new ArrayList<>();
        addFitnessTestItems(summary);
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.TimeElapsed, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.Distance, false));
        if (showGymKit) {
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.ActiveCalories, false));
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.TotalCalories, false));
        } else {
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.Calories, false));
        }
        if (!config.WATTS_INACCURATE) summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageWatts, true));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AveragePace, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageSpeed, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageMets, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageRpm, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageHeartRate, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageResistance, true));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakSpeedKph, false));
        if (!config.WATTS_INACCURATE) summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakWatts, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakMets, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakRpm, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakHeartRate, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakResistance, true));
        return summary;
    }

    private List<WorkoutSummaryItem> getClimbmillOrStepperSummary(FrameConfig config, boolean showGymKit) {
        List<WorkoutSummaryItem> summary = new ArrayList<>();
        addFitnessTestItems(summary);
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.TimeElapsed, false));
        if (showGymKit) {
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.ActiveCalories, false));
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.TotalCalories, false));
        } else {
            summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.Calories, false));
        }
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.Steps, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.Floors, true));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageSpm, false));
        if (!config.WATTS_INACCURATE) summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageWatts, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageMets, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.AverageHeartRate, true));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakSpm, false));
        if (!config.WATTS_INACCURATE)  summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakWatts, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakMets, false));
        summary.add(new WorkoutSummaryItem(WorkoutSummaryValues.PeakHeartRate, false));
        return summary;
    }

    private void addFitnessTestItems(List<WorkoutSummaryItem> summaryItems) {
        int workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        switch (workoutId) {
            case WorkoutIds.COOPER_FITNESS_TEST:
                summaryItems.add(new WorkoutSummaryItem(WorkoutSummaryValues.VO2, false));
                summaryItems.add(new WorkoutSummaryItem(WorkoutSummaryValues.VO2Percentile, true));
                break;
            case WorkoutIds.SUBMAX_FITNESS_TEST:
                summaryItems.add(new WorkoutSummaryItem(WorkoutSummaryValues.MaxVO2, true));
                break;
            case WorkoutIds.GERKIN_FITNESS_TEST:
                summaryItems.add(new WorkoutSummaryItem(WorkoutSummaryValues.MaxVO2, true));
                break;
        }
    }

    /**
     * Contains information for a single line of the workout summary.
     */
    public class WorkoutSummaryItem {
        /**
         * The ID associated with a stat ID constant in {@link WorkoutSummaryValues}
         */
        public int statId;

        /**
         * Whether the stat should have a dividing line underneath it
         */
        public boolean isDivider;

        /**
         * An item to define a single line of the workout summary
         *
         * @param statId    The ID of the stat from {@link WorkoutSummaryValues}
         * @param isDivider Whether there is a divider below the stat
         */
        public WorkoutSummaryItem(int statId, boolean isDivider) {
            this.statId = statId;
            this.isDivider = isDivider;
        }
    }
}
