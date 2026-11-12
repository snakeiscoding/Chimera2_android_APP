package com.jht.interfaces

import com.jht.enums.UnitMeasurement
import org.junit.Test
import kotlin.math.abs

class UnitMeasurementTest {

    private fun closeEnough(v1: Double, v2: Double) {
        println("$v1 ~ $v2")
        assert(abs(v1 - v2) < 0.0001f)
    }

    @Test
    fun testLength() {
        // 5 mm to meters
        closeEnough(5.0 / 1000, 5.0 * UnitMeasurement.Millimeter.ratio)
        // 5 cm to meters
        closeEnough(5.0 / 100, 5.0 * UnitMeasurement.Centimeter.ratio)
        // 5 m to meters
        closeEnough(5.0, 5.0 * UnitMeasurement.Meter.ratio)
        // 5 km to meters
        closeEnough(5000.0, 5.0 * UnitMeasurement.Kilometer.ratio)
        // 5 miles to meters
        closeEnough(8046.72, 5.0 * UnitMeasurement.Mile.ratio)
        // 5 yards to meters
        closeEnough(4.572, 5.0 * UnitMeasurement.Yard.ratio)
        // 5 feet to meters
        closeEnough(1.524, 5.0 * UnitMeasurement.Foot.ratio)
        // 5 inches to meters
        closeEnough(0.127, 5.0 * UnitMeasurement.Inch.ratio)
    }

    @Test
    fun testTime() {
        // 5 ms to sec
        closeEnough(0.005, 5.0 * UnitMeasurement.Millisecond.ratio)
        // 5 min to sec
        closeEnough(5.0 * 60, 5.0 * UnitMeasurement.Minute.ratio)
        // 5 hr to sec
        closeEnough(5.0 * 60 * 60, 5.0 * UnitMeasurement.Hour.ratio)
        // 5 day to sec
        closeEnough(432000.0, 5.0 * UnitMeasurement.Day.ratio)
        // 5 year to sec
        //closeEnough(157788000.0, 5.0 * UnitMeasurement.Year.ratio)
    }

    @Test
    fun testSpeed() {
        // 5 mph to m/s
        closeEnough(2.2352, 5.0 * UnitMeasurement.MilePerHour.ratio)
        // 5 kph to m/s
        closeEnough(1.38889, 5.0 * UnitMeasurement.KilometerPerHour.ratio)
    }

    @Test
    fun testWeight() {
        // 5 kg to g
        closeEnough(5000.0, 5.0 * UnitMeasurement.Kilogram.ratio)
        // 5 pounds to g
        closeEnough(2267.96185, 5.0 * UnitMeasurement.Pound.ratio)
    }

    /*@Test
    fun testEnergy() {
        // 5 nutrition calories to joules
        closeEnough(20920.0, 5.0 * UnitMeasurement.Kilocalorie.ratio)
    }*/
}