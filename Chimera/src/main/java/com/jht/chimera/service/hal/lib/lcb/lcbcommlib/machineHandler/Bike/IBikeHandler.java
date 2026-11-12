package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Bike;


public interface IBikeHandler {
	//Register BikeHandler callback
	void setBikeHandlerCallback(IBikeHandlerCallback bikeHandlerCallback);
	//Unregister BikeHandler callback
	void unsetBikeHandlerCallback();
	//Initial BikeHandler
	void initialBike();
	//Uninitial BikeHandler
	void unInitialBike();
	//set Bike Watt Or Resistance
	void setBikeWattOrResistance(int intValue);
	//set Auto reply Bike RPM cmd
	void setAutoReplyBikeRpm(boolean enable);
	//set Auto resend Bike resistance watt cmd
	void setAutoResendBikeWatt(boolean enable);
	//set Bike GearRatio
	void setBikeGearRatio(int intValue);
	//set Bike ResistanceType
	void setBikeResistanceType(int intValue);
	//set Bike LowLimitResistance
	void setBikeLowLimitResistance(int intValue);
	//set Bike LowLimitCharge
	void setBikeLowLimitCharge(int intValue);
	//set Bike Turn Off Lcb
	void setBikeTurnOffLcb(int intValue);
	//get Bike one shot reply RPM cmd
	void getBikeOneShotReplyRpm();

}
