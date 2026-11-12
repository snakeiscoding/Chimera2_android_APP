package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public enum DeviceDataCommands {
    DeviceGetEEPromMemorySizes(0x01),
    DeviceGetEEPromMemoryContent(0x02),
    DeviceGetECBCurrent(0x03),
    DeviceGetEStopVale(0x04),
    DeviceGetDCBusStatus(0x05),
    DeviceGetLCBversion(0x06),
    DeviceGetControlZoneInfo(0x07),

    DeviceGetUpdateLcbMcuType(0x08),
    DeviceGetUpdateLcbModuleName(0x09),
    DeviceGetUpdateLcbBrand(0x0A),
    DeviceGetUpdateLcbBootVersion(0x0B),
    DeviceGetUpdateBinAddrMcuType(0x0D),
    DeviceGetUpdateBinAddrModuleName(0x0C),
    DeviceGetUpdateBinAddrBrand(0x0E),
    DeviceGetUpdateBinAddrBootVersion(0x0F),

    DeviceGetUpdateMappingAddrBinToMcu(0x11),

    DeviceSoftwareReset(0x70),
    DeviceEraseLoaderMemory(0x71),
    DeviceWriteWordLoaderData(0x72),
    DeviceReadByteData(0x73),

    DeviceSavePWM(0x80),
    DeviceReadPWM(0x81),

    DeviceGetLCBinformation(0xF0),

    DeviceSetMcbLcbParameter(0x90),
    DeviceGetMcbLcbParameter(0x91);


    private int Value;

    DeviceDataCommands(int value) {
        Value = value;
    }

    public static DeviceDataCommands fromInt(int i) {
        for (DeviceDataCommands b : DeviceDataCommands.values()) {
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
