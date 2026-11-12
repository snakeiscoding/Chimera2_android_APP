package com.jht.chimera.lib.common.tv;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.datamatrixlib.DataMatrix;


public class TvEvent {
    private static final String TAG = "TvEvent";
    public static void sendEvent(byte[] bytes, int length) {
        TvParcelable tvParcelable = new TvParcelable();
        tvParcelable.bytes = bytes;
        tvParcelable.length = length;
        Log.w(TAG, "sendEvent: data = " + length );
        DataMatrix.putParcelable(TVKeys.EVENT_PARSE_CHANNELS_PARCELABLE, tvParcelable);
    }

    public static class TvParcelable implements Parcelable {
        public byte[] bytes = new byte[0];
        public int length;

        @Override
        public int describeContents() {
            return 0;
        }

        public TvParcelable() {
        }

        public TvParcelable(Parcel in) {
            readFromParcel(in);
        }

        public static final Creator<TvParcelable> CREATOR = new Creator<TvParcelable>() {
            @Override
            public TvParcelable createFromParcel(Parcel source) {
                return new TvParcelable(source);
            }

            @Override
            public TvParcelable[] newArray(int size) {
                return new TvParcelable[size];
            }
        };

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(bytes.length);
            dest.writeByteArray(bytes);
            dest.writeInt(length);
        }

        public void readFromParcel(Parcel source) {
            int byteArraySize = source.readInt();
            bytes = new byte[byteArraySize];
            source.readByteArray(bytes);
            length = source.readInt();
        }
    }
}
