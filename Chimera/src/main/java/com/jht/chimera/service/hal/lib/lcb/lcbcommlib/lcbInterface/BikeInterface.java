package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Bike.BikeHandler;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Bike.IBikeHandlerCallback;

public class BikeInterface implements MachineInterfaceBase {
    private static final String TAG = "LCBInterface";
    private ILCBCallback serviceCallback;
    private BikeHandler mBikeHandler;

    public BikeInterface(final ILCBCallback callback) {
        mBikeHandler = BikeHandler.getInstance();
        this.serviceCallback = callback;
        mBikeHandler.setBikeHandlerCallback(mBikeHandlerCallback);
        mBikeHandler.initialBike();
    }

    private IBikeHandlerCallback mBikeHandlerCallback = new IBikeHandlerCallback() {
        @Override
        public void onBikeRPM(int intBikeRPM) {
            serviceCallback.onRpm(intBikeRPM);
        }

        @Override
        public void onBikeOneShotRPM(int intBikeRPM) {
            serviceCallback.onOneShotRpm(intBikeRPM);
        }

    };

    @Override
    public void startReplyData(boolean start) {
        mBikeHandler.setAutoReplyBikeRpm(start);
        mBikeHandler.setAutoResendBikeWatt(start);
    }

    @Override
    public void unInitial() {
        mBikeHandler.unsetBikeHandlerCallback();
        mBikeHandler.unInitialBike();
        mBikeHandler = null;
    }

    //set Bike Watt Or Resistance
    public void setBikeWattOrResistance(int intValue) {
        mBikeHandler.setBikeWattOrResistance(intValue);
    }

    //set Auto reply Bike RPM cmd
    public void setAutoReplyBikeRpm(boolean enable) {
        mBikeHandler.setAutoReplyBikeRpm(enable);
    }

    //set Auto resend Bike resistance watt cmd
    public void setAutoResendBikeWatt(boolean enable) {
        mBikeHandler.setAutoResendBikeWatt(enable);
    }

    //set Bike GearRatio
    public void setBikeGearRatio(int gearRatio) {
        mBikeHandler.setBikeGearRatio(gearRatio);
    }

    //set Bike ResistanceType
    public void setBikeResistanceType(int type) {
        mBikeHandler.setBikeResistanceType(type);
    }

    //set Bike LowLimitResistance
    public void setBikeLowLimitResistance(int resistance) {
        mBikeHandler.setBikeLowLimitResistance(resistance);
    }

    //set Bike LowLimitCharge
    public void setBikeLowLimitCharge(int charge) {
        mBikeHandler.setBikeLowLimitCharge(charge);
    }

    //set Bike Turn Off Lcb
    public void setBikeTurnOffLcb(int intValue) {
        mBikeHandler.setBikeTurnOffLcb(intValue);
    }

    //get Bike One Shot Reply Rpm
    public void getBikeOneShotReplyRpm() {
        mBikeHandler.getBikeOneShotReplyRpm();
    }
}
