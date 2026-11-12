package com.jht.chimera.lib.common.conversionlib;

/**
 * Class for simplifying distance conversions
 *
 * Created by chris.foyer on 8/2/2016.
 */
public class DistanceConvertUtil
{
    public static final int KILOMETERS_ID = 0;
    public static final int MILES_ID = 1;
    public static final int METERS_ID = 2;
    public static final int YARDS_ID = 3;
    public static final int FEET_ID = 4;
    public static final int INCHES_ID = 5;
    public static final int CENTIMETERS_ID = 6;

    /**
     * Get the converted value of a double as a new type.
     *
     * @param value       The value to convert
     * @param currentType The current unit of the value
     * @param newType     The requested unit to be converted to
     * @return value as new type
     */
    public static float convert(float value, int currentType, int newType)
    {
        return value * getConversionFactor(currentType) / getConversionFactor(newType);
    }

    /**
     * Convenience method to convert Kilometers to Miles
     *
     * @param kilometers The distance in kilometers
     * @return value in miles
     */
    public static float convertKilometersToMiles(float kilometers)
    {
        return convert(kilometers, KILOMETERS_ID, MILES_ID);
    }

    public static float convertMilesToKilometers(float miles)
    {
        return convert(miles, MILES_ID, KILOMETERS_ID);
    }

    public static float convertFeetToMeters(float feet)
    {
        return convert(feet, FEET_ID, METERS_ID);
    }

    public static float convertMetersToFeet(float meter)
    {
        return convert(meter, METERS_ID, FEET_ID);
    }

    public static float convertMetersToYards(float meter)
    {
        return convert(meter, METERS_ID, YARDS_ID);
    }

    public static float convertInchesToCm(float inches)
    {
        return convert(inches, INCHES_ID, CENTIMETERS_ID);
    }

    public static float convertCmToInches(float centimeters)
    {
        return convert(centimeters, CENTIMETERS_ID, INCHES_ID);
    }

    // Each factor is the number of meters in the unit, base unit is meters for all conversions
    private static final float KILOMETER_FACTOR = 1000f;
    private static final float MILES_FACTOR = 1609.344f;
    private static final float METERS_FACTOR = 1f;
    private static final float YARDS_FACTOR = 0.9144f;
    private static final float FEET_FACTOR = 0.3048f;
    private static final float CENTIMETER_FACTOR = 0.010f;
    private static final float INCH_FACTOR = 0.0254f;

    public static float getConversionFactor(int id)
    {
        switch (id)
        {
            case KILOMETERS_ID:
                return KILOMETER_FACTOR;
            case MILES_ID:
                return MILES_FACTOR;
            case METERS_ID:
                return METERS_FACTOR;
            case YARDS_ID:
                return YARDS_FACTOR;
            case FEET_ID:
                return FEET_FACTOR;
            case CENTIMETERS_ID:
                return CENTIMETER_FACTOR;
            case INCHES_ID:
                return INCH_FACTOR;
            default:
                throw new RuntimeException("Type " + id + " does not exist in the conversion util.");
        }
    }
}
