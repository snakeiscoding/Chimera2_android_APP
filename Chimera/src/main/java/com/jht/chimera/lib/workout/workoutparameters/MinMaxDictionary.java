package com.jht.chimera.lib.workout.workoutparameters;

import android.content.Context;

import com.jht.chimera.R;
import com.jht.data.RangeInfo;
import com.jht.chimera.lib.common.config.RangeInfoManager;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.MinMaxParam;
import com.jht.enums.UnitMeasurement;

/**
 * Links different units to their range through an enum.
 * <p>
 * Created by alex.nguyen on 7/19/2017.
 */
public class MinMaxDictionary {
    private static final float SECONDS_PER_MINUTE = 60f;

    public float min = 0;
    public float max = 0;
    public float defaultValue = 0;
    public float stepSize = 0;
    public int precision = 0;
    public String captionText = "";
    public String unitString = "";
    public UnitMeasurement measurement = null; // null indicates unit is a number, ratio, or has no associated measurement/conversions

    private void copyRangeInfo(RangeInfo rangeInfo) {
        min = rangeInfo.getMin();
        max = rangeInfo.getMax();
        defaultValue = rangeInfo.defaultValue;
        precision = rangeInfo.precision;
        stepSize = rangeInfo.stepSize;
    }

    public static MinMaxDictionary getMinMaxDictionary(MinMaxParam type) {

        MinMaxDictionary minMaxDictionary = new MinMaxDictionary();

        Context context = ContextUtil.getApplicationContext();
        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);

