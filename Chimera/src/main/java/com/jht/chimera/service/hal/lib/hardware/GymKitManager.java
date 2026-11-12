package com.jht.chimera.service.hal.lib.hardware;

import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_NOT_PULLED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.bluetooth.BTSinkCommand;
import com.jht.chimera.lib.common.bluetooth.BTSinkDevice;
import com.jht.chimera.lib.common.config.ConsoleType;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.BTSinkKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.mediadevice.MediaItem;
import com.jht.chimera.lib.common.mediadevice.MediaItemParameter;
import com.jht.chimera.lib.common.rfid.WatchData;
import com.jht.chimera.lib.common.string.StringUtil;
import com.jht.chimera.service.usbmedia.ios.lib.auth.AuthCp3;
import com.jht.chimera.ui.mymedia.adapters.MediaItems;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.DeviceState;
import com.jht.gymkitmodule.GsModule;
import com.jht.gymkitmodule.comm.BTConnectionStatus;
import com.jht.gymkitmodule.comm.BTDevice;
import com.jht.gymkitmodule.comm.EventType;
import com.jht.gymkitmodule.comm.FolderItem;
import com.jht.gymkitmodule.comm.GsCommands;
import com.jht.gymkitmodule.comm.GsConnectionStatus;
import com.jht.gymkitmodule.comm.GsMachineMeasurement;
import com.jht.gymkitmodule.comm.GsMachineStatus;
import com.jht.gymkitmodule.comm.GsMachineType;
import com.jht.gymkitmodule.comm.GsMeasurementValue;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;

import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/*
 * Note that "GymKitManager" is a bit of a misnomer, because GymKit is actually an Apple thing
 * which we used to support, but now the GymKitManager *also* handles Bluetooth [audio] sink stuff
 * (i.e. streaming audio to/from other bluetooth devices), and sometimes (e.g. on Parker)
 * doesn't even do the "GymKit" stuff.  Maybe it should be called GymKitAndOrBluetoothSinkManager.
 */
public class GymKitManager {

    public final String TAG = "GymKitManager";
    private final DataMatrix mDataMatrix = new DataMatrix();
    private SerialPort port;
    private GsModule gsModule;
    FrameConfig frameConfig;
    private boolean gymkitConnected = false;
    private final Map<String, String> btDeviceAddressNameMap = new HashMap<>();
    private boolean mHasAudioFocus;
    private int currentConnectionStatus = -1;
    private AudioSourceInput lastSourceBeforeSink = null;
    private final Handler handler = new Handler();
    private BroadcastReceiver mUpdateReceiver;

    @NonNls private static final String ACTION_BT_UPDATE_START = "bt_sink_update_start";
    @NonNls private static final String EXTRA_BT_START_PATH = "path";
    @NonNls private static final String ACTION_BT_UPDATE_COMPLETE = "firmware_update_complete";
    @NonNls private static final String ACTION_BT_UPDATE_PROGRESS = "firmware_update_progress";
    @NonNls private static final String EXTRA_BT_PROGRESS = "progress";

    public static GymKitManager getInstance() {
        return GymKitManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final GymKitManager INSTANCE = new GymKitManager();
    }


    private GymKitManager() {


        frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_LOG_GYMKIT_BOOL, value -> {

            if(gsModule != null) {
                gsModule.setLogCommunication(value);
            }
        });

