package com.jht.chimera.lib.common.config.frames;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;

import java.util.EnumSet;

public class FrameConfigS700 extends FrameConfig {
    public FrameConfigS700() {
        super();

        machineClassType = JHTCommonTypes.MachineClassificationType.Ascent;
        machineType = JHTCommonTypes.MachineType.Ascent;
        FRAME_PREFIX = "EP726"; 
        FRAME_ID = 3;
        supportLcbTypeList = new int[]{0x0A};
        MODEL = R.string.frame_s700;
        supportProductFlavors = EnumSet.of(ProductConfig.BuildFlavor.Vision600E);

        incline.setMin(incline.setMinModel(0));
        incline.setMax(incline.setMaxModel(100));
        incline.defaultValue = 0;
        incline.precision = 0;
        incline.stepSize = 5;
        inclineLimit.copy(incline);

        resistance.setMin(resistance.setMinModel(1));
        resistance.setMax(resistance.setMaxModel(20));
        resistance.defaultValue = 1;
        resistance.precision = 0;
        resistance.stepSize = 1;

        weightKg.setMin(weightKg.setMinModel(WeightConvertUtil.convertLbsToKg(50)));
        weightKg.setMax(weightKg.setMaxModel(WeightConvertUtil.convertLbsToKg(400)));
        weightKg.defaultValue = WeightConvertUtil.convertLbsToKg(150);
        weightKg.precision = 0;
        weightKg.stepSize = 1;

        weightLbs.setMin(weightKg.setMinModel(50));
        weightLbs.setMax(weightKg.setMaxModel(400));
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
        RESISTANCE_TABLE = new int[][]{{200}, {300}, {410}, {570}, {700}, {840}, {1000}, {1100}, {1260}, {1390}, {1520}, {1680}, {1820}, {1960}, {2120}, {2280}, {2440}, {2600}, {2780}, {2940}};

        // watts table

        WATTS_TABLE = new int[][]{
                { 8, 10, 17, 25, 28, 32, 37, 44, 50, 55, 63, 66, 74, 81, 88, 93,100,105,112,120,129,135,143,150,159,169,180}, //L1
                { 9, 10, 19, 29, 35, 41, 48, 54, 61, 68, 72, 80, 84, 93,100,108,120,128,138,140,147,157,168,173,183,191,208}, //L2
                {10, 11, 23, 34, 40, 47, 56, 64, 71, 77, 86, 94,100,110,117,127,137,146,159,165,172,179,190,200,206,216,237}, //L3
                {10, 11, 27, 40, 50, 57, 67, 78, 84, 95,101,114,120,134,142,151,166,176,189,197,202,211,225,230,242,255,276}, //L4
                {10, 12, 32, 47, 56, 66, 76, 87, 96,107,115,128,138,148,160,171,186,198,209,218,230,237,255,262,270,285,306}, //L5
                {10, 12, 37, 52, 63, 73, 85, 99,106,121,129,143,152,166,179,190,207,218,234,242,250,258,277,288,297,310,337}, //L6
                {10, 13, 43, 59, 73, 85, 98,112,123,138,144,162,175,190,203,218,232,240,262,272,283,292,314,324,337,353,378}, //L7
                {10, 13, 47, 64, 79, 92,106,121,133,148,158,176,187,203,219,235,251,259,283,292,305,318,338,351,364,376,404}, //L8
                {10, 13, 49, 70, 87,101,119,133,146,163,172,194,206,223,239,255,274,284,306,317,332,343,367,382,396,411,439}, //L9
                {10, 13, 51, 78, 95,111,128,145,160,177,187,211,224,243,258,279,297,307,329,343,361,373,396,413,427,445,471}, //L10
                {10, 13, 51, 84,102,119,136,154,170,189,201,225,239,258,275,296,313,327,349,367,383,397,421,436,454,475,503}, //L11
                {10, 13, 51, 90,107,127,145,165,182,203,215,238,255,276,294,315,337,350,372,389,409,423,448,465,486,505,534}, //L12
                {10, 13, 51, 97,115,137,156,177,196,218,230,256,271,295,315,336,358,375,397,414,437,453,481,500,515,538,568}, //L13
                {10, 13, 51, 97,122,145,166,188,207,230,244,271,291,314,333,356,379,398,419,440,460,479,505,530,549,567,599}, //L14
                {10, 13, 51, 97,129,153,174,198,220,243,258,286,304,329,351,373,399,417,440,463,488,503,531,552,572,595,629}, //L15
                {10, 13, 51, 97,136,162,184,209,232,256,273,302,322,348,369,393,420,437,463,485,512,529,557,582,603,623,656}, //L16
                {10, 13, 51, 97,136,167,193,218,243,265,287,315,336,362,386,411,436,455,483,508,534,553,579,606,625,648,681}, //L17
                {10, 13, 51, 97,136,174,203,229,254,280,301,331,353,380,405,431,455,476,505,533,556,577,604,637,654,677,709}, //L18
                {10, 13, 51, 97,136,184,213,241,267,295,316,348,370,397,424,453,477,505,531,557,585,603,637,666,685,710,744}, //L19
                {10, 13, 51, 97,136,194,223,252,278,308,330,362,388,417,443,472,498,525,552,581,612,633,664,690,713,743,776}, //L20
        };

        TABLE_RPM_START = 10;

        // rate
        RATE_INCLINE_PERCENT_PER_SEC = 3.7f;
        RATE_RESISTANCE_PER_SEC = 5f;

        KeyMap = FrameKeyMaps.VisionResistance;
    }

}
