package com.jht.chimera.lib.common.stats;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class WorkoutSummaryList implements Parcelable {

    public ArrayList<WorkoutSummary> workoutSummaries = new ArrayList<>();

    public WorkoutSummaryList() {
    }

    protected WorkoutSummaryList(Parcel in) {
        in.readList(workoutSummaries, WorkoutSummary.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(workoutSummaries);
    }

    @SuppressWarnings("unused")
    public static final Creator<WorkoutSummaryList> CREATOR = new Creator<WorkoutSummaryList>() {
        @Override
        public WorkoutSummaryList createFromParcel(Parcel in) {
            return new WorkoutSummaryList(in);
        }

        @Override
        public WorkoutSummaryList[] newArray(int size) {
            return new WorkoutSummaryList[size];
        }
    };

    @Override
    public String toString() {
        return "WorkoutSummaries{" +
                "workoutSummaries=" + workoutSummaries +
                '}';
    }
}
