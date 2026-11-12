package com.jht.chimera.lib.common.conversionlib;

/**
 * Handles basic rounding operations
 *
 */
public class ConversionHelper
{
    public static float round(float value, int precision)
    {
        double scale = Math.pow(10, precision);
        return (float) (Math.round(value * scale) / scale);
    }

    public static float roundCeil(float value, int precision)
    {
        double scale = Math.pow(10, precision);
        return (float) (Math.ceil(value * scale) / scale);
    }

    public static float roundFloor(float value, int precision) {
        float scale = (float) Math.pow(10, precision);
        return (float) (Math.floor(value * scale) / scale);
    }
}
