package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler;

public interface IGeneralHandlerCallback {
	//LCB error code
	void onErrorCode(int lcbErrorCode);
	//LCB type
	void onLcbType(byte lcbType);
	//LCB version
	void onLcbVersion(int major, int minor);
	//LCB initial process finished
	void onLcbInitialFinish();

	//Status byte callback ==>
	//Motor Incline Downward
	void onInclineMotorDownwardStatus(boolean motorDownwardStatus);
	//Motor Incline Upward
	void onInclineMotorUpwardStatus(boolean motorUpwardStatus);
	//Motor Incline Upward
	void onMainMotorStatus(boolean motorStatus);
	//Encoder RPM Status 1 : AC 0: DC
	void onEncoderRPMStatus(boolean encoderRPMStatus);
	// battery capacity 0:enougt 1:not enough
	void onBatteryCapacityStatus(boolean batteryCapacityStatus);
	// command Error Status 0:correct 1:error
	void onCommandErrorStatus(boolean commandErrorStatus);
	// initial calibration status 0:initial already 1:action
	void onInitialCalibrationStatus(boolean initialCalibrationStatus);
	//<== Status byte callback

	//Battery Voltage
	void onBatteryVoltage(int intBatteryVoltage);
	
	//LCB Status
	void onLcbStatus(byte lcbstatus);

	//clear LCB serial number status
	void onClearLcbSnStatus(boolean clearStatus);
	//set LCB serial number status
	void onSetLcbSnStatus(boolean setStatus);
	//get LCB serial number string (this callback include getLcbSerialNumber & getLcbMcbSelfSerialNumber)
	void onGetLcbSerialNumber(boolean getStatus, String getString);
	//get LCB serial number max length ,this callback not all LCB can get
	void onGetLcbSnMaxLength(boolean getStatus, int intMaxLength);

	//Lcb Into Update Mode response
	void onIntoLcbUpdateMode(boolean intoSuccess);

	//Finish Calibration Magnet Angle value
	void onLcbFinishMagnetAngleValue(int intMagnetAngle);
	//TM Mcb Moto Type
	void onMcbMotoType(int intMotoType);
	//Get Mcb Voltage value
	void onGetMcbVoltageValue(int intVoltage);
	
}
