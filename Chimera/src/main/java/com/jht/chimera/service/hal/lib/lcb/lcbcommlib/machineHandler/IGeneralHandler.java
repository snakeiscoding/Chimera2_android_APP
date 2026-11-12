package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler;


public interface IGeneralHandler {
	//Register GeneralHandler callback
	void setGeneralHandlerCallback(IGeneralHandlerCallback generalHandlerCallback);
	//unRegister GeneralHandler callback
	void unRegisterGeneralHandlerCallback();
	//recevice I/O safe key trigger 0x02B2 and E-Stop LCB used priority cmd
	void eStopMachineStop();
	//release CSafe key and E-Stop revert and re-initial LCB
	void eStopRevertLcbInitial();
	//stop Machine used priority cmd 
	void forceStopMachine();
	//set Control Type
	void setControlType(byte controlType);
	//set Auto reply General Battery Status cmd
	void setAutoReplyGeneralBattery(boolean enable);
	//set logMessages and logMessageDetails. Used to turn advanced logging on and off.
	void setAdvancedLogging(boolean logMessages, boolean logMessageDetails);
	
	//set Lcb cmd clear serial number .
	void clearLcbSerialNumber();
	//set Lcb cmd set serial number .
	void setLcbSerialNumber(String serial);
	//set Lcb cmd get serial number .
	void getLcbSerialNumber();	
	//set Lcb cmd get LCB/MCB itself serial number , this function different getLcbSerialNumber.
	void getLcbMcbSelfSerialNumber();	
	//set Lcb cmd get serial number max length.
	void getLcbSnMaxLength();	
	//set Lcb into Update Program mode.
	void setLcbIntoUpdateProgramMode();	
	//Magnet angle Calibration.
	void startMagnetAngleCalibration();	
	//set Auto reply General Magnet angle value cmd
	void setAutoReplyGeneralMagnetAngle(boolean enable);
	//get Magnet angle .
	void getMagnetAngle();	
	//set PFC mode value cmd
	void  setPFCmode(boolean enable);
	//get Moto type .
	void getMotoType();
	//get MCB voltage .
	void getMcbVoltage();	
	
	// Initialize the LCB/MCB before starting the workout.
	void initLcbMcbBeforeWorkout();

}
