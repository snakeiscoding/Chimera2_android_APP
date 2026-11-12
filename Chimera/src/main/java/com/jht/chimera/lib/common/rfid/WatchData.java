package com.jht.chimera.lib.common.rfid;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.rfid.Legic.RFIDChip;

public class WatchData implements Parcelable {
    private RFIDChip chip;
    private byte[] watchData;

    @Override
    public int describeContents() {
        return 0;
    }

    public WatchData(RFIDChip chip, byte[] watchData) {
        this.chip = chip;
        this.watchData = watchData;
    }

    public WatchData(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<WatchData> CREATOR = new Creator<WatchData>() {
        @Override
        public WatchData createFromParcel(Parcel source) {
            return new WatchData(source);
        }

        @Override
        public WatchData[] newArray(int size) {
            return new WatchData[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(chip, 0);
        dest.writeInt(watchData.length);
        for(Byte b : watchData) {
            dest.writeByte(b);
        }
    }

    public void readFromParcel(Parcel source) {
        chip = source.readParcelable(RFIDChip.class.getClassLoader());
        watchData = new byte[source.readInt()];
        for(int i = 0; i < watchData.length; i++) {
            watchData[i] = source.readByte();
        }
    }

    public RFIDChip chip() {
        return chip;
    }

    public byte[] watchData() { return watchData; }
}
