package com.jht.chimera.lib.widget.statarray;

import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.AVERAGE_PACE;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.AVERAGE_SPM;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.CALORIES;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.CALORIES_PER_HOUR;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.CLOCK;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.DISTANCE_IMPERIAL;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.DISTANCE_METRIC;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.ELEVATION;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.FLOORS;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.GOAL_REMAINING;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.HEART_RATE;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.METS;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.PACE;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.PEAK_HEART_RATE;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.RPM;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.SPEED;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.SPM;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.STEPS;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.TIME_ELAPSED;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.TOTAL_GOAL;
import static com.jht.chimera.lib.widget.statarray.StatValueUpdaterCreator.WATTS;

import android.util.Log;

import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;

/**
 * Maps values of stats used on the console to different data matrix keys and integer values.
 * Uses these mapped values for to define the stat bar layouts.
 * <p>
 * Created by chris.foyer on 8/22/2017.
 */
public class StatArrays
{
    private static final String TAG = "StatArrays";

    public static final int[][] treadmill = {
            {HEART_RATE, PEAK_HEART_RATE},
            {CALORIES, CALORIES_PER_HOUR, WATTS, METS},
            {TIME_ELAPSED, GOAL_REMAINING, TOTAL_GOAL, CLOCK},
            {DISTANCE_METRIC, DISTANCE_IMPERIAL},
            {PACE, AVERAGE_PACE, ELEVATION}};

    public static final int[][] rpmBased = {
            {HEART_RATE, PEAK_HEART_RATE},
            {CALORIES, CALORIES_PER_HOUR, WATTS, METS},
            {TIME_ELAPSED, GOAL_REMAINING, TOTAL_GOAL, CLOCK},
            {DISTANCE_METRIC, DISTANCE_IMPERIAL},
            {RPM, SPEED, PACE, AVERAGE_PACE}};

    public static final int[][] stepsBased = {
            {HEART_RATE, PEAK_HEART_RATE},
            {CALORIES, CALORIES_PER_HOUR, WATTS, METS},
            {TIME_ELAPSED, GOAL_REMAINING, TOTAL_GOAL, CLOCK},
            {STEPS, FLOORS},
            {SPM, AVERAGE_SPM}};


    // 30 series does not have watts due to low accuracy
    public static final int[][] rpmBased30Series = {
            {HEART_RATE, PEAK_HEART_RATE},
            {CALORIES, CALORIES_PER_HOUR, METS},
            {TIME_ELAPSED, GOAL_REMAINING, TOTAL_GOAL, CLOCK},
            {DISTANCE_METRIC, DISTANCE_IMPERIAL},
            {RPM, SPEED, PACE, AVERAGE_PACE}};

    /**
     * Gets the current frame stat array based on the values that are currently in the config.
     * @return The current frame stat array
     */
    public static int[][] getCurrentFrameArray()
    {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        switch (config.machineClassType)
        {
            case Treadmill:
                return StatArrays.treadmill;
            case Elliptical:
            case RecumbentElliptical:
            case Bike:
            case Ascent:
            case SpinBike:
                if (config.WATTS_INACCURATE) return StatArrays.rpmBased30Series;
                else return StatArrays.rpmBased;
            case Stepper:
            case Climbmill:
                return StatArrays.stepsBased;
            default:
                Log.e(TAG, "Frame type " + config.machineClassType.name() + " is not supported! Setting as treadmill stats");
                return StatArrays.treadmill;
        }
    }
}
