package com.jht.data;

/** Straight move of the old RangeInfo class. Will have to investigate a proper Kotlin conversion */
public class RangeInfo extends ValueRange {
    private float min = 0;
    private float max = 0;
    public float defaultValue = 0;
    public float stepSize = 0;
    public int precision = 0;

    private float minModel = 0;
    private float maxModel = 0;

    /**
     * Constructor
     */
    public RangeInfo() {
        super(0, 0);
    }

    /** Return true if range is zero, that is both min and max are zero, which is what default constructor does */
    public boolean isZero() {
        return this.min == 0 && this.max == 0;
    }

    /**
     * Copies values from another RangeInfo
     * @param other the RangeInfo to copy from
     */
    public void copy(RangeInfo other) {
        min = other.min;
        max = other.max;
        defaultValue = other.defaultValue;
        stepSize = other.stepSize;
        precision = other.precision;
        updateRange(other.minModel, other.maxModel);

    }

    /**
     * Returns a float value that is clamped between the min and max
     * @param value the float to clamp
     * @return the value after clamp (min and max) applied
     */
    public float clamp(float value) {
        float clampedValue = value;
        clampedValue = Math.min(clampedValue, max);
        clampedValue = Math.max(clampedValue, min);
        return clampedValue;
    }

    /**
     * Returns an int value that is clamped between the min and max
     * @param value the int to clamp
     * @return the value after clamp (min and max) applied
     */
    public int clamp(int value) {
        int clampedValue = value;
        clampedValue = (int)Math.min(clampedValue, max);
        clampedValue = (int)Math.max(clampedValue, min);
        return clampedValue;
    }

    /**
     * Returns a value that is clamped between the minModel and maxModel
     * @param value the value to clamp
     * @return the value after clamp (min and max) applied
     */
    public int clampModel(int value) {
        int clampedValue = value;
        clampedValue = (int)Math.min(clampedValue, maxModel);
        clampedValue = (int)Math.max(clampedValue, minModel);
        return clampedValue;
    }

    /**
     * returns the closest stepSize to the provided value
     * @param value the value to find the closest stepSize of
     * @return the closest stepSize to the provided value
     */
    public float getClosestStepValue(float value) {
        if (value > max) {
            return max;
        } else if (value < min) {
            return min;
        } else {
            return Math.round(value / stepSize) * stepSize;
        }
    }

    /**
     * Make sure the max, min, and default are in the correct range.
     */
    public void fix() {
        if(max > maxModel) {
            max = maxModel;
        }
        if(min < minModel) {
            min = minModel;
        }
        defaultValue = clamp(defaultValue);
    }

    /**
     * gets the minimum
     * @return the minimum for this range
     */
    public float getMin() {
        return min;
    }

    /**
     * sets the minimum
     * @param min the minimum for this range
     * @return the value which was just set as the minimum
     */
    public float setMin(float min) {
        this.min = min;
        return min;
    }

    /**
     * gets the maximum
     * @return the maximum for this range
     */
    public float getMax() {
        return max;
    }

    /**
     * sets the maximum
     * @param max the maximum for this range
     * @return the value which was just set as the maximum
     */
    public float setMax(float max) {
        this.max = max;
        return max;
    }

    /**
     * gets the minModel
     * @return minModel, the minimum that the range can be adjusted to
     */
    public float getMinModel() {
        return minModel;
    }

    /**
     * sets the minModel
     * @param minModel the minimum that the range can be adjusted to
     * @return minModel, the value which was just set
     */
    public float setMinModel(float minModel) {
        updateRange(minModel, maxModel);
        return minModel;
    }

    /**
     * gets the maxModel
     * @return maxModel, the maximum that the range can be adjusted to
     */
    public float getMaxModel() {
        return maxModel;
    }

    /**
     * sets the maxModel
     * @param maxModel the maximum that the range can be adjusted to
     * @return maxModel, the value which was just set
     */
    public float setMaxModel(float maxModel) {
        updateRange(minModel, maxModel);
        return maxModel;
    }

    /**
     * updates the min, minModel, max, and maxModel values
     * @param min the value to set min and minModel to
     * @param max the value to set max and maxModel to
     */
    @Override
    public void updateRange(double min, double max) {
        super.updateRange(min, max);
        minModel = (float)min;
        maxModel = (float)max;
    }
}
