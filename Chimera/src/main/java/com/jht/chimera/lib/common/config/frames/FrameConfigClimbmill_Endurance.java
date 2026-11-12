package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigClimbmill_Endurance extends FrameConfig {
    public FrameConfigClimbmill_Endurance() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Climbmill;
        machineType = JHTCommonTypes.MachineType.Climbmill;
        FRAME_PREFIX = "CS34"; //NON-NLS
        FRAME_ID = 99;
        supportLcbTypeList = new int[]{0x17};
        MODEL = MODEL_ENDURANCE;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

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

        //control zone
        HAS_CM_CONTROL_ZONE = false;

        // resistance calibration table
        RESISTANCE_TABLE = CalibrationTable.EN_Climbmill.RESISTANCE_TABLE;

        // rate
        RATE_RESISTANCE_PER_SEC = 5f;

        STEPS_PER_FLOOR = 16;
        STEPS_PER_METER = 4.92f;

        KeyMap = FrameKeyMaps.Commercial;
    }
}
