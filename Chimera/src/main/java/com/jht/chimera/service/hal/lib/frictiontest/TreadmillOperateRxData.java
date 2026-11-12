package com.jht.chimera.service.hal.lib.frictiontest;

/**
 * Class for data from the treadmill operate command
 */
public class TreadmillOperateRxData {
    public int currentError;
    public int motorTorque;
    public int actualMotorSpeed;
    public int actualInclinePos;
    public int motorVolts;
    public int motorCurrent;
    public int status;
    public boolean treadmillInUse;
}
