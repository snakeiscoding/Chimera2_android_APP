package com.jht.chimera.ui.machinesettings.frictiontest

import androidx.annotation.VisibleForTesting
import com.jht.chimera.lib.common.formatting.BasicNumberFormat
import kotlin.math.pow


data class Point(val x: Double, val y: Double) {
    constructor(x: Int, y: Int): this(x.toDouble(), y.toDouble())
}

/**
 * Calculates friction from a collection of data points retrieved during a friction test
 */
fun calculateFriction(
    raisingTorque: List<Int>,
    loweringTorque: List<Int>,
    raisingIncline: List<Float>,
    loweringIncline: List<Float>
): String {
    // The torque for the frame when no user is on the belt
    val tareTorque = loweringTorque.average()

    // Average torque data over a window. The torque data is stepped by 2 otherwise.
    val raisingTorqueAvg = windowedAverage(raisingTorque.map { it - tareTorque })

    val raisingPoints = mutableListOf<Point>()
    for (i in raisingTorqueAvg.indices) {
        // convert incline to non-percent format (15%->0.15)
        raisingPoints.add(Point(raisingTorqueAvg[i], raisingIncline[i].toDouble() / 100.0))
    }

    val raisingLine = doLinearRegressionOnPoints(raisingPoints)
    val frictionValue = raisingLine.b
    return BasicNumberFormat.format(frictionValue.toFloat(), 3)
}

@VisibleForTesting
fun windowedAverage(numbers: List<Double>, windowSize: Int = 10): List<Double> {
    val result = mutableListOf<Double>()

    for (i in numbers.indices) {
        val adjustedWindowSize = minOf(i, windowSize, (numbers.size - 1) - i)
        val start = i - adjustedWindowSize
        val end = i + adjustedWindowSize
        val window = numbers.subList(start, end + 1)
        val average = window.average()
        result.add(average)
    }

    return result
}

/**
 * Finds the closest line for a set of points
 */
@VisibleForTesting
fun doLinearRegressionOnPoints(points: List<Point>): LineEquation {
    // averages of x and y
    val meanX = points.sumOf { it.x } / points.size
    val meanY = points.sumOf { it.y } / points.size

    // calculating m
    val m = points.sumOf { (it.x - meanX) * (it.y - meanY) } /
            points.sumOf { (it.x - meanX).pow(2) }

    // calculating b
    val b = meanY - (m * meanX)
    return LineEquation(m, b)
}

// Defines a line with the equation (m*x + b)
data class LineEquation(
    val m: Double,
    val b: Double
)