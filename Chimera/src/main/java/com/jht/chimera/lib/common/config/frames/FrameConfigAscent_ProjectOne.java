package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class FrameConfigAscent_ProjectOne extends FrameConfig {
    public FrameConfigAscent_ProjectOne() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Ascent;
        machineType = JHTCommonTypes.MachineType.Ascent;
        FRAME_PREFIX = "EP634"; //NON-NLS
        FRAME_ID = 133;
        supportLcbTypeList = new int[]{0x0A};
        MODEL = MODEL_PROJECTONE;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.ProjectOne);

        incline.setMin(incline.setMinModel(0));
        incline.setMax(incline.setMaxModel(100));
        incline.defaultValue = 0;
        incline.precision = 0;
        incline.stepSize = 5;
        inclineLimit.copy(incline);

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

        // incline ranges
        INCLINE_ADC_MIN = 0;
        INCLINE_ADC_MAX = 32768;
        inclineAdc.setMin(inclineAdc.setMinModel(INCLINE_ADC_MIN));
        inclineAdc.setMax(inclineAdc.setMaxModel(INCLINE_ADC_MAX));
        inclineAdc.stepSize = 1;

        // resistance calibration table
        RESISTANCE_TABLE = CalibrationTable.Ascent.RESISTANCE_TABLE;

        // watts table
        WATTS_TABLE = CalibrationTable.Ascent.WATTS_TABLE;

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 3.7f;
        RATE_RESISTANCE_PER_SEC = 5f;

        // Set the bump sensor trigger threshold to 40
        BUMP_SENSOR_TRIGGER_THRESHOLD = 40;

        KeyMap = FrameKeyMaps.Commercial;
        LCB_CAN_CONTROL_WATTS = true;
    }

    private static final String PROJECTONE_FRAME_PREFIX = "FEP634"; // NON-NLS

    @Override
    public String getFramePrefix() {
        return "FTM"; //NON-NLS
    }

    @Override
    public List<String> getFramePrefixes() {
        List<String> prefixes = new ArrayList<>();
        prefixes.add(PROJECTONE_FRAME_PREFIX);
        return prefixes;
    }

    @Override
    public boolean framePrefixValid(String frameSerialNumber) {
        return frameSerialNumber.contains(PROJECTONE_FRAME_PREFIX); // NON-NLS
    }

    @VisibleForTesting
    @Override
    public String getValidFramePrefix() {
        return PROJECTONE_FRAME_PREFIX;
    }

    @Override
    public String getFrameSerialNumberSuffix(String serialNumber) {
        // Dynamically identify where the prefix ends to safely extract the suffix regardless of whether 'F' is present.
        int startIndex = serialNumber.indexOf(PROJECTONE_FRAME_PREFIX);
        if (startIndex >= 0) {
            return serialNumber.substring(startIndex + PROJECTONE_FRAME_PREFIX.length());
        }
        return serialNumber;
    }

    @Override
    public int getModelId() {
        String frameSerialNumber = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);

        if(frameSerialNumber != null && frameSerialNumber.contains(PROJECTONE_FRAME_PREFIX)) {
            return 133;
        }
        return 101;
    }
}
