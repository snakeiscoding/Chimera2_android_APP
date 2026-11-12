package com.jht.chimera.lib.common.conversionlib;

import com.jht.chimera.lib.common.unit.WeightUnit;

public class WeightConvertUtil
{

    public static double convert(double value, WeightUnit inputUnit, WeightUnit outputUnit) {
        return inputUnit == outputUnit ? value : value * inputUnit.unitValue / outputUnit.unitValue;
    }

    public static float convertKilogramsToPounds(float kg) {
        return (float)convert(kg, WeightUnit.KILOGRAM, WeightUnit.POUND);
    }
    public static float convertLbsToKg(float lb) {
        return (float)convert(lb, WeightUnit.POUND, WeightUnit.KILOGRAM);
    }

}
