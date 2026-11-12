package com.jht.chimera.lib.common.math;

public class AverageHelper {

    private float[] mValues;
    private int mIndex;
    private float mAverage;
    private int mCounter;

    public AverageHelper(int size) {
        mValues = new float[size];
    }

    public void reset() {
        for (int i = 0; i < mValues.length; i++) {
            mValues[i] = 0;
        }
        mIndex = 0;
        mAverage = 0f;
        mCounter = 0;
    }

    public float addValue(float value) {

        // if the count is less than the max size
        if (mCounter < mValues.length) {
            // need to compute the average since we don't have the max number of entries
            mCounter++;
            mValues[mIndex] = value;
            mAverage = 0f;
            for (int i = 0; i < mCounter; i++) {
                mAverage += mValues[i] / (double) mCounter;
            }

            // if we now have enough entries, we can store averages
            // to optimize computing the averages
            if (mCounter == mValues.length) {
                for (int i = 0; i < mCounter; i++) {
                    mValues[i] /= (double) mCounter;
                }
            }
        } else {
            // optimize computing of the averages
            mAverage -= mValues[mIndex];
            mValues[mIndex] = value / mValues.length;
            mAverage += mValues[mIndex];
        }

        mIndex++;
        if (mIndex >= (int) mValues.length) {
            mIndex = 0;
        }

        return mAverage;
    }

    public float getValue() {
        return mAverage;
    }
}
