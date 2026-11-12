package com.jht.chimera.lib.common.conversionlib;

/**
 * Created by chris.foyer on 8/2/2016.
 */
public class PowerConvertUtil
{
    public static final int WATTS_ID = 0;
    public static final int KILOCALORIES_PER_HOUR_ID = 1;

    /**
     * Get the converted value of a double as a new type.
     *
     * @param value       The value to convert
     * @param currentType The current unit of the value
     * @param newType     The requested unit to be converted to
     * @return The converted value
     */
    public static float convert(float value, int currentType, int newType)
    {
        return value * getConversionFactor(currentType) / getConversionFactor(newType);
    }

    /**
     * Convenience method to convert Watts to KiloCalories per hour
     *
     * @param watts The distance in kilometers
     * @return value in miles
     */
    public static double convertWattsToKiloCaloriesPerHour(float watts)
    {
        return convert(watts, WATTS_ID, KILOCALORIES_PER_HOUR_ID);
    }

    // Each factor is the number of watts in the unit, base unit is watts for all conversions
    private static final float WATTS_FACTOR = 1f;
    private static final float KILOCALORIES_PER_HOUR_FACTOR = 1.163f;

    private static float getConversionFactor(int id)
    {
        switch (id)
        {
            case WATTS_ID:
                return WATTS_FACTOR;
            case KILOCALORIES_PER_HOUR_ID:
                return KILOCALORIES_PER_HOUR_FACTOR;
            default:
                throw new RuntimeException("Type " + id + " does not exist in the conversion util.");
        }
    }
}
