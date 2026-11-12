package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public interface IDeviceDataReceived {
    //Get EEProm Memory Sizes of sub command 0x01
    void deviceGetEEPromMemorySizesHandler(int intValue);
    //Get EEProm Memory Content of sub command 0x02
    void deviceGetEEPromMemoryContentHandler(byte[] bytesMemoryContent);
    //Get ECB Current of sub command 0x03
    void deviceGetECBCurrentHandler(byte[] bytesECBCurrent);
    //Get EStop Vale of sub command 0x04
    void deviceGetEStopValeHandler(byte[] bytesEStopVale);
    //Get DC Bus Status of sub command 0x05
    void deviceGetDCBusStatusHandler(byte[] bytesDCBusStatus);
    //Get LCB version of sub command 0x06
    void deviceGetLCBversionHandler(byte[] bytesLCBversion);
    //Get Control Zone Info of sub command 0x07
    void deviceGetControlZoneInfoHandler(byte[] bytesControlZoneInfo);
    //Get LCB McuType of sub command 0x08
    void deviceGetUpdateLcbMcuTypeHandler(byte[] bytesMcuType);
    //Get LCB ModuleName of sub command 0x09
    void deviceGetUpdateLcbModuleNameHandler(byte[] bytesModuleName);
    //Get LCB Brand of sub command 0x0A
    void deviceGetUpdateLcbBrandHandler(byte[] bytesBrand);
    //Get LCB BootVersion of sub command 0x0B
    void deviceGetUpdateLcbBootVersionHandler(byte[] bytesBootVersion);
    //Get LCB McuType of sub command 0x0D
    void deviceGetUpdateBinAddrMcuTypeHandler(byte[] bytesMcuTypeAddr);
    //Get LCB ModuleName of sub command 0x0C
    void deviceGetUpdateBinAddrModuleNameHandler(byte[] bytesModuleNameAddr);
    //Get LCB Brand of sub command 0x0E
    void deviceGetUpdateBinAddrBrandHandler(byte[] bytesBrandAddr);
    //Get LCB BootVersion of sub command 0x0F
    void deviceGetUpdateBinAddrBootVersionHandler(byte[] bytesBootVersionAddr);
    //Get LCB BootVersion of sub command 0x0F
    void deviceGetUpdateMappingAddrBinToMcuHandler(byte[] bytesMappingAddr);
    //LCB Run Reset (PWM) Lcb Software Reset of sub command 0x70
    void deviceSoftwareResetHandler();
    //Erase Loader Memory of sub command 0x71
    void deviceEraseLoaderMemoryHandler(byte byteStatus);
    //Write Word Loader Data of sub command 0x72
    void deviceWriteWordLoaderDataHandler(byte byteStatus);
    //Read Byte Data of sub command 0x73
    void deviceReadByteDataHandler(byte[] bytesStatusLenData);
    //Save PWM of sub command 0x80
    void deviceSavePWMHandler(byte byteStatus);
    //Read PWM of sub command 0x81
    void deviceReadPWMHandler(byte[] bytesData);
    //Get LCB information of sub command 0xF0
    void deviceGetLCBinformationHandler(byte[] bytesLcbInfo);
    //Set Mcb Lcb Parameter of sub command 0x90
    void deviceSetMcbLcbParameterHandler(byte byteStatus);
    //Get Mcb Lcb Parameter of sub command 0x91
    void deviceGetMcbLcbParameterHandler(byte[] bytesTypeLenData);
/*    // triggered by lcb status byte filter FILTER_LCB_STATUS_B0_BIT_INCLINE_DOWN_ACTION 
    // 0: stop 1:action
    void changeLcbInclineMotorDownwardStatusHandler(boolean isLcbDownwardStatus);
    // triggered by lcb status byte filter FILTER_LCB_STATUS_B2_BIT_MAIN_MOTOR_ACTION 
    // 0: stop 1:action
    // triggered by response of command 0x50
    void setLcbDeviceDataHandler(byte[] bytesDataReceived);	
    // triggered by response of command 0x83
    void getLcbECBstatusHandler(byte byteStatus);	
    // triggered by response of command 0x4B
    //Get SPM
    void getLcbSPMHandler(int intValue);	
*/	
}
