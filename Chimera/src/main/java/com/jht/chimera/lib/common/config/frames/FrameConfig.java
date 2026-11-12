package com.jht.chimera.lib.common.config.frames;

import static com.jht.androidcommonalgorithms.units.UnitConverter.KPH_PER_MPH;

import com.jht.androidcommonalgorithms.math.ValueRange;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.data.RangeInfo;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class FrameConfig extends RangeDefaults {

    public static final int THIRD_PARTY_MAX_WORKOUT_TIME_SECONDS = 4 * 60 * 60;
    public static final int THIRD_PARTY_MAX_WORKOUT_DISTANCE = 200;
    public static final int MODEL_LIFESTYLE = R.string.lifestyle_tc;
    public static final int MODEL_ENDURANCE = R.string.endurance_tc;
    public static final int MODEL_ENDURANCE_02 = R.string.endurance_02_tc;
    public static final int MODEL_ENDURANCE_MD = R.string.endurance_md_tc;
    public static final int MODEL_PERFORMANCE = R.string.performance_tc;
    public static final int MODEL_PERFORMANCE_02 = R.string.performance_02_tc;
    public static final int MODEL_PERFORMANCE_PLUS = R.string.performance_plus_tc;
    public static final int MODEL_PERFORMANCE_PLUS_02 = R.string.performance_plus_02_tc;
    public static final int MODEL_PERFORMANCE_PLUS_2027 = R.string.performance_plus_2027_tc;
    public static final int MODEL_PERFORMANCE_NEXGEN = R.string.performance_nexgen_tc;
    public static final int MODEL_PERFORMANCE_2027 = R.string.performance_2027_tc;
    public static final int MODEL_PROJECTONE = R.string.project_one_tc; // Place holder and will need Project One translations
    public static final int MINIMUM_FRAME_SERIAL_LENGTH = 7;

    public JHTCommonTypes.MachineClassificationType machineClassType = JHTCommonTypes.MachineClassificationType.Bike;
    public JHTCommonTypes.MachineType machineType = JHTCommonTypes.MachineType.HybridBike;
    public int[] supportLcbTypeList = new int[0];
    public int MODEL = R.string.unknown_uc;
    // TODO - Do we filter on some kind of frame categories or just specify which specific flavors should have them?
    public EnumSet<ProductConfig.BuildFlavor> supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera);
    public boolean WATTS_INACCURATE = false;
    public String defaultAppOverrideId = null;

    public int[] blockedWorkoutIds = null;
    public boolean isBlockedWorkout(int workoutId) {
        // if specific frames must block specific workouts for specific reasons
        if (blockedWorkoutIds == null) return false;
        for (int id : blockedWorkoutIds) {
            if (id == workoutId) return true;
        }
        return false;
    }

    // frame specific ranges
    public RangeInfo inclineLimit = new RangeInfo(); // needed when converting to adc
    public RangeInfo incline = new RangeInfo();
    public RangeInfo inclineAdc = new RangeInfo(); // needed for engineering range
    public RangeInfo resistance = new RangeInfo();
    public RangeInfo speedKph = new RangeInfo();
    public RangeInfo speedRPM = new RangeInfo(); // RPM values that correspond to the kph for the machine.
    public RangeInfo speedMph = new RangeInfo();
    public RangeInfo speedReverseKph = new RangeInfo();
    public RangeInfo speedReverseMph = new RangeInfo();
    public RangeInfo weightKg = new RangeInfo();
    public RangeInfo weightLbs = new RangeInfo();


    // incline ranges
    public int INCLINE_ADC_MIN = 0;
    public int INCLINE_ADC_MAX = 0;
    public int INCLINE_ADC_OFFSET_MIN = 0;
    public int INCLINE_ADC_OFFSET_MAX = 0;
    public boolean USES_REVERSE_INCLINE = false;

    // treadmill reverse belt
    public boolean TM_SUPPORTS_REVERSE_BELT = false;
    public float TM_REVERSE_BELT_MPH_MAX = 0;
    public float TM_REVERSE_BELT_KPH_MAX = 0;

    // motor
    int MOTOR_TYPE = 0;

    // resistance calibration table
    public int[] RESISTANCE_CAL_TABLE = null;
    public static int[] inductionCalPoints = {0, 0, 0, 0, 0};
    public static byte hasInductionCal = 0;
    public static byte validInductionCal = 0;
    public static String dataPoints = "";
    // calibration table
    public int[][] WATTS_TABLE = null;
    public int[][] RESISTANCE_TABLE = null;
    public int TABLE_RPM_START = 10;
    public int TABLE_RPM_STEP = 5;

    // rates
    public float RATE_INCLINE_PERCENT_PER_SEC = 0;
    public float RATE_RESISTANCE_PER_SEC = 0;
    public float RATE_KPH_PER_SEC = 0;

    // safety
    public boolean HAS_TM_ESTOP = false;
    public boolean HAS_TM_LOCK_STATE = false;
    public boolean HAS_IR_SENSOR = false;

    //DC bus current OC
    public boolean HAS_CURRENT_OC = false;
    protected int DC_BUS_TRIGGER_LIMIT_UPPER = 72;
    protected int DC_BUS_TRIGGER_LIMIT_LOWER = 63;
    
	//TM ramp rpm
    public boolean HAS_TM_AUTO_RAMP_UP_AND_LAST_RPM_VALUE = false;
    public int RPM_RAMP_UP_STAGE_VALUE = 300; // Speed Up ramp stage default value
    public int RPM_RAMP_DOWN_STAGE_VALUE = 260; // Speed Down ramp stage default value

    //TM Over Speed
    public boolean HAS_TM_OVER_SPEED = false;

    //TM Cadence
    public boolean HAS_CADENCE = false;

    //CAN communication enabled (2027 TM, CM, and Bike models)
    public boolean HAS_CAN = false;
    public boolean USE_CAN_HEART_RATE = false;

    // climbmill
    public int STEPS_PER_FLOOR = 0;
    public float STEPS_PER_METER = 0; // Climbmill and stepper
    public boolean HAS_CM_CONTROL_ZONE = false; // most frames will not have one cause they are not climbmills

    /**
     * The default bump sensor trigger threshold is -1. This implies the device is not supported.
     * Each frame will need a custom value for this and the setting will be applied when HAL starts.
     */
    public int BUMP_SENSOR_TRIGGER_THRESHOLD = -1;

    // maintenance
    public boolean REQUIRES_BELT_LUBRICATION = false;
    public boolean REQUIRES_CHAIN_LUBRICATION = false;
    public boolean SUPPORT_RETAIL_TM_CALIBRATION = false;

    // True if the LCB can control watts.  We also set this to true if it is ok to dial in the watts
    // in software based on RPM.  This can cause frequent movement so in general should not be done on retail.
    public boolean LCB_CAN_CONTROL_WATTS = false;

    @NonNls protected String FRAME_PREFIX = "";
    int FRAME_ID = 0;

    public boolean isLifestyle = false;
    FrameKeyMaps KeyMap = null;

    // if frame has a power management rule, can assign from suitable constants (these constants may be platform specific and may be checked by platform specific handlers)
    public int powerRule = FramePowerRules.RULE_NONE;

    public FrameConfig() {
        super();
    }

    public void copy(FrameConfig other) {
        super.copy(other);

        machineClassType = other.machineClassType;
        machineType = other.machineType;
        supportLcbTypeList = other.supportLcbTypeList;
        MODEL = other.MODEL;
        supportProductFlavors = other.supportProductFlavors;
        WATTS_INACCURATE = other.WATTS_INACCURATE;
        blockedWorkoutIds = other.blockedWorkoutIds;

        // frame specific ranges
        inclineLimit.copy(other.inclineLimit);
        incline.copy(other.incline);
        inclineAdc.copy(other.inclineAdc);
        resistance.copy(other.resistance);
        speedKph.copy(other.speedKph);
        speedMph.copy(other.speedMph);
        speedReverseKph.copy(speedReverseKph);
        speedReverseMph.copy(speedReverseMph);
        speedRPM.copy(other.speedRPM);
        weightKg.copy(other.weightKg);
        weightLbs.copy(other.weightLbs);

        // incline ranges
        INCLINE_ADC_MIN = other.INCLINE_ADC_MIN;
        INCLINE_ADC_MAX = other.INCLINE_ADC_MAX;
        INCLINE_ADC_OFFSET_MIN = other.INCLINE_ADC_OFFSET_MIN;
        INCLINE_ADC_OFFSET_MAX = other.INCLINE_ADC_OFFSET_MAX;
        USES_REVERSE_INCLINE = other.USES_REVERSE_INCLINE;

        // treadmill reverse belt
        TM_SUPPORTS_REVERSE_BELT = other.TM_SUPPORTS_REVERSE_BELT;

        // motor type
        MOTOR_TYPE = other.MOTOR_TYPE;

        // resistance calibration table
        RESISTANCE_TABLE = other.RESISTANCE_TABLE;
        RESISTANCE_CAL_TABLE = other.RESISTANCE_CAL_TABLE;

        // watts table
        WATTS_TABLE = other.WATTS_TABLE;
        TABLE_RPM_START = other.TABLE_RPM_START;
        TABLE_RPM_STEP = other.TABLE_RPM_STEP;

        // rates
        RATE_INCLINE_PERCENT_PER_SEC = other.RATE_INCLINE_PERCENT_PER_SEC;
        RATE_RESISTANCE_PER_SEC = other.RATE_RESISTANCE_PER_SEC;
        RATE_KPH_PER_SEC = other.RATE_KPH_PER_SEC;

        // safety
        HAS_TM_ESTOP = other.HAS_TM_ESTOP;
        HAS_TM_LOCK_STATE = other.HAS_TM_LOCK_STATE;
        HAS_IR_SENSOR=other.HAS_IR_SENSOR;

        //DC bus current OC
        HAS_CURRENT_OC = other.HAS_CURRENT_OC;

        REQUIRES_BELT_LUBRICATION = other.REQUIRES_BELT_LUBRICATION;
        REQUIRES_CHAIN_LUBRICATION = other.REQUIRES_CHAIN_LUBRICATION;
        SUPPORT_RETAIL_TM_CALIBRATION = other.SUPPORT_RETAIL_TM_CALIBRATION;

        STEPS_PER_FLOOR = other.STEPS_PER_FLOOR;
        STEPS_PER_METER = other.STEPS_PER_METER;
        HAS_CM_CONTROL_ZONE = other.HAS_CM_CONTROL_ZONE;

        FRAME_PREFIX = other.FRAME_PREFIX;
        FRAME_ID = other.FRAME_ID;

        BUMP_SENSOR_TRIGGER_THRESHOLD = other.BUMP_SENSOR_TRIGGER_THRESHOLD;

        LCB_CAN_CONTROL_WATTS = other.LCB_CAN_CONTROL_WATTS;

        //KeyMap = (other.KeyMap == null ? null : other.KeyMap.clone());
        KeyMap = other.KeyMap;  // shallow copy, as this property is only ever read
        isLifestyle = other.isLifestyle;
        powerRule = other.powerRule;
        HAS_TM_AUTO_RAMP_UP_AND_LAST_RPM_VALUE = other.HAS_TM_AUTO_RAMP_UP_AND_LAST_RPM_VALUE;
        RPM_RAMP_UP_STAGE_VALUE = other.RPM_RAMP_UP_STAGE_VALUE;
        RPM_RAMP_DOWN_STAGE_VALUE = other.RPM_RAMP_DOWN_STAGE_VALUE;

        // TM Over Speed
        HAS_TM_OVER_SPEED = other.HAS_TM_OVER_SPEED;

        HAS_CADENCE = other.HAS_CADENCE;
        HAS_CAN = other.HAS_CAN;
        USE_CAN_HEART_RATE = other.USE_CAN_HEART_RATE;

        defaultAppOverrideId = other.defaultAppOverrideId;
    }

    public boolean hasIncline() {
        // used to check for non-zero INCLINE_ADC_MAX, now just see if config set a range
        //return INCLINE_ADC_MAX > 0;
        return !incline.isZero();
    }

    public boolean hasResistance() {
        // used to check if RESISTANCE_TABLE was defined, now just see if config set a range
        //return RESISTANCE_TABLE != null;
        return !resistance.isZero();
    }

    public boolean hasSpeed() {
        return machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill);
    }

    public boolean requiresLubricationPerDistance() {
        return REQUIRES_BELT_LUBRICATION;
    }
    public boolean requiresLubricationPerTime() {
        return REQUIRES_CHAIN_LUBRICATION;
    }
    public boolean requiresBeltLubrication() {
        return REQUIRES_BELT_LUBRICATION;
    }
    public boolean requiresChainLubrication() {
        return REQUIRES_CHAIN_LUBRICATION;
    }

    public String getFramePrefix() {
        return FRAME_PREFIX; //NON-NLS
    }

    public List<String> getFramePrefixes() {
        List<String> prefixes = new ArrayList<>();
        prefixes.add(getFramePrefix());
        return prefixes;
    }

    public String getFrameSerialNumberSuffix(String serialNumber) {
        return serialNumber.substring(getFramePrefix().length());
    }

    @VisibleForTesting
    public String getValidFramePrefix() { return getFramePrefix();  }


    public int getFramePrefixLength() {
        return getFramePrefix().length();
    }

    public boolean framePrefixValid(String frameSerialNumber) {
        return frameSerialNumber.startsWith(getFramePrefix());
    }

    public FrameKeyMaps getKeyMap() { return KeyMap; }

    public boolean supportsLcbType(int lcbType) {
        for (int curLcbType : supportLcbTypeList) {
            if (curLcbType == lcbType) return true;
        }
        return false;
    }

    public int getMotorType() {
        return MOTOR_TYPE;
    }

    public int getModelId() {
        return FRAME_ID;
    }

    public void fixRanges() {
        incline.fix();
        speedKph.fix();
        speedMph.fix();
        resistance.fix();
        weightKg.fix();
        weightLbs.fix();
    }

    public float getDistancePerStep() {
        // TODO - do we need this to be a frame config parameter? add step depth or a step distance?
        if (ProductConfig.isCommercial()) {
            // For commercial we just do the vertical height.  The machine is moving you horizontally.
            // We have the steps per meter.  Convert that to km.
            return (1f / STEPS_PER_METER) / 1000f;
        }
        return 0.000271f; // retail step is approx 23 cm x 14.3 cm for diagonal of 27.08 cm
    }

    public float getEffectiveMinSpeedKph() {
        if (machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill || machineClassType == JHTCommonTypes.MachineClassificationType.Stepper) {
            if (RESISTANCE_CAL_TABLE != null) {
                if (RESISTANCE_CAL_TABLE.length > 0) {
                    return getDistancePerStep() * RESISTANCE_CAL_TABLE[0] * 60;
                }

            } else if (RESISTANCE_TABLE != null) {
                if (RESISTANCE_TABLE.length > 0) {
                    if (RESISTANCE_TABLE[0].length > 0) {
                        return getDistancePerStep() * RESISTANCE_TABLE[0][0] * 60;
                    }
                }
            }
            return 0;
        }
        return speedKph.getMin();
    }

    public float getEffectiveMaxSpeedKph() {
        if (machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill || machineClassType == JHTCommonTypes.MachineClassificationType.Stepper) {
            if(RESISTANCE_CAL_TABLE != null) {
                if(RESISTANCE_CAL_TABLE.length > 0) {
                    int maxLevel = Math.min((int) resistance.getMax(), RESISTANCE_CAL_TABLE.length) - 1;
                    return getDistancePerStep() * RESISTANCE_CAL_TABLE[maxLevel] * 60;
                }

            }
            else if(RESISTANCE_TABLE != null) {
                if(RESISTANCE_TABLE.length > 0) {
                    int maxLevel = Math.min((int) resistance.getMax(), RESISTANCE_TABLE.length) - 1;
                    if(RESISTANCE_TABLE[maxLevel].length > 0) {
                        return getDistancePerStep() * RESISTANCE_TABLE[maxLevel][0] * 60;
                    }
                }
            }
            return 0;
        }
        else if (speedKph.getMinModel() == 0 && speedKph.getMaxModel() == 0 && resistance.getMaxModel() > 0) {
            // ifit would like us to convey a max speed over MatrixComm for RPM based frames
            final int maxRpm = 140; // our watts table tops off at 140 rpm
            float maxWatts = getWatts((int)resistance.getMaxModel(), maxRpm);
            return (float)(SpeedConvertUtil.bikeWattsToMPH(maxWatts) * KPH_PER_MPH);
        } else {
            // If console is imperial and you enter max, the calculated kph can exceed the frame config stated kph max
            float speed = SpeedConvertUtil.convertMphToKph(speedMph.getMax());
            speed = Math.max(speedKph.getMax(), speed);
            // should we round up a little to cover math precision differences? or does not make this even more hacky? since we do same math our side it should be same precision?
            return speed;
        }
    }

    public int getCbusTriggerLimitUpper() {
        return DC_BUS_TRIGGER_LIMIT_UPPER; //NON-NLS
    }
    public int getCbusTriggerLimitLower() {
        return DC_BUS_TRIGGER_LIMIT_LOWER; //NON-NLS
    }

    // moved from Machine.java so we can use in getEffectiveMaxSpeedKph without importing from hardware layer
    // (we need max watts to compute max speed, maybe there is a more streamlined way? is just using the max value in a table cheating?)
    public float getWatts(int resistanceLevel, int rpm) {
        if (this.WATTS_TABLE == null) return 0f;

        int levelIndex = resistanceLevel - 1;
        levelIndex = (int)new ValueRange(0, this.WATTS_TABLE.length - 1).clamp(levelIndex);

        int rpmIndex = (rpm - this.TABLE_RPM_START) / this.TABLE_RPM_STEP;
        int maxRpmIndex = this.WATTS_TABLE[0].length - 1;
        rpmIndex = (int)new ValueRange(0, maxRpmIndex).clamp(rpmIndex);
        if (rpmIndex < maxRpmIndex) {
            return (int)new ValueRange(
                    rpmIndex * this.TABLE_RPM_STEP + this.TABLE_RPM_START,
                    (rpmIndex + 1) * this.TABLE_RPM_STEP + this.TABLE_RPM_START
            ).interpolate(
                    Math.max(rpm, this.TABLE_RPM_START),
                    new ValueRange(this.WATTS_TABLE[levelIndex][rpmIndex], this.WATTS_TABLE[levelIndex][rpmIndex + 1]));
        }
        else {
            return this.WATTS_TABLE[levelIndex][rpmIndex];
        }
    }
}
