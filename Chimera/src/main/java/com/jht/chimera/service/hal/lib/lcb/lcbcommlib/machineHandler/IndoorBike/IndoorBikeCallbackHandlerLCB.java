package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike;

import android.util.Log;

import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.Constants;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataReceived;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbCommReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IndoorBikeCallbackHandlerLCB implements ILcbCommReceive , IDeviceDataReceived  {
    private static final String TAG = "IndoorBikeCallbackHandlerLCB";
	
    private IndoorBikeHandler mIndoorBikeHandler;

    public IndoorBikeCallbackHandlerLCB(IndoorBikeHandler indoorBikeHandler) {
	mIndoorBikeHandler = indoorBikeHandler;
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
    	// ICR70 uses this to get load cell ADC (do we need an LCB type check here or elsewhere?)
        if (mIndoorBikeHandler.mIndoorBikeHandlerCallback != null ) {
            if (mIndoorBikeHandler.isIcr()) mIndoorBikeHandler.mIndoorBikeHandlerCallback.onIndoorBikeRawAdc(intRange);
        }
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
         if (mIndoorBikeHandler.mIndoorBikeHandlerCallback != null) {
             mIndoorBikeHandler.mIndoorBikeHandlerCallback.onIndoorBikeRPM(intRpm);
         }
    }

     @Override
    public void getOneShotLcbECBrpmHandler(int intRpm) {
         if (mIndoorBikeHandler.mIndoorBikeHandlerCallback != null) {
             mIndoorBikeHandler.mIndoorBikeHandlerCallback.onIndoorBikeOneShotRPM(intRpm);
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
        if (mIndoorBikeHandler.mIndoorBikeHandlerCallback != null ) {
            mIndoorBikeHandler.mIndoorBikeHandlerCallback.onIndoorBikeWatts(intValue);
        }
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
        IndoorBikeSensorData parsed = parse(data);
        if (parsed == null) return;
        if (mIndoorBikeHandler.mIndoorBikeHandlerCallback != null) {
            mIndoorBikeHandler.mIndoorBikeHandlerCallback.onIndoorBikeSensorData(parsed);
        }
    }

    @Override
    public void getOneShotLcbIndoorBikeSensorDataHandler(byte[] data) {
        IndoorBikeSensorData parsed = parse(data);
        if (parsed == null) return;
        if (mIndoorBikeHandler.mIndoorBikeHandlerCallback != null) {
            mIndoorBikeHandler.mIndoorBikeHandlerCallback.onIndoorBikeOneShotSensorData(parsed);
        }
    }

    // Layout (big-endian, 19 bytes when fully populated):
    //   [0]   lcbType        (u8, discarded  used only for filtering)
    //   [1]   machineType    (u8, discarded)
    //   [2-3] crankRpm       (s16)
    //   [4]   crankRpmDirection (u8)
    //   [5-6] flywheelRpm    (s16)
    //   [7-8] watts          (s16, 0.1W units -> /10 to get watts)
    //   [9-10]  stepperMotorStatus   (s16)
    //   [11]    stepperMotorStepMode (u8)
    //   [12-14] stepperMotorPosition (24-bit little-endian)
    //   [15-17] stepperMotorRange    (24-bit little-endian)
    //   [18]    resistanceLevel      (u8)
    private IndoorBikeSensorData parse(byte[] data) {
        if (data == null || data.length == 0) return null;
        if (DataUtils.unsignedByteToInt(data[0]) == LcbType.LCB_TCV_FX.Value) return null;

        IndoorBikeSensorData d = new IndoorBikeSensorData();
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.BIG_ENDIAN);

        if (data.length < 2) return d;
        buf.get();  // lcbType
        buf.get();  // machineType

        if (data.length < 4) return d;
        d.crankRpm = DataUtils.shortToInt(buf.getShort());

        if (data.length < 5) return d;
        d.crankRpmDirection = DataUtils.unsignedByteToInt(buf.get());

        if (data.length < 7) return d;
        d.flywheelRpm = DataUtils.shortToInt(buf.getShort());

        if (data.length < 9) return d;
        d.watts = DataUtils.shortToInt(buf.getShort()) / 10;  // 0.1W -> W

        if (data.length < 11) return d;
        d.stepperMotorStatus = DataUtils.shortToInt(buf.getShort());

        if (data.length < 12) return d;
        d.stepperMotorStepMode = DataUtils.unsignedByteToInt(buf.get());

        if (data.length < 15) return d;
        byte[] pos = new byte[3];
        pos[0] = buf.get();
        pos[1] = buf.get();
        pos[2] = buf.get();
        d.stepperMotorPosition = DataUtils.unsignedBytesToInt(pos[2], pos[1], pos[0], (byte) 0x00);

        if (data.length < 18) return d;
        byte[] rng = new byte[3];
        rng[0] = buf.get();
        rng[1] = buf.get();
        rng[2] = buf.get();
        d.stepperMotorRange = DataUtils.unsignedBytesToInt(rng[2], rng[1], rng[0], (byte) 0x00);

        if (data.length < Constants.RECEIVE_DATA_SIZE_INDOORBIKE_SENSORDATA) {
            Log.w(TAG, "IndoorBike SensorData short frame: len=" + data.length);
            return d;
        }
        d.resistanceLevel = DataUtils.unsignedByteToInt(buf.get());
        return d;
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
        if (mIndoorBikeHandler.mIndoorBikeHandlerCallback != null ) {
                mIndoorBikeHandler.mIndoorBikeHandlerCallback.onIndoorBikeGetCalcParaOfDevice(bytesData);
        }
    }
	
    @Override
    public void setLcbIndoorBikeActionSpinDownHandler(byte[] bytesDataReceived) {
        if (mIndoorBikeHandler.mIndoorBikeHandlerCallback != null ) {
                mIndoorBikeHandler.mIndoorBikeHandlerCallback.onIndoorBikeSetActionSpinDown(bytesDataReceived);
        }
    }

     @Override
    public void getLcbIndoorBikeInfoSpinDownHandler(IndoorBikeInfoSpinDown indoorBikeInfoSpinDown) {
        if (mIndoorBikeHandler.mIndoorBikeHandlerCallback != null ) {
                mIndoorBikeHandler.mIndoorBikeHandlerCallback.onIndoorBikeInfoSpinDown(indoorBikeInfoSpinDown);
        }
    }

    @Override
    public void setLcbIndoorBikeErgModeSettingsHandler(byte[] bytesDataReceived) {
        if (mIndoorBikeHandler.mIndoorBikeHandlerCallback != null ) {
                mIndoorBikeHandler.mIndoorBikeHandlerCallback.onIndoorBikeSetErgModeSettings(bytesDataReceived);
        }
    }

     @Override
    public void getLcbIndoorBikeErgModeSettingsHandler(byte[] bytesData) {
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
