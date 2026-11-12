package com.jht.chimera.lib.common.stats;


import android.os.Parcel;
import android.os.Parcelable;


public class WorkoutSplit implements Parcelable {
    public int timeMs = 0;
    public int timeStartMs = 0;
    public float distanceKm = 0;
    public float distanceStartKm = 0;

    public WorkoutSplit(int timeStartMs, float distanceStartKm) {
        this.timeStartMs = timeStartMs;
        this.distanceStartKm = distanceStartKm;
    }

    protected WorkoutSplit(Parcel in) {
        timeMs = in.readInt();
        timeStartMs = in.readInt();
        distanceKm = in.readFloat();
        distanceStartKm = in.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(timeMs);
        dest.writeInt(timeStartMs);
        dest.writeFloat(distanceKm);
        dest.writeFloat(distanceStartKm);
    }

    @SuppressWarnings("unused")
    public static final Creator<WorkoutSplit> CREATOR = new Creator<WorkoutSplit>() {
        @Override
        public WorkoutSplit createFromParcel(Parcel in) {
            return new WorkoutSplit(in);
        }

        @Override
        public WorkoutSplit[] newArray(int size) {
            return new WorkoutSplit[size];
        }
    };

    @Override
    public String toString() {
        return "WorkoutSplit{" +
                "timeSec=" + timeMs +
                ", timeStartSec=" + timeStartMs +
                ", distanceKm=" + distanceKm +
                ", distanceStartKm=" + distanceStartKm +
                '}';
    }
}