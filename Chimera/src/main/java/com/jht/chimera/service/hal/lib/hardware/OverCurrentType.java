package com.jht.chimera.service.hal.lib.hardware;

public enum OverCurrentType {
    OC_AUTO_LCB(0x01),    //auto OC ex. Proformance Plus
    OC_DC_BUS_CURRENT_LCB(0x02),    //dc-bus current OC ex. T600E 
    OC_ERROR_CODE_LCB(0x03),    //error code OC ex. Proformance Plus
    OC_UNKNOWN(0x00); //non OC

    public int Value;

    OverCurrentType(int value) {
        Value = value;
    }

    public static OverCurrentType fromInt(int i) {
        for (OverCurrentType b : OverCurrentType.values()) {
            if (b.Value == i) {
                return b;
            }
        }
        return OC_UNKNOWN;
    }

    public int getValue() {
        return Value;
    }

    public byte getByteValue() {
        byte byteValue = (byte) Value;
        return byteValue;
    }

}
