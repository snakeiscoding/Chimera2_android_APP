package com.jht.chimera.lib.workout;

import com.jht.chimera.lib.common.ObjectUtil;

/** Minimal bits of workout information needed to show
 * a workout on the 'recent workouts' / 'recommended for you' screen. */
public final class PreviousWorkout extends WorkoutStartupParams
{
    public final long timestampMs;

    public PreviousWorkout(WorkoutStartupParams params, long timestampMs) {
        super(params);
        this.timestampMs = timestampMs;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PreviousWorkout)) return false;
        PreviousWorkout other = (PreviousWorkout) obj;
        return programTypeId == other.programTypeId
                && ObjectUtil.equals(appUuid, other.appUuid)
                && ObjectUtil.equals(customWorkoutUuid, other.customWorkoutUuid)
                && timestampMs == other.timestampMs;
    }
}
