package com.jht.chimera.service.hal.lib.hardware;

import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_NOT_PULLED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.config.ConsoleType;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.rfid.PFBarcodeData;
import com.jht.chimera.lib.common.rfid.WatchData;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.lib.fleet.FleetCredentialStore;
import com.jht.chimera.lib.login.WalletPass;
import com.jht.data.RfidOperatingMode;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.DeviceState;
import com.jht.rfid.Legic.RFIDChip;
import com.jht.rfid.LegicEnums.RfStandard;
import com.jht.rfidlib.EcpMode;
import com.jht.rfidlib.ReaderFeatures;
import com.jht.rfidlib.WalletType;
import com.jht.rfidreaders.GymKit.Data.GsConnectionStatus;
import com.jht.rfidreaders.GymKit.Data.GsDeviceInformation;
import com.jht.rfidreaders.GymKit.Data.GsMachineMeasurement;
import com.jht.rfidreaders.GymKit.Data.GsMachineStatus;
import com.jht.rfidreaders.GymKit.Data.GsMachineType;
import com.jht.rfidreaders.GymKit.Data.GsMeasurementValue;
import com.jht.rfidreaders.GymKit.Data.GsWatchType;
import com.jht.rfidreaders.RFID;
import com.jht.rfidreaders.RFID1Hardware;
import com.jht.rfidreaders.RFID2Hardware;
import com.jht.rfidreaders.RFID4Hardware;
import com.jht.rfidreaders.RFIDHardware;
import com.jht.rfidreaders.RFIDReaderFinder;
import com.jht.rfidreaders.WATCH;
import com.jht.serialport.ISerialPort;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * This class will manage the RFID module. It has functionality to find the RFID
 * module (ie RFID1, RFID2, etc). Upon a scanned tag event, it will handle the
 * event accordingly depending on the current state of the console. It has
 * logic to support firmware updates if the module supports it.
 */
public class RfidReaderManager {

    private final String TAG = "RfidReaderManager";
    private final boolean DEBUG = false;

    FrameConfig frameConfig;
    private GsWatchType watchConnected = GsWatchType.NA;
    private RfidOperatingMode lastOperatingMode = null;
    private final EnumSet<GsWatchType> lastAllowedDevices = EnumSet.allOf(GsWatchType.class);
    private boolean blockWatches = false;

    // keep these strings the same so we do have to add new one to updater service
    @NonNls
    private static final String EXTRA_BT_START_PATH = "path";
    @NonNls
    private static final String ACTION_BT_UPDATE_COMPLETE = "firmware_update_complete";
    @NonNls
    private static final String ACTION_BT_UPDATE_PROGRESS = "firmware_update_progress";
    @NonNls
    private static final String EXTRA_BT_PROGRESS = "progress";

    private ISerialPort port;
    private final DataMatrix dataMatrix = new DataMatrix();
    private boolean callbackRegistered = false;
    private final Context context;
    private int tries = 0;

    private RFIDHardware rfidReader;
    private final RFIDChip[] chipsToIgnore = {
            new RFIDChip(RfStandard.Iso14443B, new byte[]{0x11, 0x22, 0x33, 0x44}), // Samsung watch RFID 2
            new RFIDChip(RfStandard.Iso14443B, new byte[]{-30, -121, -32, -17}),        // Apple Watch
            new RFIDChip(RfStandard.Iso14443A, new byte[]{0, 1, 2, 3}),                 // Apple Watch
    };

    public RfidReaderManager(Context context) {
        frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        this.context = context;
    }

    /**
     * Check if the RFID is found
     *
     * @return true if RFID is found; otherwise false
     */
    public boolean isFound() {
        return rfidReader != null;
    }

    /**
     * Get the broadcast receiver action name for firmware update (ie bt_rfid2_update for RFID2)
     *
     * @return broadcast receiver action name
     */
    String getBroadcastReceiverUpdateActionName() {
        if (rfidReader != null) {
            return String.format("bt_%s_update", rfidReader.getModuleName());
        }
        return "";
    }

    /**
     * Get the RFID modules to detect, in the order to detect them. Every module ahead of the one actually
     * fitted costs a response timeout, so put the module the frame is expected to have first. Vortex frames
     * use the SGM (RFID4).
     *
     * @return unstarted readers to probe in order; build these per detection attempt since find() starts them
     */
    private static RFIDHardware[] getHardwareToDetect() {
        if (ProductConfig.hardware.isVortexBase()) {
            return new RFIDHardware[]{new RFID4Hardware(), new RFID2Hardware(), new RFID1Hardware()};
        }
        return RFIDReaderFinder.getDefaultHardware();
    }

