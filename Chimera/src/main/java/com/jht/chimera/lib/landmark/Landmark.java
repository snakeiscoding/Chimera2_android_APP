package com.jht.chimera.lib.landmark;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Landmark implements Parcelable {
    public String _title;
    public String _description;
    public String _version;
    public String _location;
    public List<BackgroundImage> _backgroundImageFiles;
    public String _silhouetteFullFile;    // for full screen
    public String _silhouetteNormalFile; // for normal screen
    public String _startVideoFile;
    public List<StepEvent> _events;
    public int _goalType;

    public int _meters;
    public int _feets;
    public int _floors;
    public int _floorsRetail;
    public int _steps;

    public Landmark() {
        _description = "";
        _title = "";
        _version = "";
        _location = "";
        _backgroundImageFiles = new ArrayList<>();
        _silhouetteFullFile = "";
        _silhouetteNormalFile = "";
        _startVideoFile = "";
        _events = new ArrayList<>();
        _goalType = -1;

        _meters = 0;
        _feets = 0;
        _floors = 0;
        _steps = 0;
    }

    public Landmark(String location, String description, int meters,
                    int feets, int floors, int steps) {
        _location = location;
        _description = description;
        _meters = meters;
        _feets = feets;
        _floors = floors;
        _steps = steps;
    }

    public static final Parcelable.Creator<Landmark> CREATOR = new Parcelable.Creator<Landmark>() {

        @Override
        public Landmark createFromParcel(Parcel source) {
            return new Landmark(source);
        }

        @Override
        public Landmark[] newArray(int size) {
            return new Landmark[size];
        }

    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_title);
        dest.writeString(_location);
        dest.writeString(_description);
        dest.writeInt(_meters);
        dest.writeInt(_feets);
        dest.writeInt(_floors);
        dest.writeInt(_floorsRetail);
        dest.writeInt(_steps);
        dest.writeTypedList(_backgroundImageFiles);
        dest.writeString(_silhouetteFullFile);
        dest.writeString(_silhouetteNormalFile);
        dest.writeString(_startVideoFile);
        dest.writeTypedList(_events);
        dest.writeInt(_goalType);
    }

    public Landmark(Parcel source) {
        _title = source.readString();
        _location = source.readString();
        _description = source.readString();
        _meters = source.readInt();
        _feets = source.readInt();
        _floors = source.readInt();
        _floorsRetail = source.readInt();
        _steps = source.readInt();

        _backgroundImageFiles = new ArrayList<>();
        source.readTypedList(_backgroundImageFiles, BackgroundImage.CREATOR);

        _silhouetteFullFile = source.readString();
        _silhouetteNormalFile = source.readString();
        _startVideoFile = source.readString();

        _events = new ArrayList<>();
        source.readTypedList(_events, StepEvent.CREATOR);

        _goalType = source.readInt();
    }

    @Override
    public String toString() {
        return "{" +
                ", _title" + _title +
                ", _description" + _description +
                ", _version" + _version +
                ", _location" + _location +
                ", _silhouetteFullFile" + _silhouetteFullFile +
                ", _silhouetteNormalFile" + _silhouetteNormalFile +
                ", _startVideoFile" + _startVideoFile +
                ", _goalType" + _goalType +
                ", _meters" + _meters +
                ", _feets" + _feets +
                ", _floors" + _floors +
                ", _steps" + _steps
                + "}";
    }
}