package com.jht.chimera.service.workout

import com.jht.chimera.service.workout.math.CadenceFormulas.averageCadence
import com.jht.chimera.service.workout.math.CadenceFormulas.strideLength
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class StrideLengthTest {

    // Threshold delta for floating-point assertions
    private val delta = 0.00001f

    //Test very low values
    @Test
    fun `zero spm zero kph`() {
        val spm = 0
        val speedKph = 0f
        val strideLengthMeters = strideLength(speedKph, spm.toFloat(), true)
        assertEquals(0f, strideLengthMeters, delta)
    }

    @Test
    fun `zero spm 5 kph`() {
        val spm = 0
        val speedKph = 5f
        val strideLengthMeters = strideLength(speedKph, spm.toFloat(), true)
        assertEquals(0f, strideLengthMeters, delta)
    }

    //test reasonable values
    @Test
    fun `9 kph 150 spm`() {
        val spm = 150
        val speedKph = 9f
        val strideLengthMeters = strideLength(speedKph, spm.toFloat(), true)
        assertEquals(1.000002f, strideLengthMeters, delta)
    }

    @Test
    fun `10 kph 150 spm`() {
        val spm = 150
        val speedKph = 10f
        val strideLengthMeters = strideLength(speedKph, spm.toFloat(), true)
        assertEquals(1.1111133f, strideLengthMeters, delta)
    }

    @Test
    fun `9 kph 157 spm - standard mid jog`() {
        val spm = 157
        val speedKph = 9f
        val strideLengthMeters = strideLength(speedKph, spm.toFloat(), true)
        assertEquals(0.955416f, strideLengthMeters, delta)
    }

    @Test
    fun `13 kph 170 spm - steady fast run`() {
        val spm = 170
        val speedKph = 13f
        val strideLengthMeters = strideLength(speedKph, spm.toFloat(), true)
        assertEquals(1.274512f, strideLengthMeters, delta)
    }

    @Test
    fun `15 kph 180 spm - high intensity workout`() {
        val spm = 180
        val speedKph = 15f
        val strideLengthMeters = strideLength(speedKph, spm.toFloat(), true)
        assertEquals(1.388892f, strideLengthMeters, delta)
    }

    //test extremely high values
    @Test
    fun `24 kph 200 spm - elite sprinting peak`() {
        val spm = 200
        val speedKph = 24f
        val strideLengthMeters = strideLength(speedKph, spm.toFloat(), true)
        assertEquals(2.000004f, strideLengthMeters, delta)
    }

    //test unlikely combinations
    @Test
    fun `15 kph 60 spm - giant leaps bounding`() {
        val spm = 60
        val speedKph = 15f
        val strideLengthMeters = strideLength(speedKph, spm.toFloat(), true)
        assertEquals(4.166675f, strideLengthMeters, delta) // Unrealistic 4.1-meter stride
    }

    @Test
    fun `2 kph 180 spm - ultra fast baby steps`() {
        val spm = 180
        val speedKph = 2f
        val strideLengthMeters = strideLength(speedKph, spm.toFloat(), true)
        assertEquals(0.185185f, strideLengthMeters, delta) // Tiny 18cm stride
    }

    //Test imperial conversion
    @Test
    fun `9 kph 150 spm in yards`() {
        val spm = 150
        val speedKph = 9f
        val strideLengthYards = strideLength(speedKph, spm.toFloat(), false)

        // 1.000002f meters / 0.9144f = 1.0936155f yards
        assertEquals(1.0936155f, strideLengthYards, delta)
    }

    @Test
    fun `13 kph 170 spm in yards`() {
        val spm = 170
        val speedKph = 13f
        val strideLengthYards = strideLength(speedKph, spm.toFloat(), false)

        // 1.274512f meters / 0.9144f = 1.3938233f yards
        assertEquals(1.3938233f, strideLengthYards, delta)
    }
}

class StrideLengthOutlierTest {

    private val delta = 0.00001f

    @Test
    fun `high cadence with zero speed - stepping in place`() {
        val spm = 200f
        val speedKph = 0f
        val strideLengthMeters = strideLength(speedKph, spm, true)
        // Numerator is 0, should safely return exactly 0
        assertEquals(0f, strideLengthMeters, delta)
    }

    @Test
    fun `very high cadence with microscopic speed - tiny shuffle`() {
        val spm = 300f
        val speedKph = 0.1f // Barely moving forward
        val strideLengthMeters = strideLength(speedKph, spm, true)

        // (0.1 * 16.6667) / 300 = 0.00555556 meters (approx 5.5 millimeters)
        assertEquals(0.00555556f, strideLengthMeters, delta)
    }

    @Test
    fun `maximum allowed speed with 1 spm - giant leap`() {
        val spm = 1f
        val speedKph = 30f // Max treadmill speed
        val strideLengthMeters = strideLength(speedKph, spm, true)

        // (30 * 16.6667) / 1 = 500.001f meters
        assertEquals(500.001f, strideLengthMeters, delta)
    }

