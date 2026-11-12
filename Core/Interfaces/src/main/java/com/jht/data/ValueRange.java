package com.jht.data;


import androidx.annotation.NonNull;

/** Straight copy of the old ValueRange class from com.jht.androidcommonalgorithms.math.ValueRange. Will have to investigate a proper Kotlin conversion */
public class ValueRange {
    private double min;
    private double max;

    /**
     * Default constructor.
     * @param min       The minimum value.
     * @param max       The maximum value.
     */
    public ValueRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Return a value that is within the allowable range.
     * @param value     The value to check.
     * @return          The value clamped to the range.
     */
    public double clamp(double value) {
        return Math.min(Math.max(value, min), max);
    }

    /**
     * Get the percent this value is within the range.
     *
     * @param value     The value to convert.
     * @return          The percentage this is within the range.
     */
    public double getPercent(double value) {
        return (value - min) / (max - min);
    }

    /**
     * This returns a value based on teh percentage input.
     *
     * @param percent     The percentage
     * @return          The value based on teh percentage input.
     */
    public double percentToValue(double percent) {
        return percent * (max - min) + min;
    }


    /**
     * Interpolates the value input to a value in the range input.
     *
     * @param value	This is the value to convert.
     */
    public double interpolate (double value, @NonNull ValueRange range)
    {
        return range.percentToValue(getPercent(value));
    }


    /**
     * Inverse of interpolation.  For example if this range is 0 - 100 and the input range is 0 - 100
     * and we pass it a value of 75 then this outputs 25.
     *
     * @param value	This is the value to convert.
     */
    public double interpolateI(double value, @NonNull ValueRange range)
    {
        return range.percentToValue(1 - getPercent(value));
    }

    public void updateRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    double min() { return min; }
    double max() { return max; }

}
