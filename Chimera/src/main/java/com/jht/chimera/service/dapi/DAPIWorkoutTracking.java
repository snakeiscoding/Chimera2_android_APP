package com.jht.chimera.service.dapi;

import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_PAUSE;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_RUNNING;
import static com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys.Constants.RUN_STATE_STOP;
import static com.jht.chimera.service.dapi.DAPIService.FIELD_NA_PLACEHOLDER;

import android.util.Log;

import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.math.SpeedToPace;
import com.jht.chimera.lib.common.stats.SummaryData;
import com.jht.chimera.lib.common.stats.WorkoutSummary;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.data.sprint8.Sprint8SweatScores;
import com.jhtp.dapiservice.DapiResult;
import com.jhtp.dapiservice.comm.CborCommands;
import com.jhtp.dapiservice.comm.DapiComm;
import com.jhtp.dapiservice.um.GenderIndentity;
import com.jhtp.dapiservice.wt.DapiWorkout;
import com.jhtp.dapiservice.wt.IDapiWorkoutTrackingResponse;
import com.jhtp.dapiservice.wt.cardio.DapiCardioComplete;
import com.jhtp.dapiservice.wt.cardio.DapiCardioSnapshot;
import com.jhtp.dapiservice.wt.cardio.DapiWorkoutSummaryCardio;
import com.jhtp.dapiservice.wt.strength.DapiWorkoutSummaryStrength;

import java.util.ArrayList;
import java.util.List;

public class DAPIWorkoutTracking implements IDapiWorkoutTrackingResponse {
    private final String TAG = "DAPIWorkoutTracking";
    DataMatrix mDataMatrix = new DataMatrix();

    private boolean mWorkoutStarted;
    private int mWorkoutStartTime;
    private int mSplitCount;
    private int mLastElapsedTimeSec;
    private static ArrayList<DapiCardioSnapshot> mSnapshotList = new ArrayList<DapiCardioSnapshot>(30);
    private static final Object notifySendSnapshots = new Object();

    private static class SingletonHolder {
        private static final DAPIWorkoutTracking INSTANCE = new DAPIWorkoutTracking();
    }

    public static DAPIWorkoutTracking getInstance() {
        return DAPIWorkoutTracking.SingletonHolder.INSTANCE;
    }

