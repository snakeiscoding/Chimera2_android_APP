package com.jht.chimera.lib.common.frictiontest

import com.jht.chimera.ui.machinesettings.frictiontest.Point
import com.jht.chimera.ui.machinesettings.frictiontest.calculateFriction
import com.jht.chimera.ui.machinesettings.frictiontest.doLinearRegressionOnPoints
import com.jht.chimera.ui.machinesettings.frictiontest.windowedAverage
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class FrictionTestUtilTest {
    @Test
    fun testFrictionCalculation() {
        val raisingTorque = getRaisingTorqueTestData()
        val loweringTorque = getLoweringTorqueTestData()
        val raisingIncline = getRaisingInclineTestData()
        val loweringIncline = getLoweringInclineTestData()

        val calculatedFriction = calculateFriction(raisingTorque, loweringTorque, raisingIncline, loweringIncline)

        assertEquals("0.213", calculatedFriction)
    }

    private fun getRaisingTorqueTestData() : List<Int> {
        val listData = ArrayList<Int>()
        for (i in 0..16) { listData.add(15) }
        for (i in 0..16) { listData.add(13) }
        for (i in 0..16) { listData.add(11) }
        for (i in 0..16) { listData.add(9) }
        for (i in 0..16) { listData.add(7) }
        for (i in 0..16) { listData.add(5) }
        for (i in 0..16) { listData.add(3) }
        for (i in 0..4) { listData.add(1) }
        return listData
    }

    private fun getLoweringTorqueTestData() : List<Int> {
        val listData = ArrayList<Int>()
        for (i in 0..16) { listData.add(15) }
        for (i in 0..16) { listData.add(13) }
        for (i in 0..16) { listData.add(11) }
        for (i in 0..16) { listData.add(9) }
        for (i in 0..16) { listData.add(7) }
        for (i in 0..16) { listData.add(5) }
        for (i in 0..16) { listData.add(3) }
        for (i in 0..4) { listData.add(1) }
        return listData
    }

    private fun getRaisingInclineTestData() : List<Float> {
        val listData = ArrayList<Float>()
        for (i in 0..16) { listData.add(0.0f) }
        for (i in 0..16) { listData.add(3.0f) }
        for (i in 0..16) { listData.add(6.0f) }
        for (i in 0..16) { listData.add(8.0f) }
        for (i in 0..16) { listData.add(11.0f) }
        for (i in 0..16) { listData.add(13.0f) }
        for (i in 0..16) { listData.add(15.0f) }
        for (i in 0..4) { listData.add(15.0f) }
        return listData
    }

    private fun getLoweringInclineTestData() : List<Float> {
        val listData = ArrayList<Float>()
        for (i in 0..4) { listData.add(15.0f) }
        for (i in 0..16) { listData.add(15.0f) }
        for (i in 0..16) { listData.add(13.0f) }
        for (i in 0..16) { listData.add(11.0f) }
        for (i in 0..16) { listData.add(8.0f) }
        for (i in 0..16) { listData.add(6.0f) }
        for (i in 0..16) { listData.add(3.0f) }
        for (i in 0..16) { listData.add(0.0f) }
        return listData
    }



    @Test
    fun testWindowedAverage() {
        val numbers = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15).map { it.toDouble() }
        val expectedAverages = listOf(
            6.0, 6.5, 7.0, 7.5, 8.0, 8.0, 8.0, 8.0, 8.0, 8.0,
            8.0, 8.5, 9.0, 9.5, 10.0
        )

        val actualAverages = windowedAverage(numbers)

        // Assert that the size of the result list matches the expected size
        assertEquals(expectedAverages.size, actualAverages.size)

        // Assert that each calculated average matches the expected average
        for (i in expectedAverages.indices) {
            assertEquals(expectedAverages[i], actualAverages[i], 0.001)
        }
    }

    @Test
    fun testWindowedAverageEmptyList() {
        val numbers = emptyList<Int>().map { it.toDouble() }
        val expectedAverages = emptyList<Double>()

        val actualAverages = windowedAverage(numbers)

        // Assert that the result list is empty as well
        assertEquals(expectedAverages, actualAverages)
    }

    @Test
    fun testWindowedAverageSingleElement() {
        val numbers = listOf(5).map { it.toDouble() }
        val expectedAverages = listOf(5.0)

        val actualAverages = windowedAverage(numbers)

        // Assert that the result list has one element with the same value
        assertEquals(expectedAverages, actualAverages)
    }

    @Test
    fun testWindowedAverageBoundaryConditions() {
        val numbers = listOf(1, 2, 3, 4, 5).map { it.toDouble() }
        val expectedAverages = listOf(3.0, 3.0, 3.0, 3.0, 3.0)

        val actualAverages = windowedAverage(numbers)

        // Assert that each calculated average matches the expected average
        for (i in expectedAverages.indices) {
            assertEquals(expectedAverages[i], actualAverages[i], 0.001)
        }
    }

    @Test
    fun testRegression() {
        val pointsToTest = ArrayList<Point>()
        pointsToTest.add(Point(0, 0))
        pointsToTest.add(Point(1, 1))
        pointsToTest.add(Point(2, 2))
        val lineEquation = doLinearRegressionOnPoints(pointsToTest)

        assertEquals(lineEquation.m, 1.0, 0.0001)
        assertEquals(lineEquation.b, 0.0, 0.0001)
    }

    @Test
    fun testDoLinearRegressionOnPoints() {
        val points = listOf(
            Point(1.0, 2.0),
            Point(2.0, 3.0),
            Point(3.0, 4.0),
            Point(4.0, 5.0),
            Point(5.0, 6.0)
        )

        val expectedM = 1.0
        val expectedB = 1.0

        val lineEquation = doLinearRegressionOnPoints(points)
        assertEquals(expectedM, lineEquation.m, 0.001)
        assertEquals(expectedB, lineEquation.b, 0.001)
    }

    @Test
    fun testDoLinearRegressionOnPoints2() {
        val points = listOf(
            Point(1.0, 1.0),
            Point(3.0, 3.0),
            Point(5.0, 5.0),
            Point(7.0, 4.0),
            Point(9.0, 7.0)
        )

        // Values are expected from a least squares best fit
        val expectedM = 0.65
        val expectedB = 0.75

        val lineEquation = doLinearRegressionOnPoints(points)
        assertEquals(expectedM, lineEquation.m, 0.001)
        assertEquals(expectedB, lineEquation.b, 0.001)
    }

    @Test
    fun testCalculateFriction() {
        val raisingPoints = getTestData()

        val raisingTorqueValues = raisingPoints.map { it.x.toInt() }
        val raisingInclineValues = raisingPoints.map { (it.y * 100.0).toFloat() }
        val friction = calculateFriction(raisingTorqueValues, listOf(0, 0), raisingInclineValues, listOf(0f, 0f))
        assertEquals("0.186", friction)
    }

    private fun getTestData() : List<Point> {
        return listOf(
            Point(17.0, 0.0),
            Point(17.0, 0.0),
            Point(17.0, 0.000144788),
            Point(15.0, 0.001013514),
            Point(17.0, 0.001809846),
            Point(17.0, 0.002388996),
            Point(17.0, 0.002750965),
            Point(15.0, 0.004198842),
            Point(17.0, 0.005212355),
            Point(17.0, 0.006081081),
            Point(15.0, 0.007311776),
            Point(15.0, 0.00796332),
            Point(17.0, 0.009194015),
            Point(17.0, 0.010279923),
            Point(15.0, 0.01078668),
            Point(13.0, 0.011221042),
            Point(15.0, 0.012668919),
            Point(17.0, 0.014116795),
            Point(17.0, 0.015202703),
            Point(15.0, 0.015854247),
            Point(13.0, 0.016433398),
            Point(15.0, 0.016578185),
            Point(15.0, 0.01780888),
            Point(15.0, 0.018605212),
            Point(13.0, 0.019908301),
            Point(15.0, 0.02121139),
            Point(17.0, 0.02215251),
            Point(15.0, 0.022804054),
            Point(13.0, 0.023238417),
            Point(13.0, 0.023600386),
            Point(17.0, 0.023889961),
            Point(15.0, 0.024975869),
            Point(13.0, 0.02671332),
            Point(13.0, 0.028088803),
            Point(15.0, 0.029319498),
            Point(15.0, 0.030405405),
            Point(13.0, 0.030912162),
            Point(13.0, 0.031491313),
            Point(15.0, 0.03257722),
            Point(15.0, 0.033373552),
            Point(13.0, 0.033880309),
            Point(13.0, 0.034749035),
            Point(15.0, 0.035907336),
            Point(15.0, 0.036631274),
            Point(13.0, 0.037934363),
            Point(11.0, 0.038875483),
            Point(11.0, 0.039454633),
            Point(15.0, 0.040250965),
            Point(15.0, 0.041264479),
            Point(13.0, 0.041771236),
            Point(13.0, 0.042060811),
            Point(15.0, 0.043870656),
            Point(15.0, 0.044956564),
            Point(13.0, 0.04582529),
            Point(13.0, 0.046549228),
            Point(11.0, 0.04734556),
            Point(13.0, 0.047997104),
            Point(13.0, 0.04886583),
            Point(11.0, 0.0503861),
            Point(11.0, 0.051110039),
            Point(13.0, 0.051544402),
            Point(13.0, 0.052557915),
            Point(11.0, 0.053716216),
            Point(9.0, 0.054222973),
            Point(13.0, 0.055236486),
            Point(13.0, 0.056105212),
            Point(13.0, 0.056611969),
            Point(11.0, 0.057335907),
            Point(11.0, 0.058277027),
            Point(13.0, 0.059073359),
            Point(11.0, 0.059580116),
            Point(9.0, 0.06023166),
            Point(9.0, 0.061100386),
            Point(11.0, 0.062403475),
            Point(11.0, 0.06363417),
            Point(11.0, 0.064430502),
            Point(9.0, 0.065082046),
            Point(13.0, 0.065878378),
            Point(13.0, 0.066891892),
            Point(11.0, 0.067688224),
            Point(9.0, 0.068629344),
            Point(9.0, 0.0691361),
            Point(11.0, 0.070366795),
            Point(11.0, 0.071380309),
            Point(9.0, 0.072176641),
            Point(11.0, 0.072900579),
            Point(11.0, 0.073841699),
            Point(11.0, 0.074710425),
            Point(9.0, 0.075651544),
            Point(9.0, 0.076447876),
            Point(11.0, 0.076954633),
            Point(11.0, 0.077533784),
            Point(9.0, 0.078764479),
            Point(9.0, 0.07992278),
            Point(9.0, 0.080574324),
            Point(9.0, 0.081081081),
            Point(9.0, 0.081949807),
            Point(7.0, 0.082746139),
            Point(9.0, 0.083542471),
            Point(9.0, 0.08484556),
            Point(9.0, 0.085569498),
            Point(7.0, 0.086293436),
            Point(9.0, 0.087379344),
            Point(9.0, 0.088103282),
            Point(9.0, 0.088754826),
            Point(7.0, 0.089478764),
            Point(9.0, 0.090275097),
            Point(9.0, 0.09128861),
            Point(9.0, 0.092012548),
            Point(7.0, 0.092664093),
            Point(7.0, 0.093170849),
            Point(7.0, 0.093894788),
            Point(7.0, 0.094835907),
            Point(7.0, 0.096718147),
            Point(5.0, 0.09715251),
            Point(7.0, 0.097804054),
            Point(9.0, 0.098455598),
            Point(9.0, 0.099396718),
            Point(7.0, 0.099831081),
            Point(7.0, 0.100410232),
            Point(9.0, 0.101351351),
            Point(9.0, 0.102147683),
            Point(7.0, 0.102726834),
            Point(5.0, 0.103812741),
            Point(7.0, 0.104464286),
            Point(9.0, 0.105550193),
            Point(9.0, 0.106346525),
            Point(7.0, 0.107360039),
            Point(5.0, 0.107939189),
            Point(7.0, 0.108807915),
            Point(7.0, 0.109169884),
            Point(7.0, 0.109821429),
            Point(5.0, 0.110328185),
            Point(5.0, 0.11097973),
            Point(7.0, 0.112644788),
            Point(7.0, 0.113875483),
            Point(5.0, 0.115033784),
            Point(7.0, 0.11590251),
            Point(7.0, 0.116843629),
            Point(7.0, 0.117495174),
            Point(7.0, 0.118074324),
            Point(5.0, 0.11894305),
            Point(7.0, 0.119449807),
            Point(7.0, 0.120101351),
            Point(7.0, 0.12140444),
            Point(5.0, 0.122055985),
            Point(5.0, 0.122562741),
            Point(7.0, 0.12386583),
            Point(7.0, 0.124444981),
            Point(5.0, 0.125820463),
            Point(5.0, 0.126906371),
            Point(5.0, 0.127485521),
            Point(7.0, 0.127919884),
            Point(5.0, 0.128643822),
            Point(5.0, 0.129440154),
            Point(3.0, 0.129946911),
            Point(5.0, 0.130453668),
            Point(7.0, 0.131105212),
            Point(7.0, 0.131829151),
            Point(5.0, 0.132697876),
            Point(3.0, 0.13371139),
            Point(5.0, 0.13523166),
            Point(5.0, 0.136027992),
            Point(5.0, 0.136679537),
            Point(3.0, 0.13769305),
            Point(5.0, 0.138706564),
            Point(5.0, 0.13957529),
            Point(5.0, 0.140226834),
            Point(3.0, 0.141747104),
            Point(5.0, 0.143194981),
            Point(5.0, 0.143991313),
            Point(3.0, 0.144642857),
            Point(3.0, 0.145583977),
            Point(3.0, 0.146235521),
            Point(5.0, 0.146959459),
            Point(5.0, 0.147900579),
            Point(3.0, 0.148841699),
            Point(3.0, 0.149565637),
            Point(3.0, 0.15),
        )
    }
}