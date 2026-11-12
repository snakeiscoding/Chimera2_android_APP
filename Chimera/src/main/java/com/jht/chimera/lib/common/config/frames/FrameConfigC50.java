package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigC50 extends FrameConfig {
    public FrameConfigC50() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Climbmill;
        machineType = JHTCommonTypes.MachineType.Climbmill;
        FRAME_PREFIX = "CS31"; //NON-NLS
        FRAME_ID = 1;
        supportLcbTypeList = new int[]{0x19};
        MODEL = R.string.frame_c50;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Parker);
        powerRule = FramePowerRules.Parker.RULE_NO22_USB;

        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(30));
        resistance.defaultValue = 1;
        resistance.precision = 0;
        resistance.stepSize = 1;

        weightKg.setMin(weightKg.setMinModel(WeightConvertUtil.convertLbsToKg(50)));
        weightKg.setMax(weightKg.setMaxModel(WeightConvertUtil.convertLbsToKg(300)));
        weightKg.defaultValue = WeightConvertUtil.convertLbsToKg(150);
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightLbs.setMinModel(50));
        weightLbs.setMax(weightLbs.setMaxModel(300));
        weightLbs.defaultValue = 150;
        weightLbs.precision = 0;
        weightLbs.stepSize = 1;

        //control zone
        HAS_CM_CONTROL_ZONE = true; // PRKS-1433: yes, frame does

        // resistance calibration table
        RESISTANCE_CAL_TABLE = new int[]{24, 29, 33, 38, 43, 48, 52, 57, 62, 66, 71, 76, 80, 85, 90, 95, 99, 104, 109, 113, 118, 123, 127, 132, 137, 142, 146, 151, 156, 160};
        //RESISTANCE_TABLE = CalibrationTable.Climbmill.RESISTANCE_TABLE;
        RESISTANCE_TABLE = new int[][]{{24}, {29}, {33}, {38}, {43}, {48}, {52}, {57}, {62}, {66}, {71}, {76}, {80}, {85}, {90}, {95}, {99}, {104}, {109}, {113}, {118}, {123}, {127}, {132}, {137}, {142}, {146}, {151}, {156}, {160}};

        // rate
        RATE_RESISTANCE_PER_SEC = 5f;   // Does this apply?

        STEPS_PER_FLOOR = 22;
        STEPS_PER_METER = 7.16332f; // 139.6 mm step height

        REQUIRES_CHAIN_LUBRICATION = true;

        KeyMap = FrameKeyMaps.ResistancePauseStop;

        isLifestyle = true; // not really, but close enough
    }

}
