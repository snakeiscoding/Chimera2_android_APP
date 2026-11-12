package com.jht.chimera.lib.ptp;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class HeartRateZones implements Parcelable {
    private int regenerativeMin = -1;
    private int regenerativeMax = -1;
    private int basicExtensiveMax = -1;
    private int basicIntensiveMax = -1;
    private int developmentMax = -1;
    private int peakMax = -1;

    public HeartRateZones() {}
    public HeartRateZones(Parcel parcel) { readFromParcel(parcel); }

    public static final Creator<HeartRateZones> CREATOR = new Creator<HeartRateZones>() {
        @Override
        public HeartRateZones createFromParcel(Parcel parcel) { return new HeartRateZones(parcel); }

        @Override
        public HeartRateZones[] newArray(int size) { return new HeartRateZones[size]; }
    };

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ////  Parcelable Methods
    ///////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(regenerativeMin);
        dest.writeInt(regenerativeMax);
        dest.writeInt(basicExtensiveMax);
        dest.writeInt(basicIntensiveMax);
        dest.writeInt(developmentMax);
        dest.writeInt(peakMax);
    }

    public void readFromParcel(Parcel parcel) {
        regenerativeMin = parcel.readInt();
        regenerativeMax = parcel.readInt();
        basicExtensiveMax = parcel.readInt();
        basicIntensiveMax = parcel.readInt();
        developmentMax = parcel.readInt();
        peakMax = parcel.readInt();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ////  Overrides
    ///////////////////////////////////////////////////////////////////////////////////////////////
    @NonNull
    @Override
    public String toString() {
        return "regenerativeMin: " + regenerativeMin + ", regenerativeMax: " + regenerativeMax
                + ", basicExtensiveMax: " + basicExtensiveMax + ", basicIntensiveMax: " + basicIntensiveMax
                + ", developmentMax: " + developmentMax + ", peakMax: " + peakMax;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || object.getClass() != HeartRateZones.class) return false;
        HeartRateZones heartRateZonesObj = (HeartRateZones) object;
        return heartRateZonesObj.getRegenerativeMin() == regenerativeMin
                && heartRateZonesObj.getRegenerativeMax() == regenerativeMax
                && heartRateZonesObj.getBasicExtensiveMax() == basicExtensiveMax
                && heartRateZonesObj.getBasicIntensiveMax() == basicIntensiveMax
                && heartRateZonesObj.getDevelopmentMax() == developmentMax
                && heartRateZonesObj.getPeakMax() == peakMax;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ////  Additional Methods
    ///////////////////////////////////////////////////////////////////////////////////////////////
    public boolean isValid() {
        return regenerativeMin >= 0
                && regenerativeMax > regenerativeMin
                && basicExtensiveMax > regenerativeMax
                && basicIntensiveMax > basicExtensiveMax
                && developmentMax > basicIntensiveMax
                && peakMax > developmentMax;
    }

    public List<String> toList() {
        ArrayList<String> list = new ArrayList<>();
        list.add(regenerativeMin + " - " + regenerativeMax);
        list.add(regenerativeMax + " - " + basicExtensiveMax);
        list.add(basicExtensiveMax + " - " + basicIntensiveMax);
        list.add(basicIntensiveMax + " - " + developmentMax);
        list.add(developmentMax + " - " + peakMax);
        return list;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    ////  Getters and Setters - used while parsing associated JSON fields
    ///////////////////////////////////////////////////////////////////////////////////////////////
    public int getRegenerativeMin() { return regenerativeMin; }
    public void setRegenerativeMin(int regenerativeMin) { this.regenerativeMin = regenerativeMin; }
    public int getRegenerativeMax() { return regenerativeMax; }
    public void setRegenerativeMax(int regenerativeMax) { this.regenerativeMax = regenerativeMax; }
    public int getBasicExtensiveMax() { return basicExtensiveMax; }
    public void setBasicExtensiveMax(int basicExtensiveMax) { this.basicExtensiveMax = basicExtensiveMax; }
    public int getBasicIntensiveMax() { return basicIntensiveMax; }
    public void setBasicIntensiveMax(int basicIntensiveMax) { this.basicIntensiveMax = basicIntensiveMax; }
    public int getDevelopmentMax() { return developmentMax; }
    public void setDevelopmentMax(int developmentMax) { this.developmentMax = developmentMax; }
    public int getPeakMax() { return peakMax; }
    public void setPeakMax(int peakMax) { this.peakMax = peakMax; }
}