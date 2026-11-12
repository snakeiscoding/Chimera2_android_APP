package com.jht.chimera.service.hal.lib.hardware;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.drivers.audio.AudioSourceInput;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.lib.workout.ConstantWattsErgoline;
import com.jht.chimera.lib.workout.WorkoutIds;
import com.jht.chimera.lib.workout.WorkoutInput;
import com.jht.chimera.lib.workout.WorkoutManager;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.WorkoutMap;
import com.jht.chimera.lib.workout.workoutparameters.MinMaxDictionary;
import com.jht.chimera.lib.workoutapps.AppIds;
import com.jht.chimera.service.workout.WorkoutControllerService;
import com.jht.chimera.service.workout.WorkoutSettingOverrides;
import com.jht.csafelib.CsafeClient;
import com.jht.csafelib.CsafeCommand;
import com.jht.csafelib.CsafeCommands;
import com.jht.csafelib.CsafePacket;
import com.jht.csafelib.CsafeState;
import com.jht.csafelib.CsafeUnits;
import com.jht.csafelib.manager.events.IOnCsafeEvent;
import com.jht.csafelib.manager.events.WorkoutMeasurement;
import com.jht.data.WorkoutItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.MinMaxParam;
import com.jht.serialport.ISerialPort;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;
import com.jht.workoutcontrollerlib.workoutdefinition.statements.SettingValue;
import com.jhtp.dapiservice.um.GenderIndentity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Csafe spec - https://web.archive.org/web/20060718175014/http://www.fitlinxx.com/csafe/specification.htm

public class CSafeManager implements IOnCsafeEvent {

    interface IOnCSAFELogin {
        void onLogin(boolean successful);
    }

    public static class LoginHandler implements JHTTimer.IOnTimer {
        private String id = null;
        private IOnCSAFELogin onCSAFELogin;

        private final JHTTimer loginTimeout = new JHTTimer(30000, this);

        void doLogin(String id, IOnCSAFELogin onCSAFELogin) {
            if (CsafeClient.Instance.getState().equals(CsafeState.Idle)) {
                this.id = id;
                this.onCSAFELogin = onCSAFELogin;
                CsafeClient.Instance.setState(CsafeState.HaveId);
                CsafeClient.Instance.replyUnsolicitedStatus();
                loginTimeout.start();
            }
        }

        void onLoginResult(boolean successful) {

            loginTimeout.stop();

            if (onCSAFELogin != null) {
                onCSAFELogin.onLogin(successful);
            }
        }

        public String id() {
            return id;
        }

        @Override
        public void onTimer() {
            onLoginResult(false);
        }

    }

    public static final String TAG = "CSafeManager";
    private final DataMatrix mDataMatrix = new DataMatrix();
    private final FrameConfig mConfig = FrameConfigManager.getInstance().getFrameConfig();

    private ISerialPort serialPort;
    private boolean serialPortOpen = false;
    private final SerialPortConfiguration configuration = new SerialPortConfiguration();
    private final LoginHandler loginHandler = new LoginHandler();
    private int maxVolume = 0;
    private int numChannels = 0;
    volatile private boolean received900MhzConfig = false;
    private final JHTTimer init900MhzTimer = new JHTTimer(5000, () -> {
        if (!received900MhzConfig) {
            CsafeClient.Instance.stop();
            initSerialPort(true);
        }
    });
    private final CSafeWorkout currentWorkout = new CSafeWorkout();

    private static final Object remoteTVSettingLock = new Object();
    private int currentChannel = 0;

    private boolean traceDebug;
    private final Handler handler = new Handler();

    private int currentTime = 0;
    private int lastTime = 0;
    private double currentSpeed = 0;
    private double currentIncline = 0;
    private int currentResistance = 0;
    private int currentWatts = 0;

    public static CSafeManager getInstance() {
        return CSafeManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final CSafeManager INSTANCE = new CSafeManager();
    }

