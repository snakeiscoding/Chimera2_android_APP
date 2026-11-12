package com.jht.chimera.lib.common.conversionlib;

import static com.jht.chimera.lib.common.conversionlib.ConversionHelper.round;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gattserver2.WatchState;

public class SpeedConvertUtil {

    public static final int KILOMETERS_PER_HOUR_ID = 0;
    public static final int MILES_PER_HOUR_ID = 1;

    /**
     * Get the converted value of a float as a new type.
     *
     * @param value       The value to convert
     * @param currentType The current unit of the value
     * @param newType     The requested unit to be converted to
     * @return the converted value
     */
    public static float convert(float value, int currentType, int newType) {
        return value * getConversionFactor(currentType) / getConversionFactor(newType);
    }

    /**
     * Convenience method for converting KPH to MPH
     *
     * @param kph The value in kph to convert
     * @return The value converted to mph
     */
    public static float convertKphToMph(float kph) {
        return convert(kph, KILOMETERS_PER_HOUR_ID, MILES_PER_HOUR_ID);
    }

    public static float convertMphToKph(float mph) {
        return convert(mph, MILES_PER_HOUR_ID, KILOMETERS_PER_HOUR_ID);
    }

    public static int convertKphPaceToMphPace(float seconds) {
        return Math.round(convert(seconds, MILES_PER_HOUR_ID, KILOMETERS_PER_HOUR_ID));
    }

    public static int getPaceInSeconds(float kph, boolean paceIsMetric) {

        float speed;
        // for display purposes, we need to round to the decimal place shown
        if (DataMatrix.getInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal()) != WatchState.Connected.ordinal()) {
            speed = paceIsMetric ? kph : convertKphToMph(kph);
            speed = round(speed, 1);
        } else {
            // We are connected to a Samsung watch. Use their calculation and base it off the
            // data we sent them.
            speed = paceIsMetric ? round(kph, 2) : convertKphToMph(fixKPH(kph, 2));
        }

        if (speed < 0.1f) {
            return 0;
        }

        return (int) (3600 / speed);
    }

    // Each factor is the number of kilometers per hour in the unit, base unit is kilograms for all conversions
    public static final float KILOMETERS_PER_HOUR_FACTOR = 1;
    public static final float MILES_PER_HOUR_FACTOR = 1.609344f;

    private static float getConversionFactor(int id) {
        switch (id) {
            case KILOMETERS_PER_HOUR_ID:
                return KILOMETERS_PER_HOUR_FACTOR;
            case MILES_PER_HOUR_ID:
                return MILES_PER_HOUR_FACTOR;
            default:
                throw new RuntimeException("Type " + id + " does not exist in the conversion util.");
        }
    }

    /**
     * If we are passing metric, but the user is viewing everything in imperial we need to back
     * calculate the desired kph so the mph shows up correctly.  For example lets say we are on a
     * treadmill set to 0.5 mph.  The kph value will be 0.8045...  If we send this with two decimal
     * places we will send 0.80, but that will result in a mph < 0.5.  Since watches floor this value
     * the user will see that they went 0.4 mph.  That is confusing.  This function fixes this by determining
     * that we should send 0.81 because that will get properly converted to 0.5 mph.  Note speed is always
     * displayed as 1 decimal.
     *
     * @param kph       The current speed.
     * @param precision The precision we are using to send the kph.
     * @return The kph to get the true equivalent for the desired mph.
     */
    public static float fixKPH(float kph, int precision) {
        float desiredMPH = ConversionHelper.round(SpeedConvertUtil.convertKphToMph(kph), 1);
        return ConversionHelper.roundCeil(SpeedConvertUtil.convertMphToKph(desiredMPH), precision);
    }

    public static double bikeWattsToMPH(double watts) {
        if (watts - 13.0 > 0) {
            return Math.pow((watts - 13.0f) * 52.63f, 0.33333f);
        } else {
            return Math.pow((watts/13.0f) * 52.63f, 0.33333);
        }
    }
}
