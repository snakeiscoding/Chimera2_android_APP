package com.jht.chimera.lib.common;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a change in the workout future for the graph
 *
 * Created by chris.foyer on 9/13/2017.
 */
public class ProfileValue implements Parcelable
{
    public float timeMs;
    public float value;

    protected ProfileValue(Parcel in)
    {
        timeMs = in.readFloat();
        value = in.readFloat();
    }

    public ProfileValue(float timeMs, float value)
    {
        this.timeMs = timeMs;
        this.value = value;
    }

    public static final Creator<ProfileValue> CREATOR = new Creator<ProfileValue>()
    {
        @Override
        public ProfileValue createFromParcel(Parcel in)
        {
            return new ProfileValue(in);
        }

        @Override
        public ProfileValue[] newArray(int size)
        {
            return new ProfileValue[size];
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
        dest.writeFloat(timeMs);
        dest.writeFloat(value);
    }
}
