package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler;

import static com.jht.chimera.service.hal.lib.machine.Machine.validateCalPoints;

import android.util.Log;

import com.jht.androidcommonalgorithms.util.Strings;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.AdvancedLogging;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.Constants;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IDeviceDataReceived;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.ILcbCommReceive;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.IUniDataReceived;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike.IndoorBikeInfoSpinDown;

import java.nio.charset.Charset;

public class GeneralCallbackHandlerLCB implements ILcbCommReceive, IDeviceDataReceived, IUniDataReceived{
    private static final String TAG = "GeneralCallbackHandlerLCB";

    private GeneralHandler mGeneralHandler;
    private AdvancedLogging mAdvancedLogging = AdvancedLogging.getInstance();

//    private static int failToEnterErp = 0;
//    private LcbType machineLcbType = LcbType.LCB_UNKNOWN;

    public GeneralCallbackHandlerLCB(GeneralHandler generalHandler) {
        mGeneralHandler = generalHandler;
    }

    @Override
    public void bootUpLeaveLcbErpHandler(boolean isBootUpLeaveErpSuccess) {
        Log.i(TAG, "bootUpLeaveLcbErpHandler = " + isBootUpLeaveErpSuccess);
        if (isBootUpLeaveErpSuccess) {
            Log.i(TAG, "Boot up leave Lcb Erp Mode success. ");
//		mGeneralHandler.bootUpLeaveLcbErpSuccess();
        }
    }

    @Override
    public void enterLcbErpModeHandler(boolean isEnterSuccess) {
/*        Log.i(TAG, "enterLcbErpModeHandler = " + isEnterSuccess);
        if (isEnterSuccess) {
        } else {
            // try again
            ++failToEnterErp;
            if (failToEnterErp < 3) {
                if (mGeneralHandler.mLcb != null) mGeneralHandler.generalHandlerEnterLcbErpMode();
            } else {
                failToEnterErp = 0;
            }
        }    
*/
    }

    @Override
    public void leaveLcbErpModeHandler(boolean isLcbCommOk, boolean isLeaveSuccess) {
/*        Log.i(TAG, "leaveLcbErpModeHandler = " + isLcbCommOk + " | " + isLeaveSuccess);
            if (isLcbCommOk && isLeaveSuccess) {
	        Log.i(TAG, "Leave Lcb Erp Mode success. ");
            } else {
                  // try again
                  mGeneralHandler.generalHandlerLeaveLcbErpMode();
            }
*/
    }

    @Override
    public void changeLcbInclineMotorDownwardStatusHandler(boolean isLcbDownwardStatus) {
        if (mGeneralHandler.mGeneralHandlerCallback != null) {
            mGeneralHandler.mGeneralHandlerCallback.onInclineMotorDownwardStatus(isLcbDownwardStatus);
        }
    }

    @Override
    public void changeLcbInclineMotorUpwardStatusHandler(boolean isLcbUpwardStatus) {
        if (mGeneralHandler.mGeneralHandlerCallback != null) {
            mGeneralHandler.mGeneralHandlerCallback.onInclineMotorUpwardStatus(isLcbUpwardStatus);
        }
    }

    @Override
    public void changeLcbMainMotorStatusHandler(boolean isMainMotorStatus) {
        if (mGeneralHandler.mGeneralHandlerCallback != null) {
            mGeneralHandler.mGeneralHandlerCallback.onMainMotorStatus(isMainMotorStatus);
        }
    }

    @Override
    public void changeLcbEncoderRPMStatusHandler(boolean isEncoderRPMStatus) {
        if (mGeneralHandler.mGeneralHandlerCallback != null) {
            mGeneralHandler.mGeneralHandlerCallback.onEncoderRPMStatus(isEncoderRPMStatus);
        }
    }


    @Override
    public void changeLcbBatteryCapacityStatusHandler(boolean isBatteryCapacityStatus) {
        if (mGeneralHandler.mGeneralHandlerCallback != null) {
            mGeneralHandler.mGeneralHandlerCallback.onBatteryCapacityStatus(isBatteryCapacityStatus);
        }
    }

    @Override
    public void changeLcbCommandErrorStatusHandler(boolean isCommandErrorStatus) {
        if (mGeneralHandler.mGeneralHandlerCallback != null) {
            mGeneralHandler.mGeneralHandlerCallback.onCommandErrorStatus(isCommandErrorStatus);
        }
    }