    /**
     * Send an event 1 second after the first detection try has completed. The delay is to make sure that any commands
     * that retrieve info stored in RFID is complete before sending the event. For example, fleet provisioning needs
     * data from SGM to complete.
     */
    private void sendEventFirstTryComplete() {
        if (tries == 0) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "sendEventFirstTryComplete RFID found: " + (rfidReader != null));
                DataMatrix.putBoolean(HALKeys.DATA_RFID_FINDER_FIRST_TRY_COMPLETE_BOOL, true);
            }, 1000);
        }
    }

    /**
     * This function will try to detect if a RFID module is connected to the specified device path.
     * If a RFID module is found, it will perform some initialization.
     *
     * @param devicePath
     */
    public void start(String devicePath) {
        stop(); // cover re-entrance

        SerialPortConfiguration serialPortConfiguration = new SerialPortConfiguration();
        serialPortConfiguration.setVFlags = false;
        serialPortConfiguration.VMIN = 1;
        serialPortConfiguration.VTIME = 0;
        serialPortConfiguration.makeRaw = true;
        serialPortConfiguration.baudRate = SerialPortConfiguration.BaudRate.B115200;
        serialPortConfiguration.csize = SerialPortConfiguration.CSIZE.CS8;
        serialPortConfiguration.CLOCAL = true;
        serialPortConfiguration.CREAD = true;
        serialPortConfiguration.TRACE_DEBUG = false;

        port = new SerialPort(devicePath, serialPortConfiguration);
        new Thread(()->{
            try {
                Log.d(TAG, "RFID Initial Wait For Power");
                Thread.sleep(1000);

                tries = 0;
                while (!Thread.interrupted()) {
                    //Opens the port, if this attempt fails, the port will close and we should retry opening
                    port.open();

                    Log.d(TAG, "RFID Search attempt " + tries);
                    CountDownLatch conditionLatch = new CountDownLatch(1);
                    RFIDReaderFinder.find(port.getInputStream(), port.getOutputStream(), rfidEventListener, rfidReader -> {
                        RfidReaderManager.this.rfidReader = rfidReader;
                        conditionLatch.countDown();
                        // Some RFID readers have mode toggles to handle wallets. Set a known default here for standard operation.
                        updateScanOptions(RfidOperatingMode.Normal);
                    }, 1, 1000, getHardwareToDetect());
                    conditionLatch.await();
                    sendEventFirstTryComplete();
                    if (RfidReaderManager.this.rfidReader != null) {
                        DataMatrix.putString(HALKeys.DATA_RFID_MODULE_NAME_STRING, rfidReader.getModuleName());
                        if (rfidReader.supportsUpdate()) {
                            context.registerReceiver(updateReceiver, new IntentFilter(getBroadcastReceiverUpdateActionName()));
                            // when powering up with new RFID hardware, feature mask from previous will probably be there until new hardware is found
                            // is it worth burning a data matrix key? or scraping the module name off the version string?
                            long featureMask = ReaderFeatures.toBitmask(rfidReader.getSupportedFeatures());
                            AndroidSetting.putLong(context.getContentResolver(), "chimera_rfid_features", featureMask);
                        }
                        registerCallbacks();
                        setupWallet();
                        setupWatchInterface();
                        break;
                    } else if (++tries > 30) {
                        port.close();
                        break;
                    }
                    port.close();
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                Log.e(TAG, "RFID Search thread crashed", e);
            }
        }, "rfidSearch").start();
    }

    public static EnumSet<ReaderFeatures> getReaderFeatures() {
        long featureMask = 0L;
        Context context = ContextUtil.getApplicationContext();
        if (context != null) {
            featureMask = AndroidSetting.getLong(context.getContentResolver(), "chimera_rfid_features", 0L);
        }
        return ReaderFeatures.fromBitmask(featureMask);
    }

    /**
     * Register for DataMatrix callbacks
     */
    private void registerCallbacks() {
        if (callbackRegistered) return;
        callbackRegistered = true;

        // disable scanning in summary
        dataMatrix.createIntAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, state -> {
            if (rfidReader == null || rfidReader.rfid == null) return;
            if (state == UiServiceKeys.WorkoutPanelState.WORKOUT_SUMMARY) {
                rfidReader.rfid.stopScan();
            } else {
                rfidReader.rfid.startScan();
            }
        }, false);

        dataMatrix.createBoolAdapter(SettingKeys.AssetManagement.DATA_ENABLE_RFID_LOGIN_BOOL, enabled -> {
            if (rfidReader == null || rfidReader.rfid == null) return;
            rfidReader.rfid.scanRFID(enabled);
        });

        dataMatrix.createIntAdapter(HALKeys.ACTION_RFID_OPERATING_MODE_INT, modeOrdinal -> {
            try {
                RfidOperatingMode operatingMode = RfidOperatingMode.getEntries().get(modeOrdinal);
                if (lastOperatingMode == operatingMode) return;
                updateScanOptions(operatingMode);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set RFID operating mode", e);
            }
        });
        dataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL, enabled -> {
            boolean changed;
            if (enabled) changed = lastAllowedDevices.add(GsWatchType.Apple);
            else changed = lastAllowedDevices.remove(GsWatchType.Apple);
            if (changed) updateScanOptions();
        });
        dataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_SAMSUNG_WATCH_ENABLE_BOOL, enabled -> {
            boolean changed;
            if (enabled) changed = lastAllowedDevices.add(GsWatchType.Samsung);
            else changed = lastAllowedDevices.remove(GsWatchType.Samsung);
            if (changed) updateScanOptions();
        });
        dataMatrix.createBoolAdapter(HALKeys.ACTION_BLOCK_SMART_WATCHES_BOOL, block -> {
            if (blockWatches != block) {
                blockWatches = block;
                updateScanOptions();
            }
        });

        dataMatrix.createStringAdapter(HALKeys.ACTION_FLEET_PUSH_APPLE_MSG_BASE64_STRING, msg -> {
            if (rfidReader == null || rfidReader.rfid == null) return;
            if (msg.isEmpty()) {
                Log.d(TAG, "Deleting Apple Wallet Info");
                byte[] emptyPrivateKey = new byte[32];
                List<RFID.ApplePayMerchant> applePayMerchantList = new LinkedList<>();
                applePayMerchantList.add(new RFID.ApplePayMerchant("pass.nopass", emptyPrivateKey));
                rfidReader.rfid.setApplePayMerchantList(applePayMerchantList);
            } else {
                byte[] data = Base64.decode(msg, Base64.NO_WRAP);
                rfidReader.rfid.deliverKeyApple(data);
            }
        });

        dataMatrix.createStringAdapter(HALKeys.ACTION_FLEET_PUSH_GOOGLE_MSG_BASE64_STRING, msg -> {
            if (rfidReader == null || rfidReader.rfid == null) return;
            if (msg.isEmpty()) {
                Log.d(TAG, "Deleting Google Wallet Info");
                byte[] emptyPrivateKey = new byte[32];
                rfidReader.rfid.setGooglePayMerchant(new RFID.GooglePayMerchant(emptyPrivateKey, 0x00000000, 0, 0));
            } else {
                byte[] data = Base64.decode(msg, Base64.NO_WRAP);
                rfidReader.rfid.deliverKeyGoogle(data);
            }
        });
    }

    RFID.RFIDEventListener rfidEventListener = new RFID.RFIDEventListener() {
        @Override
        public void onVersion(String version) {
            if (DEBUG) Log.d(TAG, "version: " + version + "-" + rfidReader.getModuleName());
            DataMatrix.putString(HALKeys.DATA_VERSION_NFC_STRING, version + "-" + rfidReader.getModuleName());

            // put the version on the into the Android Settings Secure which the update manager uses to get the version
            // (ie set chimera_rfid2_version to be this version)
            if (rfidReader.supportsUpdate()) {
                AndroidSetting.putString(context.getContentResolver(),
                        String.format("chimera_%s_version", rfidReader.getModuleName()),
                        version);
            }
        }

        @Override
        public void onTagScanned(RFIDChip rfidChip) {
            sendTagEvent(rfidChip);
        }

        @Override
        public void onAppleWatchScanned(RFIDChip rfidChip, byte[] watchData) {
            if (DEBUG) Log.d(TAG, "==> onAppleWatchScanned: " + rfidChip);
            if (rfidReader.watch != null) return; // not necessary, for safety
            if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_GYMKIT_ENABLE_BOOL)) {
                DataMatrix.putParcelable(HALKeys.EVENT_APPLE_WATCH_DETECTED_PARCELABLE, new WatchData(rfidChip, watchData));
            }
        }

        @Override
        public void onSamsungWatchScanned(RFIDChip rfidChip, byte[] watchData) {
            if (rfidReader.watch != null) return; // not necessary, for safety
            if (DEBUG) Log.d(TAG, "==> onSamsungWatchScanned: " + rfidChip);
            if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_SAMSUNG_WATCH_ENABLE_BOOL)) {
                DataMatrix.putParcelable(HALKeys.EVENT_SAMSUNG_WATCH_DETECTED_PARCELABLE, new WatchData(rfidChip, watchData));
            }
        }

        @Override
        public void onSimpleTextScanned(RFIDChip rfidChip, String text) {
            if (DEBUG) Log.d(TAG, "==> onSimpleTextScanned: " + rfidChip);
            if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_BARCODE_LOGIN_BOOL)) {
                DataMatrix.putParcelable(HALKeys.EVENT_PF_BARCODE_DETECTED_PARCELABLE, new PFBarcodeData(rfidChip, text));
            } else {
                sendTagEvent(rfidChip);
            }
        }

        @Override
        public void onWalletScanned(WalletType walletType, String pass) {
            if (DEBUG) Log.d(TAG, "==> onWalletScanned: " + walletType); // do not log user credentials

            FleetCredentialStore store = FleetCredentialStore.getInstance(context);
            String issuerId = (walletType == WalletType.ApplePay) ? store.getApplePassTypeIdBlocking() : store.getGoogleCollectorIdBlocking();
            if (issuerId == null) issuerId = "unknown";
            WalletPass walletPass = new WalletPass(pass, walletType, issuerId);
            DataMatrix.putParcelable(HALKeys.EVENT_WALLET_PASS_DETECTED_PARCELABLE, walletPass);
        }

        @Override
        public void onWalletResponse(WalletType walletType, byte[] response) {
            Log.d(TAG, "==> onWalletResponse: " + StringUtil.byteArray2Hex(response));
        }

        @Override
        public void onError(String error) {
            Log.e(TAG, "==> onError: " + error);
        }

        @Override
        public void onClaimCertPEM(String pem) {
            DataMatrix.updateString(HALKeys.DATA_FLEET_CLAIM_CERT_STRING, pem);
        }

        @Override
        public void onClaimKeyPEM(String pem) {
            DataMatrix.updateString(HALKeys.DATA_FLEET_CLAIM_KEY_STRING, pem);
        }

        @Override
        public void onAttestation(@NonNull byte[] publicKey, @NonNull byte[] signature) {
            String publicKeyBase64 = Base64.encodeToString(publicKey, Base64.NO_WRAP);
            String signatureBase64 = Base64.encodeToString(signature, Base64.NO_WRAP);
            DataMatrix.updateString(HALKeys.DATA_FLEET_PUBLIC_KEY_BASE64_STRING, publicKeyBase64);
            DataMatrix.updateString(HALKeys.DATA_FLEET_SIGNATURE_BASE64_STRING, signatureBase64);
        }

    };

    private void updateScanOptions() { updateScanOptions(lastOperatingMode); }
    private void updateScanOptions(RfidOperatingMode operatingMode) {
        if (rfidReader == null || rfidReader.rfid == null) return;
        Log.d(TAG, "setting operating mode operatingMode=" + operatingMode + ", lastOperatingMode=" + lastOperatingMode + ", lastAllowedDevices=" + lastAllowedDevices + ", blockWatches="  + blockWatches);
        if (operatingMode == RfidOperatingMode.Wallet) {
            rfidReader.rfid.setScanOption(true, EcpMode.ECP_VASONLY, EnumSet.noneOf(GsWatchType.class));
        } else {
            EnumSet<GsWatchType> allowedDevices = blockWatches ? EnumSet.noneOf(GsWatchType.class) : lastAllowedDevices;
            rfidReader.rfid.setScanOption(false, EcpMode.ECP_GYMKIT, allowedDevices); // default to Normal operation
        }
        lastOperatingMode = operatingMode;
    }

    /**
     * Only send tag event if scan RFID is enabled
     *
     * @param rfidChip
     */
    void sendTagEvent(RFIDChip rfidChip) {
        if (DataMatrix.getBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL) && !ignoreChip(rfidChip)) {
            if (DEBUG) Log.d(TAG, "==> onTagScanned: " + rfidChip);
            DataMatrix.putParcelable(HALKeys.EVENT_RFID_TAG_DETECTED_PARCELABLE, rfidChip);
        } else {
            if (DEBUG) Log.e(TAG, "==> IGNORED onTagScanned: " + rfidChip);
        }
    }

    private boolean ignoreChip(RFIDChip chip) {
        for(RFIDChip chipToIgnore : chipsToIgnore) {
            if(chipToIgnore.isChip(chip)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Perform clean for stopping the manager
     */
    public void stop() {
        if (port != null) try {
            port.close();
        } catch (IOException ignored) {}
        dataMatrix.finish();
        callbackRegistered = false;
        if (rfidReader != null) {
            rfidReader.stop();
            if (rfidReader.supportsUpdate()) ContextUtil.getApplicationContext().unregisterReceiver(updateReceiver);
            rfidReader = null;
        }
    }


    boolean isUpdatingFirmware = false;
    BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (rfidReader == null) return;
            if (!rfidReader.supportsUpdate()) return;

            if (isUpdatingFirmware) return;
            isUpdatingFirmware = true;

            new Thread(() -> {
                String filePath = intent.getStringExtra(EXTRA_BT_START_PATH);
                Log.d(TAG, rfidReader.getModuleName() + " firmware update with path " + filePath);

                boolean successful = rfidReader.updateFirmware(filePath, percent -> {
                    // send progress via broadcast to update service
                    context.sendBroadcast(new Intent(ACTION_BT_UPDATE_PROGRESS).putExtra(EXTRA_BT_PROGRESS, percent));
                    if (DEBUG) Log.d(TAG, "RFID update percent=" + percent);
                });

                // Update service looks for boolean extra for success
                Intent updateCompleteIntent = new Intent(ACTION_BT_UPDATE_COMPLETE);
                updateCompleteIntent.putExtra("success", successful);
                context.sendBroadcast(updateCompleteIntent);
                Log.d(TAG, "Done with update success=" + successful);
                isUpdatingFirmware = false;
            }, "rfidUpdate").start();
        }
    };

    /** If RFID supports wallet, setup certificates and merchant info */
    private void setupWallet() {
        if (rfidReader.rfid == null) return;
        rfidReader.rfid.getClaimCertPEM();
        rfidReader.rfid.getClaimKeyPEM();
        rfidReader.rfid.getAttestation();
    }

    /** If RFID supports FTMS/watches, setup interfaces/listeners/configuration */
    private void setupWatchInterface() {
        if (rfidReader.watch == null) return;
        //rfidReader.enableDebugLogs(DEBUG); // Logs actual low level transactions
        rfidReader.watch.watchInterface = new WATCH.WatchInterface() {
            @Override
            public void onNewDeviceConnected() {
            }
            @Override
            public void onActiveCalories(double v) {
                if (watchConnected == GsWatchType.Apple) {
                    Log.d(TAG, "setActiveCalories " + v);
                    DataMatrix.putInt(HALKeys.DATA_GYM_KIT_CALORIES_ACTIVE_INT, (int) v);
                }
            }
            @Override
            public void onTotalCalories(double v) {
                if (watchConnected == GsWatchType.Apple) {
                    Log.d(TAG, "setTotalCalories " + v);
                    DataMatrix.putInt(HALKeys.DATA_GYM_KIT_CALORIES_TOTAL_INT, (int) v);
                }
            }
            @Override
            public void onHeartRate(int i) {
                Object dmKey = (watchConnected == GsWatchType.Apple ? HALKeys.ACTION_UPDATE_GYM_KIT_HEART_RATE_INT : HALKeys.ACTION_UPDATE_SAMSUNG_HEART_RATE_INT);
                DataMatrix.putInt(dmKey, i);
            }
            @Override
            public void onAverageHeartRate(int i) {
                if (watchConnected == GsWatchType.Apple) DataMatrix.putInt(HALKeys.DATA_GYM_KIT_AVE_HEART_RATE_INT, i);
            }
            @Override
            public void onConnectionState(GsConnectionStatus connectionState, GsWatchType watchType) {
                Log.d(TAG, "state: " + connectionState + ", watch type: " + watchType);
                Object dmKeyState, dmKeyHr;
                switch (watchType) {
                    case Apple:
                        dmKeyState = HALKeys.DATA_GYM_KIT_STATUS_INT;
                        dmKeyHr = HALKeys.ACTION_UPDATE_GYM_KIT_HEART_RATE_INT;
                        break;
                    case Samsung:
                        dmKeyState = HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT;
                        dmKeyHr = HALKeys.ACTION_UPDATE_SAMSUNG_HEART_RATE_INT;
                        break;
                    case NA: return; // if watch is NA, then this status is not for a watch
                    default: Log.d(TAG, "Unknown watch type " + watchType); return;
                }
                watchConnected = (connectionState != GsConnectionStatus.Disconnected ? watchType : GsWatchType.NA);
                DeviceState deviceState = DeviceState.Connecting;
                switch (connectionState) {
                    case Connected:
                        deviceState = DeviceState.Connected;
                        if (watchConnected == GsWatchType.Apple) {
                            float calories = DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
                            Log.d(TAG, "onNewDeviceConnected " + calories);
                            // The watch appears to use calories from FTMS as a starting point for its active/total calories
                            // so we update the values accordingly so we don't see a 0 value temporarily until the watch
                            // sends the active/total calories
                            DataMatrix.updateInt(HALKeys.DATA_GYM_KIT_CALORIES_ACTIVE_INT, (int) calories);
                            DataMatrix.updateInt(HALKeys.DATA_GYM_KIT_CALORIES_TOTAL_INT, (int) calories);
                            DataMatrix.updateFloat(WorkoutKeys.DATA_ACTIVE_CALORIES_FLOAT, calories); // update UI value
                        }
                        break;
                    case OutOfRange: deviceState = DeviceState.TemporarilyDisconnected; break;
                    case Disconnected:
                        deviceState = DeviceState.Disconnected;
                        DataMatrix.putInt(dmKeyHr, 0); // put dashes back so it does not look odd
                        break;
                }
                DataMatrix.putInt(dmKeyState, deviceState.ordinal());
                updateFTMSValues();
            }
        };
        // configure watch setup
        configureMachineType();
        configureDeviceInformation();
        // DM listeners for watch stuff
        dataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, s -> configureMachineType(), false);
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
            switch (value) {
                case WorkoutKeys.Constants.RUN_STATE_PAUSE:
                    rfidReader.watch.setMachineStatus(GsMachineStatus.Paused);
                    break;
                case WorkoutKeys.Constants.RUN_STATE_RUNNING:
                    rfidReader.watch.setMachineStatus(GsMachineStatus.StartedOrResumedByUser);
                    break;
                case WorkoutKeys.Constants.RUN_STATE_STOP:
                    if (!DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) && DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT) != SAFETY_KEY_NOT_PULLED) {
                        rfidReader.watch.setMachineStatus(GsMachineStatus.StoppedBySafetyKey);
                    } else {
                        rfidReader.watch.setMachineStatus(GsMachineStatus.Stopped);
                    }
                    break;
            }
        }, false);
        //Update stats after cycle
        dataMatrix.createEventAdapter(WorkoutKeys.ACTION_WORKOUT_STATS_UPDATED, s -> updateFTMSValues(), false);
        // at splash start, make sure watch is stopped
        dataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET, key -> {
            rfidReader.watch.setMachineStatus(GsMachineStatus.Stopped);
            // make sure if there is a crash or anything, scanning is reset
            blockWatches = false;
            updateScanOptions(RfidOperatingMode.Normal);
        });
        // as splash proceeds to clear defaults, make sure watch is reset
        dataMatrix.createEventAdapter(WorkoutKeys.ACTION_RESET_STATS, key -> disconnectWatches());
        //dataMatrix.createEventAdapter(HALKeys.ACTION_DISCONNECT_SMART_WATCHES, key -> disconnectWatches()); // this might be redundant, it seems to be issued on the way to splash
    }
    private void disconnectWatches() {
        if (rfidReader.watch == null) return;
        rfidReader.watch.setMachineStatus(GsMachineStatus.Reset); // Apple watch wants explicit reset state
        rfidReader.watch.resetModule(false);
    }
    /** Configures FTMS/watch module with our frame info */
    private void configureMachineType() {
        if (rfidReader.watch == null) return;
        frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        switch (frameConfig.machineType){
            default:
            case Treadmill:
                rfidReader.watch.setMachineType(GsMachineType.Treadmill);
                break;
            case Ascent:
            case LowerBodyAscent:
            case Elliptical:
            case RecumbentElliptical:
                rfidReader.watch.setMachineType(GsMachineType.CrossTrainer);
                break;
            case HybridBike:
            case UprightBike:
            case RecumbentBike:
            case CardioSpinBike:
                rfidReader.watch.setMachineType(GsMachineType.IndoorBike);
                break;
            case Stepper:
            case Climbmill:
                rfidReader.watch.setMachineType(GsMachineType.StepClimber);
                break;
        }
    }
    /** Configures FTMS/watch module with our console info */
    private void configureDeviceInformation() {
        if (rfidReader.watch != null){
            ConsoleType consoleType = ProductConfig.getConsoleType();
            GsDeviceInformation info = new GsDeviceInformation();
            info.manufacturer = "Matrix"; //NON-NLS
            info.model = consoleType.marketingName;
            // CGC-21518: The hw/sw revision has to be console info, not SGM info (match it with DeviceInformationServiceConsole)
            info.hardwareRevision = ProductConfig.detectUCBVendor(); //rfidReader.getModuleName();
            info.softwareRevision = BuildConfig.SoftwareVersion; //DataMatrix.getString(HALKeys.DATA_VERSION_NFC_STRING);
            // Pnp ID values copied from gatt server (this might be hardcoded in certain versions of the firmware
            info.pnpVendorIdSource = 1; // BluetoothSig
            info.pnpVendorId = 0x07C4;
            info.pnpProductId = 0xCE60;
            info.pnpProductVersion = 0x0001;
            rfidReader.watch.setDeviceInformation(info);
        }
    }
    /** Sends workout stats update to FTMS/watch module */
    private void updateFTMSValues() {
        if (rfidReader.watch != null) { // old gymkit would only do this when a watch was connected, but SGM will handle data to the actual device
            GsMeasurementValue[] values;
            float speed; // need to use actual for speed, except for treadmill which use target
            float averageSpeed = DataMatrix.getFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT);
            switch (frameConfig.machineType) {
                default:
                case Treadmill: {
                    speed = DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT);
                    values = new GsMeasurementValue[]
                            {
                                    createMeasurementValue(GsMachineMeasurement.TotalEnergy, (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT) & 0xFFFF),                               // uint16 kilocalories
                                    createMeasurementValue(GsMachineMeasurement.TotalDistance, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 1000.0) & 0xFFFFFF),            // uint24 meters
                                    createMeasurementValue(GsMachineMeasurement.ElapsedTime, DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000 & 0xFFFF),                                 // uint16 seconds
                                    createMeasurementValue(GsMachineMeasurement.InstantaneousSpeed, (int) ((roundSpeed(speed) * 100.0) + 1) & 0xFFFF),         // uint16 km/h
                                    createMeasurementValue(GsMachineMeasurement.AverageSpeed, (int) ((roundSpeed((averageSpeed)) * 100.0) + 1) & 0xFFFF),  // uint16 km/h
                                    createMeasurementValue(GsMachineMeasurement.Inclination, (int) (DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT) * 10.0) & 0xFFFF),                   // sint16 %
                                    createMeasurementValue(GsMachineMeasurement.PositiveElevationGain, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) * 10.0) & 0xFFFF)   // uint16 meters
                            };
                }
                break;
                case Ascent:
                case Elliptical:
                case RecumbentElliptical:
                    speed = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
                    values = new GsMeasurementValue[]
                            {
                                    createMeasurementValue(GsMachineMeasurement.TotalEnergy, (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT) & 0xFFFF),                                // uint16 kilocalories NOTE CALORIES ARE ALREADY IN KILOCALS (Big C = kcal)
                                    createMeasurementValue(GsMachineMeasurement.TotalDistance, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 1000.0) & 0xFFFFFF),            // uint24 meters
                                    createMeasurementValue(GsMachineMeasurement.ElapsedTime, DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000 & 0xFFFF),                                 // uint16 seconds
                                    createMeasurementValue(GsMachineMeasurement.PositiveElevationGain, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) * 10.0) & 0xFFFF),  // uint16 meters
                                    createMeasurementValue(GsMachineMeasurement.StrideCount, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT) * (1 / 60000.0) * DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) * 2 * 10)),  //multiply by 10 to counter module probably diving the result by 10                                                                               // uint16
                                    createMeasurementValue(GsMachineMeasurement.ResistanceLevel, (DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT) * 10) & 0xFFFF),                               // sint16
                                    createMeasurementValue(GsMachineMeasurement.InstantaneousPower, (int) (DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT)) & 0xFFFF),                             // sint16 watts
                                    createMeasurementValue(GsMachineMeasurement.AveragePower, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_WATTS_FLOAT)) & 0xFFFF),
                                    createMeasurementValue(GsMachineMeasurement.InstantaneousSpeed, (int) ((roundSpeed(speed) * 100.0) + 1) & 0xFFFF),         // uint16 km/h
                                    createMeasurementValue(GsMachineMeasurement.AverageSpeed, (int) ((roundSpeed((averageSpeed)) * 100.0) + 1) & 0xFFFF),  // uint16 km/h
                                    createMeasurementValue(GsMachineMeasurement.StepPerMinute, DataMatrix.getInt(HALKeys.DATA_RPM_INT) & 0xFFFF),
                                    createMeasurementValue(GsMachineMeasurement.AverageStepRate, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT)) & 0xFFFF),
                            };
                    break;
                case HybridBike:
                case UprightBike:
                case RecumbentBike:
                case CardioSpinBike:
                    speed = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
                    values = new GsMeasurementValue[]
                            {
                                    createMeasurementValue(GsMachineMeasurement.TotalEnergy, (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT) & 0xFFFF),                               // uint16 kilocalories
                                    createMeasurementValue(GsMachineMeasurement.TotalDistance, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 1000.0) & 0xFFFFFF),            // uint24 meters
                                    createMeasurementValue(GsMachineMeasurement.ElapsedTime, DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000 & 0xFFFF),                                 // uint16 seconds
                                    createMeasurementValue(GsMachineMeasurement.InstantaneousCadence, (DataMatrix.getInt(HALKeys.DATA_RPM_INT) * 2) & 0xFFFF),                                  // uint16 1/m
                                    createMeasurementValue(GsMachineMeasurement.AverageCadence, (int) (Math.floor(DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT)) * 2) & 0xFFFF),      // uint16 1/m
                                    createMeasurementValue(GsMachineMeasurement.ResistanceLevel, DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT) & 0xFFFF),                                      // sint16
                                    createMeasurementValue(GsMachineMeasurement.InstantaneousPower, (int) (DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT)) & 0xFFFF),                            // sint16 watts
                                    createMeasurementValue(GsMachineMeasurement.AveragePower, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_WATTS_FLOAT)) & 0xFFFF),                       // sint16 watts
                                    createMeasurementValue(GsMachineMeasurement.InstantaneousSpeed, (int) ((roundSpeed(speed) * 100.0) + 1) & 0xFFFF),         // uint16 km/h
                                    createMeasurementValue(GsMachineMeasurement.AverageSpeed, (int) ((roundSpeed((averageSpeed)) * 100.0) + 1) & 0xFFFF),  // uint16 km/h
                            };
                    break;
                case Stepper:
                case Climbmill:
                    speed = DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT);
                    values = new GsMeasurementValue[]
                            {
                                    createMeasurementValue(GsMachineMeasurement.TotalEnergy, (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT) & 0xFFFF),                               // uint16 kilocalories
                                    createMeasurementValue(GsMachineMeasurement.ElapsedTime, DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000 & 0xFFFF),                                 // uint16 seconds
                                    createMeasurementValue(GsMachineMeasurement.PositiveElevationGain, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT)) & 0xFFFF),          // uint16 meters
                                    createMeasurementValue(GsMachineMeasurement.Floors, (int) DataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT) & 0xFFFF),
