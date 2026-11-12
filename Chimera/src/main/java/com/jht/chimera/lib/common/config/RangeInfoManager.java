package com.jht.chimera.lib.common.config;

import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.data.RangeInfo;
import com.jht.datamatrixlib.DataMatrix;

public class RangeInfoManager {

    public enum RangeType {
        AGE,
        HEIGHT_CM,
        HEIGHT_IN,
        WEIGHT_KG,
        WEIGHT_LBS,
        CONSTANT_WATTS,
        PERCENT_MAX_HEART_RATE,
        TARGET_HEART_RATE,
        INCLINE,
        RESISTANCE,
        SPEED_KPH,
        WORKOUT_TIME_MIN,
        WORKOUT_CALORIES,
        WORKOUT_DISTANCE_KM,
        WORKOUT_DISTANCE_MILES,
        WORKOUT_FLOORS
    }

    private static FrameConfigManager frameConfigManager = FrameConfigManager.getInstance();

    public static RangeInfo getRange(RangeType type) {
        RangeInfo rangeInfo = new RangeInfo();
        FrameConfig frameConfig = frameConfigManager.getFrameConfig();
        switch (type) {
            case AGE:
                rangeInfo.copy(frameConfig.age);
                break;

            case HEIGHT_CM:
                rangeInfo.copy(frameConfig.heightCm);
                break;

            case HEIGHT_IN:
                rangeInfo.copy(frameConfig.heightIn);
                break;

            case WEIGHT_KG:
                rangeInfo.copy(frameConfig.weightKg);
                break;

            case WEIGHT_LBS:
                rangeInfo.copy(frameConfig.weightLbs);
                break;

            case CONSTANT_WATTS:
                rangeInfo.copy(frameConfig.constantWatts);
                break;

            case PERCENT_MAX_HEART_RATE:
                rangeInfo.copy(frameConfig.percentTargetHeartRate);
                break;

            case TARGET_HEART_RATE:
                rangeInfo.copy(frameConfig.targetHeartRate);
                break;

            case INCLINE:
                rangeInfo.copy(frameConfig.incline);
                break;

            case RESISTANCE:
                rangeInfo.copy(frameConfig.resistance);
                break;

            case SPEED_KPH:
                boolean metricSpeed = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
                if (metricSpeed) {
                    rangeInfo.copy(frameConfig.speedKph);
                } else {
                    rangeInfo.copy(frameConfig.speedMph);
                }
                break;

            case WORKOUT_TIME_MIN:
                rangeInfo.copy(frameConfig.workoutTime);
                break;

            case WORKOUT_CALORIES:
                rangeInfo.copy(frameConfig.workoutCalories);
                break;

            case WORKOUT_DISTANCE_KM:
                rangeInfo.copy(frameConfig.workoutDistanceKm);
                break;


            case WORKOUT_DISTANCE_MILES:
                rangeInfo.copy(frameConfig.workoutDistanceMi);
                break;

            case WORKOUT_FLOORS:
                rangeInfo.copy(frameConfig.workoutFloors);
                break;

        }

        return rangeInfo;
    }
}
