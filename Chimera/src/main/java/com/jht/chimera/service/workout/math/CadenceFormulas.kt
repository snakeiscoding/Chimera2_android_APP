package com.jht.chimera.service.workout.math

import com.jht.chimera.lib.common.conversionlib.DistanceConvertUtil.convertMetersToYards

object CadenceFormulas {

    /**
     * Returns the average cadence (in steps per minute), given [accumulatedSteps] and [timeSeconds]
     */
    @JvmStatic
    fun averageCadence(accumulatedSteps: Int, timeSeconds: Int): Float {
        if (timeSeconds <= 0) return 0f
        return (accumulatedSteps.toFloat() / timeSeconds.toFloat()) * 60f
    }

    /**
     * Returns stride length in meters or yards depending on [isMetric]
     */
    @JvmStatic
    fun strideLength(speedKph: Float, cadenceSpm: Float, isMetric: Boolean): Float {
        if (cadenceSpm <= 0) return 0f

        // Base calculation in meters: (KPH * 1000 / 60) / SPM
        val strideMeters = (speedKph * 16.6667f) / cadenceSpm
        return if (isMetric) strideMeters else convertMetersToYards(strideMeters)
    }

}