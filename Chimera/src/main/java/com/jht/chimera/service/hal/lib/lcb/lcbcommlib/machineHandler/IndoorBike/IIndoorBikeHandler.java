package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike;


public interface IIndoorBikeHandler {
	//Register IndoorBikeHandler callback
	void setIndoorBikeHandlerCallback(IIndoorBikeHandlerCallback indoorBikeHandlerCallback);
	//Unregister IndoorBikeHandler callback
	void unsetIndoorBikeHandlerCallback();
	//Initial IndoorBikeHandler
	void initialIndoorBike();
	//Uninitial IndoorBikeHandler
	void unInitialIndoorBike();
	//set Auto reply IndoorBike RPM cmd
	void setAutoReplyIndoorBikeRpm(boolean enable);
	//set IndoorBike GearRatio
	void setIndoorBikeGearRatio(int intValue);
	//set IndoorBike ResistanceType
	void setIndoorBikeResistanceType(int intValue);
	//set IndoorBike LowLimitResistance
	void setIndoorBikeLowLimitResistance(int intValue);
	//set IndoorBike LowLimitCharge
	void setIndoorBikeLowLimitCharge(int intValue);
	//set IndoorBike Turn Off Lcb
	void setIndoorBikeTurnOffLcb(int intValue);
	//get IndoorBike one shot reply RPM cmd
	void getIndoorBikeOneShotReplyRpm();
	//set Auto reply IndoorBike SensorData cmd
	void setAutoReplyIndoorBikeSensorData(boolean enable);
	//get IndoorBike one shot reply RPM cmd
	void getIndoorBikeOneShotReplySensorData();
	//set IndoorBike Stepper Motor Position cmd
	void setLcbIndoorBikeStepperMotorPosition(byte[] bytesCmdData);
	//set IndoorBike calculation parameters of device or algorithm cmd
	void setLcbIndoorBikeCalcParaOfDevice(byte[] bytesCmdData);
	//get IndoorBike calculation parameters of device or algorithm cmd
	void getLcbIndoorBikeCalcParaOfDevice();
	void setLcbIndoorBikeActionSpinDown(byte[] bytesData );
	void getLcbIndoorBikeInfoSpinDown();
	//set Auto reply IndoorBike Info SpinDown cmd
	void setAutoReplyIndoorBikeInfoSpinDown(boolean enable);
	void setLcbIndoorBikeErgModeSettings(byte[] bytesData );
	//get IndoorBike Erg Mode Settings cmd
	void getLcbIndoorBikeErgModeSettings();
	//set IndoorBike ResistanceLevel
	void setLcbIndoorBikeResistanceLevel(int intValue );
	//set IndoorBike ErgWatts
	void setLcbIndoorBikeErgWatts(int targetWatts );
	
}
