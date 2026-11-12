package com.jht.chimera.lib.common.unit;

import com.jht.chimera.R;

public enum WeightUnit {
    KILOGRAM(1, R.string.kg_lc),
    POUND(0.45359237, R.string.lbs_lc);

    public final double unitValue;
    public final int lcAbbreviationStringId;
    WeightUnit(double unitValue, int lcAbbreviationStringId) {
        this.unitValue = unitValue;
        this.lcAbbreviationStringId = lcAbbreviationStringId;
    }
}
