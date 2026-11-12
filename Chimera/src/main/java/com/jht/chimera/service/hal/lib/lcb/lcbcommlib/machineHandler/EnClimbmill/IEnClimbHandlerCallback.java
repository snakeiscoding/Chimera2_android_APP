package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.EnClimbmill;

public interface IEnClimbHandlerCallback {
	//Reply EnClimbMill status
	void onEnClimbMillStatus(int intEnClimbMillStatus);
	//Reply EnClimbMill SPM
	void onEnClimbMillSPM(int intEnClimbMillSPM);
	//Reply EnClimbMill BrakePressure
	void onEnClimbMillBrakePressure(int intEnClimbMillBrakePressure);
	//Reply EnClimbMill Watts
	void onEnClimbMillWatts(int intEnClimbMillWatts);
	//Reply EnClimbMill EStopVale
	void onEnClimbMillEStopVale(byte[] bytesEStopVale);
	//Reply EnClimbMill CalScaling
	void onEnClimbMillCalScaling(int intEnClimbMillCalScaling);
	//Reply EnClimbMill FrameCalScaling
	void onEnClimbMillFrameCalScaling(int intEnClimbMillFrameCalScaling);
	//One Shot Reply EnClimb SPM
	void onEnClimbOneShotSPM(int intEnClimbMillSPM);
	
}
