package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.JhtADboard;


public interface IJhtADboardHandler {
	//Register JhtADboardHandler callback
	void setJhtADboardHandlerCallback(IJhtADboardHandlerCallback jhtADboardHandlerCallback);
	//Unregister JhtADboardHandler callback
	void unsetJhtADboardHandlerCallback();
	//Initial JhtADboardHandler
	void initialJhtADboard();
	//Uninitial JhtADboardHandler
	void unInitialJhtADboard();
	//set JhtADboard Watt Or Resistance
	void setJhtADboardWattOrResistance(int intValue);
	//set Auto reply JhtADboard RPM cmd
	void setAutoReplyJhtADboardRpm(boolean enable);
	//set Bike GearRatio
	void setJhtADboardGearRatio(int intValue);
	//get JhtADboard one shot reply RPM cmd
	void getJhtADboardOneShotReplyRpm();

}
