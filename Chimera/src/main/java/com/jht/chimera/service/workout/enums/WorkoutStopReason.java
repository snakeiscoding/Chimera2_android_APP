package com.jht.chimera.service.workout.enums;

public enum WorkoutStopReason {
    NORMAL,           // workout ended normally
    RESET,            // workout reset by splash screen
    ERROR,            // workout ended by error in code
    USER,             // workout ended manually by user
    NO_PAUSE,         // workout ended because it was paused
    PAUSE_TIMEOUT,    // workout ended because the pause timer timed out
    TEST_FAIL_NO_HR;  // workout ended because no heart rate was detected
}
