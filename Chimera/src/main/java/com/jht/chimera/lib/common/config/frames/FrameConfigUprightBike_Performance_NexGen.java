package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.units.UnitConverter;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigUprightBike_Performance_NexGen extends FrameConfig {
    public FrameConfigUprightBike_Performance_NexGen() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Bike;
        machineType = JHTCommonTypes.MachineType.UprightBike;
        FRAME_PREFIX = "CB211"; //NON-NLS
        FRAME_ID = 159;
        supportLcbTypeList = new int[]{0x14, 0x21};
        MODEL = MODEL_PERFORMANCE_NEXGEN;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

        speedMph.setMin(speedMph.setMinModel(0));
        speedMph.setMax(speedMph.setMaxModel((float)SpeedConvertUtil.bikeWattsToMPH(520)));
        speedMph.defaultValue = 15;
        speedMph.precision = 1;
        speedMph.stepSize = .1f;

        speedKph.setMin(speedKph.setMinModel(.8f));
        speedKph.setMax(speedKph.setMaxModel((float)UnitConverter.ConvertMphToKph(speedMph.getMaxModel())));
        speedKph.defaultValue = 20;
        speedKph.precision = 1;
        speedKph.stepSize = .1f;

        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(30));
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
        RESISTANCE_TABLE = CalibrationTable.JIS1xBike.RESISTANCE_TABLE;

        // watts table
        WATTS_TABLE = CalibrationTable.JIS1xBike.WATTS_TABLE;

        // rate
        RATE_RESISTANCE_PER_SEC = 5f;

        //Uses the "Dumb Plastics" CAN module in the grips.
        HAS_CAN = true;

        workoutDistanceKm.setMax(workoutDistanceKm.setMaxModel(100));
        workoutDistanceMi.setMax(workoutDistanceMi.setMaxModel(62.2f));

        KeyMap = FrameKeyMaps.Commercial;
        LCB_CAN_CONTROL_WATTS = true;
    }
}
