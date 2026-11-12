package com.jht.chimera.service.hal.lib.hardware.heartrate

import android.content.Context
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.service.hal.lib.salutron.SalutronDevice
import com.jht.datamatrixlib.DataMatrix
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Drives a real SalutronHeartRateReader (real HeartRateBlinker and timers) wired into a real
 * HeartRateManager, with the hardware layer mocked out: DataMatrix adapters are stubbed so no
 * binder/service is needed, and hardware init never runs because the detected console is not
 * a Vortex and no IO power-on event is delivered — so SalutronManager and the serial port are
 * never touched.
 *
 * The Salutron board's inputs are simulated through the reader's own IHeartRateCallback
 * (blink/stopBlink) and processSalutronHeartRate(), which is exactly how SalutronManager
 * delivers them on hardware.
 */
class SalutronHeartRateReaderTest {

    private val store = ConcurrentHashMap<String, Any>()
    private val publishedHeartRates = CopyOnWriteArrayList<Int>()
    private lateinit var manager: HeartRateManager
    private lateinit var reader: SalutronHeartRateReader

    @Before
    fun setup() {
        mockkConstructor(DataMatrix::class)
        every { anyConstructed<DataMatrix>().createIntAdapter(any(), any(), any()) } returns mockk(relaxed = true)
        every { anyConstructed<DataMatrix>().createEventAdapter(any(), any()) } returns mockk(relaxed = true)
        every { anyConstructed<DataMatrix>().createEventAdapter(any(), any(), any()) } returns mockk(relaxed = true)
        every {
            anyConstructed<DataMatrix>().createParcelableAdapter(
                any(),
                any<Class<SalutronDevice.SalutronDeviceParcelable>>(),
                any(),
            )
        } returns mockk(relaxed = true)
        every { anyConstructed<DataMatrix>().finish() } returns Unit

        mockkStatic(DataMatrix::class)
        every { DataMatrix.getBoolean(any()) } answers { store[firstArg<Any>().toString()] as? Boolean ?: false }
        every { DataMatrix.getBoolean(any(), any()) } answers { store[firstArg<Any>().toString()] as? Boolean ?: secondArg() }
        every { DataMatrix.getInt(any()) } answers { store[firstArg<Any>().toString()] as? Int ?: 0 }
        every { DataMatrix.putInt(any(), any()) } answers {
            val key = firstArg<Any>().toString()
            val value = secondArg<Int>()
            store[key] = value
            if (key == HALKeys.DATA_HEART_RATE_INT.toString()) publishedHeartRates.add(value)
        }
        every { DataMatrix.putBoolean(any(), any()) } answers { store[firstArg<Any>().toString()] = secondArg<Boolean>() }
        every { DataMatrix.putString(any(), any()) } answers { store[firstArg<Any>().toString()] = secondArg<String>() }
        every { DataMatrix.putBytes(any(), any()) } answers { store[firstArg<Any>().toString()] = secondArg<ByteArray?>() ?: ByteArray(0) }

        manager = newManager()
        reader = SalutronHeartRateReader(mockk<Context>(relaxed = true), manager)
        injectReader(manager, reader)
        publishedHeartRates.clear()
    }

    @After
    fun tearDown() {
        // stop the blink timer directly; reader.close() would touch the real SalutronManager
        reader.callback.stopBlink()
        unmockkAll()
        store.clear()
        publishedHeartRates.clear()
    }

    private fun newManager(): HeartRateManager {
        val ctor = HeartRateManager::class.java.getDeclaredConstructor().apply { isAccessible = true }
        return ctor.newInstance()
    }

    private fun injectReader(manager: HeartRateManager, reader: IHeartRateReader) {
        val readersField = HeartRateManager::class.java.getDeclaredField("readers").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (readersField.get(manager) as MutableList<IHeartRateReader>).add(reader)
    }

    private fun awaitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(20)
        }
        return condition()
    }

    @Test
    fun `grip blink is published while no device is connected and no heart rate exists yet`() {
        store[HALKeys.DATA_SALUTRON_CONNECTED_BOOL.toString()] = false

        // the salutron board detected handgrips (Contact_Stat hand-on) and asked for a
        // blink; there is no heart rate lock and no wireless device connected yet
        reader.callback.blink(true)

        // the reader's blank state toggles...
        assertTrue("blinker never toggled the blank state", awaitUntil(2000) { reader.blankState() })

        // ...and the blink must reach the display: the manager selects the blinking reader
        // and publishes the blank half of each cycle as -1
        assertTrue(
            "blink (-1) was never published, grip blink is being dropped",
            awaitUntil(3000) { publishedHeartRates.contains(-1) },
        )
    }

    @Test
    fun `reader goes back to inactive when the grip blink ends with nothing connected`() {
        store[HALKeys.DATA_SALUTRON_CONNECTED_BOOL.toString()] = false

        reader.callback.blink(true)
        assertTrue(awaitUntil(2000) { reader.blankState() })

        // hand off the grips: the board stops the blink and no device/heart rate remains,
        // so the reader must not stay selected and block other readers
        reader.callback.stopBlink()
        assertTrue(awaitUntil(1000) { manager.activeReader == null })
        assertFalse(reader.blankState())
    }

    @Test
    fun `blink is published as minus one while a salutron device is connected`() {
        store[HALKeys.DATA_SALUTRON_CONNECTED_BOOL.toString()] = true

        reader.callback.blink(false)

        assertTrue(
            "blink was never published as -1",
            awaitUntil(3000) { publishedHeartRates.contains(-1) },
        )
    }

    @Test
    fun `blink is published once a heart rate is received`() {
        store[HALKeys.DATA_SALUTRON_CONNECTED_BOOL.toString()] = false

        // a heart rate arriving starts the blink, and heartRate > 0 makes the reader active
        reader.processSalutronHeartRate(70)

        assertTrue("blank (-1) never published", awaitUntil(3000) { publishedHeartRates.contains(-1) })
        assertTrue("heart rate never published", awaitUntil(3000) { publishedHeartRates.contains(70) })
    }

    @Test
    fun `zero heart rate publishes zero and rearms the blinker`() {
        store[HALKeys.DATA_SALUTRON_CONNECTED_BOOL.toString()] = true
        reader.processSalutronHeartRate(0)

        assertEquals(0, reader.heartRate())
        assertTrue(publishedHeartRates.contains(0))
    }
}