    private DAPIWorkoutTracking() {
        reset();
        snapshotThread.start();

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
            switch (value) {
                case RUN_STATE_STOP:
                    Log.e(TAG, "RUN_STATE_STOP");
                    if (mWorkoutStarted) {
                        Log.e(TAG, "mWorkoutStarted RUN_STATE_STOP");
                        mWorkoutStarted = false;
                        kickSnapshotThread();
                        sendCardioComplete();
                        reset();
                    }
                    break;
                case RUN_STATE_RUNNING:
                    Log.e(TAG, "RUN_STATE_RUNNING");
                    if (!mWorkoutStarted) {
                        Log.e(TAG, "mWorkoutStarted RUN_STATE_RUNNING");
                        mWorkoutStarted = true;
                        mWorkoutStartTime = (int) (System.currentTimeMillis() / 1000L);
                        // so the survey never pairs with the previous workout's 701
                        DataMatrix.putString(WorkoutKeys.DATA_LAST_PROGRAM_UUID_STRING, "");
                    }
                    break;
                case RUN_STATE_PAUSE:
                    Log.e(TAG, "RUN_STATE_PAUSE");
                    break;
            }
        }, false);

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value -> {
            int workoutElapsedTimeSec = Math.round(value / 1000);
            if (workoutElapsedTimeSec != mLastElapsedTimeSec) {
                mLastElapsedTimeSec = workoutElapsedTimeSec;
                DapiCardioSnapshot snapshot = pollWorkoutData();
                if (snapshot.timeInState > 0) {
                    synchronized (mSnapshotList) {
                        mSnapshotList.add(snapshot);
                        if (mSnapshotList.size() >= 90) {
                            kickSnapshotThread();
                        }
                    }
                }
            }
        }, false);
    }

    private Thread snapshotThread = new Thread(() -> {
        DapiCardioSnapshot[] dapiCardioSnapshots = null;
        while (!Thread.currentThread().isInterrupted()) {
            synchronized (mSnapshotList) {
                if (mSnapshotList.size() > 0) {
                    // is there any max size of snapshots that can be sent? what if this backs up?
                    addPlaceHolderForWorkoutSnapshot(mSnapshotList);
                    dapiCardioSnapshots = new DapiCardioSnapshot[mSnapshotList.size()];
                    dapiCardioSnapshots = mSnapshotList.toArray(dapiCardioSnapshots);
                    mSnapshotList.clear();
                }
            }
            // Only send workout snapshots based off of MES Asset Management setting
            switch (DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WORKOUT_SNAPSHOT_SETTING_INT)) {
                case SettingKeys.AssetManagementConstants.WORKOUT_SNAPSHOT_SETTING_ON: // Always send snapshots (On)
                    sendWorkoutSnapshots(dapiCardioSnapshots);
                    break;
                case SettingKeys.AssetManagementConstants.WORKOUT_SNAPSHOT_SETTING_OFF: // Never send snapshots (Off)
                    break;
                case SettingKeys.AssetManagementConstants.WORKOUT_SNAPSHOT_SETTING_LOGGED_IN_ONLY: // Only send logged-in user's snapshots (Logged-In Only)
                default:
                    if (!DapiUser.isGuestUser()) sendWorkoutSnapshots(dapiCardioSnapshots);
                    break;
            }
            dapiCardioSnapshots = null;

            synchronized (notifySendSnapshots) {
                // wait throws InterruptedException, so we won't set a time out as stopping thread should bail
                try { notifySendSnapshots.wait(); }
                catch (Exception ignored) {}
            }
        }
    }, "snapshotThread");
    private void kickSnapshotThread() {
        synchronized (notifySendSnapshots) { notifySendSnapshots.notify(); }
    }
    /*private void stopSnapshotThread() {
        snapshotThread.interrupt();
    }*/

    void reset() {
        mWorkoutStartTime = 0;
        mSplitCount = 1;
        mLastElapsedTimeSec = -1;
        synchronized (mSnapshotList) { mSnapshotList.clear(); }
    }

    void sendCardioComplete() {
        try {
            WorkoutSummary summaryWarmup = getSummaryWarmup();
            WorkoutSummary summaryWorkout = getSummaryWorkout();
            WorkoutSummary summaryCooldown = getSummaryCooldown();
            WorkoutSummary summaryTotal = getSummaryTotal();
            sendCardioCompleteMessage(summaryWarmup, summaryWorkout, summaryCooldown, summaryTotal);
        } catch  (Exception e) {
            Log.e(TAG, "sendCardioComplete failed", e);
        }
    }

    WorkoutSummary getSummaryCooldown() {
        SummaryData summaryData = DataMatrix.getParcelable(WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE, SummaryData.class);
        if (summaryData == null) return null;
        return summaryData.cooldownSummary;
    }

    WorkoutSummary getSummaryWorkout() {
        SummaryData summaryData = DataMatrix.getParcelable(WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE, SummaryData.class);
        if (summaryData == null) return null;
        return summaryData.workoutSummary;
    }

    WorkoutSummary getSummaryWarmup() {
        SummaryData summaryData = DataMatrix.getParcelable(WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE, SummaryData.class);
        if (summaryData == null) return null;
        return summaryData.warmupSummary;
    }

    WorkoutSummary getSummaryTotal() {
        SummaryData summaryData = DataMatrix.getParcelable(WorkoutKeys.DATA_WORKOUT_SUMMARY_INFO_PARCELABLE, SummaryData.class);
        if (summaryData == null) return null;
        return summaryData.totalSummary;
    }

    private DapiCardioSnapshot pollWorkoutData() {
        DapiCardioSnapshot workoutData = new DapiCardioSnapshot();
        workoutData.workoutType = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        workoutData.workoutStartTime = mWorkoutStartTime;
        workoutData.distance = DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT);
        workoutData.resistance = DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
        workoutData.heartRate = DataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT);
        workoutData.watts = (int) DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT);
        workoutData.workoutState = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT);
        workoutData.timeInState = DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
        workoutData.lapIndex = DataMatrix.getInt(WorkoutKeys.DATA_LAP_COUNT_INT) +1;//Lap index is +1 from laps completed
        workoutData.splitIndex = mSplitCount;
        if(FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill)) {
            workoutData.kph = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT);
        }
        else {
            workoutData.kph = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
        }

        workoutData.incline = DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT);
        workoutData.totalCalories = (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
        workoutData.mets = DataMatrix.getFloat(HALKeys.DATA_METS_FLOAT);
        workoutData.rpm = DataMatrix.getInt(HALKeys.DATA_RPM_INT);
        workoutData.programLevel = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_LEVEL_INT);
        workoutData.targetHeartRate = DataMatrix.getInt(WorkoutKeys.DATA_TARGET_HEART_RATE_INT);
        workoutData.targetWatts = DataMatrix.getInt(WorkoutKeys.DATA_TARGET_WATTS_INT);
        workoutData.totalFloors = (int) DataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT);
        workoutData.totalSteps = DataMatrix.getInt(WorkoutKeys.DATA_STEPS_INT);
        workoutData.elevationMeters = DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT);
        workoutData.vo2 = DataMatrix.getFloat(WorkoutKeys.DATA_VO2_FLOAT);
        workoutData.machineSubtype = "cardio"; //NON-NLS

        return workoutData;
    }


    private void sendWorkoutSnapshots(DapiCardioSnapshot[] dapiCardioSnapshots) {
        try {
            if (!AddressHelper.isNetworkConnected(ContextUtil.getApplicationContext()) || dapiCardioSnapshots == null) return;

            Log.e(TAG, "sendWorkoutSnapshots");
            String userToken = DAPIUserManagement.getInstance().getUserToken();
            DapiComm.Instance.WorkoutTracking.cardioWorkoutSnapshot(userToken, DapiUtil.getMachineId(), dapiCardioSnapshots);
        } catch (Exception e) {
            Log.e(TAG, "sendWorkoutSnapshots failed", e);
        }
    }

    private void addPlaceHolderForWorkoutSnapshot(List<DapiCardioSnapshot> snapshots){

        for(DapiCardioSnapshot snapshot: snapshots){

            if(snapshot.heartRate <= 0) {
                snapshot.heartRate = FIELD_NA_PLACEHOLDER;
            }

            if(snapshot.programLevel <= 0){
                snapshot.programLevel = FIELD_NA_PLACEHOLDER;
            }

            int id = snapshot.workoutType;
            if(!(id == 44 || id == 45 || id == 46)){
                snapshot.targetHeartRate = FIELD_NA_PLACEHOLDER;
            }

            if(id != 20){
                snapshot.targetWatts = FIELD_NA_PLACEHOLDER;
            }

            if(!(id == 38 || id == 30 || id == 52 || id == 53)){
                snapshot.vo2 = FIELD_NA_PLACEHOLDER;
            }

            String strFrameType = DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING);
            if(strFrameType.toLowerCase().contains("treadmill")) {
                snapshot.resistance = FIELD_NA_PLACEHOLDER;
                snapshot.rpm = FIELD_NA_PLACEHOLDER;
                snapshot.targetWatts = FIELD_NA_PLACEHOLDER;
                snapshot.totalFloors = FIELD_NA_PLACEHOLDER;
                snapshot.totalSteps = FIELD_NA_PLACEHOLDER;
            }else if(strFrameType.toLowerCase().contains("bike")){
                snapshot.incline = FIELD_NA_PLACEHOLDER;
                snapshot.totalFloors = FIELD_NA_PLACEHOLDER;
                snapshot.totalSteps = FIELD_NA_PLACEHOLDER;
                snapshot.elevationMeters = FIELD_NA_PLACEHOLDER;
            }else if(strFrameType.toLowerCase().contains("ascent")){
                snapshot.totalFloors = FIELD_NA_PLACEHOLDER;
                snapshot.totalSteps = FIELD_NA_PLACEHOLDER;
                snapshot.elevationMeters = FIELD_NA_PLACEHOLDER;
            }else if(strFrameType.toLowerCase().contains("elliptical")){
                snapshot.totalFloors = FIELD_NA_PLACEHOLDER;
                snapshot.totalSteps = FIELD_NA_PLACEHOLDER;
                snapshot.elevationMeters = FIELD_NA_PLACEHOLDER;
            }else if(strFrameType.toLowerCase().contains("climbmill")){
                snapshot.distance = FIELD_NA_PLACEHOLDER;
                snapshot.lapIndex = FIELD_NA_PLACEHOLDER;
                snapshot.kph = FIELD_NA_PLACEHOLDER;
                snapshot.incline = FIELD_NA_PLACEHOLDER;
                snapshot.targetWatts = FIELD_NA_PLACEHOLDER;
            }else if(strFrameType.toLowerCase().contains("stepper")){
                snapshot.distance = FIELD_NA_PLACEHOLDER;
                snapshot.lapIndex = FIELD_NA_PLACEHOLDER;
                snapshot.kph = FIELD_NA_PLACEHOLDER;
                snapshot.incline = FIELD_NA_PLACEHOLDER;
                snapshot.targetWatts = FIELD_NA_PLACEHOLDER;
            }
        }
    }

    private void sendCardioCompleteMessage(WorkoutSummary warmupInfo, WorkoutSummary workoutInfo,
                                           WorkoutSummary cooldownInfo, WorkoutSummary totalInfo) {


        if (!AddressHelper.isNetworkConnected(ContextUtil.getApplicationContext()) &&
           (!DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_SEND_WORKOUT_DATA_TO_ALTERNATE_ENDPOINT_BOOL))) {
            return;
        }

        Log.e(TAG, "sendCardioCompleteMessage");
        DapiCardioComplete cardioComplete = new DapiCardioComplete();
        cardioComplete.workoutType = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        // This is not an actual UUID and has embedded metadata. Too bad!
        cardioComplete.programUuid = ProgramUuidResolver.resolve();
        DataMatrix.putString(WorkoutKeys.DATA_LAST_PROGRAM_UUID_STRING, cardioComplete.programUuid);

        // Program Description is for the VA/Landmarks Title because the Workout Id is the same for all of workouts
        // within each group respectively, so we need the title to identify which workout it is within the category
        cardioComplete.programDescription = DataMatrix.getString(WorkoutKeys.DATA_SELECTED_VA_OR_LANDMARK_TITLE_STRING);
        cardioComplete.workoutStartTime = mWorkoutStartTime;
        cardioComplete.workoutTime = (int) workoutInfo.statsAccumulated.secondsStateRun;
        cardioComplete.warmUpTime = (int) warmupInfo.statsAccumulated.secondsStateRun;
        cardioComplete.coolDownTime = (int) cooldownInfo.statsAccumulated.secondsStateRun;
        cardioComplete.distanceKm = totalInfo.statsAccumulated.distanceKm;

        if(totalInfo.statsAverages.averageSpeedKph < 0.1) {
            cardioComplete.averagePace = 0;
        }
        else {
            cardioComplete.averagePace = Math.round(SpeedToPace.KphToSecPerKm(totalInfo.statsAverages.averageSpeedKph));
        }
        cardioComplete.peakResistance = totalInfo.statsPeaks.peakResistance;
        cardioComplete.averageResistance = totalInfo.statsAverages.averageResistance;
        cardioComplete.peakHeartRate = totalInfo.statsPeaks.peakHeartRate;
        cardioComplete.averageHeartRate = Math.round(totalInfo.statsAverages.averageHeartRate);
        cardioComplete.calories = totalInfo.statsAccumulated.calories;
        cardioComplete.peakWatts = totalInfo.statsPeaks.peakWatts;
        cardioComplete.averageWatts = totalInfo.statsAverages.averageWatts;
        cardioComplete.totalFloors = totalInfo.statsAccumulated.floors;
        cardioComplete.totalSteps = Math.round(totalInfo.statsAccumulated.steps);
        cardioComplete.totalElevation = totalInfo.statsAccumulated.elevationMeters;
        cardioComplete.peakKph = totalInfo.statsPeaks.peakSpeedKph;
        cardioComplete.averageKph = totalInfo.statsAverages.averageSpeedKph;
        cardioComplete.machineSubtype = "cardio"; //NON-NLS
        cardioComplete.weightKg = DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT);

        boolean canSendFitnessScore = false;
        boolean canSendSweatScore = false;
        int workoutID = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
        if (    workoutID == 30 || //Gerkin Fitness Test = 30
                workoutID == 33 || //Army Fitness Test = 33
                workoutID == 34 || //Navy Fitness Test = 34
                workoutID == 35 || //Air Force Fitness Test = 35
                workoutID == 36 || //Marines Fitness Test = 36
                workoutID == 37 || //PEB Fitness Test = 37
                workoutID == 38 || //Cooper Fitness Test = 38
                workoutID == 51 || //CPAT Test = 51
                workoutID == 52 || //WFI Test = 52
                workoutID == 53 || //Submaxmial Test = 53
                workoutID == 56 || //Stepper Fitness Test = 56
                workoutID == 72 || //8 Minute FTP test (Athena)
                workoutID == 75 || //20 Minute FTP test (Athena)
                workoutID == 76    //Max HR Test/Estimate (Athena)
        ) {
            canSendFitnessScore = true; // we send even if the user fails or quits
            // The fitness score can be found in one of 3 variables depending on test and gender.
            int genderIdentity = DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT);
            float fitnessTestScore = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FLOAT);

            if (fitnessTestScore <= 0) {
                if (genderIdentity == GenderIndentity.Male.Value)
                    fitnessTestScore = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_MALE_FLOAT);
                else
                    fitnessTestScore = DataMatrix.getFloat(WorkoutKeys.DATA_FITNESS_TEST_SCORE_FEMALE_FLOAT);
            }

            cardioComplete.fitnessTestScore = (int) Math.max (fitnessTestScore, 0);
        } else {
            cardioComplete.fitnessTestScore = FIELD_NA_PLACEHOLDER;  Log.d("DAPI_D", "NOT fitnessTestScore float : ");
        }
        cardioComplete.sendFitnessScore = canSendFitnessScore;
        Sprint8SweatScores sprint8Parc = DataMatrix.getParcelable(WorkoutKeys.DATA_SPRINT8_SWEAT_SCORES_PARCELABLE, Sprint8SweatScores.class);
        int totalSweatScore = 0;
        if (sprint8Parc == null || sprint8Parc.sweatScores.size() == 0) {
            cardioComplete.sprint1SweatScore = FIELD_NA_PLACEHOLDER;
            cardioComplete.sprint2SweatScore = FIELD_NA_PLACEHOLDER;
            cardioComplete.sprint3SweatScore = FIELD_NA_PLACEHOLDER;
            cardioComplete.sprint4SweatScore = FIELD_NA_PLACEHOLDER;
            cardioComplete.sprint5SweatScore = FIELD_NA_PLACEHOLDER;
            cardioComplete.sprint6SweatScore = FIELD_NA_PLACEHOLDER;
            cardioComplete.sprint7SweatScore = FIELD_NA_PLACEHOLDER;
            cardioComplete.sprint8SweatScore = FIELD_NA_PLACEHOLDER;
        } else {
            if (sprint8Parc.sweatScores.get(0) == 0) {
                cardioComplete.sprint1SweatScore = FIELD_NA_PLACEHOLDER;
            } else {
                cardioComplete.sprint1SweatScore = sprint8Parc.sweatScores.get(0);
                totalSweatScore += cardioComplete.sprint1SweatScore;
            }
            if ( sprint8Parc.sweatScores.size() < 2 || sprint8Parc.sweatScores.get(1) == 0) {
                cardioComplete.sprint2SweatScore = FIELD_NA_PLACEHOLDER;
            } else {
                cardioComplete.sprint2SweatScore = sprint8Parc.sweatScores.get(1);
                totalSweatScore += cardioComplete.sprint2SweatScore;
            }
            if (sprint8Parc.sweatScores.size() < 3 || sprint8Parc.sweatScores.get(2) == 0) {
                cardioComplete.sprint3SweatScore = FIELD_NA_PLACEHOLDER;
            } else {
                cardioComplete.sprint3SweatScore = sprint8Parc.sweatScores.get(2);
                totalSweatScore += cardioComplete.sprint3SweatScore;
            }
            if (sprint8Parc.sweatScores.size() < 4 || sprint8Parc.sweatScores.get(3) == 0) {
                cardioComplete.sprint4SweatScore = FIELD_NA_PLACEHOLDER;
            } else {
                cardioComplete.sprint4SweatScore = sprint8Parc.sweatScores.get(3);
                totalSweatScore += cardioComplete.sprint4SweatScore;
            }
            if (sprint8Parc.sweatScores.size() < 5 || sprint8Parc.sweatScores.get(4) == 0) {
                cardioComplete.sprint5SweatScore = FIELD_NA_PLACEHOLDER;
            } else {
                cardioComplete.sprint5SweatScore = sprint8Parc.sweatScores.get(4);
                totalSweatScore += cardioComplete.sprint5SweatScore;
            }
            if (sprint8Parc.sweatScores.size() < 6 || sprint8Parc.sweatScores.get(5) == 0) {
                cardioComplete.sprint6SweatScore = FIELD_NA_PLACEHOLDER;
            } else {
                cardioComplete.sprint6SweatScore = sprint8Parc.sweatScores.get(5);
                totalSweatScore += cardioComplete.sprint6SweatScore;
            }
            if (sprint8Parc.sweatScores.size() < 7 || sprint8Parc.sweatScores.get(6) == 0) {
                cardioComplete.sprint7SweatScore = FIELD_NA_PLACEHOLDER;
            } else {
                cardioComplete.sprint7SweatScore = sprint8Parc.sweatScores.get(6);
                totalSweatScore += cardioComplete.sprint7SweatScore;
            }
            if (sprint8Parc.sweatScores.size() < 8 || sprint8Parc.sweatScores.get(7) == 0) {
                cardioComplete.sprint8SweatScore = FIELD_NA_PLACEHOLDER;
            } else {
                cardioComplete.sprint8SweatScore = sprint8Parc.sweatScores.get(7);
                totalSweatScore += cardioComplete.sprint8SweatScore;
            }
        }
        int workoutLevel = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_LEVEL_INT);
        if (totalSweatScore > 0) {
            canSendSweatScore = true;
            // 1-20 sprint8 levels, 21 is a custom sprint8 workout.  if out of bounds for some reason go to default
            if (workoutLevel < 1 || workoutLevel > 21) {
                cardioComplete.sprint8ProgramLevel = 1;//default is 1
            } else {
                cardioComplete.sprint8ProgramLevel = workoutLevel;
            }
        } else {
            totalSweatScore = FIELD_NA_PLACEHOLDER;
            cardioComplete.sprint8ProgramLevel = 1;//default is 1
        }
        cardioComplete.totalSweatScore = totalSweatScore;
        cardioComplete.sendSprint8Results = canSendSweatScore;

        // unsure of the upper bound for all of our workouts that run levels so rn it is 30
        if (workoutLevel < 1 || workoutLevel > 30) {
            cardioComplete.programLevel = 1;//default level is 1, for workouts that don't have levels
        } else {
            cardioComplete.programLevel = workoutLevel;
        }
        String workoutOriginatorString = DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_ORIGINATOR_STRING);
        if (workoutOriginatorString.equals("")) {
            switch (workoutID) {
                case WorkoutIds.IFIT:
                    cardioComplete.workoutOriginator = "IFIT";//NON-NLS default string
                    break;
                case WorkoutIds.YMCA_360:
                    cardioComplete.workoutOriginator = "YMCA360";//NON-NLS default string
                    break;
                case WorkoutIds.EGYM:
                    cardioComplete.workoutOriginator = "EGYM";//NON-NLS default string
                    break;
                default:
                    cardioComplete.workoutOriginator = "MATRIX";//NON-NLS default string
                    break;
            }
        } else {
            cardioComplete.workoutOriginator = workoutOriginatorString;//NON-NLS default string
        }
