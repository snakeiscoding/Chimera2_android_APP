package com.jht.chimera.ui.mainapp.workouttracking;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.ProfileValue;
import com.jht.chimera.lib.common.WorkoutFuture;
import com.jht.chimera.lib.common.WorkoutFutureProfile;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.datamatrixlists.DataMatrixListFactory;
import com.jht.chimera.lib.common.datamatrixlists.WorkoutTrackingSnapshot;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.workoutcontrollerlib.WorkoutController;
import com.jht.workoutdata.client.WorkoutDataClient;
import com.jht.workoutdata.model.FutureProfile;
import com.jht.workoutdata.model.FutureProfilePoint;
import com.jht.workoutdata.model.WorkoutDataPoint;
import com.jht.workoutdata.model.WorkoutFutureData;
import com.jht.workoutdata.model.WorkoutMetric;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks workout information for graphing the workout history and sending the workout to DAPI.
 * <p>
 * Created by chris.foyer on 1/29/2018.
 */
public class WorkoutTrackingService extends Service {
    private static final String TAG = "WorkoutTrackingService";

    private DataMatrix mDataMatrix = new DataMatrix();

    private int mSplitCount = 1;

    private DistanceMarkers distanceMarkers = new DistanceMarkers();
    private DataMatrixListFactory.WorkoutSnapshotProducer snapshots;
    private WorkoutDataClient mWorkoutDataClient;


    @Override
    public void onCreate() {
        Log.d("ServiceStarter", "Starting " + this);
        super.onCreate();
        init();
        Log.d("ServiceStarter", "Ending " + this);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void init() {
        snapshots = DataMatrixListFactory.snapshotListProducer(mDataMatrix);
        mWorkoutDataClient = new WorkoutDataClient(this);

        // STOP ends the workout: clear the provider (which bumps its generation token —
        // the reset signal incremental graph readers depend on), the DAPI snapshot list,
        // splits, and distance markers so the next workout starts clean.
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, runState -> {
            if (runState == WorkoutKeys.Constants.RUN_STATE_STOP) {
                resetTracking();
            }
        });

        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_START_SPLIT, keyReturnedBack -> mSplitCount++);

