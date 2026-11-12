package com.jht.chimera.lib.common.conversionlib;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.datamatrixlib.DataMatrix;

import org.junit.Assert;
import org.junit.Test;

public class GoalConverterTest {

    /**
     * Test out workout goal conversions from calories and distance to workout time.
     *
     * @test_name       "Workout Goal Converter"
     */
    @Test @Emulator @Default @Chimera
    public void convert() {
        DataMatrix.putFloat(HALKeys.DATA_CALORIES_PER_SEC_FLOAT, 5.0f);
        Assert.assertEquals(12 * 1000, GoalConverter.convert(ConsoleWorkoutVariables.GOAL_TYPE_CALORIES, ConsoleWorkoutVariables.GOAL_TYPE_TIME, 60.0f), 0.1);
        DataMatrix.putFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, 5.0f);
        DataMatrix.putFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, 5.0f);
        Assert.assertEquals(30*60 * 1000, GoalConverter.convert(ConsoleWorkoutVariables.GOAL_TYPE_DISTANCE, ConsoleWorkoutVariables.GOAL_TYPE_TIME, 2.5f), 0.1);
    }
}