package com.jht.chimera.service.hal.lib.hardware;

import static com.jht.chimera.service.hal.lib.io.iocommlib.McuProtocol.LED_ERP_OFF;
import static com.jht.chimera.service.hal.lib.io.iocommlib.McuProtocol.LED_ERP_ON;
import static com.jht.chimera.service.hal.lib.io.iocommlib.McuProtocol.LED_RFID_OFF;
import static com.jht.chimera.service.hal.lib.io.iocommlib.McuProtocol.LED_RFID_ON;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.SystemBrightness;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.enums.HardwareControlDevices;
import com.jht.chimera.service.hal.lib.io.CmdHandler;
import com.jht.chimera.service.hal.lib.io.DataHandler;
import com.jht.chimera.service.hal.lib.io.iocommlib.IMCUCallback;
import com.jht.chimera.service.hal.lib.io.iocommlib.IRMCUPacketManager;
import com.jht.chimera.service.hal.lib.io.iocommlib.McuPacketManager;
import com.jht.chimera.service.hal.lib.io.iocommlib.McuPassthroughDevice;
import com.jht.chimera.service.hal.lib.io.iocommlib.McuPassthroughKt;
import com.jht.chimera.service.hal.lib.io.iocommlib.PowerType;
import com.jht.mastboard.data.LEDItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.ISerialPort;
import com.jht.serialport.SerialPort;
import com.jht.serialport.SerialPortConfiguration;

import java.io.File;
import java.util.Objects;

public class IOCommManager {

    public final String TAG = "IOCommManager";
    private final static String UPDATE_IO_BROADCAST = "com.jht.hal.update.io"; //NON-NLS

    private final DataMatrix mDataMatrix = new DataMatrix();
    private DataHandler mDataHandler;
    private CmdHandler mCmdHandler;
    private final CanManager mCanManager = new CanManager();

    private boolean isBeep = true;
    private final static String DEVICE_PATH = ProductConfig.portMappings.ioPath();
    private final SerialPortConfiguration.BaudRate BAUD_RATE = SerialPortConfiguration.BaudRate.B115200;
    private MCU_MODE mcuMode = MCU_MODE.APPLICATION;

    private enum MCU_MODE{
        BOOTLOADER,
        APPLICATION
    }

    public static IOCommManager getInstance() {
        return IOCommManager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final IOCommManager INSTANCE = new IOCommManager();
    }

    private IOCommManager() {
    }

    private static final String UPDATE_STARTED_INTENT = "com.jht.update_started_intent"; //NON-NLS

    public interface MCUKeyPressListener {
        void onKeyPress(Driver.struct_input_event event);
    }

    private MCUKeyPressListener mKeyPressListener;

    public void start() {
        Context context = ContextUtil.getApplicationContext();

        McuPacketManager.getInstance().start(createIoSerialPort(DEVICE_PATH), context);
        if(ProductConfig.hasSeparateIoForIr()) {
            IRMCUPacketManager.getInstance().start(createIoSerialPort(ProductConfig.portMappings.irSerialPath()), context);
        }
        McuPacketManager.getInstance().setCallback(callback);
        if (mDataHandler == null) {
            mDataHandler = new DataHandler(Objects.requireNonNull(context));
        }
        if (mCmdHandler == null) {
            mCmdHandler = new CmdHandler();
        }
        ContextCompat.registerReceiver(Objects.requireNonNull(context), mUpdateReceiver, new IntentFilter(UPDATE_IO_BROADCAST), ContextCompat.RECEIVER_EXPORTED);
        IntentFilter filterReboot = new IntentFilter();
        filterReboot.addAction(Intent.ACTION_SHUTDOWN);
        filterReboot.addAction("android.intent.action.QUICKBOOT_POWEROFF");
        ContextCompat.registerReceiver(context, mShutdownReceiver, filterReboot, ContextCompat.RECEIVER_EXPORTED);

        mCmdHandler.getOperationMode();
    }

