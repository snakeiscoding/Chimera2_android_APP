package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public enum MachineCategoryType {
    CATEGORY_BIKE(0x01),
    CATEGORY_TM(0x02),

    CATEGORY_UNKNOWN(0x00);

    public int Value;

    MachineCategoryType(int value) {
        this.Value = value;
    }

    public static MachineCategoryType fromInt(int i) {
        for (MachineCategoryType b : MachineCategoryType.values()) {
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
