package com.jht.chimera.lib.workout;

import com.jhtp.dapiservice.wt.DapiProgramType;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.Random;
import java.util.UUID;

public class WorkoutStartupParamsTest extends TestCase
{
    protected void randomize(WorkoutStartupParams.Builder b) {
        Random r = new Random();
        b.programTypeId = r.nextInt();
        b.appUuid = UUID.randomUUID().toString();
        b.workoutLevel = r.nextInt();
        b.featureUuid = UUID.randomUUID().toString();
        b.mediaTypeName = null;
    }

    @Test
    public void testBuilderConstructor() {
        WorkoutStartupParams.Builder b = new WorkoutStartupParams.Builder();
        randomize(b);
        WorkoutStartupParams w = new WorkoutStartupParams(b);
        assertEquals(b.programTypeId, w.programTypeId);
        assertEquals(b.featureUuid, w.featureUuid);
        assertEquals(b.appUuid, w.appUuid);
        assertEquals(b.mediaTypeName, w.mediaTypeName);
        assertEquals(b.workoutLevel, w.workoutLevel);
        assertEquals(b.customWorkoutUuid, w.customWorkoutUuid);
    }

    @Test
    public void testCopyConstructor() {
        WorkoutStartupParams.Builder b = new WorkoutStartupParams.Builder();
        randomize(b);

        WorkoutStartupParams w1 = new WorkoutStartupParams(b);
        WorkoutStartupParams w2 = new WorkoutStartupParams(w1);

        assertEquals(w1.programTypeId, w2.programTypeId);
        assertEquals(w1.featureUuid, w2.featureUuid);
        assertEquals(w1.appUuid, w2.appUuid);
        assertEquals(w1.mediaTypeName, w2.mediaTypeName);
        assertEquals(w1.workoutLevel, w2.workoutLevel);
        assertEquals(w1.customWorkoutUuid, w2.customWorkoutUuid);
    }

    @Test
    public void testCreateForCustomWorkout() {
        UUID aUuid = UUID.randomUUID();
        WorkoutStartupParams params = WorkoutStartupParams.forCustomWorkout(aUuid);
        // Regarding custom workouts being 19 or 90, this conversation happened on 2020-10-13:
        // Kong: Ask Foyer to confirm. I believe 90 is the right one.
        // Ed: Foyer is saying 90.
        // (DapiProgramType.CardioCustomWorkout.Value = 90)
        assertEquals(DapiProgramType.CardioCustomWorkout.Value, params.programTypeId.intValue());
        assertEquals(aUuid.toString(), params.customWorkoutUuid);
        assertNull(params.appUuid);
    }
}
