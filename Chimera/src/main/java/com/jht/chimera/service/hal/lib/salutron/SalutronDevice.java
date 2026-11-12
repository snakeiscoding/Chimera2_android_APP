package com.jht.chimera.service.hal.lib.salutron;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;


public class SalutronDevice {

    public static void sendDevice(String name, byte[] id, salutronCommands.DeviceType type) {
        SalutronDevice.SalutronDeviceParcelable salutronDeviceParcelable = new SalutronDevice.SalutronDeviceParcelable(name, id, type);
        DataMatrix.putParcelable(HALKeys.EVENT_SALUTRON_DEVICE_FOUND_PARCELABLE, salutronDeviceParcelable);
    }

    public static void sendConnectDevice(String name, byte[] id, salutronCommands.DeviceType type){
        SalutronDevice.SalutronDeviceParcelable salutronDeviceParcelable = new SalutronDevice.SalutronDeviceParcelable(name, id, type);
        DataMatrix.putParcelable(HALKeys.EVENT_SALUTRON_DEVICE_CONNECT_PARCELABLE, salutronDeviceParcelable);
    }

    public static void sendDisConnectDevice(String name, byte[] id, salutronCommands.DeviceType type){
        SalutronDevice.SalutronDeviceParcelable salutronDeviceParcelable = new SalutronDevice.SalutronDeviceParcelable(name, id, type);
        DataMatrix.putParcelable(HALKeys.EVENT_SALUTRON_DEVICE_DISCONNECT_PARCELABLE, salutronDeviceParcelable);
    }

    public static class SalutronDeviceParcelable implements Parcelable {
        public String name;
        public byte[] id = new byte[0];
        public salutronCommands.DeviceType type;

        @Override
        public int describeContents() {
            return 0;
        }

        public SalutronDeviceParcelable() {
        }

        public SalutronDeviceParcelable(String name, byte[] id, salutronCommands.DeviceType type){
            this.name = name;
            this.id = id;
            this.type = type;
        }

        public SalutronDeviceParcelable(Parcel in) {
            readFromParcel(in);
        }

        public static final Creator<SalutronDevice.SalutronDeviceParcelable> CREATOR = new Creator<SalutronDevice.SalutronDeviceParcelable>() {
            @Override
            public SalutronDevice.SalutronDeviceParcelable createFromParcel(Parcel source) {
                return new SalutronDevice.SalutronDeviceParcelable(source);
            }

            @Override
            public SalutronDevice.SalutronDeviceParcelable[] newArray(int size) {
                return new SalutronDevice.SalutronDeviceParcelable[size];
            }
        };

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeInt(id.length);
            dest.writeByteArray(id);
            dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        }

        public void readFromParcel(Parcel source) {
            name = source.readString();
            int byteArraySize = source.readInt();
            id = new byte[byteArraySize];
            source.readByteArray(id);
            type = salutronCommands.DeviceType.values()[source.readInt()];
        }
    }
}
