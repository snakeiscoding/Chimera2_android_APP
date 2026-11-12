package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Bike;

public interface IBikeHandlerCallback {
    //Auto Reply Bike RPM
    void onBikeRPM(int intBikeRPM);

    //One Shot Reply Bike RPM
    void onBikeOneShotRPM(int intBikeRPM);

}
