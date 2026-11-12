package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Bike;

import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataReceived;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbCommReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeInfoSpinDown;

public class BikeCallbackHandlerLCB implements ILcbCommReceive, IDeviceDataReceived {
    private static final String TAG = "BikeCallbackHandlerLCB";

    private BikeHandler mBikeHandler;

    public BikeCallbackHandlerLCB(BikeHandler bikeHandler) {
        mBikeHandler = bikeHandler;
    }

    @Override
    public void bootUpLeaveLcbErpHandler(boolean isBootUpLeaveErpSuccess) {
        //do nothing
    }

    @Override
    public void enterLcbErpModeHandler(boolean isEnterSuccess) {
        //do nothing
    }

    @Override
    public void leaveLcbErpModeHandler(boolean isLcbCommOk, boolean isLeaveSuccess) {
        //do nothing
    }

    @Override
    public void changeLcbInclineMotorDownwardStatusHandler(boolean isLcbDownwardStatus) {
        //do nothing
    }

    @Override
    public void changeLcbInclineMotorUpwardStatusHandler(boolean isLcbUpwardStatus) {
        //do nothing
    }

    @Override
    public void changeLcbMainMotorStatusHandler(boolean isMainMotorStatus) {
        //do nothing
    }

    @Override
    public void changeLcbEncoderRPMStatusHandler(boolean isEncoderRPMStatus) {
        //do nothing
    }


    @Override
    public void changeLcbBatteryCapacityStatusHandler(boolean isBatteryCapacityStatus) {
        //do nothing
    }

    @Override
    public void changeLcbCommandErrorStatusHandler(boolean isCommandErrorStatus) {
        //do nothing
    }

    @Override
    public void changeLcbLcbErrorStatusDetectedHandler(boolean isLcbErrorStatus) {
        //do nothing
    }

    @Override
    public void changeLcbInitialCalibrationStatusHandler(boolean isInitialCalibrationStatus) {
        //do nothing
    }

    @Override
    public void setLcbInitialHandler() {
        //do nothing
    }

    @Override
    public void getLcbStatusHandler(byte lcbstatus) {
        //do nothing
    }

    @Override
    public void getLcbErrorCodeHandler(int lcbErrorCode) {
        //do nothing
    }

    @Override
    public void returnErrorCodeToUIHandler(int lcbErrorCode) {
        //do nothing
    }

    @Override
    public void getLcbVersionHandler(int lcbVersion) {
        //do nothing
    }

    @Override
    public void setLcbCalibrateHandler() {
        //do nothing
    }

    @Override
    public void setLcbUpdateProgramHandler(byte byteRtnRsp) {
        //do nothing
    }

    @Override
    public void setLcbSkipCurrErrorHandler() {
        //do nothing
    }

    @Override
    public void setLcbSpeExtCmdHandler(byte byteCmd) {
        //do nothing
    }

    @Override
    public void getLcbDciVersionHandler(byte[] bytesDciLcbInfo) {
        //do nothing
    }

    @Override
    public void getLcbDciEnvHandler(byte[] bytesDciEnv) {
        //do nothing
    }

    @Override
    public void setLcbCmdEUPsModeHandler(byte byteRtn) {
        //do nothing
    }

    @Override
    public void setLcbCmdConsolePowerHandler() {
        //do nothing
    }

    @Override
    public void getLcbProtocolVersionHandler(int protocolVersion) {
        //do nothing
    }

    @Override
    public void setLcbCmdSerialNumberHandler(byte[] bytesDataReceived) {
        //do nothing
    }

    @Override
    public void setLcbTreadmillOperateHandler(TreadmillOperateRxData data) {
        // do nothing
    }

    @Override
    public void getLcbTreadmillCadenceHandler(byte[] bytesCadenceData) {
        // do nothing
    }

    @Override
    public void setLcbMotorSpeedRtnRPMHandler(int intRtnRPM) {
        //do nothing
    }

    @Override
    public void setLcbMotorRPMHandler() {
        //do nothing
    }

    @Override
    public void setLcbIncPWMStepHandler() {
        //do nothing
    }

