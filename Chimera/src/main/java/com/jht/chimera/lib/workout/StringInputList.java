package com.jht.chimera.lib.workout;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.UUID;

/**
 * A parcelable that represents a set of initial conditions required for a workout
 */
public class StringInputList implements Parcelable {
    public UUID id;
    public List<String> initialInputs;

    public StringInputList(UUID id, List<String> inputs) {
        this.id = id;
        initialInputs = inputs;
    }

    protected StringInputList(Parcel in) {
        long msb = in.readLong();
        long lsb = in.readLong();
        id = new UUID(msb, lsb);
        initialInputs = in.createStringArrayList();
    }

    public static final Creator<StringInputList> CREATOR = new Creator<StringInputList>() {
        @Override
        public StringInputList createFromParcel(Parcel in) {
            return new StringInputList(in);
        }

        @Override
        public StringInputList[] newArray(int size) {
            return new StringInputList[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id == null) {
            dest.writeLong(0);
            dest.writeLong(0);
        } else {
            dest.writeLong(id.getMostSignificantBits());
            dest.writeLong(id.getLeastSignificantBits());
        }
        dest.writeStringList(initialInputs);
    }
}
