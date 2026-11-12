package com.jht.chimera.lib.common.stats;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class that represents one or multiple phases of stats. Contains accumulated stats during the
 * phase, average stats during the phase, peak stats during the phase, as well as splits and lap
 * information. It is a parcelable so it can be passed into the DataMatrix.
 * <p>
 * There will likely be four of these classes, one for each phase (warmup, workout, cooldown) and
 * one for the total overall stats in the workout.
 */
public class WorkoutSummary implements Parcelable
{
    public StatsAccumulated statsAccumulated = new StatsAccumulated();
    public StatsAverages statsAverages = new StatsAverages();
    public StatsPeaks statsPeaks = new StatsPeaks();
    public StatsSplits statsSplits = new StatsSplits();
    public StatsSprint8 statsSprint8 = new StatsSprint8();

    public WorkoutSummary()
    {
    }

    public WorkoutSummary(Parcel in)
    {
        statsAccumulated = (StatsAccumulated) in.readValue(StatsAccumulated.class.getClassLoader());
        statsAverages = (StatsAverages) in.readValue(StatsAverages.class.getClassLoader());
        statsPeaks = (StatsPeaks) in.readValue(StatsPeaks.class.getClassLoader());
        statsSplits = (StatsSplits) in.readValue(StatsSplits.class.getClassLoader());
        statsSprint8 = (StatsSprint8) in.readValue(StatsSprint8.class.getClassLoader());
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeValue(statsAccumulated);
        dest.writeValue(statsAverages);
        dest.writeValue(statsPeaks);
        dest.writeValue(statsSplits);
        dest.writeValue(statsSprint8);
    }

    @SuppressWarnings("unused")
    public static final Creator<WorkoutSummary> CREATOR = new Creator<WorkoutSummary>()
    {
        @Override
        public WorkoutSummary createFromParcel(Parcel in)
        {
            return new WorkoutSummary(in);
        }

        @Override
        public WorkoutSummary[] newArray(int size)
        {
            return new WorkoutSummary[size];
        }
    };

    @Override
    public String toString()
    {
        return "WorkoutSummary{" +
                "statsAccumulated=" + statsAccumulated +
                ", statsAverages=" + statsAverages +
                ", statsPeaks=" + statsPeaks +
                ", statsSplits=" + statsSplits +
                ", statsSprint8=" + statsSprint8 +
                '}';
    }
}
