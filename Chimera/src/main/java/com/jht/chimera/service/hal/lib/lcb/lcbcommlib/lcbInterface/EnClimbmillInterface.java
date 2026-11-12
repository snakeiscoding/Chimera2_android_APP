package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import android.util.Log;

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.EnClimbmill.EnClimbHandler;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.EnClimbmill.IEnClimbHandlerCallback;

import org.jetbrains.annotations.NonNls;

public class EnClimbmillInterface implements MachineInterfaceBase {

    private static final String TAG = "LCBInterface";
    private ILCBCallback serviceCallback;
    private EnClimbHandler mEnCmHandler;

    private int oldStatus = 0;
    private boolean startControl;
    private boolean irFunctionOn;
    private boolean irTrigger;
    private boolean irLock;
    private boolean czFunctionOn;
    private boolean czTrigger;
    private boolean czLock;
    private boolean mastSafetykeyDown;
    private boolean mastSafetykeyLock;
    private boolean handSafetykeyDown;
    private boolean handSafetykeyLock;
    private boolean noPerson;
    private boolean calibration;

    public EnClimbmillInterface(final ILCBCallback callback) {
        mEnCmHandler = EnClimbHandler.getInstance();
        this.serviceCallback = callback;
        mEnCmHandler.setEnClimbHandlerCallback(mCmHandlerCallback);
        mEnCmHandler.initialEnClimb();
    }

    private IEnClimbHandlerCallback mCmHandlerCallback = new IEnClimbHandlerCallback() {

        @Override
        public void onEnClimbMillStatus(int intEnClimbMillStatus) {
            onEnCmStatusParser(intEnClimbMillStatus);
        }

        @Override
        public void onEnClimbMillSPM(int intEnClimbMillSPM) {
            serviceCallback.onSpm(intEnClimbMillSPM);
        }

        @Override
        public void onEnClimbMillBrakePressure(int intEnClimbMillBrakePressure) {

        }

        @Override
        public void onEnClimbMillWatts(int intEnClimbMillWatts) {
            serviceCallback.onEnClimbMillWatts(intEnClimbMillWatts);
        }

        @Override
        public void onEnClimbMillEStopVale(byte[] b) {
            int cz = 0x00 << 24 | 0x00 << 16 | (b[0] & 0xff) << 8 | (b[1] & 0xff);
            int ir = 0x00 << 24 | 0x00 << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);

            serviceCallback.onEnclimbmillCZcapaIRfreq(cz, ir);
        }

        @Override
        public void onEnClimbMillCalScaling(int intEnClimbMillCalScaling) {
            serviceCallback.onEnClimbMillCalScaling(intEnClimbMillCalScaling);
        }

        @Override
        public void onEnClimbMillFrameCalScaling(int intEnClimbMillFrameCalScaling) {
            serviceCallback.onEnClimbMillFrameCalScaling(intEnClimbMillFrameCalScaling);

        }

