package com.jht.chimera.lib.workout.workoutlist;

import android.util.Log;

import com.amazonaws.util.IOUtils;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A list of all workout scripts that are available to run on the system. These workout scripts are
 * part of the resources for the console.
 */
public class WorkoutScripts {
    private static final String TAG = "WorkoutScripts";

    private List<WorkoutItem> workouts = new ArrayList<>();

    /**
     * Used to define a workout. Needs to point at workout script in the workout scripts folder.
     */
    private class WorkoutItem {
        int script;
        int workoutId;
        int maxValue;
        boolean treadmill;
        boolean bike;
        boolean elliptical;
        boolean ascent;
        boolean climbmill;
        boolean stepper;
        boolean cardioSpinBike;

        public WorkoutItem(int script, int workoutId, int maxValue, boolean treadmill,
                           boolean bike, boolean elliptical, boolean ascent, boolean climbmill, boolean stepper, boolean cardioSpinBike) {
            this.script = script;
            this.workoutId = workoutId;
            this.maxValue = maxValue;
            this.treadmill = treadmill;
            this.bike = bike;
            this.elliptical = elliptical;
            this.ascent = ascent;
            this.climbmill = climbmill;
            this.stepper = stepper;
            this.cardioSpinBike = cardioSpinBike;
        }
    }

