package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Stepper;


public interface IStepperHandler {
	//Register StepperHandler callback
	void setStepperHandlerCallback(IStepperHandlerCallback stepperHandlerCallback);
	//Unregister StepperHandler callback
	void unsetStepperHandlerCallback();
	//Initial StepperHandler
	void initialStepper();
	//Uninitial StepperHandler
	void unInitialStepper();
	//set Stepper Resistance
	void setStepperResistance(int intValue);
	//set Auto reply Stepper RPM cmd
	void setAutoReplyStepperRpm(boolean enable);
	//set Stepper GearRatio
	void setStepperGearRatio(int intValue);
	//set Stepper ResistanceType
	void setStepperResistanceType(int intValue);
	//set Stepper Turn Off Lcb
	void setStepperTurnOffLcb(int intValue);
	//get Stepper one shot reply RPM cmd
	void getStepperOneShotReplyRpm();
	
}