    @Override
    public void setLcbDecPWMStepHandler() {
        //do nothing
    }

    @Override
    public void setLcbStopPWMStepHandler() {
        //do nothing
    }

    @Override
    public void setLcbInclineActionHandler() {
        //do nothing
    }

    @Override
    public void setLcbInclineLocHandler() {
        //do nothing
    }

    @Override
    public void setLcbOperatingStatusHandler() {
        //do nothing
    }

    @Override
    public void getLcbRollerRPMHandler(int intRPM) {
        //do nothing
    }

    @Override
    public void getLcbMotorRPMHandler(int intRPM) {
        //do nothing
    }

    @Override
    public void getOneShotLcbMotorRpmHandler(int intRPM) {
        //do nothing
    }

    @Override
    public void getLcbInclineLocHandler(int intLoc) {
        //do nothing
    }

    @Override
    public void setLcbTuneEndInclineHandler() {
        //do nothing
    }

    @Override
    public void setLcbTuneEndIncline2Handler() {
        //do nothing
    }

    @Override
    public void setLcbInclineStrokeHandler() {
        //do nothing
    }

    @Override
    public void setLcbCompensationVolHandler(int intRtnVol) {
        //do nothing
    }

    @Override
    public void setLcbMotorSpecificationHandler() {
        //do nothing
    }

    @Override
    public void getLcbMotorTypeHandler(byte byteType) {
        //do nothing
    }

    @Override
    public void getLcbDCIdataMaxLenHandler(byte byteLen) {
        //do nothing
    }


    @Override
    public void setLcbMotorPowerOffHandler() {
        //do nothing
    }

    @Override
    public void setLcbDCIforceInclineHandler() {
        //do nothing
    }

    @Override
    public void getLcbTreadmillInUsedHandler(byte byteStatus) {
        //do nothing
    }

    @Override
    public void getLcbMainMotorInfoHandler(byte[] bytesMainMotorInfo) {
        //do nothing
    }

    @Override
    public void getLcbTreadmillExtStatusHandler(int intValue) {
        //do nothing
    }

    @Override
    public void setLcbTreadmillEcbActionHandler(byte byteData) {

    }

    @Override
    public void getLcbDCIinclineRangeHandler(int intRange) {
        //do nothing
    }

    @Override
    public void setLcbDCIinclineRangeHandler() {
        //do nothing
    }

    @Override
    public void getLcbDCIrpmSpeedRangeHandler(int intRange) {
        //do nothing
    }

    @Override
    public void setLcbDCIrpmSpeedRangeHandler() {
        //do nothing
    }

    @Override
    public void setLcbUniversalHandler(int intRtnValue) {
        //do nothing
    }

    @Override
    public void setLcbDCIManualCalibrationHandler() {
        //do nothing
    }

    @Override
    public void setLcbDeviceDataHandler(byte[] bytesDataReceived) {
        //do nothing
    }

    @Override
    public void setLcbResetECBHandler() {
        //do nothing
    }

    @Override
    public void setLcbECBpositionHandler() {
        //do nothing
    }

    @Override
    public void getLcbECBrpmHandler(int intRpm) {
        if (mBikeHandler.mBikeHandlerCallback != null) {
            mBikeHandler.mBikeHandlerCallback.onBikeRPM(intRpm);
        }
    }

    @Override
    public void getOneShotLcbECBrpmHandler(int intRpm) {
        if (mBikeHandler.mBikeHandlerCallback != null) {
            mBikeHandler.mBikeHandlerCallback.onBikeOneShotRPM(intRpm);
        }
    }

    @Override
    public void getLcbECBadcHandler(int intAdc) {
        //do nothing
    }

    @Override
    public void setLcbPWMpercentHandler() {
        //do nothing
    }

    @Override
    public void getLcbBatteryVoltageHandler(int intVoltage) {
        //do nothing
    }

    @Override
    public void setLcbWattsHandler() {
        //do nothing
    }

    @Override
    public void setLcbRPMgearRatioHandler() {
        //do nothing
    }

    @Override
    public void setLcbGeneratorPolePairHandler() {
        //do nothing
    }

