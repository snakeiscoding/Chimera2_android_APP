package com.jht.chimera.service.hal;

import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_TM_HYBRID_IR_ESTOP;
import static com.jht.chimera.service.hal.lib.machine.Machine.resistanceCalTable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.util.SerialNumberUtil;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.SystemBrightness;
import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.common.enums.JHTTypes;
import com.jht.chimera.lib.common.log.ConsoleEvent;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.service.hal.lib.frictiontest.FrictionTestManagerKt;
import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;
import com.jht.chimera.service.hal.lib.lcb.LCBCmdHandler;
import com.jht.chimera.service.hal.lib.lcb.LCBDataHandler;
import com.jht.chimera.service.hal.lib.lcb.LCBDataTimerHandler;
import com.jht.chimera.service.hal.lib.lcb.LCBInfo;
import com.jht.chimera.service.hal.lib.lcb.LCBMachineInfoHandler;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.LcbType;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.ILCBCallback;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.LCBInterface;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface.TreadMillInterface;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.lcbupdateInterface.ILCBUpdateCallback;
import com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.lcbupdateInterface.LCBUpdateInterface;
import com.jht.chimera.service.hal.lib.machine.Machine;
import com.jht.chimera.service.hal.lib.machine.MachineData;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.liberror.ErrorCodeManager;
import com.jht.services.ChildConsoleService;
import com.jht.services.ConsoleServiceState;

import java.io.File;
import java.util.Arrays;

public class LCBService extends ChildConsoleService {
    private static final String TAG = "LCBService";

    private static final String UPDATE_LCB_BROADCAST = "com.jht.hal.update.lcb"; //NON-NLS

    private LCBInterface lcb;
    FrameConfig frameConfig;
    private final DataMatrix dataMatrix = new DataMatrix();

    // Used to handle LCB Data.
    private LCBDataHandler lcbDataHandler;
    public static LCBCmdHandler lcbCmdHandler;

    private String preConsoleSN = "";
    private String preFrameSN = "";

    private LCBDataTimerHandler lcbDataTimerHandler;
    private LCBMachineInfoHandler lcbMachineInfoHandler;

    private boolean supportSN = false;
    boolean demoMode = false;
    public MachineData machineData = new MachineData();

    private boolean mIsStartStopMachine = false;
    private JHTTimer mInitTimer;
    private final PeriodicTimer demoRpmTimer = new PeriodicTimer(0, 1000, this::updateDemoRPM);

    // Forces the watts to be equal to the forced watts value
    private int mForceWattsValue = 0;
    static FrameConfig mConfig = FrameConfigManager.getInstance().getFrameConfig();

