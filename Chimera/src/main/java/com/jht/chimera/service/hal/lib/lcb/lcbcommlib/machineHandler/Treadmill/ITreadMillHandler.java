package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Treadmill;


public interface ITreadMillHandler {
	//Register TreadMillHandler callback
	void setTreadMillHandlerCallback(ITreadMillHandlerCallback treadMillHandlerCallback);
	//Unregister TreadMillHandler callback
	void unsetTreadMillHandlerCallback();
	//Initial TreadMillHandler
	void initialTreadMill();
	//Uninitial TreadMillHandler
	void unInitialTreadMill();
	//set TreadMill Resistance
	void setTreadMillRPM(int intValue);
	//set TreadMill Incline
	void setTreadMillIncline(int intLoc);
	//set Auto reply TreadMill RPM cmd
	void setAutoReplyTmRpm(boolean enable);
	//set Auto reply TreadMill Incline cmd
	void setAutoReplyTmIncline(boolean enable);
	//set Auto reply TreadMill InUsed cmd
	void setAutoReplyTmInUsed(boolean enable);
	//set Auto reply TreadMill Speedup InUsed cmd
	void setAutoReplyTmSpeedUpInUsed(boolean enable);
	//set Auto reply TreadMill MotorInfo cmd
	void setAutoReplyTmMotorInfo(boolean enable);
	//set TreadMill Incline Action
	void setTreadMillInclineAction(byte byteValue);
	//set TreadMill Motor Type
	void setTreadMillMotorType(int intValue);
	//get TreadMill one shot reply RPM cmd
	void getTreadMillOneShotReplyRpm();
	//set Auto reply TreadMill Ext Status cmd
	void setAutoReplyTreadmillExtStatus(boolean enable);
	//set Tread Mill Ecb Action
	void setTreadMillEcbAction(byte byteValue);
	//get TreadMill OneShot Reply Incline Adc
	void getTreadMillOneShotReplyInclineAdc();
    //get HybridTM Ir Addr Version cmd
    void getHybridTMIrAddrVersion();
    //set HybridTM Ir Addr Auto Detect Ir Staus
    void getHybridTMIrAddrIrStatus();
    void setHybridTMIrAddrIrDetection(boolean bEnable);
    //set HybridTM Ir Addr board enter ERP cmd
    void setHybridTMIrAddrEnterErpMode();
    //set HybridTM Ir Addr board leave ERP cmd
    void setHybridTMIrAddrLeaveErpMode();
}
