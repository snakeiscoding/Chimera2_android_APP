package com.jht.chimera.lib.integrations.myzone;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

public class MyZoneLib{

    //#region Intent Request Actions
    @NonNls public final static String REQUEST_DISPLAY_NAME = "com.myzone.myzoneble.console_service.REQUEST_DISPLAY_NAME";
    @NonNls public final static String REQUEST_DISPLAY_NAME_TEST = "test.com.myzone.myzoneble.console_service.REQUEST_DISPLAY_NAME";
    @NonNls public final static String RESULT_DISPLAY_NAME = "com.myzone.myzoneble.console_service.DISPLAY_NAME";
    @NonNls public final static String RESULT_DISPLAY_NAME_TEST = "test.com.myzone.myzoneble.console_service.DISPLAY_NAME";

    @NonNls public final static String REQUEST_CONNECTION = "com.myzone.myzoneble.console_service.REQUEST_CONNECTION";
    @NonNls public final static String REQUEST_CONNECTION_TEST= "test.com.myzone.myzoneble.console_service.REQUEST_CONNECTION";
    @NonNls public final static String CONNECTED = "com.myzone.myzoneble.console_service.CONNECTED";
    @NonNls public final static String CONNECTED_TEST = "test.com.myzone.myzoneble.console_service.CONNECTED";

    @NonNls public final static String REQUEST_FINISH_SESSION = "com.myzone.myzoneble.console_service.FINISH_SESSION";
    @NonNls public final static String REQUEST_FINISH_SESSION_TEST = "test.com.myzone.myzoneble.console_service.FINISH_SESSION";
    @NonNls public final static String RESULT_SESSION_FINISHED = "com.myzone.myzoneble.console_service.SESSION_FINISHED";
    @NonNls public final static String RESULT_SESSION_FINSIHED_TEST = "test.com.myzone.myzoneble.console_service.SESSION_FINISHED";

    @NonNls public final static String REQUEST_TIME_IN_ZONES = "com.myzone.myzoneble.console_service.REQUEST_TIME_IN_ZONES";
    @NonNls public final static String REQUEST_TIME_IN_ZONES_TEST = "test.com.myzone.myzoneble.console_service.REQUEST_TIME_IN_ZONES";
    @NonNls public final static String RESULT_TIME_IN_ZONES = "com.myzone.myzoneble.console_service.TIME_IN_ZONES";
    @NonNls public final static String RESULT_TIME_IN_ZONES_TEST = "test.com.myzone.myzoneble.console_service.TIME_IN_ZONES";

    @NonNls public final static String REQUEST_UPLOAD = "com.myzone.myzoneble.console_service.REQUEST_UPLOAD";
    @NonNls public final static String REQUEST_UPLOAD_TEST = "test.com.myzone.myzoneble.console_service.REQUEST_UPLOAD";
    @NonNls public final static String RESULT_UPLOAD_COMPLETE = "com.myzone.myzoneble.console_service.UPLOAD_COMPLETE";
    @NonNls public final static String RESULT_UPLOAD_COMPLETE_TEST = "test.com.myzone.myzoneble.console_service.UPLOAD_COMPLETE";

    @NonNls public final static String WORKOUT_UPDATE = "com.myzone.myzoneble.console_service.WORKOUT_UPDATE";
    @NonNls public final static String WORKOUT_UPDATE_TEST = "test.com.myzone.myzoneble.console_service.WORKOUT_UPDATE";

    @NonNls public final static String DISCONNECTED = "com.myzone.myzoneble.console_service.DISCONNECTED";
    @NonNls public final static String DISCONNECTED_TEST = "test.com.myzone.myzoneble.console_service.DISCONNECTED";

    @NonNls public final static String ERROR = "com.myzone.myzoneble.console_service.ERROR";
    @NonNls public final static String ERROR_TEST = "test.com.myzone.myzoneble.console_service.ERROR";
    //#endregion

    //private final static String TAG = "MyZoneLib";
    private final boolean DEBUG;

