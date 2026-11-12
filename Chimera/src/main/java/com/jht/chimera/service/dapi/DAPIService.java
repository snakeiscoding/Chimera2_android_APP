package com.jht.chimera.service.dapi;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.util.SerialNumberUtil;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.dapi.DapiUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.log.ServiceLog;
import com.jht.chimera.lib.language.StringHelper;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.services.ChildConsoleService;
import com.jht.services.ConsoleServiceState;
import com.jhtp.dapiservice.am.ServiceEntry;
import com.jhtp.dapiservice.comm.DapiComm;

@SuppressLint("MissingPermission")
public class DAPIService extends ChildConsoleService {
    private final String TAG = "DAPIService";

    static int FIELD_NA_PLACEHOLDER = 0;

    DAPIAnalytics mAnalytics;
    DAPIUserManagement mUserManagement;
    DAPIWorkoutTracking mWorkoutTracking;
    DAPIAssetManagement mAssetManagement;
    boolean mSentInformation = false;

    private DataMatrix mDataMatrix;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean initialized = false;
    @Override
    public void onCreate() {
        Log.d("ServiceStarter", "Starting " + this);
        super.onCreate();
        mDataMatrix = new DataMatrix();
        mDataMatrix.createBoolAdapter(HALKeys.DATA_HAL_SERVICE_READY_BOOL, ready -> {
            if(ready && !initialized) {
                Log.d("ServiceStarter", "Initializing DAPIService");
                initialized = true;
                init();
                //Adding a delay ensures related services are fully initialized before syncTimerStart() runs
                new Handler(Looper.getMainLooper()).postDelayed(()-> mUserManagement.syncTimerStart(), 1000);
                updateMyServiceState(ConsoleServiceState.READY);
                Log.d("ServiceStarter", "Done Initializing DAPIService");
            }

        });

        Log.d("ServiceStarter", "Ending " + this);
    }

    @Override
    public void onDestroy() {
        if (mUserManagement != null) mUserManagement.syncTimerStop();
        super.onDestroy();
    }

    private boolean isMachineSetup = false;
    void init() {

        // create dapi objects
        Log.d(TAG, "MACHINE_UUID_STRING: " + DapiUtil.getMachineId());
        DataMatrix.putBoolean(DAPIKeys.DATA_CONNECTED_TO_DAPI_BOOL, true);
        setUrl(DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT));

