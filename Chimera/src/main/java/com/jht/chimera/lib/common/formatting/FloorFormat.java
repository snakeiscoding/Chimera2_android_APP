package com.jht.chimera.lib.common.formatting;

import com.jht.androidcommonalgorithms.math.Resolution;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gattserver2.WatchState;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Formats distances in kilometers (float) to be passed to the stat bar for display
 * <p>
 */
public class FloorFormat {
    private int mPrecision;

    /**
     * Formats a float value to a set precision.
     *
     * @param value     The value to format
     * @param precision The precision of the value
     * @return The formatted value
     */
    public static String format(Float value, int precision) {
        DecimalFormat format = Resolution.getDecimalFormat(1.0 / Math.pow(10, precision));
        format.setRoundingMode(RoundingMode.FLOOR);
        return format.format(value);
    }

    public static String formatAvgHr(Float value, int precision) {
        DecimalFormat format = Resolution.getDecimalFormat(1.0 / Math.pow(10, precision));
        format.setRoundingMode(RoundingMode.FLOOR);
        if(DataMatrix.getInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal()) == WatchState.Connected.ordinal()){
            format.setRoundingMode(RoundingMode.HALF_UP);
        }
        return format.format(value);
    }

    public FloorFormat(int precision) {
        mPrecision = precision;
    }

    public String format(Float value) {
        return format(value, mPrecision);
    }
}
