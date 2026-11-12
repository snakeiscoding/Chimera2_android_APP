package com.jht.chimera.lib.common.log;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;

public class ServiceLog implements Parcelable {
    public int serviceType;
    public double accumulatedDistanceKm;
    public double accumulatedFloor;
    public int accumulatedTimeSec;
    public String message;
    public Calendar logTime;

    public ServiceLog() {
        serviceType = 0;
        accumulatedDistanceKm = 0;
        accumulatedTimeSec = 0;
        accumulatedFloor = 0;
        message = "";
        logTime = Calendar.getInstance();
    }


    public ServiceLog(Parcel source) {
        serviceType = source.readInt();
        accumulatedDistanceKm = source.readDouble();
        accumulatedTimeSec = source.readInt();
        accumulatedFloor = source.readDouble();
        message = source.readString();

        logTime = Calendar.getInstance();
        logTime.set(source.readInt(), source.readInt(), source.readInt(), source.readInt(), source.readInt(), source.readInt());
    }

    public static final Creator<ServiceLog> CREATOR = new Creator<ServiceLog>() {
        public ServiceLog createFromParcel(Parcel in) {
            return new ServiceLog(in);
        }

        public ServiceLog[] newArray(int size) {
            return new ServiceLog[size];
        }
    };


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(serviceType);
        dest.writeDouble(accumulatedDistanceKm);
        dest.writeInt(accumulatedTimeSec);
        dest.writeDouble(accumulatedFloor);
        dest.writeString(message);

        dest.writeInt(logTime.get(Calendar.YEAR));
        dest.writeInt(logTime.get(Calendar.MONTH));
        dest.writeInt(logTime.get(Calendar.DATE));
        dest.writeInt(logTime.get(Calendar.HOUR_OF_DAY));
        dest.writeInt(logTime.get(Calendar.MINUTE));
        dest.writeInt(logTime.get(Calendar.SECOND));

    }

    @Override
    public String toString() {
        return "ServiceLog{" +
                "serviceType=" + serviceType +
                "message=" + message +
                ", accumulatedDistanceKm=" + accumulatedDistanceKm +
                ", accumulatedTimeSec=" + accumulatedTimeSec +
                ", accumulatedFloor=" + accumulatedFloor +
                '}';
    }
}