    private boolean sendLCBSensorData;

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        lcb = new LCBInterface(callback, this);
        init();
        updateMyServiceState(ConsoleServiceState.READY);
    }

    private void init() {

        frameConfig = FrameConfigManager.getInstance().getFrameConfig();

        Log.d(TAG, "Initializing LCB Service");

        if (lcbDataTimerHandler == null) {
            lcbDataTimerHandler = new LCBDataTimerHandler(this);
        }

        if (lcbMachineInfoHandler == null) {
            lcbMachineInfoHandler = new LCBMachineInfoHandler(this, lcb);
        }

        dataMatrix.createIntAdapter(SettingKeys.Calibration.DATA_CLIMBMILL_CAL_FACTOR_USER_INPUT_INT, this::setClimbFrameCalScaling);

        dataMatrix.createBoolAdapter(HALKeys.DATA_ATHENA_MONITOR_SENSOR_DATA_BOOL, (monitor) -> sendLCBSensorData = monitor);

        timerInitThread();
    }

    private void timerInitThread() {
        mInitTimer = new JHTTimer(500, () -> {
            initDataCmdHandler();
            mInitTimer.stop();
        });
        mInitTimer.start();
    }

    private void initDataCmdHandler() {
        Log.d(TAG, "Setting LCB handler");
        if (lcbDataHandler == null) {
            lcbDataHandler = new LCBDataHandler(lcb, this, machineData);
        }

        if (lcbCmdHandler == null) {
            lcbCmdHandler = new LCBCmdHandler(lcb);
        }

        setupCallback();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dataMatrix.finish();
        demoRpmTimer.stop();
        lcbDataHandler.onDestroy();
        lcbCmdHandler.onDestroy();
        lcbDataTimerHandler.onDestroy();
        lcbMachineInfoHandler.onDestroy();
    }

    // Note if we don't delay this event, then there is a chance that the frame will not work unless you
    // reboot.  See CHIM-8795. (This change is from PRKS-3278)
    private final JHTTimer mActionSetupFrameTimer = new JHTTimer(500, () -> {
        // PRKS-1542 0xC1 LCB generating 04A0 during factory reset comms blackout
        // Need to ensure LCB initialization sequence is run once frame has been set up
        lcb.reInitLcb();
        lcbDataHandler.initLCBModule(true);
    });

    public void setupCallback() {

        dataMatrix.createBoolAdapter(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, value -> {
            demoMode = value;
            if (value) {
                // should we go as far as to leave demo timer null when not needed to save space?
                demoRpmTimer.start("DemoRpmTimer");
            } else {
                demoRpmTimer.stop();
            }
        });

        dataMatrix.createEventAdapter(HALKeys.ACTION_SETUP_FRAME, keyReturnedBack -> {
            mActionSetupFrameTimer.start();
        });

        dataMatrix.createFloatAdapter(HALKeys.ACTION_SET_SPEED_KPH_FLOAT, value -> {
            if (demoMode) {
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                    // update speed/rpm so stats can update
                    machineData.targetKPH = value;
                    machineData.rpm = Machine.kphToRpm(value);
                    updateMachineData();
                }
                return;
            }
            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                int rpm = Machine.kphToRpm(value);

                // set the reverse bit to true if reverse belt mode is true
                if (frameConfig.TM_SUPPORTS_REVERSE_BELT && DataMatrix.getBoolean(HALKeys.DATA_TM_IS_REVERSE_BELT_BOOL)) {
                    rpm = rpm | 0x8000;
                }
                Log.i(TAG, "ACTION_SET_SPEED_KPH_FLOAT " + value + " | rpm = " + rpm);

                machineData.targetKPH = value;
                lcbCmdHandler.setTreadmillRPM(rpm);
                updateMachineData();
            }
        });

        dataMatrix.createFloatAdapter(HALKeys.ACTION_SET_INCLINE_FLOAT, value -> {
            if (demoMode) {
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill
                        || frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Ascent) {
                    // update incline so stats can update
                    machineData.targetIncline = value;
                    machineData.inclineADC = Machine.inclineToAdc(value);
                    updateMachineData();
                }
                return;
            }
            int adc = Machine.inclineToAdc(value);
            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                Log.i(TAG, "ACTION_SET_INCLINE_FLOAT Treadmill " + value + " | adc = " + adc);
                machineData.targetIncline = value;
                lcbCmdHandler.setRawIncline(adc);
                updateMachineData();
            } else if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Ascent) {
                Log.i(TAG, "ACTION_SET_INCLINE_FLOAT Ascent " + value + " | adc = " + adc);
                machineData.targetIncline = value;
                lcbCmdHandler.setAscentIncline(adc);
                updateMachineData();
            }

        });

        dataMatrix.createEventAdapter(HALKeys.ACTION_START_FRICTION_TEST_DATA_COLLECTION, key -> {
            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                FrictionTestManagerKt.startSendingFrictionTestData((targetSpeedKph, targetIncline) -> {
                    // Send the treadmill operate command with updated data
                    int rpm = Machine.kphToRpm(targetSpeedKph);
                    int adc = Machine.inclineToAdc(targetIncline);
                    machineData.targetKPH = targetSpeedKph;
                    machineData.targetIncline = targetIncline;
                    lcbCmdHandler.setTreadmillOperate(rpm, adc);
                    updateMachineData();
                    return null;
                });
            }
        });

        dataMatrix.createEventAdapter(HALKeys.ACTION_STOP_FRICTION_TEST_DATA_COLLECTION, key -> {
            if (FrictionTestManagerKt.isSendingData()) {
                FrictionTestManagerKt.stopFrictionTestData();

                // Stop the belt with the treadmill operate command
                int rpm = Machine.kphToRpm(0f);
                int adc = Machine.inclineToAdc(0f);
                machineData.targetKPH = 0f;
                machineData.targetIncline = 0f;
                lcbCmdHandler.setTreadmillOperate(rpm, adc);
                updateMachineData();
            }
        });

        dataMatrix.createFloatAdapter(HALKeys.ACTION_SET_FRICTION_TEST_TARGET_SPEED_KPH_FLOAT, FrictionTestManagerKt::setTargetSpeedKph);

        dataMatrix.createFloatAdapter(HALKeys.ACTION_SET_FRICTION_TEST_TARGET_INCLINE_FLOAT, FrictionTestManagerKt::setTargetIncline);

        dataMatrix.createEventAdapter(HALKeys.ACTION_STOP_INCLINE, keyReturnedBack -> {
            if (demoMode) return;
            Log.i(TAG, "ACTION_STOP_INCLINE");
            lcbCmdHandler.stopInclineAction();
        });

        dataMatrix.createIntAdapter(WorkoutKeys.DATA_LCB_CONTROL_MODE_INT, mode -> {
            if (mode != 1) {
                mForceWattsValue = 0;
            } else {
                machineData.resistanceLevel = 1;
                setTargetWatts(DataMatrix.getInt(WorkoutKeys.DATA_TARGET_WATTS_INT), machineData.rpm);
                Log.d(TAG, "####CSsetTargetWatts trigger by DataMatrix key DATA_LCB_CONTROL_MODE_INT");
            }
            updateMachineData();
        });

        dataMatrix.createIntAdapter(HALKeys.ACTION_SET_RESISTANCE_INT, value -> {
            if (DataMatrix.getInt(WorkoutKeys.DATA_LCB_CONTROL_MODE_INT) != 0) {
                return;
            }
            if (frameConfig.machineType == JHTCommonTypes.MachineType.ManualResistanceSpinBike) {
                return;
            }

            if (demoMode) {
                int rawResistance = Machine.getResistance(value, DataMatrix.getInt(HALKeys.DATA_RPM_INT));
                machineData.resistanceLevel = value;
                if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
                    machineData.targetSpm = rawResistance;
                    //machineData.spm = rawResistance;
                } else if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
                    // help fake the demo data better
                    updateDemoRPM();
                }
                updateMachineData();
                return;
            }
            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill)
                return;

            machineData.resistanceLevel = value;
            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
                int rawResistance = Machine.getResistance(value, DataMatrix.getInt(HALKeys.DATA_RPM_INT));
                Log.i(TAG, "ACTION_SET_RESISTANCE_INT = " + value + " | raw resistance = " + rawResistance);
                machineData.targetSpm = rawResistance;
                lcbCmdHandler.setSPM(rawResistance);
            } else {
                updateResistance(machineData.resistanceLevel, DataMatrix.getInt(HALKeys.DATA_RPM_INT));
            }
            updateMachineData();
        });

        dataMatrix.createIntAdapter(WorkoutKeys.DATA_TARGET_WATTS_INT, targetWatts -> {
            if (DataMatrix.getInt(WorkoutKeys.DATA_LCB_CONTROL_MODE_INT) != 1) return;
            setTargetWatts(targetWatts, machineData.rpm);
            Log.d(TAG, "####CSsetTargetWatts trigger by DataMatrix key DATA_TARGET_WATTS_INT");
        });

        dataMatrix.createEventAdapter(HALKeys.ACTION_START_MACHINE, keyReturnedBack -> {
            if (demoMode) return;
            lcbCmdHandler.startMachine();
            mIsStartStopMachine = true;
        });

        dataMatrix.createStringAdapter(HALKeys.DATA_PWM_CAL_POINTS_STRING, value -> mConfig.dataPoints = value);

        dataMatrix.createEventAdapter(HALKeys.ACTION_PWM_CAL_TABLE, keyReturnedBack -> {
            String data = "   ----   ----   ----   ----   ----";
            if (!mConfig.WATTS_INACCURATE) { // skip if ECB based resistance?
                if (mConfig.hasInductionCal == 1) {
                    //mConfig.hasInductionCal = 0;
                    data = "   "
                            + mConfig.inductionCalPoints[0]
                            + "   "
                            + mConfig.inductionCalPoints[1]
                            + "   "
                            + mConfig.inductionCalPoints[2]
                            + "   "
                            + mConfig.inductionCalPoints[3]
                            + "   "
                            + mConfig.inductionCalPoints[4];
                }
                if (mConfig.validInductionCal == 1) {
                    mConfig.validInductionCal = 0;
                    resistanceCalTable();
                    Log.i(TAG, "resistance table update");
                }
            }
            DataMatrix.putString(HALKeys.DATA_PWM_CAL_POINTS_STRING, data);
        });

        dataMatrix.createEventAdapter(HALKeys.ACTION_STOP_MACHINE, keyReturnedBack -> {
            if (demoMode) return;

            lcbCmdHandler.stopMachine();
            mIsStartStopMachine = false;
            ConsoleEvent.logEvent("STOP MACHINE: event handler");
        });

        dataMatrix.createBoolAdapter(SettingKeys.Workout.DATA_TREAD_SENSE_ENABLED_BOOL, value -> {
            if (frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill))
                lcbCmdHandler.setTreadmillTreadSenseOnOff(value);
        });
        dataMatrix.createBoolAdapter(SettingKeys.Workout.DATA_CM_ESTOP_ENABLE_BOOL, value -> {
            if (frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill))
                lcbCmdHandler.setmEnClimbmillControlZoneOn((frameConfig.HAS_CM_CONTROL_ZONE) && (value));
        });
        dataMatrix.createBoolAdapter(SettingKeys.Workout.DATA_CM_DETECT_PERSON_BOOL, value -> {
            if (frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill))
                lcbCmdHandler.setEnClimbmillNoPersonOnOff(value);
        });

        dataMatrix.createFloatAdapter(HALKeys.ACTION_SET_TARGET_METS_FLOAT, targetMets -> {
            if (!demoMode) return;
            if (!frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill)) {
                if (targetMets == 0) {
                    machineData.targetSpm = 0;
                    lcbCmdHandler.setSPM(0);
                } else {
                    double spm = ((targetMets * 3.5 - 3.5) * 4.0) / 1.4647986;
                    machineData.targetSpm = (int) spm;
                    lcbCmdHandler.setSPM(spm);
                }
            }
        });


        dataMatrix.createBoolAdapter(HALKeys.ACTION_CALIBRATE_BOOL, this::startCalibration);
        dataMatrix.createEventAdapter(HALKeys.EVENT_GET_MAGNENT_ANGLE, value -> getMagnentAngle());
        dataMatrix.createEventAdapter(HALKeys.EVENT_AUTO_TUNE_MAGNENT_ANGLE, value -> startAutoTuneMagnetAngle());

        dataMatrix.createIntAdapter(HALKeys.ACTION_SET_SPEED_RPM_INT, value -> lcbCmdHandler.setTreadmillRPM(value));

        dataMatrix.createBoolAdapter(HALKeys.ACTION_SHOW_CZ_IR_FREQ_BOOL, value -> lcbCmdHandler.replyEnClimbCZcapaIRfreq(value));

        dataMatrix.createEventAdapter(HALKeys.EVENT_GET_CLIMB_FRAME_CAL_SCALING, value -> getClimbFrameCalScaling());

        dataMatrix.createEventAdapter(HALKeys.ACTION_SHUTDOWN_SYSTEM, key -> {
            if (demoMode) return;
            if (!ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.UltraThin)) { // ERP wake mod for UT requires we keep the port open?
                //erp stop uart port
                lcbCmdHandler.erpStopUartPort();
            }
        });

        // Add a callback for to simulate an error code from adb shell
        dataMatrix.createIntAdapter(HALKeys.ACTION_SIMULATE_LCB_ERROR_CODE_INT,
                value -> ErrorCodeManager.handleLCBErrorCode(value, false));
        
        dataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_PERFORMANCE_PLUS_TM_PFC_MODE_BOOL, value -> {

            if (frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill) && frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS) {
                Log.d(TAG, "Switch PFC function " + value);
                lcbMachineInfoHandler.switchPFCfunction(value);
            }
        });

        dataMatrix.createBoolAdapter(HALKeys.DATA_IO_IS_SAFETY_PULLED_BOOL, value -> {

            //Only treadmill have safety switch
            if (frameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Treadmill)
                return;

            onTmSafetykeyStateUpdate(value, LCBInfo.MCBState.SWITCH_LOCK.getId());
        });

        preConsoleSN = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
        dataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, string -> {
            // Log.d("LCBTrace", "Console serial number change." + string);
            if (!preConsoleSN.equals(string)) {
                //store console serial number on system
                if (!string.equals("") && BuildConfig.SUPPORTS_SAVE_CONSOLE_SERIAL) {
                    AndroidSetting.putString(getContentResolver(), "chimera_console_sn", string); //NON-NLS
                }

                preConsoleSN = string;
                if (supportSN)
                    lcbCmdHandler.modifyWriteLcbSerialNumber(preConsoleSN, preFrameSN);
            }
        });

        preFrameSN = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
        dataMatrix.createStringAdapter(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, string -> {
            //Log.d("LCBTrace", "Frame serial number change." + string);
            if (!preFrameSN.equals(string)) {
                preFrameSN = string;
                if (supportSN)
                    lcbCmdHandler.modifyWriteLcbSerialNumber(preConsoleSN, preFrameSN);
            }
        });

        dataMatrix.createEventAdapter(HALKeys.ACTION_UPDATE_LCB_SERIAL_NUMBERS, event -> {
            String consoleSN = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
            String frameSN = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
            if (supportSN)
                lcbCmdHandler.modifyWriteLcbSerialNumber(consoleSN, frameSN);
        });

        //Needless to set motor type, LCB had default setting
        //If set the wrong type will cause site effect

        dataMatrix.createIntAdapter(SettingKeys.Hardware.DATA_TREADMILL_MOTOR_TYPE_INT, pos -> {
            Log.i(TAG, "DATA_TREADMILL_MOTOR_TYPE_INT = " + pos);
            lcbCmdHandler.setTreadMillMotorTypeValue(pos);
        });
        dataMatrix.createEventAdapter(HALKeys.EVENT_GET_MOTOR_TYPE, value -> getMotorType());


        dataMatrix.createIntAdapter(HALKeys.ACTION_REQUEST_RAW_ADC_INT, v -> {
            lcbCmdHandler.requestRawAdc(v);
        });

        dataMatrix.createEventAdapter(HALKeys.ACTION_START_SPINDOWN_TEST, string -> {
            // ATHENA To Be Completed
            lcbCmdHandler.setSpinBikeSpinDownOnOff(true);
            lcbCmdHandler.replySpinBikeInfoSpinDown(true);
        });

        dataMatrix.createEventAdapter(HALKeys.ACTION_STOP_SPINDOWN_TEST, string -> {
            // ATHENA To Be Completed
            lcbCmdHandler.setSpinBikeSpinDownOnOff(false);
            lcbCmdHandler.replySpinBikeInfoSpinDown(false);
        });
        dataMatrix.createEventAdapter(HALKeys.ACTION_CLEAR_TORQUE, event -> lcbCmdHandler.setSpinBikeClearTorque());
        dataMatrix.createBoolAdapter(HALKeys.ACTION_GET_INFO_SPIN_DOWN_BOOL, value -> lcbCmdHandler.replySpinBikeInfoSpinDown(value));

        dataMatrix.createEventAdapter(HALKeys.ACTION_RAPTOR_PARA_BACKUP, event -> lcbCmdHandler.setSpinBikeRaptorPara(DataMatrix.getString(SettingKeys.Calibration.DATA_SETTING_RAPTOR_PARA_DATA_STRING)));
        dataMatrix.createEventAdapter(HALKeys.ACTION_RAPTOR_PARA_RESTORE, event -> lcbCmdHandler.getSpinBikeRaptorPara());

        dataMatrix.createEventAdapter(HALKeys.ACTION_ENTER_ERP, event -> {
            boolean isInWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP;
            if (isInWorkout) { // RGC-3536 ERP button during a workout
                Log.d(TAG, "ERP action during a workout, ignoring");
                //DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT); // should we pause workout instead? ERP during a workout seems unintentional
                /*MessageCenterMessage msg = new MessageCenterMessage()
                        .setMessageText(ContextUtil.getString(R.string.erp_tc) + " - " + ContextUtil.getString(R.string.ignore_tc))
                        .setTextColor(Color.WHITE)
                        .setBackgroundColor(Color.BLACK)
                        .setHideOnSplash(true)
                        .setHideTime(4); // 4 secs
                DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msg);*/
                return;
            }
            ConsoleEvent.logEvent("Entering ERP");
            if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                if (isScreenErp()) {
                    ExecuteAsRoot.simulatePowerButton();
                } else {
                    lcbCmdHandler.setTM548EcbActionPowerOFF();
                    //I/O control ERP, stopAllLcbCommunication before IO enter ERP.
                    lcbCmdHandler.stopAllLcbCommunication();
                    SystemClock.sleep(2000);

                    new JHTTimer(1000, () -> DataMatrix.sendEvent(HALKeys.EVENT_LCB_POWERED_OFF)).start();
                }
            } else {
                if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.UltraThin) && !isScreenErp()) {
                    if (offButtonTimer == null) { // somehow getting two executions with button, so only do this if timer was never made
                        offButtonTimer = new JHTTimer(500, ExecuteAsRoot::suspendToMemory); // RGC-3533 power button can hang on app wake locks, use suspend
                        lcbCmdHandler.setErp(true); // normally IO mcu handles 7A command, but now console must send it
                        offButtonTimer.start(); // we could add a check to 7A enter erp response handler to issue suspend instead of relying on a delay?
                    }
                } else {
                    executeErpEvent();
                }
            }
        });

        dataMatrix.createEventAdapter(HALKeys.EVENT_TM548_MES_ECB_RUN_RELEASE, event -> {
            if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                lcbCmdHandler.setTM548MESEcbRunRelease();
            }
        });

        dataMatrix.createEventAdapter(HALKeys.EVENT_TM548_MES_ECB_RUN_BRAKE, event -> {
            if (frameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
                lcbCmdHandler.setTM548MESEcbRunBrake();
            }
        });

        dataMatrix.createEventAdapter(WorkoutKeys.EVENT_WORKOUT_START, value -> {
            lcb.setLcbInitBeforeWorkout();
        });


        registerReceiver(updateReceiver, new IntentFilter(UPDATE_LCB_BROADCAST));

    }

    private JHTTimer offButtonTimer = null;

    private static void executeErpEvent() {
        if (ProductConfig.hasIO()) {
            if (isScreenErp()) {
                if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)) {
                    SystemBrightness.setSystemBrightnessToZero(context.getContentResolver());
                } else {
                    // just do android standby
                    ExecuteAsRoot.simulatePowerButton();
                }
            } else {
                //I/O control ERP, stopAllLcbCommunication before IO enter ERP.
                lcbCmdHandler.stopAllLcbCommunication();
                SystemClock.sleep(2000);

                // signal for full ERP
                DataMatrix.sendEvent(HALKeys.EVENT_LCB_POWERED_OFF);
            }
        } else {
            ExecuteAsRoot.simulatePowerButton(); // if no IO, just do power button
        }
    }

    private static boolean isScreenErp() {
        return (DataMatrix.getInt(SettingKeys.Hardware.DATA_ENERGY_SAVER_TYPE_ENUM_INT) == SettingKeys.ERPType.Screen.ordinal());
    }

    private void setTargetWatts(int targetWatts, int rpm) {
        mForceWattsValue = targetWatts;

        if (!demoMode) {
            if (LCBInfo.isChopper()) {
                lcbCmdHandler.setRawResistance(targetWatts);
                Log.d(TAG, "####CS set resistance trigger by setTargetWatts");
            } else if (frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.SpinBike)) {
                lcbCmdHandler.setErgWatts(targetWatts);
            } else {
                int rawResistanceToSet = Machine.getResistanceFromWatts(targetWatts, rpm);
                lcbCmdHandler.setRawResistance(rawResistanceToSet);
                Log.d(TAG, "####CS set resistance trigger by setTargetWatts, rawResistanceToSet: " + rawResistanceToSet);
            }
        }
    }


    private void updateDemoRPM() {
        // technically demoMode is usually checked before this is called, but I suppose the timer could fire after demo is disabled?
        if (demoMode) {
            int rpm = DataMatrix.getInt(HALKeys.DATA_DEMO_RPM_INT);
            switch (frameConfig.machineClassType) {
                case Elliptical:
                case Ascent:
                    machineData.rpm = rpm;
                    machineData.spm = rpm * 2;
                    break;
                case Bike:
                case Stepper:
                case RecumbentElliptical:
                    machineData.rpm = rpm;
                    break;
                case Climbmill:
                    machineData.rpm = rpm;
                    machineData.spm = rpm;
                    break;

                    /*  SpinBike gets watts directly from LCB, so for demo mode the watts
                    has to be faked.   Using the RPM value * resistance/3 level to fake watts value
                     */
                case SpinBike:
                    machineData.rpm = rpm;
                    if (DataMatrix.getInt(WorkoutKeys.DATA_LCB_CONTROL_MODE_INT) == 1) {
                        machineData.watts = mForceWattsValue;
                    } else {
                        machineData.watts = machineData.resistanceLevel / 3f * rpm;
                    }
                    break;
            }
            updateMachineData();
        }
    }

    public void updateResistance(int resistanceLevel, int rpm) {
        if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Ascent ||
                frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Elliptical ||
                frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Stepper ||
                frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Bike ||
                frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.RecumbentElliptical ||
                frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
            int rawResistance = Machine.getResistance(resistanceLevel, rpm);
            if (mIsStartStopMachine) {
                lcbCmdHandler.setRawResistance(rawResistance);
            } else {
                lcbCmdHandler.setRawResistance(0);
            }
        }
    }

    public void updateMachineData() {
        if (!DataMatrix.getBoolean(HALKeys.DATA_LOCK_MACHINE_DATA_FOR_TESTING_BOOL)) {
            float massKg = DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT);
            switch (frameConfig.machineClassType) {

                case Treadmill:
                    Machine.computeTreadmill(machineData, massKg);
                    break;
                case Ascent:
                case Elliptical:
                    Machine.computeEllipticalAscent(machineData, massKg, DataMatrix.getInt(WorkoutKeys.DATA_LCB_CONTROL_MODE_INT) == 1, mForceWattsValue);
                    break;
                case Bike:
                case RecumbentElliptical:
                    Machine.computeBike(machineData, massKg, DataMatrix.getInt(WorkoutKeys.DATA_LCB_CONTROL_MODE_INT) == 1, mForceWattsValue);
                    break;
                case SpinBike:
                    Machine.computeSpinBike(machineData, massKg);
                    break;
                case Stepper:
                    Machine.computeStepper(machineData, massKg);
                    break;
                case Climbmill:
                    Machine.computeClimbmill(machineData, massKg);
                    break;
            }

            // Evolt calories will be shown for all the frame modalities
            if (DataMatrix.getFloat(DAPIKeys.DATA_USER_EVOLT_SMM_FLOAT) > 0f) {
                // The Evolt formula can return negative numbers, so get the max of at least 0
                //TODO: Add constants or a formula here?
                machineData.caloriesPerSecond = Math.max(0, ((DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT) * 0.074f) - (DataMatrix.getFloat(DAPIKeys.DATA_USER_EVOLT_SMM_FLOAT) * 0.05741f) + ((DataMatrix.getInt(HALKeys.DATA_HEART_RATE_INT) * 0.4472f) - 20.4022f)) * (1 / 60f) / 4.184f);
            }

            try {
                DataMatrix.updateFloat(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, machineData.targetKPH);
                DataMatrix.updateFloat(HALKeys.DATA_ACTUAL_SPEED_KPH_FLOAT, machineData.kph);
                DataMatrix.updateFloat(HALKeys.DATA_TARGET_INCLINE_FLOAT, machineData.targetIncline);
                DataMatrix.updateFloat(HALKeys.DATA_ACTUAL_INCLINE_FLOAT, machineData.incline);
                lcbDataHandler.updateCurrentInclineADC(machineData.inclineADC); // previously a datamatrix key
                DataMatrix.updateInt(HALKeys.DATA_RESISTANCE_INT, machineData.resistanceLevel);
                DataMatrix.updateInt(HALKeys.DATA_RPM_INT, machineData.rpm);
                DataMatrix.updateInt(HALKeys.DATA_SPM_INT, machineData.spm);
                DataMatrix.updateFloat(HALKeys.DATA_WATTS_FLOAT, machineData.watts);
                DataMatrix.updateFloat(HALKeys.DATA_CALORIES_PER_SEC_FLOAT, machineData.caloriesPerSecond);
                DataMatrix.updateFloat(HALKeys.DATA_METS_FLOAT, machineData.mets);
            } catch (Exception ex) {
                Log.e(TAG, "Write dataMatrix exception ! ", ex);
            }
        }
    }

    private ILCBCallback callback = new ILCBCallback() {
        @Override
        public void onErrorCode(int lcbErrorCode) {
            lcbDataHandler.onErrorCode(lcbErrorCode);
        }

        @Override
        public void onLcbType(byte lcbType) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : on lcb type  " + lcbType);
            lcbDataHandler.onLcbType(lcbType);
        }

        @Override
        public void onLcbVersion(int major, int minor) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB :on lcb version  " + major + "." + minor);
            lcbDataHandler.onLcbVersion(major, minor);
            //load console serial number from system
            String ConsoleSN = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
            if (ConsoleSN.equals("") && BuildConfig.SUPPORTS_SAVE_CONSOLE_SERIAL) {
                String stringSysConsoleSN = Settings.Secure.getString(getContentResolver(), "chimera_console_sn"); //NON-NLS
                if (stringSysConsoleSN != null && !stringSysConsoleSN.equals("")) {
                    DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(stringSysConsoleSN));
                }
            }

            // PRKS-2757 this results in frame SN retrieval from LCB, assume that if flavor does not support saving console SN, the same goes for frame SN?
            if (BuildConfig.SUPPORTS_SAVE_CONSOLE_SERIAL) {
                supportSN = checkLcbVerSupportAutoSN(major, minor);
                Log.d(TAG, "supportSN: " + supportSN);
                if (supportSN)
                    lcbDataHandler.dataHandlerGetLcbSN();
            }
        }

        @Override
        public void onLcbInitialFinish() {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : on lcb init finish ");
            lcbDataHandler.initLCBModule(false);
        }

        @Override
        public void onReadLcbSerialNumber(String stringLcbSN) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : on Read Lcb Serial Number ");
            lcbDataHandler.dataHandlerParseLcbSN(stringLcbSN);
        }

        @Override
        public void onInclineMotorDownwardStatus(boolean motorDownwardStatus) {
            if (LCBInfo._lcbDebug)
                Log.d(TAG, "LCB : on incline motor downward  " + motorDownwardStatus);

            lcbDataHandler.onInclineDownAction(motorDownwardStatus);
        }

        @Override
        public void onInclineMotorUpwardStatus(boolean motorUpwardStatus) {
            if (LCBInfo._lcbDebug)
                Log.d(TAG, "LCB : on incline motor upward  " + motorUpwardStatus);

            lcbDataHandler.onInclineUpAction(motorUpwardStatus);
        }

        @Override
        public void onMainMotorStatus(boolean motorStatus) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : on incline motor status  " + motorStatus);
        }

        @Override
        public void onEncoderRPMStatus(boolean encoderRPMStatus) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : on encoder rpm status  " + encoderRPMStatus);
        }

        @Override
        public void onBatteryCapacityStatus(boolean batteryCapacityStatus) {
            if (LCBInfo._lcbDebug)
                Log.d(TAG, "LCB : on battery capacity status  " + batteryCapacityStatus);
        }

        @Override
        public void onCommandErrorStatus(boolean commandErrorStatus) {
            if (LCBInfo._lcbDebug)
                Log.d(TAG, "LCB : on command error status  " + commandErrorStatus);
        }

        @Override
        public void onInitialCalibrationStatus(boolean initialCalibrationStatus) {
            if (LCBInfo._lcbDebug)
                Log.d(TAG, "LCB : on initial calibration status  " + initialCalibrationStatus);
        }

        @Override
        public void onRawADC(int adc) {
            DataMatrix.putInt(HALKeys.ACTION_RECEIVE_RAW_ADC_INT, adc);
        }

        @Override
        public void onRpm(int rpm) {
            if (demoMode) return;
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : on rpm  " + rpm);

            lcbDataHandler.onRpm(rpm);
            // The spinbike implements constant watts (ERG mode) in the LCB - sending resistance will deactivate ERG mode
            // So we check to see what mode we are in.   The workout script controls the ERG mode setting, and it is only
            // set in the workout portion of the workout and unset in the warmup and cooldown
            if (DataMatrix.getInt(WorkoutKeys.DATA_LCB_CONTROL_MODE_INT) == 0) {
                updateResistance(machineData.resistanceLevel, rpm);
            } else if (DataMatrix.getInt(WorkoutKeys.DATA_LCB_CONTROL_MODE_INT) == 1) {
                // 1 = lcb is controlling watts, but elliptical/ascent can't actually control constant watts.
                if (frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Elliptical) ||
                        frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Ascent)
                ) {
                    setTargetWatts(DataMatrix.getInt(WorkoutKeys.DATA_TARGET_WATTS_INT), rpm);
                    Log.d(TAG, "onRpm Target Watts Flow (Elliptical/Ascent)e");
                }
            }
            updateMachineData();
        }

        @Override
        public void onWatts(int watts) {
            if (demoMode) return;
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : on watts  " + watts);

            lcbDataHandler.onWatts(watts);
            // The spinbike implements constant watts (ERG mode) in the LCB - sending resistance will deactivate ERG mode
            // So we check to see what mode we are in.   The workout script controls the ERG mode setting, and it is only
            // set in the workout portion of the workout and unset in the warmup and cooldown

            // UpdateMachineData will get called by onRpm - so skip calling it here
            //updateMachineData();
        }

        @Override
        public void onResistanceLevel(int resistanceLevel) {
            if (demoMode) return;
            // TcvFxSpinBike has no user-side resistance control; resistance comes from the LCB sensor frame.
            // updateMachineData() is invoked by the following onRpm callback and will publish DATA_RESISTANCE_INT.
            machineData.resistanceLevel = resistanceLevel;
        }

        @Override
        public void onOneShotRpm(int rpm) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : on OneShotRpm" + rpm);
            //TO DO
        }

        @Override
        public void onSpm(int spm) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : on spm  " + spm);

            lcbDataHandler.onSpm(spm);
            updateMachineData();
        }

        @Override
        public void onInclineADC(int inclineADC) {

            machineData.inclineADC = inclineADC;
            lcbDataHandler.onInclineADC(inclineADC);

//            int lastInclineADC = dataMatrix.getInt(HALKeys.DATA_INCLINE_ADC_INT, -1);
//            if (lastInclineADC != -1) {
//                dataMatrix.putBoolean(HALKeys.DATA_INCLINE_MOVING_BOOL, Math.abs(inclineADC - lastInclineADC) > 15);
//            } else {
//                dataMatrix.putBoolean(HALKeys.DATA_INCLINE_MOVING_BOOL, false);
//            }
            updateMachineData();
        }

        @Override
        public void onPersonInPlace(boolean personInPlace) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : on personal in place  " + personInPlace);
            lcbDataHandler.onPersonInPlace(personInPlace);

            if (LCBInfo._inCalibration && !personInPlace &&
                    (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill)) {
//                startCalibration(false);
                lcbMachineInfoHandler.handleCMpersonInPlaceError();
            }


            DataMatrix.putBoolean(HALKeys.DATA_PERSON_IN_PLACE_BOOL, personInPlace);
        }


        @Override
        public void onShowRawSensorDataString(String stringSensorData) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB : Raw Sensor Data String  " + stringSensorData);

            if (sendLCBSensorData && frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.SpinBike) {
                DataMatrix.putString(HALKeys.DATA_ATHENA_SENSOR_DATA_STRING, stringSensorData);
            }
        }

        @Override
        public void onTreadMillMotorInfo(byte[] bytesMotorInfo) {
            if (LCBInfo._lcbDebug)
                Log.d(TAG, "LCB : on treadmill motro info  " + Arrays.toString(bytesMotorInfo));
            lcbDataHandler.onTreadMillMotorInfo(bytesMotorInfo);
        }

        @Override
        public void onTreadMillOperate(TreadmillOperateRxData data) {
            lcbDataHandler.onTreadmillOperateReceived(data);
        }

        @Override
        public void onTreadMillCadence(byte[] bytesCadenceData) {
            lcbDataHandler.onTreadmillCadenceReceived(bytesCadenceData);
        }

        @Override
        public void onLCBSafetykeyLockedCode(boolean value) {

            if (frameConfig.machineClassType != JHTCommonTypes.MachineClassificationType.Treadmill) {
                Log.e(TAG, "LCB : Config not TM but get 0x02B2 safetykey lock ");
                return;
            }

            onTmSafetykeyStateUpdate(value, LCBInfo.MCBState.MCB_CODE_LOCK.getId());
        }

        @Override
        public void onTreadmillSafetykeyLockState(boolean value) {

            onTmSafetykeyStateUpdate(value, LCBInfo.MCBState.MCB_STATE_LOCK.getId());
        }

        @Override
        public void onTreadmillStartUnlock() {
            onTmSafetykeyStateUpdate(false, LCBInfo.MCBState.MCB_STATE_CIRCUIT_LOCK.getId());
        }

        @Override
        public void onTMautoOCState(boolean detectState) {

            Log.i(TAG, "MCB auto OC state : detect state " + detectState);
            DataMatrix.putBoolean(HALKeys.ACTION_AUTO_OVER_CURRENT_STATUS_BOOL, detectState);
        }

        @Override
        public void onTreadmillMagnetAngleValue(int value) {
            lcbMachineInfoHandler.updateMagnetAngleValue(value);
        }

        @Override
        public void onTreadmillMotorTypeValue(int value) {
            lcbMachineInfoHandler.updateMotorTypeValue(value);
        }

        @Override
        public void onTreadmillMcbVoltageValue(int value) {

            lcbMachineInfoHandler.updateMcbVoltageValue(value);
        }

        @Override
        public void onTreadmillOSstopWorkout(TreadMillInterface.OverSpeedRule.OverSpeedType overSpeedType) {
            DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT);
            String text = "";
            int hideTime = 5;
            switch (overSpeedType) {
                case SPEED_DIFF_REDUCE_INCLINE_STOP:
                    text = getString(R.string.speed_range_error_tc);
                    break;
                case SPEED_DIFF_STOP:
                        text =  "030C";
                    break;
                case NONE:
                    return;
            }
            MessageCenterMessage msg = new MessageCenterMessage()
                    .setMessageText(text)
                    .setTextColor(Color.BLACK)
                    .setBackgroundColor(Color.RED)
                    .setHideOnSplash(false)
                    .setId(JHTTypes.MessageIds.SPEED_RANGE_ERROR_STOP_WORKOUT.ordinal())
                    .setHideTime(hideTime);
            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msg);
        }

        @Override
        public void onTreadmillOSreduceIncline(TreadMillInterface.OverSpeedRule.OverSpeedType overSpeedType, float fReduceIncline) {
            float incline = Math.max(FrameConfigManager.getInstance().getFrameConfig().incline.getMin(), machineData.targetIncline - fReduceIncline);
            DataMatrix.putFloat(WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT, incline);
            MessageCenterMessage msg = new MessageCenterMessage()
                    .setMessageText(getString(R.string.speed_range_error_decrease_incline_tc))
                    .setTextColor(Color.BLACK)
                    .setBackgroundColor(Color.RED)
                    .setHideOnSplash(false)
                    .setId(JHTTypes.MessageIds.SPEED_RANGE_ERROR_REDUCE_INCLINE.ordinal())
                    .setHideTime(5);
            DataMatrix.putParcelable(UiServiceKeys.ACTION_SHOW_MESSAGE_PARCELABLE, msg);
            setupCallback(incline);
        }

        final DataMatrix mOverSpeedDataMatrix = new DataMatrix();

        private void setupCallback(float targetIncline) {
            clearCallback(); // clear previous listener
            // cancel the listener if user changes the incline. If this is not needed, consider to set a timer to cancel it.
            mOverSpeedDataMatrix.createFloatAdapter(WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT, incline -> {
                if (incline != targetIncline)
                    clearCallback();
            });
            // Beeps when actual incline reaches the target reduced value
            mOverSpeedDataMatrix.createFloatAdapter(HALKeys.DATA_ACTUAL_INCLINE_FLOAT, incline -> {
                if (incline <= targetIncline && targetIncline - incline < 0.5) { // Give a tolerance about 0.5%
                    DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP);
                    clearCallback();
                }
            });
            mOverSpeedDataMatrix.createEventAdapter(WorkoutKeys.ACTION_STOP_WORKOUT, event -> {
                clearCallback();
            });
        }

        private synchronized void clearCallback() {
            mOverSpeedDataMatrix.finish();
        }

        @Override
        public void onEnClimbmillSafetykeyPulled(boolean mast, boolean hand) {
            onCmSafetykeyPulled(mast, hand);

            //Stop calibration when trigger
            if (LCBInfo._inCalibration && (mast || hand)) {
                startCalibration(false);
            }
        }

        @Override
        public void onEnClimbmillStartUnlock() {
            onCmSaftykeyUnlock();
        }

        @Override
        public void onBatteryVoltage(int batteryVoltage) {
            lcbDataHandler.onBatteryVoltage(batteryVoltage);
        }

        @Override
        public void onLcbStatus(byte lcbstatus) {
            LCBInfo._status.onStatus(lcbstatus);
            lcbDataHandler.onLcbStatusUpdate(LCBInfo._status);
        }

        @Override
        public void onLcbTMStatus(byte lcbstatus) {
            LCBInfo._status.onTMStatus(lcbstatus);
            lcbDataHandler.onLcbTmStatusUpdate(LCBInfo._status);
        }

        @Override
        public void onEnClimbmillCZ(boolean cz) {
            lcbDataHandler.onEnClimbmillCZ(cz);

            if (LCBInfo._inCalibration && cz &&
                    (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill)) {
//                startCalibration(false);
                lcbMachineInfoHandler.handleCMpersonInPlaceError();
            }
        }

        @Override
        public void onEnClimbmillIR(boolean ir) {
            lcbDataHandler.onEnClimbmillIR(ir);

            if (LCBInfo._inCalibration && ir &&
                    (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill)) {
//                startCalibration(false);
                lcbMachineInfoHandler.handleCMpersonInPlaceError();
            }
        }

        @Override
        public void onEnClimbMillWatts(int watts) {
            lcbDataHandler.onEnClimbMillWatts(watts);
        }

        @Override
        public void onEnClimbmillCalibraton(boolean calibrating) {
            //0 : stop 1: calibration
            lcbMachineInfoHandler.onCMCalibrateStatus(calibrating);
        }

        @Override
        public void onEnclimbmillCZcapaIRfreq(int cz, int ir) {
            lcbDataHandler.onEnclimbmillCZcapaIRfreq(cz, ir);
        }

        @Override
        public void onEnClimbMillCalScaling(int intEnClimbMillCalScaling) {
            lcbMachineInfoHandler.onClimbmillCalScaling(intEnClimbMillCalScaling);
        }

        @Override
        public void onEnClimbMillFrameCalScaling(int intEnClimbMillFrameCalScaling) {
            lcbMachineInfoHandler.onEnClimbMillFrameCalScaling(intEnClimbMillFrameCalScaling);
        }


        @Override
        public void onEnClimbmillStartControl(boolean value) {
            onCmStartControl(value);
        }

        @Override
        public void onSpinBikeInfoSpinDown(int spinDownStatus, int spinDownRPM, float spinDownTorqueScalar, float spinDownCorrectionFactor, float spinDownInertia) {
            lcbDataHandler.onSpinBikeInfoSpinDown(spinDownStatus, spinDownRPM, spinDownTorqueScalar, spinDownCorrectionFactor, spinDownInertia);
        }

        @Override
        public void onSpinBikeRaptorPara(byte[] bytesRaptorPara) {
            lcbDataHandler.onSpinBikeRaptorPara(bytesRaptorPara);
        }

        @Override
        public void onTmRollerGuard(boolean triggered) {

            // This follows the same logic as the onHybridTmIrAddrIrEStop function; onTmSafetykeyPulled will
            // update the DATA_SAFETY_KEY_STATUS_INT key
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB :onTmRollerGuard triggered = " + triggered);
            if (triggered) {
                LCBInfo._mcbLockstate |= LCBInfo.MCBState.MCB_CODE_LOCK.getId();
                lcbDataHandler.onTmSafetykeyPulled(true);
                lcbDataTimerHandler.stopTmReleaseCheckTimer();
                Log.i(TAG, "MCB Safety : Roller guard triggered, state " + LCBInfo._mcbLockstate);
            } else {
                LCBInfo._mcbLockstate &= ~LCBInfo.MCBState.MCB_CODE_LOCK.getId();
                lcbCmdHandler.onTmSafetykeyUnlock();

                // Refresh the safety key bitmap: always clears the roller guard bit; keeps the
                // e-stop state if another lock source (e.g. the safety key) is still active.
                lcbDataHandler.onTmSafetykeyPulled(LCBInfo.getMCBLockState());
                Log.i(TAG, "MCB Safety : Roller guard released, state " + LCBInfo._mcbLockstate);
            }
        }

        @Override
        public void onHybridTmIrAddrVersion(int major) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB :on HybridTm Ir Addr version  " + major);
        }

        @Override
        public void onHybridTmIrAddrIrStatus(int intIrAddrIrStatus) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB :on HybridTm Ir Addr Ir Status  " + intIrAddrIrStatus);
        }

        @Override
        public void onHybridTmIrAddrErpRtnByte(byte byteRtn) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB :on HybridTm Ir Addr Erp RtnByte = " + byteRtn);
        }

        @Override
        public void onHybridTmIrAddrEnterErpMode(boolean isEnterSuccess) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB :on HybridTm Ir Addr Enter Erp Mode = " + isEnterSuccess);
        }

        @Override
        public void onHybridTmIrAddrLeaveErpMode(boolean isLeaveSuccess) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB :on HybridTm Ir Addr Leave Erp Mode = " + isLeaveSuccess);
        }

        @Override
        public void onHybridTmIrAddrIrEStop(boolean triggered) {
            if (LCBInfo._lcbDebug) Log.d(TAG, "LCB :on HybridTm Ir Addr IrEStop = " + triggered);
            if (triggered) {
                LCBInfo._mcbLockstate |= LCBInfo.MCBState.MCB_CODE_LOCK.getId();
                lcbDataHandler.onTmSafetykeyPulled(true);
                lcbDataTimerHandler.stopTmReleaseCheckTimer();
                Log.i(TAG, "MCB Safety : IR-EStop triggered, state " + LCBInfo._mcbLockstate);
            } else {
                LCBInfo._mcbLockstate &= ~LCBInfo.MCBState.MCB_CODE_LOCK.getId();
                lcbDataHandler.onTmSafetykeyPulled(false);
                lcbCmdHandler.onTmSafetykeyUnlock();
                Log.i(TAG, "MCB Safety : IR-EStop released, state " + LCBInfo._mcbLockstate);
            }

            // update the safety key status
            int previousState = DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT);
            int safetyKeyState = triggered ? (previousState | SAFETY_KEY_TM_HYBRID_IR_ESTOP) : (previousState & ~SAFETY_KEY_TM_HYBRID_IR_ESTOP);
            if (previousState != safetyKeyState) DataMatrix.updateInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT, safetyKeyState);
        }
    };

    /* Treadmill */

    public void onTmSafetykeyStateUpdate(boolean trigger, byte st) {

        //Normal state, return
        if (!trigger && LCBInfo._mcbLockstate == 0)
            return;

        if (trigger) {
            //Set trigger bit
            LCBInfo._mcbLockstate |= st;

            //Show E-STOP animation
            lcbDataHandler.onTmSafetykeyPulled(true);

            //Stop release timer
            lcbDataTimerHandler.stopTmReleaseCheckTimer();

            //Safety switch is up but gets 02B2 MCB lock, do unlock.
            if (LCBInfo._mcbLockstate == LCBInfo.MCBState.MCB_CODE_LOCK.getId()) {
                //Send unlock command to MCB
                lcbCmdHandler.onTmSafetykeyUnlock();

                Log.i(TAG, "MCB Safety : get 0x02B2 re-unlock ");
            }
        } else {
            LCBInfo._mcbLockstate &= ~st;

            //Send unlock command to MCB
            lcbCmdHandler.onTmSafetykeyUnlock();

            if (st == LCBInfo.MCBState.MCB_STATE_LOCK.getId()) {
                //Quick stop, Wait 2 sec, stop animation
                lcbDataTimerHandler.setTmReleaseDelayTimer();
            } else {
                //Wait 6 sec, make sure no 0x02B2
                lcbDataTimerHandler.setTmReleaseCheckTimer();
            }
        }

        Log.i(TAG, "MCB Safety : E-STOP " + trigger + ", event " + st + ",  state " + LCBInfo._mcbLockstate);
        //debug
//        LCBInfo.showMCBString();
    }

    public void onTmSafetykeyReset() {

        Log.i(TAG, "MCB Safety : Reset.");

        lcbDataHandler.onTmSafetykeyPulled(false);

        //Reset treadmill
        lcbCmdHandler.resetTreadmill();
    }

    /* ENClimbmill */

    public void onCmSafetykeyPulled(boolean mast, boolean hand) {

        boolean locked = (mast || hand);

        if (locked) {
            LCBInfo._mcbLockstate = LCBInfo.MCBState.MCB_STATE_LOCK.getId();

            lcbDataHandler.onCmMastSafetykeyPulled(mast);
            lcbDataHandler.onCmHandrailSafetykeyPulled(hand);
        }

        lcbDataTimerHandler.setCmlcbLockStateTimer(locked);
    }

    public void onCmStartControl(boolean value) {
        lcbDataHandler.onCmStartControl(value);
    }

    public void onCmSaftykeyUnlock() {
        lcbCmdHandler.onEnClimbmillSafetykeyUnlock();
    }

    public void onCmSafetykeyRelease() {

        //Reset lcb state to normal
        LCBInfo.resetMCBLockState();

        lcbDataHandler.onCmMastSafetykeyPulled(false);
        lcbDataHandler.onCmHandrailSafetykeyPulled(false);
    }

    public boolean getLCBLockState() {
        return (LCBInfo.getMCBLockState());
    }

    public void startCalibration(boolean start) {
        lcbMachineInfoHandler.startCalibration(start);
    }


    public void getMagnentAngle() {
        lcbMachineInfoHandler.getMagnetAngle();
    }

    public void getMotorType() {
        lcbMachineInfoHandler.getTreadMillMotorType();
    }

    public void startAutoTuneMagnetAngle() {
        lcbMachineInfoHandler.startMagnetAngleAutoTuning(true);
    }

    public void getClimbFrameCalScaling() {
        lcbMachineInfoHandler.getEnClimbClimbmillFrameCalScaling();
    }

    public void setClimbFrameCalScaling(int intValue) {
        lcbMachineInfoHandler.setEnClimbClimbmillFrameCalScaling(intValue);
    }

    /*
    public void onResetMachine() {

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setClassName("com.jht.chimera.view", "com.jht.view.machinesetup.MachineSetup");
        startActivity(intent);

    }*/

    //Lcb Version support auto Serial Number function (cmd 0x7D)
    private boolean checkLcbVerSupportAutoSN(int major, int minor) {
        boolean rtnSupport = true;
        int lcbVendor = DataMatrix.getInt(SettingKeys.General.DATA_LCB_TYPE_INT);

        switch (lcbVendor) {
            case 0x13://(LCBA HUR -Bike) version:005
                if (major < 7) // (version:5 - LCB storage data too small and not support) (ver:6 for JHT NA test )
                    rtnSupport = false;
                break;

            case 0x14://(JIS -1xBike) version:003
                if (major < 3) //
                    rtnSupport = false;
                break;

            case 0x07://(LCB1 S3x,E3x) version:103
                if (major < 104)//103 sometime write because lcb return data later
                    rtnSupport = false;
                break;

            case 0x0A://(LCB2 A3x,A5x) version:104
                if (major < 105)//104 sometime write because lcb return data later
                    rtnSupport = false;
                break;

            case 0x17://(En_climbmill) version:003
                if (major < 3)
                    rtnSupport = false;
                break;

            case 0x0B://(Climbmill)    version:013
                if (major < 14)//13  sometime write because lcb return data later
                    rtnSupport = false;
                break;

            case 0xC3:
            case 0xC7:
            case 0xC8:
                if (major < 100)//ver under 100 not support
                    rtnSupport = false;
                break;
            case 0xC5:
                if (major < 4)//ver under 100 not support
                    rtnSupport = false;
                break;
            case 0xC6://support auto SN
            case 0x1B:
                break;

            default://Other lcb not support auto SN
                rtnSupport = true;
                break;
        }

        return rtnSupport;
    }

    void updateLcb(String filePath) {
        if (!new File(filePath).exists()) {
            Log.e(TAG, "LcbUpdateService file path does not exists: " + filePath);
            return;
        }

        LCBUpdateInterface lcbUpdateInterface = new LCBUpdateInterface(mLcbUpdateCallback, this);

        // stop lcb communication
        lcbCmdHandler.stopAllLcbCommunication();
        SystemClock.sleep(2000);

        boolean bStartLcb = lcbUpdateInterface.startLcbIntoUpdateMode();
        if (!bStartLcb) {
            Log.e(TAG, "LcbUpdateService IntoUpdateMode start Lcb error!");
            return;
        }

        SystemClock.sleep(2000);

        //If LCB already into update mode
        // stop lcb communication
        lcbCmdHandler.stopAllLcbCommunication();
        SystemClock.sleep(2000);

        LcbType machineLcbType = LcbType.LCB_UNKNOWN;
        int lcbVendor = DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT);
        if (lcbVendor == 0x07) {
            machineLcbType = LcbType.LCB_LCB1_Current;
        } else if (lcbVendor == 0x0A) {
            machineLcbType = LcbType.LCB_LCB2;
        } else if (lcbVendor == 0x13) {
            machineLcbType = LcbType.LCB_LCBA;
        } else if (lcbVendor == 0x14) {
            machineLcbType = LcbType.LCB_1x_Bike_LCB;
        } else if (lcbVendor == 0x17) {
            machineLcbType = LcbType.LCB_Commercial_Climbmill;
        } else if (lcbVendor == 0x19) {
            machineLcbType = LcbType.LCB_Retail_Climbmill;
        } else if (lcbVendor == 0x1B) {
            machineLcbType = LcbType.LCB_ATHENA_LCB;
        } else if (lcbVendor == 0x1C) {
            machineLcbType = LcbType.LCB_JHT_AD_Board;
        } else if (lcbVendor == 0x1D) {
            machineLcbType = LcbType.LCB_JIS_ATHENA;
        } else if (lcbVendor == 0x1F) {
            machineLcbType = LcbType.LCB_Performance_Ascent_DC36V;
        } else if (lcbVendor == 0x20) {
            machineLcbType = LcbType.LCB_Performance_Climbmill_DC36V;
        } else if (lcbVendor == 0x21) {
            machineLcbType = LcbType.LCB_Performance_Bike_DC36V;
        } else if (lcbVendor == 0x22) {
            machineLcbType = LcbType.LCB_TCV_FX;
        } else {
            Log.e(TAG, "Update does not have LCB " + lcbVendor);
        }

        lcbUpdateInterface.startLcbUpdateFlow(machineLcbType, filePath);
    }

    ILCBUpdateCallback mLcbUpdateCallback = new ILCBUpdateCallback() {

        @Override
        public void onUpdateLcbTypeError() {
        }

        @Override
        public void onUpdateLcbFilePathError() {
        }

        @Override
        public void onLcbUpdateFinish() {
        }

    };

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent.getAction());

            String path = intent.getStringExtra("path");
            if (path != null) {
                Log.d(TAG, "onReceive path: " + path);
                updateLcb(path);
            }
        }
    };


}
