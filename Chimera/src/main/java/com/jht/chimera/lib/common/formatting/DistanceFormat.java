package com.jht.chimera.lib.common.formatting;

import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Formats distances in kilometers (float) to be passed to the stat bar for display
 * <p>
 */
public class DistanceFormat {
    /**
     * Formats elevation in meters to the correct value based on the settings of the console
     *
     * @param meters Meters gained
     * @return The formatted string
     */
    public static String formatElevation(float meters) {
        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
        return formatElevation(meters, isMetric);
    }

    public static String formatElevation(float meters, boolean metric) {
        // round the same way as gymkit which takes in .1 meters
        meters = (int) (meters * 10f) / 10f;

        if (metric) {
            return FloorFormat.format(meters, 0);
        } else {
            float feet = DistanceConvertUtil.convert(meters, DistanceConvertUtil.METERS_ID, DistanceConvertUtil.FEET_ID);
            return FloorFormat.format(feet, 0);
        }
    }

    /**
     * Formats workout distance and converts to the correct unit based on the settings of the console.
     *
     * @param kilometers The workout distance in kilometers
     * @return The formatted string
     */
    public static String formatWorkoutDistance(float kilometers) {
        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
        return formatWorkoutDistance(kilometers, isMetric);
    }

    /**
     * Formats workout distance and converts to the correct unit based on the input parameter.
     *
     * @param kilometers The workout distance in kilometers
     * @param metric     Which type to format this to (miles/kilometers)
     * @return The formatted string
     */
    public static String formatWorkoutDistance(float kilometers, boolean metric) {
        int distancePrecision = DataMatrix.getInt(SettingKeys.Experimental.DATA_DISTANCE_PRECISION_INT);

        // round the same way as gymkit which takes in meters
        kilometers = (float)(Math.floor(kilometers * 1000) / 1000);

        if (metric) {
            // In metric, convert to metric
            return FloorFormat.format(kilometers, distancePrecision);
        } else {
            // In metric, convert to imperial
            float miles = DistanceConvertUtil.convertKilometersToMiles(kilometers);
            return FloorFormat.format(miles, distancePrecision);
        }
    }

    /**
     * Formats workout distance and converts to the correct unit based on the input parameter.
     *
     * @param kilometers The workout distance in kilometers
     * @param metric     Which type to format this to (miles/kilometers)
     * @return The formatted string
     */
    public static String formatWorkoutDistanceGoal(float kilometers, boolean metric) {
        int distancePrecision = DataMatrix.getInt(SettingKeys.Experimental.DATA_DISTANCE_PRECISION_INT);

        // round the same way as gymkit which takes in meters
        // Round to the nearest meter
        kilometers = (int) ((kilometers) * 1000f) / 1000f;
        if (metric) {
            // Round to the nearest 0.01
            return BasicNumberFormat.format(kilometers, distancePrecision);
        } else {
            // In metric, convert to imperial
            float miles = DistanceConvertUtil.convertKilometersToMiles(kilometers);
            return BasicNumberFormat.format(miles, distancePrecision);
        }
    }
}
