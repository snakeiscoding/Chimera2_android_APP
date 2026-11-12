package com.jht.chimera.lib.common.math;

public class RunningAverageHelper {

    private long mCounter;
    private float mAverage;

    public RunningAverageHelper() {
        reset();
    }

    public void reset() {
        mAverage = 0f;
        mCounter = 0;
    }

    public float addValue(float value) {
        mAverage = mAverage * ((float)mCounter / (mCounter + 1f)) +
                value * (1f / (mCounter + 1f));
        mCounter++;
        return mAverage;
    }

    public float getValue() {
        return mAverage;
    }
}
