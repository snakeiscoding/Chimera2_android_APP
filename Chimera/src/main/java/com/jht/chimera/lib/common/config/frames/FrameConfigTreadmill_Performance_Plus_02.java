package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.util.EnumSet;

public class FrameConfigTreadmill_Performance_Plus_02 extends FrameConfig {

    public FrameConfigTreadmill_Performance_Plus_02() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Treadmill;
        machineType = JHTCommonTypes.MachineType.Treadmill;
        FRAME_PREFIX = "TM548"; //NON-NLS
        FRAME_ID = 140;
        supportLcbTypeList = new int[]{0xF1};
        MODEL = MODEL_PERFORMANCE_PLUS_02;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

        inclineLimit.setMin(inclineLimit.setMinModel(0));
        inclineLimit.setMax(inclineLimit.setMaxModel(20));
        inclineLimit.defaultValue = 0;
        inclineLimit.precision = 1;
        inclineLimit.stepSize = .5f;
        incline.copy(inclineLimit);
        incline.setMax(15);

        speedKph.setMin(speedKph.setMinModel(.8f));
        speedKph.setMaxModel(26f);
        speedKph.setMax(20f);
        speedKph.defaultValue = speedKph.getMin();
        speedKph.precision = 1;
        speedKph.stepSize = .1f;

        speedMph.setMin(speedMph.setMinModel(.5f));
        speedMph.setMaxModel(16f);
        speedMph.setMax(12f);
        speedMph.defaultValue = speedMph.getMin();
        speedMph.precision = 1;
        speedMph.stepSize = .1f;

        weightKg.setMin(weightKg.setMinModel(WeightConvertUtil.convertLbsToKg(50)));
        weightKg.setMax(weightKg.setMaxModel(WeightConvertUtil.convertLbsToKg(500)));
        weightKg.defaultValue = WeightConvertUtil.convertLbsToKg(150);
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightLbs.setMinModel(50));
        weightLbs.setMax(weightLbs.setMaxModel(500));
        weightLbs.defaultValue = 150;
        weightLbs.precision = 0;
        weightLbs.stepSize = 1;

        // incline ranges
        INCLINE_ADC_OFFSET_MIN = 384;
        INCLINE_ADC_OFFSET_MAX = 512;
        INCLINE_ADC_MIN = (3584 + INCLINE_ADC_OFFSET_MIN); // 31x128 = 3968 = 3584 + INCLINE_ADC_OFFSET_MIN
        INCLINE_ADC_MAX = 24448 - INCLINE_ADC_OFFSET_MAX; // 23936 = 24448 - INCLINE_ADC_OFFSET_MAX
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;
        USES_REVERSE_INCLINE = false;

        // speed rpm kph ratio
        speedRPM.updateRange(108, 3400);

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 0.66f;
        RATE_KPH_PER_SEC = SpeedConvertUtil.convertMphToKph(0.5f);

        // safety
        HAS_TM_ESTOP = true;
        HAS_TM_LOCK_STATE = true;

        //DC bus current OC
        HAS_CURRENT_OC = true;
        KeyMap = FrameKeyMaps.Commercial_Treadmill;
    }

    @Override
    public int getCbusTriggerLimitUpper(){
        int voltage = DataMatrix.getInt(HALKeys.DATA_GET_MOTOR_VOLTAGE_INT);
        if (voltage == 110)
            DC_BUS_TRIGGER_LIMIT_UPPER = 72;
        else if(voltage == 220)
            DC_BUS_TRIGGER_LIMIT_UPPER = 97;
			
        return DC_BUS_TRIGGER_LIMIT_UPPER;
    }
	
    @Override
    public int getCbusTriggerLimitLower(){
        int voltage = DataMatrix.getInt(HALKeys.DATA_GET_MOTOR_VOLTAGE_INT);
        if (voltage == 110)
            DC_BUS_TRIGGER_LIMIT_LOWER = 63;
        else if(voltage == 220)
            DC_BUS_TRIGGER_LIMIT_LOWER = 86;
			
        return DC_BUS_TRIGGER_LIMIT_LOWER;
    }
	
}
