package com.jht.chimera.service.hal.lib.hardware;

import android.util.Log;

import com.jht.chimera.lib.common.config.ConsoleType;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.config.frames.FramePowerRules;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

// TOP LEVEL BASE CLASS
public class PowerLimiter {
    public static PowerLimiter getInstance() { return globalInstance; }
    private static final PowerLimiter globalInstance = createLimiter();
    private static PowerLimiter createLimiter() {
        switch (ProductConfig.flavor) {
            case Parker:
            case Vision600E:
            case T600XE:
                return new PowerLimiter_Parker();
        }
        return new PowerLimiter();
    }
    public void checkApplyLimits(boolean isExternalPower) { }
    public void removeAllLimits() {}
}


// PARKER
class PowerLimiter_Parker extends PowerLimiter {
    private static final String TAG =  "PowerLimiter_Parker";
    private static final float volumeLimit = 0.5f;  // TODO - what ratio gets us 0.5W, try 50% first?

    private void applyVolumeLimit() {
        Log.d(TAG, "apply volume limit ratio " + volumeLimit);
        DataMatrix.putFloat(HALKeys.DATA_SYSTEM_VOLUME_LIMIT_RATIO_FLOAT, volumeLimit);
    }
    private void removeVolumeLimit() {
        Log.d(TAG, "remove volume limit");
        DataMatrix.putFloat(HALKeys.DATA_SYSTEM_VOLUME_LIMIT_RATIO_FLOAT, 1);
    }

    @Override
    public void checkApplyLimits(boolean isExternalPower) {
        // 2020-05-14 Detection of DC 14V via current is not reliable at lower power due to leakage current up to 100mA at sensor! Rely on frame alone.
        // 2020-07-02 Voltage based detection of external power added to parker rev D board.
        FrameConfigEnum frame = FrameConfigManager.getInstance().getFrameType();
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        if (frame == null || frameConfig == null) {
            Log.e(TAG, "NULL frame or frame.frameConfig given");
            Driver.GpioWrapper.powerLimitCharging(true); // make sure front USB is setup, OS might boot it in off state?
            return;
        }
        ConsoleType type = ProductConfig.getConsoleType();
        boolean is22 = (type.inches == 22);
        boolean is16 = (type.inches == 16);
        boolean invalidCombo = false;
        //int lcb = DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT);
        Log.d(TAG, "checkApplyLimits isExternalPower=" + isExternalPower + ", frame=" + frame.name() + ", type=" + type.name());// + ", lcb=" + lcb);
        switch (frameConfig.powerRule) {
            case FramePowerRules.Parker.RULE_NO22_USB_VOLUME:
                if (is22) {
                    invalidCombo = true;
                } else {
                    Driver.GpioWrapper.powerLimitCharging(true);
                    Driver.GpioWrapper.powerHdmiBoard(false);
                    if (is16) {
                        applyVolumeLimit();
                    }
                }
                break;
            case FramePowerRules.Parker.RULE_NO22_USB:
                if (is22) {
                    invalidCombo = true;
                } else {
                    Driver.GpioWrapper.powerLimitCharging(true);
                    Driver.GpioWrapper.powerHdmiBoard(false);
                }
                break;
            case FramePowerRules.Parker.RULE_NO22:
                if (is22) {
                    invalidCombo = true;
                } else {
                    removeAllLimits();
                }
                break;
            case FramePowerRules.Parker.RULE_FULL_POWER:
                removeAllLimits();
                break;
            default:
                //    Default to the state least likely to have the load exceed the power available.
                Log.w(TAG, "Frame not in power limit list, running all limits = " + frame.name());
                applyAllLimits();
                break;
        }

        if (invalidCombo) {
            applyAllLimits();
            Log.e(TAG, "Machine Configuration Error! Applying all power limits!");
        }
        DataMatrix.putBoolean(HALKeys.DATA_FRAME_CONSOLE_COMBO_INVALID_BOOL, invalidCombo); // either clear or set invalid flag
    }

    @Override
    public void removeAllLimits() {
        Driver.GpioWrapper.powerLimitCharging(false);
        Driver.GpioWrapper.powerHdmiBoard(true);
        removeVolumeLimit();
    }
    private void applyAllLimits() {
        Driver.GpioWrapper.powerLimitCharging(true);
        Driver.GpioWrapper.powerHdmiBoard(false);
        applyVolumeLimit();
    }

}
