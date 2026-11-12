package com.jht.chimera.ui.mainapp.workouttracking;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the information needed to draw a single history profile line. Gets the list of goal values
 * to use along with the data values. These two lists are not necessarily the same size. The
 * GraphChannelChange keeps track of the index of the goal to use and the goals are found
 * by utilizing that index.
 * <p>
 * To use this class iterate through the points calling {#get(int)} to get the DataPoint
 * associated with the changes in the graph.
 */
public class DistanceMarkers implements Parcelable {
    public List<Integer> mileMarkers = new ArrayList<>();
    public List<Integer> kmMarkers = new ArrayList<>();

    public DistanceMarkers(Parcel in) {

        int size;
        size = in.readInt();
        for(int i = 0; i < size; i++) {
            mileMarkers.add(in.readInt());
        }
        size = in.readInt();
        for(int i = 0; i < size; i++) {
            kmMarkers.add(in.readInt());
        }
    }
    /**
     * Sets up a new distance marker
     *
     */
    public DistanceMarkers() {
    }

    public static final Creator<DistanceMarkers> CREATOR = new Creator<DistanceMarkers>() {
        @Override
        public DistanceMarkers createFromParcel(Parcel in) {
            return new DistanceMarkers(in);
        }

        @Override
        public DistanceMarkers[] newArray(int size) {
            return new DistanceMarkers[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {


        dest.writeInt(mileMarkers.size());
        for (Integer i : mileMarkers) {
            dest.writeInt(i);
        }
        dest.writeInt(kmMarkers.size());
        for (Integer i : kmMarkers) {
            dest.writeInt(i);
        }
    }
}
