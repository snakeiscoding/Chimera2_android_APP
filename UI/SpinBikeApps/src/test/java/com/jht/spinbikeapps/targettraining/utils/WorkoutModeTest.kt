package com.jht.spinbikeapps.targettraining.utils

import com.jht.spinbikeapps.targettraining.utils.WorkoutMode.Companion.toTTType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The target training type is stored as a bare int, so these mappings are a wire contract with the frame.
 * Changing a number silently changes which screen a console returns to.
 */
class WorkoutModeTest {

    @Test
    fun `target training type numbers are stable`() {
        assertEquals(0, WorkoutMode.Watts.toTTType())
        assertEquals(1, WorkoutMode.Rpm.toTTType())
        assertEquals(2, WorkoutMode.HeartRate.toTTType())
        assertEquals(3, WorkoutMode.Distance.toTTType())
        assertEquals(4, WorkoutMode.Calories.toTTType())
    }

    @Test
    fun `every mode survives a round trip through its type number`() {
        WorkoutMode.entries.forEach { mode ->
            assertEquals(mode, WorkoutMode.fromTTType(mode.toTTType()))
        }
    }

    @Test
    fun `an unknown type number maps to no mode`() {
        assertNull(WorkoutMode.fromTTType(-1))
        assertNull(WorkoutMode.fromTTType(5))
    }
}
