package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.AscentTrainer;

public interface IAscentHandlerCallback {
	//Auto Reply Ascent RPM
	void onAscentRPM(int intAscentRPM);
	//Auto Reply Ascent Incline
	void onAscentIncline(int intAscentIncline);
	//One Shot Reply Ascent RPM
	void onAscentOneShotRPM(int intAscentRPM);
	
}
