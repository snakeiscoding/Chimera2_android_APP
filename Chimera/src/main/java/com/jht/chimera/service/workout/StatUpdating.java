package com.jht.chimera.service.workout;

import static com.jht.chimera.lib.common.datamatrixkeys.HALKeys.STATUS_GYM_KIT_DISCONNECTED;

import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.stats.StatsAccumulated;
import com.jht.chimera.lib.common.stats.StatsAverages;
import com.jht.chimera.lib.common.stats.StatsPeaks;
import com.jht.chimera.lib.common.stats.StatsSplits;
import com.jht.chimera.lib.common.stats.SummaryData;
import com.jht.chimera.lib.common.stats.WorkoutSplit;
import com.jht.chimera.lib.common.stats.WorkoutSummary;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.service.hal.lib.lcb.ReceiveTreadmillCadence;
import com.jht.chimera.service.workout.math.CadenceFormulas;
import com.jht.chimera.service.workout.stat.MachineData;
import com.jht.chimera.service.workout.stat.WorkoutDataContainer;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.datamatrixlib.adapter.FloatAdapter;
import com.jht.datamatrixlib.adapter.IntAdapter;
import com.jht.datamatrixlib.adapter.ParcelableAdapter;
import com.jht.gattserver2.WatchState;
import com.jht.data.TrackInfo;
import com.jht.data.sprint8.Sprint8SweatScores;
import com.jht.workoutcontrollerlib.workoutcardio.WorkoutControllerCardio;

import java.util.List;

/**
 * Calculates and tracks the different stat values, including cumulative stats such as
 * distance or elevation. Also tracks averages over the course of a workout.
 */
@SuppressWarnings("WeakerAccess")
class StatUpdating {
    protected static final String TAG = "StatUpdating";
    private WorkoutControllerCardio mWorkoutController;

    private int mWorkoutTime = 0;
    private final MachineData mWorkoutMachineData = new MachineData();

    private static final int MS_TO_HOURS = 3600000;

    private boolean adaptersInit = false;

    private final DataMatrix mDataMatrix = new DataMatrix();

    @Nullable
    private FloatAdapter distanceAdapter;
    @Nullable
    private IntAdapter averageSpmInt;
    @Nullable
    private IntAdapter stepsAdapter;
    @Nullable
    private FloatAdapter floorsAdapter;
    @Nullable
    private FloatAdapter elevationAdapter;
    @Nullable
    private IntAdapter paceAdapter;
    @Nullable
    private IntAdapter averagePaceAdapter;
    @Nullable
    private FloatAdapter workoutSpeedAdapter;
    @Nullable
    private FloatAdapter workoutAverageSpeedAdapter;
    @Nullable
    private FloatAdapter caloriesAdapter;
    @Nullable
    private FloatAdapter activeCaloriesAdapter;
    @Nullable
    private IntAdapter calPerHourAdapter;
    @Nullable
    private IntAdapter peakHeartRateAdapter;
    @Nullable
    private FloatAdapter joulesAdapter;
    @Nullable
    private FloatAdapter averageWattsAdapter;
    @Nullable
    private FloatAdapter averageRmpAdapter;
    @Nullable
    private FloatAdapter averageHeartRateAdapter;
    @Nullable
    private FloatAdapter averageInclineAdapter;
    @Nullable
    private FloatAdapter averageResistanceAdapter;
    @Nullable
    private FloatAdapter strideCountAdapter;
    @Nullable
    private ParcelableAdapter<TrackInfo> trackInfoAdapter;
    private int cadenceSteps = 0;
    private int cadenceStepsOffset = 0;

    private float strideLengthMeters = 0.0f;
    @Nullable
    private FloatAdapter averageStrideLengthAdapter;
    @Nullable
    private FloatAdapter strideLengthAdapter;


    public StatUpdating() {
        // does nothing
    }

    public void finishDM() {
        mDataMatrix.finish();
    }

