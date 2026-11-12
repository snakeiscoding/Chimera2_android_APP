package com.jht.chimera.service.hal.lib.hardware.heartrate;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.math.AverageHelper;
import com.jht.chimera.lib.integrations.myzone.MyZoneLib;
import com.jht.chimera.lib.integrations.myzone.TimeInZones;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.HeartRateSource;
import com.jht.oauthlib.auth.BksCredentialProvider;
import com.jhtp.dapiservice.an.types.ButtonPressType;

import org.jetbrains.annotations.NonNls;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressLint("MissingPermission")
class MyZoneReader implements IHeartRateReader {
    @NonNls private static final String MYZONE_PREFIX = "MYZONE-";
    @NonNls private static final String SESSION_ID = "MATRIX";
    private volatile int heartRate;
    private AverageHelper maxHRaverager = new AverageHelper(10);
    private int loopToSendCounter;
    private final IHeartRateUpdated heartRateUpdated;

    private final MyZoneLib myZoneLib;

    private int beltId;
    private long workoutStartTime;
    private boolean isConnected = false; // use this within the class and the DM key outside
    private final Context context;


    MyZoneReader(Context context, IHeartRateUpdated heartRateUpdated) {
        this.heartRateUpdated = heartRateUpdated;
        this.context = context;
        myZoneLib = new MyZoneLib(context, (kCal, meps, heartRate, percentEffort) -> {
            if(isConnected) {
                DataMatrix.putInt(HALKeys.DATA_MYZONE_CALORIES_INT, kCal);
                DataMatrix.putInt(HALKeys.DATA_MYZONE_MEPS_INT, meps);
                DataMatrix.putInt(HALKeys.DATA_MYZONE_EFFORT_INT, percentEffort);

                if (percentEffort > 0)  maxHRaverager.addValue(heartRate / (percentEffort / 100f));
                if (loopToSendCounter++ > 10) {
                    DataMatrix.putInt(HALKeys.DATA_MYZONE_CALCULATED_AVG_MAX_HR_INT, (int) maxHRaverager.getValue());
                    loopToSendCounter = 0;
                }
                setBLEHeartRate(heartRate);
            }

        }, (beltNumber -> onDisconnect()));
        monitorWorkoutEvents();
    }

    private void monitorWorkoutEvents() {
        AtomicInteger previousState = new AtomicInteger(DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, state -> {
           if(previousState.get() == WorkoutKeys.Constants.RUN_STATE_STOP && state == WorkoutKeys.Constants.RUN_STATE_RUNNING) {
               startMyZoneWorkout();
           }
           else if(previousState.get() != WorkoutKeys.Constants.RUN_STATE_STOP && state == WorkoutKeys.Constants.RUN_STATE_STOP && isConnected) {
               endMyZoneWorkout();
           }
           previousState.set(state);
        }, false);
    }

    private void endMyZoneWorkout() {
        long workoutEndTime = new Date().getTime() / 1000;
        myZoneLib.requestUpload(SESSION_ID, new MyZoneLib.IRequestUploadResultListener() {
            @Override
            public void onResult(String id) {
                myZoneLib.requestTimeInZones(SESSION_ID, beltId, workoutStartTime, workoutEndTime, new MyZoneLib.IRequestTimeInZonesResultListener() {
                    @Override
                    public void onResult(String id, int beltNumber, int[] timeInZones, int[] mepsInZones) {
                        DataMatrix.putParcelable(HALKeys.DATA_MYZONE_TIME_IN_ZONES_PARCELABLE, new TimeInZones(timeInZones, mepsInZones));
                        DataMatrix.putBoolean(HALKeys.DATA_HAVE_MYZONE_TIME_IN_ZONES_BOOL, true);
                        myZoneLib.finishSession(SESSION_ID, null);
                        Log.d(this.getClass().getName(), new TimeInZones(timeInZones, mepsInZones).toString());
                        onDisconnect();
                    }

                    @Override
                    public void onError(String id, String action, String reason) {
                        myZoneLib.finishSession(SESSION_ID, null);
                        onDisconnect();
                        Log.d(this.getClass().getName(), "Error getting time in zones " + reason);
                    }
                });
            }

            @Override
            public void onError(String id, String action, String reason) {
                myZoneLib.finishSession(SESSION_ID, null);
                onDisconnect();
                Log.d(this.getClass().getName(), "Error uploading data in zones " + reason);

            }
        });

    }

    private void startMyZoneWorkout() {
        workoutStartTime = new Date().getTime() / 1000;
        DataMatrix.putBoolean(HALKeys.DATA_HAVE_MYZONE_TIME_IN_ZONES_BOOL, false);
    }

    public interface ConnectCallback {
        void onConnected();
    }

    public void connectToMyZoneDevice(BluetoothDevice device, ConnectCallback callback) {
        try {
            if(device.getName().toLowerCase().startsWith(MYZONE_PREFIX.toLowerCase())) {
                beltId = Integer.parseInt(device.getName().substring(MYZONE_PREFIX.length()));
                BksCredentialProvider credentialProvider = new BksCredentialProvider(context);
                String myZoneApiKey = credentialProvider.getString("MYZONE_API_KEY");
                myZoneLib.requestDisplayName(SESSION_ID, myZoneApiKey, beltId, new MyZoneLib.IRequestDisplayNameResultListener() {
                    @Override
                    public void onResult(String id, int beltNumber, String displayName, String profileImage, String rank) {
                        myZoneLib.requestConnection(id, beltNumber, new MyZoneLib.IRequestConnectionResultListener() {
                            @Override
                            public void onResult(String id, int beltNumber) {
                                DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, ButtonPressType.CONNECT_MYZONE_HEARTRATE.name);
                                isConnected = true;
                                DataMatrix.putBoolean(HALKeys.DATA_MY_ZONE_CONNECTED_BOOL, true);
                                setBLEHeartRate(0);
                                if (callback != null) callback.onConnected();
                            }

                            @Override
                            public void onError(String id, String action, String reason) {
                                Log.d(this.getClass().getName(), "Myzone connect failure. " + reason);
                            }
                        });
                    }

                    @Override
                    public void onError(String id, String action, String reason) {
                        Log.d(this.getClass().getName(), "Myzone get display name failure. " + reason);
                    }
                });
            }
        } catch(NumberFormatException ignored) {
        }
    }

    @Override
    public void close() {
        myZoneLib.destroy();
    }

    @Override
    public synchronized int heartRate() {
        return heartRate;
    }
    @Override
    public synchronized boolean blankState() { return false; }
    @Override
    public HeartRateSource getSource() {
        return HeartRateSource.MyZone;
    }

    @Override
    public boolean hasConnection() {
        return isConnected;
    }


    public void setBLEHeartRate(int heartRate) {
        this.heartRate = heartRate;
        heartRateUpdated.heartRateUpdated();
    }

    public void disconnect() {
        if (isConnected) {
            myZoneLib.finishSession(SESSION_ID, null);
            onDisconnect();
        }
    }

    private void onDisconnect() {
        isConnected = false;
        DataMatrix.putBoolean(HALKeys.DATA_MY_ZONE_CONNECTED_BOOL, false);
        setBLEHeartRate(0);
    }
}
