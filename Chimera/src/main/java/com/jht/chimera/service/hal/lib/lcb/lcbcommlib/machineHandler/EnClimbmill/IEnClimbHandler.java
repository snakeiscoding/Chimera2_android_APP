package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.EnClimbmill;


public interface IEnClimbHandler {
	//Register EnClimbHandler callback
	void setEnClimbHandlerCallback(IEnClimbHandlerCallback enClimbHandlerCallback);
	//Unregister EnClimbHandler callback
	void unsetEnClimbHandlerCallback();
	//Initial EnClimbHandler
	void initialEnClimb();
	//Uninitial EnClimbHandler
	void unInitialEnClimb();
	//set EnClimb SPM
	void setEnClimbSPM(int intValue);
	//set EnClimb Resistance
	void setEnClimbResistance(int intValue);
	//set EnClimb Unlock
	void setEnClimbUnlock();
	//set Auto reply EnClimbMill SPM cmd
	void setAutoReplyEnClimbSpm(boolean enable);
	//set Auto reply EnClimbMill BrakePressure cmd
	void setAutoReplyEnClimbBrakePressure(boolean enable);
	//set Auto reply EnClimbMill Watts cmd
	void setAutoReplyEnClimbWatts(boolean enable);
	//set Auto reply EnClimbMill CalScaling cmd
	void setAutoReplyEnClimbCalScaling(boolean enable);
	//set Auto reply EnClimbMill CZ-capacitance IR-frequency cmd
	void setAutoReplyEnClimbCZcapaIRfreq(boolean enable);
	//set EnClimbMill GearRatio
	void setEnClimbGearRatio(int intValue);
	//set EnClimbMill ResistanceType
	void setEnClimbResistanceType(int intValue);
	//set EnClimbMill GeneratorPole
	void setEnClimbGeneratorPole(int intValue);
	//set EnClimbMill EStopParameter
	void setEnClimbEStopParameter(byte byteValue);
	//set EnClimbMill ClimbmillCommand
	void setEnClimbClimbmillCommand(byte byteValue);
	//set EnClimbMill ClimbmillCal
	void setEnClimbClimbmillCal(byte[] bytes);
	//get EnClimbMill ClimbmillCalScaling
	void getEnClimbClimbmillCalScaling();
	//set EnClimbMill ClimbmillCalScaling
	void setEnClimbClimbmillCalScaling(int intValue);
	//get EnClimbMill ClimbmillFrameCalScaling
	void getEnClimbClimbmillFrameCalScaling();
	//set EnClimbMill ClimbmillFrameCalScaling
	void setEnClimbClimbmillFrameCalScaling(int intValue);
	//get EnClimb one shot reply RPM cmd
	void getEnClimbOneShotReplySpm();
	
}
