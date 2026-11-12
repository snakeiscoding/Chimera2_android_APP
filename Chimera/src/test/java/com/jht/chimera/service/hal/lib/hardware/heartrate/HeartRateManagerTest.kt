package com.jht.chimera.service.hal.lib.hardware.heartrate

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.HeartRateSource
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Verifies HeartRateManager.heartRateUpdated() publishing rules with fake readers:
 * a reader's blankState (the blink) only reaches DATA_HEART_RATE_INT while that reader
 * is returned by getActiveReader().
 */
class HeartRateManagerTest {

    private class FakeReader(
        @Volatile var connected: Boolean = false,
        @Volatile var rate: Int = 0,
        @Volatile var blank: Boolean = false,
        private val source: HeartRateSource = HeartRateSource.BLE,
    ) : IHeartRateReader {
        override fun blankState() = blank
        override fun heartRate() = rate
        override fun getSource() = source
        override fun hasConnection() = connected
        override fun close() {}
    }

    private val store = ConcurrentHashMap<String, Any>()
    private val publishedHeartRates = CopyOnWriteArrayList<Int>()

    @Before
    fun setup() {
        mockkConstructor(DataMatrix::class)
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
    }

    @After
    fun tearDown() {
        unmockkAll()
        store.clear()
        publishedHeartRates.clear()
    }

    /** Builds a manager without start() so no real hardware readers are constructed. */
    private fun newManager(vararg fakes: IHeartRateReader): HeartRateManager {
        val ctor = HeartRateManager::class.java.getDeclaredConstructor().apply { isAccessible = true }
        val manager = ctor.newInstance()
        val readersField = HeartRateManager::class.java.getDeclaredField("readers").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (readersField.get(manager) as MutableList<IHeartRateReader>).addAll(fakes)
        return manager
    }

    private fun publishedValue() = store[HALKeys.DATA_HEART_RATE_INT.toString()] as? Int
    private fun publishedSource() = store[HALKeys.DATA_HEART_RATE_SOURCE_INT.toString()] as? Int

    @Test
    fun `connected reader with blank state publishes minus one`() {
        val reader = FakeReader(connected = true, rate = 0, blank = true)
        newManager(reader).heartRateUpdated()
        assertEquals(-1, publishedValue())
    }

    @Test
    fun `connected reader without blank state publishes its heart rate`() {
        val reader = FakeReader(connected = true, rate = 88, blank = false)
        newManager(reader).heartRateUpdated()
        assertEquals(88, publishedValue())
    }

    @Test
    fun `reader with a heart rate but no connection is still selected`() {
        val reader = FakeReader(connected = false, rate = 65, blank = false)
        val manager = newManager(reader)
        assertSame(reader, manager.getActiveReader())
        manager.heartRateUpdated()
        assertEquals(65, publishedValue())
    }

    @Test
    fun `reader reporting no connection and no heart rate is not selected`() {
        // the manager only publishes for the active reader — so a reader that wants its
        // blink displayed must report a connection while blinking, the way
        // VortexSalutron8600HeartRate does with its grip state
        val reader = FakeReader(connected = false, rate = 0, blank = true)
        val manager = newManager(reader)

        assertNull(manager.getActiveReader())
        manager.heartRateUpdated()

        assertEquals(0, publishedValue())
        assertEquals(HeartRateSource.None.ordinal, publishedSource())
        assertFalse(publishedHeartRates.contains(-1))
    }

    @Test
    fun `no readers publishes zero with source none`() {
        newManager().heartRateUpdated()
        assertEquals(0, publishedValue())
        assertEquals(HeartRateSource.None.ordinal, publishedSource())
    }
}
