package com.jht.chimera.lib.common.stats;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class used to pass all the workout summaries together through the DataMatrix. Summaries are the
 * information collected for all workout script stages that match the respective stage name.
 *
 * Created by chris.foyer on 1/12/2018.
 */
@SuppressWarnings("WeakerAccess")
public class SummaryData implements Parcelable
{
    public WorkoutSummary warmupSummary;
    public WorkoutSummary workoutSummary;
    public WorkoutSummary cooldownSummary;
    public WorkoutSummary totalSummary;

    public SummaryData(WorkoutSummary warmupSummary, WorkoutSummary workoutSummary, WorkoutSummary cooldownSummary, WorkoutSummary total)
    {
        this.warmupSummary = warmupSummary;
        this.workoutSummary = workoutSummary;
        this.cooldownSummary = cooldownSummary;
        totalSummary = total;
    }

    protected SummaryData(Parcel in)
    {
        warmupSummary = (WorkoutSummary) in.readValue(WorkoutSummary.class.getClassLoader());
        workoutSummary = (WorkoutSummary) in.readValue(WorkoutSummary.class.getClassLoader());
        cooldownSummary = (WorkoutSummary) in.readValue(WorkoutSummary.class.getClassLoader());
        totalSummary = (WorkoutSummary) in.readValue(WorkoutSummary.class.getClassLoader());
    }

    public static final Creator<SummaryData> CREATOR = new Creator<SummaryData>()
    {
        @Override
        public SummaryData createFromParcel(Parcel in)
        {
            return new SummaryData(in);
        }

        @Override
        public SummaryData[] newArray(int size)
        {
            return new SummaryData[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeValue(warmupSummary);
        dest.writeValue(workoutSummary);
        dest.writeValue(cooldownSummary);
        dest.writeValue(totalSummary);
    }
}
