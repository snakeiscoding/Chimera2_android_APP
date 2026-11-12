package com.jht.data.sprint8;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;

/**
 * Saves the sweat scores for each sprint period during a sprint8 workout.
 */
public class Sprint8SweatScores implements Parcelable {
    public ArrayList<Float> sweatScores = new ArrayList<>();

    public Sprint8SweatScores() {
    }

    protected Sprint8SweatScores(Parcel in) {
        in.readList(sweatScores, Sprint8SweatScores.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(sweatScores);
    }

    public static final Creator<Sprint8SweatScores> CREATOR = new Creator<Sprint8SweatScores>() {
        @Override
        public Sprint8SweatScores createFromParcel(Parcel in) {
            return new Sprint8SweatScores(in);
        }

        @Override
        public Sprint8SweatScores[] newArray(int size) {
            return new Sprint8SweatScores[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "Sprint8SweatScores{" +
                "Sprint8SweatScores=" + sweatScores +
                '}';
    }

    /**
     * Adds a sweat score to the sweat scores list
     *
     * @param sweatScore The value to add
     */
    public void addSweatScore(float sweatScore) {
        sweatScores.add(sweatScore);
    }
}
