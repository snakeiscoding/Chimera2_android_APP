package com.jht.interfaces.models

import com.jht.chimera.lib.common.dapi.DapiLogin
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.PTPKeys
import com.jht.chimera.lib.ptp.LoadedPtpAssignment
import com.jht.datamatrixlib.DMCallback
import com.jht.datamatrixlib.DataMatrix
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * A PTP/PACE assignment UUID must not outlive the user it belongs to. PtpNative arms
 * [PTPKeys.DATA_PTP_LOADED_ASSIGNMENT_PARCELABLE] on preload, so browsing a list is enough, and it
 * used to be cleared only at workout completion - the next user submitted against it.
 */
class MatrixEngage360SystemModelUserChangeTest {

    private val store = mutableMapOf<Any, String>()
    private var loaded: LoadedPtpAssignment? = null
    private val boolCallbacks = mutableMapOf<Any, DMCallback<Boolean>>()
    private val eventCallbacks = mutableMapOf<Any, DMCallback<String>>()

    private lateinit var model: MatrixEngage360SystemModel

    @Before
    fun setUp() {
        mockkStatic(DataMatrix::class)
        mockkConstructor(DataMatrix::class)

        // A real store rather than a fixed return, or an assertion cannot tell which key was read.
        every { DataMatrix.getString(any()) } answers { store[firstArg()] ?: "" }
        every { DataMatrix.putString(any(), any()) } answers {
            store[firstArg()] = secondArg()
            Unit
        }
        every { DataMatrix.getBoolean(any()) } returns false
        every {
            DataMatrix.getParcelable(PTPKeys.DATA_PTP_LOADED_ASSIGNMENT_PARCELABLE, LoadedPtpAssignment::class.java)
        } answers { loaded }
        every { DataMatrix.putParcelable(PTPKeys.DATA_PTP_LOADED_ASSIGNMENT_PARCELABLE, any()) } answers {
            loaded = secondArg<LoadedPtpAssignment?>()
            Unit
        }

        // Hold onto the registered callbacks so the test can drive them as DataMatrix would.
        every { anyConstructed<DataMatrix>().createBoolAdapter(any(), any(), any()) } answers {
            boolCallbacks[firstArg()] = secondArg()
            mockk(relaxed = true)
        }
        every { anyConstructed<DataMatrix>().createEventAdapter(any(), any(), any()) } answers {
            eventCallbacks[firstArg()] = secondArg()
            mockk(relaxed = true)
        }

        store[DAPIKeys.DATA_USER_DAPI_UUID_STRING] = USER_A
        model = MatrixEngage360SystemModel()
        model.setUserChangedCallback { }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun loadedExerciseId() = loaded?.exerciseId

    private fun switchTo(uuid: String) {
        store[DAPIKeys.DATA_USER_DAPI_UUID_STRING] = uuid
    }

    private fun ownerUuid() = loaded?.ownerUuid

    private fun fireLogout() =
        eventCallbacks.getValue(DapiLogin.Keys.ACTION_LOGOUT).dataMatrixCallback("")

    private fun fireLoginFinished() =
        boolCallbacks.getValue(DAPIKeys.DATA_GETTING_USER_INFO_BOOL).dataMatrixCallback(false)

    @Test
    fun `the owner is the uuid PtpNative supplies, not whoever is logged in`() {
        // A stale assignment list re-preloading under user B must stay attributed to user A.
        switchTo(USER_B)

        model.onExerciseLoaded(ASSIGNMENT, USER_A)

        assertEquals(USER_A, ownerUuid())
    }

    @Test
    fun `logging out clears the exercise before the switch to guest lands`() {
        // ACTION_LOGOUT is what triggers setUserToGuest, so the uuid still reads as the outgoing
        // user when this fires. A compare-and-clear would see owner == current and do nothing.
        model.onExerciseLoaded(ASSIGNMENT, USER_A)

        fireLogout()

        assertNull(loadedExerciseId())
    }

    @Test
    fun `splash reset does not clear - the idle timeout is covered by the logout it triggers`() {
        // Clearing on this event lands before the aborted workout's cardio-complete reads the keys,
        // costing a real PTP workout its assignment. SplashScreenLogic.resetDefaults() logs out.
        model.onExerciseLoaded(ASSIGNMENT, USER_A)

        assertFalse(eventCallbacks.containsKey(HALKeys.ACTION_SPLASH_RESET))
        assertEquals(ASSIGNMENT, loadedExerciseId())
    }

    @Test
    fun `a new user finishing login clears the previous user's exercise`() {
        model.onExerciseLoaded(ASSIGNMENT, USER_A)
        switchTo(USER_B)

        fireLoginFinished()

        assertNull(loadedExerciseId())
    }

    @Test
    fun `the owner's own exercise survives a repeated user-info fetch`() {
        // GETTING_USER_INFO drops twice per login, so an unconditional clear would lose this.
        model.onExerciseLoaded(ASSIGNMENT, USER_A)

        fireLoginFinished()
        fireLoginFinished()

        assertEquals(ASSIGNMENT, loadedExerciseId())
    }

    @Test
    fun `a login edge after the owner already left clears both keys`() {
        model.onExerciseLoaded(ASSIGNMENT, USER_A)
        switchTo(USER_B)

        fireLoginFinished()

        assertNull(loadedExerciseId())
        assertNull(ownerUuid())
    }

    private companion object {
        const val ASSIGNMENT = "ml:15774e10-d820-46db-8d3a-00e86a5bb01b:5b4b3756-f067-4486-be72-b1a89540f42b"
        const val USER_A = "10f9bd24-5353-4c16-bafd-7ea4af89496b"
        const val USER_B = "fa2f2e1a-9aba-4d32-babb-6bb2dfe79742"
    }
}
