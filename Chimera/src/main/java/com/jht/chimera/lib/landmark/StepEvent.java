package com.jht.chimera.lib.landmark;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class StepEvent implements Parcelable {
    private static final String TAG = "StepEvent";
    public EventType _eventType;
    public int _steps;                // occur on steps
    public String _message;
    public String _soundFile;

    public enum EventType {
        MESSAGE,
        SOUND,
        NONE
    }

    public StepEvent() {
        _eventType = EventType.NONE;
        _message = "";
        _soundFile = "";
        _steps = -1;
    }

    public interface IEvent {
        public void trigger(Context context);
    }

    public static class MessageEvent extends StepEvent implements IEvent {

        public MessageEvent(int steps, String message) {
            _eventType = EventType.MESSAGE;
            _message = message;
            _soundFile = "";
            _steps = steps;
        }

        @Override
        public void trigger(Context context) {
        }
    }

    public static class SoundEvent extends StepEvent implements IEvent {

        public SoundEvent(int steps, String soundFile) {
            _eventType = EventType.SOUND;
            _message = "";
            _soundFile = soundFile;
            _steps = steps;
        }

        @Override
        public void trigger(Context context) {
        }
    }

    public static final Parcelable.Creator<StepEvent> CREATOR = new Parcelable.Creator<StepEvent>() {

        @Override
        public StepEvent createFromParcel(Parcel source) {
            return new StepEvent(source);
        }

        @Override
        public StepEvent[] newArray(int size) {
            return new StepEvent[size];
        }

    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_eventType.ordinal());
        dest.writeInt(_steps);
        dest.writeString(_message);
        dest.writeString(_soundFile);

        Log.d(TAG, "Event write: " + this.toString());
    }

    public StepEvent(Parcel source) {
        int eventType = source.readInt();
        if (eventType >= EventType.NONE.ordinal()) {
            _eventType = EventType.NONE;
            _steps = 0;
            _message = "";
            _soundFile = "";
        } else {
            _eventType = EventType.values()[eventType];
            _steps = source.readInt();
            _message = source.readString();
            _soundFile = source.readString();
        }

        Log.d(TAG, "Event read: " + this.toString());
    }

    @Override
    public String toString() {
        return "{" +
                ", _eventType" + _eventType +
                ", _steps" + _steps +
                ", _message" + _message +
                ", _soundFile" + _soundFile +
                "}";
    }
}
