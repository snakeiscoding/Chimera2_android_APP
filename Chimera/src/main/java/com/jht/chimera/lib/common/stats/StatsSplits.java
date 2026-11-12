package com.jht.chimera.lib.common.stats;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class StatsSplits implements Parcelable {

    public ArrayList<WorkoutSplit> workoutSplits = new ArrayList<>();

    public StatsSplits() {
    }

    protected StatsSplits(Parcel in) {
        in.readList(workoutSplits, WorkoutSplit.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(workoutSplits);
    }

    @SuppressWarnings("unused")
    public static final Creator<StatsSplits> CREATOR = new Creator<StatsSplits>() {
        @Override
        public StatsSplits createFromParcel(Parcel in) {
            return new StatsSplits(in);
        }

        @Override
        public StatsSplits[] newArray(int size) {
            return new StatsSplits[size];
        }
    };

    @Override
    public String toString() {
        return "StatsSplits{" +
                "workoutSplits=" + workoutSplits +
                '}';
    }

    public WorkoutSplit getLastSplit() {
        if (workoutSplits.size() <= 0) return null;
        return workoutSplits.get(workoutSplits.size() - 1);
    }
}
