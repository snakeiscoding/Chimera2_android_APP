package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public interface IUniDataReceived {
    //Get Moto Type of sub command 0x03
    void uniGetMotoTypeHandler(int intMotoTypes);
    //Set PFC mode of sub command 0x23
    void uniSetPFCmodeHandler(byte[] bytesData);
    //Magnet Angle State of sub command 0x24
    void uniMagnetAngleStateHandler(byte[] bytesData);
    //Mcb Voltage of sub command 0x25
    void uniMcbVoltageHandler(byte[] bytesData);
}
