package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigTF50_02 extends FrameConfig {
    // Using extended inner class for JIV factory SNs, as they seem to be identical for now, can split off into own class if needed
    public static class JIV extends FrameConfigTF50_02 {
        public JIV() {
            super();
            FRAME_PREFIX = "TM1016"; //NON-NLS
        }
    }
    public FrameConfigTF50_02() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Treadmill;
        machineType = JHTCommonTypes.MachineType.Treadmill;
        FRAME_PREFIX = "TM694B"; //NON-NLS
        FRAME_ID = 2; // "Since the frame prefix is a derivative (prefix+”B”) the frame id will be the same as for the main entry"
        supportLcbTypeList = new int[]{0x06, 0x0E, 0xD0, 0xD1};
        MODEL = R.string.frame_tf50_02;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Parker);
        powerRule = FramePowerRules.Parker.RULE_NO22_USB_VOLUME;

        incline.setMin(incline.setMinModel(0));
        incline.setMax(incline.setMaxModel(15));
        incline.defaultValue = 0;
        incline.precision = 1;
        incline.stepSize = .5f;
        inclineLimit.copy(incline);

        speedKph.setMin(speedKph.setMinModel(.8f));
        speedKph.setMax(speedKph.setMaxModel(20f));
        speedKph.defaultValue = speedKph.getMin();
        speedKph.precision = 1;
        speedKph.stepSize = .1f;

        speedMph.setMin(speedMph.setMinModel(.5f));
        speedMph.setMax(speedMph.setMaxModel(12.5f));
        speedMph.defaultValue = speedMph.getMin();
        speedMph.precision = 1;
        speedMph.stepSize = .1f;

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

        // incline ranges
        INCLINE_ADC_MIN = 34;
        INCLINE_ADC_MAX = 1130;
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;

        // speed rpm kph ratio
        speedRPM.updateRange(169, 4210);

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 0.71f;
        RATE_KPH_PER_SEC = SpeedConvertUtil.convertMphToKph(0.5f);

        // safety
        HAS_TM_ESTOP = true;

        REQUIRES_BELT_LUBRICATION = true;

        KeyMap = FrameKeyMaps.Treadmill_6key;
		
        HAS_TM_AUTO_RAMP_UP_AND_LAST_RPM_VALUE = true;
        RPM_RAMP_UP_STAGE_VALUE = 250; // Speed Up ramp stage value
        RPM_RAMP_DOWN_STAGE_VALUE = 180; // Speed Down ramp stage value

        // Enable Over Speed
        HAS_TM_OVER_SPEED = true;

    }

}
