package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Stepper;

public interface IStepperHandlerCallback {
    //Auto Reply Stepper RPM
    void onStepperRPM(int intStepperRPM);

    //One Shot Reply Stepper RPM
    void onStepperOneShotRPM(int intStepperRPM);

}
