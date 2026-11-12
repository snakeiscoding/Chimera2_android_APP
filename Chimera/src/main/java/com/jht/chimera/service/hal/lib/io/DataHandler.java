package com.jht.chimera.service.hal.lib.io;

import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_TREADMILL;
import static com.jht.chimera.lib.common.keypress.KeyPress.KeyState.KEY_DOWN;
import static com.jht.chimera.lib.common.keypress.KeyPress.KeyState.KEY_UP;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ConsoleType;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.enums.HardwareControlDevices;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.lib.common.log.ConsoleEvent;
import com.jht.chimera.service.hal.LCBService;
import com.jht.chimera.service.hal.lib.io.iocommlib.McuPacketManager;
import com.jht.chimera.service.hal.lib.io.iocommlib.PowerType;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.liberror.ErrorCodeManager;
import com.jht.liberror.ErrorCodes;

import java.util.List;

public class DataHandler {
    private final String TAG = "DataHandler";

    private FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

    private CmdHandler mCmdHandler = new CmdHandler();
    private Context context;

    private Boolean burningKey1 = null;
    private Boolean burningKey2 = null;
    private boolean mKeyHold = false;
    private boolean mStopKeyHold = false;
    private short mCurrentHold = -1;
    private JHTTimer burningTimer = new JHTTimer();
    private JHTTimer keyHoldTimer = new JHTTimer(60*1000, () -> {
        ErrorCodeManager.handleErrorCode(ErrorCodes.UcbKeyPressTooLong.getCode());
    });
    private boolean isLcgui = ProductConfig.getConsoleType().equals(ConsoleType.lcgui16) || ProductConfig.getConsoleType().equals(ConsoleType.lcgui22);
    private boolean isParker = ProductConfig.isParker();
    private Handler handler = new Handler(Looper.getMainLooper());

