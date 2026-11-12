package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class FrameConfigTreadmill_Performance extends FrameConfig {
    public FrameConfigTreadmill_Performance() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Treadmill;
        machineType = JHTCommonTypes.MachineType.Treadmill;
        FRAME_PREFIX = "TM538"; //NON-NLS
        FRAME_ID = 92;
        supportLcbTypeList = new int[]{0xC3,0xC4,0xC5,0xC7,0xC8,0xA1};
        MODEL = MODEL_PERFORMANCE;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

        inclineLimit.setMin(inclineLimit.setMinModel(0));
        inclineLimit.setMax(inclineLimit.setMaxModel(18));
        inclineLimit.defaultValue = 0;
        inclineLimit.precision = 1;
        inclineLimit.stepSize = .5f;
        incline.copy(inclineLimit);
        incline.setMax(15);

        speedKph.setMin(speedKph.setMinModel(.8f));
        speedKph.setMaxModel(24);
        speedKph.setMax(20);
        speedKph.defaultValue = speedKph.getMin();
        speedKph.precision = 1;
        speedKph.stepSize = .1f;

        speedMph.setMin(speedMph.setMinModel(.5f));
        speedMph.setMaxModel(15f);
        speedMph.setMax(12f);
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
        INCLINE_ADC_OFFSET_MIN = 1408;
        INCLINE_ADC_OFFSET_MAX = 128;
        INCLINE_ADC_MIN = 2092 + INCLINE_ADC_OFFSET_MIN;
        INCLINE_ADC_MAX = 23628 - INCLINE_ADC_OFFSET_MAX;
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;
        USES_REVERSE_INCLINE = true;

        // speed rpm kph ratio
        speedRPM.updateRange(178, 5290);

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 0.47f;
        RATE_KPH_PER_SEC = SpeedConvertUtil.convertMphToKph(0.5f);

        // safety
        HAS_TM_ESTOP = false;
        HAS_TM_LOCK_STATE = false;

        RESISTANCE_TABLE = null;

        KeyMap = FrameKeyMaps.Commercial_Treadmill;
    }

    private static final String IE2_1_FRAME_PREFIX = "TM538"; // NON-NLS
    private static final String IE3_FRAME_PREFIX = "TM549"; // NON-NLS
    @Override
    public String getFramePrefix() {
        return (DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT) == 0xA1 ? IE3_FRAME_PREFIX : IE2_1_FRAME_PREFIX); //NON-NLS
    }
    @Override
    public List<String> getFramePrefixes() {
        List<String> prefixes = new ArrayList<>();
        prefixes.add(IE2_1_FRAME_PREFIX);
        prefixes.add(IE3_FRAME_PREFIX);
        return prefixes;
    }

    @Override
    public boolean framePrefixValid(String frameSerialNumber) {
        return frameSerialNumber.contains(IE2_1_FRAME_PREFIX) || frameSerialNumber.contains(IE3_FRAME_PREFIX); // NON-NLS
    }

    @Override
    public int getModelId() {
        String frameSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
        return (frameSerial != null && frameSerial.contains(IE3_FRAME_PREFIX)) ? 116 : 92;
    }
}
