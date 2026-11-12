package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.AscentTrainer.AscentHandler;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.AscentTrainer.IAscentHandlerCallback;

public class AscentInterface implements MachineInterfaceBase {
    private static final String TAG = "LCBInterface";
    private ILCBCallback serviceCallback;
    private AscentHandler mAscentHandler;

    public AscentInterface(final ILCBCallback callback) {
        mAscentHandler = AscentHandler.getInstance();
        this.serviceCallback = callback;
        mAscentHandler.setAscentHandlerCallback(mAscentHandlerCallback);
        mAscentHandler.initialAscent();
    }

    private IAscentHandlerCallback mAscentHandlerCallback = new IAscentHandlerCallback() {
        @Override
        public void onAscentRPM(int intAscentRPM) {
            serviceCallback.onRpm(intAscentRPM);
        }

        @Override
        public void onAscentIncline(int intAscentIncline) {
            serviceCallback.onInclineADC(intAscentIncline);
        }

        @Override
        public void onAscentOneShotRPM(int intAscentRPM) {
            serviceCallback.onOneShotRpm(intAscentRPM);
        }
    };

    @Override
    public void startReplyData(boolean start) {
        mAscentHandler.setAutoReplyAscentRpm(start);
        mAscentHandler.setAutoReplyAscentIncline(start);
    }

    @Override
    public void unInitial() {
        mAscentHandler.unsetAscentHandlerCallback();
        mAscentHandler.unInitialAscent();
        mAscentHandler = null;
    }

    //set Ascent Resistance
    public void setAscentResistance(int resistance) {
        mAscentHandler.setAscentResistance(resistance);
    }

    //set Ascent Incline
    public void setAscentIncline(int incline) {
        mAscentHandler.setAscentIncline(incline);
    }

    //set Auto reply Ascent RPM cmd
    public void setAutoReplyAscentRpm(boolean enable) {
        mAscentHandler.setAutoReplyAscentRpm(enable);
    }

    //set Auto reply Ascent Incline cmd
    public void setAutoReplyAscentIncline(boolean enable) {
        mAscentHandler.setAutoReplyAscentIncline(enable);
    }

    //set Ascent GearRatio
    public void setAscentGearRatio(int GearRatio) {
        mAscentHandler.setAscentGearRatio(GearRatio);
    }

    //set Ascent ResistanceType
    public void setAscentResistanceType(int type) {
        mAscentHandler.setAscentResistanceType(type);
    }

    //set Ascent GeneratorPole
    public void setAscentGeneratorPole(int intValue) {
        mAscentHandler.setAscentGeneratorPole(intValue);
    }

    //set Ascent LowLimitResistance
    public void setAscentLowLimitResistance(int resistance) {
        mAscentHandler.setAscentLowLimitResistance(resistance);
    }

    //set Ascent LowLimitCharge
    public void setAscentLowLimitCharge(int charge) {
        mAscentHandler.setAscentLowLimitCharge(charge);
    }

    //set Ascent TuneEndIncline
    public void setAscentTuneEndIncline(int intValue) {
        mAscentHandler.setAscentTuneEndIncline(intValue);
    }

    //set Ascent ConsolePower
    public void setAscentConsolePower(int power) {
        mAscentHandler.setAscentConsolePower(power);
    }

    //set Ascent Incline Action
    public void setAscentInclineAction(byte byteValue) {
        mAscentHandler.setAscentInclineAction(byteValue);
    }

    //auto Ascent calibrate Incline
    public void autoAscentCalibrateIncline() {
        mAscentHandler.autoAscentCalibrateIncline();
    }

    //set Ascent Turn Off Lcb
    public void setAscentTurnOffLcb(int intValue) {
        mAscentHandler.setAscentTurnOffLcb(intValue);
    }

    //stop Ascent calibrate
    public void stopAscentCalibrate() {
        mAscentHandler.stopAscentCalibrate();
    }

    //stop Ascent Incline Action
    public void stopAscentInclineAction() {
        mAscentHandler.stopAscentInclineAction();
    }

    //get Ascent One Shot Reply Rpm
    public void getAscentOneShotReplyRpm() {
        mAscentHandler.getAscentOneShotReplyRpm();
    }

}
