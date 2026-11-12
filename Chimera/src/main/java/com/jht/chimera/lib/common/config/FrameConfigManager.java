package com.jht.chimera.lib.common.config;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.units.UnitConverter;
import com.jht.chimera.lib.common.SystemPropertyUtil;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.datamatrixlib.DMCallback;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.VisibleForTesting;

public class FrameConfigManager {
    public final String TAG = "FrameConfigManager";
    protected DataMatrix mDataMatrix = new DataMatrix();
    protected FrameConfig mFrameConfig = new FrameConfig();
    protected FrameConfigEnum mFrameType;
    protected FrameConfig mStockFrameConfig;
    public final Object FRAME_UPDATE_LOCK = new Object();

    private FrameConfigManager() {
        mFrameType = FrameConfigEnum.getFrameTypeFromString(DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING));
        mStockFrameConfig = (mFrameType == null ? null : mFrameType.createFrameConfig());
        registerSettingCallbacks();
        updateSettings();
        mFrameConfig.fixRanges();
    }

    public void setFrame(FrameConfigEnum frame) {
        DataMatrix.putString(SettingKeys.General.DATA_FRAME_TYPE_STRING, frame.name());
        setFrameType(frame);
        SystemPropertyUtil.set("persist.jht.frame", frame.name());
        DataMatrix.sendEvent(HALKeys.ACTION_SETUP_FRAME);
    }

    public void refreshFrame() {
        String strFrameType = DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING);
        FrameConfigEnum frameType = FrameConfigEnum.getFrameTypeFromString(strFrameType);
        setFrameType(frameType);
    }

    public FrameConfig getFrameConfig() {
        return mFrameConfig;
    }

    private static class SingletonHolder {
        private static final FrameConfigManager INSTANCE = new FrameConfigManager();
    }

    public static FrameConfigManager getInstance() {
        return FrameConfigManager.SingletonHolder.INSTANCE;
    }

    private final DMCallback<Integer> mIntCallback = value -> updateSettings();
    private final DMCallback<Float> mFloatCallback = value -> updateSettings();
    protected void registerSettingCallbacks() {
        mDataMatrix.createIntAdapter(DAPIKeys.DATA_USER_AGE_INT, this::updateAge, true);
        mDataMatrix.createFloatAdapter(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, this::updateWeight, true);
        mDataMatrix.createFloatAdapter(SettingKeys.Workout.DATA_MAX_INCLINE_FLOAT, mFloatCallback, false);
        mDataMatrix.createIntAdapter(SettingKeys.Workout.DATA_MAX_CLIMBMILL_LEVEL_INT, mIntCallback, false);
        mDataMatrix.createIntAdapter(SettingKeys.Workout.DATA_MAX_LIFESTYLE_CLIMBMILL_LEVEL_INT, mIntCallback, false);
        mDataMatrix.createIntAdapter(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT, mIntCallback, false);
        mDataMatrix.createFloatAdapter(HALKeys.DATA_MAX_WORKOUT_SPEED_KPH_FLOAT, value -> updateTreadmillSpeed(), false);
        mDataMatrix.createFloatAdapter(HALKeys.DATA_MAX_WORKOUT_SPEED_MPH_FLOAT, value -> updateTreadmillSpeed(), true);
        mDataMatrix.createIntAdapter(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT, mIntCallback, false);
        mDataMatrix.createIntAdapter(SettingKeys.Workout.DATA_MAX_WORKOUT_CALORIES_INT, mIntCallback, false);
        mDataMatrix.createFloatAdapter(SettingKeys.Workout.DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT, mFloatCallback, false);
        mDataMatrix.createIntAdapter(SettingKeys.Workout.DATA_MAX_WORKOUT_FLOORS_INT, mIntCallback, false);
        mDataMatrix.createIntAdapter(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT, mIntCallback, false);
        mDataMatrix.createIntAdapter(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT, mIntCallback, false);
        mDataMatrix.createIntAdapter(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT, mIntCallback, false);
        mDataMatrix.createIntAdapter(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT, mIntCallback, false);

        mDataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, value -> refreshFrame(), false);

        // only register for DATA_TM_REVERSE_BELT_BOOL if frame supports reverse belt direction
        if (mStockFrameConfig != null && mStockFrameConfig.TM_SUPPORTS_REVERSE_BELT) {
            mDataMatrix.createBoolAdapter(HALKeys.DATA_TM_IS_REVERSE_BELT_BOOL, value -> {
                // use the reverse belt max if in reverse belt direction; otherwise, restore to default
                if (value) {
                    float reverseKphMax = DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_REVERSE_FLOAT);
                    if (reverseKphMax <= 0f) reverseKphMax = mStockFrameConfig.speedReverseKph.getMax();
                    mFrameConfig.speedKph.setMin(mStockFrameConfig.speedReverseKph.getMin());
                    mFrameConfig.speedKph.setMax(reverseKphMax);

                    float reverseMphMax = DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_REVERSE_FLOAT);
                    if (reverseMphMax <= 0f) reverseMphMax = mStockFrameConfig.speedReverseMph.getMax();
                    mFrameConfig.speedMph.setMin(mStockFrameConfig.speedReverseMph.getMin());
                    mFrameConfig.speedMph.setMax(reverseMphMax);
                }
                else {
                    updateTreadmillSpeed();
                }
            }, false);
        }
    }


    private void setFrameType(FrameConfigEnum frameType) {
        if (frameType != mFrameType) {
            mFrameType = frameType;
            if (mFrameType != null) {
                mStockFrameConfig = frameType.createFrameConfig();
                resetSettings(); // new frame, purge old calibration and user config frame data.
                updateSettings();
            } else {
                mStockFrameConfig = null;
            }
        }
    }

    public FrameConfigEnum getFrameType() {
        return mFrameType;
    }

    public void updateSettings() {
        if (mStockFrameConfig == null) return;

        synchronized (FRAME_UPDATE_LOCK) {
            mFrameConfig.copy(mStockFrameConfig);

            // the max incline is defined in engineering setting
            float maxIncline = DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_INCLINE_FLOAT);
            if (maxIncline >= 0) {
                mFrameConfig.incline.setMax(maxIncline);
                mFrameConfig.inclineLimit.setMax(maxIncline);
            } else {
                DataMatrix.updateFloat(SettingKeys.Workout.DATA_MAX_INCLINE_FLOAT, mFrameConfig.incline.getMax());
            }

            // the max resistance is defined in engineering setting for a climbmill
            if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
                if (mFrameConfig.isLifestyle) {
                    mFrameConfig.resistance.setMax(mFrameConfig.resistance.clampModel(DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_LIFESTYLE_CLIMBMILL_LEVEL_INT)));
                } else {
                    mFrameConfig.resistance.setMax(mFrameConfig.resistance.clampModel(DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_CLIMBMILL_LEVEL_INT)));
                }
            }
            // the default resistance is defined in engineering settings
            mFrameConfig.resistance.defaultValue = mFrameConfig.resistance.clampModel(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT));

            // the max workout time is defined in engineering setting
            mFrameConfig.workoutTime.setMax(DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT));

            // the max workout calories is defined in engineering setting
            mFrameConfig.workoutCalories.setMax(DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_CALORIES_INT));

            // the max workout distance is defined in engineering setting
            mFrameConfig.workoutDistanceKm.setMax(DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT));
            mFrameConfig.workoutDistanceMi.setMax(DistanceConvertUtil.convertKilometersToMiles(mFrameConfig.workoutDistanceKm.getMax()));

            // the max workout floor is defined in engineering setting
            mFrameConfig.workoutFloors.setMax(DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_FLOORS_INT));

            //  custom calibration ADC min/max
            if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                int inclineADCMin = DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT, -1);
                if (inclineADCMin >= 0) {
                    mFrameConfig.INCLINE_ADC_MIN = inclineADCMin;
                } else {
                    DataMatrix.updateInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT, mFrameConfig.INCLINE_ADC_MIN);
                }
                int inclineADCMax = DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT);
                if (inclineADCMax >= 0) {
                    mFrameConfig.INCLINE_ADC_MAX = inclineADCMax;
                } else {
                    DataMatrix.updateInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT, mFrameConfig.INCLINE_ADC_MAX);
                }


                //Incline Offset
                int inclineADCMaxOffset = DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT);
                if (inclineADCMaxOffset >= 0) {
                    mFrameConfig.INCLINE_ADC_OFFSET_MAX = Math.max(inclineADCMaxOffset, mFrameConfig.INCLINE_ADC_OFFSET_MAX);
                } else {
                    DataMatrix.updateInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT, mFrameConfig.INCLINE_ADC_OFFSET_MAX);
                }

                int inclineADCMinOffset = DataMatrix.getInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT);
                if (inclineADCMinOffset >= 0) {
                    mFrameConfig.INCLINE_ADC_OFFSET_MIN = Math.max(inclineADCMaxOffset, mFrameConfig.INCLINE_ADC_OFFSET_MIN);
                } else {
                    DataMatrix.updateInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT, mFrameConfig.INCLINE_ADC_OFFSET_MIN);
                }
            }

            // Send the bump sensor default value if the data matrix doesn't contain a value.
            if (mFrameConfig.BUMP_SENSOR_TRIGGER_THRESHOLD != -1 && DataMatrix.getInt(SettingKeys.ProjectOne.DATA_BUMP_SENSOR_THRESHOLD_INT) == -1) {
                DataMatrix.updateInt(SettingKeys.ProjectOne.DATA_BUMP_SENSOR_THRESHOLD_INT, mFrameConfig.BUMP_SENSOR_TRIGGER_THRESHOLD);
            }

            updateTreadmillSpeed();
            mFrameConfig.fixRanges();
        }
    }

    private void updateAge(int age){
        // the default is the pushed by DAPI (user age or engineering setting)
        mFrameConfig.age.defaultValue = mFrameConfig.age.clamp(age);
    }

    private void updateWeight(float weight){
        // the default is the pushed by DAPI (user weight or engineering setting)
        mFrameConfig.weightKg.defaultValue = mFrameConfig.weightKg.clamp(weight);
        mFrameConfig.weightLbs.defaultValue = mFrameConfig.weightLbs.clamp(Math.round(WeightConvertUtil.convertKilogramsToPounds(weight)));
    }

    private void updateTreadmillSpeed(){
        if (mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
            // the max speed is defined in engineering setting
            mFrameConfig.speedKph.setMax(DataMatrix.getFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_KPH_FLOAT, -1));
            mFrameConfig.speedMph.setMax(DataMatrix.getFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_MPH_FLOAT, -1));
            if (mFrameConfig.speedKph.getMax() <= 0) {
                mFrameConfig.speedKph.setMax(DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_FLOAT));
                mFrameConfig.speedMph.setMax(DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_FLOAT));
            }
            if (mFrameConfig.speedKph.getMax() <= 0) {
                mFrameConfig.speedKph.setMax(mFrameConfig.speedKph.getMaxModel());
                mFrameConfig.speedMph.setMax(mFrameConfig.speedMph.getMaxModel());
            }

            // the default speed is defined in engineering settings
            float startSpeedKph = DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT);
            if (startSpeedKph >= 0) {
                mFrameConfig.speedKph.defaultValue = startSpeedKph;
                mFrameConfig.speedMph.defaultValue = SpeedConvertUtil.convertKphToMph(startSpeedKph);
            } else {
                DataMatrix.updateFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT, mFrameConfig.speedKph.defaultValue);
            }
        }
    }

    /**
     * This is called when the frame type is changed.  Here we need to reset the user controlled frame settings to the defaults.
     * This includes incline, speed, resistance, and calibration numbers.  These can vary from machine to machine so we should
     * reset them.  Also the max distance is different between bikes and non bikes so reset that too.
     */
    public void resetSettings() {
        if (mStockFrameConfig == null) return;

        // Stop all callbacks as we don't want our changes looping back to us.
        mDataMatrix.finish();


        // Reset the incline max.
        DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_INCLINE_FLOAT, mStockFrameConfig.incline.getMax());
        mFrameConfig.incline.setMax(mStockFrameConfig.incline.getMax());
        mFrameConfig.inclineLimit.setMax(mStockFrameConfig.incline.getMax());

        // Reset the climbmill max resistance.
        if (mStockFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
            if(mStockFrameConfig.isLifestyle) {
                DataMatrix.putInt(SettingKeys.Workout.DATA_MAX_LIFESTYLE_CLIMBMILL_LEVEL_INT, 25);
            }
            else {
                DataMatrix.putInt(SettingKeys.Workout.DATA_MAX_CLIMBMILL_LEVEL_INT, 20);
            }
        }
        mFrameConfig.resistance.setMax(mStockFrameConfig.resistance.getMax());

        // Reset speed an treadmill calibration.
        if (mStockFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {

            // Reset the workout speed.
            DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_KPH_FLOAT, mStockFrameConfig.speedKph.getMax());
            DataMatrix.putFloat(HALKeys.DATA_MAX_WORKOUT_SPEED_MPH_FLOAT, mStockFrameConfig.speedMph.getMax());
            DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_FLOAT, mStockFrameConfig.speedKph.getMax());
            DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_FLOAT, mStockFrameConfig.speedMph.getMax());
            mFrameConfig.speedKph.setMax(mStockFrameConfig.speedKph.getMax());
            mFrameConfig.speedMph.setMax(mStockFrameConfig.speedMph.getMax());

            // Reset the reverse workout speed settings.
            if (mStockFrameConfig.TM_SUPPORTS_REVERSE_BELT) {
                DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_REVERSE_FLOAT, mStockFrameConfig.speedReverseKph.getMax());
                DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_REVERSE_FLOAT, mStockFrameConfig.speedReverseMph.getMax());
            }

            // Reset the start speed.
            DataMatrix.putFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT, mStockFrameConfig.speedKph.defaultValue);
            mFrameConfig.speedKph.defaultValue = mStockFrameConfig.speedKph.defaultValue;
            mFrameConfig.speedMph.defaultValue = mStockFrameConfig.speedMph.defaultValue;

            // Reset the incline ADC.
            DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MAX_INT, mStockFrameConfig.INCLINE_ADC_MAX);
            DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_INLINE_ADC_MIN_INT, mStockFrameConfig.INCLINE_ADC_MIN);
            DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MAX_INT, mStockFrameConfig.INCLINE_ADC_OFFSET_MAX);
            DataMatrix.putInt(SettingKeys.Calibration.DATA_TREADMILL_INCLINE_ADC_OFFSET_MIN_INT, mStockFrameConfig.INCLINE_ADC_OFFSET_MIN);
            mFrameConfig.INCLINE_ADC_MAX = mStockFrameConfig.INCLINE_ADC_MAX;
            mFrameConfig.INCLINE_ADC_MIN = mStockFrameConfig.INCLINE_ADC_MIN;
            mFrameConfig.INCLINE_ADC_OFFSET_MAX = mStockFrameConfig.INCLINE_ADC_OFFSET_MAX;
            mFrameConfig.INCLINE_ADC_OFFSET_MIN = mStockFrameConfig.INCLINE_ADC_OFFSET_MIN;
        }

        // Reset the max distance
        if(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL)) {
            DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT, mStockFrameConfig.workoutDistanceKm.getMax());
        }
        else {
            DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT, (float)UnitConverter.ConvertMilesToKm(mStockFrameConfig.workoutDistanceMi.getMax()));
        }
        mFrameConfig.workoutDistanceKm.setMax(mStockFrameConfig.workoutDistanceKm.getMax());
        mFrameConfig.workoutDistanceMi.setMax(mStockFrameConfig.workoutDistanceMi.getMax());

        // Reset the bump sensor threshold key to it's default value of -1.
        DataMatrix.putInt(SettingKeys.ProjectOne.DATA_BUMP_SENSOR_THRESHOLD_INT, -1);

        // Re-register all callbacks.
        registerSettingCallbacks();

        // Reset TM548 OC defauly enabled.
        if(mStockFrameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02 || mStockFrameConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS) {
            //OC default enabled
            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_ENABLE_OVER_CURRENT_PROTECTION_BOOL, true);
        }

        mFrameConfig.fixRanges();

    }

    @VisibleForTesting
    public String getValidFramePrefix() { return mStockFrameConfig.getValidFramePrefix();  }

    public String getFramePrefix() {
        return mStockFrameConfig.getFramePrefix();
    }

    public String getFrameSerialNumberSuffix(String serialNumber) {
        return mStockFrameConfig.getFrameSerialNumberSuffix(serialNumber);
    }

    public int getFramePrefixLength() {
        return mFrameConfig.getFramePrefixLength();
    }

    public boolean framePrefixValid(String frameSerialNumber) {
        return !FrameConfigEnum.framePrefixKnown(frameSerialNumber) || mStockFrameConfig.framePrefixValid(frameSerialNumber);
    }

    public int getMotorType() {
        return mStockFrameConfig.getMotorType();
    }

    public int getModelId() {
        return mStockFrameConfig.getModelId();
    }

    public boolean checkOverCurrentEnabled() {
        return
                mFrameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill &&
                DataMatrix.getBoolean(SettingKeys.Hardware.DATA_ENABLE_OVER_CURRENT_PROTECTION_BOOL) &&
                !DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) &&
                DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_RUNNING;
    }
}

