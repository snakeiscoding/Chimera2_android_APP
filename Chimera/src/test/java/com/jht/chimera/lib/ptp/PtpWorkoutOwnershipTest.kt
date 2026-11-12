package com.jht.chimera.lib.ptp

import com.jht.chimera.lib.common.dapi.DapiUser
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.PTPKeys
import com.jht.datamatrixlib.DataMatrix
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PtpWorkoutOwnershipTest {

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** Pins which key feeds which parameter; the pure function alone cannot catch a wrong key. */
    @Test
    fun `reads the assignment from the loaded key and the user from the uuid key`() {
        var currentUser = USER_A
        mockkStatic(DataMatrix::class)
        every {
            DataMatrix.getParcelable(PTPKeys.DATA_PTP_LOADED_ASSIGNMENT_PARCELABLE, LoadedPtpAssignment::class.java)
        } returns LoadedPtpAssignment(ASSIGNMENT, USER_A)
        every { DataMatrix.getString(DAPIKeys.DATA_USER_DAPI_UUID_STRING) } answers { currentUser }

        assertEquals(ASSIGNMENT, PtpWorkoutOwnership.resolveLoadedAssignment())

        currentUser = USER_B
        assertNull(PtpWorkoutOwnership.resolveLoadedAssignment())
    }

    @Test
    fun `submits the assignment when the owner still holds the console`() {
        assertEquals(ASSIGNMENT, resolve(ASSIGNMENT, USER_A, USER_A))
    }

    @Test
    fun `drops the assignment when another user is now on the console`() {
        assertNull(resolve(ASSIGNMENT, USER_A, USER_B))
    }

    @Test
    fun `drops the assignment when no owner was recorded`() {
        assertNull(resolve(ASSIGNMENT, "", USER_A))
    }

    /** Every guest shares one uuid, so guest-to-guest would otherwise match and submit. */
    @Test
    fun `drops the assignment when the owner is guest`() {
        assertNull(resolve(ASSIGNMENT, DapiUser.GUEST_USER_UUID, DapiUser.GUEST_USER_UUID))
    }

    /** The common case: nothing was ever loaded, so this must not look suspicious. */
    @Test
    fun `passes through when no assignment is loaded`() {
        assertNull(PtpWorkoutOwnership.resolveProgramUuid(null, USER_A))
        assertNull(resolve(NIL_UUID, "", USER_A))
        assertNull(resolve("", USER_A, USER_A))
    }

    private fun resolve(exerciseId: String, owner: String, currentUser: String?) =
        PtpWorkoutOwnership.resolveProgramUuid(LoadedPtpAssignment(exerciseId, owner), currentUser)

    private companion object {
        // An all-zeros exercise id from older builds must still count as nothing loaded.
        const val NIL_UUID = "00000000-0000-0000-0000-000000000000"
        const val ASSIGNMENT = "ml:15774e10-d820-46db-8d3a-00e86a5bb01b:5b4b3756-f067-4486-be72-b1a89540f42b"
        const val USER_A = "10f9bd24-5353-4c16-bafd-7ea4af89496b"
        const val USER_B = "fa2f2e1a-9aba-4d32-babb-6bb2dfe79742"
    }
}
