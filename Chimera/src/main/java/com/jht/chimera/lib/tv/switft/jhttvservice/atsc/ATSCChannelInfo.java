package com.jht.chimera.lib.tv.switft.jhttvservice.atsc;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by mutombolin on 2018/08/23.
 */

public class ATSCChannelInfo implements Parcelable {

    public int mIndex;
    public String noMajor;
    public String noMinor;
    public String name;
    public EnumATSCServiceType serviceType;
    public boolean favorite;
    public boolean skip;
    public boolean isDefault = false;

    public ATSCChannelInfo() {

    }

    public ATSCChannelInfo(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        noMajor = String.valueOf(buffer.getShort());
        noMinor = String.valueOf(buffer.getShort());
        byte[] value = new byte[33];
        buffer.get(value);
        name = new String(value, Charset.forName("UTF-8"));
        name = name.replaceAll("\u0000.*", "");
        serviceType = EnumATSCServiceType.values()[buffer.get()];
        favorite = buffer.get() == 0x01;
        skip = buffer.get() == 0x01;
    }

    public String getChannelNumber() {
        return ATSCUtils.getInstance().getChannelNumbers(noMajor, noMinor);
    }

    public String getChannelName() {
        return name;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public boolean isSkip() {
        return skip;
    }

    protected ATSCChannelInfo(Parcel in) {
        noMajor = in.readString();
        noMinor = in.readString();
        name = in.readString();
        serviceType = (EnumATSCServiceType) in.readSerializable();
        favorite = in.readInt() == 1;
        skip = in.readInt() == 1;
        isDefault = in.readInt() == 1;
    }

    public static final Creator<ATSCChannelInfo> CREATOR = new Creator<ATSCChannelInfo>() {
        @Override
        public ATSCChannelInfo createFromParcel(Parcel in) {
            return new ATSCChannelInfo(in);
        }

        @Override
        public ATSCChannelInfo[] newArray(int size) {
            return new ATSCChannelInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(noMajor);
        dest.writeString(noMinor);
        dest.writeString(name);
        dest.writeSerializable(serviceType);
        dest.writeInt(favorite ? 1 : 0);
        dest.writeInt(skip ? 1 : 0);
        dest.writeInt(isDefault ? 1 : 0);
    }

    @Override
    public String toString() {
        return "ATSCChannelInfo: " +
                " noMajor = " + noMajor +
                " noMinor = " + noMinor +
                " name = " + name +
                " serviceType = " + serviceType.toString() +
                " favorite = " + favorite +
                " skip = " + skip +
                " isDefault = " + isDefault;
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int index) {
        mIndex = index;
    }
}