        // Gated to match the retired 1Hz polling timer: the controller fires
        // ACTION_WORKOUT_STATS_UPDATED in other run states/stages too, but points must
        // only be recorded while the workout is actually running in the workout stage —
        // this feeds both the WorkoutData provider and the DAPI snapshot list.
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_WORKOUT_STATS_UPDATED, key -> {
            if (isWorkoutRunning()) {
                graphUpdate();
            }
        });

        // Single mirror point for all future publishers (WorkoutControllerService, VirtualActive):
        // whenever DATA_WORKOUT_FUTURE_PARCELABLE changes, convert it to the provider's
        // frame-agnostic model and push it. The provider fires its future-change notification so
        // graphs can re-fetch. trigger=true seeds the provider with the current value on startup.
        mDataMatrix.createParcelableAdapter(WorkoutKeys.DATA_WORKOUT_FUTURE_PARCELABLE, WorkoutFuture.class,
                this::mirrorFutureToProvider, true);
    }

    /** Converts the DataMatrix {@link WorkoutFuture} to the provider model and pushes it. */
    private void mirrorFutureToProvider(@Nullable WorkoutFuture future) {
        try {
            final List<FutureProfile> profiles = new ArrayList<>();
            if (WorkoutFuture.isValid(future)) {
                for (final WorkoutFutureProfile profile : future.profiles) {
                    final WorkoutMetric metric = metricForScriptVariable(profile.parameter);
                    if (metric == null || profile.profileValues == null) continue;
                    final List<FutureProfilePoint> points = new ArrayList<>(profile.profileValues.size());
                    for (final ProfileValue value : profile.profileValues) {
                        points.add(new FutureProfilePoint(value.timeMs / 1000f, value.value));
                    }
                    profiles.add(new FutureProfile(metric, points));
                }
            }
            mWorkoutDataClient.setFuture(new WorkoutFutureData(profiles));
        } catch (Exception e) {
            // Never let a provider failure disrupt the tracking service.
            Log.e(TAG, "Failed to mirror workout future to provider", e);
        }
    }

    /** Workout-script variable -> provider metric (mirrors GraphManager's channel mapping). */
    @Nullable
    private static WorkoutMetric metricForScriptVariable(@Nullable String parameter) {
        if (ConsoleWorkoutVariables.SPEED.equals(parameter)) return WorkoutMetric.SPEED;
        if (ConsoleWorkoutVariables.INCLINE.equals(parameter)) return WorkoutMetric.INCLINE;
        if (ConsoleWorkoutVariables.RESISTANCE.equals(parameter)) return WorkoutMetric.RESISTANCE;
        if (ConsoleWorkoutVariables.TARGET_WATTS.equals(parameter)) return WorkoutMetric.WATTS;
        return null; // no graphable future for other script variables
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDataMatrix.finish();
    }

    /** The tracking gate: running, and in the workout stage. */
    private boolean isWorkoutRunning() {
        int runState     = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT);
        int workoutState = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT);
        return runState == WorkoutKeys.Constants.RUN_STATE_RUNNING && workoutState == WorkoutController.WORKOUT_STAGE_WORKOUT;
    }

    private void graphUpdate() {

        pollWorkoutData();

        float  kilometers = DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT);
        double miles = DistanceConvertUtil.convertKilometersToMiles(kilometers);
        int    seconds = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT)/1000;
        boolean addedMarker = false;


        int mileIndex = (int)miles;
        for(int i = distanceMarkers.mileMarkers.size(); i <= mileIndex; i++) {
            addedMarker = true;
            distanceMarkers.mileMarkers.add(seconds);
        }
        distanceMarkers.mileMarkers.set(mileIndex, seconds);

        int kmIndex = (int)kilometers;
        for(int i = distanceMarkers.kmMarkers.size(); i <= kmIndex; i++) {
            addedMarker = true;
            distanceMarkers.kmMarkers.add(seconds);
        }
        distanceMarkers.kmMarkers.set(kmIndex, seconds);

        if(addedMarker) {
            DataMatrix.putParcelable(WorkoutKeys.EVENT_DISTANCE_MARKERS_PARCELABLE, distanceMarkers);
        }
    }

    /**
     * Polls for information about the workout. Also updates the graphing points.
     *
     */
    private void pollWorkoutData() {
        WorkoutTrackingSnapshot workoutData = new WorkoutTrackingSnapshot();
        workoutData.distance = DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT);
        workoutData.resistance = DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
        workoutData.heartRate = DataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT);
        workoutData.watts = (int) DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT);
        workoutData.workoutState = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT);
        workoutData.timeInState = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT);
        workoutData.lapIndex = DataMatrix.getInt(WorkoutKeys.DATA_LAP_COUNT_INT);
        workoutData.splitIndex = mSplitCount;
        workoutData.kph = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT);
        workoutData.incline = DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT);
        workoutData.totalCalories = DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
        workoutData.mets = DataMatrix.getFloat(HALKeys.DATA_METS_FLOAT);
        workoutData.rpm = DataMatrix.getInt(HALKeys.DATA_RPM_INT);
        workoutData.targetHeartRate = DataMatrix.getInt(WorkoutKeys.DATA_TARGET_HEART_RATE_INT);
        workoutData.targetWatts = DataMatrix.getInt(WorkoutKeys.DATA_TARGET_WATTS_INT);
        workoutData.totalFloors = DataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT);
        workoutData.totalSteps = DataMatrix.getInt(WorkoutKeys.DATA_STEPS_INT);
        workoutData.elevationMeters = DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT);
        workoutData.spm = DataMatrix.getInt(HALKeys.DATA_SPM_INT);
        snapshots.addElement(workoutData);

        try {
            int elapsedSec = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
            WorkoutDataPoint point = new WorkoutDataPoint.Builder(elapsedSec)
                    .workoutStage(workoutData.workoutState)
                    .lapIndex(workoutData.lapIndex)
                    .splitIndex(workoutData.splitIndex)
                    .distanceKm(workoutData.distance)
                    .speedKph(workoutData.kph)
                    .inclinePercent(workoutData.incline)
                    .resistance(workoutData.resistance)
                    .rpm(workoutData.rpm)
                    .spm(workoutData.spm)
                    .heartRate(workoutData.heartRate)
                    .watts(workoutData.watts)
                    .targetHeartRate(workoutData.targetHeartRate)
                    .targetWatts(workoutData.targetWatts)
                    .totalCalories(workoutData.totalCalories)
                    .mets(workoutData.mets)
                    .totalFloors(workoutData.totalFloors)
                    .totalSteps(workoutData.totalSteps)
                    .elevationMeters(workoutData.elevationMeters)
                    .build();
            mWorkoutDataClient.addPoint(point);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add point to WorkoutData provider", e);
        }
    }

    private void resetTracking() {
        mSplitCount = 1;
        distanceMarkers.mileMarkers.clear();
        distanceMarkers.kmMarkers.clear();
        DataMatrix.putParcelable(WorkoutKeys.EVENT_DISTANCE_MARKERS_PARCELABLE, distanceMarkers);
        snapshots.clearList();
        try {
            mWorkoutDataClient.clearBlocking();
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear WorkoutData provider", e);
        }
    }

}