//        Log.d("DAPI_D", "------------------------------------------");
//        Log.d("DAPI_D", "workoutID = "+workoutID);
//        Log.d("DAPI_D", "sendFitnessOrSweatScore = "+cardioComplete.sendFitnessOrSweatScore);
//        Log.d("DAPI_D", "sprint1SweatScore: "+cardioComplete.sprint1SweatScore);
//        Log.d("DAPI_D", "sprint2SweatScore: "+cardioComplete.sprint2SweatScore);
//        Log.d("DAPI_D", "sprint3SweatScore: "+cardioComplete.sprint3SweatScore);
//        Log.d("DAPI_D", "sprint4SweatScore: "+cardioComplete.sprint4SweatScore);
//        Log.d("DAPI_D", "sprint5SweatScore: "+cardioComplete.sprint5SweatScore);
//        Log.d("DAPI_D", "sprint6SweatScore: "+cardioComplete.sprint6SweatScore);
//        Log.d("DAPI_D", "sprint7SweatScore: "+cardioComplete.sprint7SweatScore);
//        Log.d("DAPI_D", "sprint8SweatScore: "+cardioComplete.sprint8SweatScore);
//        Log.d("DAPI_D", "totalSweatScore: "+cardioComplete.totalSweatScore);
//        Log.d("DAPI_D", "fitnessTestScore: "+cardioComplete.fitnessTestScore);
        addPlaceHolderForWorkoutComplete(cardioComplete);

        if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_SEND_WORKOUT_DATA_TO_ALTERNATE_ENDPOINT_BOOL)) {
            // Send workout tracking info to alternate endpoint defined in M/E/S.
            NonDAPIWorkoutTrackingUtil.sendNonDAPIWorkoutSummary(cardioComplete);
        } else {
            String userToken = DAPIUserManagement.getInstance().getUserToken();
            DapiComm.Instance.WorkoutTracking.cardioCompleteSnapshot(userToken, DapiUtil.getMachineId(), cardioComplete, false);
        }
    }

    private void addPlaceHolderForWorkoutComplete(DapiCardioComplete dapiCardioComplete){

        if(dapiCardioComplete.peakHeartRate <= 0){
            dapiCardioComplete.peakHeartRate = FIELD_NA_PLACEHOLDER;
        }

        if(dapiCardioComplete.averageHeartRate <= 0){
            dapiCardioComplete.averageHeartRate = FIELD_NA_PLACEHOLDER;
        }

        switch (FrameConfigManager.getInstance().getFrameConfig().machineClassType) {
            case Treadmill:
                dapiCardioComplete.peakResistance = FIELD_NA_PLACEHOLDER;
                dapiCardioComplete.averageResistance = FIELD_NA_PLACEHOLDER;
                dapiCardioComplete.totalFloors = FIELD_NA_PLACEHOLDER;
                dapiCardioComplete.totalSteps = FIELD_NA_PLACEHOLDER;
                break;
            case Bike:
            case SpinBike:
            case Ascent:
            case Elliptical:
            case RecumbentElliptical:
                dapiCardioComplete.totalFloors = FIELD_NA_PLACEHOLDER;
                dapiCardioComplete.totalSteps = FIELD_NA_PLACEHOLDER;
                dapiCardioComplete.totalElevation = FIELD_NA_PLACEHOLDER;
                break;
            case Climbmill:
            case Stepper:
                dapiCardioComplete.peakKph = FIELD_NA_PLACEHOLDER;
                dapiCardioComplete.averageKph = FIELD_NA_PLACEHOLDER;
                dapiCardioComplete.distanceKm = FIELD_NA_PLACEHOLDER;
                break;
        }
    }

    @Override
    public void wtHandler(CborCommands cborCommands, DapiResult dapiResult) {

    }

    @Override
    public void wtDapiWorkoutsHandler(CborCommands cborCommands, DapiResult dapiResult, DapiWorkout[] dapiWorkouts) {
        /*
        Log.d("bdc_wt_history", "Got workouts " + dapiWorkouts.length);
        String userId = DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING);

        if(dapiWorkouts.length > 0) {
            for (DapiWorkout workout : dapiWorkouts) {
                Log.d("bdc_wt_history", "Get workout " +  workout.WorkoutId + " " + workout.StartTime.Year + "/" + workout.StartTime.Month + "/" + workout.StartTime.Day + " " + workout.StartTime.Hour + ":" + workout.StartTime.Minute + " " + workout.StartTime.Second);
                DapiComm.Instance.WorkoutTracking.getWorkoutSummary(userId, workout.WorkoutId);
            }
        }
         */

    }

    @Override
    public void wtSummaryCardioHandler(CborCommands cborCommands, DapiResult dapiResult, DapiWorkoutSummaryCardio dapiWorkoutSummaryCardio) {
        //Log.d("bdc_wt_history", "Got workout " + dapiWorkoutSummaryCardio.WorkoutId);
    }

    @Override
    public void wtSummaryStrengthHandler(CborCommands cborCommands, DapiResult dapiResult, DapiWorkoutSummaryStrength dapiWorkoutSummaryStrength) {

    }

    @Override
    public void wtGetProgramWorkoutsHandler(CborCommands cborCommands, DapiResult dapiResult, DapiWorkoutSummaryStrength[] dapiWorkoutSummaryStrengths) {

    }

}
