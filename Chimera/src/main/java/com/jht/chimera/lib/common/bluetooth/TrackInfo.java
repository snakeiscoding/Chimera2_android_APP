package com.jht.chimera.lib.common.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

public class TrackInfo implements Parcelable {
    public String uid;
    public String artist;
    public String album;
    public String trackName;
    public int duration;
    public String genre;

    public TrackInfo() {
        this.uid = null;
        this.trackName = null;
        this.album = null;
        this.artist = null;
        this.genre = null;
        this.duration = 0;
    }

    public TrackInfo(Parcel in) {
        this.uid = in.readString();
        this.trackName = in.readString();
        this.album = in.readString();
        this.artist = in.readString();
        this.genre = in.readString();
        this.duration = in.readInt();
    }

    public static final Creator<TrackInfo> CREATOR = new Creator<TrackInfo>() {

        @Override
        public TrackInfo createFromParcel(Parcel source) {
            return new TrackInfo(source);
        }

        @Override
        public TrackInfo[] newArray(int size) {
            return new TrackInfo[size];
        }

    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.uid);
        out.writeString(this.trackName);
        out.writeString(this.album);
        out.writeString(this.artist);
        out.writeString(this.genre);
        out.writeInt(this.duration);
    }

    @Override
    public String toString() {
        return "uid:" + this.uid +
                ",trackName:" + this.trackName +
                ",album:" + this.album +
                ",artist:" + this.artist +
                ",genre:" + this.genre +
                ",duration:" + this.duration;
    }
}
