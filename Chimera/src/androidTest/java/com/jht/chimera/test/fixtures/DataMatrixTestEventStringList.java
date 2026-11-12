package com.jht.chimera.test.fixtures;

import com.jht.datamatrixlib.DataMatrix;

import java.util.List;

/**
 * Extends the test event for data matrix keys.
 */
public class DataMatrixTestEventStringList extends TestEventBase<List<String>> {

    /**
     * Create an event.
     *
     * @param timing    Time of the event.
     * @param variable  Variable to check or modify.
     * @param value     Value to set or compare against.
     */
    public DataMatrixTestEventStringList(int timing, Object variable, List<String> value) {
        super(timing, variable, value, 0.0f);
    }

    /**
     * It is time to inject the event.  Take the variable and update it to the value given.
     */
    @Override
    public void inject() {
        DataMatrix.putStringList(variable, value);
    }

    /**
     * 
     * @return  The current value as is stored in the data matrix.
     */
    @Override
    protected Object getValue() {
        return DataMatrix.getStringList(variable);
    }

}
