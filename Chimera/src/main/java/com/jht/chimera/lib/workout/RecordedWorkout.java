package com.jht.chimera.lib.workout;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.ObjectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RecordedWorkout implements Parcelable {
    public static class RecordedWorkoutEvent {
        public double time; // Time since beginning of workout, in seconds
        // Each component may be null, indicating no change:
        public @Keep @Nullable Float speed = null; // Speed in km per hour
        public @Keep @Nullable Float resistance = null;
        public @Keep @Nullable Float incline = null;
        public RecordedWorkoutEvent(double time) {
            this.time = time;
        }

        public RecordedWorkoutEvent setSpeed(float speed) { this.speed = speed; return this; }
        public RecordedWorkoutEvent setResistance(float resistance) { this.resistance = resistance; return this; }
        public RecordedWorkoutEvent setIncline(float incline) { this.incline = incline; return this; }

        /** Is it the same disregarding time? */
        public boolean changeEquals(RecordedWorkoutEvent other) {
            return ObjectUtil.equals(speed,other.speed) && ObjectUtil.equals(resistance,other.resistance) && ObjectUtil.equals(incline,other.incline);
        }

        //// Parcelable implementation

        static float unboxNanly(Float f) {
            return f == null ? Float.NaN : f.floatValue();
        }
        static Float boxNanly(float f) {
            return Float.isNaN(f) ? null : Float.valueOf(f);
        }

        public RecordedWorkoutEvent(Parcel parcel) {
            time = parcel.readDouble();
            speed = boxNanly(parcel.readFloat());
            resistance = boxNanly(parcel.readFloat());
            incline = boxNanly(parcel.readFloat());
        }
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeDouble(time);
            parcel.writeFloat(unboxNanly(speed));
            parcel.writeFloat(unboxNanly(resistance));
            parcel.writeFloat(unboxNanly(incline));
        }

        @Override
        public String toString() {
            ArrayList<String> parts = new ArrayList<>();
            parts.add("time: "+time);
            if (speed != null) parts.add("speed: "+speed);
            if (resistance != null) parts.add("resistance: "+resistance);
            if (incline != null) parts.add("incline: "+incline);
            return "RecordedWorkoutEvent { "+String.join(", ", parts)+"}";
        }
    }

    /** Time duration of workout, in seconds */
    public @Keep double duration;
    public @Keep List<RecordedWorkoutEvent> events;

    public RecordedWorkout(double duration, List<RecordedWorkoutEvent> events) {
        this.duration = duration;
        this.events = events;
    }

    protected static float clamp(double v, float min, float max) {
        if (v < min) return min;
        if (v > max) return max;
        return (float)v;
    }

    public static RecordedWorkout random() {
        Random rand = new Random();
        long duration = 999 * (30 + rand.nextInt(1000));
        float currentSpeed = 1 + rand.nextFloat()*2;
        float currentIncline = rand.nextFloat()*50;
        float currentResistance = rand.nextFloat();
        long eventOffset = 0;
        List<RecordedWorkout.RecordedWorkoutEvent> events = new ArrayList<>();
        while (eventOffset < duration) {
            RecordedWorkoutEvent event = new RecordedWorkoutEvent(eventOffset);
            if (rand.nextBoolean()) {
                currentResistance = clamp(currentResistance + rand.nextFloat() - 0.3, 0, 25);
                event.resistance = currentResistance;
            }
            if (rand.nextBoolean()) {
                currentIncline = clamp(currentIncline + rand.nextFloat()*5 - 2, 0, 100);
                event.incline = currentIncline;
            }
            if (rand.nextBoolean()) {
                currentSpeed = clamp(currentSpeed + rand.nextFloat()*2-0.7, 0.75f, 20);
                event.speed = currentSpeed;
            }
            if (event.resistance != null || event.incline != null || event.speed != null) {
                events.add(event);
            }
            eventOffset += rand.nextInt(123456);
        }
        return new RecordedWorkout(duration, events);
    }

    //// Parcelable implementation

    @SuppressWarnings("unchecked")
    public RecordedWorkout(Parcel parcel) {
        duration = parcel.readDouble();
        // "I don't know how to make the parcel array read/write functions work" -- Dan
        int eventCount = parcel.readInt();
        events = new ArrayList<>();
        for (int i=0; i<eventCount; ++i) {
            events.add(new RecordedWorkoutEvent(parcel));
        }
    }

    public static final Parcelable.Creator<RecordedWorkout> CREATOR = new Parcelable.Creator<RecordedWorkout>() {
        public RecordedWorkout createFromParcel(Parcel in)
        {
            return new RecordedWorkout(in);
        }
        public RecordedWorkout[] newArray(int size)
        {
            return new RecordedWorkout[size];
        }
    };

    @Override public int describeContents() { return 0; }

    @Override public void writeToParcel(Parcel parcel, int i) {
        parcel.writeDouble(duration);
        parcel.writeInt(events.size());
        for (RecordedWorkoutEvent evt : events) evt.writeToParcel(parcel, i);
    }
}
