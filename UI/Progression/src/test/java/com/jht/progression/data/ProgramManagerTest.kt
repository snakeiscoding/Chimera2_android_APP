package com.jht.progression.data

import com.jht.enums.ProgramType
import com.jht.progression.data.program.program10k.Program10k
import com.jht.progression.data.program.program5k.Program5k
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramManagerTest {
    @Test
    fun `programs currently exposes only the 5k program`() {
        val programs = ProgramManager().programs
        assertEquals(1, programs.size)
        assertEquals(ProgramType.Program5k, programs.first().programType)
    }

    @Test
    fun `getProgramOfType returns a program matching the requested type`() {
        assertEquals(ProgramType.Program5k, getProgramOfType(ProgramType.Program5k).programType)
        assertEquals(ProgramType.Program10k, getProgramOfType(ProgramType.Program10k).programType)
    }

    @Test
    fun `getProgramOfType returns the 5k workout interface for the 5k program`() {
        assertTrue(getProgramOfType(ProgramType.Program5k).workoutInterface is Program5k)
    }

    @Test
    fun `getProgramOfType returns the 10k workout interface for the 10k program`() {
        assertTrue(getProgramOfType(ProgramType.Program10k).workoutInterface is Program10k)
    }
}
