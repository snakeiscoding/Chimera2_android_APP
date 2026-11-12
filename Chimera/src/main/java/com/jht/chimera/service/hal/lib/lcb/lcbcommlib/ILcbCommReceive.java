package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeInfoSpinDown;
public interface ILcbCommReceive {
    // triggered by boot up check lcb erp response of message 0x7A leave ERP
    void bootUpLeaveLcbErpHandler(boolean isBootUpLeaveErpSuccess);
    // triggered by response of message 0x7A enter ERP mode
    void enterLcbErpModeHandler(boolean isEnterSuccess);
    // triggered by response of message 0x7A leave ERP mode
    void leaveLcbErpModeHandler(boolean isLcbCommOk, boolean isLeaveSuccess);
    // triggered by lcb status byte filter FILTER_LCB_STATUS_B0_BIT_INCLINE_DOWN_ACTION 
    // 0: stop 1:action
    void changeLcbInclineMotorDownwardStatusHandler(boolean isLcbDownwardStatus);
    // triggered by lcb status byte filter FILTER_LCB_STATUS_B1_BIT_INCLINE_UP_ACTION
    // 0: stop 1:action
    void changeLcbInclineMotorUpwardStatusHandler(boolean isLcbUpwardStatus);
    // triggered by lcb status byte filter FILTER_LCB_STATUS_B2_BIT_MAIN_MOTOR_ACTION
    // 0: stop 1:action
    void changeLcbMainMotorStatusHandler(boolean isMainMotorStatus);
    // triggered by lcb status byte filter FILTER_LCB_STATUS_B3_BIT_POWER_STATUS_AC
    //This bit will be to indicate the power source when the control board is an AC / DC (self-powered) system for bike series.
    //  1 : AC 0: DC
    void changeLcbEncoderRPMStatusHandler(boolean isEncoderRPMStatus);
    // triggered by lcb status byte filter FILTER_LCB_STATUS_B4_BIT_BATTERY_CAP_NOT_ENOUGH
    // battery capacity.
    //  0:enougt 1:not enough
    void changeLcbBatteryCapacityStatusHandler(boolean isBatteryCapacityStatus);
    // triggered by lcb status byte filter FILTER_LCB_STATUS_B5_BIT_COMMAND_ERROR
    // command error.
    //  0:correct 1:error
    void changeLcbCommandErrorStatusHandler(boolean isCommandErrorStatus);
    // triggered by lcb status byte filter FILTER_LCB_STATUS_B6_BIT_LCB_ERROR
    // Lcb error.
    //  0:correct 1:error
    void changeLcbLcbErrorStatusDetectedHandler(boolean isLcbErrorStatus);
    // triggered by lcb status byte filter FILTER_LCB_STATUS_B7_BIT_CALIBRATION_ACTION
    // initial (calibration )status.
    //  0:initial already 1:action
    void changeLcbInitialCalibrationStatusHandler(boolean isInitialCalibrationStatus);
    // triggered by response of command 0x70 do nothing
    void setLcbInitialHandler();
    // triggered by response of command 0x71 
    void getLcbStatusHandler(byte lcbstatus);
    // triggered by response of command 0x72 and 04B0 procCommLcbErrorHandler
    //this callback is for error code occur (not callback to UI)
    void getLcbErrorCodeHandler(int lcbErrorCode);
    //this callback is for all error code occur callback to UI
    void returnErrorCodeToUIHandler(int lcbErrorCode);
    // triggered by response of command 0x73
    void getLcbVersionHandler(int lcbVersion);
    // triggered by response of command 0x74 do nothing
    void setLcbCalibrateHandler();
    // triggered by response of command 0x75 
    void setLcbUpdateProgramHandler(byte byteRtnRsp);
    // triggered by response of command 0x76 do nothing
    void setLcbSkipCurrErrorHandler();
    // triggered by response of command 0x77 
    void setLcbSpeExtCmdHandler(byte byteCmd);
    // triggered by response of command 0x78 DCI LCB info
    void getLcbDciVersionHandler(byte[] bytesDciLcbInfo);
    // triggered by response of command 0x79 DCI Env
    void getLcbDciEnvHandler(byte[] bytesDciEnv);
    // triggered by response of command 0x7A
    void setLcbCmdEUPsModeHandler(byte byteRtn);
    // triggered by response of command 0x7B
    void setLcbCmdConsolePowerHandler();
    // triggered by response of command 0x7C
    void getLcbProtocolVersionHandler(int protocolVersion);
    // triggered by response of command 0x7D
    void setLcbCmdSerialNumberHandler(byte[] bytesDataReceived);
    // triggered by response of command 0x7E
    void setLcbTreadmillOperateHandler(TreadmillOperateRxData data);
    // triggered by response of command 0x7F
    void getLcbTreadmillCadenceHandler(byte[] bytesCadenceData);
    // triggered by response of command 0xF0
    void setLcbMotorSpeedRtnRPMHandler(int intRtnRPM);
    // triggered by response of command 0xF1 
    void setLcbMotorRPMHandler();
    // triggered by response of command 0xF2
    void setLcbIncPWMStepHandler();
    // triggered by response of command 0xF3
    void setLcbDecPWMStepHandler();
    // triggered by response of command 0xF4
    void setLcbStopPWMStepHandler();
    // triggered by response of command 0xF5
    void setLcbInclineActionHandler();
    // triggered by response of command 0xF6
    void setLcbInclineLocHandler();
    // triggered by response of command 0xF7
    void setLcbOperatingStatusHandler();
    // triggered by response of command 0xF8
    void getLcbRollerRPMHandler(int intRPM);
    // triggered by response of command 0xF9
    void getLcbMotorRPMHandler(int intRPM);
    void getOneShotLcbMotorRpmHandler(int intRpm);
    // triggered by response of command 0xFA
    void getLcbInclineLocHandler(int intLoc);
    // triggered by response of command 0xFB 
    void setLcbTuneEndInclineHandler();
    // triggered by response of command 0xFC
    void setLcbTuneEndIncline2Handler();
    // triggered by response of command 0xFD
    void setLcbInclineStrokeHandler();
    // triggered by response of command 0xFE
    void setLcbCompensationVolHandler(int intRtnVol);
    // triggered by response of command 0x90 
    void setLcbMotorSpecificationHandler();
    // triggered by response of command 0x91
    void getLcbMotorTypeHandler(byte byteType);
    // triggered by response of command 0x92
    void getLcbDCIdataMaxLenHandler(byte byteLen);
    // triggered by response of command 0x93
    void setLcbMotorPowerOffHandler();
    // triggered by response of command 0x94
    void setLcbDCIforceInclineHandler();
    // triggered by response of command 0x95
    void getLcbTreadmillInUsedHandler(byte byteStatus);
    // triggered by response of command 0x96 MainMotorInfo
    void getLcbMainMotorInfoHandler(byte[] bytesMainMotorInfo);
    // triggered by response of command 0x97
    //Get the status of the LCB for Treadmill Ext
    void getLcbTreadmillExtStatusHandler(int intValue);
    // triggered by response of command 0x98 
    void setLcbTreadmillEcbActionHandler(byte byteData);
    // triggered by response of command 0xA0
    void getLcbDCIinclineRangeHandler(int intRange);
    // triggered by response of command 0xA1 
    void setLcbDCIinclineRangeHandler();
    // triggered by response of command 0xA6
    void getLcbDCIrpmSpeedRangeHandler(int intRange);
    // triggered by response of command 0xA7 
    void setLcbDCIrpmSpeedRangeHandler();
    // triggered by response of command 0xAA
    void setLcbUniversalHandler(int intRtnValue);
    // triggered by response of command 0xAB
    void setLcbDCIManualCalibrationHandler();
    // triggered by response of command 0x50
    void setLcbDeviceDataHandler(byte[] bytesDataReceived);
    // triggered by response of command 0x61
    void setLcbResetECBHandler();
    // triggered by response of command 0x62 
    void setLcbECBpositionHandler();
    // triggered by response of command 0x63
    void getLcbECBrpmHandler(int intRpm);
    void getOneShotLcbECBrpmHandler(int intRpm);
    // triggered by response of command 0x64
    void getLcbECBadcHandler(int intAdc);
    // triggered by response of command 0x65 
    void setLcbPWMpercentHandler();
    // triggered by response of command 0x66
    //Get battery capacity data 0.1V
    void getLcbBatteryVoltageHandler(int intVoltage);
    // triggered by response of command 0x67 
    void setLcbWattsHandler();
    // triggered by response of command 0x68
    void setLcbRPMgearRatioHandler();
    // triggered by response of command 0x69
    void setLcbGeneratorPolePairHandler();
    // triggered by response of command 0x6A
    void setLcbLimitRPMchargeHandler();
    // triggered by response of command 0x6B
    void setLcbLimitRPMresistanceHandler();
    // triggered by response of command 0x6C
    void setLcbMachineTypeHandler();
    // triggered by response of command 0x6D
    void setLcbPowerOffHandler();
    // triggered by response of command 0x6E
    void setLcbBatteryChargeLevelHandler();
    // triggered by response of command 0x6F
    void setLcbMinResistanceInclineWorkingHandler();
    // triggered by response of command 0x80
    void setLcbInclinePercentHandler();
    // triggered by response of command 0x81
    //Get incline travel speed ( 0: 0% 32768: 100% )
    void getLcbInclinePercentHandler(int intValue);
    // triggered by response of command 0x82
    void setLcbECBactionHandler();
    // triggered by response of command 0x83
    void getLcbECBstatusHandler(byte byteStatus);
    // triggered by response of command 0x84
    //Get ECB count 0~65535
    void getLcbECBcountHandler(int intValue);
    // triggered by response of command 0x85
    void setLcbElectromagneticCurrentHandler();
    // triggered by response of command 0x86
    void setLcbResistanceTypeHandler();
    // triggered by response of command 0x87
    void setLcbBeginBatteryChargeHandler();
    // triggered by response of command 0x88
    //Get BatteryCapacity 0 ~ 100 %
    void getLcbBatteryCapacityHandler(int intValue);
    // triggered by response of command 0x8A
    void setLcbClimbmillStatusHandler();
    // triggered by response of command 0x8B
    //Get the status of the LCB for climbmill
    void getLcbClimbmillStatusHandler(int intValue);
    // triggered by response of command 0x8C
    void setLcbEStopParameterHandler();
    // triggered by response of command 0x8D
    //Get brake pressure value
    void getLcbBrakePressureValueHandler(int intValue);
    // triggered by response of command 0x8E
    //Get Watts
    void getLcbWattsHandler(int intValue);
    // triggered by response of command 0x8F
    void setLcbClimbmillCalHandler();
    // triggered by response of command 0x47
    void setLcbRecordCurrentHandler();
    // triggered by response of command 0x48
    void setLcbPWMHandler();
    // triggered by response of command 0x49
    void setLcbFinishCalibratingHandler();
    // triggered by response of command 0x4A
    void setLcbSPMHandler();
    // triggered by response of command 0x4B
    //Get SPM
    void getLcbSPMHandler(int intValue);
    void getOneShotLcbSpmHandler(int intValue);
    // triggered by response of command 0x4C
    void setLcbCalScalingHandler();
    // triggered by response of command 0x4D
    //Get CalScaling
    void getLcbCalScalingHandler(int intValue);
    // triggered by response of command 0x4E
    void setLcbFrameCalScalingHandler();
    // triggered by response of command 0x4F
    //Get CalScaling
    void getLcbFrameCalScalingHandler(int intValue);
    // triggered by response of command 0x51
    //Get Sensor Data
    void getLcbIndoorBikeSensorDataHandler(byte[] data);
    void getOneShotLcbIndoorBikeSensorDataHandler(byte[] data);
    // triggered by response of command 0x52
    void setLcbIndoorBikeStepperMotorPositionHandler();
    // triggered by response of command 0x53
    //Set calculation parameters of device or algorithm	
    void setLcbIndoorBikeCalcParaOfDeviceHandler();
    // triggered by response of command 0x54
    //Get calculation parameters of device or algorithm
    void getLcbIndoorBikeCalcParaOfDeviceHandler(byte[] bytesData);
    // triggered by response of command 0x55
    // Set the action of spin down 
    void setLcbIndoorBikeActionSpinDownHandler(byte[] bytesDataReceived);
    // triggered by response of command 0x56
    //Get the info of spin down
    void getLcbIndoorBikeInfoSpinDownHandler(IndoorBikeInfoSpinDown indoorBikeInfoSpinDown);
    // triggered by response of command 0x57
    // Set Erg Mode Settings 
    void setLcbIndoorBikeErgModeSettingsHandler(byte[] bytesDataReceived);
    // triggered by response of command 0x59
    // Get Erg Mode Settings 
    void getLcbIndoorBikeErgModeSettingsHandler(byte[] bytesDataReceived);
    // triggered by response of command 0x58
    void setLcbIndoorBikeResistanceLevelHandler();
	
    // triggered by Treadmill speed set rpm value on writing serial port
    void setTreadMillRpmValueHandler(int intSetRpm);
    // triggered by Treadmill incline set adc value on writing serial port
    void setTreadMillAdcValueHandler(int intSetAdc);
    // triggered by response of IR address 0x26 command 0x73
    void getIrAddrVersionHandler(int intIrAddrVersion);

    // triggered by response of IR address 0x26 command 0x99
    void getIrAddrIrStatusHandler(byte byteStatus);

    void setIrAddrCmdEUPsModeHandler(byte byteRtn);
    void enterIrAddrErpModeHandler(boolean isEnterSuccess);
    void leaveIrAddrErpModeHandler(boolean isLeaveSuccess);

}
