package com.jht.chimera.lib.common.dapi;

import android.os.Parcel;
import android.os.Parcelable;

import com.jht.datamatrixlib.DataMatrix;
import com.jht.datamatrixlib.adapter.ParcelableAdapter;


public class DapiGetSoftware {

    static DataMatrix sDataMatrix = new DataMatrix();
    public final static String ACTION_GET_LATEST_SW_STRING = "action_get_latest_sw_string"; //NON-NLS
    public final static String EVENT_GET_LATEST_SW_RESPONSE_PARCEL = "event_get_latest_sw_response_parcel"; //NON-NLS

    public interface OnGetLatestSoftwareListener {
        void onGetLatestSoftware(DapiGetLatestSoftwareResponse response);
    }

    static ParcelableAdapter sGetLatestSoftwareCallback = null;

    public static void getLatestSoftware(String softwareType, final OnGetLatestSoftwareListener listener) {
        sGetLatestSoftwareCallback = sDataMatrix.createParcelableAdapter(EVENT_GET_LATEST_SW_RESPONSE_PARCEL, DapiGetLatestSoftwareResponse.class, response -> {
            if (listener != null) {
                listener.onGetLatestSoftware(response);
            }
            if (sGetLatestSoftwareCallback != null) {
                sDataMatrix.deregister(sGetLatestSoftwareCallback);
            }
        });

        sDataMatrix.putString(ACTION_GET_LATEST_SW_STRING, softwareType);
    }

    public static class DapiGetLatestSoftwareResponse implements Parcelable {
        public int dapiResponse;
        public String softwareType;
        public String version;
        public String url;
        public String md5;

        public DapiGetLatestSoftwareResponse() {
        }

        public DapiGetLatestSoftwareResponse(Parcel in) {
            dapiResponse = in.readInt();
            softwareType = in.readString();
            version = in.readString();
            url = in.readString();
            md5 = in.readString();
        }

        public static final Creator<DapiGetLatestSoftwareResponse> CREATOR = new Creator<DapiGetLatestSoftwareResponse>() {
            @Override
            public DapiGetLatestSoftwareResponse createFromParcel(Parcel in) {
                return new DapiGetLatestSoftwareResponse(in);
            }

            @Override
            public DapiGetLatestSoftwareResponse[] newArray(int size) {
                return new DapiGetLatestSoftwareResponse[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(dapiResponse);
            dest.writeString(softwareType);
            dest.writeString(version);
            dest.writeString(url);
            dest.writeString(md5);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("dapiResponse = " + dapiResponse);
            sb.append(" | softwareType = " + softwareType);
            sb.append(" | version = " + version);
            sb.append(" | url = " + url);
            sb.append(" | md5 = " + md5);
            return sb.toString();
        }
    }
}