    private final Context mContext;
    private IRequestDisplayNameResultListener mRequestDisplayNameResult;
    private IRequestConnectionResultListener mRequestConnectionResult;
    private IFinishSessionResultListener mFinishSessionResult;
    private IRequestTimeInZonesResultListener mRequestTimeZoneResult;
    private IRequestUploadResultListener mRequestUploadResult;
    //Potentially more than one listener to workout data updates
    private final IWorkoutDataUpdateListener mWorkoutUpdateResultListenerList;
    //Potentially more than one listener to disconnection updates
    private final IDisconnectionListener mDisconnectionListenerList;

    public MyZoneLib(@NonNull Context context, @Nullable IWorkoutDataUpdateListener dataUpdateListener, @Nullable IDisconnectionListener disconnectionListener){
        this.mContext = context;
        mWorkoutUpdateResultListenerList = dataUpdateListener;
        mDisconnectionListenerList = disconnectionListener;
        DEBUG = DataMatrix.getBoolean(HALKeys.DATA_USE_MYZONE_SIMULATOR_BOOL);

        //Display Name
        IntentFilter displayNameFilter = new IntentFilter();
        displayNameFilter.addAction(RESULT_DISPLAY_NAME);
        displayNameFilter.addAction(RESULT_DISPLAY_NAME_TEST);
        displayNameFilter.addAction(ERROR_TEST);
        displayNameFilter.addAction(ERROR);
        mContext.registerReceiver(displayNameReceiver, displayNameFilter);

        //Request Connection
        IntentFilter requestConnectionFilter = new IntentFilter();
        requestConnectionFilter.addAction(CONNECTED);
        requestConnectionFilter.addAction(CONNECTED_TEST);
        requestConnectionFilter.addAction(ERROR_TEST);
        requestConnectionFilter.addAction(ERROR);
        mContext.registerReceiver(connectionReceiver, requestConnectionFilter);

        //Finish Session
        IntentFilter finishSessionFilter = new IntentFilter();
        finishSessionFilter.addAction(RESULT_SESSION_FINISHED);
        finishSessionFilter.addAction(RESULT_SESSION_FINSIHED_TEST);
        finishSessionFilter.addAction(ERROR_TEST);
        finishSessionFilter.addAction(ERROR);
        mContext.registerReceiver(finishSessionReceiver,finishSessionFilter);

        //Request time in zones
        IntentFilter requestTimeInZonesFilter = new IntentFilter();
        requestTimeInZonesFilter.addAction(RESULT_TIME_IN_ZONES);
        requestTimeInZonesFilter.addAction(RESULT_TIME_IN_ZONES_TEST);
        requestTimeInZonesFilter.addAction(ERROR_TEST);
        requestTimeInZonesFilter.addAction(ERROR);
        mContext.registerReceiver(requestTimeInZonesReceiver, requestTimeInZonesFilter);

        //Request Upload
        IntentFilter requestUploadFilter = new IntentFilter();
        requestUploadFilter.addAction(RESULT_UPLOAD_COMPLETE);
        requestUploadFilter.addAction(RESULT_UPLOAD_COMPLETE_TEST);
        requestUploadFilter.addAction(ERROR_TEST);
        requestUploadFilter.addAction(ERROR);
        mContext.registerReceiver(requestUploadReceiver, requestUploadFilter);

        //Workout Update
        IntentFilter workoutUpdateFilter = new IntentFilter();
        workoutUpdateFilter.addAction(WORKOUT_UPDATE);
        workoutUpdateFilter.addAction(WORKOUT_UPDATE_TEST);
        mContext.registerReceiver(workoutUpdateReceiver, workoutUpdateFilter);

        //Disconnection Update
        IntentFilter disconnectUpdateFilter = new IntentFilter();
        disconnectUpdateFilter.addAction(DISCONNECTED);
        disconnectUpdateFilter.addAction(DISCONNECTED_TEST);
        mContext.registerReceiver(disconnectionUpdateReceiver, disconnectUpdateFilter);

     }

    /**
     * Request a display name for a given belt number. Note that belt numbers can be parsed from the
     * bluetooth device name.
     *
     * @param id = An identifier, the value of which does not matter, will be included in responses.
     * @param auth = A per-console authentication process by the facility owner will yield this
     *        authentication token.
     * @param beltNumber = The 7 digit belt number parsed from the bluetooth device name. For
     *        example ‘1001580’.
     * @param resultListener = The result callback for Display Name to handle result and errors. This is a one-time use callback per function call and is set
     *        to null within the broadcast receiver that handles results from the MyZone service.
     */

