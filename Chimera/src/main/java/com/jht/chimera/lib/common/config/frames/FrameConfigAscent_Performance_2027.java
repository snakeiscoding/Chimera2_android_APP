package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigAscent_Performance_2027 extends FrameConfig {

    public FrameConfigAscent_Performance_2027() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Ascent;
        machineType = JHTCommonTypes.MachineType.Ascent;
        FRAME_PREFIX = "EP645"; //NON-NLS
        FRAME_ID = 158;
        supportLcbTypeList = new int[]{0x1F};
        MODEL = MODEL_PERFORMANCE_2027;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

        incline.setMin(incline.setMinModel(0));
        incline.setMax(incline.setMaxModel(100));
        incline.defaultValue = 0;
        incline.precision = 0;
        incline.stepSize = 5;
        inclineLimit.copy(incline);

        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(25));
        resistance.defaultValue = 1;
        resistance.precision = 0;
        resistance.stepSize = 1;

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
        INCLINE_ADC_MIN = 0;
        INCLINE_ADC_MAX = 32768;
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;

        // resistance calibration table
        RESISTANCE_TABLE = CalibrationTable.Ascent.RESISTANCE_TABLE;

        // watts table
        WATTS_TABLE = CalibrationTable.Ascent.WATTS_TABLE;

        //Uses the "Dumb Plastics" CAN module in the grips.
        HAS_CAN = true;

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 3.7f;
        RATE_RESISTANCE_PER_SEC = 5f;

        KeyMap = FrameKeyMaps.Commercial;
        LCB_CAN_CONTROL_WATTS = true;
    }

}
