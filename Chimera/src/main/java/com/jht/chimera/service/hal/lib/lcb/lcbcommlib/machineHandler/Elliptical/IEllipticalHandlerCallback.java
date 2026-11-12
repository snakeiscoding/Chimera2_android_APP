package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Elliptical;

public interface IEllipticalHandlerCallback {
    //Auto Reply Elliptical RPM
    void onEllipticalRPM(int intEllipticalRPM);

    //OneShot Reply Elliptical RPM
    void onEllipticalOneShotRPM(int intEllipticalRPM);

}
