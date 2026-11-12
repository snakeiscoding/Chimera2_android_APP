package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Treadmill;

import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;

public interface ITreadMillHandlerCallback {
    //get Motor RPM
    void onTreadMillRPM(int intMotorRPM);

    //get Motor Incline Location
    void onTreadMillInclineLoc(int intMotorInclineLoc);

    //get TreadMill InUsed
    void onTreadMillInUsed(byte byteStatus);

    //get TreadMill MotorInfo
    void onTreadMillMotorInfo(byte[] bytesMotorInfo);

    //Reply Treadmill Ext status
    void onTreadmillExtStatus(int intTreadmillExtStatus);

    //get TreadMill Operate
    void onTreadMillOperate(TreadmillOperateRxData data);

    //get TreadMill Cadence (SPM and Accumulate Steps)
    void onTreadMillCadence(byte[] bytesCadenceData);

    //TM- Service Switch 0:OFF 1:ON
    void onTreadMillServiceSwitch(boolean isServiceOn);

    //One Shot Reply TreadMill RPM
    void onTreadMillOneShotRPM(int intTreadMillRPM);

    //GUI get Treadmill speed set rpm value on writing serial port
    void onTreadMillLcbSetRpmValue(int intSetRpm);

    //GUI get Treadmill incline set adc value on writing serial port
    void onTreadMillLcbSetAdcValue(int intSetAdc);

    //get Hybrid TM IR Addr version
    void onHybridTmIrAddrVersion(int intIrAddrVersion);

    //get Hybrid TM IR Addr Ir Status
    void onHybridTmIrAddrIrStatus(int intStatus);

    //get Hybrid TM IR Addr ERP Rtn byte
    void onHybridTmIrAddrErpRtnByte(byte byteRtn);

    //get Hybrid TM IR Addr enter ERP Rtn succes/fail
    void onHybridTmIrAddrEnterErpMode(boolean isEnterSuccess);
    //get Hybrid TM IR Addr leave ERP Rtn succes/fail
    void onHybridTmIrAddrLeaveErpMode(boolean isLeaveSuccess);
}