    public void requestDisplayName(String id, String auth, int beltNumber, @NonNull IRequestDisplayNameResultListener resultListener){
        mRequestDisplayNameResult = resultListener;
        Intent intent = new Intent(DEBUG ? REQUEST_DISPLAY_NAME_TEST : REQUEST_DISPLAY_NAME);
        intent.putExtra("belt_number",beltNumber);
        intent.putExtra("id",id);
        intent.putExtra("auth",auth);
        sendIntent(intent);
    }

    private void sendIntent(Intent intent) {
        if(!DEBUG) {
            intent.setComponent(new ComponentName("com.myzone.myzoneble.console_service", "com.myzone.myzoneble.console_service.ConsoleBroadcastReceiver"));
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        }
        mContext.sendBroadcast(intent);
    }

    /**
     * Request the Myzone service to connect to the specified belt. Requires the client to have already
     * have established a BLE connection with the belt (via BluetoothDevice.gattConnect() and then
     * waiting for BluetoothGatt.STAT_CONNECTED), as the Myzone app will use
     * BluetoothManager.getConnectedDevices() to find the belt, rather than scanning itself . Upon
     * receipt of the CONNECTED response (detailed below), please disconnect with
     * BluetoothGatt.disconnect().
     *
     * @param id = An identifier, the value of which does not matter, will be included in responses.
     * @param beltNumber = The 7 digit belt number parsed from the bluetooth device name. For
     *        example ‘1001580’.
     * @param resultListener = The result callback for Connection to handle result and errors. This is a one-time use callback per function call and is set
     *        to null within the broadcast receiver that handles results from the MyZone service.
     */

    public void requestConnection(String id, int beltNumber, @NonNull IRequestConnectionResultListener resultListener){
        //TODO: Add debug/test vs release logic
        mRequestConnectionResult = resultListener;
        Intent intent = new Intent(DEBUG? REQUEST_CONNECTION_TEST : REQUEST_CONNECTION);
        intent.putExtra("belt_number",beltNumber);
        intent.putExtra("id",id);
        sendIntent(intent);
    }

    /**
     *Upon receipt of this intent, the Myzone service will disconnect from the belt, and then upload any
     * unprocessed workout data. After successfully disconnecting, and uploading workout data, the
     * Myzone service will respond with SESSION_FINISHED.
     *
     * @param id = An identifier, the value of which does not matter, will be included in responses.
     * @param resultListener = The result callback for Finish Session to handle result and errors. This is a one-time use callback per function call and is set
     *        to null within the broadcast receiver that handles results from the MyZone service.
     */

    public void finishSession(String id, IFinishSessionResultListener resultListener){
        //TODO: Add debug/test vs release logic
        mFinishSessionResult = resultListener;
        Intent intent = new Intent(DEBUG ? REQUEST_FINISH_SESSION_TEST : REQUEST_FINISH_SESSION);
        intent.putExtra("id",id);
        sendIntent(intent);
    }

    /**
     * Request the amount of time spent in each zone during the specified time interval, for the specified
     * belt number. The Myzone service will respond with a TIME_IN_ZONES intent containing an array
     * of seconds spent in each zone. From index 0, to index 5, the array values represent seconds spent
     * in: Light Gray, Gray, Blue, Green, Yellow, Red.
     *
     * @param id = An identifier, the value of which does not matter, will be included in responses.
     * @param beltNumber = The 7 digit belt number parsed from the bluetooth device name. For
     *        example ‘1001580’.
     * @param intervalStart = A UTC timestamp, in seconds since 1st January 1970. This is the start of
     *        the interval for which you want to query Time In Zones data for.
     * @param intervalEnd = A UTC timestamp, in seconds since 1st January 1970. This is the end of
     *        the interval for which you want to query Time In Zones data for.
     * @param resultListener = The result callback for Request Time In Zones to handle result and errors. This is a one-time use callback per function call and is set
     *        to null within the broadcast receiver that handles results from the MyZone service.
     */