    public DataHandler(Context context) {
        this.context = context;

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    KeyPress.KeyCode key = KeyPress.KeyCode.valueOf(intent.getStringExtra("key_code"));
                    DataMatrix.putInt(HALKeys.EVENT_KEYPRESS_BITMAP_INT, KeyPress.createBitmapKey(key, KEY_DOWN));
                }
                catch(Exception ignored) {}


            }
        }, new IntentFilter("com.jht.keypress"));
    }

    public void version(int version) {
        String versionString = "" + (version >> 8 & 0x00ff) + "." + (version & 0x00ff);
        DataMatrix.putString(HALKeys.DATA_VERSION_IO_STRING, "" + versionString);
        AndroidSetting.putString(context.getContentResolver(), "chimera_io_version", versionString + ".0.0"); //NON-NLS
    }

    public void safeKeyStatus(boolean status) {
        if(!status && (DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT) & SAFETY_KEY_TREADMILL) == 0)
            return;
        DataMatrix.putBoolean(HALKeys.DATA_IO_IS_SAFETY_PULLED_BOOL, status);
        Log.d(TAG, "Notify LCB Service mcuSafetyKeyPulled " + status);
    }

    public void powerState(byte source, boolean _switch) {
        PowerType pt = PowerType.format(source);
        if(pt == null) return;
        HardwareControlDevices device = HardwareControlDevices.values()[pt.ordinal()];
        Log.d(TAG, "Device: " + device + "  switch: " + _switch);
        if (_switch)
            DataMatrix.putInt(HALKeys.DATA_IO_POWER_ON_DEVICE_ENUM_INT, source);
        if (device.equals(HardwareControlDevices.TV)){
            DataMatrix.putBoolean(HALKeys.DATA_TV_IO_POWER_STATE_BOOL, _switch);
        }

        ///machineSettings.powerState.getValue(device).setValue(_switch);
    }

    public void externalPower(McuPacketManager.PowerVoltage powerVoltage, int current){
        Log.d(TAG, "Power source: " + powerVoltage + "  Current: " + current);
        if (ProductConfig.hasPowerLimits()) {
            // this function is called with either 14V or 12V based on return from io mcu POWER_STATE command
            if (powerVoltage == McuPacketManager.PowerVoltage.POWER_14V && current != 0) {
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL, true);
            }
            else if (!ProductConfig.platform.equals(ProductConfig.Platform.LCGUI)){
                mCmdHandler.setPower(PowerType.ext_14V.getID(), 0);
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL, false);
            }
            Log.d(TAG, "External Power Board: " + DataMatrix.getBoolean(HALKeys.DATA_IO_EXTERNAL_POWER_BOOL));
            mCmdHandler.setPower(PowerType.qiPower.getID(), DataMatrix.getBoolean(HALKeys.DATA_IO_EXTERNAL_POWER_BOOL) ? McuPacketManager.enablePower : McuPacketManager.disablePower);
        } else if (ProductConfig.requiresExtPower()) {
            int extPowerMode = ProductConfig.needsExtPowerSwitch()
                    ? McuPacketManager.powerOnERPOff
                    : McuPacketManager.enablePower;
            mCmdHandler.setPower(PowerType.ext_14V.getID(), extPowerMode);
        } else {
            if(powerVoltage == McuPacketManager.PowerVoltage.POWER_14V  && current > 0) {
                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL, true);
                mCmdHandler.setPower(PowerType.qiPower.getID(), (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL) ? 1 : 0));
            }
            Log.d(TAG, "externalPower: " + (powerVoltage == McuPacketManager.PowerVoltage.POWER_14V));
        }
    }

    public void consolePowerVoltage(short voltage) {
        //Log.d(TAG, "consolePowerVoltage: " + voltage);
        DataMatrix.putInt(HALKeys.DATA_CONSOLE_VOLTAGE_INT,voltage);
    }

    public void extendPowerVoltage(short voltage, boolean check) {
        Log.d(TAG, "extendPowerVoltage: " + voltage);
        DataMatrix.putInt(HALKeys.DATA_EXTEND_VOLTAGE_INT,voltage);
//        if(check){
//            if(voltage > 1100)
//                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL,true);
//            else {
//                mCmdHandler.setPower(PowerType.ext_14V.getID(), false);
//                DataMatrix.putBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL, false);
//            }
//            Log.d(TAG, "External Power Board: " + DataMatrix.getBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL));
//            mCmdHandler.setPower(PowerType.qiPower.getID(),DataMatrix.getBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL));
//        }

    }

    public void usbVoltage(short voltage) {
        //Log.d(TAG, "usbVoltage: " + voltage);
        DataMatrix.putInt(HALKeys.DATA_USB_VOLTAGE_INT,voltage);
    }

    public void consolePowerCurrent(short current) {
        //Log.d(TAG, "consolePowerCurrent: " + current);
        DataMatrix.putInt(HALKeys.DATA_CONSOLE_CURRENT_INT,current);
    }

    public void extendPowerCurrent(short current) {
        //Log.d(TAG, "extendPowerCurrent: " + current);
        DataMatrix.putInt(HALKeys.DATA_EXTEND_CURRENT_INT,current);
//        if(current > 3000){
//            // TODO: We need to log an analytics event.
//            Log.e(TAG, "Extend Power Overload( " + current + " )");
//        }
    }

    //public void extendPowerError(boolean error) {
    //    DataMatrix.putBoolean(HALKeys.DATA_EXTEND_ERROR_BOOL,error);
    //}

    public void boot(boolean state) {
        if (state)
            DataMatrix.putInt(SettingKeys.Debug.DATA_WATCHDOG_REBOOT_COUNTER_INT, DataMatrix.getInt(SettingKeys.Debug.DATA_WATCHDOG_REBOOT_COUNTER_INT) + 1);
    }

    private static final List<Short> tmStopKeyCode = List.of((short) 0x204, (short) 0x0034);
    private static final List<Short> nonTmStopKeyCode = List.of((short) 0x208, (short) 0x0033);
    private boolean burningKey(int event, short keyCode){
        KeyPress.KeyState keyState = null;

        if (keyCode == 0x110 || keyCode == 0x220) {
            if (event == 0x01)
                keyState = KEY_UP;
            else if (event == 0x00)
                keyState = KEY_DOWN;

            if (keyState != null) {
                switch (keyCode) {
                    case 0x110:
                        burningKey1 = keyState == KEY_DOWN;
                        break;
                    case 0x220:
                        burningKey2 = keyState == KEY_DOWN;
                        break;
                }
                if (burningKey1 != null && burningKey2 != null) {
                    if (burningKey1 && burningKey2) {
                        Log.d(TAG, "keyEvent: BurningTimer start");
                        if (burningTimer != null) {
                            burningTimer.stop();
                            burningTimer = null;
                        }
                        burningTimer = new JHTTimer(10000, () -> {
                            DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
                            Log.d(TAG, "keyEvent: launch stability APP");
                            Intent burningAppIntent;
                            burningAppIntent = context.getPackageManager().getLaunchIntentForPackage("com.into.stability");
                            if (burningAppIntent != null) {
                                burningAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(burningAppIntent);
                            }
                        });
                        burningTimer.start();
                        burningKey1 = null;
                        burningKey2 = null;

                    } else if (!burningKey1 && !burningKey2) {
                        DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, false);
                        Log.d(TAG, "keyEvent: launch splash screen");
                        if (burningTimer != null)
                            burningTimer.stop();
                        LaunchAppUtil.launchSplashScreen();
                        burningKey1 = null;
                        burningKey2 = null;
                    }
                }
            }
            return true;
        }
        return false;
    }
    public void keyEvent(int event, short keyCode) {
        KeyPress.KeyState keyState = KEY_DOWN;
        keyHoldTimer.start();

        if (burningKey(event, keyCode))
            return;

        // This is checking whether this is the stop key is the one being acted on
        // sets the stop key to pressed
        boolean stopKeyPressed = false;
        if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill
            && tmStopKeyCode.contains(keyCode)){
            stopKeyPressed = true;

        } else if (frameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Treadmill
            && nonTmStopKeyCode.contains(keyCode)){
            stopKeyPressed = true;
        }

        // if this is releasing the button
        if (event == 0x01) {
            // if we were currently holding a button
            if (keyCode == mCurrentHold){
                if (!stopKeyPressed) {
                    mKeyHold = false;
                } else {
                    mStopKeyHold = false;
                }
                mCurrentHold = -1;
            } else {
                if (stopKeyPressed){
                    mStopKeyHold = false;
                } else {
                    return;
                }
            }

            keyState = KEY_UP;
            keyHoldTimer.stop();
        }

        // If this is key down event
        if (event == 0x00) {
            Log.d("DataHandler"," mStopKeyHold: " + mStopKeyHold + " mKeyHold: " + mKeyHold + " stopKeyPressed: " + stopKeyPressed);
            // If we are holding the stop key, do not do anything with this keyevent
            if ((mStopKeyHold || mKeyHold) && !stopKeyPressed){
                return;
            }

            if (stopKeyPressed) {
                mStopKeyHold = true;
            } else {
                mKeyHold = true;
                mCurrentHold = keyCode;
            }
        }


        // Handle creating bitmap for key
        int bitmapKey = -1;
        switch (keyCode) {
            case 0x101:
                //key = JHTCommonTypes.JHT_KEY.JHT_KEY_INCLINE_UP;
                bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_INCLINE_UP, keyState);
                break;
            case 0x0048:
                if (isLcgui){
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_INCLINE_UP, keyState);
                } else if (isParker){
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_STOP, keyState);
                }
                break;
            case 0x102:
                //key = JHTCommonTypes.JHT_KEY.JHT_KEY_INCLINE_DOWN;
                bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_INCLINE_DOWN, keyState);
                break;
            case 0x003E:
                if (isLcgui){
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_INCLINE_DOWN, keyState);
                } else if (isParker){
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_GO, keyState);
                }
                break;
            case 0x201:
                if (ProductConfig.product.equals(ProductConfig.Product.ProjectOne)) {
                    // Project One play button
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_GO, keyState);
                } else if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    //key = JHTCommonTypes.JHT_KEY.JHT_KEY_SPEED_UP;
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_SPEED_UP, keyState);
                } else {
                    //key = JHTCommonTypes.JHT_KEY.JHT_KEY_RESISTANCE_UP;
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_RESISTANCE_UP, keyState);
                }
                break;
            case 0x003F:
                if ((frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill)){
                    if (isLcgui) {
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_SPEED_DOWN, keyState);
                    } else if (isParker){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_SPEED_UP, keyState);
                    }
                } else{
                    if (isLcgui){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_RESISTANCE_DOWN, keyState);
                    } else if (isParker){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_RESISTANCE_UP, keyState);
                    }
                }
                break;
            case 0x202:
                if (ProductConfig.product.equals(ProductConfig.Product.ProjectOne)) {
                    // Project One pause button
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_PAUSE, keyState);
                } else if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    //key = JHTCommonTypes.JHT_KEY.JHT_KEY_SPEED_DOWN;
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_SPEED_DOWN, keyState);
                } else {
                    //key = JHTCommonTypes.JHT_KEY.JHT_KEY_RESISTANCE_DOWN;
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_RESISTANCE_DOWN, keyState);
                }
                break;
            case 0x0049:
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    if (isLcgui){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_SPEED_UP, keyState);
                    } else if (isParker){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_SPEED_DOWN, keyState);
                    }
                } else {
                    if (isLcgui){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_RESISTANCE_UP, keyState);
                    } else if (isParker){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_RESISTANCE_DOWN, keyState);
                    }
                }
                break;
            case 0x204:
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    //key = JHTCommonTypes.JHT_KEY.JHT_KEY_STOP;
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_STOP, keyState);
                } else {
                    //key = JHTCommonTypes.JHT_KEY.JHT_KEY_PAUSE;
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_PAUSE, keyState);
                }
                break;
            case 0x0034:
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    if (isParker){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_INCLINE_UP, keyState);
                    }
                } else {
                    if (isParker){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_PAUSE, keyState);
                    }
                }
                break;
            case 0x208:
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    //key = JHTCommonTypes.JHT_KEY.JHT_KEY_GO;
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_GO, keyState);
                } else {
                    //key = JHTCommonTypes.JHT_KEY.JHT_KEY_STOP;
                    bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_STOP, keyState);
                }
                break;
            case 0x0033:
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    if (isParker){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_INCLINE_DOWN, keyState);
                    }
                } else {
                    if (isParker){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_STOP, keyState);
                    }
                }
                break;
            case 0x120:
                bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_COOLDOWN, keyState);
                break;
            case 0x002B:
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    if (isLcgui){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_GO, keyState);
                    }
                } else {
                    if (isLcgui){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_STOP, keyState);
                    }
                }
                break;
            case 0x0035:
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    if (isLcgui){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_STOP, keyState);
                    }
                } else {
                    if (isLcgui){
                        bitmapKey = KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_PAUSE, keyState);
                    }
                }
                break;
        }
        if (bitmapKey != -1) {

            // send LCB stop commands on KEY_STOP or KEY_PAUSE key down event
            if (keyState == KEY_DOWN) {
                KeyPress.KeyCode keycode = KeyPress.getKeyCode(bitmapKey);
                if (keycode == KeyPress.KeyCode.KEY_STOP || keycode == KeyPress.KeyCode.KEY_PAUSE) {
                    sendLcbStopCommands();
                    ConsoleEvent.logEvent("STOP MACHINE: " + keycode + " down event --> sending LCB stop commands");
                }
            }

            Log.d(TAG, " key code: " + KeyPress.getKeyCode(bitmapKey) + " state : " + KeyPress.getKeyState(bitmapKey));
            DataMatrix.putInt(HALKeys.EVENT_KEYPRESS_BITMAP_INT, bitmapKey);
        }
    }

    public void safeKeyEvent(boolean status) {
        if(!status && (DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT) & SAFETY_KEY_TREADMILL) == 0)
            return;
        DataMatrix.putBoolean(HALKeys.DATA_IO_IS_SAFETY_PULLED_BOOL, status);
        Log.d(TAG, "Notify LCB Service mcuSafetyKeyPulled " + status);
    }

    public void debugMessage(String message) {
        Log.e(TAG, "Message: " + message);
    }

    public void irCommand(boolean result) {
        DataMatrix.putBoolean(HALKeys.EVENT_IO_IR_RESULT_BOOL, result);
    }

    /**
     * Function that will send LCB commands to stop the machine
     *
     * Note: the LCBCmdHandler was already a static object so using this to send the LCB commands
     */
    private void sendLcbStopCommands() {
        // do a handler post which will process the runnable in the main thread (IO and LCB share the same main thread)
        // to help to make sure the calls are synchronized as if it was processing a DataMatrix callback which does a handler post
        //
        // NOTE: the lcbCmdHandler is used in by DataMatrix callback (ie main thread), timer (different thread), and
        // LCB read thread message handling (read thread) so there could possibly be some odd threading behavior but
        // no issues have been reported so far
        handler.post(() -> {
            if(LCBService.lcbCmdHandler != null) {
                // send the stop machine which should stop the treadmill and climbmill
                LCBService.lcbCmdHandler.stopMachine();
                /* CGC-16771 - For now, don't interrupt incline. If safety concern is ruled more important than the incline reset, could see this coming back into place.
                    Could check if in workout, but do we want to involve DM here? Maybe we could buffer value from DM, but if DM dies it would be useless?
                    Could try to restore incline reset later, but where, splash? Would user be ready for movement to resume? E-stop has 2 states, so there is an action to trigger clear.
                // send the stop incline in case there is incline
                LCBService.lcbCmdHandler.stopInclineAction(); */
            }
        });
    }

}