    public void init(){
        mCmdHandler.getVersion();
        mCmdHandler.getBoot();
        mCmdHandler.getExternalPowerState();
        mCmdHandler.setDebug(true, 3);
        mCmdHandler.setERPLED(LED_ERP_OFF);

        // TODO VORTEX do we need this? setQiVoltage function was commented out
        // add command to set Qi voltage for Vortex to 9V
        if (ProductConfig.hardware.isVortexBase()) {
            mCmdHandler.setQiVoltage(1);
        }

        //Disable this function on chimera 1.0
//        mCmdHandler.getPowerMonitorState();

//        lcbType = DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT);

//        if(lcbType != 0) {
//            if ((lcbType & 0xC0) != 0)
//                mCmdHandler.getSafeKeyState();
//        }
//        else
//            mCmdHandler.getLcbType();

        if(FrameConfigManager.getInstance().getFrameConfig().machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill)
            mCmdHandler.getSafeKeyState();

        boolean isPersistent = Settings.Secure.getInt(Objects.requireNonNull(ContextUtil.getApplicationContext()).getContentResolver(), "jht_disable_persistent", 0) == 0; //NON-NLS
        if (isPersistent && !DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL)) {
            mCmdHandler.setWatchdog(1000 * 60 * 3);
        }


        for (HardwareControlDevices device : HardwareControlDevices.values()) {
            if(!device.equals(HardwareControlDevices.TV) && !device.equals(HardwareControlDevices.IPOD)) {
                mCmdHandler.setPower(getHardwarePowerType(device), 1);
                mCmdHandler.getDevicePowerState(getHardwarePowerType(device));
            }
        }

//        Since the IR power will turn on by default for project one, chimera needs to turn off it when boot up complete
        if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera) || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_3)) {
            if (ProductConfig.isCommercial() && !ProductConfig.isProjectOne())
                mCmdHandler.setPower(getHardwarePowerType(HardwareControlDevices.IR_5V), 0);
        }

        new JHTTimer(800, () -> {
            //USB port will no power if there is a device plugged into usb port before console boot
            //IPOD power should be opened after extension power checking -> IO version should be 72
            mCmdHandler.setPower(getHardwarePowerType(HardwareControlDevices.IPOD), 1);
            mCmdHandler.getDevicePowerState(getHardwarePowerType(HardwareControlDevices.IPOD));
        }).start();

        // TODO: Need to further define this based on https://jhtnaee.atlassian.net/browse/CHIM-5029. (now CGC-5029)
        mDataMatrix.createStringAdapter(SettingKeys.TV.DATA_TV_SOURCE_STRING, source -> {
            mCmdHandler.setPower(getHardwarePowerType(HardwareControlDevices.TV), !source.equals(SettingKeys.TVSource.US4.name()) ? 1 : 0);
            mCmdHandler.getDevicePowerState(getHardwarePowerType(HardwareControlDevices.TV));
        }, true);

        // Set headphone LED to ON which is only for ProjectOne frames (headphone LED is the ERP LED)
        if (ProductConfig.isProjectOne()) mCmdHandler.setERPLED(LED_ERP_ON);
        if (ProductConfig.hasRfidLed()) mCmdHandler.setERPLED(LED_RFID_ON);

        mDataMatrix.createBoolAdapter(HALKeys.ACTION_SET_TV_IO_POWER_STATE_BOOL, state -> {
            mCmdHandler.setPower(getHardwarePowerType(HardwareControlDevices.TV), state ? 1 : 0);
            mCmdHandler.getDevicePowerState(getHardwarePowerType(HardwareControlDevices.TV));
        });

        // we could use either DM or intent/receiver for the passthrough, DM seems easiest right now
        mDataMatrix.createParcelableAdapter(HALKeys.ACTION_SEND_DATA_TO_IO_PASSTHROUGH_PARCELABLE, Bundle.class, bundle -> {
            if (bundle == null) return;
            //bundle.setClassLoader(context.getClassLoader()); // if using basic types, class loader should not be needed
            McuPassthroughDevice device = McuPassthroughDevice.fromString(bundle.getString(McuPassthroughKt.PASSTHROUGH_DEVICE_KEY));
            byte[] payload = bundle.getByteArray(McuPassthroughKt.PASSTHROUGH_PAYLOAD_KEY);
            if (device != null && payload != null) {
                mCmdHandler.sendPassthrough(device, payload);
            } else {
                Log.w(TAG, "Unknown IO passthrough destination " + device);
            }
        });

        mDataMatrix.createEventAdapter(HALKeys.ACTION_IO_PLAY_BEEP, event -> mCmdHandler.beep());
        mDataMatrix.createEventAdapter(HALKeys.EVENT_LCB_POWERED_OFF, event -> {
            if (ProductConfig.hasRfidLed()) mCmdHandler.setERPLED(LED_RFID_OFF);
            if (FrameConfigManager.getInstance().getFrameConfig().HAS_CAN) mCanManager.sendGripsToSleep();
            ioEnterERP();
        });

        mDataMatrix.createByteArrayAdapter(HALKeys.ACTION_IO_SEND_IR_COMMAND_BYTES, bytes -> mCmdHandler.sendIRcommand(bytes), false);
        mDataMatrix.createIntAdapter(HALKeys.DATA_IO_SET_FAN_SPEED_INT, integer -> {
            if (ProductConfig.isProjectOne()) {
                    mCmdHandler.setFanSpeed(4);
            }else{
	            if(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL))
	                mCmdHandler.setFanSpeed(integer);
	            if(integer == 0)
	                DataMatrix.putInt(HALKeys.DATA_IO_FAN_RPM_INT,0);
            }
        });
        mDataMatrix.createEventAdapter(HALKeys.ACTION_IO_LEAVE_ERP, event -> mCmdHandler.enterSuspend());
        mDataMatrix.createEventAdapter(HALKeys.ACTION_IO_ENTER_ERP, event -> ioEnterERP());
        mDataMatrix.createStringAdapter(HALKeys.ACTION_IO_UPDATE_PATH_STRING, path -> mCmdHandler.updateIO(Uri.fromFile(new File(path)), McuPacketManager.UPDATE_TARGET), false);

        IntentFilter stopWatchdogFilter = new IntentFilter("com.jht.update_started_intent");
        Intent stopIntent = ContextCompat.registerReceiver(ContextUtil.getApplicationContext(), stopWatchdogReceiver, stopWatchdogFilter, ContextCompat.RECEIVER_EXPORTED);
        if (stopIntent != null) {
            if(mcuMode == MCU_MODE.APPLICATION)
                mCmdHandler.setWatchdog(0);
        }

        mDataMatrix.createIntAdapter(HALKeys.ACTION_IO_POWER_ON_DEVICE_ENUM_INT, integer -> {
//            if (ioVersion < 7680 || integer.byteValue() != (byte) 0x0F) {
            mCmdHandler.setPower(integer.byteValue(), 1);
            mCmdHandler.getDevicePowerState(integer.byteValue());
//            }
        }, false);

        if(ProductConfig.platform.equals(ProductConfig.Platform.LCGUI)) {
            Log.d(TAG, "Send this for lcgui to start salutron 8700");
            DataMatrix.putInt(HALKeys.DATA_IO_POWER_ON_DEVICE_ENUM_INT, PowerType.heartRatePower.getID());
        }

        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, value -> {
            if (value == WorkoutKeys.Constants.RUN_STATE_STOP) {
                DataMatrix.putInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT, 0);
            }
        }, false);

        mDataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_KEY_BEEP_BOOL, isBeep -> {
            int beepToggle = 0;
            Log.d(TAG, "start: Is key beep --> " + isBeep);
            if (this.isBeep != isBeep) {
                this.isBeep = isBeep;
                McuPacketManager.getInstance().setTone(beepToggle);
            }
        });

        DataMatrix.putInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT, 0);

        mDataMatrix.createEventAdapter(HALKeys.ACTION_SETUP_FRAME, keyReturnedBack -> {
            // restart to check for safety key
            stop();
            start();
        });

        if(FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.SpinBike)) {
            mCmdHandler.setPower(PowerType.ext_14V.getID(), 0);
        } else if (ProductConfig.requiresExtPower()) {
            int extPowerMode = ProductConfig.needsExtPowerSwitch()
                    ? McuPacketManager.powerOnERPOff
                    : McuPacketManager.enablePower;
            mCmdHandler.setPower(PowerType.ext_14V.getID(), extPowerMode);
        }
        else {
            mDataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL, value -> mCmdHandler.setPower(PowerType.ext_14V.getID(), value ? 1 : 0));
        }
        mDataMatrix.createEventAdapter(HALKeys.ACTION_IO_POWER_MONITOR, event -> {
            mCmdHandler.getPowerState((byte)0x00);
            mCmdHandler.getPowerState((byte)0x01);
            mCmdHandler.getPowerState((byte)0x02);
        });

        mDataMatrix.createIntAdapter(HALKeys.DATA_IO_SET_IR_FREQUENCY_KHZ_INT, frequency -> mCmdHandler.sendIRFrequency(frequency.shortValue()));

        mDataMatrix.createParcelableAdapter(SettingKeys.ProjectOne.ACTION_LED_COLOR_HEX_PARCELABLE, LEDItem.class, this::setLedColor);

        if(FrameConfigManager.getInstance().getFrameConfig().HAS_CAN) {
            mCanManager.start(mCmdHandler);
        }
    }

    private void setLedColor(LEDItem ledItem) {
        Log.d(TAG, "New Active LED " + ledItem.color + " " + ledItem.ledZone.name());
        switch (ledItem.ledZone){
            case ALL:
            case ALL_EXCEPT_PLAY_STOP:
            case BACK:
                mCmdHandler.setLEDColor(0, ledItem.color);
                break;
        }
    }

    public void stop() {
        mDataMatrix.finish();
        McuPacketManager.getInstance().stop();
        mCanManager.stop();
        try {
            // FIXME - Re-use the acquired context.
            Objects.requireNonNull(ContextUtil.getApplicationContext()).unregisterReceiver(mUpdateReceiver);
            ContextUtil.getApplicationContext().unregisterReceiver(mShutdownReceiver);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void registerKeyPressListener(MCUKeyPressListener listener) {
        mKeyPressListener = listener;
    }


    private final IMCUCallback callback = new IMCUCallback() {

        @Override
        public void operationMode(int mode) {
            if(mode == 0) {
                mcuMode = MCU_MODE.APPLICATION;
                init();
            }
            else {
                mcuMode = MCU_MODE.BOOTLOADER;
                Log.e(TAG, "IO error: MCU is in bootloader mode");
            }
        }

        @Override
        public void version(int version) {
            Log.d(TAG, "version: " + version);
            mDataHandler.version(version);
        }

        @Override
        public void safeKeyStatus(boolean status) {
            mDataHandler.safeKeyStatus(status);
        }

        @Override
        public void powerState(byte source, boolean _switch) {
            mDataHandler.powerState(source, _switch);
        }

        @Override
        public void externalPower(McuPacketManager.PowerVoltage powerVoltage, int current) {
            mDataHandler.externalPower(powerVoltage,current);
        }

        @Override
        public void boot(boolean state) {
            mDataHandler.boot(state);
        }

        @Override
        public void fanSpeed(byte level, int rpm) {
            // project one has logic to control the cpu fan so skipping the following logic for the workout fan
            if (ProductConfig.isProjectOne()) return;

            int fanLevel = DataMatrix.getInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT);
            DataMatrix.putInt(HALKeys.DATA_IO_FAN_RPM_INT,rpm);
            switch (fanLevel){
                case 1:
                    if(rpm > 2740)
                        mCmdHandler.setFanSpeed(level - 1);
                    else if(rpm < 2540)
                        mCmdHandler.setFanSpeed(level + 1);
                    break;
                case 2:
                    if(rpm > 3864)
                        mCmdHandler.setFanSpeed(level - 1);
                    else if(rpm < 3664)
                        mCmdHandler.setFanSpeed(level + 1);
                    break;
                case 3:
                    if(rpm > 5050)
                        mCmdHandler.setFanSpeed(level - 1);
                    else if(rpm < 4850)
                        mCmdHandler.setFanSpeed(level + 1);
                    break;
            }
        }

        @Override
        public void lcbType(byte type) {
            int lcbType = type & 0xff;
            if ((lcbType & 0xC0) != 0) {
                mCmdHandler.getSafeKeyState();
            }
        }

        @Override
        public void consolePowerVoltage(short voltage) {
           mDataHandler.consolePowerVoltage(voltage);
        }

        @Override
        public void extendPowerVoltage(short voltage, boolean check) {
            mDataHandler.extendPowerVoltage(voltage, check);
        }

        @Override
        public void usbVoltage(short voltage) {
            mDataHandler.usbVoltage(voltage);
        }

        @Override
        public void powerMonitorState(boolean enable) {
            if(!enable)
                mCmdHandler.powerMonitor(true,5);
        }

        @Override
        public void consolePowerCurrent(short current) {
            mDataHandler.consolePowerCurrent(current);
        }

        @Override
        public void extendPowerCurrent(short current) {
           mDataHandler.extendPowerCurrent(current);
        }

        @Override
        public void extendPowerError(boolean error) {
            DataMatrix.putBoolean(HALKeys.DATA_EXTEND_ERROR_BOOL,error);
        }

        @Override
        public void keyEvent(int event, short keyCode) {
            Log.d(TAG, "keyEvent: " + event + " " + keyCode);
            if (mKeyPressListener != null) {
                Driver.struct_input_event inputEvent = new Driver.struct_input_event();
                inputEvent.type = Driver.struct_input_event.EV_KEY;
                inputEvent.code = keyCode;
                //Follow the event logic on the other key driver to avoid the 0306 error code trigger
                inputEvent.value = event == 1 ? 0 : 1;
                mKeyPressListener.onKeyPress(inputEvent);
            }
            else {
                mDataHandler.keyEvent(event, keyCode);
            }
        }

        @Override
        public void safeKeyEvent(boolean status) {
            mDataHandler.safeKeyEvent(status);
        }

        @Override
        public void debugMessage(String message) {
            mDataHandler.debugMessage(message);
        }

        @Override
        public void cecCommand(int result) {
        }

        @Override
        public void getCECresponse(byte[] response) {
        }

        @Override
        public void irCommand(boolean result) {
            mDataHandler.irCommand(result);
        }

        @Override
        public void heartRateEvent(int heartRate) {
//            DataMatrix.putInt(DATA_HEART_RATE_INT,heartRate);
        }

        @Override
        public void canRx(byte[] data) {
            mCanManager.processCanRx(data);
        }
    };

    private ISerialPort createIoSerialPort(String devicePath) {
        SerialPortConfiguration serialPortConfiguration = SerialPortConfiguration.createRawPort(BAUD_RATE);
        serialPortConfiguration.TRACE_DEBUG = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_LOG_MCU_PACKETS_BOOL);
        // we use a dedicated read thread, so blocking for data or close should be fine
        serialPortConfiguration.setReadBlocking();
        return new SerialPort(devicePath, serialPortConfiguration);
    }

    private byte getHardwarePowerType(HardwareControlDevices device) {
        return PowerType.values()[device.ordinal()].getID();
    }

    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent.getAction());
            Log.d(TAG, "rebooting from mCmdHandler");

            //SystemVolume.getInstance().setVolume(0);
            //SystemVolume.getInstance().mute();
            //Driver.GpioWrapper.speakerEnable(false); // disable speaker if current hardware has the ability

            mCmdHandler.enterReboot();
        }
    };
    private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent.getAction());
            DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.UPDATE.name());
            String target = intent.getStringExtra("target");
            if (target == null) target = McuPacketManager.UPDATE_TARGET;

            String path = intent.getStringExtra("path");
            if (path != null) {
                Log.d(TAG, "onReceive path: " + path + " for " + target);
                File file = new File(path);
                if (file.exists()) {
                    mCmdHandler.updateIO(Uri.fromFile(file), target);
                }
            }
        }
    };
    private final BroadcastReceiver stopWatchdogReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Update starting, disabling IO watchdog timer");
            if(mcuMode == MCU_MODE.APPLICATION)
                mCmdHandler.setWatchdog(0);
        }
    };

    private void ioEnterERP() {
        IntentFilter filter = new IntentFilter(UPDATE_STARTED_INTENT);
        Context context = ContextUtil.getApplicationContext();
        if (context != null) {
            Intent stickyIntent = context.registerReceiver(null, filter);
            if (stickyIntent == null) {
                if (DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TYPE_ENUM_INT)
                        == SettingKeys.ERPType.Screen.ordinal()) {
                    if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                        SystemBrightness.setSystemBrightnessToZero(context.getContentResolver());
                        DataMatrix.putBoolean(UiServiceKeys.ACTION_SHOW_OVERLAY_PANEL_BOOL, true);
                    } else {
                        ExecuteAsRoot.simulatePowerButton();
                    }
                } else {
                    DataMatrix.putInt(UiServiceKeys.ACTION_SHOW_OVERLAY_PANEL_MS_INT, 10000);

                    // disable speaker if current hardware has the ability
                    Driver.GpioWrapper.speakerEnable(false);

                    // Only run this logic on Vortex-based hardware to resolve issue where console flashes before entering ERP (ESDV-509).
                    // This command sends the power button which will turns off the screen.
                    if (ProductConfig.hardware.isVortexBase()) {
                        ExecuteAsRoot.run("input keyevent 26");
                    }

                    mCmdHandler.enterERP();
                }
            } else {
                Log.d(TAG, "Not entering ERP, currently displaying update screen!");
            }
        }
    }
}
