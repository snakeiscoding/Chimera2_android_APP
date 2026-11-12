package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Elliptical;


public interface IEllipticalHandler {
	//Register EllipticalHandler callback
	void setEllipticalHandlerCallback(IEllipticalHandlerCallback ellipticalHandlerCallback);
	//Unregister EllipticalHandler callback
	void unsetEllipticalHandlerCallback();
	//Initial EllipticalHandler
	void initialElliptical();
	//Uninitial EllipticalHandler
	void unInitialElliptical();
	//set Elliptical Resistance
	void setEllipticalResistance(int intValue);
	//set Auto reply Bike RPM cmd
	void setAutoReplyEllipticalRpm(boolean enable);
	//set Elliptical GearRatio
	void setEllipticalGearRatio(int intValue);
	//set Elliptical ResistanceType
	void setEllipticalResistanceType(int intValue);
	//set Elliptical LowLimitResistance
	void setEllipticalLowLimitResistance(int intValue);
	//set Elliptical LowLimitCharge
	void setEllipticalLowLimitCharge(int intValue);
	//set Elliptical Turn Off Lcb
	void setEllipticalTurnOffLcb(int intValue);
	//get Elliptical one shot reply RPM cmd
	void getEllipticalOneShotReplyRpm();
	
}
