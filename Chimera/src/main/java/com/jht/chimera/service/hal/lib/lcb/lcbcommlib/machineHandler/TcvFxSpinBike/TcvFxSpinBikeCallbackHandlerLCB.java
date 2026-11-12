package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.TcvFxSpinBike;

import android.util.Log;

import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.Constants;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataReceived;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbCommReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeInfoSpinDown;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TcvFxSpinBikeCallbackHandlerLCB implements ILcbCommReceive, IDeviceDataReceived {
    private static final String TAG = "TcvFxSpinBikeCallbackHandlerLCB";

    private TcvFxSpinBikeHandler mTcvFxSpinBikeHandler;

    public TcvFxSpinBikeCallbackHandlerLCB(TcvFxSpinBikeHandler tcvFxSpinBikeHandler) {
        mTcvFxSpinBikeHandler = tcvFxSpinBikeHandler;
    }

    @Override
    public void bootUpLeaveLcbErpHandler(boolean isBootUpLeaveErpSuccess) { }

    @Override
    public void enterLcbErpModeHandler(boolean isEnterSuccess) { }

    @Override
    public void leaveLcbErpModeHandler(boolean isLcbCommOk, boolean isLeaveSuccess) { }

    @Override
    public void changeLcbInclineMotorDownwardStatusHandler(boolean isLcbDownwardStatus) { }

    @Override
    public void changeLcbInclineMotorUpwardStatusHandler(boolean isLcbUpwardStatus) { }

    @Override
    public void changeLcbMainMotorStatusHandler(boolean isMainMotorStatus) { }

    @Override
    public void changeLcbEncoderRPMStatusHandler(boolean isEncoderRPMStatus) { }

    @Override
    public void changeLcbBatteryCapacityStatusHandler(boolean isBatteryCapacityStatus) { }

    @Override
    public void changeLcbCommandErrorStatusHandler(boolean isCommandErrorStatus) { }

    @Override
    public void changeLcbLcbErrorStatusDetectedHandler(boolean isLcbErrorStatus) { }

    @Override
    public void changeLcbInitialCalibrationStatusHandler(boolean isInitialCalibrationStatus) { }

    @Override
    public void setLcbInitialHandler() { }

    @Override
    public void getLcbStatusHandler(byte lcbstatus) { }

    @Override
    public void getLcbErrorCodeHandler(int lcbErrorCode) { }

    @Override
    public void returnErrorCodeToUIHandler(int lcbErrorCode) { }

    @Override
    public void getLcbVersionHandler(int lcbVersion) { }

    @Override
    public void setLcbCalibrateHandler() { }

    @Override
    public void setLcbUpdateProgramHandler(byte byteRtnRsp) { }

    @Override
    public void setLcbSkipCurrErrorHandler() { }

    @Override
    public void setLcbSpeExtCmdHandler(byte byteCmd) { }

    @Override
    public void getLcbDciVersionHandler(byte[] bytesDciLcbInfo) { }

    @Override
    public void getLcbDciEnvHandler(byte[] bytesDciEnv) { }

    @Override
    public void setLcbCmdEUPsModeHandler(byte byteRtn) { }

    @Override
    public void setLcbCmdConsolePowerHandler() { }

    @Override
    public void getLcbProtocolVersionHandler(int protocolVersion) { }

    @Override
    public void setLcbCmdSerialNumberHandler(byte[] bytesDataReceived) { }

    @Override
    public void setLcbTreadmillOperateHandler(TreadmillOperateRxData data) { }

    @Override
    public void getLcbTreadmillCadenceHandler(byte[] bytesCadenceData) { }

    @Override
    public void setLcbMotorSpeedRtnRPMHandler(int intRtnRPM) { }

    @Override
    public void setLcbMotorRPMHandler() { }

    @Override
    public void setLcbIncPWMStepHandler() { }

    @Override
    public void setLcbDecPWMStepHandler() { }

    @Override
    public void setLcbStopPWMStepHandler() { }

    @Override
    public void setLcbInclineActionHandler() { }

    @Override
    public void setLcbInclineLocHandler() { }

    @Override
    public void setLcbOperatingStatusHandler() { }

    @Override
    public void getLcbRollerRPMHandler(int intRPM) { }

    @Override
    public void getLcbMotorRPMHandler(int intRPM) { }

    @Override
    public void getOneShotLcbMotorRpmHandler(int intRPM) { }

    @Override
    public void getLcbInclineLocHandler(int intLoc) { }

    @Override
    public void setLcbTuneEndInclineHandler() { }

    @Override
    public void setLcbTuneEndIncline2Handler() { }

    @Override
    public void setLcbInclineStrokeHandler() { }

    @Override
    public void setLcbCompensationVolHandler(int intRtnVol) { }

    @Override
    public void setLcbMotorSpecificationHandler() { }

    @Override
    public void getLcbMotorTypeHandler(byte byteType) { }

    @Override
    public void getLcbDCIdataMaxLenHandler(byte byteLen) { }

    @Override
    public void setLcbMotorPowerOffHandler() { }

    @Override
    public void setLcbDCIforceInclineHandler() { }

    @Override
    public void getLcbTreadmillInUsedHandler(byte byteStatus) { }

    @Override
    public void getLcbMainMotorInfoHandler(byte[] bytesMainMotorInfo) { }

    @Override
    public void getLcbTreadmillExtStatusHandler(int intValue) { }

    @Override
    public void setLcbTreadmillEcbActionHandler(byte byteData) { }

    @Override
    public void getLcbDCIinclineRangeHandler(int intRange) { }

    @Override
    public void setLcbDCIinclineRangeHandler() { }

    @Override
    public void getLcbDCIrpmSpeedRangeHandler(int intRange) { }

    @Override
    public void setLcbDCIrpmSpeedRangeHandler() { }

    @Override
    public void setLcbUniversalHandler(int intRtnValue) { }

    @Override
    public void setLcbDCIManualCalibrationHandler() { }

    @Override
    public void setLcbDeviceDataHandler(byte[] bytesDataReceived) { }

    @Override
    public void setLcbResetECBHandler() { }

    @Override
    public void setLcbECBpositionHandler() { }

    @Override
    public void getLcbECBrpmHandler(int intRpm) { }

    @Override
    public void getOneShotLcbECBrpmHandler(int intRpm) { }

    @Override
    public void getLcbECBadcHandler(int intAdc) { }

    @Override
    public void setLcbPWMpercentHandler() { }

    @Override
    public void getLcbBatteryVoltageHandler(int intVoltage) { }

    @Override
    public void setLcbWattsHandler() { }

    @Override
    public void setLcbRPMgearRatioHandler() { }

    @Override
    public void setLcbGeneratorPolePairHandler() { }

    @Override
    public void setLcbLimitRPMchargeHandler() { }

    @Override
    public void setLcbLimitRPMresistanceHandler() { }

    @Override
    public void setLcbMachineTypeHandler() { }

    @Override
    public void setLcbPowerOffHandler() { }

    @Override
    public void setLcbBatteryChargeLevelHandler() { }

    @Override
    public void setLcbMinResistanceInclineWorkingHandler() { }

    @Override
    public void setLcbInclinePercentHandler() { }

    @Override
    public void getLcbInclinePercentHandler(int intValue) { }

    @Override
    public void setLcbECBactionHandler() { }

    @Override
    public void getLcbECBstatusHandler(byte byteStatus) { }

    @Override
    public void getLcbECBcountHandler(int intValue) { }

    @Override
    public void setLcbElectromagneticCurrentHandler() { }

    @Override
    public void setLcbResistanceTypeHandler() { }

    @Override
    public void setLcbBeginBatteryChargeHandler() { }

    @Override
    public void getLcbBatteryCapacityHandler(int intValue) { }

    @Override
    public void setLcbClimbmillStatusHandler() { }

    @Override
    public void getLcbClimbmillStatusHandler(int intValue) { }

    @Override
    public void setLcbEStopParameterHandler() { }

    @Override
    public void getLcbBrakePressureValueHandler(int intValue) { }

    @Override
    public void getLcbWattsHandler(int intValue) { }

    @Override
    public void setLcbClimbmillCalHandler() { }

    @Override
    public void setLcbRecordCurrentHandler() { }

    @Override
    public void setLcbPWMHandler() { }

    @Override
    public void setLcbFinishCalibratingHandler() { }

    @Override
    public void setLcbSPMHandler() { }

    @Override
    public void getLcbSPMHandler(int intValue) { }

    @Override
    public void getOneShotLcbSpmHandler(int intValue) { }

    @Override
    public void setLcbCalScalingHandler() { }

    @Override
    public void getLcbCalScalingHandler(int intValue) { }

    @Override
    public void setLcbFrameCalScalingHandler() { }

    @Override
    public void getLcbFrameCalScalingHandler(int intValue) { }

    @Override
    public void getLcbIndoorBikeSensorDataHandler(byte[] data) {
        TcvFxSpinBikeSensorData parsed = parse(data);
        if (parsed == null) return;
        ITcvFxSpinBikeHandlerCallback cb = mTcvFxSpinBikeHandler.getCallback();
        if (cb == null) return;
        cb.onTcvFxSpinBikeSensorData(parsed);
        // Dispatch resistance before RPM so machineData.resistanceLevel is set before
        // LCBService.onRpm triggers updateMachineData() (which publishes DATA_RESISTANCE_INT).
        cb.onTcvFxSpinBikeResistanceLevel(parsed.resistanceLevel);
        cb.onTcvFxSpinBikeRPM(parsed.crankRpm);
        cb.onTcvFxSpinBikeWatts(parsed.watts);
    }

    @Override
    public void getOneShotLcbIndoorBikeSensorDataHandler(byte[] data) {
        TcvFxSpinBikeSensorData parsed = parse(data);
        if (parsed == null) return;
        ITcvFxSpinBikeHandlerCallback cb = mTcvFxSpinBikeHandler.getCallback();
        if (cb == null) return;
        cb.onTcvFxSpinBikeResistanceLevel(parsed.resistanceLevel);
        cb.onTcvFxSpinBikeRPM(parsed.crankRpm);
        cb.onTcvFxSpinBikeWatts(parsed.watts);
    }

    // Parse the 0x51 payload for LCB_TCV_FX (0x22). Returns null for non-TCV-FX frames
    // (owned by IndoorBikeCallbackHandlerLCB) or short/malformed frames.
    //
    // Layout (big-endian, fixed 11 bytes):
    //   [0]    lcbType            (u8, must be 0x22)
    //   [1]    machineType        (u8)
    //   [2-3]  crankRpm           (s16)
    //   [4-5]  flywheelRpm        (s16)
    //   [6-7]  ecbPositionSensor  (s16)
    //   [8]    resistanceLevel    (u8)
    //   [9-10] watts              (s16, raw 0.1W units; conversion to W done in TcvFxSpinBikeInterface)
    private TcvFxSpinBikeSensorData parse(byte[] data) {
        if (data == null || data.length == 0) return null;
        if (DataUtils.unsignedByteToInt(data[0]) != LcbType.LCB_TCV_FX.Value) return null;
        if (data.length < Constants.RECEIVE_DATA_SIZE_TCV_FX_SPINBIKE_SENSORDATA) {
            Log.w(TAG, "TCV-FX SensorData short frame: len=" + data.length);
            return null;
        }

        TcvFxSpinBikeSensorData d = new TcvFxSpinBikeSensorData();
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.BIG_ENDIAN);
        d.lcbType = DataUtils.unsignedByteToInt(buf.get());
        d.machineType = DataUtils.unsignedByteToInt(buf.get());
        d.crankRpm = DataUtils.shortToInt(buf.getShort());
        d.flywheelRpm = DataUtils.shortToInt(buf.getShort());
        d.ecbPositionSensor = DataUtils.shortToInt(buf.getShort());
        d.resistanceLevel = DataUtils.unsignedByteToInt(buf.get());
        d.watts = DataUtils.shortToInt(buf.getShort());
        return d;
    }

    @Override
    public void setLcbIndoorBikeStepperMotorPositionHandler() { }

    @Override
    public void setLcbIndoorBikeCalcParaOfDeviceHandler() { }

    @Override
    public void getLcbIndoorBikeCalcParaOfDeviceHandler(byte[] bytesData) { }

    @Override
    public void setLcbIndoorBikeActionSpinDownHandler(byte[] bytesDataReceived) { }

    @Override
    public void getLcbIndoorBikeInfoSpinDownHandler(IndoorBikeInfoSpinDown indoorBikeInfoSpinDown) { }

    @Override
    public void setLcbIndoorBikeErgModeSettingsHandler(byte[] bytesDataReceived) { }

    @Override
    public void getLcbIndoorBikeErgModeSettingsHandler(byte[] bytesData) { }

    @Override
    public void setLcbIndoorBikeResistanceLevelHandler() { }

    @Override
    public void setTreadMillRpmValueHandler(int intSetRpm) { }

    @Override
    public void setTreadMillAdcValueHandler(int intSetAdc) { }

    @Override
    public void getIrAddrVersionHandler(int intIrAddrVersion) {

    }

    @Override
    public void getIrAddrIrStatusHandler(byte byteStatus) {

    }

    @Override
    public void setIrAddrCmdEUPsModeHandler(byte byteRtn) {

    }

    @Override
    public void enterIrAddrErpModeHandler(boolean isEnterSuccess) {

    }

    @Override
    public void leaveIrAddrErpModeHandler(boolean isLeaveSuccess) {

    }

    @Override
    public void deviceGetEEPromMemorySizesHandler(int intValue) { }

    @Override
    public void deviceGetEEPromMemoryContentHandler(byte[] bytesMemoryContent) { }

    @Override
    public void deviceGetECBCurrentHandler(byte[] bytesECBCurrent) { }

    @Override
    public void deviceGetEStopValeHandler(byte[] bytesEStopVale) { }

    @Override
    public void deviceGetDCBusStatusHandler(byte[] bytesDCBusStatus) { }

    @Override
    public void deviceGetLCBversionHandler(byte[] bytesLCBversion) { }

    @Override
    public void deviceGetControlZoneInfoHandler(byte[] bytesControlZoneInfo) { }

    @Override
    public void deviceGetUpdateLcbMcuTypeHandler(byte[] bytesMcuType) { }

    @Override
    public void deviceGetUpdateLcbModuleNameHandler(byte[] bytesModuleName) { }

    @Override
    public void deviceGetUpdateLcbBrandHandler(byte[] bytesBrand) { }

    @Override
    public void deviceGetUpdateLcbBootVersionHandler(byte[] bytesBootVersion) { }

    @Override
    public void deviceGetUpdateBinAddrMcuTypeHandler(byte[] bytesMcuTypeAddr) { }

    @Override
    public void deviceGetUpdateBinAddrModuleNameHandler(byte[] bytesModuleNameAddr) { }

    @Override
    public void deviceGetUpdateBinAddrBrandHandler(byte[] bytesBrandAddr) { }

    @Override
    public void deviceGetUpdateBinAddrBootVersionHandler(byte[] bytesBootVersionAddr) { }

    @Override
    public void deviceGetUpdateMappingAddrBinToMcuHandler(byte[] bytesMappingAddr) { }

    @Override
    public void deviceSoftwareResetHandler() { }

    @Override
    public void deviceEraseLoaderMemoryHandler(byte byteStatus) { }

    @Override
    public void deviceWriteWordLoaderDataHandler(byte byteStatus) { }

    @Override
    public void deviceReadByteDataHandler(byte[] bytesStatusLenData) { }

    @Override
    public void deviceSavePWMHandler(byte byteStatus) { }

    @Override
    public void deviceReadPWMHandler(byte[] bytesData) { }

    @Override
    public void deviceGetLCBinformationHandler(byte[] bytesLcbInfo) { }

    @Override
    public void deviceSetMcbLcbParameterHandler(byte byteStatus) { }

    @Override
    public void deviceGetMcbLcbParameterHandler(byte[] bytesTypeLenData) { }
}
