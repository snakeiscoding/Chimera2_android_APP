package com.jht.chimera.service.hal.lib.lcb;

public class LCBStatus {


    public LCBStatus(byte status) {
        inclineDown = (status & 0x01) != 0;
        inclineUp = (status & 0x02) != 0;
        mainMotorStatus = (status & 0x04) != 0;
        selfPower = (status & 0x08) != 0;
        encoderRPMStatus = (status & 0x08) != 0;

        batteryCapacity = (status & 0x10) != 0;
        commandError = (status & 0x20) != 0;
        lcbError = (status & 0x40) != 0;
        initialCalibration = (status & 0x80) != 0;
    }

    public LCBStatus(LCBStatus in) {
        inclineDown = in.inclineDown;
        inclineUp = in.inclineUp;
        mainMotorStatus = in.mainMotorStatus;
        selfPower = in.selfPower;
        encoderRPMStatus = in.encoderRPMStatus;
        batteryCapacity = in.batteryCapacity;
        commandError = in.commandError;
        lcbError = in.lcbError;
        initialCalibration = in.initialCalibration;
    }

    @Override
    public String toString() {
        return "IOStatus:" + "\n" +
                "\tinclineDown: " + inclineDown + "\n" +
                "\tinclineUp: " + inclineUp + "\n" +
                "\tmainMotorStatus: " + mainMotorStatus + "\n" +
                "\tselfPower: " + selfPower + "\n" +
                "\tencoderRPMStatus: " + encoderRPMStatus + "\n" +
                "\tbatteryCapacity: " + batteryCapacity + "\n" +
                "\tcommandError: " + commandError + "\n" +
                "\tlcbError: " + lcbError + "\n" +
                "\tinitialCalibration: " + initialCalibration;
    }

    public void onStatus(byte status) {
        inclineDown = (status & 0x01) != 0;
        inclineUp = (status & 0x02) != 0;
        mainMotorStatus = (status & 0x04) != 0;
        selfPower = (status & 0x08) != 0;

        batteryCapacity = (status & 0x10) != 0;
        commandError = (status & 0x20) != 0;
        lcbError = (status & 0x40) != 0;
        initialCalibration = (status & 0x80) != 0;
    }

    public void onTMStatus(byte status) {
        inclineDown = (status & 0x01) != 0;
        inclineUp = (status & 0x02) != 0;
        mainMotorStatus = (status & 0x04) != 0;
        encoderRPMStatus = (status & 0x08) != 0;

        batteryCapacity = (status & 0x10) != 0;
        commandError = (status & 0x20) != 0;
        lcbError = (status & 0x40) != 0;
        initialCalibration = (status & 0x80) != 0;

    }

    public boolean inclineDown;
    public boolean inclineUp;
    public boolean mainMotorStatus;
    public boolean selfPower;
    public boolean batteryCapacity;
    public boolean commandError;
    public boolean lcbError;
    public boolean initialCalibration;
    public boolean encoderRPMStatus;
}
