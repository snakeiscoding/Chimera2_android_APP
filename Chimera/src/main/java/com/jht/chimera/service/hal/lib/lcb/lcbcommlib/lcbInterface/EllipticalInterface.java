package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Elliptical.EllipticalHandler;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Elliptical.IEllipticalHandlerCallback;

public class EllipticalInterface implements MachineInterfaceBase {

    private static final String TAG = "LCBInterface";
    private ILCBCallback serviceCallback;
    private EllipticalHandler mEpHandler;

    public EllipticalInterface(final ILCBCallback callback) {
        mEpHandler = EllipticalHandler.getInstance();
        this.serviceCallback = callback;
        mEpHandler.setEllipticalHandlerCallback(mEpHandlerCallback);
        mEpHandler.initialElliptical();
    }

    private IEllipticalHandlerCallback mEpHandlerCallback = new IEllipticalHandlerCallback() {

        @Override
        public void onEllipticalRPM(int intEllipticalRPM) {
            serviceCallback.onRpm(intEllipticalRPM);
        }

        @Override
        public void onEllipticalOneShotRPM(int intEllipticalRPM) {
            serviceCallback.onOneShotRpm(intEllipticalRPM);
        }

    };

    @Override
    public void startReplyData(boolean start) {
        mEpHandler.setAutoReplyEllipticalRpm(start);
    }

    @Override
    public void unInitial() {
        mEpHandler.unsetEllipticalHandlerCallback();
        mEpHandler.unInitialElliptical();
        mEpHandler = null;

    }

    //set Elliptical Resistance
    public void setEllipticalResistance(int level) {
        mEpHandler.setEllipticalResistance(level);
    }

    //set Auto reply Bike RPM cmd
    public void setAutoReplyEllipticalRpm(boolean enable) {
        mEpHandler.setAutoReplyEllipticalRpm(enable);
    }

    //set Elliptical GearRatio
    public void setEllipticalGearRatio(int gearRatio) {
        mEpHandler.setEllipticalGearRatio(gearRatio);
    }

    //set Elliptical ResistanceType
    public void setEllipticalResistanceType(int type) {
        mEpHandler.setEllipticalResistanceType(type);
    }

    //set Elliptical LowLimitResistance
    public void setEllipticalLowLimitResistance(int resistance) {
        mEpHandler.setEllipticalLowLimitResistance(resistance);
    }

    //set Elliptical LowLimitCharge
    public void setEllipticalLowLimitCharge(int charge) {
        mEpHandler.setEllipticalLowLimitCharge(charge);
    }

    //set Elliptical Turn Off Lcb
    public void setEllipticalTurnOffLcb(int intValue) {
        mEpHandler.setEllipticalTurnOffLcb(intValue);
    }

    //get Elliptical One Shot Reply Rpm
    public void getEllipticalOneShotReplyRpm() {
        mEpHandler.getEllipticalOneShotReplyRpm();
    }

}
