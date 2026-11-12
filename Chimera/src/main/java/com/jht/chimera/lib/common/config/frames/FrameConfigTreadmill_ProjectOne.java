package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class FrameConfigTreadmill_ProjectOne extends FrameConfig {
    public FrameConfigTreadmill_ProjectOne() {
        super();
        machineClassType = JHTCommonTypes.MachineClassificationType.Treadmill;
        machineType = JHTCommonTypes.MachineType.Treadmill;
        FRAME_PREFIX = "TM545"; //NON-NLS
        FRAME_ID = 132;
        supportLcbTypeList = new int[]{0xA1};
        MODEL = MODEL_PROJECTONE;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.ProjectOne);

        incline.setMin(incline.setMinModel(0));
        incline.setMaxModel(18);
        incline.setMax(15);
        incline.defaultValue = 0;
        incline.precision = 1;
        incline.stepSize = .5f;
        inclineLimit.copy(incline);

        speedKph.setMin(speedKph.setMinModel(.8f));
        speedKph.setMaxModel(25f);
        speedKph.setMax(20f);
        speedKph.defaultValue = speedKph.getMin();
        speedKph.precision = 1;
        speedKph.stepSize = .1f;

        speedMph.setMin(speedMph.setMinModel(.5f));
        speedMph.setMaxModel(15.5f);
        speedMph.setMax(12f);
        speedMph.defaultValue = speedMph.getMin();
        speedMph.precision = 1;
        speedMph.stepSize = .1f;

        weightKg.setMin(weightKg.setMinModel(WeightConvertUtil.convertLbsToKg(50)));
        weightKg.setMax(weightKg.setMaxModel(WeightConvertUtil.convertLbsToKg(500)));
        weightKg.defaultValue = WeightConvertUtil.convertLbsToKg(150);
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightLbs.setMinModel(50));
        weightLbs.setMax(weightLbs.setMaxModel(500));
        weightLbs.defaultValue = 150;
        weightLbs.precision = 0;
        weightLbs.stepSize = 1;

        // incline ranges
        INCLINE_ADC_OFFSET_MIN = 1152;
        INCLINE_ADC_OFFSET_MAX = 2816; //22*128
        INCLINE_ADC_MIN = 1848 + INCLINE_ADC_OFFSET_MIN;  /*3000 = 1848 + INCLINE_ADC_OFFSET_MIN*/
        INCLINE_ADC_MAX = 23616 - INCLINE_ADC_OFFSET_MAX;  /*20800 = 23616 - INCLINE_ADC_OFFSET_MAX*/
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;
        USES_REVERSE_INCLINE = true;

        // speed rpm kph ratio
        speedRPM.updateRange(178, 5400);

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 0.47f;
        RATE_KPH_PER_SEC = SpeedConvertUtil.convertMphToKph(0.5f);

        // safety
        HAS_TM_ESTOP = true;
        HAS_TM_LOCK_STATE = false;

        // Set the bump sensor trigger threshold to 40
        BUMP_SENSOR_TRIGGER_THRESHOLD = 40;

        KeyMap = FrameKeyMaps.Commercial_Treadmill;
    }
    private static final String IE2_1_FRAME_PREFIX = "TM538"; // NON-NLS
    private static final String IE3_FRAME_PREFIX = "TM549"; // NON-NLS
    @Override
    public String getFramePrefix() {
        return FRAME_PREFIX; //NON-NLS
    }
    @Override
    public List<String> getFramePrefixes() {
        List<String> prefixes = new ArrayList<>();
        prefixes.add(FRAME_PREFIX);
        return prefixes;
    }

    @Override
    public boolean framePrefixValid(String frameSerialNumber) {
        return frameSerialNumber.contains(FRAME_PREFIX);
    }
}
