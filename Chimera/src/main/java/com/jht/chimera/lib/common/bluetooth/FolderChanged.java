package com.jht.chimera.lib.common.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;


public class FolderChanged implements Parcelable {

    public int uidCount = 0;
    public String folderUid = "";
    public int numItems = 0;

    public FolderChanged() {}

    public FolderChanged(Parcel in) {
        uidCount = in.readInt();
        folderUid = in.readString();
        numItems = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(uidCount);
        dest.writeString(folderUid);
        dest.writeInt(numItems);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<FolderChanged> CREATOR = new Parcelable.Creator<FolderChanged>() {
        @Override
        public FolderChanged createFromParcel(Parcel in) {
            return new FolderChanged(in);
        }

        @Override
        public FolderChanged[] newArray(int size) {
            return new FolderChanged[size];
        }
    };
}
