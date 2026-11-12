package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public enum ControlType {
    CONTROL_BIKE(0x00),
    CONTROL_STEPPER(0x01),
    CONTROL_RAMP_EP(0x02),
    CONTROL_SWING_EP(0x03),
    CONTROL_TM(0x04),
    CONTROL_CLIMBMILL(0x05),
    CONTROL_RETAIL_CLIMBMILL(0x06),
	CONTROL_INDOORBIKE(0x07),

    CONTROL_UNKNOWN(0xFF);

    public int Value;

    ControlType(int value) {
        this.Value = value;
    }

    public static ControlType fromInt(int i) {
        for (ControlType b : ControlType.values()) {
            if (b.Value == i) {
                return b;
            }
        }
        return null;
    }

    public int getValue() {
        return Value;
    }

    public byte getByteValue() {
        byte byteValue = (byte) Value;
        return byteValue;
    }
}
