package com.jht.chimera.lib.common.config;

import android.os.Build;
import android.view.WindowManager;

/**
 * TODO https://jhtnaee.atlassian.net/browse/CGC-20594
 */
@SuppressWarnings("deprecation")
public class ChimeraEnums {

    // same as WindowManager.LayoutParams.TYPE_SYSTEM_ALERT but removes warning
    public static final int TYPE_SYSTEM_ALERT = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) ? WindowManager.LayoutParams.TYPE_SYSTEM_ALERT : WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY; // windows takes space on the screen
    // same as WindowManager.LayoutParams.TYPE_SYSTEM_ERROR but removes warning
    public static final int TYPE_SYSTEM_ERROR = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR; // window overlays onto the screen

    public static boolean useErrorForSystemAlert = false;
    public static int getTypeSystemAlert() {
        return useErrorForSystemAlert ? WindowManager.LayoutParams.TYPE_SYSTEM_ERROR : TYPE_SYSTEM_ALERT;
    }

    public static final int SAFETY_KEY_NOT_PULLED = 0x0000;
    public static final int SAFETY_KEY_TREADMILL = 0x0001;
    public static final int SAFETY_KEY_CLIMBMILL_UCB = 0x0002;
    public static final int SAFETY_KEY_CLIMBMILL_HANDRAIL = 0x0004;
    public static final int SAFETY_KEY_CLIMBMILL_CZ = 0x0008;
    public static final int SAFETY_KEY_CLIMBMILL_IR = 0x0010;
    public static final int SAFETY_KEY_TM_ROLLER_GUARD = 0x0020;
    public static final int SAFETY_KEY_TM_HYBRID_IR_ESTOP = 0x0040;

    /** Any safety key/switch that halts a treadmill belt. */
    public static final int SAFETY_KEY_TREADMILL_MASK = SAFETY_KEY_TREADMILL | SAFETY_KEY_TM_ROLLER_GUARD | SAFETY_KEY_TM_HYBRID_IR_ESTOP;
    public static boolean isTreadmillSafetyKeyPulled(int status) {
        return (status & SAFETY_KEY_TREADMILL_MASK) != 0;
    }

    public static boolean isTreadmillRollerGuard(int status) {
        return (status & SAFETY_KEY_TM_ROLLER_GUARD) > 0;
    }

    public static boolean isTreadmillHybridIrEStop(int status) {
        return (status & SAFETY_KEY_TM_HYBRID_IR_ESTOP) > 0;
    }

    public static boolean isClimbmillHandrailPulled(int status) {
        return (status & SAFETY_KEY_CLIMBMILL_HANDRAIL) > 0;
    }

    public static boolean isClimbmillUCBPulled(int status) {
        return (status & SAFETY_KEY_CLIMBMILL_UCB) > 0;
    }

    public static boolean isClimbmillControlZoneTriggered(int status) {
        return (status & SAFETY_KEY_CLIMBMILL_CZ) > 0;
    }

    public static boolean isClimbmillIRSensorTriggered(int status) {
        return (status & SAFETY_KEY_CLIMBMILL_IR) > 0;
    }

    public static boolean isAnySafetyErrorTriggered(int status) {
        return isTreadmillSafetyKeyPulled(status) | isClimbmillHandrailPulled(status) | isClimbmillUCBPulled(status);
    }

    public static boolean isClimbmillSafetyErrorTriggered(int status) {
        return isClimbmillHandrailPulled(status) | isClimbmillUCBPulled(status);
    }

    public enum Brand {
        VISION(1),
        HORIZON(2),
        SPRINT8(3),
        JOHNSON(4),
        MATRIX_RETAIL(5),
        PASSPORT(6);

        public int value;

        Brand(int value) {
            this.value = value;
        }
    }

/*
    public enum MachineSubtype {
        Unknown,
        Treadmill,
        Elliptical,
        HybridBike,
        UprightBike,
        RecumbentBike,
        Ascent,
        LowerBodyAscent,
        Stepper,
        Climbmill,
        Rower,
        PassportPlayer,
        Strength,
        SDrive;

        public int getValue() {
            return this.ordinal();
        }
    }

    public enum MachineType {
        UNKNOWN(-1),
        TREADMILL(0),
        ELLIPTICAL(1),
        BIKE(2),
        ASCENT(3),
        STEPPER(4),
        CLIMBMILL(5),
        ROWER(6),
        PASSPORT_PLAYER(7),
        STRENGTH(8),
        SDRIVE(9),
        SFORCE(10);

        public int value;

        MachineType(int value) {
            this.value = value;
        }
    }
*/
    public enum ConsoleId {
        XR(1),
        XER(2),
        XIR(3),
        AR90(4),
        XER2(5),
        XIR2(6),
        XUR(7),
        CHIMERA1(8),
        CHIMERA2(9);

        public int value;

        ConsoleId(int value) {
            this.value = value;
        }
    }

}
