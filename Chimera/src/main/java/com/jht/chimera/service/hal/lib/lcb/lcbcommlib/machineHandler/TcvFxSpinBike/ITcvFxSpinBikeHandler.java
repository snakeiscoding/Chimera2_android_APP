package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.TcvFxSpinBike;


public interface ITcvFxSpinBikeHandler {
	//Register TcvFxSpinBikeHandler callback
	void setTcvFxSpinBikeHandlerCallback(ITcvFxSpinBikeHandlerCallback tcvFxSpinBikeHandlerCallback);
	//Unregister TcvFxSpinBikeHandler callback
	void unsetTcvFxSpinBikeHandlerCallback();
	//Initial TcvFxSpinBikeHandler
	void initialTcvFxSpinBike();
	//Uninitial TcvFxSpinBikeHandler
	void unInitialTcvFxSpinBike();
	//set Auto reply TcvFxSpinBike SensorData cmd
	void setAutoReplyTcvFxSpinBikeSensorData(boolean enable);
	//get TcvFxSpinBike one shot reply SensorData cmd
	void getTcvFxSpinBikeOneShotReplySensorData();
}
