package com.jht.chimera.lib.common.stats;


import android.os.Parcel;
import android.os.Parcelable;

public class StatsPeaks implements Parcelable {
    // peak speed
    public float peakSpeedKph;
    // peak watts
    public float peakWatts;
    // peak heart rate
    public int peakHeartRate;
    // peak incline
    public float peakIncline;
    // peak resistance
    public int peakResistance;
    // peak mets
    public float peakMets;
    // peak rpm
    public int peakRpm;
    // peak steps per minute
    public int peakStepsPerMinute;

    public StatsPeaks() {
    }

    protected StatsPeaks(Parcel in) {
        peakSpeedKph = in.readFloat();
        peakWatts = in.readFloat();
        peakHeartRate = in.readInt();
        peakIncline = in.readFloat();
        peakResistance = in.readInt();
        peakMets = in.readFloat();
        peakRpm = in.readInt();
        peakStepsPerMinute = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(peakSpeedKph);
        dest.writeFloat(peakWatts);
        dest.writeInt(peakHeartRate);
        dest.writeFloat(peakIncline);
        dest.writeInt(peakResistance);
        dest.writeFloat(peakMets);
        dest.writeInt(peakRpm);
        dest.writeInt(peakStepsPerMinute);
    }

    @SuppressWarnings("unused")
    public static final Creator<StatsPeaks> CREATOR = new Creator<StatsPeaks>() {
        @Override
        public StatsPeaks createFromParcel(Parcel in) {
            return new StatsPeaks(in);
        }

        @Override
        public StatsPeaks[] newArray(int size) {
            return new StatsPeaks[size];
        }
    };

    @Override
    public String toString() {
        return "StatsPeaks{" +
                "peakSpeedKph=" + peakSpeedKph +
                ", peakWatts=" + peakWatts +
                ", peakHeartRate=" + peakHeartRate +
                ", peakIncline=" + peakIncline +
                ", peakResistance=" + peakResistance +
                ", peakMets=" + peakMets +
                ", peakRpm=" + peakRpm +
                ", peakStepsPerMinute=" + peakStepsPerMinute +
                '}';
    }
}
