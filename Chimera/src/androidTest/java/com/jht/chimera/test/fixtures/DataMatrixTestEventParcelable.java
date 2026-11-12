package com.jht.chimera.test.fixtures;

import android.os.Parcelable;

import com.jht.datamatrixlib.DataMatrix;

/**
 * Extends the test event for data matrix keys.
 * @param <T>   The type for the data matrix key.
 */
public class DataMatrixTestEventParcelable<T extends Parcelable> extends TestEventBase<T> {

    /**
     * Create an event.
     *
     * @param timing    Time of the event.
     * @param variable  Variable to check or modify.
     * @param value     Value to set or compare against.
     */
    public DataMatrixTestEventParcelable(int timing, Object variable, T value) {
        super(timing, variable, value, 0.0f);
    }

    /**
     * It is time to inject the event.  Take the variable and update it to the value given.
     */
    @Override
    public void inject() {
        DataMatrix.putParcelable(variable, value);
    }

    /**
     *
     * @return  The current value as is stored in the data matrix.
     */
    @Override
    protected Object getValue() {
        return DataMatrix.getParcelable(variable, value.getClass());
    }

}
