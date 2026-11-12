package com.jht.chimera.lib.common.config;

import android.util.Log;

import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

public final class SerialNumberHelper {
    private final static String TAG = "SerialNumberHelper";

    // should give some indication why validate failed
    public enum Error {
        NONE, BAD_LENGTH, NO_MATCHES;
        public boolean isError() { return this != NONE; }
    }

    public static FrameConfigEnum getFrameFromSn(String frameSn) {
        int lcb = DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT);
        if (!FrameConfigEnum.isKnownLcb(lcb)) {
            Log.w(TAG, "No supported frames with lcb of 0x" + String.format("%02X", lcb));
            lcb = 0; // this is used as a wild card for LCB type checking
        }
        return FrameConfigEnum.getFrameFromSerial(frameSn, lcb);
    }

    public static Error isFrameSnValid(String frameSn) {
        FrameConfigEnum frameConfigEnum = getFrameFromSn(frameSn);
        return isFrameSnValid(frameConfigEnum, frameSn);
    }
    public static Error isFrameSnValid(FrameConfigEnum frameConfigEnum, String frameSn) {
        if (frameConfigEnum == null) return Error.NO_MATCHES;
        if (frameSn.length() < FrameConfig.MINIMUM_FRAME_SERIAL_LENGTH ) return Error.BAD_LENGTH;
        return Error.NONE;
    }

    public static Error isConsoleSnValid(String consoleSn) {
        String frameSn = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
        FrameConfigEnum frameConfigEnum = getFrameFromSn(frameSn);
        return isConsoleSnValid(frameConfigEnum, consoleSn);
    }
    public static Error isConsoleSnValid(FrameConfigEnum frameConfigEnum, String consoleSn) {
        if (frameConfigEnum == null) return Error.NO_MATCHES;
        FrameConfig frameConfig = frameConfigEnum.createFrameConfig();
        ConsoleType consoleType = ProductConfig.getConsoleType(frameConfig.machineType);
        ConsoleType consoleTypeSn = ConsoleType.findBySerialNumber(consoleSn);
        if (!consoleType.equals(consoleTypeSn)) return Error.NO_MATCHES;
        String prefix = consoleType.extractMatchingPrefix(consoleSn);
        if (consoleSn.length() - prefix.length() < 2) return Error.BAD_LENGTH;
        return Error.NONE;
    }

}
