package com.jht.chimera.service.hal.lib.hardware.heartrate

import com.jht.androidcommonalgorithms.timer.PeriodicTimer
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.HeartRateSource
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Drives a real VortexSalutron8600HeartRate (real HeartRateBlinker and polling timer) wired
 * into a real HeartRateManager, with the hardware mocked: ExecuteAsRoot is stubbed so no su
 * shell runs, and the two sysfs files (grip GPIO debug and heart rate) are replaced with
 * temp files the test rewrites to simulate gripping and heart rate readings.
 *
 * The reader's constructor can't open the real sysfs paths on the JVM, so the temp-file
 * handles are injected and the polling timer started via reflection.
 */
class VortexSalutron8600HeartRateTest {

    private val gripLoLine = "gpio-42 (heart-measure) in lo" // hands on the grips
    private val gripHiLine = "gpio-42 (heart-measure) in hi" // hands off

    private val store = ConcurrentHashMap<String, Any>()
    private val publishedHeartRates = CopyOnWriteArrayList<Int>()

    private lateinit var hrFile: File
    private lateinit var gripFile: File
    private lateinit var manager: HeartRateManager
    private lateinit var reader: VortexSalutron8600HeartRate

    @Before
    fun setup() {
        hrFile = File.createTempFile("vortex-hr", null)
        gripFile = File.createTempFile("vortex-grip", null)

        mockkStatic(ExecuteAsRoot::class)
        every { ExecuteAsRoot.chmod(any(), any()) } just Runs

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

        manager = newManager()
    }

    @After
    fun tearDown() {
        if (::reader.isInitialized) reader.close()
        unmockkAll()
        store.clear()
        publishedHeartRates.clear()
        hrFile.delete()
        gripFile.delete()
    }

    /** Construct the reader on the given simulated file contents and start it polling. */
    private fun startReader(gripLine: String, heartRate: String) {
        gripFile.writeText(gripLine + "\n")
        hrFile.writeText(heartRate + "\n")

        reader = VortexSalutron8600HeartRate(manager)
        setField("heartRateFile", RandomAccessFile(hrFile, "r"))
        setField("gripStatusFile", RandomAccessFile(gripFile, "r"))
        val timerField = VortexSalutron8600HeartRate::class.java.getDeclaredField("hrTimer").apply { isAccessible = true }
        (timerField.get(reader) as PeriodicTimer).start("VortexHRTest")

        injectReader(manager, reader)
        publishedHeartRates.clear()
    }

    private fun setField(name: String, value: Any) {
        VortexSalutron8600HeartRate::class.java.getDeclaredField(name).apply { isAccessible = true }.set(reader, value)
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
    fun `grip with no heart rate starts the blink and publishes it`() {
        // hands off first so the reader sees the grip line transition (module detection)
        startReader(gripHiLine, "0")
        assertTrue("reader never polled", awaitUntil(5000) { publishedHeartRates.isNotEmpty() })

        // hands on: still no heart rate, the display must blink
        gripFile.writeText(gripLoLine + "\n")

        assertTrue(
            "grip blink (-1) was never published",
            awaitUntil(8000) { publishedHeartRates.contains(-1) },
        )
        // the gripped reader is the active one even though no heart rate exists yet
        assertSame(reader, manager.activeReader)
        assertEquals(HeartRateSource.Grips.ordinal, store[HALKeys.DATA_HEART_RATE_SOURCE_INT.toString()])
    }

    @Test
    fun `heart rate reading publishes the value and stops any blink`() {
        startReader(gripHiLine, "72")

        assertTrue(
            "heart rate was never published",
            awaitUntil(5000) { publishedHeartRates.contains(72) },
        )
        assertEquals(72, reader.heartRate())
        assertFalse(reader.blankState())
        assertSame(reader, manager.activeReader)
    }

    @Test
    fun `no grip and no heart rate stays inactive and never blinks`() {
        startReader(gripHiLine, "0")

        assertTrue("expected polls to publish", awaitUntil(5000) { publishedHeartRates.size >= 2 })
        assertNull(manager.activeReader)
        assertFalse(publishedHeartRates.contains(-1))
        assertTrue(publishedHeartRates.all { it == 0 })
    }

    @Test
    fun `releasing the grips stops the blink and deactivates the reader`() {
        startReader(gripHiLine, "0")
        assertTrue("reader never polled", awaitUntil(5000) { publishedHeartRates.isNotEmpty() })

        gripFile.writeText(gripLoLine + "\n")
        assertTrue("grip blink never started", awaitUntil(8000) { publishedHeartRates.contains(-1) })

        // hands off again: blink stops and the reader releases its active slot
        gripFile.writeText(gripHiLine + "\n")
        assertTrue(
            "reader should go inactive after grip release",
            awaitUntil(8000) { manager.activeReader == null },
        )
        assertFalse(reader.blankState())
        assertEquals(0, store[HALKeys.DATA_HEART_RATE_INT.toString()])
    }
}