    @Override
    public void changeLcbLcbErrorStatusDetectedHandler(boolean isLcbErrorStatus) {
        if (isLcbErrorStatus && !mGeneralHandler.checkInGetErrorState()) {
            mGeneralHandler.setGetLcbErrorState(GeneralHandler.GetLcbErrorState.LCB_ERROR_GET);
            mGeneralHandler.runGetErrorProcesses();
		    if(mGeneralHandler.mSkipErrorStage){//Before initial skip error code
				mGeneralHandler.mInitialHaveError = true;
		    }
        }

    }

    @Override
    public void changeLcbInitialCalibrationStatusHandler(boolean isInitialCalibrationStatus) {
        if (mGeneralHandler.mGeneralHandlerCallback != null) {
            mGeneralHandler.mGeneralHandlerCallback.onInitialCalibrationStatus(isInitialCalibrationStatus);
        }
    }

    @Override
    public void setLcbInitialHandler() {
        if (mGeneralHandler.getInitialLcbState() == GeneralHandler.InitialLcbState.LCB_INIT_TM_3_CMDS_INITIAL) {
            mGeneralHandler.mInitialCmdDone = true;
            mGeneralHandler.setInitialLcbState(GeneralHandler.InitialLcbState.LCB_INIT_CMD_OPERATING_STOP);
        } else if (mGeneralHandler.getInitialLcbState() == GeneralHandler.InitialLcbState.LCB_INIT_CMD_INITIAL) {
//        if (mGeneralHandler.getInitialLcbState() == GeneralHandler.InitialLcbState.LCB_INIT_CMD_INITIAL) {
            mGeneralHandler.mInitialCmdDone = true;
            mGeneralHandler.setInitialLcbState(GeneralHandler.InitialLcbState.LCB_INIT_CMD_OPERATING_STOP);
            mGeneralHandler.autoInitialGeneralHandlerLCB();
        } else if (mGeneralHandler.getInitialLcbState() == GeneralHandler.InitialLcbState.LCB_INIT_DEFAULT_CMD_INITIAL) {
            mGeneralHandler.mDefaultInitialCmdDone = true;
            mGeneralHandler.setInitialLcbState(GeneralHandler.InitialLcbState.LCB_INIT_FINISH);
            mGeneralHandler.autoInitialGeneralHandlerLCB();
        }
    }

    @Override
    public void getLcbStatusHandler(byte lcbstatus) {
        //do nothing
        mGeneralHandler.mGeneralHandlerCallback.onLcbStatus(lcbstatus);
    }

    @Override
    public void getLcbErrorCodeHandler(int lcbErrorCode) {
        //Log.i(TAG, String.format("GeneralCallbackHandlerLCB getLcbErrorCodeHandler lcbErrorCode =%X ", lcbErrorCode));


        if (lcbErrorCode == 0x04B0) {
            ;//Log.d(TAG, "getLcbErrorCodeHandler: Error 04B0 occurred.");
        } else {
            //handle exception errorcode
            mGeneralHandler.handleExceptionErrorCode(lcbErrorCode);

            //skip error code
            if (mGeneralHandler.mSafeKeyStopMachine == true) {//Safe key signal Stop Machine trigger 02B2 doesn't SkipCurrError
                mGeneralHandler.mSafeKeyStopMachine = false;
            } else {
                if (mGeneralHandler.getGetLcbErrorState() == GeneralHandler.GetLcbErrorState.LCB_ERROR_GET) {
                    mGeneralHandler.setGetLcbErrorState(GeneralHandler.GetLcbErrorState.LCB_ERROR_SKIP);
                    mGeneralHandler.runGetErrorProcesses();
                }
            }

        }


    }

    @Override
    public void returnErrorCodeToUIHandler(int lcbErrorCode) {
        // callback to UI show
	    if(mGeneralHandler.mSkipErrorStage && mGeneralHandler.mInitialHaveError){ //Before initial skip error code
			mGeneralHandler.mInitialHaveError = false;
			return;
	    }

        if (lcbErrorCode == 0x02B2) { // error code 0x02B2 continue callback
            if (mGeneralHandler.mGeneralHandlerCallback != null) {
                mGeneralHandler.lastErrorCode = lcbErrorCode;
                mGeneralHandler.mGeneralHandlerCallback.onErrorCode(lcbErrorCode);
            }
        } else {
            //don't check lastErrorCode , let UI to check
            //if ((mGeneralHandler.mGeneralHandlerCallback != null) && (mGeneralHandler.lastErrorCode != lcbErrorCode)) {
            mGeneralHandler.lastErrorCode = lcbErrorCode;
            mGeneralHandler.mGeneralHandlerCallback.onErrorCode(lcbErrorCode);
            //}
        }
    }

