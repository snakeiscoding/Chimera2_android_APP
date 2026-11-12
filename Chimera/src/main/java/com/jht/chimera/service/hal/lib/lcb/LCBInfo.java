package com.jht.chimera.service.hal.lib.lcb;

public class LCBInfo {
    public static LCBStatus _status = new LCBStatus((byte) 0);
    public static byte lcbType = 0x14;
    public static boolean _machineInitialized = false;
    public static boolean _startMachine = false;
    public static boolean _inCalibration = false;

    /*
    For Treadmill MCB lock define
    Bit 0 : Switch Lock State (on : 1)
    Bit 1 : Error Code Lock State (02B2 : 1)
    Bit 2 : MCB Lock State ( MCB Lock : 1)
    Bit 3 : MCB Circuit Lock State ( MCB Safety Trigger : 1)
    Bit 4 : Unlock state (02B1 :1)
    */
    public enum MCBState {
        SWITCH_LOCK(0x01),
        MCB_CODE_LOCK(0x02),
        MCB_STATE_LOCK(0x04),
        MCB_STATE_CIRCUIT_LOCK(0x08),
        DEFAULT(0x00);

        private final byte id;

        MCBState(int id) {
            this.id = (byte) id;
        }

        public byte getId() {
            return this.id;
        }
    }

    public static byte _mcbLockstate = 0x00;

    public static void resetMCBLockState() { _mcbLockstate = 0x00; }

    public static boolean getMCBLockState() { return (_mcbLockstate >0 ); }

    public static boolean _lcbDebug = false;

    public static boolean isChopper() {
        return lcbType == 0x14 || lcbType == 0x13;
    }

}