        DapiComm.Instance.addAllClients();
        DapiComm.Instance.start(null);
        mAnalytics = DAPIAnalytics.getInstance();
        mUserManagement = DAPIUserManagement.getInstance();
        mWorkoutTracking = DAPIWorkoutTracking.getInstance();
        mAssetManagement = DAPIAssetManagement.getInstance();
        DapiComm.Instance.Analytics.clearListeners();
        DapiComm.Instance.Analytics.addAnalyticsListener(mAnalytics);
        DapiComm.Instance.Analytics.pauseEventCollection(!DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_DAPI_ANALYTICS_BOOL));
        DapiComm.Instance.UserManagement.clearListeners();
        DapiComm.Instance.UserManagement.addUserManagementListener(mUserManagement);
        DapiComm.Instance.WorkoutTracking.clearListeners();
        DapiComm.Instance.WorkoutTracking.addWorkoutTrackingListener(mWorkoutTracking);
        DapiComm.Instance.AM.clearListeners();
        DapiComm.Instance.AM.addListener(mAssetManagement);

        // Register for internet connection changes
        mDataMatrix.createBoolAdapter(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL, this::updateInternetConnectivity);

        mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_LOG_DAPI_PACKETS_BOOL, value -> DapiComm.Instance.setLogMessages(value), true);

        mDataMatrix.createParcelableAdapter(LoggerKeys.ACTION_ADD_SERVICE_PARCELABLE, ServiceLog.class, serviceLog -> {
            ServiceEntry entry;
            switch(StringHelper.ServiceTypes.values()[serviceLog.serviceType]) {
                case Battery : entry = ServiceEntry.Battery; break;
                case BeltDrive: entry = ServiceEntry.BeltDrive; break;
                case BeltGeneratorFlyWheel: entry = ServiceEntry.BeltGeneratorFlywheel; break;
                case BeltRunningFlipDeck:  entry = ServiceEntry.DeckFlip; break;
                case BeltRunningNewDeck: entry = ServiceEntry.DeckNew; break;
                case ChainDrive: entry = ServiceEntry.ChainDrive; break;
                case StairDrive: entry = ServiceEntry.ChainStair; break;
                case ConsoleCable: entry = ServiceEntry.ConsoleCable; break;
                case ResetAccumulatedTime:
                case ResetAccumulatedDistance:
                case ResetAccumulatedFloors:
                case ResetHeadphoneCounter:
                case RestoreToFactoryDefaults:
                case LubeBelt:
                default:
                    return;
            }
            DapiComm.Instance.AM.logService(DapiUtil.getMachineId(), entry);
        });

        isMachineSetup = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL);
        mDataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, value -> {
            if(value != isMachineSetup && value) {
                doFirstTimeConnectedTasks(false, false);
            }
            isMachineSetup = value;
        });
        frameSerialNumber = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
        mDataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, serialNumber -> {
            serialNumber = SerialNumberUtil.fixSNToBeAlphanumeric(serialNumber);
            if(!frameSerialNumber.equals(serialNumber)) {
                frameSerialNumber = serialNumber;
                if(DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL)) {
                    doFirstTimeConnectedTasks(true, true);
                }
            }
        });

        mDataMatrix.createEventAdapter(HALKeys.EVENT_MACHINE_SETUP_COMPLETE, value -> doFirstTimeConnectedTasks(true, true));

        mDataMatrix.createIntAdapter(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT, value -> {
            DAPIService.setUrl(value);
            doFirstTimeConnectedTasks(true, true);
        }, false);

        mDataMatrix.createStringAdapter(DAPIKeys.ACTION_SET_DAPI_TEST_URL_STRING, url -> {
            if(url.isEmpty()) {
                DAPIService.setUrl(DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT));
            }
            else {
                DapiComm.Instance.setUrl(url);
            }
        }, false);

        mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_ENABLE_DAPI_ANALYTICS_BOOL, value -> DapiComm.Instance.Analytics.pauseEventCollection(!value));

        // This is useful for testing the process.
        mDataMatrix.createBoolAdapter(DAPIKeys.ACTION_DO_FIRST_TIME_CONNECTED_TASKS_BOOL, getMachineStats -> {
            DataMatrix.putBoolean(DAPIKeys.DATA_LOCK_CARDIO_INFO_BOOL, true);
            doFirstTimeConnectedTasks(true, getMachineStats);
        }, false);
    }
    private String frameSerialNumber;

    public static void setUrl(int dapiEnv) {
        DapiComm.Instance.setUrl(DapiUtil.getDapiUrl(dapiEnv));
    }

    void doFirstTimeConnectedTasks(boolean force, boolean getMachineStatus) {
        Log.d(TAG, "doFirstTimeConnectedTasks " + force + " " + getMachineStatus);
        if ((force || !mSentInformation) && DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL)) {
            DataMatrix.putBoolean(DAPIKeys.DATA_DAPI_SERVICE_READY_BOOL, false);
            mSentInformation = true;
            new Thread(() -> {
                while (FrameConfigManager.getInstance().getFrameType() == null) {
                    SystemClock.sleep(500);
                }
                if (BuildConfig.DAPIMachineIdReady) {
                    Log.d(TAG, "1 doFirstTimeConnectedTasks " + force + " " + getMachineStatus);
                    mAssetManagement.getMachineUUID(success -> {
                        if(getMachineStatus) {
                            Log.d(TAG, "10 doFirstTimeConnectedTasks " + force + " " + getMachineStatus);
                            mAssetManagement.getMachineStats(success1 -> {
                                Log.d(TAG, "20 doFirstTimeConnectedTasks " + force + " " + getMachineStatus);
                                DataMatrix.putBoolean(DAPIKeys.DATA_LOCK_CARDIO_INFO_BOOL, false);
                                mAssetManagement.cardioInfo();
                            });
                        }
                        else {
                            Log.d(TAG, "11 doFirstTimeConnectedTasks " + force + " " + getMachineStatus);
                            DataMatrix.putBoolean(DAPIKeys.DATA_LOCK_CARDIO_INFO_BOOL, false);
                            mAssetManagement.cardioInfo();
                        }
                    });

                }
                else {

                    Log.d(TAG, "3 cardioInfo");
                    DataMatrix.putBoolean(DAPIKeys.DATA_LOCK_CARDIO_INFO_BOOL, false);
                    mAssetManagement.cardioInfo();
                }

                // Check machine


                // wait a bit before asking for RSCU updates
                SystemClock.sleep(5000);

                AutoUpdateManager.getInstance().checkRscuOnBootIfRequired();
            }).start();
        }
    }

    void updateInternetConnectivity(boolean networkConnected) {
        if (networkConnected) {
            doFirstTimeConnectedTasks(false, false);
        }
    }
}
