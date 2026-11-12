package com.jht.chimera.service.dapi;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.jht.chimera.lib.common.dapi.DapiGetSoftware.ACTION_GET_LATEST_SW_STRING;
import static com.jht.chimera.service.dapi.DAPIService.FIELD_NA_PLACEHOLDER;
import static com.jhtp.dapiservice.DapiResult.InternalException;
import static com.jhtp.dapiservice.DapiResult.Success;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.dapi.DapiGetSoftware;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.log.ConsoleEvent;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.lib.workout.SpecialWorkouts;
import com.jht.datamatrixlib.DataMatrix;
import com.jhtp.dapiservice.DapiResult;
import com.jhtp.dapiservice.am.AMResponse;
import com.jhtp.dapiservice.am.CardioInfo;
import com.jhtp.dapiservice.am.FacilityMachine;
import com.jhtp.dapiservice.comm.CborCommands;
import com.jhtp.dapiservice.comm.DapiComm;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DAPIAssetManagement implements AMResponse {
    private final String TAG = getClass().getSimpleName();
    private PeriodicTimer dapiNetworkConnectivityTimer;
    private ConnectivityManager connectivityManager;

    interface IOnCheckMachineUUID {
        void onCheckMachineUUID(boolean success);
    }

    interface IOnGetMachineStats {
        void onGetMachineStats(boolean success);
    }
    private IOnCheckMachineUUID onMachineUUID;
    private IOnGetMachineStats onGetMachineStats;


    private DAPIAssetManagement() {

        // update the machine info whenever the dapi environment changes
        DAPIService.setUrl(DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT));
        DataMatrix mDataMatrix = new DataMatrix();
        mDataMatrix.createIntAdapter(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, integer -> cardioInfo(), false);

        mDataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, event -> cardioInfo(), false);
        mDataMatrix.createIntAdapter(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT, value -> cardioInfo(), false);
        mDataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, value -> cardioInfo(), false);
        mDataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL, value -> cardioInfo(), false);
        SpecialWorkouts.monitorIfitStatus(mDataMatrix, value -> cardioInfo());

        // handle action get latest software
        mDataMatrix.createStringAdapter(ACTION_GET_LATEST_SW_STRING, this::getLatestSoftwareVersion, false);

        mDataMatrix.createIntAdapter(HALKeys.ACTION_REPORT_ERROR_CODE_INT, value -> {
            DapiComm.Instance.AM.logError(DapiUtil.getMachineId(), value);
        }, false);

        mDataMatrix.createStringAdapter(DAPIKeys.ACTION_VALIDATE_SETTINGS_STRING, s -> {
            String[] args = s.split(",");
            if (!DataMatrix.getBoolean(DAPIKeys.DATA_CONNECTED_TO_DAPI_BOOL)) {
                DataMatrix.sendEvent(DAPIKeys.EVENT_SETTINGS_VALID);
            } else {
                if (args.length != 3) {
                    DataMatrix.sendEvent(DAPIKeys.EVENT_SETTINGS_INVALID);
                } else {
                    try {
                        //arg[0] = Frame Serial Number, arg[1] = Console Serial Number, arg[2] = Club ID
                        DapiComm.Instance.AM.validateSettings(DapiUtil.computeMachineId(args[0]), args[0], args[1], Integer.parseInt(args[2]));
                    } catch (Exception ex) {
                        DataMatrix.sendEvent(DAPIKeys.EVENT_SETTINGS_INVALID);
                    }
                }
            }
        }, false);

        /* When guest mode is not allowed
         *     - Listen for network connectivity states to change.
         *     - Test internet connectivity whenever a network connection is added / lost (multiple network connections may be available)
         *     - Test internet connectivity every 30 seconds.
         *     - Update DATA_DAPI_CONNECTION_LOST bool with appropriate network status
         */
        mDataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL, allowGuest -> {
            Context context = ContextUtil.getApplicationContext();
            if (dapiNetworkConnectivityTimer != null) dapiNetworkConnectivityTimer.stop();
            if (allowGuest) return;

            try {
                if (connectivityManager == null && context != null) {
                    connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
                    connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(@NonNull Network network) {
                            if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL)) return;
                            if (!(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL) || DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL))) return;
                            try { updateDapiConnectionLostStatus(isNetworkOnline(5).get()); }
                            catch (Exception e) { Log.w(TAG, "Error checking network status: " + e); }
                        }
                        @Override
                        public void onLost(@NonNull Network network) {
                            if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL)) return;
                            if (!(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL) || DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL))) return;
                            try { updateDapiConnectionLostStatus(isNetworkOnline(5).get()); }
                            catch (Exception e) {Log.w(TAG, "Error checking network status: " + e);}
                        }
                    });
                }
            } catch (Exception e) { Log.w(TAG, "Could not register connectivity manager: " + e); }

            dapiNetworkConnectivityTimer = new PeriodicTimer(0, 30000, () -> {
                if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ALLOW_GUEST_BOOL)) return;
                if (!(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL) || DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL))) return;
                try { updateDapiConnectionLostStatus(isNetworkOnline(2).get()); }
                catch (Exception e) { Log.w(TAG, "Error checking network status: " + e); }
            });
            dapiNetworkConnectivityTimer.start();
        });
    }



    private static class SingletonHolder {
        private static final DAPIAssetManagement INSTANCE = new DAPIAssetManagement();
    }

    public static DAPIAssetManagement getInstance() {
        return DAPIAssetManagement.SingletonHolder.INSTANCE;
    }

    void cardioInfo() {
        Log.d(TAG, "100 cardioInfo ");
        Context context = ContextUtil.getApplicationContext();
        if (context == null || !AddressHelper.isNetworkConnected(context))
            return; // don't send if there is no internet
        if (DataMatrix.getBoolean(DAPIKeys.DATA_LOCK_CARDIO_INFO_BOOL) || !DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL))
            return; // don't send if the machine has not been setup yet

        if (FrameConfigManager.getInstance().getFrameType() == null) {
            Log.e(TAG, "Frame type is null!");
            return;
        }

        //int workoutCount = DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_WORKOUTS_INT);
        int totalTime = DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT);
        float totalDistance;

        if (FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Stepper) ||
                FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill)) {
            totalDistance = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT);
        } else {
            totalDistance = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT);
        }


        //int secondsPerWorkout = zeroSafeIntDivide(totalTime, workoutCount);
        //float distancePerWorkout = zeroSafeFloatDivide(totalDistance, (float) workoutCount);
        //int averagePace = Math.round(zeroSafeFloatDivide((float) secondsPerWorkout, distancePerWorkout));

        CardioInfo cardioInfo = new CardioInfo();
        cardioInfo.machineId = DapiUtil.getMachineId();
        cardioInfo.brand = DapiUtil.getBrandId();
        cardioInfo.machineType = FrameConfigManager.getInstance().getFrameConfig().machineClassType.getValue();
        cardioInfo.modelType = FrameConfigManager.getInstance().getModelId();
        cardioInfo.consoleType = ProductConfig.getConsoleType().id;
        cardioInfo.csafeManufacture = FIELD_NA_PLACEHOLDER; // n/a for retail
        cardioInfo.csafeId = FIELD_NA_PLACEHOLDER; // n/a for retail
        cardioInfo.csafeModelId = FIELD_NA_PLACEHOLDER; // n/a for retail
        cardioInfo.consoleSerialNumber = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
        cardioInfo.frameSerialNumber = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
        cardioInfo.clubId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT);
        cardioInfo.softwareVersion = "GUI:" + DataMatrix.getString(HALKeys.DATA_VERSION_CHIMERA_STRING); //NON-NLS
        cardioInfo.osVersion = DataMatrix.getString(HALKeys.DATA_VERSION_OS_STRING);
        cardioInfo.maxUsers = 1;
        cardioInfo.ipAddress = AddressHelper.getIPAddress("wlan0"); //NON-NLS
        cardioInfo.totalTime = totalTime;
        cardioInfo.averageTimePerWorkout = FIELD_NA_PLACEHOLDER; // = secondsPerWorkout;
        cardioInfo.totalDistance = totalDistance;
        cardioInfo.averageDistancePerWorkout = FIELD_NA_PLACEHOLDER; // = distancePerWorkout;
        cardioInfo.averagePace = FIELD_NA_PLACEHOLDER;
        cardioInfo.peakResistance = FIELD_NA_PLACEHOLDER;
        cardioInfo.averageResistance = FIELD_NA_PLACEHOLDER;
        cardioInfo.peakWatts = FIELD_NA_PLACEHOLDER;
        cardioInfo.averageWatts = FIELD_NA_PLACEHOLDER;
        cardioInfo.batteryPercentage = FIELD_NA_PLACEHOLDER; // n/a
        cardioInfo.peakKph = FIELD_NA_PLACEHOLDER;
        cardioInfo.averageKph = FIELD_NA_PLACEHOLDER;
        cardioInfo.headphoneInsertions = DataMatrix.getInt(SettingKeys.Hardware.DATA_HEADPHONE_A_COUNT_INT);
        cardioInfo.headphoneRemovals = FIELD_NA_PLACEHOLDER;
        cardioInfo.machineSubtype = "cardio"; //NON-NLS

        // Cardio specific (not needed?)
        cardioInfo.driveType = FIELD_NA_PLACEHOLDER;
        cardioInfo.minKph = (int) FrameConfigManager.getInstance().getFrameConfig().speedKph.getMin();
        cardioInfo.maxKph = (int) FrameConfigManager.getInstance().getFrameConfig().speedKph.getMax();
        cardioInfo.minResistance = (int) FrameConfigManager.getInstance().getFrameConfig().resistance.getMin();
        cardioInfo.maxResistance = (int) FrameConfigManager.getInstance().getFrameConfig().resistance.getMax();
        cardioInfo.minIncline = (int) FrameConfigManager.getInstance().getFrameConfig().incline.getMin();
        cardioInfo.maxIncline = (int) FrameConfigManager.getInstance().getFrameConfig().incline.getMax();
        cardioInfo.lcbVendor = DataMatrix.getInt(SettingKeys.General.DATA_LCB_TYPE_INT);
        cardioInfo.lcbVersion = DataMatrix.getString(HALKeys.DATA_VERSION_LCB_STRING) + ".0.0";

        DapiComm.Instance.AM.cardioInfo(cardioInfo);

        if (ProductConfig.isCommercial()) {
            DapiComm.Instance.AM.consoleFeatureSet(
                    DapiUtil.getMachineId(),
                    DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL),
                    DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL),
                    SpecialWorkouts.isIfitEnabled()
            );
        }
    }


    private void getLatestSoftwareVersion(String softwareType) {
        Context context = ContextUtil.getApplicationContext();
        if (context == null || !AddressHelper.isNetworkConnected(context)) {
            softwareVersion(DapiResult.Timeout, null, null, null, null);
            return;
        }

        String machineId = DapiUtil.getMachineId();
        int brand = DapiUtil.getBrandId();
        FrameConfigEnum frameType = FrameConfigManager.getInstance().getFrameType();
        if (frameType == null) {
            return;
        }
        int machineType = FrameConfigManager.getInstance().getFrameConfig().machineClassType.getValue();
        int modelType = FrameConfigManager.getInstance().getModelId();
        int consoleType = ProductConfig.getConsoleType().id;
        int deployment = DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT);
        String currentVersion = "";
        DapiComm.Instance.AM.softwareVersion(machineId, brand, machineType, modelType, consoleType, deployment, softwareType, currentVersion);
    }

    void getMachineStats(IOnGetMachineStats onGetMachineStats) {
        this.onGetMachineStats = onGetMachineStats;
        if (ProductConfig.isCommercial()) {
            DapiComm.Instance.AM.getMachineStats(DapiUtil.getMachineId());
        }
    }

    void getMachineUUID(IOnCheckMachineUUID onMachineUUID) {
        this.onMachineUUID = onMachineUUID;
        DataMatrix.putBoolean(DAPIKeys.DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL, false);
        String uuid = DapiUtil.computeMachineId();
        DataMatrix.putString(DAPIKeys.DATA_DAPI_MACHINE_UUID_STRING, uuid);
        DapiComm.Instance.AM.getMachineId(uuid, DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING));
        ConsoleEvent.logEvent("DAPI Machine UUID: " + uuid);
    }
    @Override
    public void responseAM(CborCommands cborCommands, DapiResult dapiResult) {
        Log.d(TAG, "responseAM " + cborCommands);
        DataMatrix.putBoolean(TopBarKeys.DATA_DAPI_CONNECTED_BOOL, dapiResult != InternalException);

        if (cborCommands == CborCommands.ValidateSettings) {
            DataMatrix.sendEvent(dapiResult.Value == Success.Value ? DAPIKeys.EVENT_SETTINGS_VALID : DAPIKeys.EVENT_SETTINGS_INVALID);
        }

        if (cborCommands == CborCommands.CardioInfo) {
            // GeneralError here means the serial numbers are not valid (used by the MES Update tab)
            DataMatrix.updateInt(DAPIKeys.DATA_CARDIO_INFO_DAPI_RESULT_INT, dapiResult.Value);
        }

        if(cborCommands == CborCommands.ConsoleFeatureSet) {
            DataMatrix.putBoolean(DAPIKeys.DATA_DAPI_SERVICE_READY_BOOL, true);
        }
    }

    @Override
    public void consoleSkin(DapiResult dapiResult, String s, String s1) {
        // no skin support
    }

    @Override
    public void softwareVersion(DapiResult dapiResult, String softwareType, String softwareVersion, String softwareUrl, String md5) {
        // handle event get latest software response
        //boolean success = dapiResult == DapiResult.Success;
        DapiGetSoftware.DapiGetLatestSoftwareResponse responseParcel = new DapiGetSoftware.DapiGetLatestSoftwareResponse();
        responseParcel.dapiResponse = dapiResult.Value;
        responseParcel.softwareType = softwareType;
        responseParcel.version = softwareVersion;
        responseParcel.url = softwareUrl;
        responseParcel.md5 = md5;
        DataMatrix.putParcelable(DapiGetSoftware.EVENT_GET_LATEST_SW_RESPONSE_PARCEL, responseParcel);
    }

    @Override
    public void getMachineStats(DapiResult dapiResult, float distance, int time, int reps, long weight, long workouts) {

        Log.d(TAG, "getMachineStats result " + dapiResult + " " + distance + " " + time + " " + workouts);

        Log.d(TAG,
                "1 Accumulated data " +
                        DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT) + " " +
                        DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT) + " " +
                        DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT));

        if(dapiResult.Value == Success.Value && BuildConfig.DAPIMachineIdReady) {
            if (FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Stepper) ||
                    FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill)) {

                if(Math.abs(distance - DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT)) > 1.1) {
                    DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT, distance);
                }
            } else {
                if(Math.abs(distance - DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT)) > 0.3) {
                    DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT, distance);
                }


            }
            if(time != DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT)) {
                DataMatrix.putInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT, time);
            }
        }
        else if(BuildConfig.DAPIMachineIdReady) {
            DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT, 0);
            DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT, 0);
            DataMatrix.putInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT, 0);

        }
        Log.d(TAG,
                "2 Accumulated data " +
                        DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT) + " " +
                        DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT) + " " +
                        DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT));

        if(onGetMachineStats != null) {
            onGetMachineStats.onGetMachineStats(dapiResult.Value == Success.Value);
        }
        onGetMachineStats = null;
    }


    @Override
    public void getFacilityMachines(DapiResult dapiResult, List<FacilityMachine> list) {

    }

    @Override
    public void getUtcTime(DapiResult dapiResult, int i) {

    }

    @Override
    public void getMachineId(DapiResult dapiResult, String uuid) {
        Log.d(TAG, "getMachineId response: " + DapiUtil.getMachineId() + " | " + uuid);
        if(dapiResult.Value == Success.Value && uuid != null && !uuid.isEmpty()) {
            if(StringUtil.isUUID(uuid)) {
                DataMatrix.updateString(DAPIKeys.DATA_DAPI_MACHINE_UUID_STRING, uuid);
            }
        }
        DataMatrix.putBoolean(DAPIKeys.DATA_DAPI_MACHINE_UUID_VALIDATED_BOOL, true);
        if(onMachineUUID != null) {
            onMachineUUID.onCheckMachineUUID(dapiResult.Value == Success.Value);
        }
        onMachineUUID = null;

    }


    public static CompletableFuture<Boolean> isNetworkOnline(int attempts) {
        if (attempts < 0) throw new InvalidParameterException("Attempts cannot be less than 0");
        return CompletableFuture.supplyAsync(() -> {
            for (int i = 0; i < attempts; i++) {
                try {
                    int timeoutMs = 5000;
                    Socket socket = new Socket();
                    SocketAddress socketAddress = new InetSocketAddress("8.8.8.8", 53);
                    socket.connect(socketAddress, timeoutMs);
                    socket.close();
                    return true;
                } catch (IOException ignored) {}
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {}
            }
            return false;
        });
    }

    private static void updateDapiConnectionLostStatus(boolean networkOnline){
        if (networkOnline == DataMatrix.getBoolean(DAPIKeys.DATA_DAPI_CONNECTION_LOST_BOOL)) {
            DataMatrix.putBoolean(DAPIKeys.DATA_DAPI_CONNECTION_LOST_BOOL, !networkOnline);
        }
    }
}
