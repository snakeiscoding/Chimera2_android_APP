package com.jht.chimera.lib.landmark;

import android.os.Parcel;
import android.os.Parcelable;

public class LandmarkSettings implements Parcelable {

    public int level = 1;
    public int weight = 150;
    public Landmark landmark = null;

    public LandmarkSettings() {
    }

    public static final Parcelable.Creator<LandmarkSettings> CREATOR = new Creator<LandmarkSettings>() {

        @Override
        public LandmarkSettings createFromParcel(Parcel source) {
            new LandmarkSettings(source);
            return null;
        }

        @Override
        public LandmarkSettings[] newArray(int size) {
            return new LandmarkSettings[size];
        }

    };

    @Override
    public int describeContents() {
        return 0;
    }

    public LandmarkSettings(Parcel source) {
        readFromParcel(source);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(level);
        dest.writeInt(weight);
        dest.writeParcelable(landmark, 0);
    }

    private void readFromParcel(Parcel source) {
        level = source.readInt();
        weight = source.readInt();
        landmark = source.readParcelable(Landmark.class.getClassLoader());
    }
}