//                                    createMeasurementValue(GsMachineMeasurement.StepCount, (int)(DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT) * (1/60000.0) * DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) * 2))
                                    createMeasurementValue(GsMachineMeasurement.StepCount, DataMatrix.getInt(WorkoutKeys.DATA_STEPS_INT) & 0xFFFF),
                                    createMeasurementValue(GsMachineMeasurement.StepPerMinute, DataMatrix.getInt(HALKeys.DATA_SPM_INT) & 0xFFFF),
                                    createMeasurementValue(GsMachineMeasurement.AverageStepRate, DataMatrix.getInt(WorkoutKeys.DATA_AVERAGE_SPM_INT) & 0xFFFF),
                            };
                    break;
            }
            rfidReader.watch.setMachineMeasurementValues(values);
            rfidReader.watch.setEnhancedData((int) (roundSpeed(speed) * 1000000.0), (int) (roundSpeed(DataMatrix.getFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT)) * 1000000.0));
        }
    }
    /** rounds speed values for samsung speed stats */
    private double roundSpeed(double speed){
        double roundedSpeed;
        if (DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)) {
            roundedSpeed = Math.round(speed * 10)/10.0;
        } else {
            roundedSpeed = Math.round(speed * 10 / 1.609344) /10.0 * 1.609344;
        }
        return roundedSpeed;
    }
    /** Packs [GsMeasurementValue] with optional debug log */
    private GsMeasurementValue createMeasurementValue(GsMachineMeasurement measurement, int value) {
        if (DEBUG) Log.d(TAG, "Adding measurement " + measurement + " " + value);
        return new GsMeasurementValue(measurement, value);
    }
}