    @Override
    public void setLcbLimitRPMchargeHandler() {
        //do nothing
    }

    @Override
    public void setLcbLimitRPMresistanceHandler() {
        //do nothing
    }

    @Override
    public void setLcbMachineTypeHandler() {
        //do nothing
    }

    @Override
    public void setLcbPowerOffHandler() {
        //do nothing
    }

    @Override
    public void setLcbBatteryChargeLevelHandler() {
        //do nothing
    }

    @Override
    public void setLcbMinResistanceInclineWorkingHandler() {
        //do nothing
    }

    @Override
    public void setLcbInclinePercentHandler() {
        //do nothing
    }

    @Override
    public void getLcbInclinePercentHandler(int intValue) {
        //do nothing
    }

    @Override
    public void setLcbECBactionHandler() {
        //do nothing
    }

    @Override
    public void getLcbECBstatusHandler(byte byteStatus) {
        //do nothing
    }

    @Override
    public void getLcbECBcountHandler(int intValue) {
        //do nothing
    }

    @Override
    public void setLcbElectromagneticCurrentHandler() {
        //do nothing
    }

    @Override
    public void setLcbResistanceTypeHandler() {
        //do nothing
    }

    @Override
    public void setLcbBeginBatteryChargeHandler() {
        //do nothing
    }

    @Override
    public void getLcbBatteryCapacityHandler(int intValue) {
        //do nothing
    }

    @Override
    public void setLcbClimbmillStatusHandler() {
        //do nothing
    }

    @Override
    public void getLcbClimbmillStatusHandler(int intValue) {
        //do nothing
    }

    @Override
    public void setLcbEStopParameterHandler() {
        //do nothing
    }

    @Override
    public void getLcbBrakePressureValueHandler(int intValue) {
        //do nothing
    }

    @Override
    public void getLcbWattsHandler(int intValue) {
        //do nothing
    }

    @Override
    public void setLcbClimbmillCalHandler() {
        //do nothing
    }

    @Override
    public void setLcbRecordCurrentHandler() {
        //do nothing
    }

    @Override
    public void setLcbPWMHandler() {
        //do nothing
    }

    @Override
    public void setLcbFinishCalibratingHandler() {
        //do nothing
    }

    @Override
    public void setLcbSPMHandler() {
        //do nothing
    }

    @Override
    public void getLcbSPMHandler(int intValue) {
        //do nothing
    }

    @Override
    public void getOneShotLcbSpmHandler(int intValue) {
        //do nothing
    }

    @Override
    public void setLcbCalScalingHandler() {
        //do nothing
    }

    @Override
    public void getLcbCalScalingHandler(int intValue) {
        //do nothing
    }

    @Override
    public void setLcbFrameCalScalingHandler() {
        //do nothing
    }

    @Override
    public void getLcbFrameCalScalingHandler(int intValue) {
        //do nothing
    }

     @Override
    public void getLcbIndoorBikeSensorDataHandler(byte[] data) {
    	//do nothing
    }

     @Override
    public void getOneShotLcbIndoorBikeSensorDataHandler(byte[] data) {
    	//do nothing
    }


     @Override
    public void setLcbIndoorBikeStepperMotorPositionHandler() {
    	//do nothing
    }

     @Override
    public void setLcbIndoorBikeCalcParaOfDeviceHandler() {
    	//do nothing
    }
     @Override
    public void getLcbIndoorBikeCalcParaOfDeviceHandler(byte[] bytesData) {
    	//do nothing
    }

    @Override
    public void setLcbIndoorBikeActionSpinDownHandler(byte[] bytesDataReceived) {
    	//do nothing
    }

     @Override
    public void getLcbIndoorBikeInfoSpinDownHandler(IndoorBikeInfoSpinDown indoorBikeInfoSpinDown) {
    	//do nothing
    }

     @Override
    public void setLcbIndoorBikeErgModeSettingsHandler(byte[] bytesDataReceived)  {
    	//do nothing
    }

     @Override
    public void getLcbIndoorBikeErgModeSettingsHandler(byte[] bytesDataReceived)  {
    	//do nothing
    }