    @Test
    fun `microscopic positive cadence and speed - fractional precision test`() {
        val spm = 0.01f
        val speedKph = 0.01f

        val strideLengthMeters = strideLength(speedKph, spm, true)

        // (0.01 * 16.6667) / 0.01 = 16.6667f meters
        // Ensures extreme fractional division maintains precision
        assertEquals(16.6667f, strideLengthMeters, delta)
    }
}

class AverageStrideLengthTest {

    // Helper function to simulate UI rounding to 2 decimal places
    // Using Locale.US ensures predictable "." decimal separators in tests
    private fun formatStride(value: Float): String {
        return String.format(Locale.US, "%.2f", value)
    }

    @Test
    fun `9 kph jog with averaged spm`() {
        val averageSpeedKph = 9.0f
        val averageSpm = 154.5f
        val averageStrideLengthMeters = strideLength(averageSpeedKph, averageSpm, true)

        // Raw: 0.9708757f -> Rounds to "0.97"
        val formattedResult = formatStride(averageStrideLengthMeters)
        assertEquals("0.97", formattedResult)
    }

    @Test
    fun `13 kph steady run with high-precision average speed and cadence`() {
        val averageSpeedKph = 13.25f
        val averageSpm = 171.4f
        val averageStrideLengthMeters = strideLength(averageSpeedKph, averageSpm, true)

        // Raw: 1.2884117f -> Rounds up to "1.29"
        val formattedResult = formatStride(averageStrideLengthMeters)
        assertEquals("1.29", formattedResult)
    }

    @Test
    fun `15 kph high intensity interval with micro-step precision`() {
        val averageSpeedKph = 15.0f
        val averageSpm = 178.75f
        val averageStrideLengthMeters = strideLength(averageSpeedKph, averageSpm, true)

        // Raw: 1.3986042f -> Rounds up to "1.40"
        val formattedResult = formatStride(averageStrideLengthMeters)
        assertEquals("1.40", formattedResult)
    }

    @Test
    fun `average cadence in yards`() {
        val averageSpeedKph = 13.0f
        val averageSpm = 168.33f
        val averageStrideLengthYards = strideLength(averageSpeedKph, averageSpm, false)

        // Raw: 1.4076517f -> Rounds up to "1.41"
        val formattedResult = formatStride(averageStrideLengthYards)
        assertEquals("1.41", formattedResult)
    }
}

class AverageCadenceTest {
    private val delta = 0.00001f

    //Very low values and boundaries
    @Test
    fun `zero steps zero seconds`() {
        val steps = 0
        val seconds = 0
        val actualCadence = averageCadence(steps, seconds)
        assertEquals(0f, actualCadence, delta)
    }

    @Test
    fun `100 steps zero seconds`() {
        val steps = 100
        val seconds = 0
        val actualCadence = averageCadence(steps, seconds)
        assertEquals(0f, actualCadence, delta)
    }

    @Test
    fun `100 steps negative seconds`() {
        val steps = 100
        val seconds = -30 // Faulty sensor or system clock edge case
        val actualCadence = averageCadence(steps, seconds)
        assertEquals(0f, actualCadence, delta)
    }

    @Test
    fun `zero steps with positive time`() {
        val steps = 0
        val seconds = 60
        val actualCadence = averageCadence(steps, seconds)
        assertEquals(0f, actualCadence, delta)
    }

    //Reasonable values for treadmill
    @Test
    fun `9 kph treadmill jog simulation`() {
        // A runner doing 155 SPM for a 10-minute (600s) warm-up jog
        val steps = 1550
        val seconds = 600
        val actualCadence = averageCadence(steps, seconds)
        assertEquals(155.0f, actualCadence, delta)
    }

    @Test
    fun `13 kph steady run simulation`() {
        // A runner maintaining 170 SPM for a 5-minute (300s) tempo block
        val steps = 850
        val seconds = 300
        val actualCadence = averageCadence(steps, seconds)
        assertEquals(170.0f, actualCadence, delta)
    }

    @Test
    fun `15 kph high intensity 1-minute interval`() {
        // A runner hitting exactly 30 steps in a short 10-second burst (180 SPM)
        val steps = 30
        val seconds = 10
        val actualCadence = averageCadence(steps, seconds)
        assertEquals(180.0f, actualCadence, delta)
    }

    // Test outliers and extreme combinations
    @Test
    fun `microscopic time fraction`() {
        val steps = 1
        val seconds = 1 // 1 step in 1 second
        val actualCadence = averageCadence(steps, seconds)
        assertEquals(60.0f, actualCadence, delta)
    }

    @Test
    fun `massive time tracking - ultra marathon`() {
        // 55,000 steps over 5 hours (18,000 seconds)
        val steps = 55000
        val seconds = 18000
        val actualCadence = averageCadence(steps, seconds)
        // (55000 / 18000) * 60 = 183.33333 SPM
        assertEquals(183.33333f, actualCadence, delta)
    }
}