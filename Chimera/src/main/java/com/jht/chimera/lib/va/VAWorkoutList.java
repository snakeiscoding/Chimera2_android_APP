package com.jht.chimera.lib.va;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.chimera.service.workout.InitialCondition;
import com.jht.virtualactive.service.datamodels.Workout;

import java.util.ArrayList;
import java.util.List;

/**
 * A parcelable that represents a set of initial conditions required for a virtual active workout
 */
public class VAWorkoutList implements Parcelable {
    public List<Workout> vaWorkouts = new ArrayList<>();

    public VAWorkoutList(Parcel in) {
        in.readList(vaWorkouts, InitialCondition.class.getClassLoader());
    }

    public VAWorkoutList(List<Workout> vaWorkouts) {
        this.vaWorkouts = vaWorkouts;
    }

    public static final Creator<VAWorkoutList> CREATOR = new Creator<VAWorkoutList>() {
        @Override public VAWorkoutList createFromParcel(Parcel in) { return new VAWorkoutList(in); }
        @Override public VAWorkoutList[] newArray(int size) { return new VAWorkoutList[size]; }
    };

    @Override public int describeContents() { return 0; }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(vaWorkouts);
    }
}
