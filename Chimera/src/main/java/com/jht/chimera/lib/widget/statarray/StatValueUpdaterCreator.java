package com.jht.chimera.lib.widget.statarray;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.widget.statarray.formatting.DistanceFormatter;
import com.jht.chimera.lib.widget.statarray.formatting.ElevationFormatter;
import com.jht.chimera.lib.widget.statarray.formatting.FloatToIntFormatter;
import com.jht.chimera.lib.widget.statarray.formatting.GoalFormatter;
import com.jht.chimera.lib.widget.statarray.formatting.HeartRateFormatter;
import com.jht.chimera.lib.widget.statarray.formatting.InclineFormatter;
import com.jht.chimera.lib.widget.statarray.formatting.PaceFormatter;
import com.jht.chimera.lib.widget.statarray.formatting.SpeedFormatter;
import com.jht.chimera.lib.widget.statarray.formatting.StatFloorFormatter;
import com.jht.chimera.lib.widget.statarray.formatting.StatFormatter;
import com.jht.chimera.lib.widget.statarray.formatting.TimeFormatter;

import org.jetbrains.annotations.NonNls;

import java.lang.reflect.Method;

/**
 * Maps values of stats used on the console to different data matrix keys and integer values.
 * Uses these mapped values for to define the stat bar layouts. Values go through a formatter which
 * can convert the values and provide information on how they should be displayed.
 * <p>
 * Created by chris.foyer on 8/22/2017.
 */
public class StatValueUpdaterCreator {
    @NonNls private final String TAG = "StatValueUpdaterCreator";

    public static final int HEART_RATE = 0;
    public static final int PEAK_HEART_RATE = 1;

    public static final int CALORIES = 2;
    public static final int CALORIES_PER_HOUR = 3;
    public static final int WATTS = 4;
    public static final int METS = 5;

    public static final int TIME_ELAPSED = 6;
    public static final int GOAL_REMAINING = 7;
    public static final int TOTAL_GOAL = 8;
    public static final int CLOCK = 9;

    public static final int DISTANCE_METRIC = 10;
    public static final int DISTANCE_IMPERIAL = 11; // For the alternative unit

    public static final int PACE = 12;
    public static final int AVERAGE_PACE = 13;
    public static final int ELEVATION = 14;

    public static final int RPM = 15;
    public static final int TARGET_SPEED = 16;
    public static final int TARGET_SPEED_SECONDARY = 17;
    public static final int SPEED = 18;
    public static final int SPEED_SECONDARY = 19;

    public static final int STEPS = 20;
    public static final int FLOORS = 21;

    public static final int SPM = 22;
    public static final int AVERAGE_SPM = 23;

    public static final int INCLINE = 24;
    public static final int RESISTANCE = 25;

    public static final int TARGET_WATTS = 26;

    public static final int PERCENT_HEART_RATE = 27;
    public static final int MYZONE_HR_PERCENT = 28; // Used for MyZone HR Percentage
    public static final int MYZONE_MEPS = 29; // MyZone Effort Points

    public static final int STRIDE_LENGTH_METERS = 30;  //Cadence related
    public static final int AVERAGE_STRIDE_LENGTH_METERS = 31;

    private SparseArray<StatValueUpdater> mStatValues;

    private static StatValueUpdaterCreator mStatCreator;

    public static StatValueUpdaterCreator getInstance() {
        if (mStatCreator == null) mStatCreator = new StatValueUpdaterCreator();
        return mStatCreator;
    }

    private StatValueUpdaterCreator() {
        initializeStatUpdating();
    }

