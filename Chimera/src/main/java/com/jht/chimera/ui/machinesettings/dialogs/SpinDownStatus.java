package com.jht.chimera.ui.machinesettings.dialogs;

public enum SpinDownStatus {
    None(0x00),
    Calculating(0x01),
    Succeed(0x02),
    Fail(0x03),
    DeviceError(0x04),
    OperationError(0x05),
    Canceled(0x06);

    int value;

    SpinDownStatus(int value) {
        this.value = value;
    }

    public static SpinDownStatus fromInt(int i) {
        for (SpinDownStatus b : SpinDownStatus.values()) {
            if (b.value == i) {
                return b;
            }
        }
        return null;
    }
}
