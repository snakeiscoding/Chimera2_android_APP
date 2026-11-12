package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;

/**
 * Created by samlu on 2018/3/22.
 */

public interface ILCBCallback {

    //LCB error code
    void onErrorCode(int lcbErrorCode);
    //LCB type
    void onLcbType(byte lcbType);
    //LCB version
    void onLcbVersion(int major, int minor);
    //LCB initial process finished
    void onLcbInitialFinish();
    //LCB serial number
    void onReadLcbSerialNumber(String stringLcbSN);

    //Status byte callback ==>
    //Motor Incline Downward
    void onInclineMotorDownwardStatus(boolean motorDownwardStatus);
    //Motor Incline Upward
    void onInclineMotorUpwardStatus(boolean motorUpwardStatus);
    //Motor Incline Upward
    void onMainMotorStatus(boolean motorStatus);
    //Encoder RPM Status 1 : AC 0: DC
    void onEncoderRPMStatus(boolean encoderRPMStatus);
    // battery capacity 0:enougt 1:not enough
    void onBatteryCapacityStatus(boolean batteryCapacityStatus);
    // command Error Status 0:correct 1:error
    void onCommandErrorStatus(boolean commandErrorStatus);
    // initial calibration status 0:initial already 1:action
    void onInitialCalibrationStatus(boolean initialCalibrationStatus);
    //<== Status byte callback

    //Battery Voltage
    void onBatteryVoltage(int intBatteryVoltage);

    //LCB Status
    void onLcbStatus(byte lcbstatus);

    //MCB Status for Treadmill
    void onLcbTMStatus(byte lcbstatus);

    /* Workout */
    void onRpm(int rpm);
    void onOneShotRpm(int rpm);

    // SpinBike
    void onWatts (int watts);

    void onResistanceLevel(int resistanceLevel);

    void onSpm(int spm);

    void onInclineADC(int inclineADC);
    void onRawADC(int adc);

    void onPersonInPlace(boolean personInPlace);

    void onShowRawSensorDataString(String stringSensorData);

    /* Teeadmill */
    void onTreadMillMotorInfo(byte[] bytesMotorInfo);

    void onTreadMillOperate(TreadmillOperateRxData data);

    void onTreadMillCadence(byte[] bytesCadenceData);

    //Lock by error code rule
    void onLCBSafetykeyLockedCode(boolean locked);

    //Lock by mcb status
    void onTreadmillSafetykeyLockState(boolean lockState);

    void onTreadmillStartUnlock();

    void onTMautoOCState(boolean detectState);

    void onTreadmillMagnetAngleValue(int value);
    void onTreadmillMotorTypeValue(int value);

    void onTreadmillMcbVoltageValue(int value);

    void onTreadmillOSstopWorkout(TreadMillInterface.OverSpeedRule.OverSpeedType overSpeedType);
    void onTreadmillOSreduceIncline(TreadMillInterface.OverSpeedRule.OverSpeedType overSpeedType, float fReduceIncline);
    /* ENClimbmill */
    void onEnClimbmillSafetykeyPulled(boolean mast, boolean hand);

    void onEnClimbmillStartUnlock();

    void onEnClimbmillCZ(boolean cz);

    void onEnClimbmillIR(boolean ir);

    void onEnClimbMillWatts(int watts);

    void onEnClimbmillCalibraton(boolean calibrating);

    void onEnclimbmillCZcapaIRfreq(int cz, int ir);

    void onEnClimbMillCalScaling(int intEnClimbMillCalScaling);

    void onEnClimbMillFrameCalScaling(int intEnClimbMillFrameCalScaling);

    void onEnClimbmillStartControl(boolean value);

    /* Spin Bike */
    void onSpinBikeInfoSpinDown(int spinDownStatus , int spinDownRPM , float spinDownTorqueScalar, float spinDownCorrectionFactor, float spinDownInertia);
    void onSpinBikeRaptorPara(byte[] bytesRaptorPara);

    void onTmRollerGuard(boolean triggered);

    //Ir Addr board version
    void onHybridTmIrAddrVersion(int major);

    //Ir Addr board Ir Status
    void onHybridTmIrAddrIrStatus(int byteIrAddrIrStatus);

    //Ir Addr board ERP return byte
    void onHybridTmIrAddrErpRtnByte(byte byteRtn);

    //Ir Addr board Enter ERP is Success
    void onHybridTmIrAddrEnterErpMode(boolean isEnterSuccess);

    //Ir Addr board Leave ERP is Success
    void onHybridTmIrAddrLeaveErpMode(boolean isLeaveSuccess);

    //Ir Addr board Leave ERP is Success
    void onHybridTmIrAddrIrEStop(boolean triggered);

}
