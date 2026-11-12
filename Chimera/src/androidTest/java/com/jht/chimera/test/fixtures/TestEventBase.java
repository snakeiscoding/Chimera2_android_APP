package com.jht.chimera.test.fixtures;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.string.StringUtil;

import java.util.Locale;

/**
 * Helper class for unit tests where we need to inject system events and check the system
 * at certain points in the test.  This is common for workout tests where we need to modify the system
 * based on the workout time and also check the system data.
 * @param <T>   Data type for the event.
 */
public abstract class  TestEventBase<T> implements Comparable<TestEventBase<?>> {
    protected final int timing;         // Time for the event.  Determines on the child class, but in general this wil be ms.
    protected final Object variable;    // The variable to look for.  In general this is the DataMatrix key.
    protected final T value;            // The expected value for checks and value to inject for setting system data.
    protected final float tolerance;    // Tolerance for floating point math checks.

    /**
     * Create an event.
     *
     * @param timing    Time of the event.
     * @param variable  Variable to check or modify.
     * @param value     Value to set or compare against.
     */
    public TestEventBase(int timing, @NonNull Object variable, @NonNull T value) {
        this(timing, variable, value, 0.0f);
    }

    /**
     * Create an event.
     *
     * @param timing    Time of the event.
     * @param variable  Variable to check or modify.
     * @param value     Value to set or compare against.
     * @param tolerance Used to compare floating point values.
     */
    public TestEventBase(int timing, @NonNull Object variable, @NonNull T value, float tolerance) {
        this.timing = timing;
        this.variable = variable;
        this.value = value;
        this.tolerance = tolerance;
    }

    /**
     * It is time to inject the event.  Take the variable and update it to the value given.
     */
    abstract public void inject();

    /**
     *
     * @return  The current value in the system.
     */
    abstract protected @Nullable Object getValue();

    /**
     * Check the system to make sure the value is the expected value.
     *
     * @return  Null if the check succeeds.  Error message if the check fails.
     */
    public @Nullable String check() {
        Object realValue = getValue();
        if(value instanceof Float && realValue instanceof Float) {
            if (Math.abs((Float) value - (Float)realValue) > tolerance) {
                return String.format(Locale.ENGLISH, "Checking %s expected %f, but got %f at %d", variable.toString(), (Float)value, (Float)realValue, timing);
            }
        }
        else if(value instanceof Boolean && realValue instanceof Boolean) {
            if (((Boolean)value).booleanValue() != (Boolean)realValue) {
                return String.format(Locale.ENGLISH, "Checking %s expected %s, but got %s at %d", variable.toString(), (Boolean)value ? "true" : "false", (Boolean)realValue ? "true" : "false", timing);
            }
        }
        else if(value instanceof Integer && realValue instanceof Integer) {
            if (((Integer)value).intValue() != (Integer)realValue) {
                return String.format(Locale.ENGLISH, "Checking %s expected %d, but got %d at %d", variable.toString(), (Integer)value, (Integer)realValue, timing);
            }
        }
        else if(value instanceof String && realValue instanceof String) {
            if (!value.equals(realValue)) {
                return String.format(Locale.ENGLISH, "Checking %s expected %s, but got %s at %d", variable.toString(), (String)value, (String)realValue, timing);
            }
        }
        else if(value instanceof byte[] && realValue instanceof byte[]) {

            String realValueStr = StringUtil.byteArray2Hex((byte[])realValue);
            String expectedValueStr = StringUtil.byteArray2Hex((byte[])value);

            if(!realValueStr.equals(expectedValueStr)) {
                return String.format(Locale.ENGLISH, "Checking %s expected %s, but got %s at %d", variable.toString(), expectedValueStr, realValueStr, timing);
            }
        }
        else if(value instanceof Double && realValue instanceof Double) {
            if (Math.abs((Double) value - (Double)realValue) > tolerance) {
                return String.format(Locale.ENGLISH, "Checking %s expected %f, but got %f at %d", variable.toString(), (Double)value, (Double)realValue, timing);
            }
        }
        else if(value instanceof Long && realValue instanceof Long) {
            if (((Long)value).longValue() != (Long)realValue) {
                return String.format(Locale.ENGLISH, "Checking %s expected %d, but got %d at %d", variable.toString(), (Long)value, (Long)realValue, timing);
            }
        }
        return null;
    }

    // Getters for the event data.
    public int timing() { return timing; }


    /**
     * Allows us to sort the list based on the time.  This speeds up the check of the events
     * while running the workout.
     *
     * @param o The event to compare to.
     * @return  -1 if we are earlier, 0 if the same time, 1 if the event input is earlier.
     */
    @Override
    public int compareTo(TestEventBase o) {
        if(timing < o.timing()) {
            return -1;
        }
        else if(timing > o.timing()) {
            return 1;
        }
        return 0;
    }
}
