package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.units.UnitConverter;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

// TCV Indoor Cycle is basically another form of spin bike, so copied athena spin bike as reference and altered as needed
abstract class FrameConfigTcvIndoorCycle extends FrameConfig {
    public static class FR extends FrameConfigTcvIndoorCycle {
        public FR() {
            super();
            FRAME_PREFIX = "FC64"; //NON-NLS
            FRAME_ID = 170;
            supportLcbTypeList = new int[]{0x1B};
            MODEL = R.string.tcv_indoor_cycle_fr_tc;
        }
    }
    public FrameConfigTcvIndoorCycle() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.SpinBike;
        machineType = JHTCommonTypes.MachineType.CardioSpinBike;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

        speedMph.setMin(speedMph.setMinModel(0));
        speedMph.setMax(speedMph.setMaxModel((float)SpeedConvertUtil.bikeWattsToMPH(500)));
        speedMph.defaultValue = 15;
        speedMph.precision = 1;
        speedMph.stepSize = .1f;

        speedKph.setMin(speedKph.setMinModel(.8f));
        speedKph.setMax(speedKph.setMaxModel((float)UnitConverter.ConvertMphToKph(speedMph.getMaxModel())));
        speedKph.defaultValue = 20;
        speedKph.precision = 1;
        speedKph.stepSize = .1f;

        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(25));
        resistance.defaultValue = 1;
        resistance.precision = 0;
        resistance.stepSize = 1;

        weightKg.setMin(weightKg.setMinModel(22));
        weightKg.setMax(weightKg.setMaxModel(159));
        weightKg.defaultValue = WeightConvertUtil.convertLbsToKg(150);
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightLbs.setMinModel(50));
        weightLbs.setMax(weightLbs.setMaxModel(350));
        weightLbs.defaultValue = 150;
        weightLbs.precision = 0;
        weightLbs.stepSize = 1;

        // resistance calibration table
        RESISTANCE_TABLE = CalibrationTable.CardioSpinBike.RESISTANCE_TABLE;

        // watts table
        WATTS_TABLE = CalibrationTable.ChopperBike.WATTS_TABLE;

        // rate
        RATE_RESISTANCE_PER_SEC = 5f;

        workoutDistanceKm.setMax(workoutDistanceKm.setMaxModel(100));
        workoutDistanceMi.setMax(workoutDistanceMi.setMaxModel(62.2f));

        KeyMap = FrameKeyMaps.Commercial;
        LCB_CAN_CONTROL_WATTS = true;
    }
}
