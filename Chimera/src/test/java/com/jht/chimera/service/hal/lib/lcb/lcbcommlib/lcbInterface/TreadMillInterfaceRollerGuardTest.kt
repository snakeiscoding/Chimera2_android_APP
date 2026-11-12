package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Treadmill.ITreadMillHandlerCallback
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Treadmill.TreadMillHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Roller guard trigger/reset edge logic in [TreadMillInterface].
 *
 * The e-stop, roller guard, and IR sensor share one safety circuit; status byte bit4 reports
 * that circuit and falls on EVERY reset. [TreadMillInterface] must therefore only report a
 * roller guard release (via ILCBCallback.onTmRollerGuard(false)) when a roller guard trigger
 * (0x02C4) is actually in progress — that is the whole job of the wasRollerGuardTriggered flag.
 *
 * The real TreadMillHandler singleton is replaced so no comm stack is needed; the handler
 * callback registered by the constructor is captured and driven directly, exactly as the
 * LCB comm layer would.
 */
class TreadMillInterfaceRollerGuardTest {

    private lateinit var serviceCallback: ILCBCallback
    private lateinit var tmHandler: TreadMillHandler
    private lateinit var handlerCallback: ITreadMillHandlerCallback
    private lateinit var treadmill: TreadMillInterface

    @Before
    fun setUp() {
        serviceCallback = mockk(relaxed = true)
        tmHandler = mockk(relaxed = true)
        mockkStatic(TreadMillHandler::class)
        every { TreadMillHandler.getInstance() } returns tmHandler

        val callbackSlot = slot<ITreadMillHandlerCallback>()
        every { tmHandler.setTreadMillHandlerCallback(capture(callbackSlot)) } answers { }

        treadmill = TreadMillInterface(serviceCallback)
        handlerCallback = callbackSlot.captured
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `bit4 falling edge without a roller guard in progress does not release`() {
        // A plain e-stop reset also drops bit4 — it must not be reported as a roller guard reset
        handlerCallback.onTreadMillServiceSwitch(false)

        verify(exactly = 0) { serviceCallback.onTmRollerGuard(any()) }
    }

    @Test
    fun `roller guard trigger sets the flag`() {
        treadmill.triggerRollerGuard()

        assertTrue(treadmill.isRollerGuardTriggered)
    }

    @Test
    fun `bit4 falling edge after a roller guard releases exactly once and clears the flag`() {
        treadmill.triggerRollerGuard()

        handlerCallback.onTreadMillServiceSwitch(false)

        verify(exactly = 1) { serviceCallback.onTmRollerGuard(false) }
        assertFalse(treadmill.isRollerGuardTriggered)
    }

    @Test
    fun `second bit4 falling edge after release does not release again`() {
        treadmill.triggerRollerGuard()
        handlerCallback.onTreadMillServiceSwitch(false)

        handlerCallback.onTreadMillServiceSwitch(false)

        verify(exactly = 1) { serviceCallback.onTmRollerGuard(false) }
    }

    @Test
    fun `repeated 02C4 triggers release exactly once on a single reset`() {
        // The LCB repeats 0x02C4 about every 5s while the guard is held
        treadmill.triggerRollerGuard()
        treadmill.triggerRollerGuard()
        treadmill.triggerRollerGuard()

        handlerCallback.onTreadMillServiceSwitch(false)

        verify(exactly = 1) { serviceCallback.onTmRollerGuard(false) }
        assertFalse(treadmill.isRollerGuardTriggered)
    }

    @Test
    fun `bit4 rising edge never releases and keeps the flag`() {
        treadmill.triggerRollerGuard()

        handlerCallback.onTreadMillServiceSwitch(true)

        verify(exactly = 0) { serviceCallback.onTmRollerGuard(any()) }
        assertTrue(treadmill.isRollerGuardTriggered)
    }

    @Test
    fun `roller guard can re-trigger after a completed reset cycle`() {
        treadmill.triggerRollerGuard()
        handlerCallback.onTreadMillServiceSwitch(false)

        treadmill.triggerRollerGuard()
        handlerCallback.onTreadMillServiceSwitch(false)

        verify(exactly = 2) { serviceCallback.onTmRollerGuard(false) }
        assertFalse(treadmill.isRollerGuardTriggered)
    }
}
