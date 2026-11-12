package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

// E30 with different plastic
public class FrameConfigE30_02 extends FrameConfig {
    // Using extended inner class for JIV factory SNs, as they seem to be identical for now, can split off into own class if needed
    public static class JIV extends FrameConfigE30_02 {
        public JIV() {
            super();
            FRAME_PREFIX = "EP299"; //NON-NLS
        }
    }
    public FrameConfigE30_02() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Elliptical;
        machineType = JHTCommonTypes.MachineType.Elliptical;
        FRAME_PREFIX = "EP730"; //NON-NLS
        FRAME_ID = 20;
        supportLcbTypeList = new int[]{0x12}; // What is 0x13 ?
        MODEL = R.string.frame_e30_02;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Parker);
        powerRule = FramePowerRules.Parker.RULE_NO22_USB;


        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(20));
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
        RESISTANCE_CAL_TABLE = CalibrationTable.E30.RESISTANCE_CAL_TABLE;
        //RESISTANCE_TABLE = CalibrationTable.Elliptical.RESISTANCE_TABLE;
        RESISTANCE_TABLE = CalibrationTable.E30.RESISTANCE_TABLE;

        // watts table
        WATTS_TABLE = CalibrationTable.E30.WATTS_TABLE;
        TABLE_RPM_START = CalibrationTable.E30.TABLE_RPM_START;

        // Watts inaccurate on 30 series
        WATTS_INACCURATE = true;

        // rate
        RATE_RESISTANCE_PER_SEC = 1f;

        KeyMap = FrameKeyMaps.ResistanceOnly;
    }

}
