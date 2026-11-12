package com.jht.chimera.service.workout;


import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.Sprint8Keys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.stats.SummaryData;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.RecordedWorkout;
import com.jht.chimera.lib.workout.RecordedWorkout.RecordedWorkoutEvent;
import com.jht.chimera.lib.workout.RecordedWorkoutDatabase;
import com.jht.chimera.lib.workout.SavedWorkoutData;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.lib.workout.WorkoutStartupParams;
import com.jht.chimera.lib.workout.generator.BaseSprint8Config;
import com.jht.chimera.lib.workout.workoutlist.WorkoutEntry;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.datamatrixlib.DataMatrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WorkoutSaverService extends Service {

    protected static final String TAG = "WorkoutSaverService";

    DataMatrix dataMatrix = new DataMatrix();

    static boolean isWorkoutEmpty(SummaryData sd) {
        // Are these good criteria for ignoring a workout?  I'm not sure!
        if (sd.totalSummary.statsAccumulated.activeCalories != 0) return false;
        if (sd.totalSummary.statsAccumulated.distanceKm != 0) return false;
        if (sd.totalSummary.statsAccumulated.joules != 0) return false;
        return true;
    }

    // Relative/elapsed system time, in milliseconds
    static long currentSystemElapsedMillis() {
        return System.nanoTime()/1000000;
    }

    //// Stuff for tracking elapsed time
    boolean workoutIsPaused;

    float currentSpeed;
    float currentResistance;
    float currentIncline;
    List<RecordedWorkoutEvent> events = new ArrayList<>();
    RecordedWorkout replaying;
    void replayingRecordedWorkout(RecordedWorkout rw) {
        Log.d(TAG, "Replaying a recorded workout of "+rw.duration+" seconds");
        this.replaying = rw;
    }
    void workoutStarted() {
        events.clear();
        RecordedWorkoutEvent setupEvent = new RecordedWorkoutEvent(0);
        setupEvent.incline = currentIncline;
        setupEvent.resistance = currentResistance;
        setupEvent.speed = currentSpeed;
        events.add(setupEvent);
        workoutResumed();
    }
    void workoutResumed() {
        workoutIsPaused = false;
    }
    void workoutPaused() {
        workoutIsPaused = true;
    }

    boolean workoutTimeSanityCheckEnabled = true;

    /** Workout time since start of workout, not including pauses, in seconds */
    double currentWorkoutTime() {
        workoutTimeSanityCheck: if (workoutTimeSanityCheckEnabled) {
            if (workoutTime == -1) {
                Log.w(TAG, "Workout time has not been set!");
                return 0;
            }

            long currentSystemTime = currentSystemElapsedMillis();
            if (!workoutIsPaused && currentSystemTime - workoutTimeLastSet > 1500) {
                Log.w(TAG, "Workout time has not been updated for a while!");
            }
        }

        // We might be able to ignore events that happen while workoutIsPaused.
        // e.g. speed changing to zero and then back to its original value.
        // On the other hand, I'm not sure if they will always cancel out,
        // and those changes will be filtered out when replayed anyway.

        return workoutTime / 1000.0;
    }

    int workoutTime = -1; // Elapsed time from workout controller, in milliseconds
    long workoutTimeLastSet = -1; // Elapsed system time of last workoutTime update, in milliseconds
    // Maximum difference in timestamps of incoming from recorded events
    // to allow correcting the incoming one to match the recorded one.
    // TODO: Now that we're getting timing information via DATA_TIME_ELAPSED_MS_INT,
    // timestamps should match exactly, and this might be unnecessary!
    double maxCorrectableReplaySlop = 1;

    void recordEvent(RecordedWorkoutEvent event) {
        Log.v(TAG, event.toString());

        events.add(event);

        if (event.speed != null) currentSpeed = event.speed;
        if (event.incline != null) currentIncline = event.incline;
        if (event.resistance != null) currentResistance = event.resistance;
    }

    void timeElapsed(int workoutTime) {
        this.workoutTime = workoutTime;
        this.workoutTimeLastSet = currentSystemElapsedMillis();
        //Log.v(TAG, "Time elapsed: "+workoutTime+" ("+currentWorkoutTime()+" seconds)");
    }
    void speedChanged(float speed) {
        if (speed == this.currentSpeed) return;
        recordEvent(new RecordedWorkoutEvent(currentWorkoutTime()).setSpeed(speed));
    }
    void resistanceChanged(float resistance) {
        if (resistance == this.currentResistance) return;
        recordEvent(new RecordedWorkoutEvent(currentWorkoutTime()).setResistance(resistance));
    }
    void inclineChanged(float incline) {
        if (incline == this.currentIncline) return;
        recordEvent(new RecordedWorkoutEvent(currentWorkoutTime()).setIncline(incline));
    }
    void stateChanged(int state) {
        Log.v(TAG, "Workout run state changed: "+state);
        if (state == WorkoutKeys.Constants.RUN_STATE_STOP) {
            Log.d(TAG, "Workout ending at "+currentWorkoutTime());
            // Reset cached Sprint8 config when workout stops so that it does not persist to next workout
            // Workout controller service sends DATA_WORKOUT_SUMMARY_INFO_PARCELABLE prior to the workout ending
            // This means the Sprint8 config will get saved prior to it being reset here.
            sprint8Config = null;
        }
    }

    // Don't save workouts less than this long
    static final int MIN_SAVED_WORKOUT_SECONDS = 60;

    protected void workedOut(SummaryData sd) {
        if (isWorkoutEmpty(sd)) {
            Log.d(TAG, "Got an empty workout; ignoring");
            return;
        }
        if (sd.totalSummary.statsAccumulated.secondsStateRun < MIN_SAVED_WORKOUT_SECONDS) {
            Log.d(TAG, "Workout less than "+MIN_SAVED_WORKOUT_SECONDS+" long; not saving");
            return;
        }

        SavedWorkoutData savedData = new SavedWorkoutData();
        savedData.endTime = new Date();
        savedData.totalSummary = sd.totalSummary;

        WorkoutStartupParams.Builder wspb = new WorkoutStartupParams.Builder();
        wspb.programTypeId = currentWorkoutProgramTypeId;
        wspb.workoutLevel = currentWorkoutLevel;
        // DATA_CUSTOM_WORKOUT_UUID_STRING is reset between workouts, so assume if it is set that we must have started one
        if (this.currentCustomWorkoutUuid != null && !this.currentCustomWorkoutUuid.isEmpty()) {
            // Then they were probably launched together; record the app UUID in there
            Log.d(TAG, "Associating custom workout with this workout: " + this.currentCustomWorkoutUuid);
            wspb.customWorkoutUuid = this.currentCustomWorkoutUuid;
        }
        // DATA_WORKOUT_APP_UUID_STRING is reset between workouts, so assume if it is set that we must have prelaunched with it (TODO - should app id be optional param on workout start params?)
        if (this.currentAppUuid != null && !this.currentAppUuid.isEmpty()) {
            // Then they were probably launched together; record the app UUID in there
            Log.d(TAG, "Associating app with this workout: " + this.currentAppUuid);
            wspb.appUuid = this.currentAppUuid;
        }
        if (currentWorkoutProgramTypeId != null && currentWorkoutProgramTypeId == WorkoutIds.SPRINT8) {
            wspb.mediaTypeName = null;
            wspb.sprint8Config = sprint8Config;
            wspb.numberOfSprints = DataMatrix.getInt(Sprint8Keys.DATA_SPRINT8_NUM_SPRINTS_INT);
        }
        if (currentWorkoutProgramTypeId != null && currentWorkoutProgramTypeId == WorkoutIds.LANDMARKS){
            wspb.landmarkTitle = DataMatrix.getString(WorkoutKeys.DATA_SELECTED_VA_OR_LANDMARK_TITLE_STRING);
        }
        savedData.startupParams = new WorkoutStartupParams(wspb);

        double duration = currentWorkoutTime();
        if (replaying != null) {
            double durationDiff = duration - replaying.duration;
            if (Math.abs(durationDiff) < maxCorrectableReplaySlop) {
                Log.d(TAG, "Replay-adjusted duration ("+duration+") close enough to use duration from replayed workout: "+replaying.duration);
                duration = replaying.duration;
            } else {
                Log.d(TAG, "Difference between measured duration ("+duration+") and replayed ("+replaying.duration+") too big to match: "+durationDiff);
            }
        }
        savedData.recordedWorkout = new RecordedWorkout(duration, events);
        try {
            RecordedWorkoutDatabase.getInstance().save(savedData);
        } catch (RecordedWorkoutDatabase.WorkoutEncodingException|IOException e) {
            Log.e(TAG, "Error writing workout: ", e);
        }
    }

    Integer currentWorkoutProgramTypeId;
    Integer currentWorkoutLevel;

    protected void workoutLoading(WorkoutEntry workout) {
        Log.d(TAG, "Workout loading with program type ID = "+workout.workoutId);
        this.currentWorkoutProgramTypeId = workout.workoutId;
    }

    String currentAppUuid;
    protected void appLaunching(String appUuid) {
        if (AppIds.DEFAULT_APP_UUID.equals(appUuid) || appUuid == null) appUuid = ""; // if we save some workouts with nothing and NIL UUID, they may appear as different workouts
        Log.d(TAG, "App launching: " + appUuid);
        this.currentAppUuid = appUuid;
    }

    String currentCustomWorkoutUuid;
    protected void customWorkoutLaunching(String uuid) {
        Log.d(TAG, "Custom workout launching: "+uuid);
        this.currentCustomWorkoutUuid = uuid;
    }

    protected void initialConditionsChanged(InitialConditionsList conditions) {
        this.currentWorkoutLevel = null;
        for (InitialCondition cond : conditions.initialConditions) {
            if (ConsoleWorkoutVariables.LEVEL.equals(cond.conditionName)) {
                this.currentWorkoutLevel = Integer.valueOf((int)cond.conditionValue);
            }
        }
        Log.v(TAG, "Level set to "+currentWorkoutLevel);
    }

    // Cache Sprint 8 custom workout config so that it can be saved with the workout
    // We store this now since it is sent over a DM ACTION key and will not be available later
    private BaseSprint8Config sprint8Config = null;
    private void onSprint8ConfigLoaded(BaseSprint8Config config) {
        this.sprint8Config = config;
    }

    public void onCreate() {
        super.onCreate();
        dataMatrix.createEventAdapter(WorkoutKeys.EVENT_WORKOUT_START, s -> workoutStarted());
        dataMatrix.createEventAdapter(WorkoutKeys.ACTION_PAUSE_WORKOUT, s -> workoutPaused());
        dataMatrix.createEventAdapter(WorkoutKeys.ACTION_RESUME_WORKOUT, s -> workoutResumed());

        dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, this::timeElapsed);
        dataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_INCLINE_FLOAT, this::inclineChanged);
        dataMatrix.createIntAdapter(HALKeys.DATA_RESISTANCE_INT, this::resistanceChanged);
        dataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, this::speedChanged);
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, this::stateChanged);

        dataMatrix.createStringAdapter(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, this::appLaunching);
        dataMatrix.createStringAdapter(WorkoutKeys.DATA_CUSTOM_WORKOUT_UUID_STRING, this::customWorkoutLaunching);
        dataMatrix.createParcelableAdapter(WorkoutKeys.DATA_CURRENT_RECORDED_WORKOUT_PARCELABLE, RecordedWorkout.class, this::replayingRecordedWorkout);
        dataMatrix.createParcelableAdapter(WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE, SummaryData.class, this::workedOut);
        dataMatrix.createParcelableAdapter(WorkoutKeys.ACTION_LOAD_WORKOUT_ENTRY_PARCELABLE, WorkoutEntry.class, this::workoutLoading);
        dataMatrix.createParcelableAdapter(WorkoutKeys.ACTION_SET_SPRINT8_WORKOUT_INFO_PARCELABLE, BaseSprint8Config.class, this::onSprint8ConfigLoaded);
        dataMatrix.createParcelableAdapter(WorkoutKeys.ACTION_LAUNCH_WORKOUT_PARCELABLE, WorkoutLaunchData.class, data -> {
            initialConditionsChanged(data.getInitialConditionsList());
        });
    }

    @Override
    public void onDestroy() {
        dataMatrix.finish();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
