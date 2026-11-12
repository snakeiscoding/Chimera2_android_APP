package com.jht.chimera.service.hal.lib.hardware.heartrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.service.hal.lib.io.iocommlib.PowerType;
import com.jht.chimera.service.hal.lib.salutron.IHeartRateCallback;
import com.jht.chimera.service.hal.lib.salutron.SalutronDevice;
import com.jht.chimera.service.hal.lib.salutron.SalutronManager;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.HeartRateSource;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;

import java.io.File;

import kotlin.Unit;

class SalutronHeartRateReader implements IHeartRateReader {
    private volatile int heartRate;
    private volatile boolean blankState;
    private volatile HeartRateSource heartRateSource = HeartRateSource.Unknown;
    private final Context context;
    private final DataMatrix dataMatrix = new DataMatrix();
    private final IHeartRateUpdated heartRateUpdated;
    private boolean mHeartRateSalutronWorkout = false;
    private boolean mHeartRateSalutronScanning = false;
    private boolean mSalutronInit;
    private SalutronDevice.SalutronDeviceParcelable currentSalutronDevice;
    private final HeartRateBlinker blinker;
    private final SerialPortConfiguration.BaudRate BAUD_RATE = SerialPortConfiguration.BaudRate.B9600;
    public static final Object notifySalutronDisconnectedSync = new Object();

    private final String UPDATE_8700_BROADCAST = "com.jht.hal.update.salutron"; //NON-NLS

