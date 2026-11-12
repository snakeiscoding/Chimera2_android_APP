package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.workout.WorkoutIds;

import java.util.EnumSet;

// Initially copied from FrameConfigCardioSpinBike.java since frame is largely athena based
public class FrameConfigICR70 extends FrameConfig {
    public FrameConfigICR70() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.SpinBike;
        machineType = JHTCommonTypes.MachineType.CardioSpinBike;
        FRAME_PREFIX = "FC211"; //NON-NLS
        FRAME_ID = 25;
        supportLcbTypeList = new int[]{0x1D};
        MODEL = R.string.frame_icr70;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Parker); // ICR70 runs parker on Ultra Thin console

        /* not sure why commercial bikes are defining speed ranges for a bike?
        speedMph.setMin(speedMph.setMinModel(0));
        speedMph.setMax(speedMph.setMaxModel((float)SpeedConvertUtil.bikeWattsToMPH(500)));
        speedMph.defaultValue = 15;
        speedMph.precision = 1;
        speedMph.stepSize = .1f;

        speedKph.setMin(speedKph.setMinModel(.8f));
        speedKph.setMax(speedKph.setMaxModel((float)UnitConverter.ConvertMphToKph(speedMph.getMaxModel())));
        speedKph.defaultValue = 20;
        speedKph.precision = 1;
        speedKph.stepSize = .1f;*/

        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(25)); // ICR70 has been given 25 levels to match athena
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
        RESISTANCE_TABLE = CalibrationTable.CardioSpinBike.RESISTANCE_TABLE; // athena

        // watts table
        WATTS_TABLE = CalibrationTable.ChopperBike.WATTS_TABLE;
        //WATTS_INACCURATE = true; // ICR70 cannot do constant watts workout BUT it should still show watts !
        blockedWorkoutIds = new int[]{WorkoutIds.CONSTANT_WATTS};

        // rate
        RATE_RESISTANCE_PER_SEC = 5f;

        workoutDistanceKm.setMax(workoutDistanceKm.setMaxModel(100));
        workoutDistanceMi.setMax(workoutDistanceMi.setMaxModel(62.2f));

        //KeyMap = KeypadManager.KeyPreset_Comercial; // ICR70 - no frame buttons
    }
}
