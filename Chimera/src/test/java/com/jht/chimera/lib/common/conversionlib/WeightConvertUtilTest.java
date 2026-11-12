package com.jht.chimera.lib.common.conversionlib;

import static com.jht.chimera.lib.common.unit.WeightUnit.KILOGRAM;
import static com.jht.chimera.lib.common.unit.WeightUnit.POUND;

import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.lib.common.i18n.Unit;
import com.jht.chimera.lib.common.unit.WeightUnit;
import com.jht.chimera.lib.common.value.Weight;
import com.jhtp.dapiservice.um.DapiUserInfo;

import junit.framework.TestCase;

import org.junit.Test;

public class WeightConvertUtilTest extends TestCase
{
    // WeightConvertUtil.convert is pretty trivial,
    // but let's make sure I didn't mix up the multiply and divide symbols

    static final float KG_PER_LB = 0.45359237f;

    @Test public void testConvertKgToLb() {
        assertEquals(99.2, WeightConvertUtil.convert(45, KILOGRAM, POUND), 0.1);
        assertEquals(99.2, WeightConvertUtil.convertKilogramsToPounds(45), 0.1);
        assertEquals(0, WeightConvertUtil.convertKilogramsToPounds(0), 0.0);
    }
    @Test public void testConvertLbToKg() {
        assertEquals(45.4, WeightConvertUtil.convert(100, POUND, KILOGRAM), 0.1);
        assertEquals(45.4, WeightConvertUtil.convertLbsToKg(100), 0.1);
    }
    @Test public void testConvertNoop() {
        for(WeightUnit unit : WeightUnit.values()) {
            assertEquals(123, WeightConvertUtil.convert(123, unit, unit), 0.0001);
        }
    }

    protected void testSetWeightOnDapiUserInfo(Unit preferredSystem, WeightUnit weightUnit, float value) {
        // Do our own independent conversions to make sure Weight et all are behaving:
        final float inputKg, inputLb;
        switch(weightUnit) {
            case KILOGRAM:
                inputKg = value;
                inputLb = value / KG_PER_LB;
                break;
            case POUND:
                inputLb = value;
                inputKg = value * KG_PER_LB;
                break;
            default:
                throw new RuntimeException("Unrecognized weight unit: "+weightUnit);
        }
        DapiUserInfo du = new DapiUserInfo();
        DapiUser.setInfoUnitPreferenceAndWeight(du, preferredSystem, new Weight(value, weightUnit));
        assertEquals(du.UnitsPreferenceMetric, preferredSystem == Unit.METRIC);
        final float expectedStoredKg, expectedStoredLb;
        switch(preferredSystem) {
            case METRIC:
                expectedStoredKg = inputKg;
                expectedStoredLb = expectedStoredKg / KG_PER_LB;
                assertEquals(expectedStoredKg, du.Weight);
                break;
            case IMPERIAL:
                expectedStoredLb = inputLb;
                expectedStoredKg = expectedStoredLb * KG_PER_LB;
                assertEquals(expectedStoredLb, du.Weight);
                break;
            default:
                throw new RuntimeException("Unrecognized unit system: "+preferredSystem);
        }

        // Also test WeightConvertUtil.getWeight
        Weight gottenWeight = DapiUser.getInfoWeight(du);
        assertEquals(expectedStoredKg, gottenWeight.getValue(KILOGRAM), 0.0001);
        assertEquals(expectedStoredLb, gottenWeight.getValue(POUND), 0.0001);
    }

    @Test public void testSetKgOnMetricDapiUserInfo() {
        testSetWeightOnDapiUserInfo(Unit.METRIC, KILOGRAM, 78);
    }
    @Test public void testSetLbOnMetricDapiUserInfo() {
        testSetWeightOnDapiUserInfo(Unit.METRIC, POUND, 79);
    }
    @Test public void testSetKgOnImperialDapiUserInfo() {
        testSetWeightOnDapiUserInfo(Unit.IMPERIAL, KILOGRAM, 160);
    }
    @Test public void testSetLbOnImperialDapiUserInfo() {
        testSetWeightOnDapiUserInfo(Unit.IMPERIAL, POUND, 161);
    }
}
