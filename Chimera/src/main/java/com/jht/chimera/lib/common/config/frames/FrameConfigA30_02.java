package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigA30_02 extends FrameConfig {
    public FrameConfigA30_02() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Ascent;
        machineType = JHTCommonTypes.MachineType.Ascent;
        // EP701 / EP701B
        FRAME_PREFIX = "EP701"; //NON-NLS
        FRAME_ID = 1;
        supportLcbTypeList = new int[]{0x0B, 0xB6};
        MODEL = R.string.frame_a30_02;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Parker);
        powerRule = FramePowerRules.Parker.RULE_NO22_USB_VOLUME;

        incline.setMin(incline.setMinModel(0));
        incline.setMax(incline.setMaxModel(100));
        incline.defaultValue = 0;
        incline.precision = 0;
        incline.stepSize = 5;
        inclineLimit.copy(incline);

        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(20));
        resistance.defaultValue = 1;
        resistance.precision = 0;
        resistance.stepSize = 1;

        weightKg.setMin(weightKg.setMinModel(WeightConvertUtil.convertLbsToKg(50)));
        weightKg.setMax(weightKg.setMaxModel(WeightConvertUtil.convertLbsToKg(325)));
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightLbs.setMinModel(50));
        weightLbs.setMax(weightLbs.setMaxModel(325));
        weightLbs.precision = 0;
        weightLbs.stepSize = 1;

        // incline ranges
        INCLINE_ADC_MIN = 34;
        INCLINE_ADC_MAX = 1575;
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;

        // resistance calibration table
        RESISTANCE_CAL_TABLE = CalibrationTable.A30.RESISTANCE_CAL_TABLE;
        //RESISTANCE_TABLE = CalibrationTable.Ascent.RESISTANCE_TABLE;
        RESISTANCE_TABLE = CalibrationTable.A30.RESISTANCE_TABLE;

        // watts table
        WATTS_TABLE = CalibrationTable.A30.WATTS_TABLE;
        TABLE_RPM_START = CalibrationTable.A30.TABLE_RPM_START;

        // Watts inaccurate on 30 series
        WATTS_INACCURATE = true;

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 3.7f;
        RATE_RESISTANCE_PER_SEC = 1f;

        KeyMap = FrameKeyMaps.ResistanceAndIncline;
    }

}
