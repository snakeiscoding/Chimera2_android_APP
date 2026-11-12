package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigElliptical_Performance extends FrameConfig {
    public FrameConfigElliptical_Performance() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Elliptical;
        machineType = JHTCommonTypes.MachineType.Elliptical;
        FRAME_PREFIX = "EP629"; //NON-NLS
        FRAME_ID = 98;
        supportLcbTypeList = new int[]{0x07};
        MODEL = MODEL_PERFORMANCE;
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

        // resistance calibration table
        RESISTANCE_TABLE = CalibrationTable.Elliptical.RESISTANCE_TABLE;

        // watts table
        WATTS_TABLE = CalibrationTable.Elliptical.WATTS_TABLE;

        // rate
        RATE_RESISTANCE_PER_SEC = 5f;

        KeyMap = FrameKeyMaps.Commercial;
        LCB_CAN_CONTROL_WATTS = true;
    }

}
