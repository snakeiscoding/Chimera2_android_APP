package com.jht.chimera.lib.common.config.frames;

import android.util.Log;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigTreadmill_Performance_2027 extends FrameConfig {
    public FrameConfigTreadmill_Performance_2027() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Treadmill;
        machineType = JHTCommonTypes.MachineType.Treadmill;
        FRAME_PREFIX = "TM560"; //NON-NLS
        FRAME_ID = 156;
        supportLcbTypeList = new int[]{0xC9};
        MODEL = MODEL_PERFORMANCE;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

        inclineLimit.setMin(inclineLimit.setMinModel(0));
        inclineLimit.setMax(inclineLimit.setMaxModel(25));
        inclineLimit.defaultValue = 0;
        inclineLimit.precision = 1;
        inclineLimit.stepSize = .5f;
        incline.copy(inclineLimit);
        incline.setMax(25);

        speedKph.setMin(speedKph.setMinModel(.8f));
        speedKph.setMaxModel(30);
        speedKph.setMax(30);
        speedKph.defaultValue = speedKph.getMin();
        speedKph.precision = 1;
        speedKph.stepSize = .1f;

        speedMph.setMin(speedMph.setMinModel(.5f));
        speedMph.setMaxModel(18.6f);
        speedMph.setMax(18.6f);
        speedMph.defaultValue = speedMph.getMin();
        speedMph.precision = 1;
        speedMph.stepSize = .1f;

        weightKg.setMin(weightKg.setMinModel(WeightConvertUtil.convertLbsToKg(50)));
        weightKg.setMax(weightKg.setMaxModel(WeightConvertUtil.convertLbsToKg(400)));
        weightKg.defaultValue = WeightConvertUtil.convertLbsToKg(150);
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightLbs.setMinModel(50));
        weightLbs.setMax(weightLbs.setMaxModel(400));
        weightLbs.defaultValue = 150;
        weightLbs.precision = 0;
        weightLbs.stepSize = 1;

        // incline ranges
        INCLINE_ADC_OFFSET_MIN = 512;
        INCLINE_ADC_OFFSET_MAX = 2752;
        INCLINE_ADC_MIN = 2784 + INCLINE_ADC_OFFSET_MIN; // 3296 = 2784 + INCLINE_ADC_OFFSET_MIN
        INCLINE_ADC_MAX = 26064 - INCLINE_ADC_OFFSET_MAX; // 23312 = 26064 - INCLINE_ADC_OFFSET_MAX
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;
        USES_REVERSE_INCLINE = true;

        // speed rpm kph ratio
        speedRPM.updateRange(125, 4464);

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 0.47f;
        RATE_KPH_PER_SEC = SpeedConvertUtil.convertMphToKph(0.5f);

        // safety
        HAS_TM_LOCK_STATE = false;

        HAS_CADENCE = true;

        //Uses the "Dumb Plastics" CAN module in the grips.
        HAS_CAN = true;
        USE_CAN_HEART_RATE = true;

        KeyMap = FrameKeyMaps.Commercial_Treadmill;
    }

}
