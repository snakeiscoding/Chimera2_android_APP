package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

public class FrameConfigTcvIndoorCycle_Fx extends FrameConfig {
    public FrameConfigTcvIndoorCycle_Fx() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.SpinBike;
        machineType = JHTCommonTypes.MachineType.ManualResistanceSpinBike;
        FRAME_PREFIX = "FC65"; //NON-NLS
        FRAME_ID = 171;
        supportLcbTypeList = new int[]{0x22};
        MODEL = R.string.tcv_indoor_cycle_fx_tc;

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

        // rate
        RATE_RESISTANCE_PER_SEC = 5f;

        workoutDistanceKm.setMax(workoutDistanceKm.setMaxModel(100));
        workoutDistanceMi.setMax(workoutDistanceMi.setMaxModel(62.2f));

        KeyMap = FrameKeyMaps.Commercial;
        LCB_CAN_CONTROL_WATTS = false;
    }

}
