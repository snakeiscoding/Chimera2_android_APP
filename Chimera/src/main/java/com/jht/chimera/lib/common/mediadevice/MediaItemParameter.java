package com.jht.chimera.lib.common.mediadevice;

import android.os.Parcel;
import android.os.Parcelable;

public class MediaItemParameter implements Parcelable {
    public String uid;
    public int attrId;
    public String name;
    public boolean isPlayable;
    public byte[] currentFolderBytes = new byte[8];;

    public MediaItemParameter(String uid, int attrId, String name, boolean isPlayable, byte[] currentFolderBytes) {
        this.uid = uid;
        this.attrId = attrId;
        this.name = name;
        this.isPlayable = isPlayable;
        this.currentFolderBytes = currentFolderBytes;
    }

    public MediaItemParameter(Parcel in) {
        uid = in.readString();
        attrId = in.readInt();
        name = in.readString();
        isPlayable = in.readInt() == 1;
        if(in.dataAvail() > 0)
            in.readByteArray(currentFolderBytes);
    }

    public static final Creator<MediaItemParameter> CREATOR = new Creator<MediaItemParameter>() {
        @Override
        public MediaItemParameter createFromParcel(Parcel in) {
            return new MediaItemParameter(in);
        }

        @Override
        public MediaItemParameter[] newArray(int size) {
            return new MediaItemParameter[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeInt(attrId);
        dest.writeString(name);
        dest.writeInt(isPlayable ? 1 : 0);
        if(currentFolderBytes != null)
            dest.writeByteArray(currentFolderBytes);
    }

}
