package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Stepper.IStepperHandlerCallback;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Stepper.StepperHandler;

public class StepperInterface implements MachineInterfaceBase {


    private static final String TAG = "LCBInterface";
    private ILCBCallback serviceCallback;
    private StepperHandler mStepperHandler;

    public StepperInterface(final ILCBCallback callback) {
        mStepperHandler = StepperHandler.getInstance();
        this.serviceCallback = callback;
        mStepperHandler.setStepperHandlerCallback(mStepperHandlerCallback);
        mStepperHandler.initialStepper();
    }

    private IStepperHandlerCallback mStepperHandlerCallback = new IStepperHandlerCallback() {

        @Override
        public void onStepperRPM(int intStepperRPM) {
            serviceCallback.onRpm(intStepperRPM);
        }

        @Override
        public void onStepperOneShotRPM(int intStepperRPM) {
            serviceCallback.onOneShotRpm(intStepperRPM);
        }

    };

    @Override
    public void startReplyData(boolean start) {
        mStepperHandler.setAutoReplyStepperRpm(start);
    }

    @Override
    public void unInitial() {
        mStepperHandler.unsetStepperHandlerCallback();
        mStepperHandler.unInitialStepper();
        mStepperHandler = null;
    }

    //set Stepper Resistance
    public void setStepperResistance(int resistance) {
        mStepperHandler.setStepperResistance(resistance);
    }

    //set Stepper GearRatio
    public void setStepperGearRatio(int gearRatio) {
        mStepperHandler.setStepperGearRatio(gearRatio);
    }

    //set Stepper ResistanceType
    public void setStepperResistanceType(int type) {
        mStepperHandler.setStepperResistanceType(type);
    }

    //set Stepper Turn Off Lcb
    public void setStepperTurnOffLcb(int offLcb) {
        mStepperHandler.setStepperTurnOffLcb(offLcb);
    }

    //get Stepper One Shot Reply Rpm
    public void getStepperOneShotReplyRpm() {
        mStepperHandler.getStepperOneShotReplyRpm();
    }

}
