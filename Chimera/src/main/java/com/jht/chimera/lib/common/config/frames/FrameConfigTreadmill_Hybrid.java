package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigTreadmill_Hybrid extends FrameConfig {
    public FrameConfigTreadmill_Hybrid() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Treadmill;
        machineType = JHTCommonTypes.MachineType.Treadmill;
        FRAME_PREFIX = "TM725"; //NON-NLS
        FRAME_ID = 161;
        supportLcbTypeList = new int[]{0xCA};
        MODEL = R.string.hybrid_treadmill_tc;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

        inclineLimit.setMin(inclineLimit.setMinModel(0));
        inclineLimit.setMax(inclineLimit.setMaxModel(15));
        inclineLimit.defaultValue = 0;
        inclineLimit.precision = 1;
        inclineLimit.stepSize = .5f;
        incline.copy(inclineLimit);
        incline.setMax(15);

        speedKph.setMin(speedKph.setMinModel(.2f));
        speedKph.setMaxModel(20);
        speedKph.setMax(20);
        speedKph.defaultValue = speedKph.getMin();
        speedKph.precision = 1;
        speedKph.stepSize = .1f;

        speedMph.setMin(speedMph.setMinModel(.1f));
        speedMph.setMaxModel(12f);
        speedMph.setMax(12f);
        speedMph.defaultValue = speedMph.getMin();
        speedMph.precision = 1;
        speedMph.stepSize = .1f;
        speedReverseKph.setMin(speedReverseKph.setMinModel(.2f));
        speedReverseKph.setMax(speedReverseKph.setMaxModel(6.4f));
        speedReverseMph.setMin(speedReverseMph.setMinModel(.1f));
        speedReverseMph.setMax(speedReverseMph.setMaxModel(4f));

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

        // incline ranges (TODO copied from Lifestyle TM)
        INCLINE_ADC_OFFSET_MIN = 1664;
        INCLINE_ADC_OFFSET_MAX = 640;
        INCLINE_ADC_MIN = (1536 + INCLINE_ADC_OFFSET_MIN);
        INCLINE_ADC_MAX = (27392 - INCLINE_ADC_OFFSET_MAX);

        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;
        USES_REVERSE_INCLINE = true;

        TM_SUPPORTS_REVERSE_BELT = true;

        // speed rpm kph ratio
        speedRPM.updateRange(28, 3440);

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 0.47f;
        RATE_KPH_PER_SEC = SpeedConvertUtil.convertMphToKph(0.5f);

        // safety
        HAS_TM_LOCK_STATE = false;
		// TM IR Board
        HAS_IR_SENSOR = true;

        KeyMap = FrameKeyMaps.Commercial_Treadmill;
    }

}
