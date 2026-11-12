package com.jht.chimera.lib.workout.generator;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.workoutcontrollerlib.model.DataModel;

import java.util.List;

public class Sprint8Interval implements Parcelable
{
    List<DataModel> sprintStatements;
    List<DataModel> recoveryStatements;

    public Sprint8Interval(List<DataModel> sprintSets, List<DataModel> recoverySets)
    {
        sprintStatements = sprintSets;
        recoveryStatements = recoverySets;
    }

    protected Sprint8Interval(Parcel in) {
        sprintStatements = in.createTypedArrayList(DataModel.CREATOR);
        recoveryStatements = in.createTypedArrayList(DataModel.CREATOR);
    }

    public static final Creator<Sprint8Interval> CREATOR = new Creator<Sprint8Interval>() {
        @Override
        public Sprint8Interval createFromParcel(Parcel in) {
            return new Sprint8Interval(in);
        }

        @Override
        public Sprint8Interval[] newArray(int size) {
            return new Sprint8Interval[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(sprintStatements);
        dest.writeTypedList(recoveryStatements);
    }
}
