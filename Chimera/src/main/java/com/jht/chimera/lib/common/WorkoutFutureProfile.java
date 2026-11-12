package com.jht.chimera.lib.common;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of changes for a single parameter
 */
public class WorkoutFutureProfile implements Parcelable
{
    public String parameter;
    public List<ProfileValue> profileValues = new ArrayList<>();

    protected WorkoutFutureProfile(Parcel in)
    {
        parameter = in.readString();
        in.readList(profileValues, ProfileValue.class.getClassLoader());
    }

    public WorkoutFutureProfile(String parameter, List<ProfileValue> profileList)
    {
        this.parameter = parameter;
        profileValues = profileList;
    }

    public static final Parcelable.Creator<WorkoutFutureProfile> CREATOR = new Creator<WorkoutFutureProfile>()
    {
        @Override
        public WorkoutFutureProfile createFromParcel(Parcel in)
        {
            return new WorkoutFutureProfile(in);
        }

        @Override
        public WorkoutFutureProfile[] newArray(int size)
        {
            return new WorkoutFutureProfile[size];
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
        dest.writeString(parameter);
        dest.writeList(profileValues);
    }
}
