package com.jht.chimera.test.fixtures;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

/**
 * Helper class for unit tests where we need to inject system events and check the system
 * at certain points in the test.  This is common for workout tests where we need to modify the system
 * based on the workout time and also check the system data.
 */
public abstract class TestEventBaseStringList extends TestEventBase<List<String>> {

    /**
     * Create an event.
     *
     * @param timing    Time of the event.
     * @param variable  Variable to check or modify.
     * @param value     Value to set or compare against.
     */
    TestEventBaseStringList(int timing, @NonNull Object variable, @NonNull List<String> value) {
        super(timing, variable, value);
    }


    /**
     * Check the system to make sure the value is the expected value.
     *
     * @return  Null if the check succeeds.  Error message if the check fails.
     */
    public @NonNull String check() {
        Object realValue = getValue();
        if(realValue instanceof List) {
            if(((List<?>)realValue).size() != value.size()) {
                return String.format(Locale.ENGLISH, "Checking %s failed expected %d size, but got %d at %d", variable.toString(), value.size(), ((List<?>)realValue).size(), timing);
            }
            for(int i = 0; i < value.size(); i++) {
                if(!value.get(i).equals(((List<?>)realValue).get(i))) {
                    return String.format(Locale.ENGLISH, "Checking %s failed at index %d expected %s, but got %s at %d", variable.toString(), i, value.get(i), ((List<?>)realValue).get(i), timing);
                }
            }
            return null;
        }

        return String.format(Locale.ENGLISH, "Checking %s and it is the wrong type.", variable.toString());
    }
}