    private CSafeManager() {
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_LOG_CSAFE_BOOL, value -> {
            if (serialPort != null) {
                serialPort.setTraceDebug(value);
            }
            traceDebug = value;
        });
        dataMatrix.createIntAdapter("CSAFETEST", value -> {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    switch (value) {
                        case 0:
                            byte[] empty = {(byte) 0xF1, (byte) 0x00, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(empty);
                            break;
                        case 1:
                            byte[] reset = {(byte) 0xF1, (byte) 0x81, (byte) 0x81, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(reset);
                            break;
                        case 2:
                            byte[] autoupload = {(byte) 0xF1, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x10, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(autoupload);
                            break;
                        case 3:
                            byte[] cmdGetInfo = {(byte) 0xF1, (byte) 0x71, (byte) 0x01, (byte) 0x00, (byte) 0x70, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(cmdGetInfo);
                            break;
                        case 4:
                            byte[] cmdSetProgram = {(byte) 0xF1, (byte) 0x24, (byte) 0x02, (byte) 0x00, (byte) 0x01, 0x27, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(cmdSetProgram);
                            break;
                        case 5:
                            byte[] cmdGoIdle = {(byte) 0xF1, (byte) 0x82, (byte) 0x82, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(cmdGoIdle);
                            break;
                        case 6:
                            byte[] cmdGoInUse = {(byte) 0xF1, (byte) 0x85, (byte) 0x85, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(cmdGoInUse);
                            break;
                        case 7:
                            byte[] cmdSetPower = {(byte) 0xF1, (byte) 0x34, 0x03, 0x64, 0x00, 0x58, (byte) 0x0B, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(cmdSetPower);
                            break;
                        case 8:
                            byte[] cmdSetGear = {(byte) 0xF1, (byte) 0x29, 0x01, 0x0A, (byte) 0x22, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(cmdSetGear);
                            break;
                        case 9:
                            byte[] cmdGoFinished = {(byte) 0xF1, (byte) 0x86, (byte) 0x86, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(cmdGoFinished);
                            break;
                        case 10:
                            byte[] getStatus = {(byte) 0xF1, (byte) 0x80, (byte) 0x80, (byte) 0xF2};
                            CsafeClient.Instance.testPacket(getStatus);
                            break;
                    }
                }
            }, "CSAFETEST").start();
        });
    }

    public void start() {
        // Delay the start.  Give the hardware time to power on.
        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker)
                || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)
        ){
            Log.d(TAG, "open cSafe power");
            Driver.cSafePowerOpen();
        }
        new Handler().postDelayed(this::init, 4000);
    }

    public void stop() {
        CsafeClient.Instance.stop();
        try { serialPort.close(); } catch (IOException ignored) {}
        serialPortOpen = false;
        mDataMatrix.finish();
    }

    private void init() {
        serialPort = new SerialPort(ProductConfig.portMappings.csafePath(), configuration);
        configuration.baudRate = SerialPortConfiguration.BaudRate.B9600;
        configuration.CREAD = true;
        configuration.CLOCAL = true;
        configuration.csize = SerialPortConfiguration.CSIZE.CS8;
        configuration.makeRaw = true;
        configuration.NOCTTY = true;
        configuration.TRACE_DEBUG = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_CSAFE_BOOL);

        mDataMatrix.createIntAdapter(HALKeys.DATA_AUDIO_ACTIVE_SOURCE_INPUT_ENUM_INT, audioSelection -> {
            if (serialPortOpen && DataMatrix.getBoolean(SettingKeys.TV.DATA_TV_REMOTE_TV_BOOL)) {
                boolean mute = audioSelection != AudioSourceInput.RemoteTV.ordinal();
                CsafePacket packet = new CsafePacket();
                int volume = Math.round(DataMatrix.getInt(SettingKeys.TV.DATA_TV_REMOTE_TV_VOLUME_INT) / 100f * maxVolume);
                packet.getCommands().add(CsafeClient.Instance.buildByteCommand(CsafeCommands.SetAudioVolume, mute ? (byte) 0 : (byte) volume));
                CsafeClient.Instance.sendRemoteTVPacket(packet);
            }
        });

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, currentState -> {
            CsafeState csafeState = CsafeClient.Instance.getState();
            currentWorkout.workoutState = currentState;
            if (!DataMatrix.getBoolean(SettingKeys.TV.DATA_TV_REMOTE_TV_BOOL)) {
                switch (currentState) {
                    case WorkoutKeys.Constants.RUN_STATE_RUNNING:
                        switch (csafeState) {
                            case Ready:
                            default:
                                CsafeClient.Instance.setState(CsafeState.OffLine);
                                CsafeClient.Instance.replyUnsolicitedStatus();
                                break;
                            case Idle:
                            case HaveId:
                                CsafeClient.Instance.setState(CsafeState.Manual);
                                CsafeClient.Instance.replyUnsolicitedStatus();
                                break;
                            case Paused:
                                CsafeClient.Instance.setState(CsafeState.InUse);
                                CsafeClient.Instance.replyUnsolicitedStatus();
                                break;
                            case InUse:
                                //In use state from idle will be transitioned to by the CSAFE asynchronous command
                                // as there is no way to differentiate that from the manual state with just the callback
                                break;
                        }
                        break;
                    case WorkoutKeys.Constants.RUN_STATE_PAUSE:
                        //Pause state will only be triggered from the inUse State
                        if (csafeState == CsafeState.InUse) {
                            CsafeClient.Instance.setState(CsafeState.Paused);
                            CsafeClient.Instance.replyUnsolicitedStatus();
                        }
                        break;
                    case WorkoutKeys.Constants.RUN_STATE_STOP:
                        switch (csafeState) {
                            case OffLine:
                            default:
                                CsafeClient.Instance.setState(CsafeState.Ready);
                                CsafeClient.Instance.replyUnsolicitedStatus();
                                break;
                            case InUse:
                            case Paused:
                                CsafeClient.Instance.setState(CsafeState.Finished);
                                CsafeClient.Instance.replyUnsolicitedStatus();
                                break;
                            case Manual:
                                CsafeClient.Instance.setState(CsafeState.Idle);
                                CsafeClient.Instance.replyUnsolicitedStatus();
                                break;
                        }
                        currentWorkout.reset();
                        break;
                }
            }
        }, false);

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, value -> {
            if (value == WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN) {//Stop at cooldown
                if (CsafeClient.Instance.getState() == CsafeState.InUse) {
                    currentWorkout.stopWorkout();
                }
            }
        });


        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, value -> {
            currentTime = value;
            if (currentTime - lastTime > 30 * 1000) {
                if (CsafeClient.Instance.getState() == CsafeState.InUse) {
                    Log.e(TAG, "CSAFE stop workout from heartbeat timer");
                    currentWorkout.stopWorkout();
                }
            }
        });

        mDataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, value -> {
            currentSpeed = value;
        });

        mDataMatrix.createFloatAdapter(HALKeys.DATA_TARGET_INCLINE_FLOAT, value -> {
            currentIncline = value;
        });

        mDataMatrix.createIntAdapter(HALKeys.DATA_RESISTANCE_INT, value -> {
            currentResistance = value;
        });

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_TARGET_WATTS_INT, value -> {
            currentWatts = value;
        });

        mDataMatrix.createIntAdapter(TVKeys.DATA_REMOTE_TV_CURRENT_CHANNEL_INT, channel -> {

            if (serialPortOpen && DataMatrix.getBoolean(SettingKeys.TV.DATA_TV_REMOTE_TV_BOOL)) {
                currentChannel = channel;
                clampChannel();

                CsafePacket packet = new CsafePacket();
                packet.getCommands().add(CsafeClient.Instance.buildByteCommand(CsafeCommands.SetAudioChannel, (byte) (currentChannel)));
                CsafeClient.Instance.sendRemoteTVPacket(packet);
            }
        }, false);

        // this will open the correct port
        mDataMatrix.createBoolAdapter(SettingKeys.TV.DATA_TV_REMOTE_TV_BOOL, enabled -> new Thread(() -> initSerialPort(enabled), "RemoteTvEnable").start());

    }

    private void initSerialPort(boolean remoteTVEnabled) {
        synchronized (remoteTVSettingLock) {
            try {
                CsafeClient.Instance.stop();
                if (serialPortOpen) {
                    serialPort.close();
                    serialPortOpen = false;
                }
                SystemClock.sleep(1000);


                try {
                    if (traceDebug) {
                        Log.i(TAG, "1 checkSerialPort  open serial port");
                    }
                    serialPort.open();
                    serialPortOpen = true;
                } catch (IOException ignored) {
                    if (traceDebug) {
                        Log.i(TAG, "checkSerialPort  open serial error");
                    }
                    return;
                }

                CsafeClient.Instance.start(CSafeManager.this, serialPort.getInputStream(), serialPort.getOutputStream(), remoteTVEnabled);
                received900MhzConfig = false;
                if (remoteTVEnabled) {
                    init900Mhz();
                    init900MhzTimer.start("Csafe900MHz");
                }
            } catch (Exception err) {
                Log.i(TAG, "2 checkSerialPort  open serial error");
                err.printStackTrace();
            }

        }
    }

    private void clampChannel() {
        if (currentChannel <= 0) {
            currentChannel = 1;
        } else if (currentChannel > numChannels) {
            currentChannel = numChannels;
        }
    }

    private void init900Mhz() {
        if (traceDebug) {
            Log.i(TAG, "init900Mhz ");
        }
        CsafePacket packet = new CsafePacket();
//        packet.getCommands().add(CsafeClient.Instance.buildByteCommand(CsafeCommands.Get900MhzConfig, (byte) 0x01));
        packet.getCommands().add(new CsafeCommand(CsafeCommands.Get900MhzConfig));
        CsafeClient.Instance.sendRemoteTVPacket(packet);
    }

    @SuppressWarnings("unused")
    private void login(String id, IOnCSAFELogin onLogin) {
        if (serialPortOpen) {
            loginHandler.doLogin(id, onLogin);
        } else {
            loginHandler.onLoginResult(false);
        }
    }

    @Override
    public void on900MhzConfig(int maxChannels, int maxVolume, int version) {
        if (traceDebug) {
            Log.i(TAG, "on900MhzConfig " + maxChannels + " " + maxVolume + " " + version);
        }
        received900MhzConfig = true;
        numChannels = maxChannels;

        this.maxVolume = maxVolume;
        currentChannel = maxChannels;
        clampChannel();

        DataMatrix.putInt(TVKeys.DATA_REMOTE_TV_NUM_CHANNELS_INT, numChannels);

        CsafePacket packet = new CsafePacket();
        int volume = Math.round(DataMatrix.getInt(SettingKeys.TV.DATA_TV_REMOTE_TV_VOLUME_INT) / 100f * maxVolume);
        packet.getCommands().add(CsafeClient.Instance.buildByteCommand(CsafeCommands.SetAudioVolume, (byte) volume));
        packet.getCommands().add(CsafeClient.Instance.buildByteCommand(CsafeCommands.SetAudioMute, (byte) 0x01));
        CsafeClient.Instance.sendRemoteTVPacket(packet);
        currentChannel = DataMatrix.getInt(SettingKeys.TV.DATA_TV_REMOTE_TV_DEFAULT_CHANNEL_INT);
        clampChannel();
        DataMatrix.putInt(TVKeys.DATA_REMOTE_TV_CURRENT_CHANNEL_INT, currentChannel);
    }

    @Override
    public void setAudioChannel(int channel) {
        if (traceDebug) {
            Log.i(TAG, "setAudioChannel " + channel);
        }
        // Not applicable
    }

    @Override
    public void setMute(boolean mute) {
        if (traceDebug) {
            Log.i(TAG, "setMute " + mute);
        }
        // Not applicable
    }

    @Override
    public void setAudioVolume(int volume) {
        if (traceDebug) {
            Log.i(TAG, "setAudioVolume " + volume);
        }
        // Not applicable
    }

    @Override
    public void setVolumeRange(int min, int max) {
        if (traceDebug) {
            Log.i(TAG, "setVolumeRange " + min + " " + max);
        }
        // Not applicable
    }

    @Override
    public void setChannelRange(int min, int max) {
        if (traceDebug) {
            Log.i(TAG, "setChannelRange " + min + " " + max);
        }
        // Not applicable
    }

    @Override
    public int getAudioChannel() {
        if (traceDebug) {
            Log.i(TAG, "getAudioChannel ");
        }
        // Not applicable
        return 0;
    }

    @Override
    public boolean getMute() {
        if (traceDebug) {
            Log.i(TAG, "getMute ");
        }
        // Not applicable
        return false;
    }

    @Override
    public int getAudioVolume() {
        if (traceDebug) {
            Log.i(TAG, "getAudioVolume ");
        }
        // Not applicable
        return 0;
    }

    @Override
    public byte[] get900MhzConfig() {
        if (traceDebug) {
            Log.i(TAG, "get900MhzConfig ");
        }
        // Not applicable
        return new byte[0];
    }

    @Override
    public int getErrorCode() {
        if (traceDebug) {
            Log.i(TAG, "getErrorCode ");
        }
        // Not applicable
        return 0;
    }

    @Override
    public int getServiceCode() {
        if (traceDebug) {
            Log.i(TAG, "getServiceCode ");
        }
        // Not applicable
        return 0;
    }

    @Override
    public String getId() {
        if (traceDebug) {
            Log.i(TAG, "getId ");
        }
        return loginHandler.id();
    }

    @Override
    public double getMotorCurrentAmps() {
        if (traceDebug) {
            Log.i(TAG, "getMotorCurrentAmps ");
        }
        // Not applicable
        return 0;
    }

    @Override
    public int getTotalUsageSeconds() {
        if (traceDebug) {
            Log.i(TAG, "getTotalUsageSeconds ");
        }
        return DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT);
    }

    @Override
    public WorkoutMeasurement getOdometer() {
        if (traceDebug) {
            Log.i(TAG, "getOdometer ");
        }
        switch (mConfig.machineClassType) {
            case Stepper:
            case Climbmill:
                return new WorkoutMeasurement((int) DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT), CsafeUnits.Floors);

            default:
                float distanceMi = DistanceConvertUtil.convertKilometersToMiles(DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT));
                return new WorkoutMeasurement((int) (distanceMi * 10.0 + 0.5), CsafeUnits.MileTenths);
        }
    }

    @Override
    public boolean isImperial() {
        if (traceDebug) {
            Log.i(TAG, "isImperial ");
        }
        return !DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
    }

    @Override
    public WorkoutMeasurement getUserWeight() {
        if (traceDebug) {
            Log.i(TAG, "getUserWeight ");
        }
        return new WorkoutMeasurement((int) (DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT) * 10 + 0.5), CsafeUnits.KilogramsTenths);
    }

    @Override
    public int getUserAge() {
        if (traceDebug) {
            Log.i(TAG, "getUserAge ");
        }
        return DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT);
    }

    @Override
    public int getUserGender() {
        if (traceDebug) {
            Log.i(TAG, "getUserGender ");
        }
        //None = 0
        int gender = 0;
        switch (GenderIndentity.fromInt(DataMatrix.getInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT))) {
            case Male:
                gender = 1;
                break;
            case Female:
                gender = 2;
                break;
        }
        return gender;
    }

    @Override
    public int manufactureId() {
        // TODO?
        if (traceDebug) {
            Log.i(TAG, "manufactureId ");
        }
        return 0;
    }

    @Override
    public int csafeId() {
        // TODO?
        if (traceDebug) {
            Log.i(TAG, "csafeId ");
        }
        return 0;
    }

    @Override
    public int modelId() {
        // TODO?
        if (traceDebug) {
            Log.i(TAG, "modelId ");
        }
        return 0;
    }

    @Override
    public int major() {
        if (traceDebug) {
            Log.i(TAG, "major ");
        }
        String[] versionParts = BuildConfig.SoftwareVersion.split("\\.");
        try {
            return Integer.parseInt(versionParts[0]);
        } catch (Exception ignored) {
        }
        return 0;
    }

    @Override
    public int minor() {
        if (traceDebug) {
            Log.i(TAG, "minor ");
        }
        String[] versionParts = BuildConfig.SoftwareVersion.split("\\.");
        try {
            return Integer.parseInt(versionParts[1]);
        } catch (Exception ignored) {
        }
        return 0;
    }

    @Override
    public String serialNumber() {
        if (traceDebug) {
            Log.i(TAG, "serialNumber ");
        }
        return DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
    }

    @Override
    public int getWorkoutTimeSeconds() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutTimeSeconds ");
        }
        return DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000;
    }

    @Override
    public WorkoutMeasurement getWorkoutDistance() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutDistance ");
        }
        return new WorkoutMeasurement((int) (DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT) * 100.0), CsafeUnits.KilometerHundredths);
    }

    @Override
    public WorkoutMeasurement getWorkoutElevation() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutElevation ");
        }
        switch (mConfig.machineClassType) {
            case Stepper:
            case Climbmill:
                return new WorkoutMeasurement((int) DataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT), CsafeUnits.Floors);

            default:
                return new WorkoutMeasurement((int) (DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT)), CsafeUnits.Meter);
        }
    }

    @Override
    public int getWorkoutCalories() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutCalories ");
        }
        return (int) DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT);
    }

    @Override
    public int getProgramId() {
        if (traceDebug) {
            Log.i(TAG, "getProgramId ");
        }
        return DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT);
    }

    @Override
    public int getProgramLevel() {
        if (traceDebug) {
            Log.i(TAG, "getProgramLevel ");
        }
        return DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_LEVEL_INT);
    }

    @Override
    public WorkoutMeasurement getWorkoutSpeed() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutSpeed ");
        }
        if (mConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            return new WorkoutMeasurement((int) (DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT) * 10.0 + 0.5), CsafeUnits.KilometerPerHourTenths);
        }
        return new WorkoutMeasurement((int) (DataMatrix.getFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT) * 10.0 + 0.5), CsafeUnits.KilometerPerHourTenths);
    }

    @Override
    public WorkoutMeasurement getWorkoutPace() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutPace ");
        }
        return new WorkoutMeasurement((int) (DataMatrix.getInt(WorkoutKeys.DATA_PACE_SEC_PER_KM_INT) + 0.5), CsafeUnits.SecondsPerKm);
    }

    @Override
    public WorkoutMeasurement getWorkoutCadence() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutCadence ");
        }
        switch (mConfig.machineClassType) {
            case Bike:
            case Elliptical:
            case RecumbentElliptical:
            case Ascent:
                return new WorkoutMeasurement(DataMatrix.getInt(HALKeys.DATA_RPM_INT), CsafeUnits.RevsPerMinute);
            case Stepper:
            case Climbmill:
                return new WorkoutMeasurement(DataMatrix.getInt(HALKeys.DATA_SPM_INT), CsafeUnits.RevsPerMinute);
        }
        return new WorkoutMeasurement(0, CsafeUnits.RevsPerMinute);
    }

    @Override
    public double getWorkoutIncline() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutIncline ");
        }
        return DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT);
    }

    @Override
    public int getWorkoutResistance() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutResistance ");
        }
        return DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT);
    }

    @Override
    public WorkoutMeasurement getWorkoutTorque() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutTorque ");
        }
        return new WorkoutMeasurement(0, CsafeUnits.NewtonPerMeters);
    }

    @Override
    public int getWorkoutHeartRate() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutHeartRate ");
        }
        return DataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT);
    }

    @Override
    public int getWorkoutMets() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutMets ");
        }
        return (int) DataMatrix.getFloat(HALKeys.DATA_METS_FLOAT);
    }

    @Override
    public double getWorkoutWatts() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutWatts ");
        }
        //Same check as the WorkoutKeys.DATA_RUN_STATE_INT, but this is polled frequently, so using a local variable
        if (currentWorkout.workoutState != WorkoutKeys.Constants.RUN_STATE_STOP) {
            return DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT);
        }
        return 0;
    }

    @Override
    public int getWorkoutHeartRateAverage() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutHeartRateAverage ");
        }
        return (int) DataMatrix.getFloat(WorkoutKeys.DATA_AVERAGE_HEART_RATE_FLOAT);
    }

    @Override
    public int getWorkoutHeartRateMax() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutHeartRateMax ");
        }
        return (int) DataMatrix.getFloat(WorkoutKeys.DATA_MAX_HEART_RATE_FLOAT);
    }

    @Override
    public int getWorkoutHRTZoneTimeSeconds() {
        if (traceDebug) {
            Log.i(TAG, "getWorkoutHRTZoneTimeSeconds ");
        }
        return 0;
    }

    @Override
    public void setIdDigits(int digits) {
        if (traceDebug) {
            Log.i(TAG, "setIdDigits ");
        }
        // TODO: This is the max digits for the fitlinxx login.
    }

    @Override
    public void onBadId() {
        if (traceDebug) {
            Log.i(TAG, "onBadId ");
        }
        loginHandler.onLoginResult(false);
    }

    @Override
    public void onGoFinished() { // go to Finished State
        //State change handled in the artifact side
        if (traceDebug) {
            Log.i(TAG, "onGoFinished ");
        }
        currentWorkout.stopWorkout();
        // workout state listener should handle Finished state
    }

    @Override
    public void onGoHaveId() { // go to HaveID state
        //State change handled in the artifact side
        if (traceDebug) {
            Log.i(TAG, "onGoHaveId ");
        }
        // doLogin should handle setting HaveId state ?
    }

    @Override
    public void onGoIdle() { // go to Idle State, reset variables to Idle state
        //State change handled in the artifact side
        if (traceDebug) {
            Log.i(TAG, "onGoIdle ");
        }
        loginHandler.onLoginResult(false);
    }

    @Override
    public void onGoInUse() { // go to InUse State
        //State change handled in the artifact side
        if (traceDebug) {
            Log.i(TAG, "onGoInUse ");
        }
        loginHandler.onLoginResult(true);

        if (currentWorkout.startWorkout()) {
            CsafeClient.Instance.setState(CsafeState.InUse);
            handler.post(() -> sendMessage("ers.2 is active, and a proctor will have control over your exercise", 30));
        } else {
            CsafeClient.Instance.isRejected = true;
        }

    }

    @Override
    public void onGoReady() { // go to Ready State
        //State change handled in the artifact side
        if (traceDebug) {
            Log.i(TAG, "onGoReady ");
        }
        // Ready is entered from Offline, it seems to be assumed as pre-configuration so reset might be implied?
        currentWorkout.reset(); // is this what we need?
        // client sets Ready state
    }

    @Override
    public void onReset() { // Reset Slave. Initialize variables to Ready State and reset Frame Toggle and Status of Previous Frame flag to zero.
        //State change handled in the artifact side
        if (traceDebug) {
            Log.i(TAG, "onReset ");
        }
        // RESET is not on flow chart, going off of command description
        currentWorkout.resetAndStop(); // assuming if we are commanded to reset, we should stop any running workout? (if we do stop a workout, does listener need to bypass Finished?)
        handler.post(() -> sendMessage("CSAFE: cmdReset Received", 10));
    }

    @Override
    public void setGoal(int goal) {
        if (traceDebug) {
            Log.i(TAG, "setGoal " + goal);
        }
        // Not applicable as goal is defined by the device.
    }

    @Override
    public void setHRMax(int heartRate) {
        if (traceDebug) {
            Log.i(TAG, "setHRMax " + heartRate);
        }
        // Not applicable as we do not have a workout where we set max heart rate.
    }

    @Override
    public void setHRZone(int heartRate) {
        if (traceDebug) {
            Log.i(TAG, "setHRZone " + heartRate);
        }
        // Not applicable as we do not have a workout where we set a heart rate zone.
    }

    @Override
    public void setIncline(double incline) {
        if (traceDebug) {
            Log.i(TAG, "setIncline " + incline);
        }
        incline = (double) Math.round(incline * 2) / 2;
        //Prevent spamming the LCB if the incline has not changed
        if (currentIncline != incline) {
            DataMatrix.putFloat(WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT, (float) incline);
        }
    }

    @Override
    public void setMets(double mets) {
        if (traceDebug) {
            Log.i(TAG, "setMets " + mets);
        }
        // Not applicable as we do not have a workout where we set a constant mets.
    }

    @Override
    public void setProgramId(int programId) {
        if (traceDebug) {
            Log.i(TAG, "setProgramId " + programId);
        }
        currentWorkout.setWorkoutId(programId);
    }

    @Override
    public void setProgramLevel(int level) {
        if (traceDebug) {
            Log.i(TAG, "setProgramLevel " + level);
        }
        currentWorkout.setWorkoutLevel(level);
    }

    @Override
    public void setResistance(int resistance) {
        if (traceDebug) {
            Log.i(TAG, "setResistance " + resistance);
        }
        //Prevent spamming the LCB if the resistance has not changed
        if (currentResistance != resistance) {
            DataMatrix.putInt(WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, resistance);
        }
    }

    @Override
    public void setSpeed(double speed, boolean imperial) {
        if (traceDebug) {
            Log.i(TAG, "setSpeed " + speed + " " + imperial);
        }
        if (imperial) {
            speed = SpeedConvertUtil.convertMphToKph((float) speed);
        }
        //Prevent spamming the LCB if the speed has not changed.
        // Check for greater or equal to 0.05 kmh change since Double equals can be off by insignificant figures
        if (Math.abs(currentSpeed - speed) >= 0.05) {
            DataMatrix.putFloat(WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT, (float) speed);
        }
    }

    @Override
    public void setTargetHeartRate(int heartRate) {
        if (traceDebug) {
            Log.i(TAG, "setTargetHeartRate " + heartRate);
        }
        currentWorkout.setTargetHR(heartRate);
    }

    @Override
    public void setSystemTime(int hour, int minute, int seconds) {
        if (traceDebug) {
            Log.i(TAG, "setSystemTime " + hour + " " + minute + " " + seconds);
        }
        // Not applicable as setting the system time can break functionality.  Better to get it from the web.
    }

    @Override
    public void setSystemDate(int year, int month, int day) {
        if (traceDebug) {
            Log.i(TAG, "setSystemDate " + year + " " + month + " " + day);
        }
        // Not applicable as setting the system time can break functionality.  Better to get it from the web.
    }

    @Override
    public void setTimeout(int timeout) {
        if (traceDebug) {
            Log.i(TAG, "setTimeout " + timeout);
        }
        // Not applicable as this setting does nothing on the interface.  This is akin to a screen
        // timeout, but we already have a user setting for that.
    }

    @Override
    public void setTorque(WorkoutMeasurement torque) {
        if (traceDebug) {
            Log.i(TAG, "setTorque " + torque);
        }
        /// Not applicable as we do not have a workout with a constant torque.
    }

    @Override
    public void setUserInfo(double weight, boolean imperial, int age, int gender) {
        if (traceDebug) {
            Log.i(TAG, "setUserInfo " + weight + " " + imperial + " " + age + " " + gender);
        }
        //Use default age if 0
        DataMatrix.putInt(DAPIKeys.DATA_USER_AGE_INT, age == 0 ? DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_AGE_INT) : age);

        //Use default weight if 0
        if (weight == 0) {
            DataMatrix.putFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WEIGHT_KG_FLOAT));
        } else {
            DataMatrix.putFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, imperial ? WeightConvertUtil.convertLbsToKg((float) weight) : (float) weight);
        }

        //Add case for gender selection
        switch (gender) {
            case 0:
                gender = GenderIndentity.NotSpecified.Value;
                break;
            case 1:
                gender = GenderIndentity.Male.Value;
                break;
            case 2:
                gender = GenderIndentity.Female.Value;
                break;
        }
        DataMatrix.putInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT, gender);
    }

    @Override
    public void setWatts(double watts) {
        if (traceDebug) {
            Log.i(TAG, "setWatts " + watts);
        }
        //Prevent spamming the LCB if the target watts have not changed
        if (currentWatts != (int) watts) {
            DataMatrix.putInt(WorkoutKeys.DATA_TARGET_WATTS_INT, (int) watts);
            currentWorkout.lastTargetWattsValue = (int) watts;
        }
    }

    @Override
    public void setWorkoutCalories(int calories) {
        if (traceDebug) {
            Log.i(TAG, "setWorkoutCalories " + calories);
        }
        currentWorkout.setGoalCalories(calories);
    }

    @Override
    public void setWorkoutDistance(double distance, boolean imperial) {
        if (traceDebug) {
            Log.i(TAG, "setWorkoutDistance " + distance + " " + imperial);
        }
        currentWorkout.setGoalDistance(imperial ? DistanceConvertUtil.convertMilesToKilometers((float) distance) : distance);
    }

    @Override
    public void setWorkoutElevation(double elevation, boolean imperial) {
        if (traceDebug) {
            Log.i(TAG, "elevation " + elevation + " " + imperial);
        }
        if (imperial) elevation = DistanceConvertUtil.convertFeetToMeters((float) elevation);
        currentWorkout.setGoalElevation(elevation);
    }

    @Override
    public void setWorkoutFloors(double floors) {
        Log.i(TAG, "setWorkoutFloors " + floors);
        currentWorkout.setGoalFloors(floors);
    }

    @Override
    public void setWorkoutSteps(double steps) {
        if (traceDebug) {
            Log.i(TAG, "setWorkoutSteps " + steps);
        }
        currentWorkout.setGoalSteps(steps);
    }

    @Override
    public void setWorkoutTime(int workoutTimeSeconds) {
        if (traceDebug) {
            Log.i(TAG, "setWorkoutTime " + workoutTimeSeconds);
        }
        currentWorkout.setGoalTime(workoutTimeSeconds);
    }

    @Override
    public int getFrametype() {
        int frame;
        switch (mConfig.machineType) {
            case Treadmill:
                frame = 1;
                break;
            case HybridBike:
            case CardioSpinBike:
            case RecumbentBike:
            case UprightBike:
                frame = 2;
                break;
            case Elliptical:
            case RecumbentElliptical:
            case LowerBodyAscent:
            case Ascent:
                frame = 3;
                break;
            case Stepper:
            case Climbmill:
                frame = 4;
                break;
            default:
                frame = 0;
                break;
        }
        return frame;
    }

    @Override
    public void commandReceived() {
        lastTime = currentTime;
    }

    private void sendMessage(String messageText, int hideTime) {
        MessageCenterMessage message = new MessageCenterMessage("", messageText, 10, hideTime).setId(JHTTypes.MessageIds.ERGOLINE_NOTIFICATION.getId());
        DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, message);
    }

    /**
     * Internal class to track CSafe workout
     */
    private static class CSafeWorkout {
        private int lastSetWorkoutId;
        private int lastSetWorkoutLevel;
        // goals appear to be set only in "Ready, Idle or HaveID states" so not while in use, just store them for next start of workout
        private String lastGoalVariable;
        private float lastGoalValue;
        private int lastTargetWattsValue;
        public int workoutState;

        CSafeWorkout() {
            reset();
        }

        public void reset() {
            lastSetWorkoutId = 0;
            lastSetWorkoutLevel = 0;
            lastGoalVariable = ConsoleWorkoutVariables.TIME_GOAL_START;
            lastGoalValue = DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT);
            lastTargetWattsValue = (int) MinMaxDictionary.getMinMaxDictionary(MinMaxParam.ConstantWatts).min;
        }

        public void resetAndStop() {
            stopWorkout();
            reset();
        }

        public void setWorkoutId(int id) {
            lastSetWorkoutId = id;
        }

        public void setWorkoutLevel(int level) {
            lastSetWorkoutLevel = level;
        }

        public void setGoalTime(int workoutTimeSeconds) {
            lastGoalVariable = ConsoleWorkoutVariables.TIME_GOAL_START;
            lastGoalValue = workoutTimeSeconds / 60f;
        }

        public void setGoalSteps(double steps) {
            lastGoalVariable = ConsoleWorkoutVariables.FLOORS_GOAL_START;
            lastGoalValue = (float) steps / FrameConfigManager.getInstance().getFrameConfig().STEPS_PER_FLOOR;
        }

        public void setGoalFloors(double floors) {
            lastGoalVariable = ConsoleWorkoutVariables.FLOORS_GOAL_START;
            lastGoalValue = (float) floors;
        }

        public void setGoalDistance(double workoutDistanceKm) {
            lastGoalVariable = ConsoleWorkoutVariables.DISTANCE_GOAL_START;
            lastGoalValue = (float) workoutDistanceKm; // km
        }

        public void setGoalCalories(int calories) {
            lastGoalVariable = ConsoleWorkoutVariables.CALORIE_GOAL_START;
            lastGoalValue = calories; // kcal
        }

        public void setGoalElevation(double elevationMeters) {
            // ELEVATION_GOAL_START doesn't seem used? could convert to floor goal?
            setGoalSteps(elevationMeters * FrameConfigManager.getInstance().getFrameConfig().STEPS_PER_METER);
        }

        public void setTargetHR(int heartRate) {
            // TODO: is this the legal way to do it? it does not look like the other goals? is this meant to be in addition to a goal?
            //lastGoalVariable = ConsoleWorkoutVariables.TARGET_HEART_RATE;
            //lastGoalValue = heartRate;
        }

        private boolean startWorkout() {
            // loosely based on our FTMS StartWorkoutCommand.java
            try {
                WorkoutMap workoutMap = WorkoutMap.Companion.createWorkoutMap();
                WorkoutManager workoutManager = new WorkoutManager(workoutMap);
                WorkoutItem workoutEntry = workoutMap.getWorkout(lastSetWorkoutId);
                if (workoutEntry == null) {
                    Log.w(TAG, "Failed to load workout id " + lastSetWorkoutId + ", falling back to MANUAL");
                    workoutEntry = workoutMap.getWorkout(WorkoutIds.MANUAL);
                }
                if (workoutEntry == null) {
                    Log.e(TAG, "Failed to load workout entry aborting");
                } else {
                    if (workoutEntry.getWorkoutId() == WorkoutIds.MANUAL && FrameConfigManager.getInstance().getFrameConfig().machineClassType != JHTCommonTypes.MachineClassificationType.Treadmill) {
                        workoutManager.loadCustomWorkout(WorkoutIds.CUSTOM_WORKOUT, AppIds.DEFAULT_APP_UUID, ConstantWattsErgoline.getWorkoutScript(), null);
                    } else {
                        workoutManager.loadWorkoutWithId(workoutEntry.getWorkoutId(), null);
                    }

                    List<WorkoutInput> initialConditions = new ArrayList<>();
                    if (lastGoalVariable != null && !lastGoalVariable.isEmpty())
                        initialConditions.add(new WorkoutInput(lastGoalVariable, lastGoalValue));
                    initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_SPEED, DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT)));
                    initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_INCLINE, FrameConfigManager.getInstance().getFrameConfig().incline.getMin()));
                    initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.START_RESISTANCE, DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT)));
                    initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.LEVEL, lastSetWorkoutLevel));
                    initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.TARGET_WATTS_START, lastTargetWattsValue));

                    //Special logic
                    initialConditions.add(new WorkoutInput(ConsoleWorkoutVariables.ALLOW_AUTO_PAUSE, 0));

                    WorkoutSettingOverrides settingOverrides = new WorkoutSettingOverrides();
                    settingOverrides.settings.add(new SettingValue(WorkoutControllerService.CAN_MODIFY_GOAL_SETTING, 0));

                    DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_USER_PAUSE_BOOL, false);
                    DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_USER_CHANGES_BOOL, false);
                    DataMatrix.putBoolean(WorkoutKeys.DATA_ALLOW_COUNTDOWN_BOOL, false);

                    return workoutManager.startLoadedWorkout(initialConditions, false, settingOverrides);
                }
            } catch (Exception e) {
                Log.e(TAG, "startWorkout failed unexpectedly", e);
            }
            return false;
        }

        private void stopWorkout() {
            DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT);
        }
    }
}
