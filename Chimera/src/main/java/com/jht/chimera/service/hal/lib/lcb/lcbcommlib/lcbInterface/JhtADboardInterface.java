package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.JhtADboard.IJhtADboardHandlerCallback;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.JhtADboard.JhtADboardHandler;

public class JhtADboardInterface implements MachineInterfaceBase {
    private static final String TAG = "LCBInterface";
    private ILCBCallback serviceCallback;
    private JhtADboardHandler mJhtADboardHandler;

    public JhtADboardInterface(final ILCBCallback callback) {
        mJhtADboardHandler = JhtADboardHandler.getInstance();
        this.serviceCallback = callback;
        mJhtADboardHandler.setJhtADboardHandlerCallback(mJhtADboardHandlerCallback);
        mJhtADboardHandler.initialJhtADboard();
    }

    private IJhtADboardHandlerCallback mJhtADboardHandlerCallback = new IJhtADboardHandlerCallback() {
        @Override
        public void onJhtADboardRPM(int intJhtADboardRPM) {
            serviceCallback.onRpm(intJhtADboardRPM);
        }

        @Override
        public void onJhtADboardOneShotRPM(int intJhtADboardRPM) {
            serviceCallback.onOneShotRpm(intJhtADboardRPM);
        }

    };

    @Override
    public void startReplyData(boolean start) {
        mJhtADboardHandler.setAutoReplyJhtADboardRpm(start);
    }

    @Override
    public void unInitial() {
        mJhtADboardHandler.unsetJhtADboardHandlerCallback();
        mJhtADboardHandler.unInitialJhtADboard();
        mJhtADboardHandler = null;
    }

    //set JhtADboard Watt Or Resistance
    public void setJhtADboardWattOrResistance(int intValue) {
        mJhtADboardHandler.setJhtADboardWattOrResistance(intValue);
    }

    //set Auto reply JhtADboard RPM cmd
    public void setAutoReplyJhtADboardRpm(boolean enable) {
        mJhtADboardHandler.setAutoReplyJhtADboardRpm(enable);
    }

    //set JhtADboard GearRatio
    public void setJhtADboardGearRatio(int gearRatio) {
        mJhtADboardHandler.setJhtADboardGearRatio(gearRatio);
    }

    //get JhtADboard One Shot Reply Rpm
    public void getJhtADboardOneShotReplyRpm() {
        mJhtADboardHandler.getJhtADboardOneShotReplyRpm();
    }

}
