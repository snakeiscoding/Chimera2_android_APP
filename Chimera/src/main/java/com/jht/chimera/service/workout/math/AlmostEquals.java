package com.jht.chimera.service.workout.math;

public class AlmostEquals {
    public static boolean equals(float value1, float value2, float epsilon) {
        return Math.abs(value1 - value2) < epsilon;
    }

    public static boolean equals(float value1, float value2) {
        return equals(value1, value2, MathConstants.EPSILON);
    }
}
