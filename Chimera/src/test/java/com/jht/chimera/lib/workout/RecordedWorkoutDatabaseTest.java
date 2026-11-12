package com.jht.chimera.lib.workout;

import junit.framework.TestCase;

import java.text.ParseException;

public class RecordedWorkoutDatabaseTest extends TestCase
{
    static final String[] someDifferentInvalidWorkoutJsons = new String[] {
        "",                   // Empty string
        "{\"foo\": \"bar\"}", // Valid but nonsensical JSON
        "hello, world!",      // Invalid JSON!
        "1",                  // Valid JSON, wrong data type.
        "[1,2,3]",            // # Valid JSON, wrong data type.
        "{\"endTime\": \"Chewbacca\"}", // has endTime but it's malformed
    };

    static final String[] someDifferentValidWorkoutJsons = new String[] {
        "{\"endTime\": \"2020-06-10T13:44Z\"}", // Bare minimum
    };

    public void testInvalidWorkoutDataThrows() {
        for (String json : someDifferentInvalidWorkoutJsons) {
            try {
                SavedWorkoutData swd = RecordedWorkoutDatabase.parseWorkout(json);
                fail("parsing bad workout JSON should throw WorkoutLoadingException: "+json);
            } catch (RecordedWorkoutDatabase.WorkoutEncodingException ignore) {
            }
        }
    }

    public void testValidWorkoutDataDoesNotThrow() throws RecordedWorkoutDatabase.WorkoutEncodingException {
        for (String json : someDifferentValidWorkoutJsons) {
            SavedWorkoutData swd = RecordedWorkoutDatabase.parseWorkout(json);
        }
    }

    public void testSavedWorkoutJsonRoundTrip() throws ParseException, RecordedWorkoutDatabase.WorkoutEncodingException {
        String dateStr = "2020-06-10T20:52Z";

        SavedWorkoutData swd = new SavedWorkoutData();
        swd.endTime = RecordedWorkoutDatabase.ISO8601.parse(dateStr);
        String json = RecordedWorkoutDatabase.encodeSavedWorkoutData(swd);
        SavedWorkoutData loaded = RecordedWorkoutDatabase.parseWorkout(json);
        assertEquals(swd.endTime, loaded.endTime);
    }
}
