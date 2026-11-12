package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigE50_02 extends FrameConfig {
    // Using extended inner class for JIV factory SNs, as they seem to be identical for now, can split off into own class if needed
    public static class JIV extends FrameConfigE50_02 {
        public JIV() {
            super();
            FRAME_PREFIX = "EP300"; //NON-NLS
        }
    }
    public FrameConfigE50_02() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Elliptical;
        machineType = JHTCommonTypes.MachineType.Elliptical;
        FRAME_PREFIX = "EP728"; //NON-NLS
        FRAME_ID = 13;
        supportLcbTypeList = new int[]{0x0C, 0xB7, 0xB9};
        MODEL = R.string.frame_e50_02;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Parker);
        powerRule = FramePowerRules.Parker.RULE_FULL_POWER;

        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(30));
        resistance.defaultValue = 1;
        resistance.precision = 0;
        resistance.stepSize = 1;

        weightKg.setMin(weightKg.setMinModel(WeightConvertUtil.convertLbsToKg(50)));
        weightKg.setMax(weightKg.setMaxModel(WeightConvertUtil.convertLbsToKg(325)));
        weightKg.defaultValue = WeightConvertUtil.convertLbsToKg(150);
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightLbs.setMinModel(50));
        weightLbs.setMax(weightLbs.setMaxModel(325));
        weightLbs.defaultValue = 150;
        weightLbs.precision = 0;
        weightLbs.stepSize = 1;

        // resistance calibration table
        RESISTANCE_CAL_TABLE = CalibrationTable.A50_E50.RESISTANCE_CAL_TABLE;
        //RESISTANCE_TABLE = CalibrationTable.Elliptical.RESISTANCE_TABLE;
        RESISTANCE_TABLE = CalibrationTable.A50_E50.RESISTANCE_TABLE;

        // watts table
        WATTS_TABLE = CalibrationTable.A50_E50.WATTS_TABLE;
        TABLE_RPM_START = CalibrationTable.A50_E50.TABLE_RPM_START;

        // rate
        RATE_RESISTANCE_PER_SEC = 6f;

        KeyMap = FrameKeyMaps.ResistanceOnly;
    }

}
