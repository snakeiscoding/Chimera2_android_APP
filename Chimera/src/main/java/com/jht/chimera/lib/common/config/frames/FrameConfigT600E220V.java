package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigT600E220V extends FrameConfig {
    public FrameConfigT600E220V() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Treadmill;
        machineType = JHTCommonTypes.MachineType.Treadmill;
        if (ProductConfig.product.equals(ProductConfig.Product.Vision600E)) {
            FRAME_PREFIX = "TM743";
            FRAME_ID = 0;
        } else if (ProductConfig.product.equals(ProductConfig.Product.T600XE)) {
            FRAME_PREFIX = "TM772";
            FRAME_ID = 113;
        }
        supportLcbTypeList = new int[]{0xC3,0xA2};
        MODEL = R.string.frame_t600e220v;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Vision600E, ProductConfig.BuildFlavor.T600XE);

        incline.setMin(incline.setMinModel(0));
        incline.setMax(incline.setMaxModel(15));
        incline.defaultValue = 0;
        incline.precision = 1;
        incline.stepSize = .5f;
        inclineLimit.copy(incline);

        speedKph.setMin(speedKph.setMinModel(.8f));
        speedKph.setMax(speedKph.setMaxModel(22f));
        speedKph.defaultValue = speedKph.getMin();
        speedKph.precision = 1;
        speedKph.stepSize = .1f;

        speedMph.setMin(speedMph.setMinModel(.5f));
        speedMph.setMax(speedMph.setMaxModel(13.8f));
        speedMph.defaultValue = speedMph.getMin();
        speedMph.precision = 1;
        speedMph.stepSize = .1f;

        weightKg.setMin(weightKg.setMinModel(WeightConvertUtil.convertLbsToKg(75)));
        weightKg.setMax(weightKg.setMaxModel(WeightConvertUtil.convertLbsToKg(400)));
        weightKg.defaultValue = WeightConvertUtil.convertLbsToKg(150);
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightLbs.setMinModel(75));
        weightLbs.setMax(weightLbs.setMaxModel(400));
        weightLbs.defaultValue = 150;
        weightLbs.precision = 0;
        weightLbs.stepSize = 1;

        // incline ranges
        INCLINE_ADC_OFFSET_MIN = 1792;
        INCLINE_ADC_OFFSET_MAX = 1024;
        INCLINE_ADC_MIN = (1280 + INCLINE_ADC_OFFSET_MIN); // 3072 = 1280 + INCLINE_ADC_OFFSET_MIN
        INCLINE_ADC_MAX = (24320 - INCLINE_ADC_OFFSET_MAX); // 23296 = 24320 - INCLINE_ADC_OFFSET_MAX
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;
        USES_REVERSE_INCLINE = true;

        // speed rpm kph ratio
        speedRPM.updateRange(147, 4085);

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 0.71f;
        RATE_KPH_PER_SEC = SpeedConvertUtil.convertMphToKph(0.5f);

        // safety
        HAS_TM_ESTOP = true;

	//DC bus current OC
        HAS_CURRENT_OC = false;

        KeyMap = FrameKeyMaps.VisionSpeed;
    }

}