    public void requestTimeInZones(String id, int beltNumber, long intervalStart, long intervalEnd, @NonNull IRequestTimeInZonesResultListener resultListener){
        mRequestTimeZoneResult = resultListener;
        Intent intent = new Intent(DEBUG ? REQUEST_TIME_IN_ZONES_TEST : REQUEST_TIME_IN_ZONES);
        intent.putExtra("id",id);
        intent.putExtra("belt_number",beltNumber);
        intent.putExtra("interval_start",intervalStart);
        intent.putExtra("interval_end",intervalEnd);
        sendIntent(intent);

    }

    /**
     * Request that the Myzone service uploads all non-uploaded workout data to the Myzone servers.
     * Note that while the Myzone service is running, it will attempt this upload automatically once every
     * 10 minutes.
     *
     * @param id  = An identifier, the value of which does not matter, will be included in responses.
     * @param resultListener = The result callback for Request Time In Zones to handle result and errors. This is a one-time use callback per function call and is set
     *        to null within the broadcast receiver that handles results from the MyZone service.
     */

    public void requestUpload(String id, IRequestUploadResultListener resultListener){
        mRequestUploadResult = resultListener;
        Intent intent = new Intent(DEBUG ? REQUEST_UPLOAD_TEST : REQUEST_UPLOAD);
        intent.putExtra("id",id);
        sendIntent(intent);
    }

    /**
     * Clean up receivers and callbacks. Use this when finished with the MyZoneLib object.
     */

    public void destroy(){
        mContext.unregisterReceiver(displayNameReceiver);
        mContext.unregisterReceiver(connectionReceiver);
        mContext.unregisterReceiver(finishSessionReceiver);
        mContext.unregisterReceiver(requestTimeInZonesReceiver);
        mContext.unregisterReceiver(requestUploadReceiver);
        mContext.unregisterReceiver(workoutUpdateReceiver);
        mContext.unregisterReceiver(disconnectionUpdateReceiver);

        if(mRequestDisplayNameResult != null)
            mRequestDisplayNameResult = null;

        if(mRequestConnectionResult != null)
            mRequestConnectionResult = null;

        if(mFinishSessionResult != null)
            mFinishSessionResult = null;

        if(mRequestTimeZoneResult != null)
            mRequestTimeZoneResult = null;

        if(mRequestUploadResult != null)
            mRequestUploadResult = null;

    }

    //#region Interfaces
    public interface IRequestDisplayNameResultListener {
        void onResult(String id, int beltNumber, String displayName, String profileImage, String rank);
        void onError(String id, String action, String reason);
    }

    public interface IRequestConnectionResultListener {
        void onResult(String id, int beltNumber);
        void onError(String id, String action, String reason);
    }

    public interface IFinishSessionResultListener {
        void onResult(String id);
        void onError(String id, String action, String reason);
    }

    public interface IRequestTimeInZonesResultListener {
        void onResult(String id, int beltNumber, int[] timeInZones, int[] mepsInZones);
        void onError(String id, String action, String reason);
    }

    public interface IRequestUploadResultListener {
        void onResult(String id);
        void onError(String id, String action, String reason);
    }

    public interface IWorkoutDataUpdateListener {
        void onResult(int kCal, int meps, int heartRate, int percentEffort);
    }

    public interface IDisconnectionListener{
        void onDisconnection(int beltNumber);
    }
    //#endregion

