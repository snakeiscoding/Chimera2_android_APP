package com.jht.chimera.lib.common.formatting;

import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.datamatrixlib.DataMatrix;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formats speeds in kph (float) to be passed to the stat bar for display
 * <p>
 */
public class SpeedFormat {
    /**
     * Formats a speed value based on what the current value for metric vs imperial setting is in the settings keys.
     *
     * @param kmPerHour The kmPerHour value
     * @return The formatted string value
     */
    public static String formatSpeedToUserSetting(float kmPerHour) {
        boolean metric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
        return formatSpeed(kmPerHour, metric);
    }

    /**
     * Formats a speed value in the specified format
     *
     * @param kmPerHour The km per hour to convert
     * @param metric    The metric unit
     * @return The formatted value
     */
    public static String formatSpeed(float kmPerHour, boolean metric) {
        DecimalFormat format = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.ENGLISH)); // force dot decimal for all locales
        format.setRoundingMode(RoundingMode.HALF_UP);

        if (metric) {
            return format.format(kmPerHour);
        } else {
            return format.format(SpeedConvertUtil.convertKphToMph(kmPerHour));
        }
    }

    /**
     * Formats a pace value based on the input speed. Gets the pace for the current unit for the console.
     *
     * @param speedKph The speed in kph
     * @return The formatted pace
     */
    public static String formatPace(float speedKph) {
        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
        return formatPace(speedKph, isMetric);
    }

    /**
     * Formats a pace value based on the input speed.
     *
     * @param speedKph The speed in kph
     * @param metric   Whether it is metric vs imperial
     * @return The formatted pace
     */
    public static String formatPace(float speedKph, boolean metric) {
        return TimeFormat.formatTimeSeconds(SpeedConvertUtil.getPaceInSeconds(speedKph, metric));
    }
}
