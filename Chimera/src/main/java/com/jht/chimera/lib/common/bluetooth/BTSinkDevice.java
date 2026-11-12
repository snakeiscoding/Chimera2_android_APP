package com.jht.chimera.lib.common.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

public class BTSinkDevice {
    private static final String TAG = "BTSinkDevice";
    public static void sendDevice(String name, byte[] address) {
        Log.e(TAG, "sendDevice. name: " + name);
        BtDeviceParcelable btDeviceCommandParcelable = new BtDeviceParcelable();
        btDeviceCommandParcelable.name = name;
        btDeviceCommandParcelable.bytes = address;
        DataMatrix.putParcelable(HALKeys.EVENT_BT_SINK_DEVICE_FOUND_PARCELABLE, btDeviceCommandParcelable);
    }

    public static class BtDeviceParcelable implements Parcelable {
        public String name;
        public byte[] bytes = new byte[0];

        @Override
        public int describeContents() {
            return 0;
        }

        public BtDeviceParcelable() {
        }

        public BtDeviceParcelable(Parcel in) {
            readFromParcel(in);
        }

        public static final Creator<BtDeviceParcelable> CREATOR = new Creator<BtDeviceParcelable>() {
            @Override
            public BtDeviceParcelable createFromParcel(Parcel source) {
                return new BtDeviceParcelable(source);
            }

            @Override
            public BtDeviceParcelable[] newArray(int size) {
                return new BtDeviceParcelable[size];
            }
        };

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
            dest.writeInt(bytes.length);
            dest.writeByteArray(bytes);
        }

        public void readFromParcel(Parcel source) {
            name = source.readString();
            int byteArraySize = source.readInt();
            bytes = new byte[byteArraySize];
            source.readByteArray(bytes);
        }
    }
}