    /**
     * Initializes stat updating. This method defines the different stats, their labels, default values,
     * and how they are formatted. The stat updaters are then available through calling
     * {@link #getValue(int)} which allows getting the formatted values and titles through a callback.
     */
    private void initializeStatUpdating() {
        Context context;
        try {
            final Class<?> activityThreadClass;
            activityThreadClass = Class.forName("android.app.ActivityThread");
            final Method method = activityThreadClass.getMethod("currentApplication");
            Application application = (Application) method.invoke(null, (Object[]) null);
            context = application.getApplicationContext();
        } catch (Exception e) {
            Log.e(TAG, "Could not create context for stat value updater!", e);
            return;
        }

        HeartRateFormatter hrFormatter = new HeartRateFormatter(context, R.string.heart_rate_uc);
        mStatValues = new SparseArray<>();
        StatValueUpdater heartRate = new StatValueUpdater(context, HEART_RATE, HALKeys.DATA_HEART_RATE_INT.name(), StatValueUpdater.STAT_TYPE_INT, hrFormatter, hrFormatter);
        mStatValues.append(HEART_RATE, heartRate);
        HeartRateFormatter peakHrFormatter = new HeartRateFormatter(context, R.string.peak_heart_rate_uc);
        StatValueUpdater peakHeartRate = new StatValueUpdater(context, PEAK_HEART_RATE, WorkoutKeys.DATA_PEAK_HEART_RATE_INT.name(), StatValueUpdater.STAT_TYPE_INT, peakHrFormatter, peakHrFormatter);
        mStatValues.append(PEAK_HEART_RATE, peakHeartRate);
        StatValueUpdater percentHeartRate = new StatValueUpdater(context, PERCENT_HEART_RATE, WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, R.string.percent_heart_rate_uc, "0", new StatFormatter(0));
        mStatValues.append(PERCENT_HEART_RATE, percentHeartRate);

        StatValueUpdater myZonePercentHeartRate = new StatValueUpdater(context, MYZONE_HR_PERCENT, HALKeys.DATA_MYZONE_EFFORT_INT.name(), StatValueUpdater.STAT_TYPE_INT, R.string.percent_hr_uc, "0%", new StatFormatter(0, "%"));
        mStatValues.append(MYZONE_HR_PERCENT, myZonePercentHeartRate);

        StatValueUpdater myZoneMeps = new StatValueUpdater(context, MYZONE_MEPS, HALKeys.DATA_MYZONE_MEPS_INT.name(), StatValueUpdater.STAT_TYPE_INT, R.string.myzone_meps_tc, "0", new StatFormatter(0));
        mStatValues.append(MYZONE_MEPS, myZoneMeps);

        FloatToIntFormatter caloriesFormatter = new FloatToIntFormatter(context, R.string.calories_uc);
        StatValueUpdater calories = new StatValueUpdater(context, CALORIES, WorkoutKeys.DATA_CALORIES_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, caloriesFormatter, caloriesFormatter);
        mStatValues.append(CALORIES, calories);
        StatValueUpdater caloriesPerHour = new StatValueUpdater(context, CALORIES_PER_HOUR, WorkoutKeys.DATA_CALORIES_PER_HOUR_INT.name(), StatValueUpdater.STAT_TYPE_INT, R.string.calories_per_hour_uc, "0");
        mStatValues.append(CALORIES_PER_HOUR, caloriesPerHour);
        StatValueUpdater watts = new StatValueUpdater(context, WATTS, HALKeys.DATA_WATTS_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, R.string.watts_uc, "0", new StatFloorFormatter(0));
        mStatValues.append(WATTS, watts);
        StatValueUpdater mets = new StatValueUpdater(context, METS, HALKeys.DATA_METS_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, R.string.mets_uc, "0", new StatFloorFormatter(2));
        mStatValues.append(METS, mets);

        StatValueUpdater timeElapsed = new StatValueUpdater(context, TIME_ELAPSED, WorkoutKeys.DATA_TIME_ELAPSED_MS_INT.name(), StatValueUpdater.STAT_TYPE_INT, R.string.time_elapsed_uc, "0:00", new TimeFormatter());
        mStatValues.append(TIME_ELAPSED, timeElapsed);

        GoalFormatter goalRemainingFormatter = new GoalFormatter(context, true);
        GoalFormatter goalFormatter = new GoalFormatter(context, false);
        StatValueUpdater goalRemaining = new StatValueUpdater(context, GOAL_REMAINING, WorkoutKeys.DATA_GOAL_REMAINING_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, goalRemainingFormatter, goalRemainingFormatter);
        mStatValues.append(GOAL_REMAINING, goalRemaining);
        StatValueUpdater totalGoal = new StatValueUpdater(context, TOTAL_GOAL, WorkoutKeys.DATA_GOAL_VALUE_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, goalFormatter, goalFormatter);
        mStatValues.append(TOTAL_GOAL, totalGoal);
        ClockStatValue clock = new ClockStatValue(context, R.string.clock_uc, "0:00");
        mStatValues.append(CLOCK, clock);

        DistanceFormatter metricDistanceFormatter = new DistanceFormatter(context, true);
        DistanceFormatter imperialDistanceFormatter = new DistanceFormatter(context, false);
        StatValueUpdater distanceMetric = new StatValueUpdater(context, DISTANCE_METRIC, WorkoutKeys.DATA_DISTANCE_KM_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, metricDistanceFormatter, metricDistanceFormatter);
        mStatValues.append(DISTANCE_METRIC, distanceMetric);
        StatValueUpdater distanceImperial = new StatValueUpdater(context, DISTANCE_IMPERIAL, WorkoutKeys.DATA_DISTANCE_KM_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, imperialDistanceFormatter, imperialDistanceFormatter);
        mStatValues.append(DISTANCE_IMPERIAL, distanceImperial);

        PaceFormatter paceFormatter = new PaceFormatter(context, false);
        StatValueUpdater pace = new StatValueUpdater(context, PACE, WorkoutKeys.DATA_WORKOUT_SPEED_KPH_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, paceFormatter, paceFormatter);
        mStatValues.append(PACE, pace);

        PaceFormatter averagePaceFormatter = new PaceFormatter(context, true);
        StatValueUpdater averagePace = new StatValueUpdater(context, AVERAGE_PACE, WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, averagePaceFormatter, averagePaceFormatter);
        mStatValues.append(AVERAGE_PACE, averagePace);

        ElevationFormatter elevationFormatter = new ElevationFormatter(context, false);
        StatValueUpdater elevation = new StatValueUpdater(context, ELEVATION, WorkoutKeys.DATA_ELEVATION_METERS_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, elevationFormatter, elevationFormatter);
        mStatValues.append(ELEVATION, elevation);

        StatValueUpdater rpm = new StatValueUpdater(context, RPM, HALKeys.DATA_RPM_INT.name(), StatValueUpdater.STAT_TYPE_INT, R.string.rpm_uc, "0");
        mStatValues.append(RPM, rpm);

        SpeedFormatter speedFormatter = new SpeedFormatter(context, false);
        StatValueUpdater currentSpeed = new StatValueUpdater(context, TARGET_SPEED, HALKeys.DATA_TARGET_SPEED_KPH_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, speedFormatter, speedFormatter);
        currentSpeed.setNextValueDataMatrixKey(WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_SPEED_FLOAT.name());
        mStatValues.append(TARGET_SPEED, currentSpeed);

        SpeedFormatter speedFormatterSecondary = new SpeedFormatter(context, true);
        StatValueUpdater secondarySpeed = new StatValueUpdater(context, TARGET_SPEED_SECONDARY, HALKeys.DATA_TARGET_SPEED_KPH_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, speedFormatterSecondary, speedFormatterSecondary);
        mStatValues.append(TARGET_SPEED_SECONDARY, secondarySpeed);

        SpeedFormatter speedFormatterActual = new SpeedFormatter(context, false);
        StatValueUpdater speedActual = new StatValueUpdater(context, SPEED, HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, speedFormatterActual, speedFormatterActual);
        mStatValues.append(SPEED, speedActual);

        SpeedFormatter speedFormatterActualSecondary = new SpeedFormatter(context, true);
        StatValueUpdater actualSecondarySpeed = new StatValueUpdater(context, SPEED_SECONDARY, HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, speedFormatterActualSecondary, speedFormatterActualSecondary);
        mStatValues.append(SPEED_SECONDARY, actualSecondarySpeed);

        StatValueUpdater steps = new StatValueUpdater(context, STEPS, WorkoutKeys.DATA_STEPS_INT.name(), StatValueUpdater.STAT_TYPE_INT, R.string.steps_uc, "0");
        mStatValues.append(STEPS, steps);

        FloatToIntFormatter floorFormatter = new FloatToIntFormatter(context, R.string.floors_uc);
        StatValueUpdater floor = new StatValueUpdater(context, FLOORS, WorkoutKeys.DATA_FLOORS_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, floorFormatter, floorFormatter);
        mStatValues.append(FLOORS, floor);

        StatValueUpdater spm = new StatValueUpdater(context, SPM, HALKeys.DATA_SPM_INT.name(), StatValueUpdater.STAT_TYPE_INT, R.string.spm_uc, "0");
        mStatValues.append(SPM, spm);

        StatValueUpdater averageSpm = new StatValueUpdater(context, AVERAGE_SPM, WorkoutKeys.DATA_AVERAGE_SPM_INT.name(), StatValueUpdater.STAT_TYPE_INT, R.string.average_spm_uc, "0");
        mStatValues.append(AVERAGE_SPM, averageSpm);

        StatValueUpdater strideLengthMeters = new StatValueUpdater(context, STRIDE_LENGTH_METERS, WorkoutKeys.DATA_STRIDE_LENGTH_METERS_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, R.string.stride_length_tc, "0.0");
        mStatValues.append(STRIDE_LENGTH_METERS, strideLengthMeters);

        StatValueUpdater averageStrideLengthMeters = new StatValueUpdater(context, AVERAGE_STRIDE_LENGTH_METERS, WorkoutKeys.DATA_AVERAGE_STRIDE_LENGTH_METERS_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, R.string.average_stride_length_tc, "0.0");
        mStatValues.append(AVERAGE_STRIDE_LENGTH_METERS, averageStrideLengthMeters);

        InclineFormatter inclineFormatter = new InclineFormatter(context);
        StatValueUpdater incline = new StatValueUpdater(context, INCLINE, HALKeys.DATA_TARGET_INCLINE_FLOAT.name(), StatValueUpdater.STAT_TYPE_FLOAT, inclineFormatter, inclineFormatter);
        incline.setNextValueDataMatrixKey(WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_INCLINE_FLOAT.name());
        mStatValues.append(INCLINE, incline);

        StatValueUpdater resistanceUpdater = new StatValueUpdater(context, RESISTANCE, HALKeys.DATA_RESISTANCE_INT.name(), StatValueUpdater.STAT_TYPE_INT, R.string.resistance_uc, "0");
        resistanceUpdater.setNextValueDataMatrixKey(WorkoutKeys.DATA_WORKOUT_SCRIPT_NEXT_RESISTANCE_INT.name());
        mStatValues.append(RESISTANCE, resistanceUpdater);

        StatValueUpdater targetWattsUpdater = new StatValueUpdater(context, TARGET_WATTS, WorkoutKeys.DATA_TARGET_WATTS_INT.name(), StatValueUpdater.STAT_TYPE_INT, R.string.target_watts_tc, "0");
        mStatValues.append(TARGET_WATTS, targetWattsUpdater);
    }

    /**
     * Gets a StatValue from an ID.
     *
     * @param id The ID for a specific type
     * @return The stat value for the ID
     */
    public StatValueUpdater getValue(int id) {
        return mStatValues.get(id);
    }
}