    /**
     * Responsible for setting up the data matrix key callbacks.
     */
    private void setupDataMatrixKeys() {
        //***** Inputs into the workout controller ******
        distanceAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_DISTANCE_KM_FLOAT, distance -> updateVar(ConsoleWorkoutVariables.DISTANCE, distance));
        averageSpmInt = mDataMatrix.createIntAdapter(WorkoutKeys.DATA_AVERAGE_SPM_INT, averageSpm -> updateVar(ConsoleWorkoutVariables.AVERAGE_SPM, averageSpm));
        stepsAdapter = mDataMatrix.createIntAdapter(WorkoutKeys.DATA_STEPS_INT, steps -> updateVar(ConsoleWorkoutVariables.STEPS, steps));
        floorsAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_FLOORS_FLOAT, floors -> updateVar(ConsoleWorkoutVariables.FLOORS, floors));
        elevationAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT, elevation -> updateVar(ConsoleWorkoutVariables.ELEVATION, elevation));
        paceAdapter = mDataMatrix.createIntAdapter(WorkoutKeys.DATA_PACE_SEC_PER_KM_INT, pace -> updateVar(ConsoleWorkoutVariables.PACE, pace));
        averagePaceAdapter = mDataMatrix.createIntAdapter(WorkoutKeys.DATA_AVERAGE_PACE_SEC_PER_KM_INT, avgPace -> updateVar(ConsoleWorkoutVariables.AVERAGE_PACE, avgPace));
        workoutSpeedAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_WORKOUT_SPEED_KPH_FLOAT);
        workoutAverageSpeedAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT);
        caloriesAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_CALORIES_FLOAT, calories -> updateVar(ConsoleWorkoutVariables.CALORIES, (int) calories.floatValue()));
        activeCaloriesAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_ACTIVE_CALORIES_FLOAT);
        calPerHourAdapter = mDataMatrix.createIntAdapter(WorkoutKeys.DATA_CALORIES_PER_HOUR_INT);
        peakHeartRateAdapter = mDataMatrix.createIntAdapter(WorkoutKeys.DATA_PEAK_HEART_RATE_INT, peakHr -> updateVar(ConsoleWorkoutVariables.PEAK_HEART_RATE, peakHr));
        joulesAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_JOULES_FLOAT);
        averageWattsAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_AVERAGE_WATTS_FLOAT);
        averageRmpAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT);
        averageHeartRateAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_AVERAGE_HEART_RATE_FLOAT);
        averageInclineAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_AVERAGE_INCLINE_FLOAT);
        averageResistanceAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_AVERAGE_RESISTANCE_FLOAT);
        strideCountAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_STRIDE_COUNT_FLOAT);
        averageStrideLengthAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_AVERAGE_STRIDE_LENGTH_METERS_FLOAT);
        strideLengthAdapter = mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_STRIDE_LENGTH_METERS_FLOAT);

        mDataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, this::targetSpeedUpdated, true);
        mDataMatrix.createFloatAdapter(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, this::actualSpeedUpdated, true);
        mDataMatrix.createIntAdapter(HALKeys.DATA_RPM_INT, this::rpmUpdated, true);
        mDataMatrix.createIntAdapter(HALKeys.DATA_SPM_INT, this::spmUpdated, true);
        mDataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_INCLINE_FLOAT, this::inclineUpdated, true);
        mDataMatrix.createIntAdapter(HALKeys.DATA_RESISTANCE_INT, this::resistanceUpdated, true);
        mDataMatrix.createFloatAdapter(HALKeys.DATA_CALORIES_PER_SEC_FLOAT, this::calPerSecUpdated);
        mDataMatrix.createFloatAdapter(HALKeys.DATA_WATTS_FLOAT, this::wattsUpdated);
        mDataMatrix.createFloatAdapter(HALKeys.DATA_METS_FLOAT, this::metsUpdated);
        mDataMatrix.createIntAdapter(HALKeys.DATA_HEART_RATE_INT, this::hrUpdated);
        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_TARGET_HEART_RATE_PERCENT_FLOAT, percentThr -> updateVar(ConsoleWorkoutVariables.PERCENT_MAX_HEART_RATE, percentThr));
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TARGET_WATTS_INT, targetWatts -> updateVar(ConsoleWorkoutVariables.TARGET_WATTS, targetWatts));
        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_STRIDE_LENGTH_METERS_FLOAT, this::strideLengthUpdated);
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_START_SPLIT, key -> startSplit());
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_RESET_STATS, key -> resetStats());

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, key -> {
            mLapProgress = 0f;
            mLapStartMs = mWorkoutTime;
            mLapsCount = 0;
            DataMatrix.putInt(WorkoutKeys.DATA_LAP_BEST_TIME_MS_INT, 0);
        });

        mDataMatrix.createIntAdapter(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, status -> {
            int workoutStatus = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT);
            if (status == WatchState.Connected.ordinal() && (workoutStatus == 1)) {

                WorkoutDataContainer dataContainer = WorkoutDataContainer.getInstance();
                dataContainer.workoutSummaryCooldown.statsAverages.averageHeartRateHelper.reset();
                dataContainer.workoutSummaryCooldown.statsAverages.averageHeartRate = 0;
                dataContainer.workoutSummaryCooldown.statsPeaks.peakHeartRate = 0;

                dataContainer.workoutSummaryTotal.statsAverages.averageHeartRateHelper.reset();
                dataContainer.workoutSummaryTotal.statsAverages.averageHeartRate = 0;
                dataContainer.workoutSummaryTotal.statsPeaks.peakHeartRate = 0;

                dataContainer.workoutSummaryWarmup.statsAverages.averageHeartRateHelper.reset();
                dataContainer.workoutSummaryWarmup.statsAverages.averageHeartRate = 0;
                dataContainer.workoutSummaryWarmup.statsPeaks.peakHeartRate = 0;

                dataContainer.workoutSummaryWorkout.statsAverages.averageHeartRateHelper.reset();
                dataContainer.workoutSummaryWorkout.statsAverages.averageHeartRate = 0;
                dataContainer.workoutSummaryWorkout.statsPeaks.peakHeartRate = 0;
            }
        }, false);

        mDataMatrix.createParcelableAdapter(HALKeys.EVENT_RECEIVED_TREADMILL_CADENCE_PARCELABLE,
                ReceiveTreadmillCadence.class, cadenceInfo -> {

            float speedKph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, 0f);
            strideLengthMeters = CadenceFormulas.strideLength(speedKph, (float)cadenceInfo.getCadenceSpm(), true);

            // if the accumulated step got zeroed, update the offset
            if (cadenceSteps != 0 && cadenceInfo.getAccumulateSteps() < cadenceSteps) {
                cadenceStepsOffset += cadenceSteps;
            }
            cadenceSteps = cadenceInfo.getAccumulateSteps();
        });

        adaptersInit = true;
    }

    void initializeStatUpdating(WorkoutControllerCardio workoutController) {
        mWorkoutController = workoutController;
        setupDataMatrixKeys();
        resetStats();
        updateStatDataMatrixKeys();
    }

    /**
     * Updates the workout time to the passed in value. Updates stats from the last updated time
     * to the current updated time. Triggers logic to calculate and send all the necessary stats.
     *
     * @param timeMilliseconds The current workout time in milliseconds
     */
    void timeUpdated(int timeMilliseconds) {
        int diff = timeMilliseconds - mWorkoutTime;
        WorkoutSummary currentWorkoutStageSummary = WorkoutDataContainer.getInstance().getCurrentStateWorkoutSummary();
        WorkoutSummary totalSummary = WorkoutDataContainer.getInstance().workoutSummaryTotal;
        mWorkoutTime = timeMilliseconds;

        computeStatUpdates(currentWorkoutStageSummary, diff);
        computeStatUpdates(totalSummary, diff);
        if (DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT) == WorkoutKeys.Constants.WORKOUT_STAGE_WORKOUT) {
            computeStatUpdates(getCurrentIntervalSummary(), diff);
        }

        updateStatDataMatrixKeys();
        if (DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT) == WorkoutKeys.Constants.WORKOUT_STAGE_WORKOUT) {
            updateStatSplit(currentWorkoutStageSummary);
        }
        updateSprint8Stats(diff);
        updateLapsAndBestTime(currentWorkoutStageSummary);
    }

    private WorkoutSummary getCurrentIntervalSummary() {
        List<WorkoutSummary> intervals = WorkoutDataContainer.getInstance().getIntervalSummaries();
        int intervalNum = DataMatrix.getInt(WorkoutKeys.DATA_STAGE_INTERVAL_NUM_INT);

        // Add a new interval to the list if needed
        if (intervalNum >= intervals.size()) {
            intervals.add(new WorkoutSummary());
        }

        // This should not happen, but catch it just in case
        if (intervalNum < intervals.size()) {
            return intervals.get(intervalNum);
        } else {
            Log.e(TAG, "Invalid interval num when trying to get intervals stats!");
            return null;
        }
    }


    void forceGymKitUpdate() {
        WorkoutSummary workoutSummary = WorkoutDataContainer.getInstance().workoutSummaryTotal;
        StatsAccumulated totalStats = workoutSummary.statsAccumulated;

        putCalories(totalStats.calories);
        if (!adaptersInit) return;
        activeCaloriesAdapter.put(totalStats.activeCalories);
        averageHeartRateAdapter.put(workoutSummary.statsAverages.averageHeartRate);
    }

    void forceGymKitSummaryUpdate() {
        WorkoutSummary currentWorkoutStageSummary = WorkoutDataContainer.getInstance().getCurrentStateWorkoutSummary();
        WorkoutSummary totalSummary = WorkoutDataContainer.getInstance().workoutSummaryTotal;
        if (currentWorkoutStageSummary != null) computeStatUpdates(currentWorkoutStageSummary, 0);
        if (totalSummary != null) computeStatUpdates(totalSummary, 0);
    }

    private void updateStatSplit(WorkoutSummary currentSummary) {
        WorkoutSplit split = currentSummary.statsSplits.getLastSplit();
        if (split != null) {
            split.distanceKm = WorkoutDataContainer.getInstance().workoutSummaryTotal.statsAccumulated.distanceKm - split.distanceStartKm;
            split.timeMs = mWorkoutTime - split.timeStartMs;
        } else {
            WorkoutSplit newSplit = new WorkoutSplit(mWorkoutTime, WorkoutDataContainer.getInstance().workoutSummaryTotal.statsAccumulated.distanceKm);
            currentSummary.statsSplits.workoutSplits.add(newSplit);
            split = newSplit;
            DataMatrix.putParcelable(WorkoutKeys.DATA_SPLIT_LIST_PARCELABLE, currentSummary.statsSplits);
        }
        DataMatrix.putParcelable(WorkoutKeys.DATA_CURRENT_SPLIT_PARCELABLE, split);
    }

    /**
     * Track variables based on distance
     */
    // 100m
    private int m100LapStartMs = 0;
    private float m100LapProgress = 0f;
    // 400m
    private int mLapStartMs = 0;
    private float mLapProgress = 0f;
    private int mLapsCount = 0;

    /**
     * Tracks the lap count and the current best time for a single lap. This should be called after
     * the stats are tallied for that tick.
     *
     * @param totalSummary The total summary for the stats
     */
    private void updateLapsAndBestTime(WorkoutSummary totalSummary) {
        float distance = totalSummary.statsAccumulated.distanceKm;
        float nextLapProgress = distance % 0.4f; // Laps are always 400m
        int lapTime = mWorkoutTime - mLapStartMs;
        DataMatrix.putInt(WorkoutKeys.DATA_LAP_TIME_MS_INT, lapTime);

        // Calculation needs to done only if ui is premium
        if (ProductConfig.hasPremiumUi()) {
            TrackInfo trackData = DataMatrix.getParcelable(WorkoutKeys.DATA_TRACK_LAPS_PARCELABLE, TrackInfo.class);
            float nextLapProgress_100M = distance % 0.1f;
            int lapTime_100M = mWorkoutTime - m100LapStartMs;

            if (nextLapProgress_100M < m100LapProgress) {
                m100LapStartMs = mWorkoutTime;
                if (trackData != null) {
                    trackData.getTrackData().add(lapTime_100M);
                }
            }
            m100LapProgress = nextLapProgress_100M;
            DataMatrix.putParcelable(WorkoutKeys.DATA_TRACK_LAPS_PARCELABLE, trackData);
        }

        if (nextLapProgress < mLapProgress) {
            int currentBest = DataMatrix.getInt(WorkoutKeys.DATA_LAP_BEST_TIME_MS_INT);
            if (currentBest == 0 || lapTime < currentBest)
                DataMatrix.putInt(WorkoutKeys.DATA_LAP_BEST_TIME_MS_INT, lapTime);
            mLapStartMs = mWorkoutTime;
            mLapsCount++;
            DataMatrix.putInt(WorkoutKeys.DATA_LAP_COUNT_INT, mLapsCount);
        }
        mLapProgress = nextLapProgress;
        DataMatrix.putFloat(WorkoutKeys.DATA_LAP_DISTANCE_KM_FLOAT, mLapProgress);
    }

    /**
     * Calculates the stat values for accumulated, average, and peak stats
     *
     * @param workoutSummary The summary to calculate for
     * @param timeElapsedMs  The time elapsed to calculate for
     */
    private void computeStatUpdates(WorkoutSummary workoutSummary, int timeElapsedMs) {
        if (workoutSummary == null) { // Catch this case, should not happen though
            Log.e(TAG, "Cannot calculate null workout summary!");
            return;
        }

        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        float seconds = timeElapsedMs / 1000f;
        float minutes = seconds / 60f;
        float hours = (float) timeElapsedMs / (float) MS_TO_HOURS;
        StatsAccumulated accumulated = workoutSummary.statsAccumulated;
        StatsAverages averages = workoutSummary.statsAverages;
        float distance = mWorkoutMachineData.speedKph * hours;
        accumulated.distanceKm += distance;
        switch (config.machineClassType) {
            case Climbmill:
            case Stepper:
                float stepsIncreased = (float) mWorkoutMachineData.stepsPerMin * minutes;
                double elevationValue = stepsIncreased / config.STEPS_PER_METER;
                if (Double.isInfinite(elevationValue)) {
                    Log.w(TAG, "Calculated elevationValue is not finite: stepsIncreased = " + stepsIncreased + ", STEPS_PER_METER = " + config.STEPS_PER_METER);
                    break;
                }
                accumulated.elevationMeters += elevationValue;
                accumulated.steps += (float) mWorkoutMachineData.stepsPerMin * minutes;
                break;

            case Treadmill:
                double elevation = (distance * 1000 * Math.sin(Math.atan(mWorkoutMachineData.incline / 100.0)));
                if (Double.isInfinite(elevation)) {
                    Log.w(TAG, "Calculated elevation is not finite; incline = " + mWorkoutMachineData.incline);
                    break;
                }
                accumulated.elevationMeters += elevation;
                if (config.HAS_CADENCE) {
                    // LCB sends accumulated steps which resets to 0 when belt is stopped so we need to update the offset when
                    // workout is paused
                    accumulated.steps = cadenceStepsOffset + cadenceSteps;
                }
                break;
            case Elliptical:
            case RecumbentElliptical:
            case Ascent:
                accumulated.strideCount += ((mWorkoutMachineData.rpm / 60.0) * seconds) * 2;
                break;
        }

        accumulated.secondsStateRun += ((float) timeElapsedMs) / 1000f;
        accumulated.calories += mWorkoutMachineData.caloriesPerSec * seconds;
        accumulated.floors = config.STEPS_PER_FLOOR == 0 ? 0 : accumulated.steps / config.STEPS_PER_FLOOR;
        accumulated.joules += mWorkoutMachineData.watts * ((float) timeElapsedMs / 1000f);

        if (timeElapsedMs > 0) {
            averages.averageIncline = averages.averageInclineHelper.addValue(mWorkoutMachineData.incline);
            averages.averageSpeedKph = averages.averageSpeedKphHelper.addValue(mWorkoutMachineData.speedKph);
            if (mWorkoutMachineData.heartRate > 0) {
                averages.averageHeartRate = averages.averageHeartRateHelper.addValue(mWorkoutMachineData.heartRate);
            }

            averages.averageWatts = averages.averageWattsHelper.addValue(mWorkoutMachineData.watts);
            averages.averageMets = averages.averageMetsHelper.addValue(mWorkoutMachineData.mets);
            averages.averageResistance = averages.averageResistanceHelper.addValue(mWorkoutMachineData.resistance);
            averages.averageRpm = averages.averageRpmHelper.addValue(mWorkoutMachineData.rpm);
            averages.averageStepsPerMinute = averages.averageStepsPerMinuteHelper.addValue(mWorkoutMachineData.stepsPerMin);
            averages.averageStrideLengthMeters = averages.averageStrideLengthHelper.addValue(strideLengthMeters);
        }

        StatsPeaks peaks = workoutSummary.statsPeaks;
        peaks.peakSpeedKph = Math.max(peaks.peakSpeedKph, mWorkoutMachineData.speedKph);
        peaks.peakHeartRate = Math.max(peaks.peakHeartRate, mWorkoutMachineData.heartRate);
        peaks.peakIncline = Math.max(peaks.peakIncline, mWorkoutMachineData.incline);
        peaks.peakResistance = Math.max(peaks.peakResistance, mWorkoutMachineData.resistance);
        peaks.peakRpm = Math.max(peaks.peakRpm, mWorkoutMachineData.rpm);
        peaks.peakStepsPerMinute = Math.max(peaks.peakStepsPerMinute, mWorkoutMachineData.stepsPerMin);
        peaks.peakMets = Math.max(peaks.peakMets, mWorkoutMachineData.mets);
        peaks.peakWatts = Math.max(peaks.peakWatts, mWorkoutMachineData.watts);

        // gymkit override
        //TODO: Default key
        if (DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT) != STATUS_GYM_KIT_DISCONNECTED) {
            // heart rate is already handled in HAL
            int totalCalories = DataMatrix.getInt(HALKeys.DATA_GYM_KIT_CALORIES_TOTAL_INT);
            int activeCalories = DataMatrix.getInt(HALKeys.DATA_GYM_KIT_CALORIES_ACTIVE_INT);
            int averageHeartRate = DataMatrix.getInt(HALKeys.DATA_GYM_KIT_AVE_HEART_RATE_INT);
            if (totalCalories >= 0) {
                accumulated.calories = totalCalories - accumulated.caloriesOffset;
            }
            if (activeCalories >= 0) {
                accumulated.activeCalories = activeCalories - accumulated.activeCaloriesOffset;
            }
            if (averageHeartRate > 0) {
                averages.averageHeartRateHelper.forceValue(averageHeartRate, (int) accumulated.secondsStateRun);
                averages.averageHeartRate = averages.averageHeartRateHelper.getValue();
            }
        }
    }

    /**
     * Updates the data matrix keys associated with stats that appear in the system
     */
    private void updateStatDataMatrixKeys() {
        if (!adaptersInit) return;
        if (!DataMatrix.getBoolean(HALKeys.DATA_LOCK_MACHINE_DATA_FOR_TESTING_BOOL)) {
            WorkoutSummary workoutSummary = WorkoutDataContainer.getInstance().workoutSummaryTotal;
            StatsAccumulated totalStats = workoutSummary.statsAccumulated;

            distanceAdapter.put(totalStats.distanceKm);
            elevationAdapter.put(totalStats.elevationMeters);
            stepsAdapter.put((int) totalStats.steps);

            putCalories(totalStats.calories);

            peakHeartRateAdapter.put(workoutSummary.statsPeaks.peakHeartRate);
            floorsAdapter.put(totalStats.floors);

            //We don't need to send active calories when gymkit is disconnected
            //Stat computation logic should be reviewed when we look at the workout controller
            if (DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT) != STATUS_GYM_KIT_DISCONNECTED) {
                activeCaloriesAdapter.put(totalStats.activeCalories);
            }

            joulesAdapter.put(totalStats.joules);
            averageWattsAdapter.put(workoutSummary.statsAverages.averageWatts);
            averageRmpAdapter.put(workoutSummary.statsAverages.averageRpm);
            averageHeartRateAdapter.put(workoutSummary.statsAverages.averageHeartRate);
            averageInclineAdapter.put(workoutSummary.statsAverages.averageIncline);
            averageResistanceAdapter.put(workoutSummary.statsAverages.averageResistance);
            averageStrideLengthAdapter.put(workoutSummary.statsAverages.averageStrideLengthMeters);
            strideLengthAdapter.put(strideLengthMeters);
            strideCountAdapter.put(totalStats.strideCount);

            boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
            int averagePace = SpeedConvertUtil.getPaceInSeconds(workoutSummary.statsAverages.averageSpeedKph, isMetric);
            averagePaceAdapter.put(averagePace);
            workoutAverageSpeedAdapter.put(workoutSummary.statsAverages.averageSpeedKph);
            averageSpmInt.put((int) workoutSummary.statsAverages.averageStepsPerMinute);

            DataMatrix.putParcelable(WorkoutKeys.DATA_INTERVAL_LIST_PARCELABLE, WorkoutDataContainer.getInstance().intervalWorkoutSummaries);
        }
    }

    /**
     * We want to put the correct calorie reading into the DataMatrix so the stats pick up on it. This
     * will switch the calories stat between MyZone and standard calories.
     *
     * @param calories The total calories
     */
    private void putCalories(float calories) {
        if (!adaptersInit) return;
        if (DataMatrix.getBoolean(HALKeys.DATA_MY_ZONE_CONNECTED_BOOL)) {
            int myZoneCalories = DataMatrix.getInt(HALKeys.DATA_MYZONE_CALORIES_INT);
            caloriesAdapter.put(myZoneCalories);
        } else {
            caloriesAdapter.put(calories);
        }
    }

    /**
     * Updates the pace value based on the current speed value of the console
     */
    private void updatePace() {
        if (!adaptersInit) return;
        if (mWorkoutMachineData == null) return;
        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
        int pace = SpeedConvertUtil.getPaceInSeconds(mWorkoutMachineData.speedKph, isMetric);
        paceAdapter.put(pace);
        workoutSpeedAdapter.put(mWorkoutMachineData.speedKph);
    }

    /**
     * Updates sprint8 stat values if needed
     */
    private void updateSprint8Stats(int timeDiff) {
        if (mIsSprinting) {
            // Sweat score is: currentWatts * seconds / 100
            mSprint8SweatScore += mWorkoutMachineData.watts * (float) timeDiff / 100000f;
            DataMatrix.putFloat(WorkoutKeys.DATA_SPRINT8_SWEAT_SCORE_FLOAT, mSprint8SweatScore);
        }
    }

    void sendStatsToDataMatrix() {
        WorkoutDataContainer summaryContainer = WorkoutDataContainer.getInstance();
        SummaryData summaryData = new SummaryData(summaryContainer.workoutSummaryWarmup,
                summaryContainer.workoutSummaryWorkout,
                summaryContainer.workoutSummaryCooldown,
                summaryContainer.workoutSummaryTotal);
        DataMatrix.putParcelable(WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE, summaryData);
    }

    /**
     * Resets all listeners and variables for Track App
     */
    void resetTrackData() {
        m100LapStartMs = 0;
        m100LapProgress = 0;
        mLapProgress = 0;
        mLapStartMs = 0;
        mLapsCount = 0;

        DataMatrix.putInt(WorkoutKeys.DATA_LAP_TIME_MS_INT, 0);
        DataMatrix.putInt(WorkoutKeys.DATA_LAP_BEST_TIME_MS_INT, 0);
        DataMatrix.putInt(WorkoutKeys.DATA_LAP_COUNT_INT, 0);

        DataMatrix.putParcelable(WorkoutKeys.DATA_CURRENT_SPLIT_PARCELABLE, new WorkoutSplit(0, 0));
        DataMatrix.putParcelable(WorkoutKeys.DATA_SPLIT_LIST_PARCELABLE, new StatsSplits());
        DataMatrix.putFloat(WorkoutKeys.DATA_LAP_DISTANCE_KM_FLOAT, 0);
        DataMatrix.putParcelable(WorkoutKeys.DATA_TRACK_LAPS_PARCELABLE, new TrackInfo());
    }

    /**
     * Resets all the stats that are stored.
     */
    void resetStats() {
        if (!adaptersInit) return;
        resetTrackData();
        mWorkoutTime = 0;
        mSprint8SweatScore = 0;
        mIsSprinting = false;
        mSprint8Scores.sweatScores.clear();
        DataMatrix.putParcelable(WorkoutKeys.DATA_SPRINT8_SWEAT_SCORES_PARCELABLE, mSprint8Scores);
        DataMatrix.putFloat(WorkoutKeys.DATA_SPRINT8_SWEAT_SCORE_FLOAT, 0f);

        distanceAdapter.put(0);
        averageSpmInt.put(0);
        stepsAdapter.put(0);
        floorsAdapter.put(0);
        elevationAdapter.put(0);
        paceAdapter.put(0);
        averagePaceAdapter.put(0);
        workoutSpeedAdapter.put(0);
        workoutAverageSpeedAdapter.put(0);
        caloriesAdapter.put(0);
        activeCaloriesAdapter.put(0);
        calPerHourAdapter.put(0);
        peakHeartRateAdapter.put(0);
        joulesAdapter.put(0);
        averageWattsAdapter.put(0);
        averageRmpAdapter.put(0);
        averageHeartRateAdapter.put(0);
        averageInclineAdapter.put(0);
        averageResistanceAdapter.put(0);
        averageStrideLengthAdapter.put(0);

        cadenceSteps = 0;
        cadenceStepsOffset = 0;

        WorkoutDataContainer.getInstance().reset();

        DataMatrix.putFloat(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT, 0);
        DataMatrix.putFloat(WorkoutKeys.DATA_GOAL_VALUE_FLOAT, 0);
    }

    public void resetForSwitchWorkout() {
        mSprint8SweatScore = 0;
        mIsSprinting = false;
        mSprint8Scores.sweatScores.clear();
        WorkoutDataContainer.getInstance().workoutSummaryWorkout.statsSprint8.sweatScores.clear();
        WorkoutDataContainer.getInstance().workoutSummaryTotal.statsSprint8.sweatScores.clear();
        DataMatrix.putParcelable(WorkoutKeys.DATA_SPRINT8_SWEAT_SCORES_PARCELABLE, mSprint8Scores);
        DataMatrix.putFloat(WorkoutKeys.DATA_SPRINT8_SWEAT_SCORE_FLOAT, 0f);
    }

    private boolean mIsSprinting = false;
    private Sprint8SweatScores mSprint8Scores = new Sprint8SweatScores();
    private float mSprint8SweatScore = 0;

    /**
     * Calculates the sweat score for a single sprint. Pass in true to begin calculating the sweat
     * score with every time update. Pass in false to finish the calculations for that sprint.
     *
     * @param inSprint Whether a sprint is currently underway
     */
    public void startStopSweatScore(boolean inSprint) {
        if (inSprint != mIsSprinting) {
            mIsSprinting = inSprint;
            if (inSprint) {
                DataMatrix.putFloat(WorkoutKeys.DATA_SPRINT8_SWEAT_SCORE_FLOAT, 0f);
            } else {
                mSprint8Scores.addSweatScore(mSprint8SweatScore);
                DataMatrix.putParcelable(WorkoutKeys.DATA_SPRINT8_SWEAT_SCORES_PARCELABLE, mSprint8Scores);
                WorkoutSummary currentSummary = WorkoutDataContainer.getInstance().getCurrentStateWorkoutSummary();
                WorkoutSummary totalSummary = WorkoutDataContainer.getInstance().workoutSummaryTotal;
                currentSummary.statsSprint8.sweatScores.add(mSprint8SweatScore);
                totalSummary.statsSprint8.sweatScores.add(mSprint8SweatScore);
                mSprint8SweatScore = 0;
            }
        }
    }

    private void startSplit() {

        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_RUNNING) {
            WorkoutSummary summary = WorkoutDataContainer.getInstance().workoutSummaryWorkout;
            StatsSplits splitList = summary.statsSplits;
            float currentDistance = WorkoutDataContainer.getInstance().workoutSummaryTotal.statsAccumulated.distanceKm;
            WorkoutSplit split = new WorkoutSplit(mWorkoutTime, currentDistance);
            summary.statsSplits.workoutSplits.add(split);
            DataMatrix.putParcelable(WorkoutKeys.DATA_SPLIT_LIST_PARCELABLE, splitList);
            DataMatrix.putParcelable(WorkoutKeys.DATA_CURRENT_SPLIT_PARCELABLE, split);
        }
    }

    /**
     * Updates a variable in the workout controller with a value.
     *
     * @param var   The variable to set
     * @param value The value to set to
     */
    private void updateVar(String var, float value) {
        if (mWorkoutController == null) return;
        mWorkoutController.updateAndValidateValue(var, value);
    }

    private void targetSpeedUpdated(float speed) {
        if (mWorkoutController == null) return;
        if (isTreadmill()) {
            mWorkoutMachineData.speedKph = speed;
            updatePace();
            mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.SPEED_IN, speed);
        }
    }

    private void actualSpeedUpdated(float speed) {
        if (mWorkoutController == null) return;
        if (!isTreadmill()) {
            mWorkoutMachineData.speedKph = speed;
            updatePace();
            mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.SPEED_IN, speed);
        }
    }

    private void rpmUpdated(int rpm) {
        if (mWorkoutController == null) return;
        mWorkoutMachineData.rpm = rpm;
        mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.RPM, (float) rpm);
    }

    private void spmUpdated(int spm) {
        if (mWorkoutController == null) return;
        mWorkoutMachineData.stepsPerMin = spm;
        mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.SPM, (float) spm);
    }

    private void inclineUpdated(float incline) {
        if (mWorkoutController == null) return;
        mWorkoutMachineData.incline = incline;
        mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.INCLINE_IN, incline);
    }

    private void resistanceUpdated(int resistance) {
        if (mWorkoutController == null) return;
        mWorkoutMachineData.resistance = resistance;
        mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.RESISTANCE_IN, (float) resistance);
    }

    private void calPerSecUpdated(float caloriesPerSecond) {
        if (!adaptersInit) return;
        if (mWorkoutController == null) return;
        mWorkoutMachineData.caloriesPerSec = caloriesPerSecond;
        int caloriesPerHour = Math.round(caloriesPerSecond * 3600f);
        calPerHourAdapter.put(caloriesPerHour);
        mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.CALORIES_PER_HOUR, caloriesPerHour);
    }

    private void wattsUpdated(float watts) {
        if (mWorkoutController == null) return;
        mWorkoutMachineData.watts = watts;
        mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.WATTS, watts);
    }

    private void metsUpdated(float mets) {
        if (mWorkoutController == null) return;
        mWorkoutMachineData.mets = mets;
        mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.METS, mets);
    }

    private void strideLengthUpdated(float strideLength) {
        if (mWorkoutController == null) return;
        mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.STRIDE_LENGTH_METERS, strideLength);
    }

    private void hrUpdated(int heartRate) {
        if (mWorkoutController == null) return;
        mWorkoutMachineData.heartRate = heartRate;
        mWorkoutController.updateAndValidateValue(ConsoleWorkoutVariables.HEART_RATE, (float) heartRate);
    }

    private boolean isTreadmill() {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        return config.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill;
    }
}
