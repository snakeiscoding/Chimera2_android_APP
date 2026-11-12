package com.jht.chimera.lib.integrations.myzone;

import android.os.Parcel;
import android.os.Parcelable;

public class TimeInZones implements Parcelable {

    private final int[] timeInZone;
    private final int[] mepsInZone;

    public TimeInZones(int[] timeInZone, int[] mepsInZone) {
        this.timeInZone = timeInZone;
        this.mepsInZone = mepsInZone;
    }

    protected TimeInZones(Parcel in) {
        timeInZone = new int[in.readInt()];
        for (int i = 0; i < timeInZone.length; i++) {
            timeInZone[i] = in.readInt();
        }
        mepsInZone = new int[in.readInt()];
        for (int i = 0; i < mepsInZone.length; i++) {
            mepsInZone[i] = in.readInt();
        }
    }

    public int[] timeInZone() {
        return timeInZone;
    }

    public int[] mepsInZone() {
        return mepsInZone;
    }

    public static final Creator<TimeInZones> CREATOR = new Creator<TimeInZones>() {
        @Override
        public TimeInZones createFromParcel(Parcel in) {
            return new TimeInZones(in);
        }

        @Override
        public TimeInZones[] newArray(int size) {
            return new TimeInZones[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(timeInZone.length);
        for (int i : timeInZone) {
            dest.writeInt(i);
        }
        dest.writeInt(mepsInZone.length);
        for (int i : mepsInZone) {
            dest.writeInt(i);
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Time in Zones:\n\t");
        for (int j : timeInZone) {
            stringBuilder.append(j).append(", ");
        }
        stringBuilder.append("\nMeps in Zones:\n\t");
        for (int j : mepsInZone) {
            stringBuilder.append(j).append(", ");
        }
        return stringBuilder.toString();
    }
}
