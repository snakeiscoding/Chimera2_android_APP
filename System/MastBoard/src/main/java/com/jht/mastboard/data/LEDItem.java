package com.jht.mastboard.data;

import android.os.Parcel;
import android.os.Parcelable;


public class LEDItem implements Parcelable {
    public int color = -1;
    public LedZone ledZone = LedZone.ALL;
    public LEDGroup group = LEDGroup.Default;
    public enum LEDGroup {
        Default,
        WorkoutRunning,
        WorkoutPaused,
        WorkoutSummary,
        Concierge,
    }

    public LEDItem(int color, LedZone ledZone) {
        this.color = color;
        this.ledZone = ledZone;
    }

    public LEDItem(LEDGroup title) {
        this.group = title;
    }

    private LEDItem(Parcel in) {
        color = in.readInt();
        ledZone = LedZone.valueOf(in.readString());
    }

    public static final Creator<LEDItem> CREATOR = new Creator<LEDItem>() {
        @Override
        public LEDItem createFromParcel(Parcel in) {
            return new LEDItem(in);
        }

        @Override
        public LEDItem[] newArray(int size) {
            return new LEDItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(color);
        dest.writeString(ledZone.name());
    }
}