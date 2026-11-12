package com.jht.chimera.lib.common.stats;

import android.os.Parcel;
import android.os.Parcelable;

public class StatsAccumulated implements Parcelable {
    // total second in run state
    public float secondsStateRun;
    public float distanceKm;
    public float floors;
    public float steps;
    public float calories;
    public float elevationMeters;
    public float activeCalories;
    public float caloriesOffset;
    public float activeCaloriesOffset;
    public float joules;
    public float strideCount;

    public StatsAccumulated() {
    }

    protected StatsAccumulated(Parcel in) {
        secondsStateRun = in.readFloat();
        calories = in.readFloat();
        distanceKm = in.readFloat();
        floors = in.readFloat();
        steps = in.readFloat();
        elevationMeters = in.readFloat();
        activeCalories = in.readFloat();
        caloriesOffset = in.readFloat();
        activeCaloriesOffset = in.readFloat();
        joules = in.readFloat();
        strideCount = in.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(secondsStateRun);
        dest.writeFloat(calories);
        dest.writeFloat(distanceKm);
        dest.writeFloat(floors);
        dest.writeFloat(steps);
        dest.writeFloat(elevationMeters);
        dest.writeFloat(activeCalories);
        dest.writeFloat(caloriesOffset);
        dest.writeFloat(activeCaloriesOffset);
        dest.writeFloat(joules);
        dest.writeFloat(strideCount);
    }

    @SuppressWarnings("unused")
    public static final Creator<StatsAccumulated> CREATOR = new Creator<StatsAccumulated>() {
        @Override
        public StatsAccumulated createFromParcel(Parcel in) {
            return new StatsAccumulated(in);
        }

        @Override
        public StatsAccumulated[] newArray(int size) {
            return new StatsAccumulated[size];
        }
    };

    public StatsAccumulated copy() {
        StatsAccumulated copy = new StatsAccumulated();
        copy.secondsStateRun = secondsStateRun;
        copy.calories = calories;
        copy.distanceKm = distanceKm;
        copy.floors = floors;
        copy.steps = steps;
        copy.elevationMeters = elevationMeters;
        copy.activeCalories = activeCalories;
        copy.caloriesOffset = caloriesOffset;
        copy.activeCaloriesOffset = activeCaloriesOffset;
        copy.joules = joules;
        copy.strideCount = strideCount;
        return copy;
    }

    @Override
    public String toString() {
        return "StatsAccumulated{" +
                "secondsStateRun=" + secondsStateRun +
                ", calories=" + calories +
                ", distanceKm=" + distanceKm +
                ", floors=" + floors +
                ", steps=" + steps +
                ", elevationMeters=" + elevationMeters +
                ", activeCalories=" + activeCalories +
                ", caloriesOffset=" + caloriesOffset +
                ", activeCaloriesOffset=" + activeCaloriesOffset +
                ", joules=" + joules +
                ", strideCount=" + strideCount +
                '}';
    }
}
