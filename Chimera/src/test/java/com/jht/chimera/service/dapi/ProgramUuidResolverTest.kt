package com.jht.chimera.service.dapi

import com.jht.chimera.lib.ptp.PtpWorkoutOwnership
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ProgramUuidResolverTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `sends the owned PTP assignment without generating a uuid`() {
        ownedAssignment(ASSIGNMENT)
        assertEquals(ASSIGNMENT, ProgramUuidResolver.resolve(newUuid = { error("must not generate") }))
    }

    @Test
    fun `prefixes the generated uuid with manual when there is no owned assignment`() {
        ownedAssignment(null)
        assertEquals(
            ProgramUuidResolver.MANUAL_UUID_PREFIX + FIXED_UUID,
            ProgramUuidResolver.resolve(newUuid = { UUID.fromString(FIXED_UUID) }),
        )
    }

    @Test
    fun `generates a well-formed random uuid by default`() {
        ownedAssignment(null)
        val programUuid = ProgramUuidResolver.resolve()
        assertTrue(programUuid, programUuid.startsWith(ProgramUuidResolver.MANUAL_UUID_PREFIX))
        UUID.fromString(programUuid.removePrefix(ProgramUuidResolver.MANUAL_UUID_PREFIX))
    }

    @Test
    fun `never reuses a manual uuid across workouts`() {
        ownedAssignment(null)
        assertNotEquals(ProgramUuidResolver.resolve(), ProgramUuidResolver.resolve())
    }

    private fun ownedAssignment(exerciseId: String?) {
        mockkObject(PtpWorkoutOwnership)
        every { PtpWorkoutOwnership.resolveLoadedAssignment() } returns exerciseId
    }

    private companion object {
        const val ASSIGNMENT = "ml:15774e10-d820-46db-8d3a-00e86a5bb01b:5b4b3756-f067-4486-be72-b1a89540f42b"
        const val FIXED_UUID = "fa2f2e1a-9aba-4d32-babb-6bb2dfe79742"
    }
}