    public WorkoutScripts() {
        workouts.add(new WorkoutItem(R.raw.manual_speed, 0, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.manual_resistance, 0, 0, false, true, true, false, true, true, true));
        workouts.add(new WorkoutItem(R.raw.manual_ascent, 0, 0, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.rolling_hills_treadmill, 9, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.rolling_hills_resistance_20, 9, 20, false, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.rolling_hills_resistance_25, 9, 25, false, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.rolling_hills_resistance_30, 9, 30, false, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.distance_goal_treadmill, 14, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.distance_goal_resistance, 14, 0, false, true, true, false, false, false, true));
        workouts.add(new WorkoutItem(R.raw.distance_goal_ascent, 14, 0, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.calories_goal_treadmill, 15, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.calories_goal_resistance, 15, 0, false, true, true, false, true, true, true));
        workouts.add(new WorkoutItem(R.raw.calories_goal_ascent, 15, 0, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.floors_goal, 16, 0, false, false, false, false, true, true, false));
        workouts.add(new WorkoutItem(R.raw.sprint8_treadmill, 18, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.sprint8_resistance_25, 18, 25, false, true, true, false, true, true, true));
        workouts.add(new WorkoutItem(R.raw.sprint8_ascent_25, 18, 25, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.sprint8_ascent_20, 18, 20, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.sprint8_ascent_30, 18, 30, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.sprint8_resistance_20, 18, 20, false, true, true, false, true, true, true));
        workouts.add(new WorkoutItem(R.raw.sprint8_resistance_30, 18, 30, false, true, true, false, true, true, true));
        workouts.add(new WorkoutItem(R.raw.constant_watts_resistance, 20, 0, false, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.constant_watts_spinbike, 20, 0, false, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.gerkin_fitness_test, 30, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.army_fitness_test, 33, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.navy_fitness_test, 34, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.air_force_fitness_test, 35, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.marines_fitness_test, 36, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.peb_fitness_test, 37, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.cooper_fitness_test, 38, 0, false, true, true, true, false, false, true));
        workouts.add(new WorkoutItem(R.raw.intervals_treadmill, 40, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.intervals_resistance_20, 40, 20, false, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.intervals_resistance_25, 40, 25, false, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.intervals_resistance_30, 40, 30, false, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.thr_speed_treadmill, 44, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.thr_incline_treadmill, 45, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.thr_incline_ascent, 45, 0, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.thr_resistance, 46, 0, false, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.va_treadmill, 47, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.va_resistance, 47, 0, false, true, true, true, false, true, true));
        workouts.add(new WorkoutItem(R.raw.va_climbmill, 47, 0, false, false, false, false, true, false, false));
        workouts.add(new WorkoutItem(R.raw.fat_burn_20_resistance, 49, 20, false, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.fat_burn_25_resistance, 49, 25, false, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.fat_burn_30_resistance, 49, 30, false, true, true, true, false, false, true));
        workouts.add(new WorkoutItem(R.raw.fat_burn_30_climbmill, 49, 30, false, false, false, false, true, true, false));
        workouts.add(new WorkoutItem(R.raw.fat_burn_treadmill, 49, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.glutes_ascent, 50, 0, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.cpat_fitness_test, 51, 0, false, false, false, false, true, true, false));
        workouts.add(new WorkoutItem(R.raw.wfi_fitness_test, 52, 0, false, false, false, false, true, true, false));
        workouts.add(new WorkoutItem(R.raw.submax_fitness_test, 53, 0, false, false, false, false, true, true, false));
        workouts.add(new WorkoutItem(R.raw.landmarks, 55, 0, false, false, false, false, true, true, false));
        workouts.add(new WorkoutItem(R.raw.stepper_fitness_test, 56, 0, false, false, false, false, true, true, false));
        workouts.add(new WorkoutItem(R.raw.intervals_speed_treadmill, 57, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.migros_potential_resistance, 58, 0, false, false, false, false, true, true, false));
        workouts.add(new WorkoutItem(R.raw.migros_potential_speed, 58, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.migros_potential_target_watts, 58, 0, false, true, true, true, false, false, true));
        workouts.add(new WorkoutItem(R.raw.migros_exhaustion_resistance, 59, 0, false, false, false, false, true, true, false));
        workouts.add(new WorkoutItem(R.raw.migros_exhaustion_speed, 59, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.migros_exhaustion_target_watts, 59, 0, false, true, true, true, false, false, true));
        workouts.add(new WorkoutItem(R.raw.migros_fatigue_resistance, 60, 0, false, false, false, false, true, true, false));
        workouts.add(new WorkoutItem(R.raw.migros_fatigue_speed, 60, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.migros_fatigue_target_watts, 60, 0, false, true, true, true, false, false, true));
        workouts.add(new WorkoutItem(R.raw.featured_treadmill, 71, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.featured_resistance, 71, 0, false, true, true, false, true, true, true));
        workouts.add(new WorkoutItem(R.raw.featured_ascent, 71, 0, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.ftp_test_8, 72, 0, false, true, true, false, false, false, true));
        workouts.add(new WorkoutItem(R.raw.pace_time_goal, 73, 0, false, false, false, false, false, false, true));
        workouts.add(new WorkoutItem(R.raw.pace_distance_goal, 74, 0, false, false, false, false, false, false, true));
        workouts.add(new WorkoutItem(R.raw.ftp_test_20, 75, 0, false, true, true, true, false, false, true));
        workouts.add(new WorkoutItem(R.raw.max_hr_test, 76, 0, false, true, true, false, false, false, true));
        workouts.add(new WorkoutItem(R.raw.marathon_treadmill, 77, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.marathon_ascent, 77, 0, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.marathon_resistance, 77, 0, false, true, true, false, true, true, true));
        workouts.add(new WorkoutItem(R.raw.marathon_hiit, 78, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.marathon_hiit_ascent, 78, 0, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.marathon_hiit_resistance, 78, 0, false, true, true, false, false, true, true));
        workouts.add(new WorkoutItem(R.raw.marathon_egym, 79, 0, true, true, true, true, true, true, true));
        workouts.add(new WorkoutItem(R.raw.target_training_resistance, 89, 0, false, true, true, false, false, false, true));
        workouts.add(new WorkoutItem(R.raw.time_goal_speed, 93, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.time_goal_resistance, 93, 0, false, true, true, false, true, true, true));
        workouts.add(new WorkoutItem(R.raw.time_goal_ascent, 93, 0, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.manual_speed_no_cooldown, 94, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.manual_ascent_no_cooldown, 94, 0, false, false, false, true, false, false, false));
        workouts.add(new WorkoutItem(R.raw.manual_resistance_no_cooldown, 94, 0, false, true, true, false, true, true, true));
        workouts.add(new WorkoutItem(R.raw.switch_stride_hybrid_treadmill, 67, 0, true, false, false, false, false, false, false));
        workouts.add(new WorkoutItem(R.raw.spin_bike_intervals_distance, 88, 0,false, false,false,false,false,false,true));
        workouts.add(new WorkoutItem(R.raw.spin_bike_intervals_time, 118, 0,false, false,false,false,false,false,true));
    }

