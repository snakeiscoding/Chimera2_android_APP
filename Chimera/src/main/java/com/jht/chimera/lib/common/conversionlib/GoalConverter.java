package com.jht.chimera.lib.common.conversionlib;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.datamatrixlib.DataMatrix;

public class GoalConverter {

    private static float getWorkoutKPH() {
        return DataMatrix.getFloat(FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill) ? HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT : HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, 0.0f);
    }

    public static double convert(int inputGoalType, int outputGoalType, float goalValue) {
        switch (inputGoalType) {
            case ConsoleWorkoutVariables.GOAL_TYPE_TIME:
            case ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE:
                switch (outputGoalType) {
                    case ConsoleWorkoutVariables.GOAL_TYPE_DISTANCE:
                    case ConsoleWorkoutVariables.GOAL_TYPE_DISTANCE_STAGE:
                        return getWorkoutKPH() * (goalValue / 60.0f);
                    case ConsoleWorkoutVariables.GOAL_TYPE_CALORIES:
                    case ConsoleWorkoutVariables.GOAL_TYPE_CALORIES_STAGE:
                        return DataMatrix.getFloat(HALKeys.DATA_CALORIES_PER_SEC_FLOAT) * goalValue;
                    case ConsoleWorkoutVariables.GOAL_TYPE_FLOORS:
                    case ConsoleWorkoutVariables.GOAL_TYPE_FLOORS_STAGE:
                        return DataMatrix.getInt(HALKeys.DATA_SPM_INT) / 60.0f * goalValue / FrameConfigManager.getInstance().getFrameConfig().STEPS_PER_FLOOR;
                    case ConsoleWorkoutVariables.GOAL_TYPE_ELEVATION:
                    case ConsoleWorkoutVariables.GOAL_TYPE_ELEVATION_STAGE:
                        if(FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Stepper) || FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill)) {
                            return DataMatrix.getInt(HALKeys.DATA_SPM_INT) / 60.0f * goalValue * FrameConfigManager.getInstance().getFrameConfig().STEPS_PER_METER;
                        }
                        else if(FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill)) {
                            return (getWorkoutKPH() / 3.6f * Math.sin(Math.atan(DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT) / 100.0f))) * goalValue;
                        }
                    case ConsoleWorkoutVariables.GOAL_TYPE_STEPS:
                    case ConsoleWorkoutVariables.GOAL_TYPE_STEPS_STAGE:
                        return DataMatrix.getInt(HALKeys.DATA_SPM_INT) / 60.0f * goalValue;
                }
                break;
            case ConsoleWorkoutVariables.GOAL_TYPE_DISTANCE:
            case ConsoleWorkoutVariables.GOAL_TYPE_DISTANCE_STAGE: {
                float seconds = goalValue / (getWorkoutKPH() / 3600);
                return outputGoalType == ConsoleWorkoutVariables.GOAL_TYPE_TIME || outputGoalType == ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE ? seconds  * 1000: convert(ConsoleWorkoutVariables.GOAL_TYPE_TIME, outputGoalType, seconds);
            }
            case ConsoleWorkoutVariables.GOAL_TYPE_CALORIES:
            case ConsoleWorkoutVariables.GOAL_TYPE_CALORIES_STAGE: {
                float seconds = goalValue / DataMatrix.getFloat(HALKeys.DATA_CALORIES_PER_SEC_FLOAT);
                return outputGoalType == ConsoleWorkoutVariables.GOAL_TYPE_TIME || outputGoalType == ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE ? seconds  * 1000: convert(ConsoleWorkoutVariables.GOAL_TYPE_TIME, outputGoalType, seconds);
            }
            case ConsoleWorkoutVariables.GOAL_TYPE_FLOORS:
            case ConsoleWorkoutVariables.GOAL_TYPE_FLOORS_STAGE:{
                float seconds = goalValue / (DataMatrix.getInt(HALKeys.DATA_SPM_INT) / 60.0f / FrameConfigManager.getInstance().getFrameConfig().STEPS_PER_FLOOR);
                return outputGoalType == ConsoleWorkoutVariables.GOAL_TYPE_TIME || outputGoalType == ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE ? seconds : convert(ConsoleWorkoutVariables.GOAL_TYPE_TIME, outputGoalType, seconds);
            }
            case ConsoleWorkoutVariables.GOAL_TYPE_ELEVATION:
            case ConsoleWorkoutVariables.GOAL_TYPE_ELEVATION_STAGE: {
                float seconds;
                if(FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Stepper) || FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill)) {
                    seconds = goalValue / (DataMatrix.getInt(HALKeys.DATA_SPM_INT) / 60.0f / FrameConfigManager.getInstance().getFrameConfig().STEPS_PER_METER);
                }
                else if(FrameConfigManager.getInstance().getFrameConfig().machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill)) {
                    seconds = goalValue / (float)(getWorkoutKPH() / 3.6f * Math.sin(Math.atan(DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT) / 100.0f)));
                }
                else {
                    return goalValue;
                }
                return outputGoalType == ConsoleWorkoutVariables.GOAL_TYPE_TIME || outputGoalType == ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE ? seconds * 1000 : convert(ConsoleWorkoutVariables.GOAL_TYPE_TIME, outputGoalType, seconds);
            }
            case ConsoleWorkoutVariables.GOAL_TYPE_STEPS:
            case ConsoleWorkoutVariables.GOAL_TYPE_STEPS_STAGE: {

                float seconds = goalValue / (DataMatrix.getInt(HALKeys.DATA_SPM_INT) / 60.0f);

                return outputGoalType == ConsoleWorkoutVariables.GOAL_TYPE_TIME || outputGoalType == ConsoleWorkoutVariables.GOAL_TYPE_TIME_STAGE ? seconds * 1000: convert(ConsoleWorkoutVariables.GOAL_TYPE_TIME, outputGoalType, seconds);
            }
        }
        return goalValue;
    }
}
