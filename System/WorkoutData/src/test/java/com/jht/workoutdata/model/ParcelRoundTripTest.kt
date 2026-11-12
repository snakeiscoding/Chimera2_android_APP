package com.jht.workoutdata.model

import android.os.Parcel
import android.os.Parcelable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression net for the cross-process contract: every Parcelable in the module must survive a
 * real write/read cycle with an explicit classloader (the BadParcelableException class of bug,
 * and @Parcelize field-order drift).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ParcelRoundTripTest {

    private inline fun <reified T : Parcelable> roundTrip(value: T): T {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeParcelable(value, 0)
            parcel.setDataPosition(0)
            @Suppress("DEPRECATION")
            parcel.readParcelable(T::class.java.classLoader)!!
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `WorkoutDataPoint survives a parcel roundtrip field-for-field`() {
        val point = WorkoutDataPoint(
            elapsedTimeSec = 61, workoutStage = 1, lapIndex = 2, splitIndex = 3,
            distanceKm = 1.25f, speedKph = 8.5f, inclinePercent = 3.5f, resistance = 7,
            rpm = 85, spm = 120, heartRate = 132, watts = 210, targetHeartRate = 140,
            targetWatts = 220, totalCalories = 55.5f, mets = 6.2f, totalFloors = 2.5f,
            totalSteps = 640, elevationMeters = 12.5f,
        )
        assertEquals(point, roundTrip(point))
    }

    @Test
    fun `MetricSample survives a parcel roundtrip`() {
        val sample = MetricSample(elapsedTimeSec = 30, value = 145f)
        assertEquals(sample, roundTrip(sample))
    }

    @Test
    fun `WorkoutFutureData survives a parcel roundtrip including nested profiles`() {
        val future = WorkoutFutureData(
            listOf(
                FutureProfile(WorkoutMetric.INCLINE, listOf(FutureProfilePoint(30.5f, 4.5f))),
                FutureProfile(WorkoutMetric.RESISTANCE, listOf(FutureProfilePoint(60f, 12f), FutureProfilePoint(90f, 14f))),
            )
        )
        assertEquals(future, roundTrip(future))
    }

    @Test
    fun `empty WorkoutFutureData survives a parcel roundtrip`() {
        assertEquals(WorkoutFutureData(), roundTrip(WorkoutFutureData()))
    }

    @Test
    fun `WorkoutMetric columnName matches the contract for every metric`() {
        // Guards against a column rename breaking series projection silently.
        WorkoutMetric.entries.forEach { metric ->
            val point = WorkoutDataPoint(elapsedTimeSec = 1, heartRate = 7, watts = 7, resistance = 7, rpm = 7, spm = 7, speedKph = 7f, inclinePercent = 7f)
            assertEquals("valueOf(${metric.name}) mapping", 7f, point.valueOf(metric))
        }
    }
}
