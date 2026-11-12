package com.jht.chimera.lib.workout.generator;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.workoutcontrollerlib.model.DataModel;

import java.util.ArrayList;
import java.util.List;

public class BaseSprint8Config implements Parcelable {
    int type = -1;
    List<Sprint8Interval> intervals = new ArrayList<>();
    List<DataModel> warmupValues = null;

    BaseSprint8Config()
    {
    }

    protected BaseSprint8Config(Parcel in) {
        type = in.readInt();
        intervals = in.createTypedArrayList(Sprint8Interval.CREATOR);
        warmupValues = in.createTypedArrayList(DataModel.CREATOR);
    }

    public static final Creator<BaseSprint8Config> CREATOR = new Creator<BaseSprint8Config>() {
        @Override
        public BaseSprint8Config createFromParcel(Parcel in) {
            return new BaseSprint8Config(in);
        }

        @Override
        public BaseSprint8Config[] newArray(int size) {
            return new BaseSprint8Config[size];
        }
    };

    void setWarmupValues(List<DataModel> warmupValues)
    {
        this.warmupValues = warmupValues;
    }

    void addSprint8Interval(Sprint8Interval interval)
    {
        intervals.add(interval);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeTypedList(intervals);
        dest.writeTypedList(warmupValues);
    }
}
