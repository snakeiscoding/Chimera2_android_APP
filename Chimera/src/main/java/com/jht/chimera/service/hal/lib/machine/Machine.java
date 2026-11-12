package com.jht.chimera.service.hal.lib.machine;

import static com.jht.androidcommonalgorithms.units.UnitConverter.KPH_PER_MPH;

import android.util.Log;

import com.jht.androidcommonalgorithms.math.ValueRange;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;

public class Machine {

    public static final String TAG = "Machine";

    static FrameConfig mConfig = FrameConfigManager.getInstance().getFrameConfig();

    public static float rpmToKph(int rpm) {
        if (mConfig.MODEL != FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
            if (rpm < mConfig.speedRPM.getMinModel()) {
                return 0f;
            }
        }

        return (float) mConfig.speedRPM.interpolate(rpm, mConfig.speedKph);


    }

    public static int kphToRpm(float kph) {
        if (mConfig.MODEL == FrameConfig.MODEL_PERFORMANCE_PLUS_02) {
            if (Math.round(kph * 10) < Math.round(0.32f * 10)) { //0.2mph = 0.32 kph
                return 0;
            }
        } else {
            if (Math.round(kph * 10) < Math.round(mConfig.speedKph.getMinModel() * 10)) {
                return 0;
            }
        }

        return (int) Math.round(mConfig.speedKph.interpolate(kph, mConfig.speedRPM));
    }

    /*
     * Factory reset , incline max model value = 18% , default max value = 15% ; min model adc value = 3000 , max model adc value = 24000
     * Go > incline 15% adc value = 24000 error
     * If set default max value = 18% , Go > incline 18% adc value = 27XXX error
     * */
    static private int computeteInclineAdcMaxLimit() {
        int adcValue = 0;
        adcValue = (int) (((mConfig.INCLINE_ADC_MAX - mConfig.INCLINE_ADC_MIN) / mConfig.incline.getMaxModel()) * mConfig.incline.getMax()) + mConfig.INCLINE_ADC_MIN;
        return (int) adcValue;
    }

    public static float adcToIncline(int adc) {

        return mConfig.incline.clamp((float) new ValueRange(
                mConfig.INCLINE_ADC_MIN, computeteInclineAdcMaxLimit()).interpolate(
                adc, new ValueRange(mConfig.inclineLimit.getMin(), mConfig.inclineLimit.getMax())));
    }

    public static int inclineToAdc(float incline) {
        return (int) new ValueRange(mConfig.inclineLimit.getMin(), mConfig.inclineLimit.getMax())
                .interpolate(incline, new ValueRange(mConfig.INCLINE_ADC_MIN, computeteInclineAdcMaxLimit()));
    }

    public static float getWatts(int resistanceLevel, int rpm) {
        return mConfig.getWatts(resistanceLevel, rpm); // moved the look up code into FrameConfig
    }

    public static byte validateCalPoints() {
        byte testCalPoints = 0;
        if (FrameConfig.inductionCalPoints[1] == 0 && FrameConfig.inductionCalPoints[3] == 0) {
            if (FrameConfig.inductionCalPoints[0] > 0
                    && FrameConfig.inductionCalPoints[4] > FrameConfig.inductionCalPoints[2]
                    && FrameConfig.inductionCalPoints[2] > FrameConfig.inductionCalPoints[0]) {
                testCalPoints = 1;
            }
        } else if (FrameConfig.inductionCalPoints[0] > 0
                && FrameConfig.inductionCalPoints[4] > FrameConfig.inductionCalPoints[3]
                && FrameConfig.inductionCalPoints[3] > FrameConfig.inductionCalPoints[2]
                && FrameConfig.inductionCalPoints[2] > FrameConfig.inductionCalPoints[1]
                && FrameConfig.inductionCalPoints[1] > FrameConfig.inductionCalPoints[0]) {
            testCalPoints = 1;
        }
        return testCalPoints;
    }