        switch (type) {
            case Age:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.AGE));
                minMaxDictionary.captionText = StringLookup.getString(R.string.age_tc, context);
                minMaxDictionary.unitString = StringLookup.getString(R.string.years_lc, context);
                minMaxDictionary.defaultValue = Math.min(DataMatrix.getInt(DAPIKeys.DATA_USER_AGE_INT), minMaxDictionary.max);
                break;
            case Calories:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.WORKOUT_CALORIES));
                minMaxDictionary.captionText = StringLookup.getString(R.string.calories_tc, context);
                minMaxDictionary.unitString = StringLookup.getString(R.string.cals_lc, context);
                minMaxDictionary.defaultValue = Math.min(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_CALORIES_INT, Math.round(minMaxDictionary.defaultValue)), minMaxDictionary.max);
                break;
            case ConstantWatts:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.CONSTANT_WATTS));
                minMaxDictionary.captionText = StringLookup.getString(R.string.watts_tc, context);
                minMaxDictionary.unitString = StringLookup.getString(R.string.watts_uc, context);
                break;
            case FTP:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.CONSTANT_WATTS));
                minMaxDictionary.stepSize = 1;
                minMaxDictionary.captionText = StringLookup.getString(R.string.ftp_watts_tc, context);
                minMaxDictionary.unitString = StringLookup.getString(R.string.ftp_watts_tc, context);
                // TODO:  Check on this value.  get call is using minMaxDictionary for the default value
                minMaxDictionary.defaultValue = Math.min(DataMatrix.getInt(DAPIKeys.DATA_USER_FTP_INT, Math.round(minMaxDictionary.defaultValue)), minMaxDictionary.max);
                break;
            case MaxWattsPair:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.CONSTANT_WATTS));
                minMaxDictionary.min = minMaxDictionary.min + minMaxDictionary.stepSize;
                minMaxDictionary.defaultValue = minMaxDictionary.min;
                minMaxDictionary.captionText = StringLookup.getString(R.string.max_watts_tc, context);
                minMaxDictionary.unitString = StringLookup.getString(R.string.watts_uc, context);
                break;
            case MinWattsPair:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.CONSTANT_WATTS));
                minMaxDictionary.max = minMaxDictionary.max - minMaxDictionary.stepSize;
                minMaxDictionary.captionText = StringLookup.getString(R.string.min_watts_tc, context);
                minMaxDictionary.unitString = StringLookup.getString(R.string.watts_uc, context);
                break;
            case Distance:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(isMetric ? RangeInfoManager.RangeType.WORKOUT_DISTANCE_KM : RangeInfoManager.RangeType.WORKOUT_DISTANCE_MILES));
                float defaultDistance = DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_DISTANCE_KM_FLOAT);
                if (!isMetric) {
                    defaultDistance = DistanceConvertUtil.convertKilometersToMiles(defaultDistance);
                }
                minMaxDictionary.defaultValue = defaultDistance;

                minMaxDictionary.captionText = StringLookup.getString(R.string.distance_tc, context);
                if(isMetric){
                    minMaxDictionary.unitString = context.getResources().getString(R.string.km_uc);
                    minMaxDictionary.measurement = UnitMeasurement.Kilometer;
                }
                else{
                    minMaxDictionary.unitString = context.getResources().getString(R.string.miles_uc);
                    minMaxDictionary.measurement = UnitMeasurement.Mile;
                }

                break;
            case IntervalCount:
                minMaxDictionary.min = 2;
                minMaxDictionary.max = 10;
                minMaxDictionary.defaultValue = 5;
                minMaxDictionary.stepSize = 1;
                minMaxDictionary.precision = 0;
                minMaxDictionary.captionText = StringLookup.getString(R.string.number_of_intervals_tc, context);
                minMaxDictionary.unitString = "";
                break;
            // Interval and rest times are held in minutes so they match the TIME_GOAL_START
            // convention and render as MM:SS through NumPadDisplay.TimeValueMinutes
            case IntervalTime:
                minMaxDictionary.min = 5f / SECONDS_PER_MINUTE;
                minMaxDictionary.max = 5;
                minMaxDictionary.defaultValue = 30f / SECONDS_PER_MINUTE;
                minMaxDictionary.stepSize = 5f / SECONDS_PER_MINUTE;
                minMaxDictionary.precision = 0;
                minMaxDictionary.captionText = StringLookup.getString(R.string.interval_time_goal_tc, context);
                minMaxDictionary.unitString = "";
                minMaxDictionary.measurement = UnitMeasurement.Minute;
                break;
            case RestTime:
                minMaxDictionary.min = 10f / SECONDS_PER_MINUTE;
                minMaxDictionary.max = 5;
                minMaxDictionary.defaultValue = 30f / SECONDS_PER_MINUTE;
                minMaxDictionary.stepSize = 5f / SECONDS_PER_MINUTE;
                minMaxDictionary.precision = 0;
                minMaxDictionary.captionText = StringLookup.getString(R.string.rest_time_goal_tc, context);
                minMaxDictionary.unitString = "";
                minMaxDictionary.measurement = UnitMeasurement.Minute;
                break;
            case IntervalDistance:
                minMaxDictionary.min = 0.10f;
                minMaxDictionary.stepSize = 0.01f;
                minMaxDictionary.precision = 2;
                minMaxDictionary.captionText = StringLookup.getString(R.string.interval_distance_goal_tc, context);
                if (isMetric) {
                    minMaxDictionary.max = 100f;
                    minMaxDictionary.defaultValue = 5.00f;
                    minMaxDictionary.unitString = context.getResources().getString(R.string.km_uc);
                    minMaxDictionary.measurement = UnitMeasurement.Kilometer;
                } else {
                    minMaxDictionary.max = 62.14f;
                    minMaxDictionary.defaultValue = 3.11f;
                    minMaxDictionary.unitString = context.getResources().getString(R.string.miles_uc);
                    minMaxDictionary.measurement = UnitMeasurement.Mile;
                }
                break;
            case Floors:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.WORKOUT_FLOORS));
                minMaxDictionary.captionText = StringLookup.getString(R.string.floors_tc, context);
                minMaxDictionary.unitString = StringLookup.getString(R.string.floors_uc, context);
                minMaxDictionary.defaultValue = Math.min(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_FLOORS_INT, Math.round(minMaxDictionary.defaultValue)), minMaxDictionary.max);
                break;
            case Elevation:
                minMaxDictionary.min = 100;
                minMaxDictionary.max = 1000;
                minMaxDictionary.defaultValue = 50;
                minMaxDictionary.precision = 0;
                minMaxDictionary.stepSize = 10;
                minMaxDictionary.captionText = StringLookup.getString(R.string.elevation_tc, context);
                minMaxDictionary.unitString = "";
                break;

            case Height:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(isMetric ? RangeInfoManager.RangeType.HEIGHT_CM : RangeInfoManager.RangeType.HEIGHT_IN));
                minMaxDictionary.captionText = StringLookup.getString(R.string.height_tc, context);
                float defaultHeight = DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_HEIGHT_CM_FLOAT);
                if (!isMetric) {
                    defaultHeight = defaultHeight * CommonRoutines.getConvertFactorHeight();
                }
                minMaxDictionary.defaultValue = defaultHeight;

                if(isMetric){
                    minMaxDictionary.unitString = context.getResources().getString(R.string.cm_lc);
                    minMaxDictionary.measurement = UnitMeasurement.Centimeter;
                }
                else{
                    minMaxDictionary.unitString = context.getResources().getString(R.string.inches_lc);
                    minMaxDictionary.measurement = UnitMeasurement.Inch;
                }
                break;
            case Incline:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.INCLINE));
                minMaxDictionary.captionText = StringLookup.getString(R.string.incline_tc, context);
                minMaxDictionary.unitString = "%";
                break;
            case Level:
                minMaxDictionary.min = 1;
                minMaxDictionary.max = DataMatrix.getInt(WorkoutKeys.DATA_NUM_LEVELS_INT);
                minMaxDictionary.defaultValue = Math.min(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT, Math.round(minMaxDictionary.defaultValue)), minMaxDictionary.max);
                minMaxDictionary.precision = 0;
                minMaxDictionary.stepSize = 1;
                minMaxDictionary.captionText = StringLookup.getString(R.string.level_tc, context);
                minMaxDictionary.unitString = "";
                break;
            case MaxIncline:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.INCLINE));
                minMaxDictionary.captionText = StringLookup.getString(R.string.max_incline_tc, context);
                minMaxDictionary.unitString = "";

                break;
            case MaxResistance:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.RESISTANCE));
                minMaxDictionary.captionText = StringLookup.getString(R.string.max_resistance_tc, context);
                minMaxDictionary.unitString = "";
                break;
            case MaxSpeed:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.SPEED_KPH));
                minMaxDictionary.captionText = StringLookup.getString(R.string.max_speed_tc, context);
                if(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)){
                    minMaxDictionary.unitString = context.getResources().getString(R.string.kph_uc);
                    minMaxDictionary.measurement = UnitMeasurement.KilometerPerHour;
                }
                else{
                    minMaxDictionary.unitString = context.getResources().getString(R.string.mph_uc);
                    minMaxDictionary.measurement = UnitMeasurement.MilePerHour;
                }
                break;
            case MaxResistancePair:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.RESISTANCE));
                // The max resistance must be higher than the min resistance
                minMaxDictionary.min = minMaxDictionary.min + 1;
                minMaxDictionary.defaultValue = minMaxDictionary.defaultValue + 1;
                minMaxDictionary.captionText = StringLookup.getString(R.string.max_resistance_tc, context);
                minMaxDictionary.unitString = "";
                break;
            case MinResistancePair:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.RESISTANCE));
                // Min value must always be lower than max value
                minMaxDictionary.max = minMaxDictionary.max - 1;
                minMaxDictionary.captionText = StringLookup.getString(R.string.min_resistance_tc, context);
                minMaxDictionary.unitString = "";
                break;
            case MaxSpeedPair:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.SPEED_KPH));
                // Max value must always be higher than min value
                minMaxDictionary.min = minMaxDictionary.min + minMaxDictionary.stepSize;
                minMaxDictionary.defaultValue = minMaxDictionary.min;
                minMaxDictionary.captionText = StringLookup.getString(R.string.max_speed_tc, context);
                if(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)){
                    minMaxDictionary.unitString = context.getResources().getString(R.string.kph_uc);
                    minMaxDictionary.measurement = UnitMeasurement.KilometerPerHour;
                }
                else{
                    minMaxDictionary.unitString = context.getResources().getString(R.string.mph_uc);
                    minMaxDictionary.measurement = UnitMeasurement.MilePerHour;
                }
                break;
            case MinSpeedPair:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.SPEED_KPH));
                // Max value must always be higher than min value
                minMaxDictionary.max = minMaxDictionary.max - minMaxDictionary.stepSize;
                minMaxDictionary.captionText = StringLookup.getString(R.string.min_speed_tc, context);
                if(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)){
                    minMaxDictionary.unitString = context.getResources().getString(R.string.kph_uc);
                    minMaxDictionary.measurement = UnitMeasurement.KilometerPerHour;
                }
                else{
                    minMaxDictionary.unitString = context.getResources().getString(R.string.mph_uc);
                    minMaxDictionary.measurement = UnitMeasurement.MilePerHour;
                }
                break;
            case PercentMaxHeartRate:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.PERCENT_MAX_HEART_RATE));
                minMaxDictionary.captionText = StringLookup.getString(R.string.percent_max_heart_rate_tc, context);
                minMaxDictionary.unitString = "";
                break;
            case Resistance:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.RESISTANCE));
                minMaxDictionary.captionText = StringLookup.getString(R.string.resistance_tc, context);
                minMaxDictionary.unitString = "";
                break;
            case Speed:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.SPEED_KPH));
                minMaxDictionary.captionText = StringLookup.getString(R.string.speed_tc, context);

                float defaultSpeed = DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT);
                if(defaultSpeed != -1) {
                    if (!isMetric) {
                        // want 0.5 instead of 0.49709696, minMaxDictionary.precision is set above so we can use it here
                        defaultSpeed = round(SpeedConvertUtil.convertKphToMph(defaultSpeed), minMaxDictionary.precision);
                    }
                    minMaxDictionary.defaultValue = defaultSpeed;
                }

                if(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)){
                    minMaxDictionary.unitString = context.getResources().getString(R.string.kph_uc);
                    minMaxDictionary.measurement = UnitMeasurement.KilometerPerHour;
                }
                else{
                    minMaxDictionary.unitString = context.getResources().getString(R.string.mph_uc);
                    minMaxDictionary.measurement = UnitMeasurement.MilePerHour;
                }
                break;
            case TargetSpeed:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.SPEED_KPH));
                minMaxDictionary.captionText = StringLookup.getString(R.string.target_avg_speed_tc, context);

                float defaultTargetSpeed = 25;
                if (!isMetric) {
                    defaultTargetSpeed = SpeedConvertUtil.convertKphToMph(defaultTargetSpeed);
                }
                minMaxDictionary.defaultValue = defaultTargetSpeed;

                if(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)){
                    minMaxDictionary.unitString = context.getResources().getString(R.string.kph_uc);
                    minMaxDictionary.measurement = UnitMeasurement.KilometerPerHour;
                }
                else{
                    minMaxDictionary.unitString = context.getResources().getString(R.string.mph_uc);
                    minMaxDictionary.measurement = UnitMeasurement.MilePerHour;
                }
                break;
            case TargetHeartRate:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.TARGET_HEART_RATE));
                minMaxDictionary.captionText = StringLookup.getString(R.string.thr_uc, context);
                minMaxDictionary.unitString = StringLookup.getString(R.string.bpm_uc, context);
                break;
            case Weight:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(isMetric ? RangeInfoManager.RangeType.WEIGHT_KG : RangeInfoManager.RangeType.WEIGHT_LBS));
                minMaxDictionary.captionText = context.getResources().getString(R.string.weight_tc);

                float defaultWeight = DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT);
                if(defaultWeight != -1) {
                    if (!isMetric) {
                        defaultWeight = WeightConvertUtil.convertKilogramsToPounds(defaultWeight);
                    }
                    minMaxDictionary.defaultValue = defaultWeight;
                }

                if(DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)){
                    minMaxDictionary.unitString = context.getResources().getString(R.string.kg_uc);
                    minMaxDictionary.measurement = UnitMeasurement.Kilogram;
                }
                else{
                    minMaxDictionary.unitString = context.getResources().getString(R.string.lbs_uc);
                    minMaxDictionary.measurement = UnitMeasurement.Pound;
                }
                break;
            case WorkoutTime:
                minMaxDictionary.copyRangeInfo(RangeInfoManager.getRange(RangeInfoManager.RangeType.WORKOUT_TIME_MIN));
                minMaxDictionary.captionText = StringLookup.getString(R.string.workout_time_tc, context);
                minMaxDictionary.unitString = StringLookup.getString(R.string.minutes_lc, context);
                minMaxDictionary.measurement = UnitMeasurement.Minute;
                minMaxDictionary.defaultValue = Math.min(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT, 1), minMaxDictionary.max);
                break;
            case Gender:
                minMaxDictionary.min = 0;
                minMaxDictionary.max = 2;
                minMaxDictionary.defaultValue = DataMatrix.getInt (DAPIKeys.DATA_USER_GENDER_ENUM_INT);
                minMaxDictionary.precision = 0;
                minMaxDictionary.stepSize = 1;
                minMaxDictionary.captionText = StringLookup.getString(R.string.gender_tc, context);
                minMaxDictionary.unitString = "";
            default:
                break;
        }

        return minMaxDictionary;
    }

    private static float round (float value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (float) Math.round(value * scale) / scale;
    }

}
