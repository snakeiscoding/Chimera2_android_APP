package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Climbmill;


public interface IClimbHandler {
	//Register ClimbHandler callback
	void setClimbHandlerCallback(IClimbHandlerCallback climbHandlerCallback);
	//Unregister ClimbHandler callback
	void unsetClimbHandlerCallback();
	//Initial ClimbHandler
	void initialClimb();
	//Uninitial ClimbHandler
	void unInitialClimb();
	//set Climb Resistance
	void setClimbResistance(int intValue);
	//set Climb Unlock
	void setClimbUnlock();
	//set Auto reply ClimbMill RPM cmd
	void setAutoReplyClimbRpm(boolean enable);
	//set Auto reply ClimbMill CZ-capacitance IR-frequency cmd
	void setAutoReplyClimbCZcapaIRfreq(boolean enable);
	//set ClimbMill GearRatio
	void setClimbGearRatio(int intValue);
	//set ClimbMill ResistanceType
	void setClimbResistanceType(int intValue);
	//set ClimbMill GeneratorPole
	void setClimbGeneratorPole(int intValue);
	//set ClimbMill EStopParameter
	void setClimbEStopParameter(byte byteValue);
	//set ClimbMill ClimbmillCommand
	void setClimbClimbmillCommand(byte byteValue);
}
