package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.TcvFxSpinBike;

public interface ITcvFxSpinBikeHandlerCallback {
	//Auto Reply TcvFxSpinBike SensorData
	void onTcvFxSpinBikeSensorData(TcvFxSpinBikeSensorData tcvFxSpinBikeSensorData);

    //Auto Reply TcvFxSpinBike RPM
    void onTcvFxSpinBikeRPM(int intTcvFxSpinBikeRPM);
    //Auto Reply TcvFxSpinBike Watts
    void onTcvFxSpinBikeWatts(int intTcvFxSpinBikeWatts);
    //Auto Reply TcvFxSpinBike Resistance Level
    void onTcvFxSpinBikeResistanceLevel(int intTcvFxSpinBikeResistanceLevel);

}
