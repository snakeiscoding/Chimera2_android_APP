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

public class FrameConfigTreadmill_Lifestyle extends FrameConfig {
    public FrameConfigTreadmill_Lifestyle() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Treadmill;
        machineType = JHTCommonTypes.MachineType.Treadmill;
        FRAME_PREFIX = "TM759"; //NON-NLS
        FRAME_ID = 106;
        supportLcbTypeList = new int[]{0xC3,0xC7,0xA3};
        MODEL = MODEL_LIFESTYLE;
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
        INCLINE_ADC_MIN = (1536 + INCLINE_ADC_OFFSET_MIN); // 25x128 = 3200 = 1536 + INCLINE_ADC_OFFSET_MIN
        INCLINE_ADC_MAX = (27392 - INCLINE_ADC_OFFSET_MAX); // 209x128 = 26752 = 27392 - INCLINE_ADC_OFFSET_MAX
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;
        USES_REVERSE_INCLINE = true;

        // speed rpm kph ratio
        speedRPM.updateRange(139, 3440);

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 0.47f;
        RATE_KPH_PER_SEC = SpeedConvertUtil.convertMphToKph(0.5f);

        // safety
        HAS_TM_ESTOP = false;
        HAS_TM_LOCK_STATE = false;

        isLifestyle = true;

        KeyMap = FrameKeyMaps.Commercial_Treadmill;
    }

    // 0xC3,0xC7 (Old LCBs)
    private static final String IE2_1_FRAME_PREFIX = "TM759"; // NON-NLS
    // 0xA3
    private static final String IE3_FRAME_PREFIX = "TM785"; // NON-NLS
    // 0xA3
    private static final String T_LS_03_FRAME_PREFIX = "TM1054"; // NON-NLS
    @Override
    public String getFramePrefix() {
        // Note: this function is used when the frame prefix doesn't match the LCB type to try to simplify the frame serial number entry
        // since 0xA3 can either be TM785 or TM1054, return TM so the installer will need to fill in the rest
        return (DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT) == 0xA3 ? "TM" : IE2_1_FRAME_PREFIX); //NON-NLS
    }

    @Override
    public List<String> getFramePrefixes() {
        List<String> prefixes = new ArrayList<>();
        prefixes.add(IE2_1_FRAME_PREFIX);
        prefixes.add(IE3_FRAME_PREFIX);
        prefixes.add(T_LS_03_FRAME_PREFIX);
        return prefixes;
    }

    @Override
    public boolean framePrefixValid(String frameSerialNumber) {
        return frameSerialNumber.contains(IE2_1_FRAME_PREFIX)
                || frameSerialNumber.contains(IE3_FRAME_PREFIX)
                || frameSerialNumber.contains(T_LS_03_FRAME_PREFIX); // NON-NLS
    }

    @Override
    public int getMotorType() {
        return DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT) == 0xA3 ? 0 : 4; // Set to 4 if old motor type.
    }

    @Override
    public int getModelId() {
        String frameSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
        if (frameSerial != null && frameSerial.contains(IE2_1_FRAME_PREFIX)) return 118;
        if (frameSerial != null && frameSerial.contains(IE3_FRAME_PREFIX)) return 106;
        if (frameSerial != null && frameSerial.contains(T_LS_03_FRAME_PREFIX)) return 155;
        return 0;
    }

}
