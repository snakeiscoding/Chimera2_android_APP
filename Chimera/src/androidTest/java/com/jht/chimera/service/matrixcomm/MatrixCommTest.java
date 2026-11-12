package com.jht.chimera.service.matrixcomm;

import static com.jht.chimera.lib.common.keypress.KeyPress.KeyState.KEY_DOWN;
import static com.jht.chimera.lib.common.keypress.KeyPress.KeyState.KEY_UP;
import static com.jht.chimera.service.matrixcomm.messages.requests.SetWorkoutParameterCommand.WORKOUT_PARAMETER.INCLINE;
import static com.jht.chimera.service.matrixcomm.messages.requests.SetWorkoutParameterCommand.WORKOUT_PARAMETER.RESISTANCE;
import static com.jht.chimera.service.matrixcomm.messages.requests.SetWorkoutParameterCommand.WORKOUT_PARAMETER.SPEED;
import static com.jht.chimera.service.matrixcomm.messages.requests.StartWorkoutCommand.GOAL_TYPE.CALORIE;
import static com.jht.chimera.service.matrixcomm.messages.requests.StartWorkoutCommand.GOAL_TYPE.DISTANCE;
import static com.jht.chimera.service.matrixcomm.messages.requests.StartWorkoutCommand.GOAL_TYPE.INVALID;
import static com.jht.chimera.service.matrixcomm.messages.requests.StartWorkoutCommand.GOAL_TYPE.TIME;

import com.jht.androidcommonalgorithms.util.ThreadHelper;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.keypress.KeyPress;
import com.jht.chimera.service.matrixcomm.messages.events.EStopEvent;
import com.jht.chimera.service.matrixcomm.messages.events.EventBase;
import com.jht.chimera.service.matrixcomm.messages.events.FullScreenEvent;
import com.jht.chimera.service.matrixcomm.messages.events.KeyEvent;
import com.jht.chimera.service.matrixcomm.messages.events.RFIDEvent;
import com.jht.chimera.service.matrixcomm.messages.events.WorkoutStateEvent;
import com.jht.chimera.service.matrixcomm.messages.requests.BeepCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.ControlStateCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.ControlStateResponse;
import com.jht.chimera.service.matrixcomm.messages.requests.ErrorResponse;
import com.jht.chimera.service.matrixcomm.messages.requests.FanCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.FeatureCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.GeneralResponse;
import com.jht.chimera.service.matrixcomm.messages.requests.GetEStopStateRequest;
import com.jht.chimera.service.matrixcomm.messages.requests.GetEquipmentInfoRequest;
import com.jht.chimera.service.matrixcomm.messages.requests.GetEquipmentInfoResponse;
import com.jht.chimera.service.matrixcomm.messages.requests.RFIDCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.SetUserInfoCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.SetWorkoutParameterCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.StartWorkoutCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.UpdateGoalCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.WorkoutControlCommand;
import com.jht.chimera.test.annotations.platform.Chimera;
import com.jht.chimera.test.annotations.platform.Emulator;
import com.jht.chimera.test.annotations.runner.SplashApp;
import com.jht.chimera.test.fixtures.MatrixCommTestFixture;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.rfid.Legic.RFIDChip;
import com.jht.rfid.LegicEnums.RfStandard;

import org.junit.Assert;
import org.junit.Test;

public class MatrixCommTest {

    /**
     * Test the feature command.  Make sure we can use it to go to machine settings and to the
     * welcome screen.  End at the Android launcher.
     *
     */
    @Test @Chimera @Emulator @SplashApp
    public void testFeature() {
        try (MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            testFixture.checkResponse(new GeneralResponse(new FeatureCommand(FeatureCommand.FEATURE.SETUP), "ok"));
            testFixture.waitForView("machine_settings_title");

            DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_SHOW_WELCOME_SCREEN_BOOL, true);
            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, true);
            testFixture.checkResponse(new GeneralResponse(new FeatureCommand(FeatureCommand.FEATURE.HOME), "ok"));
            testFixture.waitForView("welcome_guest_button");

            testFixture.checkResponse(new GeneralResponse(new FeatureCommand(FeatureCommand.FEATURE.INVALID), "invalid feature"));