    private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String path = intent.getStringExtra("path");
            if (path != null) {
                File file = new File(path);
                if (file.exists()) {
                    SalutronManager.Instance.setFirmware(file);
                    SalutronManager.Instance.controlDevice(SalutronManager.Control_Command.START_UPDATE);
                }
            }
        }
    };

    SalutronHeartRateReader(Context context, IHeartRateUpdated heartRateUpdated) {
        this.heartRateUpdated = heartRateUpdated;
        this.context = context;
        this.blinker = new HeartRateBlinker(
                () -> heartRate,
                (state)-> {
                    blankState = state;
                    heartRateUpdated.heartRateUpdated();
                    return Unit.INSTANCE;
                },
                () -> {
                    blankState = false;
                    heartRateUpdated.heartRateUpdated();
                    return Unit.INSTANCE;
        });

        startHardware(context);
        broadcastAntInWorkout();
        monitorForWorkoutStart();
        monitorForWorkoutComplete();
        handleScan();
        handleDeviceConnections();
        handleDeviceDisconnections();
    }

    private void handleDeviceDisconnections() {
        dataMatrix.createParcelableAdapter(HALKeys.EVENT_SALUTRON_DEVICE_DISCONNECT_PARCELABLE, SalutronDevice.SalutronDeviceParcelable.class, deviceParcelable -> {
            if (deviceParcelable.name != null && !deviceParcelable.name.isEmpty()){
                synchronized (notifySalutronDisconnectedSync) {
                    SalutronManager.Instance.disconnectDevice(true, 2000);
                }
            }
        });
    }

    private void handleDeviceConnections() {
        dataMatrix.createParcelableAdapter(HALKeys.EVENT_SALUTRON_DEVICE_CONNECT_PARCELABLE, SalutronDevice.SalutronDeviceParcelable.class, deviceParcelable -> {
            if (deviceParcelable.name != null && !deviceParcelable.name.isEmpty()) {
                if (currentSalutronDevice != null){
                    synchronized (notifySalutronDisconnectedSync) {
                        SalutronManager.Instance.disconnectDevice(false, 5000);
                    }
                }
                SalutronManager.Instance.connectDevice(deviceParcelable);
            }
        });
    }

    private void handleScan() {
        dataMatrix.createEventAdapter(HALKeys.ACTION_START_SALUTRON_SCAN,key -> {
            if(!mHeartRateSalutronWorkout) {
                startSalutron();
            }
            if(!mHeartRateSalutronScanning) {
                SalutronManager.Instance.controlDevice(SalutronManager.Control_Command.SCANNING);
            }
            if(DataMatrix.getBoolean(HALKeys.DATA_SALUTRON_CONNECTED_BOOL)) {
                if (currentSalutronDevice != null) {
                    SalutronDevice.sendDevice(currentSalutronDevice.name, currentSalutronDevice.id, currentSalutronDevice.type);
                }
            }
            else {
                SalutronManager.Instance.sendDevices();
            }
        });
    }

    private void monitorForWorkoutComplete() {
        dataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET, workoutState -> {
            resetSalutron();
        }, false);
    }

    private void monitorForWorkoutStart() {
        dataMatrix.createEventAdapter(WorkoutKeys.EVENT_WORKOUT_START, key -> {
            startSalutron();
            if (heartRate > 0) {
                blinker.startBlink(false);
            }
        });
    }

    private void broadcastAntInWorkout() {
        dataMatrix.createIntAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, integer -> {
            boolean isANTEnable = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_ANT_PLUS_DEVICE_SEND_DATA_BOOL);
            if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP) {
                if (isANTEnable)
                    SalutronManager.Instance.broadcastANT(DataMatrix.getInt(SettingKeys.Hardware.DATA_ANT_PLUS_PAGE_INT) == 0);
            }
        }, false);
    }

    private void startHardware(Context context) {
        DataMatrix.putInt(HALKeys.ACTION_IO_POWER_ON_DEVICE_ENUM_INT, PowerType.heartRatePower.getID());
        dataMatrix.createIntAdapter(HALKeys.DATA_IO_POWER_ON_DEVICE_ENUM_INT, integer -> {
            if (PowerType.format(integer.byteValue()) == PowerType.heartRatePower) {
                new JHTTimer(500, () -> initHardware()).start();
            }
        }, true);

        // TODO VORTEX Figure if Vortex will power the Salutron board. It doesn't seem like it does for now so just start it
        if(ProductConfig.hardware.isVortexBase()) {
            Log.d("SalutronManager", "-->Vortex start Salutron on " + ProductConfig.portMappings.salutronPath()); // TODO remove this log
            initHardware();
        }
    }

    private void initHardware() {
        if (!mSalutronInit) {
            mSalutronInit = true;
            SalutronManager.Instance.start(new SerialPort(ProductConfig.portMappings.salutronPath(), serialPortSetting()));
            SalutronManager.Instance.registerOnReceiveListener(callback);
            SalutronManager.Instance.controlDevice(SalutronManager.Control_Command.GET_VER);
            SalutronManager.Instance.controlDevice(SalutronManager.Control_Command.GET_MAC);
            startSalutron();
            context.registerReceiver(mUpdateReceiver, new IntentFilter(UPDATE_8700_BROADCAST));
        }
    }


    @Override
    public void close() {
        dataMatrix.finish();
        try {
            context.unregisterReceiver(mUpdateReceiver);
        } catch(Exception ignored) {}
        SalutronManager.Instance.stop();

    }

    @Override
    public synchronized int heartRate() {
        return heartRate;
    }

    @Override
    public synchronized boolean blankState() {
        return blankState;
    }

    @Override
    public HeartRateSource getSource() {
        return heartRateSource;
    }

    @Override
    public boolean hasConnection() {
        // a grip-detect blink arrives before any device connects or a heart rate locks;
        // report a connection while blinking so HeartRateManager selects this reader and
        // publishes the blank toggles (same idea as VortexSalutron8600HeartRate's grip state)
        return DataMatrix.getBoolean(HALKeys.DATA_SALUTRON_CONNECTED_BOOL) || blinker.isBlinking();
    }


    public void startSalutron(){
        if(!mHeartRateSalutronWorkout) {
            SalutronManager.Instance.controlDevice(SalutronManager.Control_Command.START_WORKOUT);
        }
    }


    public void resetSalutron(){
        if(mHeartRateSalutronWorkout){
            DataMatrix.putInt(HALKeys.DATA_HEART_RATE_INT, 0);
            SalutronManager.Instance.controlDevice(SalutronManager.Control_Command.STOP_WORKOUT);
            SalutronManager.Instance.controlDevice(SalutronManager.Control_Command.START_WORKOUT);
        }
    }

    private final IHeartRateCallback callback = new IHeartRateCallback() {
        @Override
        public void onHeartRate(int value, int type) {
            processSalutronHeartRate(value);
        }

        @Override
        public void onScanning(boolean isScanning) {
            mHeartRateSalutronScanning = isScanning;
        }

        @Override
        public void onWorkout(boolean isWorkout) {
            mHeartRateSalutronWorkout = isWorkout;
        }

        @Override
        public void isConnected(boolean isConnected, SalutronDevice.SalutronDeviceParcelable connectDevice) {
            if(isConnected) {
                heartRateSource = HeartRateSource.Unknown;
                if(connectDevice != null) {
                    switch (connectDevice.type) {
                        case BLE: heartRateSource = HeartRateSource.BLE; break;
                        case ANT_HR: heartRateSource = HeartRateSource.ANT_HR; break;
                        case ANT_EXD: heartRateSource = HeartRateSource.ANT_EXD; break;
                        case FIVE_KHZ: heartRateSource = HeartRateSource.FIVE_KHZ; break;
                    }
                    currentSalutronDevice = connectDevice;
                    DataMatrix.putBytes(HALKeys.DATA_SALUTRON_CONNECTED_DEVICE_BYTES, connectDevice.id);
                }
            }
            else {
                DataMatrix.putBytes(HALKeys.DATA_SALUTRON_CONNECTED_DEVICE_BYTES,null);
                currentSalutronDevice = null;
            }
            DataMatrix.putBoolean(HALKeys.DATA_SALUTRON_CONNECTED_BOOL, isConnected);
        }

        @Override
        public void version(String version) {
            String[] versionArray = version.split(" ");
            DataMatrix.putString(HALKeys.DATA_VERSION_SALUTRON_STRING,versionArray[1]);
            AndroidSetting.putString(context.getContentResolver(), "chimera_salutron_version", versionArray[1]); //NON-NLS
        }

        @Override
        public void blink(boolean isKeep) {
            blinker.startBlink(isKeep);
        }

        @Override
        public void stopBlink() {
            blinker.stopBlink(false);
        }
    };

    private SerialPortConfiguration serialPortSetting() {

        SerialPortConfiguration serialPortConfiguration = new SerialPortConfiguration();
        serialPortConfiguration.baudRate = BAUD_RATE;
        serialPortConfiguration.CREAD = true;
        serialPortConfiguration.CLOCAL = true;
        serialPortConfiguration.csize = SerialPortConfiguration.CSIZE.CS8;
        serialPortConfiguration.makeRaw = true;
        serialPortConfiguration.NOCTTY = true;
        serialPortConfiguration.VTIME = 1;
        serialPortConfiguration.VMIN = 255;
        serialPortConfiguration.TRACE_DEBUG = false;

        return serialPortConfiguration;
    }

    @VisibleForTesting
    public void processSalutronHeartRate(int value) {
        if (value == 0) {
            blinker.setBlinkFinished(false);
            heartRate = 0;
            heartRateUpdated.heartRateUpdated();
        }

        if (value > 0) {
            heartRate = value;
            if (blinker.isBlinkFinished()) {
                heartRateUpdated.heartRateUpdated();
            } else {
                blinker.startBlink(false);
            }
        }
    }

    public IHeartRateCallback getCallback() {
        return this.callback;
    }
}