        @Override
        public void onEnClimbOneShotSPM(int intEnClimbMillSPM) {
            serviceCallback.onOneShotRpm(intEnClimbMillSPM);
        }

    };

    @Override
    public void startReplyData(boolean start) {
        mEnCmHandler.setAutoReplyEnClimbSpm(start);
        mEnCmHandler.setAutoReplyEnClimbWatts(start);
        mEnCmHandler.setAutoReplyEnClimbCZcapaIRfreq(false);
    }

    @Override
    public void unInitial() {
        mEnCmHandler.unsetEnClimbHandlerCallback();
        mEnCmHandler.unInitialEnClimb();
        mEnCmHandler = null;
    }

    private void onEnCmStatusParser(int status) {
        if (stateChange(status) || isLockState()) {
            onStatusParser(status);
            onStatusHnadler();
        }
    }

    //set EnClimbMill SPM
    public void setEnClimbSPM(int intValue) {
        mEnCmHandler.setEnClimbSPM(intValue);
    }


    //set EnClimbMill Resistance
    public void setEnClimbResistance(int intValue) {
        mEnCmHandler.setEnClimbResistance(intValue);
    }

    //set EnClimb Unlock
    public void setEnClimbUnlock() {
        mEnCmHandler.setEnClimbUnlock();
    }

    //set EnClimbmill Lock
    public void setEnClimbLock(boolean lock) {
        if(lock)
            mEnCmHandler.setEnClimbClimbmillCommand(LCBMessageType.STOP_CLIMBMILL_CONTROL);
        else
            mEnCmHandler.setEnClimbUnlock();
    }

    //set Auto reply EnClimbMill BrakePressure cmd
    public void setAutoReplyEnClimbBrakePressure(boolean enable) {
        mEnCmHandler.setAutoReplyEnClimbSpm(enable);
    }

    //set Auto reply EnClimbMill Watts cmd
    public void setAutoReplyEnClimbWatts(boolean enable) {
        mEnCmHandler.setAutoReplyEnClimbWatts(enable);
    }

    //set Auto reply EnClimbMill CalScaling cmd
    public void setAutoReplyEnClimbCalScaling(boolean enable) {
        mEnCmHandler.setAutoReplyEnClimbCalScaling(enable);
    }

    //set Auto reply EnClimbMill CZ-capacitance IR-frequency cmd
    public void setAutoReplyEnClimbCZcapaIRfreq(boolean enable) {
        mEnCmHandler.setAutoReplyEnClimbCZcapaIRfreq(enable);
    }

    //set EnClimbMill GearRatio
    public void setEnClimbGearRatio(int intValue) {
        mEnCmHandler.setEnClimbGearRatio(intValue);
    }

    //set EnClimbMill ResistanceType
    public void setEnClimbResistanceType(int intValue) {
        mEnCmHandler.setEnClimbResistanceType(intValue);
    }

    //set EnClimbMill GeneratorPole
    public void setEnClimbGeneratorPole(int intValue) {
        mEnCmHandler.setEnClimbGeneratorPole(intValue);
    }

    //set EnClimbMill EStopParameter
    public void setEnClimbEStopParameter(byte byteValue) {
        mEnCmHandler.setEnClimbEStopParameter(byteValue);
    }

    //set EnClimbMill ClimbmillCommand
    public void setEnClimbClimbmillCommand(byte byteValue) {
        mEnCmHandler.setEnClimbClimbmillCommand(byteValue);
    }


    /*  Set LCB action from UCB.
        Data 0x00: Workout Start. (To unlock DC brake)
        Data 0x01: Workout Stop. (To lock DC brake)
        Data 0xA0: 1st control zone disable, frame IR sensor disable
        Data 0xA1: 1st control zone enable, frame IR sensor enable
        Data 0xA2: 1st control zone disable, frame IR sensor enable
        Data 0xA3: 1st control zone enable, frame IR sensor disable
        Data 0xA5: To unlock the lock status of safety mechanism
        Data 0xA6: the 2nd control zone disable
        Data 0xA7: the 2nd control zone enable
        Data 0xA8: The fan on the LCB disable.
        Data 0xA9: The fan on the LCB enable.
        Data 0xB0: To close person detection
        Data 0xB1: To open person detection
    */

    public void setEnClimbmillNoPersonOnOff(boolean on) {

        Log.i(TAG, "LCB Service : Enclimbmill interface set no person  " + on);

        byte data = (on) ? ((byte) 0xB1) : ((byte) 0xB0);
        mEnCmHandler.setEnClimbClimbmillCommand(data);
    }

    public void setEnClimbmillCzOn(boolean on) {
        Log.i(TAG, "LCB Service : Enclimbmill interface set control zone  " + on);
        byte data = (on) ? ((byte) 0xA1) : ((byte) 0xA2);
        mEnCmHandler.setEnClimbClimbmillCommand(data);
    }

    public void setEnClimbmillLCBFanOn() {

        mEnCmHandler.setEnClimbClimbmillCommand((byte) 0xA9);
    }


    //Calibration Function
    public void setEnClimbClimbmillCal(byte[] bytes) {

        mEnCmHandler.setEnClimbClimbmillCal(bytes);
    }

    public void getEnClimbClimbmillCalScaling() {

        mEnCmHandler.getEnClimbClimbmillCalScaling();
    }

    public void setEnClimbClimbmillCalScaling(int intValue) {

        mEnCmHandler.setEnClimbClimbmillCalScaling(intValue);
    }

    public void getEnClimbClimbmillFrameCalScaling() {

        mEnCmHandler.getEnClimbClimbmillFrameCalScaling();
    }

    public void setEnClimbClimbmillFrameCalScaling(int intValue) {

        mEnCmHandler.setEnClimbClimbmillFrameCalScaling(intValue);
    }

    public void getEnClimbOneShotReplySpm() {

        mEnCmHandler.getEnClimbOneShotReplySpm();
    }

    private boolean stateChange(int status) {
        boolean result = (oldStatus != status);
        oldStatus = status;

        return result;
    }

    private boolean isLockState() {
        return (mastSafetykeyLock||handSafetykeyLock);
    }

    private void onStatusParser(int st) {
        /*
        For ENClimbmill byte define (status2)
        Bit 0 : Start Control Speed
        Bit 1 : N/A
        Bit 2 : IR Sensor Function On
        Bit 3 : IR Sensor Trigger
        Bit 4 : IR Sensor Lock
        Bit 5 : Control Zone Function On
        Bit 6 : Control Zone Trigger
        Bit 7 : Control Zone Lock
        */
        /*
        For ENClimbmill byte define (status2)
        Bit 0  : UCB E-Stop Down
        Bit 1  : UCB E-Stop Lock
        Bit 2  : Handrail E-Stop Down
        Bit 3  : Handrail E-Stop Lock
        Bit 4  : No Person
        Bit 5  : Calibration
        */
        byte status[] = {(byte) (st & 0xFF), (byte) ((st >> 8) & 0xFF)};

        startControl = (status[0] & 0x01) != 0;
        irFunctionOn = (status[0] & 0x04) != 0;
        irTrigger = (status[0] & 0x08) != 0;
        irLock = (status[0] & 0x10) != 0;
        czFunctionOn = (status[0] & 0x20) != 0;
        czTrigger = (status[0] & 0x40) != 0;
        czLock = (status[0] & 0x80) != 0;
        mastSafetykeyDown = (status[1] & 0x01) != 0;
        mastSafetykeyLock = (status[1] & 0x02) != 0;
        handSafetykeyDown = (status[1] & 0x04) != 0;
        handSafetykeyLock = (status[1] & 0x08) != 0;
        noPerson = (status[1] & 0x10) != 0;
        calibration = (status[1] & 0x20) != 0;

    }

    public @NonNls String showStatus() {
        return "CMStatus:" + "\n" +
                "\tstartControl: " + startControl + "\n" +
                "\tirFunctionOn: " + irFunctionOn + "\n" +
                "\tirTrigger: " + irTrigger + "\n" +
                "\tirLock: " + irLock + "\n" +
                "\tczFunctionOn: " + czFunctionOn + "\n" +
                "\tczTrigger: " + czTrigger + "\n" +
                "\tczLock: " + czLock + "\n" +
                "\tmastSafetykeyDown: " + mastSafetykeyDown + "\n" +
                "\tmastSafetykeyLock: " + mastSafetykeyLock + "\n" +
                "\thandSafetykeyDown: " + handSafetykeyDown + "\n" +
                "\thandSafetykeyLock: " + handSafetykeyLock + "\n" +
                "\tnoPersion: " + "\n" +
                "\tcalibration: " + calibration;
    }

    private void onStatusHnadler() {

        if (czLock || irLock) {

            if (czTrigger || irTrigger) {

                serviceCallback.onEnClimbmillCZ(czTrigger);
                serviceCallback.onEnClimbmillIR(irTrigger);
            } else if (!mastSafetykeyDown || !handSafetykeyDown) {
                //only lock, do unlock
                serviceCallback.onEnClimbmillStartUnlock();
            }
        } else {

            serviceCallback.onEnClimbmillCZ(czLock);
            serviceCallback.onEnClimbmillIR(irLock);
        }

        //for both mast and hand safetykey down, show mast
        if (mastSafetykeyDown || handSafetykeyDown) {
            if (mastSafetykeyDown) {
                serviceCallback.onEnClimbmillSafetykeyPulled(true, false);
            } else {
                serviceCallback.onEnClimbmillSafetykeyPulled(false, true);
            }
        } else if (mastSafetykeyLock || handSafetykeyLock) //only lock, for quick safetykey down and up
        {
            if (mastSafetykeyLock) {
                serviceCallback.onEnClimbmillSafetykeyPulled(true, false);
            } else {
                serviceCallback.onEnClimbmillSafetykeyPulled(false, true);
            }

            //only lock, need to send unlock command
            serviceCallback.onEnClimbmillStartUnlock();
        } else {
            //for lcb auto unlock, send safetykey release
            serviceCallback.onEnClimbmillSafetykeyPulled(false, false);
        }

        serviceCallback.onPersonInPlace(!noPerson);//0: Normal, 1: No person

        serviceCallback.onEnClimbmillCalibraton(calibration);

        if (startControl) {
                serviceCallback.onEnClimbmillStartControl(true);
        } else {
                serviceCallback.onEnClimbmillStartControl(false);
        }
    }
}