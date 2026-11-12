package com.jht.chimera.service.workout;

import com.jht.chimera.lib.workout.RecordedWorkout;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutHelper;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RecordedWorkoutHelper {
    protected static float coalesce(Float v, float backup) {
        if (v == null) return backup;
        return v;
    }

    /** Returns a WorkoutParameter with the specified ID
     * and everything else blank. */
    static WorkoutParameter identifierParameter(String id) {
        return new WorkoutParameter(
                0, // min
                100, // max
                50, // value
                1, // stepSize
                0, // precision
                "", // label
                "", // units
                null, // measurement
                id);
    }
    static WorkoutParameter TIME_PARAM = identifierParameter("TIME_GOAL_START"); // Length in minutes NON-NLS
    static WorkoutParameter SPEED_PARAM = identifierParameter("START_SPEED"); // Speed in kph NON-NLS
    static WorkoutParameter INCLINE_PARAM = identifierParameter("START_INCLINE"); //NON-NLS
    static WorkoutParameter RESISTANCE_PARAM = identifierParameter("START_RESISTANCE"); //NON-NLS

    static class Intervalifier {
        List<Float> timeData = new ArrayList<>();
        List<Float> resistanceData = new ArrayList<>();
        List<Float> inclineData = new ArrayList<>();
        List<Float> speedData = new ArrayList<>();

        // "what I did in the past is that if the inclince/speed/resistance didn't change without x
        // amount of seconds, then keep that last value"
        // -- Kong

        int lastEventTime = 0; // Seconds since beginning of workout
        int minIntervalength = 5; // Do not generate intervals shorter than this many seconds
        float lastSetSpeed = 0;
        float lastSetIncline = 0;
        float lastSetResistance = 0;

        int currentIntervalStartTime = 0; // Seconds
        float currentIntervalSpeed = 0; // KPH
        float currentIntervalIncline = 0;
        float currentIntervalResistance = 0;

        /**
         * An arbitrary quantization function applied to timestamps of incoming events
         * (including ending the workout) to simplify/make more predictable
         * replay behavior.
         *
         * Floor is chosen so that timestamps match the time displayed to the user
         * when the event happens.  e.g. if you pause/end a workout when the timer
         * says 1:30, this will always result in the replayed workout
         * being 1:30 long, even if the pause button was hit closer to 1:31.
         * This matches behavior elsewhere in the system.
         */
        static int quantizeTimestamp(double timestamp) {
            return (int)Math.floor(timestamp);
        }

        /**
         * Converts a duration in seconds
         * to one in minutes, rounding DOWN to the nearest 65536th of a minute,
         * which is the closest thing to a millisecond exactly representable by a float
         * that represents minutes.
         *
         * This rounding down is important for replaying,
         * because during playback, interval lengths are effectively rounded up to the nearest second,
         * which would result in compounding lengthening of replayed workouts if not compensated for.
         **/
        static float secondsToReplayableMinutes(int second) {
            return (float)(Math.floor(second * 65536.0 / 60.0) / 65536.0);
        }

        void closeCurrentInterval(int endTime) {
            // Ignore zero-second intervals!
            // These happen when there's no data yet.
            if (endTime == currentIntervalStartTime) return;

            timeData.add(secondsToReplayableMinutes(endTime - currentIntervalStartTime));
            speedData.add(currentIntervalSpeed);
            inclineData.add(currentIntervalIncline);
            resistanceData.add(currentIntervalResistance);

            // The next interval, if any, necessarily starts
            // after this one has ended:
            currentIntervalStartTime = endTime;
        }

        /** Called when time has moved forward, but before any parameter changes have been registered */
        void forwardTo(int time) {
            // Has it been a while since anything changed?
            if (time - lastEventTime >= minIntervalength) {
                // Then we found the start of an interval!
                // If the previous one is long enough (might not be in some cases)
                // then close it.  Otherwise it merges with the current one.
                if (lastEventTime - currentIntervalStartTime >= minIntervalength) {
                    //Log.v(RecordedWorkoutHelper.class.getSimpleName(), "C: Closing interval at "+time+"!  "+time+" - "+lastEventTime+" >= "+minIntervalength+" and "+lastEventTime+" - "+currentIntervalStartTime+" >= "+minIntervalength);
                    closeCurrentInterval(lastEventTime);
                } else {
                    //Log.v(RecordedWorkoutHelper.class.getSimpleName(), "B: Not closing interval at "+time+" because "+lastEventTime+" - "+currentIntervalStartTime+" < "+minIntervalength);
                }
                // In any case, the data for this current interval
                // is those last set values.
                currentIntervalSpeed = lastSetSpeed;
                currentIntervalIncline = lastSetIncline;
                currentIntervalResistance = lastSetResistance;
            } else {
                //Log.v(RecordedWorkoutHelper.class.getSimpleName(), "A: Not closing interval at "+time+" because "+time+" - "+lastEventTime+" < "+minIntervalength);
            }
        }

        void event(RecordedWorkout.RecordedWorkoutEvent evt) {
            int time = quantizeTimestamp(evt.time);

            this.forwardTo(time);

            if (evt.speed != null) this.lastSetSpeed = evt.speed.floatValue();
            if (evt.incline != null) this.lastSetIncline = evt.incline.floatValue();
            if (evt.resistance != null) this.lastSetResistance = evt.resistance.floatValue();

            this.lastEventTime = time;
        }

        CustomWorkoutItem end(double _time) {
            int time = quantizeTimestamp(_time);
            forwardTo(time);
            // Potentially make one last interval,
            // which is potentially the only interval,
            // and is potentially short.
            closeCurrentInterval(time);

            CustomWorkoutItem cw = new CustomWorkoutItem("'recorded workout'", UUID.randomUUID(), new HashMap<>(), false); //NON-NLS
            cw.hasCooldown = false;
            cw.intervalData.put(TIME_PARAM.identifier(), timeData);
            cw.intervalData.put(SPEED_PARAM.identifier(), speedData);
            cw.intervalData.put(INCLINE_PARAM.identifier(), inclineData);
            cw.intervalData.put(RESISTANCE_PARAM.identifier(), resistanceData);
            return cw;
        }
    }

    public CustomWorkoutItem recordedWorkoutToCustomWorkout(RecordedWorkout rw) {

        // We'll want to batch parameter changes:
        // - Non't allow more than one change per minIntervalLength
        // - If we replay a replayed workout, it should be identical!

        Intervalifier b = new Intervalifier();

        for (RecordedWorkout.RecordedWorkoutEvent evt : rw.events) {
            b.event(evt);
        }
        return b.end(rw.duration);
    }


    public String recordedWorkoutToScript(RecordedWorkout rw) {
        CustomWorkoutItem cw = recordedWorkoutToCustomWorkout(rw);
        cw.allowGoalChanges = true;
        // DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)
        return CustomWorkoutHelper.getWorkoutScript(cw, true);

    }
}