    @Override
    public void getLcbVersionHandler(int lcbVersion) {

        if (lcbVersion == Constants.RTN_HAVE_0X50_DATA) {
            Log.v(TAG, "getLcbVersion rtn 0xFF00, LCB will support command 0x50/ Data 0xF0 function .");
            mGeneralHandler.autoDeviceGetLcbInformation();
        } else {
            byte lcbType = DataUtils.byteGetNthOfInt(lcbVersion, 1);
            if (mGeneralHandler.mGeneralHandlerCallback != null) {
                mGeneralHandler.mGeneralHandlerCallback.onLcbType(lcbType);
            }
            //set Lcb Type and Category
            mGeneralHandler.setMachineLcbType(lcbType);

            byte lcbVer = DataUtils.byteGetNthOfInt(lcbVersion, 0);
            if (mGeneralHandler.mGeneralHandlerCallback != null) {
                mGeneralHandler.mGeneralHandlerCallback.onLcbVersion(lcbVer & 0xff, -1);
            }

            if (!mGeneralHandler.checkInitialFinish()) {
                //process initial LCB
                mGeneralHandler.procInitialLcb();
            }

            //LCB support command 0x50/ Data 0x06 function .
            if(((lcbType & 0xA0) == 0xA0) ||((lcbType & 0xF0) == 0xF0)) {//only support Johnson MAB 0xAX and 0xFx lcb type
                mGeneralHandler.getDeviceGetLCBversion();
            }
			
        }

    }

    @Override
    public void setLcbCalibrateHandler() {
        //do nothing
    }

    @Override
    public void setLcbUpdateProgramHandler(byte byteRtnRsp) {
        //do nothing
        if (byteRtnRsp == (byte) 0x59) {
            mGeneralHandler.mGeneralHandlerCallback.onIntoLcbUpdateMode(true);
        } else {
            mGeneralHandler.mGeneralHandlerCallback.onIntoLcbUpdateMode(false);
        }
    }

