package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.HybridTM;

public interface IHybridTmHandlerCallback {
    //get HybridTm RPM
    void onHybridTmRPM(int intMotorRPM);

    //get HybridTm Incline Location
    void onHybridTmInclineLoc(int intMotorInclineLoc);

    //get HybridTm InUsed
    void onHybridTmInUsed(byte byteStatus);

    //get HybridTm MotorInfo
    void onHybridTmMotorInfo(byte[] bytesMotorInfo);

}