        AudioSelectManager.monitorAudioLoss(AudioSourceInput.BT_Sink, mDataMatrix, () -> {
            if (gsModule != null) gsModule.sendCommand(GsCommands.AvrcpPause.Value);
        });
    }

    public void start(String device) {
        if (device == null || device.isEmpty()) return;

        Log.e(TAG, "GymKitManager - start(). TOP");
        reset();
        SerialPortConfiguration serialPortConfiguration = new SerialPortConfiguration();
        serialPortConfiguration.setVFlags = false;
        serialPortConfiguration.VMIN = 1;
        serialPortConfiguration.VTIME = 0;
        serialPortConfiguration.makeRaw = true;
        serialPortConfiguration.baudRate = SerialPortConfiguration.BaudRate.B115200;
        serialPortConfiguration.csize = SerialPortConfiguration.CSIZE.CS8;
        serialPortConfiguration.CLOCAL = true;
        serialPortConfiguration.CREAD = true;
        serialPortConfiguration.TRACE_DEBUG = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_GYMKIT_BOOL);

        port = new SerialPort(device, serialPortConfiguration); //NON-NLS
        try {
            port.open();
        } catch (IOException e) {
            Log.w(TAG, e);
            return;
        }
        gsModule = new GsModule(port.getInputStream(), port.getOutputStream(), new GsModule.IGsModule() {
            @Override
            public void showGymKitCalories() {
                Log.d(TAG, "showGymKitCalories " + DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT));
                DataMatrix.putInt(HALKeys.DATA_GYM_KIT_CALORIES_ACTIVE_INT, (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT));
                DataMatrix.putInt(HALKeys.DATA_GYM_KIT_CALORIES_TOTAL_INT, (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT));
            }

            @Override
            public void setActiveCalories(double v) {
                Log.d(TAG, "setActiveCalories " + v);
                DataMatrix.putInt(HALKeys.DATA_GYM_KIT_CALORIES_ACTIVE_INT, (int) v);
            }

            @Override
            public void setTotalCalories(double v) {
                Log.d(TAG, "setTotalCalories " + v);
                DataMatrix.putInt(HALKeys.DATA_GYM_KIT_CALORIES_TOTAL_INT, (int) v);
            }

            @Override
            public void setHeartRate(int i) {
                DataMatrix.putInt(HALKeys.ACTION_UPDATE_GYM_KIT_HEART_RATE_INT, i);
            }

            @Override
            public void setAverageHeartRate(int i) {
                DataMatrix.putInt(HALKeys.DATA_GYM_KIT_AVE_HEART_RATE_INT, i);
            }

            @Override
            public void setConnectionState(byte connectionState) {
                Log.d(TAG, "state: " + connectionState);
                gymkitConnected = connectionState != GsConnectionStatus.Disconnected.value;

                DeviceState deviceState = DeviceState.Connecting;
                if(connectionState == GsConnectionStatus.Connected.value){
                    deviceState = DeviceState.Connected;
                }else if (connectionState == GsConnectionStatus.Disconnected.value){
                    deviceState = DeviceState.Disconnected;
                }else if (connectionState == GsConnectionStatus.OutOfRange.value) {
                    deviceState = DeviceState.TemporarilyDisconnected;
                }
                DataMatrix.putInt(HALKeys.DATA_GYM_KIT_STATUS_INT, deviceState.ordinal());
                if(connectionState == GsConnectionStatus.OutOfRange.value){
                    DataMatrix.putInt(HALKeys.ACTION_UPDATE_GYM_KIT_HEART_RATE_INT, -1);
                }
                updateFTMSValues();
            }

            @Override
            public void getAuthenticationCertificate() {
                try (AuthCp3 authCp3 = new AuthCp3(ProductConfig.portMappings.authCp3Path())) {
                    gsModule.setAuthenticationCertificate(authCp3.getAccessoryCertificate());
                } catch (Exception e) {
                    Log.e(TAG, "getAuthenticationCertificate failed", e);
                }
            }

            @Override
            public void getAuthenticationChallengeResponse(byte[] responseData) {
                try (AuthCp3 authCp3 = new AuthCp3(ProductConfig.portMappings.authCp3Path())) {
                    gsModule.setAuthenticationChallengeResponse(authCp3.getChallengeResponse(responseData));
                } catch (Exception e) {
                    Log.e(TAG, "getAuthenticationChallengeResponse failed", e);
                }
            }

            @Override
            public void notifyBTDeviceFound(BTDevice btDevice) {
                Log.i(TAG, "notifyBTDeviceFound: " + btDevice.getDeviceName() + ", class: " + Arrays.toString(btDevice.getDeviceClass()) + ", Address: " + Arrays.toString(btDevice.getAddress()));
                Log.e(TAG, "name: " + btDevice.getDeviceName() + ", class: " +
                        StringUtil.byteArray2Hex(btDevice.getDeviceClass()) + "3rd byte: " +
                         btDevice.getDeviceClass()[1]);

                if(btDevice.getDeviceClass()[1] == 2 || btDevice.getDeviceClass()[1] == 1) {
                    BTSinkDevice.sendDevice(btDevice.getDeviceName(), btDevice.getAddress());
                    btDeviceAddressNameMap.put(StringUtil.byteArray2Hex(btDevice.getAddress()), btDevice.getDeviceName());
                }
            }

            @Override
            public void notifyBTConnection(BTConnectionStatus status) {
                //TODO: Check disconnection state is properly returning here
                int connectionStatus = status.getStatus();
                if(connectionStatus == GsConnectionStatus.Connected.value)
                {
                    if (!ProductConfig.isParker()) {
                        if(DataMatrix.getInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT) != AudioSourceInput.BT_Sink.ordinal()) {
                            // Need to switch.
                            lastSourceBeforeSink = AudioSourceInput.values()[DataMatrix.getInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT)];
                            DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.BT_Sink.ordinal());
                            mHasAudioFocus = true;
                        }
                    }
                    DataMatrix.putBytes(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_BYTES, status.getAddress());
                    String deviceName = btDeviceAddressNameMap.get(StringUtil.byteArray2Hex(status.getAddress()));
                    DataMatrix.putString(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_NAME_STRING, deviceName == null ? "" : deviceName);
                    DataMatrix.putBoolean(HALKeys.DATA_BT_SINK_CONNECTED_BOOL, true);
                } else if(connectionStatus == 4) {
                    //if Audio Disabled then Volume would be zero
                    Log.e(TAG, "master volume: " + DataMatrix.getInt(SettingKeys.Hardware.DATA_MASTER_VOLUME_V2_INT));

                    currentConnectionStatus = 4;
                    DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_SHOULD_STOP_SCANNER_TIMER_BOOL, true);
                    gsModule.registerNotification(EventType.EVENT_PLAYBACK_STATUS_CHANGED, 0);
                    gsModule.registerNotification(EventType.EVENT_TRACK_CHANGED, 0);
                    gsModule.registerNotification(EventType.EVENT_PLAYBACK_POS_CHANGED, 1);
                    gsModule.registerNotification(EventType.EVENT_PLAYER_APPLICATION_SETTING_CHANGED, 0);
                    gsModule.registerNotification(EventType.EVENT_NOW_PLAYING_CONTENT_CHANGED, 0);

                    DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_IS_AVRCP_READY_BOOL, true);
                } else if (connectionStatus == 6) {
                } else if(connectionStatus == 5) {
                    disconnectMedia();
                } else if(connectionStatus == 7){
                    disconnectMedia();
                }
            }

            @Override
            public void updateControlPointSpeed(float v) {

            }

            @Override
            public void updateControlPointIncline(float v) {

            }

            @Override
            public void updateControlPointResistance(int i) {

            }

            @Override
            public void notifyFolderItemFound(FolderItem folderItem) {
                String uid = "";
                byte[] uidByteArrary = folderItem.folder_uid_bytes;
                for(int i = 0; i < uidByteArrary.length; i++) {
                    if(i < uidByteArrary.length - 1)
                        uid += uidByteArrary[i] + ",";
                    else uid += uidByteArrary[i];
                }
                MediaItemParameter mediaItemParameter = new MediaItemParameter(uid,
                        folderItem.attribute_id, folderItem.name, folderItem.is_playable == 1, folderItem.current_folder_uid_bytes);
                DataMatrix.putParcelable(BTSinkKeys.ACTION_MEDIA_FOLDER_ITEM_DISCOVER_PARCELABLE, mediaItemParameter);
           }

            @Override
            public void notifyFirmwareVersionFound(String version) {
                DataMatrix.putString(HALKeys.DATA_GYM_KIT_FIRMWARE_VERSION_STRING, version);

                Log.e(TAG, "version: " + version);
                // Update the bluetooth firmware version for the update manager
                Context context = ContextUtil.getApplicationContext();
                if (context != null)
                {
                    AndroidSetting.putString(context.getContentResolver(), "chimera_bluetooth_version", version); //NON-NLS
                }
            }

            @Override
            public void notifyPlaybackStatusChanged(int status) {
                if(status == 1) {
                    if(DataMatrix.getInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT) != AudioSourceInput.BT_Sink.ordinal()) {
                        // Need to switch.
                        lastSourceBeforeSink = AudioSourceInput.values()[DataMatrix.getInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT)];
                        DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, AudioSourceInput.BT_Sink.ordinal());
                        mHasAudioFocus = false;
                    }
                    DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_PLAYBACK_IS_PLAYING_BOOL, true);
                }
                else if(status == 2 || status == 0){
                    DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_PLAYBACK_IS_PLAYING_BOOL, false);

                    if (ProductConfig.isParker()) {
                        // only switch if the source hasn't changed yet to the previous source when not playing
                        if (DataMatrix.getInt(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT) == AudioSourceInput.BT_Sink.ordinal()) {
                            mHasAudioFocus = false;
                            // use a delay to catch case the media app is changing song manually which causes a pause and a play event
                            handler.postDelayed(() -> {
                                if (!mHasAudioFocus) DataMatrix.putInt(HALKeys.ACTION_AUDIO_SELECT_SOURCE_INPUT_ENUM_INT, lastSourceBeforeSink.ordinal());
                            }, 1000);
                        }
                    }
                }

                Log.e(TAG, "notifyPlaybackStatusChanged - status: " + status);
            }

            @Override
            public void notifyCurrentPlayerAppSettingsChanged(int repeat, int shuffle) {
                DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SET_SHUFFLE_BOOL, shuffle != 1);
                DataMatrix.putInt(BTSinkKeys.DATA_BT_SET_REPEAT_INT, repeat);

                Log.e(TAG, "notifyCurrentPlayerAppSettingsChanged - repeat: " + repeat + ", shuffle: " + shuffle);
            }

            @Override
            public void notifyPlayContentChanged() {
                Log.e(TAG, "notifyPlayContentChanged");
            }

            @Override
            public void notifyNowPlayingInfo(String songTitle, String artistName) {
                Log.e(TAG, "notifyNowPlayingInfo: song: " + songTitle + ", artist: " + artistName);
                String currentSong = DataMatrix.getString(BTSinkKeys.DATA_BT_SINK_PLAYBACK_SONG_STRING);
                String currentArtist = DataMatrix.getString(BTSinkKeys.DATA_BT_SINK_PLAYBACK_ARTIST_STRING);

                if(!(songTitle.equals(currentSong) && artistName.equals(currentArtist))) {
                    DataMatrix.putString(BTSinkKeys.DATA_BT_SINK_PLAYBACK_SONG_STRING, songTitle);
                    DataMatrix.putString(BTSinkKeys.DATA_BT_SINK_PLAYBACK_ARTIST_STRING, artistName);
                }

            }

            @Override
            public void notifyNewFolderPath(byte[] currentFolderBytes) {
                Log.e(TAG, "notifyNewFolderPath. Bytes: " + Arrays.toString(currentFolderBytes));
                DataMatrix.putBytes(BTSinkKeys.DATA_BT_SINK_CURRENT_FOLDER_BYTES, currentFolderBytes);
            }

            @Override
            public void notifyChangePathResponse(int numOfItems) {
                Log.e(TAG, "gymkitmanager num of items: " + numOfItems);
                DataMatrix.putInt(BTSinkKeys.DATA_BT_SINK_NUM_FOLDER_ITEMS_INT, numOfItems);
            }

            @Override
            public void notifyReset() {
                Log.e(TAG, "NotifyReset()");
                DataMatrix.putInt(BTSinkKeys.DATA_BT_SINK_NUM_FOLDER_ITEMS_INT, -1);
                DataMatrix.putString(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_NAME_STRING, "");
                DataMatrix.putBytes(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_BYTES, null);
                DataMatrix.putBoolean(HALKeys.DATA_BT_SINK_CONNECTED_BOOL, false);
                DataMatrix.putInt(HALKeys.DATA_FOLDER_PATH_COUNTER_INT, 0);
            }

            @Override
            public void notifyPlayStatus(Bundle bundle) {
                DataMatrix.putParcelable(BTSinkKeys.DATA_BT_SINK_PLAY_STATUS_PARCELABLE, bundle);
            }

            @Override
            public void updateCompletionStatus(int progress) {
                Intent intent = new Intent(ACTION_BT_UPDATE_PROGRESS);
                intent.putExtra(EXTRA_BT_PROGRESS, progress);
                Context context = ContextUtil.getApplicationContext();
                if (context != null){
                    context.sendBroadcast(intent);
                }
            }

            @Override
            public void notifyFFRewindUP(boolean startSongPositionTimer) {
                DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_CAN_START_SONG_POSITION_TIMER_BOOL, startSongPositionTimer);
            }

            @Override
            public void notifyPincodeReceived(int pincode) {
                Log.e(TAG, "GymkitManager. THE NUMBER: " + pincode);
                DataMatrix.putInt(BTSinkKeys.DATA_BT_SINK_PINCODE_NOTIFICATION_INT, pincode);
            }
            
        });

        gsModule.setLogCommunication(false);

        mDataMatrix.createEventAdapter(BTSinkKeys.ACTION_PAIRING_COMPLETE, event -> {

            if(currentConnectionStatus == 4) {
                DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_SHOULD_STOP_SCANNER_TIMER_BOOL, true);
                gsModule.registerNotification(EventType.EVENT_PLAYBACK_STATUS_CHANGED, 0);
                gsModule.registerNotification(EventType.EVENT_TRACK_CHANGED, 0);
                gsModule.registerNotification(EventType.EVENT_PLAYBACK_POS_CHANGED, 1);
                gsModule.registerNotification(EventType.EVENT_PLAYER_APPLICATION_SETTING_CHANGED, 0);
                gsModule.registerNotification(EventType.EVENT_NOW_PLAYING_CONTENT_CHANGED, 0);

                DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_IS_AVRCP_READY_BOOL, true);
            }
        });

        mDataMatrix.createParcelableAdapter(HALKeys.EVENT_APPLE_WATCH_DETECTED_PARCELABLE, WatchData.class, watchData -> {
            //Ignore new incoming GymKit connections if one is already connected
            if(!gymkitConnected) {
                gsModule.ndefDetected(watchData.watchData());
            }
        }, false);

        mDataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, s -> configureMachineType());

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
            switch (value) {
                case WorkoutKeys.Constants.RUN_STATE_PAUSE:
                    gsModule.notifyStatusChange(GsMachineStatus.Paused);
                    break;
                case WorkoutKeys.Constants.RUN_STATE_RUNNING:
                    gsModule.notifyStatusChange(GsMachineStatus.StartedOrResumedByUser);
                    break;
                case WorkoutKeys.Constants.RUN_STATE_STOP:
                    if(DataMatrix.getBoolean(BTSinkKeys.DATA_BT_SINK_IS_AVRCP_READY_BOOL)) {
                        Log.e(TAG, "PAUSING in RUN_STATE");
                        gsModule.sendCommand(GsCommands.AvrcpPause.Value);
                    }
                    if (!DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) && DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT) != SAFETY_KEY_NOT_PULLED) {
                        gsModule.notifyStatusChange(GsMachineStatus.StoppedBySafetyKey);
                    } else {
                        gsModule.notifyStatusChange(GsMachineStatus.Stopped);
                    }
                    break;
            }
        }, false);

        //Update stats after cycle
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_WORKOUT_STATS_UPDATED, s -> updateFTMSValues(), false);
        Log.e(TAG, "version: " + gsModule.getFirmwareVersion());

        //Disconnect watch when splash screen reset logic runs
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_RESET_STATS, key -> gsModule.notifyStatusChange(GsMachineStatus.Stopped));

        //BT Sink calls
        mDataMatrix.createIntAdapter(HALKeys.ACTION_SEND_BT_SINK_COMMAND_INT, value ->
        {
            Log.e(TAG, "ACTION_SEND_BT_SINK_COMMAND_INT. Command: " + value);
            if(value == GsCommands.AvrcpPlay.Value || value == GsCommands.AvrcpPause.Value){

            }


            if(value == GsCommands.BtStopScan.Value) {
                //btDeviceAddressNameMap.clear();

            }


            gsModule.sendCommand(value);

        }, false);

        // Update the bluetooth module when we receive this, return response to updater
        mUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String filePath = intent.getStringExtra(EXTRA_BT_START_PATH);
                Log.d(TAG, "Starting bluetooth update with path " + filePath);
                gsModule.sendCommand(GsCommands.EnterBootloaderMode.Value);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.w(TAG, e);
                }
                boolean successful = gsModule.startBluetoothUpdate(filePath);
                Log.d(TAG, "Done with update success=" + successful);

                Intent returnBroadcast = new Intent(ACTION_BT_UPDATE_COMPLETE);
                returnBroadcast.putExtra("success", successful);
                Context appContext = ContextUtil.getApplicationContext();
                if (appContext != null) {
                    ContextUtil.getApplicationContext().sendBroadcast(returnBroadcast);
                }
                else {
                    Log.e(TAG, "Context was null when trying to send broadcast! This will cause update to fail!");
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_BT_UPDATE_START);

        Context context = ContextUtil.getApplicationContext();
        if (context != null)
        {
            context.registerReceiver(mUpdateReceiver, filter);
        }
        Log.d(TAG, "Starting BT update broadcast receiver");

        mDataMatrix.createParcelableAdapter(HALKeys.ACTION_SET_CURRENT_PLAYER_APP_SETTING_VALUE_PARCELABLE, Bundle.class, value -> {
            int id = value.getInt("id"); //NON-NLS
            int setting_value = value.getInt("value"); //NON-NLS
            gsModule.setCurrentPlayerAppSettingValue(id, setting_value);
        });

        mDataMatrix.createParcelableAdapter(HALKeys.ACTION_CHANGE_PATH_COMMAND_PARCELABLE, Bundle.class, value -> {
            String[] byteArray;
            int direction = value.getInt("direction"); //NON-NLS
            String uidString = value.getString("uid");
            Log.e(TAG, "the bytes: " + uidString);
            if (uidString != null)
            {
                byteArray = uidString.split(",");

                byte[] uidBytes = new byte[8];
                for (int i = 0; i < uidBytes.length; i++)
                {
                    uidBytes[i] = Byte.parseByte(byteArray[i]);
                }

                gsModule.changePath(direction, uidBytes);
            }

        });


        mDataMatrix.createStringAdapter(HALKeys.ACTION_PLAY_ITEM_COMMAND_STRING, value -> {

            String[] byteArray = value.split(",");
            byte[] uidBytes = new byte[8];
            for(int i = 0; i < uidBytes.length; i++) {
                uidBytes[i] = Byte.parseByte(byteArray[i]);
            }
            gsModule.playItem(uidBytes);
        });

        mDataMatrix.createIntAdapter(HALKeys.EVENT_GET_FOLDER_ITEMS, value -> {
            gsModule.getFolderItems(value);
        });

        mDataMatrix.createParcelableAdapter(HALKeys.ACTION_SEND_BT_DEVICE_COMMAND_PARCELABLE, BTSinkCommand.BtDeviceCommandParcelable.class, command -> {
            gsModule.sendBTDeviceCommand(command.type, command.bytes);
            if(command.type == GsCommands.BtDeleteBond.Value){
                DataMatrix.putBoolean(HALKeys.DATA_BT_SINK_CONNECTED_BOOL, false);
            }
        }, false);
        mDataMatrix.createIntAdapter(HALKeys.ACTION_START_BT_SINK_SCAN_INT, value -> gsModule.startBTScan(value), false);

        mDataMatrix.createIntAdapter(BTSinkKeys.DATA_BT_SINK_GET_PLAYBACK_STATUS_INT, extra -> {
            gsModule.sendCommand(GsCommands.AvrcpGetPlayStatus.Value);
        });

        mDataMatrix.createIntAdapter(HALKeys.DATA_UNREGISTER_NOTIFICATION_INT, event -> gsModule.unregisterNotification(event));

        mDataMatrix.createParcelableAdapter(BTSinkKeys.DATA_BT_SINK_PAIR_PARCELABLE, Bundle.class, bundle -> {
            if (bundle != null) gsModule.pairDevice(bundle.getInt("pairMethod"), bundle.getByteArray("address")); //NON-NLS
        });

        mDataMatrix.createEventAdapter(BTSinkKeys.EVENT_BT_SINK_RESET_SEQUENCE, event -> {
            disconnectMedia();
            if(DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT,0) != HALKeys.STATUS_GYM_KIT_CONNECTED) {}
            currentConnectionStatus = -1;

        });

        mDataMatrix.createBoolAdapter(BTSinkKeys.DATA_BT_SINK_CAN_START_BROWSING_PROCESS_BOOL, canStart -> {
            Log.e(TAG, "gscomm registering again, canStart: " + canStart + ", currentConnection: " + currentConnectionStatus);
            if(canStart && currentConnectionStatus == 4) {}
        });



        String console = "Matrix-" + DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, "btsink"); //NON-NLS
        try
        {
            //TODO: should handle case where there is no bt module
            DataMatrix.putInt(BTSinkKeys.DATA_BT_SINK_NUM_FOLDER_ITEMS_INT, -1);
            DataMatrix.putInt(HALKeys.DATA_FOLDER_PATH_COUNTER_INT, 0);
            Log.e(TAG, "name: " + console);
            configureMachineType();
            configureDeviceInformation();
            gsModule.setBluetoothDeviceName(console);
            gsModule.getDeviceInfo();
        } catch (Exception e) {
            Log.e(TAG, "Exception: ", e);
        }
    }

    private void configureDeviceInformation(){

        int MAX_LENGTH = 16;
        StringBuilder manufacturer_sb = new StringBuilder("Matrix"); //NON-NLS
        while (manufacturer_sb.length() < MAX_LENGTH) {
            manufacturer_sb.append(' ');
        }

        ConsoleType consoleType = ProductConfig.getConsoleType();
        String modelName = "Touch"; //NON-NLS
        if(consoleType == ConsoleType.chimera_22 || consoleType == ConsoleType.chimera2_22){
            modelName = "Touch XL"; //NON-NLS
        }
        else if(consoleType == ConsoleType.athena || consoleType == ConsoleType.athena2) {
            modelName = "CXV"; //NON-NLS
        }
        StringBuilder model_sb = new StringBuilder(modelName);
        while (model_sb.length() < MAX_LENGTH) {
            model_sb.append(' ');
        }

        if(gsModule != null){
            gsModule.setDeviceInformation(manufacturer_sb.toString(), model_sb.toString());
        }
    }

    /**
     * @param currentFolderUIDBytes
     * @param id
     * @param title
     * @param album
     * @param artist
     * @param genre
     * @param duration
     * @param isTrack
     * @return
     */
    private MediaItem createMediaItem(byte[] currentFolderUIDBytes, String id, String title, String album, String artist, String genre, int duration, boolean isTrack) {
        MediaItem item = new MediaItem(currentFolderUIDBytes);
        item.id = id;
        item.title = title;
        item.album = album;
        item.artist = artist;
        item.genre = genre;
        item.duration = duration;
        item.isTrack = isTrack;
        return item;
    }

    public void stop() {
        port.close();
        mDataMatrix.finish();
        Context context = ContextUtil.getApplicationContext();
        if (context != null){
            context.unregisterReceiver(mUpdateReceiver);
        }
    }

    private void reset() {
        Log.d(TAG, "reset calories " + DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT));
        DataMatrix.putInt(HALKeys.DATA_GYM_KIT_STATUS_INT, HALKeys.STATUS_GYM_KIT_DISCONNECTED);
        DataMatrix.putInt(HALKeys.DATA_GYM_KIT_CALORIES_ACTIVE_INT, 0);
        DataMatrix.putInt(HALKeys.DATA_GYM_KIT_CALORIES_TOTAL_INT, 0);
        DataMatrix.putInt(HALKeys.ACTION_UPDATE_GYM_KIT_HEART_RATE_INT, 0);
        DataMatrix.putInt(HALKeys.DATA_GYM_KIT_AVE_HEART_RATE_INT, 0);
    }

    /**
     * Delaying this because to make sure all settings are done in First Time Setup.
     * Otherwise there is a chance we will get the wrong frame config.
     */
    private void configureMachineType(){
        handler.postDelayed(() -> {
            frameConfig = FrameConfigManager.getInstance().getFrameConfig();
            switch (frameConfig.machineType){
                default:
                case Treadmill:
                    gsModule.setMachineType(GsMachineType.Treadmill);
                    break;

                case Ascent:
                case LowerBodyAscent:
                case Elliptical:
                case RecumbentElliptical:
                    gsModule.setMachineType(GsMachineType.CrossTrainer);
                    break;

                case HybridBike:
                case UprightBike:
                case RecumbentBike:
                case CardioSpinBike:
                    gsModule.setMachineType(GsMachineType.IndoorBike);
                    break;

                case Stepper:
                case Climbmill:
                    gsModule.setMachineType(GsMachineType.StepClimber);
                    break;
            }
        }, 1000);
    }

    /**
     * @param measurement
     * @param value
     * @return
     */
    private GsMeasurementValue createMeasurementValue(GsMachineMeasurement measurement, int value) {
        Log.d(TAG, "Adding measurement " + measurement + " " + value);
        return new GsMeasurementValue(measurement, value);
    }

    private void updateFTMSValues(){
        if(gymkitConnected) {
            Log.e(TAG, "value: " + (int)(DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT) * (1/60000.0) * DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) * 2));
            GsMeasurementValue[] values;
            Log.d(TAG, "setTotalCaloriesupdateFTMSValues " + DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT));
            switch (frameConfig.machineType) {
                default:
                case Treadmill:
                    Log.e(TAG, "actual: " +  ((roundSpeed(DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT)))));
                    Log.e(TAG, "actual1: " +  ((roundSpeed(DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT)) * 100.0)));
                    Log.e(TAG, "actual2: " +  ((int)(roundSpeed(DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT)) * 100.0)));
                    {
                    values = new GsMeasurementValue[]
                            {
                                    createMeasurementValue(GsMachineMeasurement.TotalEnergy, (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT) & 0xFFFF),                               // uint16 kilocalories
                                    createMeasurementValue(GsMachineMeasurement.TotalDistance, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 1000.0) & 0xFFFFFF),            // uint24 meters
                                    createMeasurementValue(GsMachineMeasurement.ElapsedTime, DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000 & 0xFFFF),                                 // uint16 seconds
                                    createMeasurementValue(GsMachineMeasurement.InstantaneousSpeed, (int) ((roundSpeed(DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT)) * 100.0) + 1) & 0xFFFF),         // uint16 km/h
                                    createMeasurementValue(GsMachineMeasurement.AverageSpeed, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT) * 100.0) & 0xFFFF),  // uint16 km/h
                                    createMeasurementValue(GsMachineMeasurement.Inclination, (int) (DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT) * 10.0) & 0xFFFF),                   // sint16 %
                                    createMeasurementValue(GsMachineMeasurement.PositiveElevationGain, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) * 10.0) & 0xFFFF)   // uint16 meters
                            };
                }
                break;

                case Ascent:
                case Elliptical:
                case RecumbentElliptical:
                    values = new GsMeasurementValue[]
                            {
                                    createMeasurementValue(GsMachineMeasurement.TotalEnergy, (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT) & 0xFFFF),                                // uint16 kilocalories NOTE CALORIES ARE ALREADY IN KILOCALS (Big C = kcal)
                                    createMeasurementValue(GsMachineMeasurement.TotalDistance, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 1000.0) & 0xFFFFFF),            // uint24 meters
                                    createMeasurementValue(GsMachineMeasurement.ElapsedTime, DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000 & 0xFFFF),                                 // uint16 seconds
                                    createMeasurementValue(GsMachineMeasurement.PositiveElevationGain, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT) * 10.0) & 0xFFFF),  // uint16 meters
                                    createMeasurementValue(GsMachineMeasurement.StrideCount, (int)(DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT) * (1/60000.0) * DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) * 2 * 10)),  //multiply by 10 to counter module probably diving the result by 10                                                                               // uint16
                                    createMeasurementValue(GsMachineMeasurement.ResistanceLevel, (DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT) * 10) & 0xFFFF),                               // sint16
                                    createMeasurementValue(GsMachineMeasurement.InstantaneousPower, (int) (DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT)) & 0xFFFF)                             // sint16 watts
                            };
                    break;

                case HybridBike:
                case UprightBike:
                case RecumbentBike:
                case CardioSpinBike:
                    values = new GsMeasurementValue[]
                            {
                                    createMeasurementValue(GsMachineMeasurement.TotalEnergy, (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT) & 0xFFFF),                               // uint16 kilocalories
                                    createMeasurementValue(GsMachineMeasurement.TotalDistance, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 1000.0) & 0xFFFFFF),            // uint24 meters
                                    createMeasurementValue(GsMachineMeasurement.ElapsedTime, DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000 & 0xFFFF),                                 // uint16 seconds
                                    createMeasurementValue(GsMachineMeasurement.InstantaneousCadence, (DataMatrix.getInt(HALKeys.DATA_RPM_INT) * 2) & 0xFFFF),                                  // uint16 1/m
                                    createMeasurementValue(GsMachineMeasurement.AverageCadence, (int) (Math.floor(DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT)) * 2) & 0xFFFF),      // uint16 1/m
                                    createMeasurementValue(GsMachineMeasurement.ResistanceLevel, DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT) & 0xFFFF),                                      // sint16
                                    createMeasurementValue(GsMachineMeasurement.InstantaneousPower, (int) (DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT)) & 0xFFFF),                            // sint16 watts
                                    createMeasurementValue(GsMachineMeasurement.AveragePower, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_WATTS_FLOAT)) & 0xFFFF)                       // sint16 watts
                            };
                    break;

                case Stepper:
                case Climbmill:
                    values = new GsMeasurementValue[]
                            {
                                    createMeasurementValue(GsMachineMeasurement.TotalEnergy, (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT) & 0xFFFF),                               // uint16 kilocalories
                                    createMeasurementValue(GsMachineMeasurement.ElapsedTime, DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000 & 0xFFFF),                                 // uint16 seconds
                                    createMeasurementValue(GsMachineMeasurement.PositiveElevationGain, (int) (DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT)) & 0xFFFF),          // uint16 meters
                                    createMeasurementValue(GsMachineMeasurement.Floors, (int) DataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT) & 0xFFFF),
