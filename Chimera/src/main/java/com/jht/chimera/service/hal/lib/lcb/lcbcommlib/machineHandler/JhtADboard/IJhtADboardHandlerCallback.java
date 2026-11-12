package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.JhtADboard;

public interface IJhtADboardHandlerCallback {
    //Auto Reply JhtADboard RPM
    void onJhtADboardRPM(int intJhtADboardRPM);

    //One Shot Reply JhtADboard RPM
    void onJhtADboardOneShotRPM(int intJhtADboardRPM);

}
