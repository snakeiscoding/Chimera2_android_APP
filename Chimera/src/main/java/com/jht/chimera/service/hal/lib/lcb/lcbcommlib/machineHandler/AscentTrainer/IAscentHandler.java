package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.AscentTrainer;


public interface IAscentHandler {
	//Register AscentHandler callback
	void setAscentHandlerCallback(IAscentHandlerCallback ascentHandlerCallback);
	//Unregister AscentHandler callback
	void unsetAscentHandlerCallback();
	//Initial AscentHandler
	void initialAscent();
	//Uninitial AscentHandler
	void unInitialAscent();
	//set Ascent Resistance
	void setAscentResistance(int intValue);
	//set Ascent Incline
	void setAscentIncline(int intLoc);
	//set Auto reply Ascent RPM cmd
	void setAutoReplyAscentRpm(boolean enable);
	//set Auto reply Ascent Incline cmd
	void setAutoReplyAscentIncline(boolean enable);
	//set Ascent GearRatio
	void setAscentGearRatio(int intValue);
	//set Ascent ResistanceType
	void setAscentResistanceType(int intValue);
	//set Ascent GeneratorPole
	void setAscentGeneratorPole(int intValue);
	//set Ascent LowLimitResistance
	void setAscentLowLimitResistance(int intValue);
	//set Ascent LowLimitCharge
	void setAscentLowLimitCharge(int intValue);
	//set Ascent TuneEndIncline
	void setAscentTuneEndIncline(int intValue);
	//set Ascent ConsolePower
	void setAscentConsolePower(int intValue);
	//set Ascent Incline Action
	void setAscentInclineAction(byte byteValue);
	//auto Ascent calibrate Incline
	void autoAscentCalibrateIncline();
	//set Ascent Turn Off Lcb
	void setAscentTurnOffLcb(int intValue);
	//stop Ascent calibrate
	void stopAscentCalibrate();
	//stop Ascent Incline Action
	void stopAscentInclineAction();
	//get Ascent one shot reply RPM cmd
	void getAscentOneShotReplyRpm();
	
}