    @Override
    public void setLcbSkipCurrErrorHandler() {
        if (mGeneralHandler.getGetLcbErrorState() == GeneralHandler.GetLcbErrorState.LCB_ERROR_SKIP) {
            mGeneralHandler.setGetLcbErrorState(GeneralHandler.GetLcbErrorState.LCB_ERROR_NONE);
            mGeneralHandler.runGetErrorProcesses();
        }
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
        if (bytesDataReceived.length < 3) //old lcb not support 0x7D will replay 0x00
            return;

        byte handlingWay = bytesDataReceived[0];
        byte handlingStatus = bytesDataReceived[1];
        byte snLength = bytesDataReceived[2];
        String sn = "";
        int snMaxLength = 0;

        if (mGeneralHandler.mGeneralHandlerCallback != null) {

            if (handlingWay == (byte) 0x00) {//Erase serial number
                if (handlingStatus == (byte) 0x00) {//success/none
                    mGeneralHandler.mGeneralHandlerCallback.onClearLcbSnStatus(true);
                } else if (handlingStatus == (byte) 0x01) {//failure(EEPROM failure or other)
                    Log.v(TAG, "Erase lcb serial number, failure(EEPROM failure or other) .");
                    mGeneralHandler.mGeneralHandlerCallback.onClearLcbSnStatus(false);
                } else if (handlingStatus == (byte) 0x02) {//failure(EEPROM failure or other)
                    Log.v(TAG, "Erase lcb serial number, space is not enough .");
                    mGeneralHandler.mGeneralHandlerCallback.onClearLcbSnStatus(false);
                }
            } else if (handlingWay == (byte) 0x01) {//Write serial number
                if (handlingStatus == (byte) 0x00) {//success/none
                    mGeneralHandler.mGeneralHandlerCallback.onSetLcbSnStatus(true);
                } else if (handlingStatus == (byte) 0x01) {//failure(EEPROM failure or other)
                    Log.v(TAG, "Write lcb serial number, failure(EEPROM failure or other) .");
                    mGeneralHandler.mGeneralHandlerCallback.onSetLcbSnStatus(false);
                } else if (handlingStatus == (byte) 0x02) {//failure(EEPROM failure or other)
                    Log.v(TAG, "Write lcb serial number, space is not enough .");
                    mGeneralHandler.mGeneralHandlerCallback.onSetLcbSnStatus(false);
                }
            } else if ((handlingWay == (byte) 0x02)     //Read serial number
                    || (handlingWay == (byte) 0x04)) {//Read LCB/MCB serial number

                if (handlingStatus == (byte) 0x00) {//success/none
                    int snLen = DataUtils.unsignedByteToInt(snLength);
                    if (snLen != 0) {
                        StringBuilder sb = new StringBuilder();
                        int serialNumberOffset = 3;
                        for (int i = 0; i < snLen; i++) {
                            if (bytesDataReceived[i + serialNumberOffset] == 0) {
                                Log.w(TAG, "Skipping null character in LCB serial number response!");
                                continue;
                            }
                            sb.append((char) (bytesDataReceived[i + serialNumberOffset] & 0xFF));
                        }
                        sn = sb.toString();
                        mGeneralHandler.mGeneralHandlerCallback.onGetLcbSerialNumber(true, sn);
                    } else {//snLen == 0
                        mGeneralHandler.mGeneralHandlerCallback.onGetLcbSerialNumber(true, "");
                    }

                } else if (handlingStatus == (byte) 0x01) {//failure(EEPROM failure or other)
                    Log.v(TAG, "Read lcb serial number, failure(EEPROM failure or other) .");
                    mGeneralHandler.mGeneralHandlerCallback.onGetLcbSerialNumber(false, "");
                } else if (handlingStatus == (byte) 0x02) {//failure(EEPROM failure or other)
                    Log.v(TAG, "Read lcb serial number, space is not enough .");
                    mGeneralHandler.mGeneralHandlerCallback.onGetLcbSerialNumber(false, "");
                }

            } else if (handlingWay == (byte) 0x03) {//Get max length
                if (handlingStatus == (byte) 0x00) {//success/none
                    int snLen = DataUtils.unsignedByteToInt(snLength);
                    snMaxLength = snLen;
                    mGeneralHandler.mGeneralHandlerCallback.onGetLcbSnMaxLength(true, snMaxLength);
                } else if (handlingStatus == (byte) 0x01) {//failure(EEPROM failure or other)
                    Log.v(TAG, "Get max length lcb serial number, failure(EEPROM failure or other) .");
                    mGeneralHandler.mGeneralHandlerCallback.onGetLcbSnMaxLength(false, 0);
                } else if (handlingStatus == (byte) 0x02) {//failure(EEPROM failure or other)
                    Log.v(TAG, "Get max length lcb serial number, space is not enough .");
                    mGeneralHandler.mGeneralHandlerCallback.onGetLcbSnMaxLength(false, 0);
                }

            }

        }

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
        if (mGeneralHandler.getInitialLcbState() == GeneralHandler.InitialLcbState.LCB_INIT_CMD_OPERATING_STOP) {
            mGeneralHandler.setInitialLcbState(GeneralHandler.InitialLcbState.LCB_INIT_CMD_OPERATING_ACTION);
//            mGeneralHandler.autoInitialGeneralHandlerLCB();
        } else if (mGeneralHandler.getInitialLcbState() == GeneralHandler.InitialLcbState.LCB_INIT_CMD_OPERATING_ACTION) {
            mGeneralHandler.mOperatingCmdDone = true;
            mGeneralHandler.setInitialLcbState(GeneralHandler.InitialLcbState.LCB_INIT_FINISH);
            mGeneralHandler.autoInitialGeneralHandlerLCB();
        }
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
    public void getLcbECBrpmHandler(int intRPM) {
        //do nothing
    }

    @Override
    public void getOneShotLcbECBrpmHandler(int intRPM) {
        //do nothing
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
        if (mGeneralHandler.mGeneralHandlerCallback != null) {
            mGeneralHandler.mGeneralHandlerCallback.onBatteryVoltage(intVoltage);
        }
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
        Log.d(TAG, "lcb_version: " + Strings.bytesToHex(bytesLCBversion));
        //Johnson VAVE fx LCB : 01 08 50 09 06 aa aa bb bb cc cc xx xx  crc
        //bytesLCBversion data aa aa bb bb cc cc xx xx , aa aa => lcb major lcb version , bb bb => minor lcb version

        if(bytesLCBversion.length == 8){
            //major lcb version
            int majorVersion = DataUtils.unsignedBytesToInt(bytesLCBversion[1],bytesLCBversion[0]);
            //minor lcb version
            int minorVersion = DataUtils.unsignedBytesToInt(bytesLCBversion[3],bytesLCBversion[2]);

            if ((mGeneralHandler.mGeneralHandlerCallback != null) && (majorVersion!=0) && (minorVersion!=0)) {
                mGeneralHandler.mGeneralHandlerCallback.onLcbVersion(majorVersion, minorVersion);
            }
        }
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
    static FrameConfig mConfig = FrameConfigManager.getInstance().getFrameConfig();
    @Override
    public void deviceReadPWMHandler(byte[] bytesData) {
        int[] data = {0, 0};
        data[0] = bytesData[0] & 0xff;
        data[1] = bytesData[1] & 0xff;
        int pwm = 0;
        pwm = data[0];
        pwm <<= 8;
        pwm += data[1];
        mConfig.inductionCalPoints[mGeneralHandler.calPoint] = pwm;
        Log.i(TAG, "read lcb pwm (" + mGeneralHandler.calPoint + ") = " + mConfig.inductionCalPoints[mGeneralHandler.calPoint]);
        mGeneralHandler.calPoint++;
        if(mGeneralHandler.calPoint >= 5) {
            mGeneralHandler.calPoint = 0;
            mConfig.hasInductionCal = 1;
            byte result = validateCalPoints();
            if(result == 1)
                mConfig.validInductionCal = 1;
        }
    }

    @Override
    public void deviceGetLCBinformationHandler(byte[] bytesLcbInfo) {
        Log.d(TAG, "lcb_version: " + Strings.bytesToHex(bytesLcbInfo));
        //JIS 1x Bike 01 08 50 05 F0 14 01 02 02 49  , 0x14 => lcb type 0x02 => major lcb version
		if(bytesLcbInfo.length == 4){
	        byte lcbType = bytesLcbInfo[0];
	        if (mGeneralHandler.mGeneralHandlerCallback != null) {
	            mGeneralHandler.mGeneralHandlerCallback.onLcbType(lcbType);
	        }
	        //set Lcb Type and Category
	        mGeneralHandler.setMachineLcbType(lcbType);

	        if (mGeneralHandler.mGeneralHandlerCallback != null) {
	            mGeneralHandler.mGeneralHandlerCallback.onLcbVersion(bytesLcbInfo[2] & 0xff, -1);
	        }
		}else{
			byte lcbType = bytesLcbInfo[1];
            if (mGeneralHandler.mGeneralHandlerCallback != null ) {
                    mGeneralHandler.mGeneralHandlerCallback.onLcbType(lcbType);
            }
			//set Lcb Type and Category
			mGeneralHandler.setMachineLcbType(lcbType);

            if (mGeneralHandler.mGeneralHandlerCallback != null ) {
                if(lcbType == 0x1B || lcbType == 0x1D) { // can we just check data length or something less LCB specific?
                    mGeneralHandler.mGeneralHandlerCallback.onLcbVersion(bytesLcbInfo[3] & 0xff, bytesLcbInfo[4] & 0xff);
                }
                else {
                    mGeneralHandler.mGeneralHandlerCallback.onLcbVersion(bytesLcbInfo[3] & 0xff, -1);
                }
            }
		}

        if (!mGeneralHandler.checkInitialFinish()) {
            //process initial LCB
            mGeneralHandler.procInitialLcb();
        }
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
    public void uniGetMotoTypeHandler(int intMotoTypes) {
            if (mGeneralHandler.mGeneralHandlerCallback != null) {
                mGeneralHandler.mGeneralHandlerCallback.onMcbMotoType(intMotoTypes);
            }
    }

    @Override
    public void uniSetPFCmodeHandler(byte[] bytesData) {
        //do nothing
    }

    @Override
    public void uniMagnetAngleStateHandler(byte[] bytesData) {

		if (!((bytesData[0] == (byte) 0xFF) &&(bytesData[1] == (byte) 0xFF))){
	            mGeneralHandler.setAutoReplyGeneralMagnetAngle(false);
		        if (mGeneralHandler.mGeneralHandlerCallback != null) {
			    int intRtnValue = DataUtils.unsignedBytesToInt(bytesData[1],bytesData[0]);
		            mGeneralHandler.mGeneralHandlerCallback.onLcbFinishMagnetAngleValue(intRtnValue);
		        }
		}
    }

    @Override
    public void uniMcbVoltageHandler(byte[] bytesData) {
            if (mGeneralHandler.mGeneralHandlerCallback != null) {
	        int intRtnValue = DataUtils.unsignedByteToInt(bytesData[0]);
                mGeneralHandler.mGeneralHandlerCallback.onGetMcbVoltageValue(intRtnValue);
            }
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
