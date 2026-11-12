package com.jht.chimera.lib.common.formatting;

import com.jht.androidcommonalgorithms.math.Resolution;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Formats distances in kilometers (float) to be passed to the stat bar for display
 * <p>
 * Created by chris.foyer on 8/23/2017.
 */
public class BasicNumberFormat {
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
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    public BasicNumberFormat(int precision) {
        mPrecision = precision;
    }

    public String format(Float value) {
        return format(value, mPrecision);
    }
}
