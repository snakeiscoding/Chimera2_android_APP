package com.jht.chimera.service.workout.math;

public class SpeedToPace {
    public static float KphToMinPerKm(float speedKph) {
        return 60f / speedKph;
    }

    public static float KphToMinPer500m(float speedKph) {
        return 60f / speedKph * 2f;
    }
}
