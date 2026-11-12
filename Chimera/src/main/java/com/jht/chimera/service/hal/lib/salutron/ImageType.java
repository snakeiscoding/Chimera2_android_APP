package com.jht.chimera.service.hal.lib.salutron;

public enum ImageType {
    SOFTDEVICE(1),
    BOOTLOADER(2),
    COMBINED_SOFTDEVICE_BOOTLOADER(3),
    APPLICATION(4);

    private int value;

    private ImageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