    //#region Broadcast Receivers
    BroadcastReceiver displayNameReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && mRequestDisplayNameResult != null){
                if((intent.getAction().equalsIgnoreCase(RESULT_DISPLAY_NAME_TEST) || intent.getAction().equalsIgnoreCase(RESULT_DISPLAY_NAME) )&& intent.hasExtra("display_name")) {
                    mRequestDisplayNameResult.onResult(intent.getStringExtra("id"),
                            intent.getIntExtra("belt_number",-1),
                            intent.getStringExtra("display_name") ,
                            intent.getStringExtra("profile_image"),
                            intent.getStringExtra("rank"));
                }else if(intent.getAction().equalsIgnoreCase(ERROR) || intent.getAction().equalsIgnoreCase(ERROR_TEST)){
                    mRequestDisplayNameResult.onError(intent.getStringExtra("id"),
                            intent.getStringExtra("action"),
                            intent.getStringExtra("reason"));
                }
                mRequestDisplayNameResult = null;
            }

        }
    };

    BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && mRequestConnectionResult != null){
                if((intent.getAction().equalsIgnoreCase(CONNECTED_TEST) || intent.getAction().equalsIgnoreCase(CONNECTED)) && intent.hasExtra("id") && intent.hasExtra("belt_number")){
                    mRequestConnectionResult.onResult(intent.getStringExtra("id"), intent.getIntExtra("belt_number",-1));
                }else if(intent.getAction().equalsIgnoreCase(ERROR) || intent.getAction().equalsIgnoreCase(ERROR_TEST)){
                    mRequestConnectionResult.onError(intent.getStringExtra("id"), intent.getStringExtra("action"),intent.getStringExtra("reason"));
                }
                mRequestConnectionResult = null;
            }
        }
    };

    BroadcastReceiver finishSessionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && mFinishSessionResult != null){
                if((intent.getAction().equalsIgnoreCase(RESULT_SESSION_FINSIHED_TEST) || intent.getAction().equalsIgnoreCase(RESULT_SESSION_FINISHED))){
                    mFinishSessionResult.onResult(intent.getStringExtra("id"));
                }else if(intent.getAction().equalsIgnoreCase(ERROR) || intent.getAction().equalsIgnoreCase(ERROR_TEST)){
                    mFinishSessionResult.onError(intent.getStringExtra("id"), intent.getStringExtra("action"),intent.getStringExtra("reason"));
                }
                mFinishSessionResult = null;
            }
        }
    };

    BroadcastReceiver requestTimeInZonesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && mRequestTimeZoneResult != null){
                if((intent.getAction().equalsIgnoreCase(RESULT_TIME_IN_ZONES_TEST) || intent.getAction().equalsIgnoreCase(RESULT_TIME_IN_ZONES)) && intent.hasExtra("time_in_zones") && intent.hasExtra("meps_in_zones") ){
                    mRequestTimeZoneResult.onResult(intent.getStringExtra("id"),
                                                    intent.getIntExtra("belt_number",0),
                                                    intent.getIntArrayExtra("time_in_zones"),
                                                    intent.getIntArrayExtra("meps_in_zones"));
                }else if(intent.getAction().equalsIgnoreCase(ERROR) || intent.getAction().equalsIgnoreCase(ERROR_TEST)){
                    mRequestTimeZoneResult.onError(intent.getStringExtra("id"),intent.getStringExtra("action"),intent.getStringExtra("reason"));
                }
                mRequestTimeZoneResult = null;
            }
        }
    };

    BroadcastReceiver requestUploadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && mRequestUploadResult != null){
                if((intent.getAction().equalsIgnoreCase(RESULT_UPLOAD_COMPLETE_TEST) || intent.getAction().equalsIgnoreCase(RESULT_UPLOAD_COMPLETE)) && intent.hasExtra("id")){
                    mRequestUploadResult.onResult(intent.getStringExtra("id"));
                    //mRequestUploadResult.onResult("");
                }else if(intent.getAction().equalsIgnoreCase(ERROR) || intent.getAction().equalsIgnoreCase(ERROR_TEST)){
                    mRequestUploadResult.onError(intent.getStringExtra("id"), intent.getStringExtra("action"),intent.getStringExtra("reason"));
                }
                mRequestUploadResult = null;
            }
        }
    };

    BroadcastReceiver workoutUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && mWorkoutUpdateResultListenerList != null){
                mWorkoutUpdateResultListenerList.onResult(intent.getIntExtra("kcal",-1),
                    intent.getIntExtra("meps",-1),
                    intent.getIntExtra("heart_rate", -1),
                    intent.getIntExtra("percent_effort",-1));
            }
        }
    };

    BroadcastReceiver disconnectionUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && mDisconnectionListenerList != null){
                mDisconnectionListenerList.onDisconnection(intent.getIntExtra("belt_number",-1));
            }
        }
    };
    //#endregion


}