//                                    createMeasurementValue(GsMachineMeasurement.StepCount, (int)(DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_RPM_FLOAT) * (1/60000.0) * DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) * 2))
                                    createMeasurementValue(GsMachineMeasurement.StepCount, DataMatrix.getInt(WorkoutKeys.DATA_STEPS_INT) & 0xFFFF)
                            };
                    break;
            }

            gsModule.setMachineMeasurementValues(values);

            //Enhanced Speed Data
            gsModule.setEnhancedData((int) (roundSpeed(DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT)) * 1000000.0), (int) (roundSpeed(DataMatrix.getFloat(WorkoutKeys.DATA_WORKOUT_AVERAGE_SPEED_KPH_FLOAT)) * 1000000.0));
        }
    }

    /**
     * @param speed
     * @return
     */
    private double roundSpeed(double speed){
        double roundedSpeed;
        if (DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)){
            roundedSpeed = Math.round(speed * 10)/10.0;
        }
        else{
            roundedSpeed = Math.round(speed * 10 / 1.609344) /10.0 * 1.609344;
        }
        return roundedSpeed;
    }

    private void disconnectMedia(){
        currentConnectionStatus = -1;

        //Clear media list
        DataMatrix.putParcelable(BTSinkKeys.DATA_MEDIA_FOLDER_ITEMS_PARCELABLE, new MediaItems());


        // AVRCP - DISCONNECTED
        DataMatrix.putInt(BTSinkKeys.DATA_BT_SINK_NUM_FOLDER_ITEMS_INT, -1);
        DataMatrix.putString(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_NAME_STRING, "");
        DataMatrix.putBytes(HALKeys.DATA_BT_SINK_CONNECTED_DEVICE_BYTES, null);
        DataMatrix.putBoolean(HALKeys.DATA_BT_SINK_CONNECTED_BOOL, false);
        DataMatrix.putInt(HALKeys.DATA_FOLDER_PATH_COUNTER_INT, 0);
        DataMatrix.putBytes(BTSinkKeys.DATA_BT_SINK_CURRENT_FOLDER_BYTES, null);
        DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_PLAYBACK_IS_PLAYING_BOOL, false);
        DataMatrix.putBoolean(BTSinkKeys.DATA_BT_SINK_IS_AVRCP_READY_BOOL, false);
    }
}
