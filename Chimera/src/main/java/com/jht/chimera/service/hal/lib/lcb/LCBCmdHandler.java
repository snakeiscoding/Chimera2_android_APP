package com.jht.chimera.service.hal.lib.lcb;

import android.util.Log;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.LCBInterface;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.LCBMessageType;

public class LCBCmdHandler {

    private static final String TAG = "LCBCmdHandler";
    LCBInterface lcb;
    FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

    public LCBCmdHandler(LCBInterface lcb) {
        this.lcb = lcb;
    }

    public void setTreadmillRPM(int rpm) {
        lcb.setTreadmillRPM(rpm);
    }

    public void setSPM(double spm) {
        lcb.setSPM(spm);
    }

    public void setRawIncline(int incline) {

        if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Ascent) {
            //incline (%) = 0 ~ 32768
            lcb.setAscentIncline(incline);
        } else {

            if(!LCBInfo.getMCBLockState())
                lcb.setTreadMillIncline(incline);
            else
                Log.d(TAG, "LCB Safety : Lock state, don't set incline.");
        }
    }

    public void setTreadmillOperate(int treadmillRpm, int inclineLocation) {
        //TODO: Does this need to a safety check like setRawIncline does?
        lcb.setTreadmillOperate(treadmillRpm, inclineLocation);
    }

    public void setAscentIncline(int incline) {
        lcb.setAscentIncline(incline);
    }

    public void setErgWatts(Integer value) {
        Log.d (TAG, "target watts=" + value);
        lcb.setErgWatts (value);
    }

    public void setRawResistance(int resistance) {

	lcb.setRawResistance(resistance);
    }

    void turnOffLcb(int delayMinutes) {
        lcb.turnOffLcb(delayMinutes);
    }

    //	void displayLCMTestPattern();
    public boolean selfPower() {
        return LCBInfo._status.selfPower;
    }

    public boolean personInPlace() {
        return true;
    }

    public void startMachine() {

        LCBInfo._startMachine = true;

        //unlock machine
        lcb.unlockMachine();
    }

    public void stopMachine() {
        lcb.stopMachine();
        LCBInfo._startMachine = false;
    }

    public void resetTreadmill() {
        lcb.setTreadmillRPM(0);
        lcb.setTreadMillIncline(frameConfig.INCLINE_ADC_MIN);
        Log.i(TAG, "LCB Safety : reset incline "+frameConfig.INCLINE_ADC_MIN);
    }

    //  For TM Incline Calibration
    public void setTmCalibrateInclineUp() { lcb.setTmInclineActionUp(); }

    public void setTmCalibrateInclineDown() {
        lcb.setTmInclineActionDown();
    }

    public void stopTmCalibrateIncline() {
        lcb.setTmInclineActionUp();
    }

    public void replyTmCalibrationInclineADC(boolean on_off) {
        lcb.setTreadmillAutoReplyIncline(on_off);
    }

    public void autoAscentCalibrateIncline() {
        lcb.autoAscentCalibrateIncline();
    }

    public void stopAscentCalibrate() {
        lcb.stopAscentCalibrate();
    }

    public void stopInclineAction() {
        lcb.stopinclineAction();
    }

    public void calibrateClimbmill(double kg, int seconds, int spm) {
    }

    public void stopClimbmillCalibration() {
    }

    public void setEnClimbmillNoPersonOnOff(boolean On) {
        lcb.setClimbmillNoPersonOnOff(On);
    }

    public void setmEnClimbmillControlZoneOn(boolean on) {
        lcb.setmClimbmillControlZoneOn(on);
    }

    public void replyEnClimbCZcapaIRfreq(boolean on) {
        lcb.setAutoReplyClimbmillCZcapaIRfreq(on);
    }

    public void setTreadmillTreadSenseOnOff(boolean on) {

        lcb.setTreadMillNoPersonDetect(on);
    }

    public void onTmSafetykeyMachineStop() {
        lcb.eStopMachineStop();
    }

    public void onTmSafetykeyUnlock() {
        lcb.onTmSafetykeyUnlock();
    }

    //Need MCU Support
    /*
     *       Received safetykey release event from MCU
     *       LCB can't detect safetykey release
     * */
    public void onMCUTmSafetykeyReleae() {
        lcb.onTmSafetykeyUnlock();
    }

    public void onEnClimbmillSafetykeyUnlock() {
        lcb.setClimbmillCommand(LCBMessageType.E_STOP_UNLOCK);
    }

    public void stopAllLcbCommunication() {
        //stop & unbind lcb machine interface
        lcb.unbindPreviousInterface();

        //stop & unregister general heart beat and lcb communication
        lcb.stopGeneralHandler();

    }

    public void modifyWriteLcbSerialNumber(String writeConsoleSN, String writeFrameSN) {
        String modified_sn = "";
        String consol_sn = writeConsoleSN;
        String frame_sn = writeFrameSN;
        if (isValidSerialNumber(consol_sn) && isValidSerialNumber(frame_sn)) {
            modified_sn = consol_sn + "/" + frame_sn;
            lcb.setILcbSerialNumber(modified_sn);
        }
    }

    private boolean isValidSerialNumber(String sn) {
        if (sn == null) {
            return false;
        } else if (sn.length() < 9 || sn.length() > 20) {
            return false;
        } else if (sn.isEmpty()) {
            return false;
        }
        return true;
    }

    public void setTreadMillMotorTypeValue(int motorTypeValue) {
        if(motorTypeValue > 0) {
            lcb.setTreadMillMotorType(motorTypeValue);
        }
    }

    public void replySpinBikeInfoSpinDown(boolean on) {
        lcb.setAutoReplySpinBikeInfoSpinDown(on);
    }

    public void setSpinBikeSpinDownOnOff(boolean On) {
        lcb.setSpinBikeSpinDownOnOff(On);
    }

    public void requestRawAdc(int index) { lcb.requestRawAdc(index); }

    public void setSpinBikeClearTorque() {
        lcb.setSpinBikeClearTorque();
    }

    public void setSpinBikeCorrectionFactor(float factor) {
        lcb.setSpinBikeCorrectionFactor(factor);
    }

    public void setSpinBikeInertia(float inertia) {
        lcb.setSpinBikeInertia(inertia);
    }

    public void getSpinBikeRaptorPara() {
        lcb.getSpinBikeRaptorPara();
    }

    public void setSpinBikeRaptorPara(String strRaptorPara) {
        lcb.setSpinBikeRaptorPara(strRaptorPara);
    }

    public void erpStopUartPort() {
        //stop lcb communication uart port
        lcb.erpStopUartPort();
    }

    public void setErp(boolean enterErp) {
        lcb.setErp(enterErp);
    }

    public void setTM548EcbActionPowerOFF() {
         if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
		    byte byteECBPowerOff = lcb.setTM548EcbPowerOff();
		    lcb.setTM548EcbStatusAction(byteECBPowerOff);
	    }
    }

    public void setTM548MESEcbRunRelease() {
        if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
            //TM548 control ECB release
            byte byteECBWorkoutRelease = lcb.setTM548EcbWorkoutRelease();
            lcb.setTM548EcbStatusAction(byteECBWorkoutRelease);
        }
    }

    public void setTM548MESEcbRunBrake() {
        if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
            //TM548 control ECB brake
            byte byteECBWorkoutBrake = lcb.setTM548EcbWorkoutBrake();
            lcb.setTM548EcbStatusAction(byteECBWorkoutBrake);
        }
    }

    public void getHybridTMIrAddrVersion() {
        lcb.getHybridTMIrAddrVersion();
    }

    public void setHybridTMIrAddrEnterErpMode() {
        lcb.setHybridTMIrAddrEnterErpMode();
    }

    public void setHybridTMIrAddrLeaveErpMode() {
        lcb.setHybridTMIrAddrLeaveErpMode();
    }

    public void onDestroy() {
    }
}
