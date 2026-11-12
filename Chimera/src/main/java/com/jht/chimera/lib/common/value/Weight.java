package com.jht.chimera.lib.common.value;

import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.unit.WeightUnit;

/* Represent a weight, with unit.
 *
 * Value and unit are private, and the only value accessor requires a unit
 * to prevent callers from ignoring units.
 * If you like, you can say "whatever unit the weight is natively in",
 * but you have to say so explicitly, by asking for its unit.
 *
 * This approach could of course be generalized for any other type of scalar measurement.
 */
public final class Weight {
    private final double value;
    private final WeightUnit unit;
    public Weight(double value, WeightUnit unit) {
        this.value = value;
        this.unit = unit;
    }
    public WeightUnit getUnit() { return unit; }
    public double getValue(WeightUnit unit) {
        return WeightConvertUtil.convert(value, this.unit, unit);
    }
}
