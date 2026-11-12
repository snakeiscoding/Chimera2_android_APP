package com.jht.chimera.test.fixtures;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Helper class for unit tests where we need to inject system events and check the system
 * at certain points in the test.  This is common for workout tests where we need to modify the system
 * based on the workout time and also check the system data.
 * @param <T>   Parcelable data type.
 */
public abstract class TestEventBaseParcelable<T extends Parcelable> extends TestEventBase<T> {

    /**
     * Create an event.
     *
     * @param timing    Time of the event.
     * @param variable  Variable to check or modify.
     * @param value     Value to set or compare against.
     */
    TestEventBaseParcelable(int timing, @NonNull Object variable, @NonNull T value) {
        super(timing, variable, value);
    }


    /**
     * Check the system to make sure the value is the expected value.
     *
     * @return  Null if the check succeeds.  Error message if the check fails.
     */
    public @Nullable String check() {
        Object realValue = getValue();

        if(!value.equals(realValue)) {
            return String.format(Locale.ENGLISH, "Checking %s expected %s, but got %s at %d", variable.toString(), value, realValue, timing);
        }
        return null;
    }
}
