package com.jht.chimera.test.fixtures;

import android.os.Parcelable;

import com.jht.datamatrixlib.DataMatrix;

/**
 * Extends the test event for data matrix keys.
 * @param <T>   The type for the data matrix key.
 */
public class DataMatrixTestEvent <T> extends TestEventBase<T> {

    /**
     * Create an event.
     *
     * @param timing    Time of the event.
     * @param variable  Variable to check or modify.
     * @param value     Value to set or compare against.
     */
    public DataMatrixTestEvent(int timing, Object variable, T value) {
        super(timing, variable, value, 0.0f);
    }

    /**
     * Create an event.
     *
     * @param timing    Time of the event.
     * @param variable  Variable to check or modify.
     * @param value     Value to set or compare against.
     * @param tolerance Used to compare floating point values.
     */
    public DataMatrixTestEvent(int timing, Object variable, T value, float tolerance) {
        super(timing, variable, value, tolerance);
    }

    /**
     * It is time to inject the event.  Take the variable and update it to the value given.
     */
    @Override
    public void inject() {
        if(value instanceof Float) {
            DataMatrix.putFloat(variable, (Float)value);
        }
        else if(value instanceof Boolean) {
            DataMatrix.putBoolean(variable, (Boolean)value);
        }
        else if(value instanceof Integer) {
            DataMatrix.putInt(variable, (Integer)value);
        }
        else if(value instanceof String) {
            DataMatrix.putString(variable, (String)value);
        }
        else if(value instanceof Parcelable) {
            DataMatrix.putParcelable(variable, (Parcelable)value);
        }
        else if(value instanceof byte[]) {
            DataMatrix.putBytes(variable, (byte[])value);
        }
        else if(value instanceof Double) {
            DataMatrix.putDouble(variable, (Double)value);
        }
        else if(value instanceof Long) {
            DataMatrix.putLong(variable, (Long)value);
        }
    }

    /**
     *
     * @return  The current value as is stored in the data matrix.
     */
    @Override
    protected Object getValue() {
        if(value instanceof Float) {
            return DataMatrix.getFloat(variable, (Float) value);
        }
        else if(value instanceof Boolean) {
            return DataMatrix.getBoolean(variable, (Boolean) value);
        }
        else if(value instanceof Integer) {
            return DataMatrix.getInt(variable, (Integer) value);
        }
        else if(value instanceof String) {
            return DataMatrix.getString(variable, (String) value);
        }
        else if(value instanceof byte[]) {
            return DataMatrix.getBytes(variable);
        }
        else if(value instanceof Double) {
            return DataMatrix.getDouble(variable, (Double) value);
        }
        else if(value instanceof Long) {
            return DataMatrix.getLong(variable, (Long) value);
        }
        return null;
    }

}