    public WorkoutScript getWorkoutScript(int workoutId) {
        WorkoutItem workoutItem = getWorkoutItem(workoutId);
        if (workoutItem != null) {
            String scriptString = getScriptString(workoutItem.script);
            return new WorkoutScript(workoutItem.workoutId, workoutItem.maxValue, scriptString);
        } else {
            return null;
        }
    }

    /**
     * Gets the script from the resources
     * @param id The ID for the script file
     * @return The script from the resources or null
     */
    private String getScriptString(int id) {
        try {
            InputStream inputStream = ContextUtil.getApplicationContext().getResources().openRawResource(id);
            return IOUtils.toString(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private WorkoutItem getWorkoutItem(int workoutId) {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        JHTCommonTypes.MachineClassificationType frameType = frameConfig.machineClassType;

        int maxValue = getMaxValue();

        List<WorkoutItem> matchingWorkouts = new ArrayList<>();
        for (WorkoutItem workoutItem : workouts) {
            if (workoutItem.workoutId == workoutId &&
                    matchesFrame(workoutItem, frameType) &&
                    matchesMaxValue(workoutItem, maxValue)) {
                matchingWorkouts.add(workoutItem);
            }
        }

        return matchingWorkouts.size() > 0 ? matchingWorkouts.get(0) : null;
    }

    /**
     * Checks that the workout can be run on the intended frame type
     *
     * @param workout   The workout to check
     * @param frameType The frame type to check
     * @return Whether the workout is available for the frame
     */
    private boolean matchesFrame(WorkoutItem workout, JHTCommonTypes.MachineClassificationType frameType) {
        switch (frameType) {
            case Treadmill:
                return workout.treadmill;
            case Ascent:
                return workout.ascent;
            case Bike:
            case RecumbentElliptical: //recumbent elliptical should get bike workouts
                return workout.bike;
            case Elliptical:
                return workout.elliptical;
            case Climbmill:
                return workout.climbmill;
            case Stepper:
                return workout.stepper;
            case SpinBike:
                return workout.cardioSpinBike;
            default:
                Log.e(TAG, "Invalid frame type: " + frameType.name() + ". Setting to treadmill workouts.");
                return workout.treadmill;
        }
    }

    /**
     * Checks whether a workout matches the max value correctly.
     *
     * @param workout  The workout to check
     * @param maxValue The intended max value
     * @return Whether the workout matches
     */
    private boolean matchesMaxValue(WorkoutItem workout, int maxValue) {
        return workout.maxValue == 0 || maxValue == 0 || workout.maxValue == maxValue;
    }

    /**
     * Gets the frame max value so we can select workouts with the matching scaled max values.
     * This is usually the max resistance for the frame.
     *
     * @return the max value for the frame
     */
    private static int getMaxValue() {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        switch (frameConfig.machineClassType) {
            case Treadmill:
                return 0;
            case Ascent:
            case Bike:
            case Elliptical:
            case Climbmill:
            case Stepper:
            case SpinBike:
                return Math.round(frameConfig.resistance.getMaxModel());
            default:
                Log.e(TAG, "Invalid frame type: " + frameConfig.machineClassType.name() + ". Setting to 0 max value.");
                return 0;
        }
    }
}
