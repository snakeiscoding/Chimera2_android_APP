package com.jht.mastboard.data;

import android.os.Parcel;
import android.os.Parcelable;

public enum LedZone implements Parcelable {
    SIDE("side_panels"),
    PLAY("play_button_backlight"),
    STOP("stop_button_backlight"),
    BACK("back_panel_console"),
    ALL_EXCEPT_PLAY_STOP("all_except_play_stop"),
    ALL("all_leds");

    public String Value;
    public static final  Parcelable.Creator<LedZone> CREATOR = new  Parcelable.Creator<LedZone>() {
        public LedZone createFromParcel(Parcel source) {
            return LedZone.valueOf(source.readString());
        }

        public LedZone[] newArray(int size) {
            return new LedZone[size];
        }
    };

    LedZone(String value) {
        this.Value = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Value);
    }
}
