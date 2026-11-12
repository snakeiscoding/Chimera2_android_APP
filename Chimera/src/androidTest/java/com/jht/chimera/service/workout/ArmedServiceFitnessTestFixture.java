package com.jht.chimera.service.workout;

import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.workoutcontrollerlib.parser.ScriptException;

/**
 */
public class ArmedServiceFitnessTestFixture extends WorkoutControllerTestFixture {

    private final float distance;
    private final int scriptId;
    public ArmedServiceFitnessTestFixture(int scriptId, float distance) {
        this.distance = distance;
        this.scriptId = scriptId;
        setFrame(FrameConfigEnum.Treadmill_Performance);
    }

    public void runTest(int age, int workoutTime, float maleScore, float femaleScore) throws ScriptException {
        runTest(age, workoutTime, maleScore, femaleScore, 0, -1);
    }

    public void runTest(int age, int workoutTime, float maleScore, float femaleScore, int warmupTime, int elevation) throws ScriptException {
        loadScript(scriptId);

        assertHasInput("COOLDOWN_TIME_START");
        assertHasInput("DISTANCE");
        assertHasInput("SPEED_IN");
        assertHasInput("GOAL");
        assertHasInput("GENDER");
        assertHasInput("AGE");
        if(elevation >= 0) {
            assertHasInput("FRAME_ELEVATION");
            updateInput("FRAME_ELEVATION", elevation);
        }


        assertHasOutput("INCLINE");
        assertHasOutput("GOAL_TYPE");
        assertHasOutput("SPEED");
        assertHasOutput("GOAL");
        assertHasOutput("TEST_SCORE_MALE");
        assertHasOutput("TEST_SCORE_FEMALE");
        assertHasOutput("ALLOW_PAUSE");

        float speed = distance / (workoutTime / 3600f);
        updateInput("COOLDOWN_TIME_START", 2 * 60 * 1000);
        updateInput("GOAL", distance);
        updateInput("DISTANCE", 0.0f);
        updateInput("SPEED_IN", speed);
        updateInput("GENDER", 2);
        updateInput("AGE", age);

        addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", maleScore, 0.01f);
        addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", femaleScore, 0.01f);

        for(int i = 0; i < workoutTime - 1; i++) {
            injectInput((i + 1 + warmupTime) * 1000, "DISTANCE", distance / workoutTime * i);
        }
        injectInput((workoutTime + warmupTime) * 1000, "DISTANCE", distance);

        runWorkout();
    }

}
