package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class FrameConfigTreadmill_Endurance extends FrameConfig {
    public FrameConfigTreadmill_Endurance() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Treadmill;
        machineType = JHTCommonTypes.MachineType.Treadmill;
        FRAME_PREFIX = "TM747"; //NON-NLS
        FRAME_ID = 91;
        supportLcbTypeList = new int[]{0xC7,0xC8,0xA1};
        MODEL = MODEL_ENDURANCE;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Chimera, ProductConfig.BuildFlavor.LCGUI);

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
        speedMph.setMax(speedMph.setMaxModel(12f));
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
        INCLINE_ADC_OFFSET_MIN = 1664;
        INCLINE_ADC_OFFSET_MAX = 640;
        INCLINE_ADC_MIN = (1536 + INCLINE_ADC_OFFSET_MIN);
        INCLINE_ADC_MAX = (24704 - INCLINE_ADC_OFFSET_MAX);
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;
        USES_REVERSE_INCLINE = true;

        // speed rpm kph ratio
        speedRPM.updateRange(178, 4323);

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 0.47f;
        RATE_KPH_PER_SEC = SpeedConvertUtil.convertMphToKph(0.5f);

        // safety
        HAS_TM_ESTOP = false;
        HAS_TM_LOCK_STATE = false;

        KeyMap = FrameKeyMaps.Commercial_Treadmill;
    }

    private static final String IE2_1_FRAME_PREFIX = "TM747"; // NON-NLS
    private static final String IE3_FRAME_PREFIX = "TM784";   // NON-NLS
    private static final String PF_FRAME_PREFIX = "TM776"; // NON-NLS

    @Override
    public String getFramePrefix() {
        return "FTM"; //NON-NLS
    }

    @Override
    public List<String> getFramePrefixes() {
        List<String> prefixes = new ArrayList<>();
        prefixes.add(IE2_1_FRAME_PREFIX);
        prefixes.add(IE3_FRAME_PREFIX);
        prefixes.add(PF_FRAME_PREFIX);
        return prefixes;
    }

    @Override
    public boolean framePrefixValid(String frameSerialNumber) {
        return frameSerialNumber.contains(PF_FRAME_PREFIX) ||
               frameSerialNumber.contains(IE2_1_FRAME_PREFIX) || 
               frameSerialNumber.contains(IE3_FRAME_PREFIX);
    }

    @VisibleForTesting
    @Override
    public String getValidFramePrefix() {
        Random random = new Random();
        switch(random.nextInt(3)) {
            case 0:
                return IE2_1_FRAME_PREFIX;
            case 1:
                return IE3_FRAME_PREFIX;
            case 2:
            default:
                return PF_FRAME_PREFIX;
        }
    }

    @Override
    public String getFrameSerialNumberSuffix(String serialNumber) {
        // Find the matched prefix dynamically and clip the suffix from its end
        List<String> prefixes = getFramePrefixes();
        for (String suffixPrefix : prefixes) {
            int startIndex = serialNumber.indexOf(suffixPrefix);
            if (startIndex >= 0) {
                return serialNumber.substring(startIndex + suffixPrefix.length());
            }
        }
        return serialNumber;
    }

    @Override
    public int getModelId() {
        String frameSerialNumber = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);

        if(frameSerialNumber != null && frameSerialNumber.contains(IE3_FRAME_PREFIX)) {
            return 117;
        }
        else if(frameSerialNumber != null && frameSerialNumber.contains(PF_FRAME_PREFIX)) {
            return 130;
        }
        return 91;
    }
}
