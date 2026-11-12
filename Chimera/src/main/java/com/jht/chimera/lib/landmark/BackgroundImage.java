package com.jht.chimera.lib.landmark;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class BackgroundImage implements Parcelable {
    private static final String TAG = "BackgroundImage";
    public String _source;    // the path of the image source file

    public float _pivotX;
    public float _pivotY;
    public float _scaleX;
    public float _scaleY;

    public BackgroundImage() {

    }

    public static final Parcelable.Creator<BackgroundImage> CREATOR = new Parcelable.Creator<BackgroundImage>() {

        @Override
        public BackgroundImage createFromParcel(Parcel source) {
            return new BackgroundImage(source);
        }

        @Override
        public BackgroundImage[] newArray(int size) {
            return new BackgroundImage[size];
        }

    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_source);
        dest.writeFloat(_pivotX);
        dest.writeFloat(_pivotY);
        dest.writeFloat(_scaleX);
        dest.writeFloat(_scaleY);

        Log.d(TAG,  " Write:" + this.toString());
    }

    public BackgroundImage(Parcel source) {
        _source = source.readString();

        _pivotX = source.readFloat();
        _pivotY = source.readFloat();
        _scaleX = source.readFloat();
        _scaleY = source.readFloat();

        Log.d(TAG, "Read: " + this.toString());
    }

    @Override
    public String toString() {
        return "{" +
                ", _source" + _source +
                ", _pivotX" + _pivotX +
                ", _pivotY" + _pivotY +
                ", _scaleX" + _scaleX +
                ", _scaleY" + _scaleY +
                "}";
    }
}
