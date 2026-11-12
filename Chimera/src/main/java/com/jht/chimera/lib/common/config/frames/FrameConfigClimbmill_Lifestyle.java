package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigClimbmill_Lifestyle extends FrameConfig {
    public FrameConfigClimbmill_Lifestyle() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Climbmill;
        machineType = JHTCommonTypes.MachineType.Climbmill;
        FRAME_PREFIX = "CS36"; //NON-NLS
        FRAME_ID = 107;
        supportLcbTypeList = new int[]{0x17, 0x19};
        MODEL = MODEL_LIFESTYLE;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(30));
        resistance.defaultValue = 1;
        resistance.precision = 0;
        resistance.stepSize = 1;

        weightKg.setMin(weightKg.setMinModel(WeightConvertUtil.convertLbsToKg(99)));
        weightKg.setMax(weightKg.setMaxModel(WeightConvertUtil.convertLbsToKg(300)));
        weightKg.defaultValue = WeightConvertUtil.convertLbsToKg(150);
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightLbs.setMinModel(99));
        weightLbs.setMax(weightLbs.setMaxModel(300));
        weightLbs.defaultValue = 150;
        weightLbs.precision = 0;
        weightLbs.stepSize = 1;

        //control zone
        HAS_CM_CONTROL_ZONE = false;

        // resistance calibration table
        RESISTANCE_TABLE = CalibrationTable.Lifestyle_Climbmill.RESISTANCE_TABLE;

        // rate
        RATE_RESISTANCE_PER_SEC = 5f;

        STEPS_PER_FLOOR = 22;
        STEPS_PER_METER = 6.45f;

        isLifestyle = true;

        KeyMap = FrameKeyMaps.Commercial;
    }
}
