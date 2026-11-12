package com.jht.chimera.lib.common.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

public class FolderContentUpdate implements Parcelable {
    public String uid = "";
    public String name = "";

    public FolderContentUpdate() {}

    public FolderContentUpdate(Parcel in) {
        uid = in.readString();
        name = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(name);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<FolderContentUpdate> CREATOR = new Parcelable.Creator<FolderContentUpdate>() {
        @Override
        public FolderContentUpdate createFromParcel(Parcel in) {
            return new FolderContentUpdate(in);
        }

        @Override
        public FolderContentUpdate[] newArray(int size) {
            return new FolderContentUpdate[size];
        }
    };
}