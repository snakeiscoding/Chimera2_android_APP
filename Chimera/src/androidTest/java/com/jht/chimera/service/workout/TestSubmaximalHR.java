package com.jht.chimera.service.workout;

import androidx.test.platform.app.InstrumentationRegistry;

import com.jht.chimera.R;
import com.jht.chimera.lib.workout.ConsoleWorkoutVariables;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.Default;
import com.jht.workoutcontrollerlib.parser.ScriptException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the Climbmill submaximal fitness test.  This focuses on the workout script.  This workout
 * has a lot of edge cases.  Here we basically need to test every path in the script and make sure
 * the workout outputs the proper score.
 */
public class TestSubmaximalHR {


    @Before
    public void setUp() {
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation().getContext());
        Assert.assertNotNull(InstrumentationRegistry.getInstrumentation().getTargetContext());
        
    }

    private int vo2ToMaleScore(float vo2) {
        if(vo2 >= 50.3f) return 4;
        else if(vo2 >= 44.5f) return 3;
        else if(vo2 >= 40.9f) return 2;
        else if(vo2 >= 37.3f) return 1;
        return 0;
    }

    private int vo2ToFemaleScore(float vo2) {
        if(vo2 >= 40.9f) return 4;
        else if(vo2 >= 36.6f) return 3;
        else if(vo2 >= 33.7f) return 2;
        else if(vo2 >= 30.4f) return 1;
        return 0;
    }

    @Test @Emulator @Default @Chimera
    public void testSubmaximalWorkoutNoHR() {
        try (WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.addTranslatedValue("TARGET_METS", "METS");
            testFixture.loadScript(R.raw.submax_fitness_test);

            testFixture.addCheck(Integer.MAX_VALUE, "SUBMAX_OUTPUT", 1.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "INTERVAL_TIME_MS", 3 * 60 * 1000, 0.001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TARGET_METS", 4.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", 0, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", 0, 0.0001f);
            
            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("METS", 1);
            testFixture.updateInput("GENDER", ConsoleWorkoutVariables.GENDER_MALE_CONSTANT);
            testFixture.updateInput("HEART_RATE", 0);
            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

    @Test @Emulator @Default @Chimera
    public void testSubmaximalHRFailStage1() {
        try (WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.addTranslatedValue("TARGET_METS", "METS");
            testFixture.loadScript(R.raw.submax_fitness_test);

            testFixture.addCheck(Integer.MAX_VALUE, "SUBMAX_OUTPUT", 2.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "INTERVAL_TIME_MS", 3 * 60 * 1000, 0.001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TARGET_METS", 4.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", 0, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", 0, 0.0001f);
            
            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("METS", 1);
            testFixture.updateInput("GENDER", ConsoleWorkoutVariables.GENDER_MALE_CONSTANT);
            testFixture.updateInput("HEART_RATE", 65);
            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

    @Test @Emulator @Default @Chimera
    public void testSubmaximalHRFailStage2() {
        try (WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.addTranslatedValue("TARGET_METS", "METS");
            testFixture.loadScript(R.raw.submax_fitness_test);

            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("METS", 1);
            testFixture.updateInput("GENDER", ConsoleWorkoutVariables.GENDER_MALE_CONSTANT);
            testFixture.updateInput("HEART_RATE", 65);
            testFixture.injectInput(2 * 60 * 1000 + 2000, "HEART_RATE", 68);
            testFixture.addCheck(Integer.MAX_VALUE, "SUBMAX_OUTPUT", 2.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "INTERVAL_TIME_MS", (3 + 3) * 60 * 1000, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TARGET_METS", 7.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", 0, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", 0, 0.0001f);
            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

    @Test @Emulator @Default @Chimera
    public void testSubmaximalHRFailStage2_2() {
        try (WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.addTranslatedValue("TARGET_METS", "METS");
            testFixture.loadScript(R.raw.submax_fitness_test);

            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("METS", 1);
            testFixture.updateInput("GENDER", ConsoleWorkoutVariables.GENDER_MALE_CONSTANT);
            testFixture.updateInput("HEART_RATE", 100);
            testFixture.injectInput(2 * 60 * 1000 + 2000, "HEART_RATE", 103);
            testFixture.addCheck(Integer.MAX_VALUE, "SUBMAX_OUTPUT", 2.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "INTERVAL_TIME_MS", (3 + 3) * 60 * 1000, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TARGET_METS", 6.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", 0, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", 0, 0.0001f);
            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

    @Test @Emulator @Default @Chimera
    public void testSubmaximalHRFailStage2_3() {
        try (WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.addTranslatedValue("TARGET_METS", "METS");
            testFixture.loadScript(R.raw.submax_fitness_test);

            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("METS", 1);
            testFixture.updateInput("GENDER", ConsoleWorkoutVariables.GENDER_MALE_CONSTANT);
            testFixture.updateInput("HEART_RATE", 120);
            testFixture.injectInput(2 * 60 * 1000 + 2000, "HEART_RATE", 123);
            testFixture.addCheck(Integer.MAX_VALUE, "SUBMAX_OUTPUT", 2.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "INTERVAL_TIME_MS", (3 + 3) * 60 * 1000, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TARGET_METS", 5.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", 0, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", 0, 0.0001f);
            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

    @Test @Emulator @Default @Chimera
    public void testSubmaximalHRFailStageHR() {
        try (WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.addTranslatedValue("TARGET_METS", "METS");
            testFixture.loadScript(R.raw.submax_fitness_test);

            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("METS", 1);
            testFixture.updateInput("GENDER", ConsoleWorkoutVariables.GENDER_MALE_CONSTANT);
            testFixture.updateInput("HEART_RATE", 80);
            testFixture.injectInput(2 * 60 * 1000 + 2000, "HEART_RATE", 86);
            testFixture.injectInput(3 * 60 * 1000 + 2000, "HEART_RATE", 96);
            testFixture.injectInput(4 * 60 * 1000 + 2000, "HEART_RATE", 106);
            testFixture.addCheck(Integer.MAX_VALUE, "SUBMAX_OUTPUT", 4.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "INTERVAL_TIME_MS", (5) * 60 * 1000, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TARGET_METS", 4.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", 0, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", 0, 0.0001f);

            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

    @Test @Emulator @Default @Chimera
    public void testSubmaximalSuccess() {
        try (WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.addTranslatedValue("TARGET_METS", "METS");
            testFixture.loadScript(R.raw.submax_fitness_test);

            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("METS", 1);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("GENDER", ConsoleWorkoutVariables.GENDER_MALE_CONSTANT);
            testFixture.updateInput("HEART_RATE", 80);
            testFixture.injectInput(2 * 60 * 1000 + 2000, "HEART_RATE", 86);
            testFixture.injectInput(3 * 60 * 1000 + 2000, "HEART_RATE", 102);
            testFixture.injectInput(4 * 60 * 1000 + 2000, "HEART_RATE", 106);
            testFixture.injectInput(6 * 60 * 1000 + 2000, "HEART_RATE", 112);
            testFixture.injectInput(7 * 60 * 1000 + 2000, "HEART_RATE", 116);
            testFixture.injectInput(9 * 60 * 1000 + 2000, "HEART_RATE", 120);
            testFixture.injectInput(10 * 60 * 1000 + 2000, "HEART_RATE", 123);

            testFixture.addCheck(Integer.MAX_VALUE, "SUBMAX_OUTPUT", 0.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TARGET_METS", 8.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "INTERVAL_TIME_MS", (11) * 60 * 1000, 0.0001f);

            float maxHR = 215 - (30 * 0.75f);
            float slope = (123 - 116) / 2.0f;
            float mets = (maxHR - 123) / slope + 8.0f;
            testFixture.addCheck(Integer.MAX_VALUE, "USER_MAX_HEART_RATE", maxHR, 0.001f);
            testFixture.addCheck(Integer.MAX_VALUE, "slope", slope, 0.001f);
            testFixture.addCheck(Integer.MAX_VALUE, "metsMax", mets, 0.001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE", 4.0f, 0.001f);
            testFixture.addCheck(Integer.MAX_VALUE, "VO2", mets * 3.5f, 0.001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", vo2ToMaleScore(mets * 3.5f), 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", vo2ToFemaleScore(mets * 3.5f), 0.0001f);

            testFixture.runWorkout();


        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }



    @Test @Emulator @Default @Chimera
    public void testSubmaximalSuccess_2() {
        try (WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.addTranslatedValue("TARGET_METS", "METS");
            testFixture.loadScript(R.raw.submax_fitness_test);

            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("METS", 1);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("GENDER", ConsoleWorkoutVariables.GENDER_MALE_CONSTANT);
            testFixture.updateInput("HEART_RATE", 80);
            testFixture.injectInput(2 * 60 * 1000 + 2000, "HEART_RATE", 86);
            testFixture.injectInput(3 * 60 * 1000 + 2000, "HEART_RATE", 88);
            testFixture.injectInput(5 * 60 * 1000 + 2000, "HEART_RATE", 115);
            testFixture.injectInput(6 * 60 * 1000 + 2000, "HEART_RATE", 118);
            testFixture.injectInput(8 * 60 * 1000 + 2000, "HEART_RATE", 119);
            testFixture.injectInput(9 * 60 * 1000 + 2000, "HEART_RATE", 120);

            float maxHR = 215 - (30 * 0.75f);
            float slope = (123 - 116) / 2.0f;
            float mets = (maxHR - 123) / slope + 8.0f;

            testFixture.addCheck(Integer.MAX_VALUE, "SUBMAX_OUTPUT", 0.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TARGET_METS", 9.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "INTERVAL_TIME_MS", (10) * 60 * 1000, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", vo2ToMaleScore(mets * 3.5f), 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", vo2ToFemaleScore(mets * 3.5f), 0.0001f);
            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }

    // Need a test where we fail at the end stage.

    @Test @Emulator @Default @Chimera
    public void testSubmaximalHRBelow115() {
        try (WorkoutControllerTestFixture testFixture = new WorkoutControllerTestFixture()) {
            testFixture.addTranslatedValue("TARGET_METS", "METS");
            testFixture.loadScript(R.raw.submax_fitness_test);


            testFixture.updateInput("STAGE_TIME_MS", 0);
            testFixture.updateInput("METS", 1);
            testFixture.updateInput("AGE", 30);
            testFixture.updateInput("GENDER", ConsoleWorkoutVariables.GENDER_MALE_CONSTANT);
            testFixture.updateInput("HEART_RATE", 30);
            testFixture.injectInput(2 * 60 * 1000 + 2000, "HEART_RATE", 50);
            testFixture.injectInput(3 * 60 * 1000 + 2000, "HEART_RATE", 52);
            testFixture.injectInput(5 * 60 * 1000 + 2000, "HEART_RATE", 60);
            testFixture.injectInput(6 * 60 * 1000 + 2000, "HEART_RATE", 62);
            testFixture.injectInput(8 * 60 * 1000 + 2000, "HEART_RATE", 70);
            testFixture.injectInput(9 * 60 * 1000 + 2000, "HEART_RATE", 72);
            testFixture.addCheck(Integer.MAX_VALUE, "SUBMAX_OUTPUT", 3.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TARGET_METS", 9.0f, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "INTERVAL_TIME_MS", (10) * 60 * 1000, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_MALE", 0, 0.0001f);
            testFixture.addCheck(Integer.MAX_VALUE, "TEST_SCORE_FEMALE", 0, 0.0001f);
            testFixture.runWorkout();

        } catch (ScriptException e) {
            Assert.fail("Error in script " + e.getMessage());
        }
    }
    // Need to test out scores.
}
