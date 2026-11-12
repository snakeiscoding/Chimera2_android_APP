package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Climbmill;

public interface IClimbHandlerCallback {
	//Reply ClimbMill status
	void onClimbMillStatus(int intClimbMillStatus);
	//Reply ClimbMill RPM
	void onClimbMillRPM(int intClimbMillRPM);
	//Reply ClimbMill CZ-capacitance IR-frequency
	void onClimbMillCZcapaIRfreq(byte[] bytesClimbMillCzIr);
	
}
