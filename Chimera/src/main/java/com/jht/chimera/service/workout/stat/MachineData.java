package com.jht.chimera.service.workout.stat;

public class MachineData
{
    public static final int HEART_RATE_STATUS_NORMAL = 0;
    public static final int HEART_RATE_STATUS_SIGNAL_DETECTED = 1;

    public int rpm;
    public int heartRate;
    public int heartRateStatus;
    public int resistance;
    public float speedKph;
    public float incline;
    public float watts;
    public int stepsPerMin;
    public float caloriesPerSec;
    public float mets;

    @Override
    public String toString()
    {
        return "MachineData{" +
                "rpm=" + rpm +
                ", heartRate=" + heartRate +
                ", heartRateStatus=" + heartRateStatus +
                ", resistance=" + resistance +
                ", speedKph=" + speedKph +
                ", incline=" + incline +
                ", watts=" + watts +
                ", stepsPerMin=" + stepsPerMin +
                ", caloriesPerSec=" + caloriesPerSec +
                ", mets=" + mets +
                '}';
    }
}
