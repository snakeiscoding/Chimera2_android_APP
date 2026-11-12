package com.jht.chimera.lib.common.stats;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class StatsSprint8 implements Parcelable {

    public ArrayList<Float> sweatScores = new ArrayList<>();

    public StatsSprint8() {
    }

    protected StatsSprint8(Parcel in) {
        in.readList(sweatScores, Float.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(sweatScores);
    }

    @SuppressWarnings("unused")
    public static final Creator<StatsSprint8> CREATOR = new Creator<StatsSprint8>() {
        @Override
        public StatsSprint8 createFromParcel(Parcel in) {
            return new StatsSprint8(in);
        }

        @Override
        public StatsSprint8[] newArray(int size) {
            return new StatsSprint8[size];
        }
    };

    @Override
    public String toString() {
        return "StatsSprint8{" +
                "sweatScores=" + sweatScores +
                '}';
    }

    public float getCurrentSweatScore() {
        if (sweatScores.size() > 0) {
            return sweatScores.get(sweatScores.size() - 1);
        }
        return 0;
    }
}
