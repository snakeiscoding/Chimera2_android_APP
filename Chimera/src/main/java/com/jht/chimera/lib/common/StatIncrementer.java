package com.jht.chimera.lib.common;

import android.view.MotionEvent;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.lib.common.config.ConsoleType;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.data.RangeInfo;
import com.jht.datamatrixlib.DataMatrix;

public class StatIncrementer {
    private static final float TOLERANCE = 0.0005f;
    private final String mCurrentValueKey;
    private final String mKeyToSet;
    private final boolean mIncreaseValue;
    private final RangeInfo mRangeInfo;
    private final IStatIncremented mCallback;

    private int mMaxChanges = 300; // Default this to 300 changes in case it gets stuck somehow
    private int mCurrentChanges = 0;
    private float mConversionFactor = 1;

    public static int TYPE_FLOAT = 0;
    public static int TYPE_INT = 1;

    private final int mType;
    // To avoid debouncing on a flaky touch screen, we wat to disable the increment from time to time to avoid
    // letting the user blow past the 1.0 mph limit.  However we only want to use the debounce timer if
    // we have a limit on the press and hold.  Don't want this if we have a flaky touch screen and we are
    // pressing and holding the speed down.
    private boolean disableIncrement = false;
    private final PeriodicTimer holdTimer = new PeriodicTimer(900, 100, this::changeValue);
    private final JHTTimer debounceTimer = new JHTTimer(20, () -> disableIncrement = false);

    /**
     * Initializes the stat incrementer
     *
     * @param currentValueKey The key that represents the current value
     * @param keyToIncrement  The key to set
     * @param increaseValue   Whether to increase or decrease the value
     * @param rangeInfo       The range
     * @param type            What type of value to get
     * @param statCallback    The stat callback
     */
    public StatIncrementer(String currentValueKey, String keyToIncrement, boolean increaseValue, RangeInfo rangeInfo, int type, IStatIncremented statCallback) {
        mCurrentValueKey = currentValueKey;
        mKeyToSet = keyToIncrement;
        mIncreaseValue = increaseValue;
        mRangeInfo = rangeInfo;
        mCallback = statCallback;
        mType = type;
    }

    /**
     * Sets the maximum number of times the button can be incremented
     *
     * @param maxChanges The max number of increments
     */
    public void setMaxChanges(int maxChanges) {
        mMaxChanges = maxChanges;
    }

    public void setConversionFactor(float conversionFactor) {
        mConversionFactor = conversionFactor;
    }

    /**
     * Stops the stat incrementer
     */
    public void stop() {
        stopHoldTimer();
    }

    public interface IStatIncremented {
        void changeEventSending();
    }

    private boolean handleFlakyTouchScreen() {
        return mMaxChanges != 0 &&
            (ProductConfig.getConsoleType().equals(ConsoleType.chimera_22) ||
            ProductConfig.getConsoleType().equals(ConsoleType.chimera2_22));
    }

    /**
     * Called when a motion even action occurs.
     *
     * @param action The action that occurred
     */
    public void onTouchEvent(int action) {
        if (action == MotionEvent.ACTION_DOWN) {
            debounceTimer.stop();
            if (!disableIncrement) {
                changeValue();
                holdTimer.reset();

                if (handleFlakyTouchScreen()) {
                    disableIncrement = true;
                }
            }
        } else if (action == MotionEvent.ACTION_UP) {
            resetHoldTimer();
        } else if (action == MotionEvent.ACTION_CANCEL) {
            resetHoldTimer();
        }
    }

    public float currentValue() {
        if (mType == TYPE_FLOAT) return DataMatrix.getFloat(mCurrentValueKey) / mConversionFactor;
        else if (mType == TYPE_INT) return DataMatrix.getInt(mCurrentValueKey) / mConversionFactor;
        return -1;
    }

    public Float changeValue() {
        float currentValue = currentValue();

        if (currentValue != -1) {

            // increment/decrement the value
            float newValue;
            boolean valueChanged = false;
            mCurrentChanges++;
            if (mIncreaseValue) {
                newValue = currentValue + mRangeInfo.stepSize;
                if (newValue <= mRangeInfo.getMax() + TOLERANCE) {
                    valueChanged = true;
                } else if (currentValue < mRangeInfo.getMax()) {
                    newValue = mRangeInfo.getMax();
                    valueChanged = true;
                }
            } else {
                newValue = currentValue - mRangeInfo.stepSize;
                if (newValue >= mRangeInfo.getMin() - TOLERANCE) {
                    valueChanged = true;
                } else if (currentValue > mRangeInfo.getMin()) {
                    newValue = mRangeInfo.getMin();
                    valueChanged = true;
                }
            }

            if (valueChanged) {
                if (mCallback != null) mCallback.changeEventSending();
                if (mType == TYPE_FLOAT) DataMatrix.putFloat(mKeyToSet, newValue * mConversionFactor);
                else if (mType == TYPE_INT) DataMatrix.putInt(mKeyToSet, Math.round(newValue * mConversionFactor));
            }

            if (mMaxChanges > 0 && mCurrentChanges >= mMaxChanges) {
                holdTimer.stop();
            }

            if (valueChanged) {
                return newValue;
            }
        }
        return null;
    }

    private void resetHoldTimer() {
        stopHoldTimer();
        if (handleFlakyTouchScreen()) {
            debounceTimer.start(100, 0, "StatIncDebounce");
        }
    }
    private void stopHoldTimer() {
        mCurrentChanges = 0;
        holdTimer.stop();
        debounceTimer.stop();
    }
}
