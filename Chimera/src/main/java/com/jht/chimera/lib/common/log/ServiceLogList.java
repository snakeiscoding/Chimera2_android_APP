package com.jht.chimera.lib.common.log;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class ServiceLogList implements Parcelable {
    public ArrayList<ServiceLog> serviceLogList = new ArrayList<>();

    public ServiceLogList(ServiceLog[] array) {
        if(array == null) return;
        for (ServiceLog serviceLog : array) {
            serviceLogList.add(serviceLog);
        }
    }

    public ServiceLogList(Parcel in) {
        in.readList(serviceLogList, ServiceLog.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(serviceLogList);
    }

    @SuppressWarnings("unused")
    public static final Creator<ServiceLogList> CREATOR = new Creator<ServiceLogList>() {
        @Override
        public ServiceLogList createFromParcel(Parcel in) {
            return new ServiceLogList(in);
        }

        @Override
        public ServiceLogList[] newArray(int size) {
            return new ServiceLogList[size];
        }
    };

    @Override
    public String toString() {
        return "ServiceLogList{" +
                "serviceLogList=" + serviceLogList +
                '}';
    }
}