    public static void resistanceCalTable() {
        // TODO - this assumes tables have 30 entries but some frames have less, this code may need adjusting?
        try {
            if (mConfig.RESISTANCE_CAL_TABLE.length < 30 || mConfig.RESISTANCE_TABLE.length < 30) {
                Log.e(TAG, "resistanceCalTable found less than 30 resistance table entries");
            } else {
                int step;
                step = (FrameConfig.inductionCalPoints[2] - FrameConfig.inductionCalPoints[0]) / 14;
                for (int j = 0; j < 14; j++) {
                    mConfig.RESISTANCE_CAL_TABLE[j] = FrameConfig.inductionCalPoints[0] + (j * step);
                }
                step = (FrameConfig.inductionCalPoints[4] - FrameConfig.inductionCalPoints[2]) / 15;
                for (int j = 0; j < 15; j++) {
                    mConfig.RESISTANCE_CAL_TABLE[j + 14] = FrameConfig.inductionCalPoints[2] + (j * step);
                }
                mConfig.RESISTANCE_CAL_TABLE[29] = FrameConfig.inductionCalPoints[4];
                for (int j = 0; j < 30; j++) {
                    mConfig.RESISTANCE_TABLE[j][0] = mConfig.RESISTANCE_CAL_TABLE[j];
                }
                for (int j = 0; j < 30; j++) {
                    Log.i(TAG, "resistance table [" + j + "] = " + mConfig.RESISTANCE_TABLE[j][0]);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "resistanceCalTable threw exception", e);
        }
    }

    public static int getResistance(int level, int rpm) {
        if (mConfig.RESISTANCE_TABLE == null) return 0;

        level--;
        if (mConfig.RESISTANCE_TABLE.length == 1) {
            if (level < 0) {
                return mConfig.RESISTANCE_TABLE[0][0];
            } else if (level >= mConfig.RESISTANCE_TABLE.length) {
                return mConfig.RESISTANCE_TABLE[0][mConfig.RESISTANCE_TABLE.length];
            }
            return mConfig.RESISTANCE_TABLE[0][level];
        }
        if (level >= mConfig.RESISTANCE_TABLE.length || level < 0 || mConfig.RESISTANCE_TABLE[level].length <= 0) {
            //Log.d(TAG, "getResistance level to large " + level + " " + mConfig.RESISTANCE_TABLE.length);
            return 0;
        } else if (rpm <= mConfig.TABLE_RPM_START || mConfig.RESISTANCE_TABLE[level].length == 1) {
            //Log.d(TAG, "getResistance get first resistance");
            return mConfig.RESISTANCE_TABLE[level][0];
        } else {
            int rpmStartIndex = (rpm - mConfig.TABLE_RPM_START) / mConfig.TABLE_RPM_STEP;
            if (rpmStartIndex >= mConfig.RESISTANCE_TABLE[level].length - 1) {
                //Log.d(TAG, "getResistance RPM too high ");
                return mConfig.RESISTANCE_TABLE[level][mConfig.RESISTANCE_TABLE[level].length - 1];
            }
            int rpmEndIndex = rpmStartIndex + 1;

            if (rpm % mConfig.TABLE_RPM_STEP == 0) {
                //Log.d(TAG, "getResistance rpm % 5 = 0 ");

                return mConfig.RESISTANCE_TABLE[level][rpmStartIndex];
            }
            // Log.d(TAG, "getResistance interpolate " + mConfig.RESISTANCE_TABLE[level][rpmStartIndex] + " " + mConfig.RESISTANCE_TABLE[level][rpmEndIndex]);
            ValueRange rpmRange = new ValueRange(1, 4);
            ValueRange resistanceRange = new ValueRange(mConfig.RESISTANCE_TABLE[level][rpmStartIndex], mConfig.RESISTANCE_TABLE[level][rpmEndIndex]);
            return (int) (rpmRange.interpolate(rpm % 5, resistanceRange) + 0.5);
        }
    }

    /**
     * Gets the closest value of resistance to
     *
     * @param targetWatts The watts to target the resistance to
     * @param rpm         The rpm of the frame
     * @return The resistance to match the target watts
     */
    public static int getResistanceFromWatts(int targetWatts, int rpm) {
        if (mConfig.WATTS_TABLE == null) return 0;
        if (mConfig.RESISTANCE_TABLE == null) return 0;

        //Double2DArrayField watts = !selfPower ? wattsTable : selfPowerWattsTable;
        //Int2DArrayField resistance = !selfPower ? resistanceTable : selfPowerResistanceTable;

        double[] levelWatts = new double[mConfig.WATTS_TABLE.length];
        double[] levelResistance = new double[mConfig.RESISTANCE_TABLE.length];

        int rpmIndex;
        if (rpm < mConfig.TABLE_RPM_START) {
            rpmIndex = 0;
            for (int i = 0; i < mConfig.WATTS_TABLE.length; i++) {
                levelWatts[i] = mConfig.WATTS_TABLE[i][rpmIndex];
                levelResistance[i] = mConfig.RESISTANCE_TABLE[i][rpmIndex];
            }
        } else if (rpm >= mConfig.TABLE_RPM_START + (mConfig.WATTS_TABLE.length - 1) * mConfig.TABLE_RPM_STEP) {
            rpmIndex = mConfig.WATTS_TABLE.length - 1;
            for (int i = 0; i < mConfig.WATTS_TABLE.length; i++) {
                levelWatts[i] = mConfig.WATTS_TABLE[i][rpmIndex];
                levelResistance[i] = mConfig.RESISTANCE_TABLE[i][rpmIndex];
            }
        } else {
            rpmIndex = (rpm - mConfig.TABLE_RPM_START) / mConfig.TABLE_RPM_STEP;
            for (int i = 0; i < mConfig.WATTS_TABLE.length; i++) {
                ValueRange rpmRange = new ValueRange(rpmIndex * 5 + 10, (rpmIndex + 1) * 5 + 10);
                ValueRange wattsRange = new ValueRange(mConfig.WATTS_TABLE[i][rpmIndex], mConfig.WATTS_TABLE[i][rpmIndex + 1]);
                ValueRange resistanceRange = new ValueRange(mConfig.RESISTANCE_TABLE[i][rpmIndex], mConfig.RESISTANCE_TABLE[i][rpmIndex + 1]);
                levelWatts[i] = rpmRange.interpolate(rpm, wattsRange);
                levelResistance[i] = rpmRange.interpolate(rpm, resistanceRange);
            }
        }
        int levelIndex = 0;
        for (double levelWatt : levelWatts) {
            if (levelWatt > targetWatts)
                break;
            levelIndex++;
        }
        if (levelIndex >= levelWatts.length - 1) {
            return (int) levelResistance[levelResistance.length - 1];
        } else {

            ValueRange wattsRange = new ValueRange(levelWatts[levelIndex], levelWatts[levelIndex + 1]);
            ValueRange resistanceRange = new ValueRange(levelResistance[levelIndex], levelResistance[levelIndex + 1]);
            return ((int) (wattsRange.interpolate(targetWatts, resistanceRange) + 0.5));
        }
    }

    static private void resetComputedData(MachineData data) {
        data.kph = 0;
        data.incline = 0;
        data.caloriesPerSecond = 0;
        data.mets = 1.0f;
        data.watts = 0;
    }

    static private void resetComputedSpinBikedData(MachineData data) {
        data.kph = 0;
        data.incline = 0;
        data.caloriesPerSecond = 0;
        data.mets = 1.0f;
    }

    static public void computeTreadmill(MachineData data, float massKg) {

        resetComputedData(data);

        data.kph = rpmToKph(data.rpm);
        data.incline = ((int) (adcToIncline(data.inclineADC) * 10.0f + 0.5f)) / 10.0f;

        float targetMPH = data.targetKPH / (float) KPH_PER_MPH;
        data.caloriesPerSecond = targetMPH > 4.0 ?
                (((targetMPH * 26.8f * 0.2f) + data.targetIncline / 100.0f * (targetMPH * 26.8f * 0.9f) + 3.5f) * massKg * 0.005f) / 60.0f :
                (((targetMPH * 26.8f * 0.1f) + data.targetIncline / 100.0f * (targetMPH * 26.8f * 1.8f) + 3.5f) * massKg * 0.005f) / 60.0f;
        data.mets = targetMPH > 4.0f ?
                (3.5f + 5.36f * targetMPH + 24.14f * targetMPH * data.targetIncline / 100.0f) / 3.5f :
                (3.5f + 2.68f * targetMPH + 48.28f * targetMPH * data.targetIncline / 100.0f) / 3.5f;
        data.watts = (data.caloriesPerSecond * 3600.0f) / 4.2f;
    }

    static public void computeSpinBike(MachineData data, float massKg) {
        //Log.d (TAG, "computeSpinBike Watts=" + data.watts);

        resetComputedSpinBikedData(data);

        if (data.rpm > 0) {
            data.kph = (float) (SpeedConvertUtil.bikeWattsToMPH(data.watts) * KPH_PER_MPH);
            if (data.watts > 0) {
                data.caloriesPerSecond = data.watts > 0f ? (((data.watts * 12.0f) + (3.5f * massKg)) * 0.005f) / 60.0f : 0f;
                data.mets = data.watts > 0f ? (data.watts * 12f + 3.5f * massKg) / massKg / 3.5f : 0f;
            }
        }
    }

    static public void computeBike(MachineData data, float massKg, boolean forceWatts, float wattsValue) {
        resetComputedData(data);

        if (data.rpm > 0) {
            if (forceWatts) {
                data.watts = wattsValue;
            } else {
                data.watts = getWatts(data.resistanceLevel, data.rpm);
            }
            data.kph = (float) (SpeedConvertUtil.bikeWattsToMPH(data.watts) * KPH_PER_MPH);
            if (data.watts > 0) {
                data.caloriesPerSecond = data.watts > 0f ? (((data.watts * 12.0f) + (3.5f * massKg)) * 0.005f) / 60.0f : 0f;
                data.mets = data.watts > 0f ? (data.watts * 12f + 3.5f * massKg) / massKg / 3.5f : 0f;
            }
        }
    }

    static public void computeEllipticalAscent(MachineData data, float massKg, boolean forceWatts, float wattsValue) {
        resetComputedData(data);

        if (data.rpm > 0) {
            if (forceWatts) {
                data.watts = wattsValue;
            } else {
                data.watts = getWatts(data.resistanceLevel, data.rpm);
            }
            float mph = (data.rpm * 0.07f) + (data.watts * 0.015f);
            data.kph = mph * (float) KPH_PER_MPH;
            data.caloriesPerSecond = data.watts > 0f ? (((mph * 26.8f * 0.2f) + 3.5f) * (massKg) * 0.005f) / 60.0f : 0f;
            data.mets = data.watts > 0f ? (data.watts * 12f + 3.5f * massKg) / massKg / 3.5f : 0f;
        }

        data.incline = (int) (adcToIncline(data.inclineADC) + 0.5f);
    }

    private static final float CLIMB_MILL_SECANT = 1.119f;

    static public void computeClimbmill(MachineData data, float massKg) {

        resetComputedData(data);

        if (data.spm > 0) {
            if (data.wattsLCB == 0) {
                data.watts = massKg * 9.8f * CLIMB_MILL_SECANT * data.spm * 0.0033867f;
            } else {
                // spm is read by LCB
                data.watts = data.wattsLCB; // watts is read by LCB
            }
            float vo2;
            vo2 = (0.2f * data.spm) / 4f + (1.33f * 1.8f * (1.0f / mConfig.STEPS_PER_METER) * data.spm) * 0.650f + 3.5f;
            data.mets = vo2 / 3.5f;
            data.caloriesPerSecond = (vo2 * massKg) / 200.0f / 60.0f;
        }
    }


    /**
     * Helper function to compute the watts for a stepper.
     *
     * @param rpm The current RPM.
     * @return The current watts.
     */
    private static float stepperWatts(int rpm) {
        float spm = rpm * 1.6f;
        float humanWatts = spm * spm / 100f * 2.16f;
        float slopePoint;
        float humanWattsFix;
        float machineWatts;

        if (rpm >= 25)
            slopePoint = (8020f - (29f * spm)) / 140f;
        else
            slopePoint = (768f - (12f * spm)) / 6f;
        if (slopePoint < 0) {
            slopePoint = 0;
        }

        humanWattsFix = (humanWatts * slopePoint) / 64f;

        if (rpm == 20)
            humanWattsFix += 1;
        else if (rpm == 21)
            humanWattsFix += 3;
        else if (rpm == 22)
            humanWattsFix += 2;
        else if (rpm == 23)
            humanWattsFix += 4;
        else if (rpm == 24)
            humanWattsFix += 1;

        if (rpm < 25)
            machineWatts = 0f;
        else
            machineWatts = 22f + (.72f * (rpm - 25f));

        return humanWattsFix + machineWatts;
    }

    static public void computeStepper(MachineData data, float massKg) {

        resetComputedData(data);

        if (data.rpm >= 0) {
            data.spm = (int) (data.rpm * 1.6f);
            data.watts = stepperWatts(data.rpm);
            if (data.watts > 0) {
                data.mets = ((data.watts + (35f * massKg)) / massKg / 3.5f) * 0.1f;
                data.caloriesPerSecond = ((data.watts * 12f) + (3.5f * massKg)) * 50f / 60f / 10000f;
            }
        }
    }

}
