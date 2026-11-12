package com.jht.chimera.lib.graph.workoutgraph;

import com.jht.data.DataPoint;

/**
 * Contains the history and the future profile information for a single stat
 *
 * Created by chris.foyer on 2/7/2018.
 */

@SuppressWarnings("WeakerAccess")
public class ChannelDataSource
{
    private DataPoint[] mFutureData;
    private DataPoint[] mHistoryData;

    private float mMinValue;
    private float mMaxValue;

    private boolean mDataChanged = false;

    public ChannelDataSource(float mMinValue, float mMaxValue)
    {
        setRange(mMinValue, mMaxValue);
    }

    /**
     * Sets the range of the data the graph can display
     *
     * @param minValue The minimum value the graph can display
     * @param maxValue The maximum value the graph can display
     */
    public void setRange(float minValue, float maxValue)
    {
        mMinValue = minValue;
        mMaxValue = maxValue;
    }

    /**
     * Gets the minimum value for the channel
     *
     * @return The minimum value for the channel
     */
    public float getMin()
    {
        return mMinValue;
    }

    /**
     * Gets the maximum value for the channel
     *
     * @return The maximum value for the channel
     */
    public float getMax()
    {
        return mMaxValue;
    }

    /**
     * Sets the data that will be displayed for the history. History data can leave out frames where
     * there is no change to the value that is being tracked. Data points should tell when a change
     * occurs (frame) and the value it changes to. History data also includes the current data.
     *
     * @param historyData The future profile data
     */
    public void setHistoryProfileData(DataPoint[] historyData)
    {
        mHistoryData = historyData;
        mDataChanged = true;
    }

    /**
     * Sets the data that will be displayed for the future profile. Future profile data can leave
     * out frames where there are not changes to the value. Data points should tell when a change
     * occurs (frame) and the value it changes to.
     *
     * @param futureData The future profile data
     */
    public void setFutureProfileData(DataPoint[] futureData)
    {
        mFutureData = futureData;
        mDataChanged = true;
    }

    /**
     * Gets the history profile data
     *
     * @return The history profile data
     */
    public DataPoint[] getHistoryData()
    {
        return mHistoryData;
    }

    /**
     * Gets the future profile data
     *
     * @return The future profile data
     */
    public DataPoint[] getFutureData()
    {
        return mFutureData;
    }

    /**
     * Resets the changed flag
     */
    public void resetChanged()
    {
        mDataChanged = false;
    }

    public boolean isDataChanged()
    {
        return mDataChanged;
    }
}