            testFixture.checkResponse(new GeneralResponse(new FeatureCommand(FeatureCommand.FEATURE.ANDROID), "ok"));
        }
    }

    /**
     * Test the get equipment info command.  Ensure the data passed match what is in the system.
     * Here we check each frame type.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testEquipmentInfo() {
        FrameConfigEnum[] framesToTest = {
                FrameConfigEnum.Treadmill_Performance,
                FrameConfigEnum.Ascent_Performance,
                FrameConfigEnum.Elliptical_Performance,
                FrameConfigEnum.UprightBike_Performance,
                FrameConfigEnum.HybridBike_Performance,
                FrameConfigEnum.RecumbentBike_Performance,
                FrameConfigEnum.Stepper_Endurance,
                FrameConfigEnum.Climbmill_Performance,
                FrameConfigEnum.CardioSpinBike
        };
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            for(FrameConfigEnum frameConfig : framesToTest) {
                testFixture.setFrame(frameConfig);
                testFixture.checkResponse(new GetEquipmentInfoResponse(new GetEquipmentInfoRequest()));
            }
        }
    }

    /**
     * Test the beep command.  We can't really check to see if the console beeps, but we can
     * make sure the API returns the correct value.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testBeep() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.checkResponse(new GeneralResponse(new BeepCommand(), "ok"));
        }
    }

    /**
     * Test the set workout parameter.  This is used to set speed, incline, or resistance during
     * a workout.  We check treadmill, bike, ascent, and elliptical to make sure the correct
     * parameters are available.  We run a manual workout before each test.
     *
     */
    @Test @Chimera @Emulator @SplashApp
    public void testSetWorkoutParameter() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            // Not in workout.
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, -1), "invalid value"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SPEED, 1), "invalid value"));

            testFixture.startNormalWorkout();
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SPEED, 1.5), "ok"));
            testFixture.waitForSystem();
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SPEED, 0.3), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SPEED, 30.0), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 5.5), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 5.3), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, -1), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 30.0), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 1), "invalid value"));
            stopWorkout();

            testFixture.setFrame(FrameConfigEnum.Ascent_Performance);
            // Not in workout.
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 4), "invalid value"));
            testFixture.startNormalWorkout();
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 4), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, -1), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 40), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 50), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 53), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, -1), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 110), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SPEED, 1), "invalid value"));
            stopWorkout();

            testFixture.setFrame(FrameConfigEnum.Elliptical_Endurance);
            testFixture.startNormalWorkout();
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 4), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, -1), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 40), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 1), "invalid value"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SPEED, 1), "invalid value"));
            stopWorkout();

            testFixture.setFrame(FrameConfigEnum.HybridBike_Performance);
            testFixture.startNormalWorkout();
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 4), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, -1), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 40), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 1), "invalid value"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SetWorkoutParameterCommand.WORKOUT_PARAMETER.SPEED, 1), "invalid value"));
            stopWorkout();

            testFixture.setFrame(FrameConfigEnum.Climbmill_Performance);
            testFixture.startNormalWorkout();
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 4), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, -1), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 40), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 1), "invalid value"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SPEED, 1), "invalid value"));
            stopWorkout();

            testFixture.setFrame(FrameConfigEnum.Stepper_Endurance);
            testFixture.startNormalWorkout();
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 4), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, -1), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 40), "out of range"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 1), "invalid value"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SPEED, 1), "invalid value"));
            testFixture.checkResponse(new ErrorResponse(new SetWorkoutParameterCommand(SetWorkoutParameterCommand.WORKOUT_PARAMETER.INVALID, 1), "unsupported workout parameter: 'invalid'"));
            stopWorkout();

        }
    }

    /**
     * Test the start workout command given it invalid values and making sure we get the proper error.
     * Basically make sure the inputs are out of range.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testInvalidStartWorkout() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            testFixture.checkResponse(new ErrorResponse(new StartWorkoutCommand(TIME, 0, 0, 0, true), "invalid time"));
            testFixture.checkResponse(new ErrorResponse(new StartWorkoutCommand(TIME, 6 * 60 * 60, 0, 0, true), "invalid time"));
            testFixture.checkResponse(new ErrorResponse(new StartWorkoutCommand(DISTANCE, 0, 0, 0, true), "invalid distance"));
            testFixture.checkResponse(new ErrorResponse(new StartWorkoutCommand(DISTANCE, 0, 300, 0, true), "invalid distance"));
            testFixture.checkResponse(new ErrorResponse(new StartWorkoutCommand(CALORIE, 0, 0, -1, true), "invalid calories"));
            testFixture.checkResponse(new ErrorResponse(new StartWorkoutCommand(CALORIE, 0, 0, 3000, true), "invalid calories"));
            DataMatrix.putBoolean(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL, true);
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, false);
            testFixture.waitForSystem();
            testFixture.checkResponse(new ErrorResponse(new StartWorkoutCommand(TIME, 30 * 60, 0, 0, true), "cannot start workout"));
            DataMatrix.putBoolean(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL, false);
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);

            testFixture.checkResponse(new ErrorResponse(new StartWorkoutCommand(INVALID, 0, 0, 0, true), "unrecognized goaltype: 'invalid'"));
        }
    }

    /**
     * Test the start workout command making sure we can start a workout on a treadmill,
     * ascent, and bike.  Once the workout is started try to update the goal and change the speed,
     * incline, and the resistance.  Ensure those have properly updated the system.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testValidStartWorkout() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            testFixture.startNormalWorkout();
            Assert.assertEquals(WorkoutKeys.Constants.GOAL_TYPE_TIME_STAGE, DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT));
            Assert.assertEquals(30 * 60 * 1000, DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_FLOAT), 0.1);
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SPEED, 1.5), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(SPEED, 1.5), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 5.5), "ok"));
            testFixture.waitForSystem();
            Assert.assertEquals(5.5, DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT), 0.1);
            Assert.assertEquals(1.5, DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT), 0.1);
            testFixture.checkResponse(new ErrorResponse(new UpdateGoalCommand(60 * 300, -1, -1), "invalid time"));
            testFixture.checkResponse(new GeneralResponse(new UpdateGoalCommand(20 * 60, -1, -1), "ok"));
            testFixture.waitForSystem();
            Assert.assertEquals(20 * 60 * 1000, DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_FLOAT), 0.1);
            stopWorkout();

            testFixture.setFrame(FrameConfigEnum.Ascent_Performance);
            testFixture.checkResponse(new GeneralResponse(new StartWorkoutCommand(DISTANCE, 30 * 60, 20, 0, true), "ok"));
            ThreadHelper.sleepIgnoreInterruption(7000);
            Assert.assertEquals(WorkoutKeys.Constants.GOAL_TYPE_DISTANCE_STAGE, DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT));
            Assert.assertEquals(20, DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_FLOAT), 0.1);
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 5), "ok"));
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(INCLINE, 60), "ok"));
            testFixture.waitForSystem();
            Assert.assertEquals(60, DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT), 0.1);
            Assert.assertEquals(5, DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT));
            testFixture.checkResponse(new ErrorResponse(new UpdateGoalCommand(-1, 500, -1),  "invalid distance"));
            testFixture.checkResponse(new GeneralResponse(new UpdateGoalCommand(-1, 10, -1),  "ok"));
            testFixture.waitForSystem();
            Assert.assertEquals(10, DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_FLOAT), 0.1);
            stopWorkout();

            testFixture.setFrame(FrameConfigEnum.CardioSpinBike);
            testFixture.checkResponse(new GeneralResponse(new StartWorkoutCommand(CALORIE, 30 * 60, 20, 500, true), "ok"));
            ThreadHelper.sleepIgnoreInterruption(7000);
            Assert.assertEquals(WorkoutKeys.Constants.GOAL_TYPE_CALORIES_STAGE, DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT));
            Assert.assertEquals(500, DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_FLOAT), 0.1);
            testFixture.checkResponse(new GeneralResponse(new SetWorkoutParameterCommand(RESISTANCE, 15), "ok"));
            testFixture.waitForSystem();
            Assert.assertEquals(15, DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT));
            testFixture.checkResponse(new ErrorResponse(new UpdateGoalCommand(-1, -1, 50000),  "invalid calories"));
            testFixture.checkResponse(new GeneralResponse(new UpdateGoalCommand(-1, -1, 600),  "ok"));
            testFixture.waitForSystem();
            Assert.assertEquals(600, DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_WORKOUT_STAGE_FLOAT), 0.1);
            stopWorkout();
        }
    }


    /**
     * Test the workout control message making sure we can pause, resume, and stop the workout.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testWorkoutControl() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            testFixture.startNormalWorkout();
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_RUNNING, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
            testFixture.checkResponse(new GeneralResponse(new WorkoutControlCommand(WorkoutControlCommand.ACTION.PAUSE),  "ok"));
            testFixture.waitForSystem();
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_PAUSE, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
            DataMatrix.putBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL, false);
            testFixture.checkResponse(new GeneralResponse(new WorkoutControlCommand(WorkoutControlCommand.ACTION.RESUME),  "cannot resume workout"));
            DataMatrix.putBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL, true);
            testFixture.checkResponse(new GeneralResponse(new WorkoutControlCommand(WorkoutControlCommand.ACTION.RESUME),  "ok"));
            testFixture.waitForSystem();
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_RUNNING, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
            testFixture.checkResponse(new GeneralResponse(new WorkoutControlCommand(WorkoutControlCommand.ACTION.STOP),  "ok"));
            ThreadHelper.sleepIgnoreInterruption(500);
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_STOP, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
            ThreadHelper.sleepIgnoreInterruption(1000);

            testFixture.checkPassiveWorkout(WorkoutControlCommand.ACTION.PASSIVESTART, true);
            testFixture.checkPassiveWorkout(WorkoutControlCommand.ACTION.PASSIVESTOP, false);
            testFixture.checkResponse(new GeneralResponse(new WorkoutControlCommand(WorkoutControlCommand.ACTION.INVALID),  "invalid action"));

            testFixture.setFrame(FrameConfigEnum.Climbmill_Performance);
            testFixture.startNormalWorkout();
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_RUNNING, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
            testFixture.checkResponse(new GeneralResponse(new WorkoutControlCommand(WorkoutControlCommand.ACTION.PAUSE),  "ok"));
            testFixture.waitForSystem();
            Assert.assertEquals(WorkoutKeys.Constants.RUN_STATE_PAUSE, DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT));
            DataMatrix.putBoolean(HALKeys.DATA_CLIMB_START_CONTROL_STATUS_BOOL, true);
            testFixture.checkResponse(new GeneralResponse(new WorkoutControlCommand(WorkoutControlCommand.ACTION.RESUME),  "cannot resume workout"));
            DataMatrix.putBoolean(HALKeys.DATA_CLIMB_START_CONTROL_STATUS_BOOL, false);
            testFixture.checkResponse(new GeneralResponse(new WorkoutControlCommand(WorkoutControlCommand.ACTION.RESUME),  "ok"));
            testFixture.waitForSystem();
            stopWorkout();
            LaunchAppUtil.launchAndroidLauncher();
        }
    }

    /**
     * Test the user info call making sure it sets the current user weight.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testUserWeight() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.checkResponse(new GeneralResponse(new SetUserInfoCommand(113), "ok"));
            testFixture.waitForSystem();
            Assert.assertEquals(113, DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT), 0.1);
            testFixture.checkResponse(new ErrorResponse(new SetUserInfoCommand(500), "invalid weight"));
        }
    }

    /**
     * Test the get estop state making sure we can request the state correctly.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testGetEStopState() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, false);
            DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, 1);
            testFixture.checkResponse(new GeneralResponse(new GetEStopStateRequest(), "set"));
            DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, 0);
            testFixture.checkResponse(new GeneralResponse(new GetEStopStateRequest(), "reset"));
            DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, 0);
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
            ThreadHelper.sleepIgnoreInterruption(500);
            LaunchAppUtil.launchAndroidLauncher();
        }
    }

    /**
     * Test the workout control state.  Make sure it handles speed, incline, and resistance changes
     * correctly and errors out correctly when it should.
     *
     */
    @Test @Chimera @Emulator @SplashApp
    public void testWorkoutControlState() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, true);
            testFixture.startNormalWorkout();
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.SPEEDUP), "ok", 0.9));
            testFixture.waitForSystem();
            Assert.assertEquals(0.9, DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT), 0.1);
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.SPEEDDOWN), "ok", 0.8));
            testFixture.waitForSystem();
            Assert.assertEquals(0.8, DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT), 0.1);
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.SPEEDDOWN), "at limit", 0.8));
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.INCLINEUP), "ok", 0.5));
            testFixture.waitForSystem();
            Assert.assertEquals(0.5, DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT), 0.1);
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.INCLINEDOWN), "ok", 0));
            testFixture.waitForSystem();
            Assert.assertEquals(0, DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT), 0.1);
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.INCLINEDOWN), "at limit", 0));
            testFixture.checkResponse(new ErrorResponse(new ControlStateCommand(ControlStateCommand.ACTION.RESISTANCEUP), "resistance not available"));
            testFixture.checkResponse(new ErrorResponse(new ControlStateCommand(ControlStateCommand.ACTION.RESISTANCEDOWN), "resistance not available"));
            stopWorkout();

            testFixture.setFrame(FrameConfigEnum.Ascent_Performance);
            testFixture.startNormalWorkout();
            testFixture.checkResponse(new ErrorResponse(new ControlStateCommand(ControlStateCommand.ACTION.SPEEDUP), "speed not available"));
            testFixture.checkResponse(new ErrorResponse(new ControlStateCommand(ControlStateCommand.ACTION.SPEEDDOWN), "speed not available"));
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.INCLINEUP), "ok", 5));
            ThreadHelper.sleepIgnoreInterruption(500);
            Assert.assertEquals(5, DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT), 0.1);
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.INCLINEDOWN), "ok", 0));
            testFixture.waitForSystem();
            Assert.assertEquals(0, DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT), 0.1);
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.INCLINEDOWN), "at limit", 0));
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.RESISTANCEUP), "ok", 2));
            testFixture.waitForSystem();
            Assert.assertEquals(2, DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT), 0.1);
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.RESISTANCEDOWN), "ok", 1));
            testFixture.waitForSystem();
            Assert.assertEquals(1, DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT), 0.1);
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.RESISTANCEDOWN), "at limit", 1));
            stopWorkout();

            testFixture.setFrame(FrameConfigEnum.CardioSpinBike);
            testFixture.startNormalWorkout();
            testFixture.checkResponse(new ErrorResponse(new ControlStateCommand(ControlStateCommand.ACTION.SPEEDUP), "speed not available"));
            testFixture.checkResponse(new ErrorResponse(new ControlStateCommand(ControlStateCommand.ACTION.SPEEDDOWN), "speed not available"));
            testFixture.checkResponse(new ErrorResponse(new ControlStateCommand(ControlStateCommand.ACTION.INCLINEUP), "incline not available"));
            testFixture.checkResponse(new ErrorResponse(new ControlStateCommand(ControlStateCommand.ACTION.INCLINEDOWN), "incline not available"));
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.RESISTANCEUP), "ok", 2));
            testFixture.waitForSystem();
            Assert.assertEquals(2, DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT), 0.1);
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.RESISTANCEDOWN), "ok", 1));
            testFixture.waitForSystem();
            Assert.assertEquals(1, DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT), 0.1);
            testFixture.checkResponse(new ControlStateResponse(new ControlStateCommand(ControlStateCommand.ACTION.RESISTANCEDOWN), "at limit", 1));
            stopWorkout();
        }
    }

    /**
     * Make sure estop events are send when they should be.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testEStopEvent() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {

            DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, 0);
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            testFixture.checkEvent(new EStopEvent(true), () -> {
                DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, false);
                DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, 1);
            });
            testFixture.checkEvent(new EStopEvent(false), () -> {
                DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, 0);
                DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
            });
        }
        finally {
            DataMatrix.putInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, 0);
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);

        }
    }

    /**
     * Make sure the full screen event is sent correctly when the workout panels change state.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testFullScreenEvent() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            testFixture.checkEvent(new FullScreenEvent("true"), () -> DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.FULLSCREEN));
            testFixture.checkEvent(new FullScreenEvent("false"), () -> DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR));
        }
        finally {
            DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.IDLE);

        }
    }

    /**
     * Make sure we can detect key events.  For this test we will inject key events and make sure
     * the message gets here.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testKeyEvent() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            testFixture.checkEvent(new KeyEvent("speedup", KeyEvent.KeyState.PRESSED), () -> DataMatrix.putInt(HALKeys.EVENT_KEYPRESS_BITMAP_INT, KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_SPEED_UP, KEY_DOWN)));
            testFixture.checkEvent(new KeyEvent("speedup", KeyEvent.KeyState.RELEASED), () -> DataMatrix.putInt(HALKeys.EVENT_KEYPRESS_BITMAP_INT, KeyPress.createBitmapKey(KeyPress.KeyCode.KEY_SPEED_UP, KEY_UP)));
        }
    }


    /**
     * Make sure we can detect key events.  For this test we will inject key events and make sure
     * the message gets here.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testRFIDEvent() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);
            RFIDChip testChip = new RFIDChip(RfStandard.LegicRf, new byte[] {0x01, 0x02, 0x03, 0x04});
            testFixture.checkEvent(new RFIDEvent(testChip), () -> DataMatrix.putParcelable(HALKeys.EVENT_RFID_TAG_DETECTED_PARCELABLE, testChip));
        }
    }

    /**
     * Test the workout event making sure the data matches the system.  Here we start a workout,
     * wait for a bit, and then check the next workout state message.
     */
    @Test @Chimera @Emulator @SplashApp
    public void testWorkoutEvent() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.setFrame(FrameConfigEnum.Treadmill_Performance);

            testFixture.startNormalWorkout();
            // Note we will check the event against the system afterwards.  Cannot fully predict before
            // event occurs.
            EventBase event = testFixture.checkEvent(new WorkoutStateEvent(
                    WorkoutStateEvent.WorkoutState.RUN,
                    WorkoutStateEvent.WorkoutStage.WORKOUT), () -> {});
            Assert.assertTrue(event instanceof WorkoutStateEvent);
            WorkoutStateEvent workoutEvent = (WorkoutStateEvent) event;
            Assert.assertEquals(DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) / 1000, workoutEvent.time());
            Assert.assertEquals(0, workoutEvent.pauseTime());
            Assert.assertEquals(DataMatrix.getFloat(WorkoutKeys.DATA_DISTANCE_KM_FLOAT), workoutEvent.distance(), 0.1);
            Assert.assertEquals(Math.floor(DataMatrix.getFloat(WorkoutKeys.DATA_CALORIES_FLOAT)), workoutEvent.calories(), 0.1);
            Assert.assertEquals(DataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT), workoutEvent.heartrate());
            Assert.assertEquals(DataMatrix.getInt(HALKeys.DATA_RPM_INT), workoutEvent.rpm());
            Assert.assertEquals(DataMatrix.getFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT), workoutEvent.speed(), 0.1);
            Assert.assertEquals(DataMatrix.getFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT), workoutEvent.incline(), 0.1);
            Assert.assertEquals(DataMatrix.getInt(HALKeys.DATA_RESISTANCE_INT), workoutEvent.resistance());
            Assert.assertEquals(Math.floor(DataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT)), workoutEvent.floors(), 0.1);
            Assert.assertEquals(DataMatrix.getInt(HALKeys.DATA_SPM_INT), workoutEvent.spm());
            Assert.assertEquals(Math.floor(DataMatrix.getFloat(HALKeys.DATA_WATTS_FLOAT)), workoutEvent.watts(), 0.1);
            Assert.assertEquals(DataMatrix.getFloat(WorkoutKeys.DATA_ELEVATION_METERS_FLOAT), workoutEvent.elevation(), 0.1);
            stopWorkout();
        }
    }

    private void stopWorkout() {
        DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT);
        ThreadHelper.sleepIgnoreInterruption(1000);
        LaunchAppUtil.launchAndroidLauncher();
    }

    /**
     * Test the RFID scan command
     */
    @Test @Chimera @Emulator @SplashApp
    public void testRFIDScan() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.checkResponse(new GeneralResponse(new RFIDCommand(true, false), "ok"));
            testFixture.waitForSystem();
            Assert.assertTrue(DataMatrix.getBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL));
            Assert.assertFalse(DataMatrix.getBoolean(HALKeys.DATA_SCAN_WATCHES_BOOL));
            testFixture.checkResponse(new GeneralResponse(new RFIDCommand(false, true), "ok"));
            testFixture.waitForSystem();
            Assert.assertFalse(DataMatrix.getBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL));
            Assert.assertTrue(DataMatrix.getBoolean(HALKeys.DATA_SCAN_WATCHES_BOOL));
        }
    }

    /**
     * Test the fan speed scan command
     */
    @Test @Chimera @Emulator @SplashApp
    public void testFanSpeed() {
        try(MatrixCommTestFixture testFixture = new MatrixCommTestFixture()) {
            testFixture.checkResponse(new GeneralResponse(new FanCommand(0), "ok"));
            testFixture.waitForSystem();
            Assert.assertTrue(DataMatrix.getInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT, 1) == 0);

            testFixture.checkResponse(new GeneralResponse(new FanCommand(3), "ok"));
            testFixture.waitForSystem();
            Assert.assertTrue(DataMatrix.getInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT, 1) == 3);

            testFixture.checkResponse(new GeneralResponse(new FanCommand(4), "ok"));
            testFixture.waitForSystem();
            Assert.assertTrue(DataMatrix.getInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT, 1) == 3);

            testFixture.checkResponse(new GeneralResponse(new FanCommand(-1), "ok"));
            testFixture.waitForSystem();
            Assert.assertTrue(DataMatrix.getInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT, 1) == 0);
        }
    }
}
