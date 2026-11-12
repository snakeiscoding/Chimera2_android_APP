package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.IndoorBike;

public interface IIndoorBikeHandlerCallback {
	//Auto Reply IndoorBike RPM
	void onIndoorBikeRPM(int intIndoorBikeRPM);
	//Auto Reply Watts (ICR70)
	void onIndoorBikeWatts(int intIndoorBikeWatts);

	//One Shot Reply IndoorBike RPM
	void onIndoorBikeOneShotRPM(int intIndoorBikeRPM);

	//Auto Reply IndoorBike RPM
	void onIndoorBikeSensorData(IndoorBikeSensorData indoorBikeSensorData);

	//One Shot Reply IndoorBike RPM
	void onIndoorBikeOneShotSensorData(IndoorBikeSensorData indoorBikeSensorData);
	//IndoorBike Get calculation parameters of device or algorithm
	void onIndoorBikeGetCalcParaOfDevice(byte[] bytesData);
	//IndoorBike Set the action of spin down 
	void onIndoorBikeSetActionSpinDown(byte[] bytesDataReceived);
	//IndoorBike Set Erg Mode Settings
	void onIndoorBikeSetErgModeSettings(byte[] bytesDataReceived);
	//IndoorBike Get info spin down 
	void onIndoorBikeInfoSpinDown(IndoorBikeInfoSpinDown indoorBikeInfoSpinDown);

	// used for ICR70 calibration
	void onIndoorBikeRawAdc(int adc);
}
