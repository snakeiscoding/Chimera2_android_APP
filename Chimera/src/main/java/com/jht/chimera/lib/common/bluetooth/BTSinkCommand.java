package com.jht.chimera.lib.common.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;


public class BTSinkCommand {

    public static void sendCommand(int command, byte[] bytes) {
        BtDeviceCommandParcelable btDeviceCommandParcelable = new BtDeviceCommandParcelable();
        btDeviceCommandParcelable.bytes = bytes;
        btDeviceCommandParcelable.type = command;
        DataMatrix.putParcelable(HALKeys.ACTION_SEND_BT_DEVICE_COMMAND_PARCELABLE, btDeviceCommandParcelable);
    }

    public static class BtDeviceCommandParcelable implements Parcelable {
        public byte[] bytes = new byte[0];
        public int type;

        @Override
        public int describeContents() {
            return 0;
        }

        public BtDeviceCommandParcelable() {
        }

        public BtDeviceCommandParcelable(Parcel in) {
            readFromParcel(in);
        }

        public static final Creator<BtDeviceCommandParcelable> CREATOR = new Creator<BtDeviceCommandParcelable>() {
            @Override
            public BtDeviceCommandParcelable createFromParcel(Parcel source) {
                return new BtDeviceCommandParcelable(source);
            }

            @Override
            public BtDeviceCommandParcelable[] newArray(int size) {
                return new BtDeviceCommandParcelable[size];
            }
        };

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(bytes.length);
            dest.writeByteArray(bytes);
            dest.writeInt(type);
        }

        public void readFromParcel(Parcel source) {
            int byteArraySize = source.readInt();
            bytes = new byte[byteArraySize];
            source.readByteArray(bytes);
            type = source.readInt();
        }
    }
}
