package com.jht.chimera.lib.common.stats;

import android.os.Parcel;
import android.os.Parcelable;

public class StatsAverages implements Parcelable {
    // average speed 
    public float averageSpeedKph;
    public transient RunningAverageHelper averageSpeedKphHelper = new RunningAverageHelper();
    // average watts 
    public float averageWatts;
    public transient RunningAverageHelper averageWattsHelper = new RunningAverageHelper();
    // average heart rate 
    public float averageHeartRate;
    public transient RunningAverageHelper averageHeartRateHelper = new RunningAverageHelper();
    // average incline 
    public float averageIncline;
    public transient RunningAverageHelper averageInclineHelper = new RunningAverageHelper();
    // average resistance 
    public float averageResistance;
    public transient RunningAverageHelper averageResistanceHelper = new RunningAverageHelper();
    // average mets 
    public float averageMets;
    public transient RunningAverageHelper averageMetsHelper = new RunningAverageHelper();
    // average rpm 
    public float averageRpm;
    public transient RunningAverageHelper averageRpmHelper = new RunningAverageHelper();
    // average steps per minute
    public float averageStepsPerMinute;
    public transient RunningAverageHelper averageStepsPerMinuteHelper = new RunningAverageHelper();

    public float averageStrideLengthMeters;
    public transient RunningAverageHelper averageStrideLengthHelper = new RunningAverageHelper();

    public StatsAverages() {
    }

    protected StatsAverages(Parcel in) {
        averageSpeedKph = in.readFloat();
        averageWatts = in.readFloat();
        averageHeartRate = in.readFloat();
        averageIncline = in.readFloat();
        averageResistance = in.readFloat();
        averageMets = in.readFloat();
        averageRpm = in.readFloat();
        averageStepsPerMinute = in.readFloat();
        averageStrideLengthMeters = in.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(averageSpeedKph);
        dest.writeFloat(averageWatts);
        dest.writeFloat(averageHeartRate);
        dest.writeFloat(averageIncline);
        dest.writeFloat(averageResistance);
        dest.writeFloat(averageMets);
        dest.writeFloat(averageRpm);
        dest.writeFloat(averageStepsPerMinute);
        dest.writeFloat(averageStrideLengthMeters);
    }

    @SuppressWarnings("unused")
    public static final Creator<StatsAverages> CREATOR = new Creator<StatsAverages>() {
        @Override
        public StatsAverages createFromParcel(Parcel in) {
            return new StatsAverages(in);
        }

        @Override
        public StatsAverages[] newArray(int size) {
            return new StatsAverages[size];
        }
    };

    @Override
    public String toString() {
        return "StatsAverages{" +
                "averageSpeedKph=" + averageSpeedKph +
                ", averageWatts=" + averageWatts +
                ", averageHeartRate=" + averageHeartRate +
                ", averageIncline=" + averageIncline +
                ", averageResistance=" + averageResistance +
                ", averageMets=" + averageMets +
                ", averageRpm=" + averageRpm +
                ", averageStepsPerMinute=" + averageStepsPerMinute +
                ", averageStrideLength=" + averageStrideLengthMeters +
                '}';
    }
}
