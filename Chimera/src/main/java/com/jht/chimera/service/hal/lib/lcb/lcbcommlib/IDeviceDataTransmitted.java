package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public interface IDeviceDataTransmitted {

    void addReceiveListener(IDeviceDataReceived listener);
	
    void removeReceiveListener(IDeviceDataReceived listener);
	
    // Sub Command 0x01
    //DeviceData GetEEPromMemorySizes
    void funDeviceGetEEPromMemorySizes();
    // Sub Command 0x02
    //DeviceData GetEEPromMemoryContent
    void funDeviceGetEEPromMemoryContent(byte[] bytesAddress);
    // Sub Command 0x03
    //DeviceData GetECBCurrent
    void funDeviceGetECBCurrent();
    // Sub Command 0x04
    //DeviceData GetEStopVale
    void funDeviceGetEStopVale(LcbComm.QueueMode mode);
    // Sub Command 0x05
    //DeviceData GetDCBusStatus
    void funDeviceGetDCBusStatus();
    // Sub Command 0x06
    //DeviceData GetLCBversion
    void funDeviceGetLCBversion();
    void funDeviceGetLCBversionHighestPriority();
    // Sub Command 0x07
    //DeviceData GetControlZoneInfo
    void funDeviceGetControlZoneInfo();
    // Sub Command 0x08
    //DeviceData GetUpdateLcbMcuType
    void funDeviceGetUpdateLcbMcuType();
    // Sub Command 0x09
    //DeviceData GetUpdateLcbModuleName
    void funDeviceGetUpdateLcbModuleName();
    // Sub Command 0x0A
    //DeviceData GetUpdateLcbBrand
    void funDeviceGetUpdateLcbBrand();
    // Sub Command 0x0B
    //DeviceData GetUpdateLcbBootVersion
    void funDeviceGetUpdateLcbBootVersion();
    // Sub Command 0x0D
    //DeviceData GetUpdateBinAddrMcuType
    void funDeviceGetUpdateBinAddrMcuType();
    // Sub Command 0x0C
    //DeviceData GetUpdateBinAddrModuleName
    void funDeviceGetUpdateBinAddrModuleName();
    // Sub Command 0x0E
    //DeviceData GetUpdateBinAddrBrand
    void funDeviceGetUpdateBinAddrBrand();
    // Sub Command 0x0F
    //DeviceData GetUpdateBinAddrBootVersion
    void funDeviceGetUpdateBinAddrBootVersion();
    // Sub Command 0x11
    //DeviceData GetUpdateMappingAddrBinToMcu
    void funDeviceGetUpdateMappingAddrBinToMcu();
    // Sub Command 0x70
    //DeviceData SoftwareReset
    void funDeviceSoftwareReset();
    // Sub Command 0x71
    //DeviceData EraseLoaderMemory
    void funDeviceEraseLoaderMemory(byte[] bytesAddress);
    // Sub Command 0x72
    //DeviceData WriteWordLoaderData
    void funDeviceWriteWordLoaderData(byte[] bytesAddressData);
    // Sub Command 0x73
    //DeviceData ReadByteData
    void funDeviceReadByteData(byte[] bytesAddressLen);
    // Sub Command 0x80
    //DeviceData SavePWM
    void funDeviceSavePWM(byte[] bytesGroupData);
    // Sub Command 0x81
    //DeviceData ReadPWM
    void funDeviceReadPWM(byte byteGroup);
    // Sub Command 0xF0
    //DeviceData GetLCBinformation
    void funDeviceGetLCBinformation();
    void funDeviceGetLCBinformationHighestPriority();
    // Sub Command 0x90
    //DeviceData SetMcbLcbParameter
    void funDeviceSetMcbLcbParameter(byte[] bytesTypeData);
    // Sub Command 0x91
    //DeviceData GetMcbLcbParameter
    void funDeviceGetMcbLcbParameter(byte byteType);
	
}
