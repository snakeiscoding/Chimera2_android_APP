package com.jht.chimera.lib.workout;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.UUID;

public class XmlWorkoutEntry implements Parcelable {
    public UUID id;
    public String xmlString;

    public XmlWorkoutEntry(UUID id, String xmlString) {
        this.id = id;
        this.xmlString = xmlString;
    }

    protected XmlWorkoutEntry(Parcel in) {
        long msb = in.readLong();
        long lsb = in.readLong();
        id = new UUID(msb, lsb);
        xmlString = in.readString();
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
        dest.writeString(xmlString);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<XmlWorkoutEntry> CREATOR = new Creator<XmlWorkoutEntry>() {
        @Override
        public XmlWorkoutEntry createFromParcel(Parcel in) {
            return new XmlWorkoutEntry(in);
        }

        @Override
        public XmlWorkoutEntry[] newArray(int size) {
            return new XmlWorkoutEntry[size];
        }
    };
}
