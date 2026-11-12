package com.jht.chimera.lib.workout.workoutlist;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.UUID;

//import androidx.annotation.NotNull;

/**
 * Class that contains information retrieved from the workout database
 */
public class WorkoutEntry implements Parcelable {
    public String scriptString;
    public String appId;
    public int workoutId;
    public UUID callerId = null; // Passed back to improve multithreading capability for the workout controller

    public WorkoutEntry(int workoutId, String appId, String scriptString) {
        this.scriptString = scriptString;
        this.appId = appId;
        this.workoutId = workoutId;
    }

    protected WorkoutEntry(Parcel in) {
        scriptString = in.readString();
        appId = in.readString();
        workoutId = in.readInt();
        long msb = in.readLong();
        long lsb = in.readLong();
        callerId = new UUID(msb, lsb);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(scriptString);
        dest.writeString(appId);
        dest.writeInt(workoutId);
        if (callerId == null) {
            dest.writeLong(0);
            dest.writeLong(0);
        } else {
            dest.writeLong(callerId.getMostSignificantBits());
            dest.writeLong(callerId.getLeastSignificantBits());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WorkoutEntry> CREATOR = new Creator<WorkoutEntry>() {
        @Override
        public WorkoutEntry createFromParcel(Parcel in) {
            return new WorkoutEntry(in);
        }

        @Override
        public WorkoutEntry[] newArray(int size) {
            return new WorkoutEntry[size];
        }
    };

    @NonNull
    @Override
    public String toString()
    {
        if (scriptString != null) {
            return scriptString;
        }
        else {
            return "bad_entry!";
        }
    }
}
