package com.jht.chimera.lib.common;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of changes for a single parameter
 */

public class WorkoutFuture implements Parcelable
{
    public List<WorkoutFutureProfile> profiles = new ArrayList<>();

    protected WorkoutFuture(Parcel in)
    {
        in.readList(profiles, WorkoutFutureProfile.class.getClassLoader());
    }

    public WorkoutFuture(List<WorkoutFutureProfile> profileList)
    {
        profiles = profileList;
    }

    public static boolean isValid(WorkoutFuture future) {
        return future != null && future.profiles != null; // must have a list to be valid
    }
    public static final Creator<WorkoutFuture> CREATOR = new Creator<WorkoutFuture>()
    {
        @Override
        public WorkoutFuture createFromParcel(Parcel in)
        {
            return new WorkoutFuture(in);
        }

        @Override
        public WorkoutFuture[] newArray(int size)
        {
            return new WorkoutFuture[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeList(profiles);
    }
}