     @Override
    public void setLcbIndoorBikeResistanceLevelHandler() {
    	//do nothing
    }

    @Override
    public void setTreadMillRpmValueHandler(int intSetRpm) {
        //do nothing
    }

    @Override
    public void setTreadMillAdcValueHandler(int intSetAdc) {
        //do nothing
    }

    @Override
    public void deviceGetEEPromMemorySizesHandler(int intValue) {
        //do nothing
    }

    @Override
    public void deviceGetEEPromMemoryContentHandler(byte[] bytesMemoryContent) {
        //do nothing
    }

    @Override
    public void deviceGetECBCurrentHandler(byte[] bytesECBCurrent) {
        //do nothing
    }

    @Override
    public void deviceGetEStopValeHandler(byte[] bytesEStopVale) {
        //do nothing
    }

    @Override
    public void deviceGetDCBusStatusHandler(byte[] bytesDCBusStatus) {
        //do nothing
    }

    @Override
    public void deviceGetLCBversionHandler(byte[] bytesLCBversion) {
        //do nothing
    }

    @Override
    public void deviceGetControlZoneInfoHandler(byte[] bytesControlZoneInfo) {
        //do nothing
    }

    @Override
    public void deviceGetUpdateLcbMcuTypeHandler(byte[] bytesMcuType) {
        //do nothing
    }

    @Override
    public void deviceGetUpdateLcbModuleNameHandler(byte[] bytesModuleName) {
        //do nothing
    }

    @Override
    public void deviceGetUpdateLcbBrandHandler(byte[] bytesBrand) {
        //do nothing
    }

    @Override
    public void deviceGetUpdateLcbBootVersionHandler(byte[] bytesBootVersion) {
        //do nothing
    }


    @Override
    public void deviceGetUpdateBinAddrMcuTypeHandler(byte[] bytesMcuTypeAddr) {
        //do nothing
    }

    @Override
    public void deviceGetUpdateBinAddrModuleNameHandler(byte[] bytesModuleNameAddr) {
        //do nothing
    }

    @Override
    public void deviceGetUpdateBinAddrBrandHandler(byte[] bytesBrandAddr) {
        //do nothing
    }

    @Override
    public void deviceGetUpdateBinAddrBootVersionHandler(byte[] bytesBootVersionAddr) {
        //do nothing
    }

    @Override
    public void deviceGetUpdateMappingAddrBinToMcuHandler(byte[] bytesMappingAddr) {
        //do nothing
    }

    @Override
    public void deviceSoftwareResetHandler() {
        //do nothing
    }

    @Override
    public void deviceEraseLoaderMemoryHandler(byte byteStatus) {
        //do nothing
    }

    @Override
    public void deviceWriteWordLoaderDataHandler(byte byteStatus) {
        //do nothing
    }

    @Override
    public void deviceReadByteDataHandler(byte[] bytesStatusLenData) {
        //do nothing
    }

    @Override
    public void deviceSavePWMHandler(byte byteStatus) {
        //do nothing
    }

    @Override
    public void deviceReadPWMHandler(byte[] bytesData) {
        //do nothing
    }

    @Override
    public void deviceGetLCBinformationHandler(byte[] bytesLcbInfo) {
        //do nothing
    }

    @Override
    public void deviceSetMcbLcbParameterHandler(byte byteStatus) {
        //do nothing
    }

    @Override
    public void deviceGetMcbLcbParameterHandler(byte[] bytesTypeLenData) {
        //do nothing
    }

    @Override
    public void getIrAddrVersionHandler(int intIrAddrVersion) {
        //do nothing
    }

    @Override
    public void getIrAddrIrStatusHandler(byte byteStatus) {
        //do nothing
    }

    @Override
    public void setIrAddrCmdEUPsModeHandler(byte byteRtn) {
        //do nothing
    }

    @Override
    public void enterIrAddrErpModeHandler(boolean isEnterSuccess) {
        //do nothing
    }

    @Override
    public void leaveIrAddrErpModeHandler(boolean isLeaveSuccess) {
        //do nothing
    }

}
