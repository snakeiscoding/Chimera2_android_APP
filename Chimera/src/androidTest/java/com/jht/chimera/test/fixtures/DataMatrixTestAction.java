package com.jht.chimera.test.fixtures;

import com.jht.datamatrixlib.DataMatrix;

/**
 * Extends the test event for data matrix keys.
 */
public class DataMatrixTestAction extends TestEventBase<Object> {

    /**
     * Create an event.
     *
     * @param timing    Time of the event.
     * @param variable  Variable to check or modify.
     */
    public DataMatrixTestAction(int timing, Object variable) {
        super(timing, variable, new Object(), 0.0f);
    }

    /**
     * It is time to inject the event.  Take the variable and update it to the value given.
     */
    @Override
    public void inject() {
        DataMatrix.sendEvent(variable);
    }

    /**
     *
     * @return  The current value as is stored in the data matrix.
     */
    @Override
    protected Object getValue() {
        return null;
    }

}
