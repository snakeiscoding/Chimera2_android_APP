package com.jht.chimera.lib.common.datamatrixlists;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;

public class WorkoutTrackingSnapshot implements Parcelable {
    public static final int PARCEL_SIZE = 19 * 4 + 4;
    public static final DataMatrixListKeys KEYS =
        new DataMatrixListKeys(
            WorkoutKeys.EVENT_GET_SNAPSHOT_HISTORY,
            WorkoutKeys.ACTION_NUM_SNAPSHOTS,
            WorkoutKeys.ACTION_SNAPSHOT_HISTORY_RESPONSE_PARCELABLE,
            WorkoutKeys.DATA_WORKOUT_SNAPSHOT_PARCELABLE
        );

    public float distance; // km
    public int resistance;
    public int heartRate;
    public int watts;
    public int workoutState; // 0=warmup, 1=workout, 2=cooldown
    public int timeInState; // time in seconds
    public int lapIndex; // 1 based index, current lap index
    public int splitIndex; // 1 based index, current split index
    public float kph;
    public float incline;
    public float totalCalories;
    public float mets;
    public int rpm;
    public int targetHeartRate;
    public int targetWatts;
    public float totalFloors;
    public int totalSteps;
    public float elevationMeters;
    public int spm;

    WorkoutTrackingSnapshot(Parcel in) {

        distance = in.readFloat();
        resistance = in.readInt();
        heartRate = in.readInt();
        watts = in.readInt();
        workoutState = in.readInt();
        timeInState = in.readInt();
        lapIndex = in.readInt();
        splitIndex = in.readInt();
        kph = in.readFloat();
        incline = in.readFloat();
        totalCalories = in.readFloat();
        mets = in.readFloat();
        rpm = in.readInt();
        targetHeartRate = in.readInt();
        targetWatts = in.readInt();
        totalFloors = in.readFloat();
        totalSteps = in.readInt();
        elevationMeters = in.readFloat();
        spm = in.readInt();
    }

    public WorkoutTrackingSnapshot() {}

    public static final Creator<WorkoutTrackingSnapshot> CREATOR = new Creator<WorkoutTrackingSnapshot>() {
        @Override
        public WorkoutTrackingSnapshot createFromParcel(Parcel in) {
            return new WorkoutTrackingSnapshot(in);
        }

        @Override
        public WorkoutTrackingSnapshot[] newArray(int size) {
            return new WorkoutTrackingSnapshot[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(distance);
        dest.writeInt(resistance);
        dest.writeInt(heartRate);
        dest.writeInt(watts);
        dest.writeInt(workoutState);
        dest.writeInt(timeInState);
        dest.writeInt(lapIndex);
        dest.writeInt(splitIndex);
        dest.writeFloat(kph);
        dest.writeFloat(incline);
        dest.writeFloat(totalCalories);
        dest.writeFloat(mets);
        dest.writeInt(rpm);
        dest.writeInt(targetHeartRate);
        dest.writeInt(targetWatts);
        dest.writeFloat(totalFloors);
        dest.writeInt(totalSteps);
        dest.writeFloat(elevationMeters);
        dest.writeInt(spm);
    }

    @NonNull
    @Override
    public String toString() {
        return
            "distance " + distance + ", " +
            "resistance " + resistance + ", " +
            "heartRate " + heartRate + ", " +
            "watts " + watts + ", " +
            "workoutState " + workoutState + ", " +
            "timeInState " + timeInState + ", " +
            "lapIndex " + lapIndex + ", " +
            "splitIndex " + splitIndex + ", " +
            "kph " + kph + ", " +
            "incline " + incline + ", " +
            "totalCalories " + totalCalories + ", " +
            "mets " + mets + ", " +
            "rpm " + rpm + ", " +
            "targetHeartRate " + targetHeartRate + ", " +
            "targetWatts " + targetWatts + ", " +
            "totalFloors " + totalFloors + ", " +
            "totalSteps " + totalSteps + ", " +
            "elevationMeters " + elevationMeters + ", " +
            "spm " + spm;
    }
}
