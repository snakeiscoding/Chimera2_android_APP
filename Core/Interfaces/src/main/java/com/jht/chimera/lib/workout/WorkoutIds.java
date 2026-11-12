package com.jht.chimera.lib.workout;

public class WorkoutIds {
    public static final int MANUAL = 0;
    public static final int ROLLING_HILLS = 9;
    public static final int DISTANCE_GOAL = 14;
    public static final int CALORIES_GOAL = 15;
    public static final int FLOORS_GOAL = 16;
    public static final int SPRINT8 = 18;
    public static final int CONSTANT_WATTS = 20;
    public static final int GERKIN_FITNESS_TEST = 30;
    public static final int ARMY_FITNESS_TEST = 33;
    public static final int NAVY_FITNESS_TEST = 34;
    public static final int AIR_FORCE_FITNESS_TEST = 35;
    public static final int MARINES_FITNESS_TEST = 36;
    public static final int PEB_FITNESS_TEST = 37;
    public static final int COOPER_FITNESS_TEST = 38;
    public static final int INTERVALS = 40;
    public static final int THR_SPEED = 44;
    public static final int THR_INCLINE = 45;
    public static final int THR_RESISTANCE = 46;
    public static final int VIRTUAL_ACTIVE = 47;
    public static final int FAT_BURN = 49;
    public static final int GLUTES = 50;
    public static final int CPAT_FITNESS_TEST = 51;
    public static final int WFI_FITNESS_TEST = 52;
    public static final int SUBMAX_FITNESS_TEST = 53;
    public static final int LANDMARKS = 55;
    public static final int STEPPER_FITNESS_TEST = 56;
    public static final int INTERVALS_SPEED = 57;
    public static final int MIGROS_POTENTIAL = 58;
    public static final int MIGROS_EXHAUSTION = 59;
    public static final int MIGROS_FATIGUE = 60;
    public static final int HYBRID_TD_SWITCH_STRIDE = 67;
    public static final int FEATURED = 71;
    public static final int FTP_TEST_8 = 72;
    public static final int PACE_TIME_GOAL = 73;
    public static final int PACE_DISTANCE_GOAL = 74;
    public static final int FTP_TEST_20 = 75;
    public static final int MAX_HR_TEST = 76;
    public static final int MARATHON = 77;
    public static final int HIIT = 78;
    public static final int EGYM = 79;
    public static final int TARGET_TRAINING = 89;
    public static final int CUSTOM_WORKOUT = 90;
    public static final int TIME_GOAL = 93;
    public static final int TIME_GOAL_MANUAL_COOLDOWN = 94; // Workout does not enter cooldown unless manually triggered
    public static final int IFIT = 95;
    public static final int YMCA_360 = 96;
    public static final int MATRIX_TEST_APP = 97;
    public static final int CARDIO_SCAN = 98;
    public static final int PROGRESSIVE_WORKOUT = 99;
//    public static final int TERA_WELLNESS = 100; comment out to retire this workout id for future
    public static final int SECA_PACE = 117;
    public static final int SPIN_BIKE_INTERVALS_DISTANCE = 88;
    public static final int SPIN_BIKE_INTERVALS_TIME = 118;

    public static boolean isMigros(int id) {
        switch (id) {
            case MIGROS_POTENTIAL:
            case MIGROS_EXHAUSTION:
            case MIGROS_FATIGUE:
                return true;
        }
        return false;
    }

    public static boolean isTargetHeartRate(int id) {
        switch (id) {
            case THR_SPEED:
            case THR_INCLINE:
            case THR_RESISTANCE:
                return true;
        }
        return false;
    }

    // The club com skip code may need further review, but for now this covers old behavior in one place.
    // (Could be added to workout item but probably not to app info?)
    public static boolean isClubComSkip(int id) {
        switch (id) {
            case COOPER_FITNESS_TEST:
            case SPRINT8:
            case GERKIN_FITNESS_TEST:
            case WFI_FITNESS_TEST:
            case SUBMAX_FITNESS_TEST:
            case STEPPER_FITNESS_TEST:
            case ARMY_FITNESS_TEST:
            case NAVY_FITNESS_TEST:
            case MARINES_FITNESS_TEST:
            case AIR_FORCE_FITNESS_TEST:
            case PEB_FITNESS_TEST:
            case CPAT_FITNESS_TEST:
            case VIRTUAL_ACTIVE:
            case LANDMARKS:
            case CUSTOM_WORKOUT:
                return true;
            default:
                return false;
        }
    }

}
