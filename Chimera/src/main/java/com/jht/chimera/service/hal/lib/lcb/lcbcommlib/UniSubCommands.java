package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public enum UniSubCommands {
    GetMotoType(0x03),
    LockMotoProtect(0x11),
    MotoRateSlip(0x12),
    MotoRateCurrent(0x13),
    RateVol(0x14),
    RateFreq(0x15),
    CompensationMotoRes(0x16),
    OverCurrentDetTime(0x17),
    OverLoadDetTime(0x18),
    OverVolDetTime(0x19),
    MotoWireBreakDetTime(0x1A),
    InclineDirDef(0x1B),
    InclineSenErrDetTime(0x1C),
    InclineErrDetTime(0x1D),
    OverCurrentCountH(0x1E),
    OverCurrentCountL(0x1F),
    OverCurrentH(0x20),
    OverCurrentL(0x21),
    GetCasterSwitchStatus(0x22),
    SetPFCmode(0x23),
    MagnetAngleCal(0x24),
    McbVoltage(0x25);


    public int Value;

    UniSubCommands(int value) {
        Value = value;
    }

    public static UniSubCommands fromInt(int i) {
        for (UniSubCommands b : UniSubCommands.values()) {
            if (b.Value == i) {
                return b;
            }
        }
        return null;
    }

    public byte getByteValue() {
        byte byteValue = (byte) Value;
        return byteValue;
    }

}
