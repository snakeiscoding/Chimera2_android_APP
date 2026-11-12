package com.jht.chimera.lib.common.math;

public class PaceToSpeed {
    public static float SecPerKmToKph(float secPerKm) {
        if(secPerKm == 0) return 0;
        return 60f / secPerKm;
    }

    public static float SecPer500mToKph(float secPer500m) {
        return 60f / secPer500m * 2f;
    }
}
