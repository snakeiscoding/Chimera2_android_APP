package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigU50 extends FrameConfig {
    public FrameConfigU50() {
        super();

        //machineType = MachineType.BIKE;
        machineClassType = JHTCommonTypes.MachineClassificationType.Bike;
        machineType = JHTCommonTypes.MachineType.UprightBike;
        FRAME_PREFIX = "CB205"; //NON-NLS
        FRAME_ID = 2;
        supportLcbTypeList = new int[]{0x0C, 0xB7, 0xB9};
        MODEL = R.string.frame_u50;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Parker);
        powerRule = FramePowerRules.Parker.RULE_NO22_USB;

        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(30));
        resistance.defaultValue = 1;
        resistance.precision = 0;
        resistance.stepSize = 1;

        weightKg.setMin(weightKg.setMinModel(WeightConvertUtil.convertLbsToKg(50)));
        weightKg.setMax(weightKg.setMaxModel(WeightConvertUtil.convertLbsToKg(350)));
        weightKg.defaultValue = WeightConvertUtil.convertLbsToKg(150);
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightLbs.setMinModel(50));
        weightLbs.setMax(weightLbs.setMaxModel(350));
        weightLbs.defaultValue = 150;
        weightLbs.precision = 0;
        weightLbs.stepSize = 1;
        
        // resistance calibration table
        RESISTANCE_CAL_TABLE = CalibrationTable.R50_U50.RESISTANCE_CAL_TABLE;
        //RESISTANCE_TABLE = CalibrationTable.Bike.RESISTANCE_TABLE;
        RESISTANCE_TABLE = CalibrationTable.R50_U50.RESISTANCE_TABLE;

        // watts table
        WATTS_TABLE = CalibrationTable.R50_U50.WATTS_TABLE;
        TABLE_RPM_START = CalibrationTable.R50_U50.TABLE_RPM_START;

        // rate
        RATE_RESISTANCE_PER_SEC = 6f;

        KeyMap = FrameKeyMaps.ResistanceOnly;
    }

}
