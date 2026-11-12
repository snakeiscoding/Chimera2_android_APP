package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigTreadmill_Performance_Plus extends FrameConfig {

    public FrameConfigTreadmill_Performance_Plus() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Treadmill;
        machineType = JHTCommonTypes.MachineType.Treadmill;
        FRAME_PREFIX = "TM543"; //NON-NLS
        FRAME_ID = 93;
        supportLcbTypeList = new int[]{0xC6};
        MODEL = MODEL_PERFORMANCE_PLUS;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

        inclineLimit.setMin(inclineLimit.setMinModel(0));
        inclineLimit.setMax(inclineLimit.setMaxModel(20));
        inclineLimit.defaultValue = 0;
        inclineLimit.precision = 1;
        inclineLimit.stepSize = .5f;
        incline.copy(inclineLimit);
        incline.setMax(15);

        speedKph.setMin(speedKph.setMinModel(.8f));
        speedKph.setMaxModel(26f);
        speedKph.setMax(20f);
        speedKph.defaultValue = speedKph.getMin();
        speedKph.precision = 1;
        speedKph.stepSize = .1f;

        speedMph.setMin(speedMph.setMinModel(.5f));
        speedMph.setMaxModel(16f);
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
        INCLINE_ADC_OFFSET_MIN = 0;
        INCLINE_ADC_OFFSET_MAX = 512;
        INCLINE_ADC_MIN = 2560 + INCLINE_ADC_OFFSET_MIN;
        INCLINE_ADC_MAX = 24762 - INCLINE_ADC_OFFSET_MAX; // 24250 = 24762 - INCLINE_ADC_OFFSET_MAX
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;
        USES_REVERSE_INCLINE = false;

        // speed rpm kph ratio
        speedRPM.updateRange(96, 3132);

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 0.66f;
        RATE_KPH_PER_SEC = SpeedConvertUtil.convertMphToKph(0.5f);

        // safety
        HAS_TM_ESTOP = true;
        HAS_TM_LOCK_STATE = true;

        KeyMap = FrameKeyMaps.Commercial_Treadmill;
    }
}
