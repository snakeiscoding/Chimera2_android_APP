package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.HybridTM;


public interface IHybridTmHandler {
	//Register HybridTmHandler callback
	void setHybridTmHandlerCallback(IHybridTmHandlerCallback hybridTmHandlerCallback);
	//Unregister HybridTmHandler callback
	void unsetHybridTmHandlerCallback();
	//Initial HybridTm
	void initialHybridTm();
	//Uninitial HybridTm
	void unInitialHybridTm();
	//set HybridTm Resistance
	void setHybridTmRPM(int intValue);
	//set HybridTm Incline
	void setHybridTmIncline(int intLoc);
	//set Auto reply HybridTM RPM cmd
	void setAutoReplyHybridTmRpm(boolean enable);
	//set Auto reply HybridTM Incline cmd
	void setAutoReplyHybridTmIncline(boolean enable);
	//set Auto reply TreadMill InUsed cmd
	void setAutoReplyHybridTmInUsed(boolean enable);
	//set Auto reply TreadMill MotorInfo cmd
	void setAutoReplyHybridTmMotorInfo(boolean enable);
	//set HybridTm Incline Action
	void setHybridTmInclineAction(byte byteValue);
	//set TreadMill Motor Type
	void setHybridTmMotorType(int intValue);
	
}
