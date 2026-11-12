package com.jht.chimera.ui.mainapp.workoutsummary;

/**
 * Contains the information for a single line of the workout summary
 *
 * Created by chris.foyer on 1/11/2018.
 */
@SuppressWarnings("WeakerAccess")
public class SummaryRowItem
{
    private String mLabel;
    private String mWarmUp;
    private String mWorkout;
    private String mCoolDown;
    private String mTotal;
    private boolean mDivider;

    public String getLabel()
    {
        return mLabel;
    }

    public void setLabel(String label)
    {
        this.mLabel = label;
    }

    public String getWarmUp()
    {
        return mWarmUp;
    }

    public void setWarmUp(String warmUp)
    {
        this.mWarmUp = warmUp;
    }

    public String getWorkout()
    {
        return mWorkout;
    }

    public void setWorkout(String workout)
    {
        this.mWorkout = workout;
    }

    public String getCoolDown()
    {
        return mCoolDown;
    }

    public void setCoolDown(String coolDown)
    {
        this.mCoolDown = coolDown;
    }

    public String getTotal()
    {
        return mTotal;
    }

    public void setTotal(String total)
    {
        this.mTotal = total;
    }

    public boolean isDivider()
    {
        return mDivider;
    }

    public void setDivider(boolean divider)
    {
        this.mDivider = divider;
    }
}
