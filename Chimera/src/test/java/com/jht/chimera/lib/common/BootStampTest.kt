package com.jht.chimera.lib.common

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * BootStamp guards once-per-version boot work in StartupLogic. The contract that matters:
 * a stamp is current only when its stored signature matches exactly, updating overwrites
 * the previous signature, and any IO problem fails open (reads as "not current") so the
 * guarded work runs instead of being silently skipped.
 */
class BootStampTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var stampDir: File
    private lateinit var stamp: BootStamp

    @Before
    fun setUp() {
        // Android Log is not available on the JVM unit test classpath.
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0

        // Not created up front: the class is responsible for creating its own directory.
        stampDir = File(tmp.root, "boot_stamps")
        stamp = BootStamp(stampDir)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ---- isCurrent -------------------------------------------------------------------

    @Test
    fun `isCurrent is false when the stamp directory does not exist`() {
        assertFalse(stampDir.exists())

        assertFalse(stamp.isCurrent(NAME, SIG_A))
    }

    @Test
    fun `isCurrent is false when the directory exists but the stamp file does not`() {
        assertTrue(stampDir.mkdirs())

        assertFalse(stamp.isCurrent(NAME, SIG_A))
    }

    @Test
    fun `isCurrent is false when the stamp path is a directory rather than a file`() {
        assertTrue(File(stampDir, "$NAME.stamp").mkdirs())

        assertFalse(stamp.isCurrent(NAME, SIG_A))
    }

    @Test
    fun `isCurrent is true after update with the same signature`() {
        stamp.update(NAME, SIG_A)

        assertTrue(stamp.isCurrent(NAME, SIG_A))
    }

    @Test
    fun `isCurrent is false after update with a different signature`() {
        stamp.update(NAME, SIG_A)

        assertFalse(stamp.isCurrent(NAME, SIG_B))
    }

    @Test
    fun `signature comparison is exact`() {
        stamp.update(NAME, SIG_A)

        assertFalse(stamp.isCurrent(NAME, SIG_A.uppercase()))
        assertFalse(stamp.isCurrent(NAME, "$SIG_A "))
        assertFalse(stamp.isCurrent(NAME, "$SIG_A\n"))
        assertFalse(stamp.isCurrent(NAME, ""))
    }

    @Test
    fun `isCurrent survives across BootStamp instances pointed at the same directory`() {
        BootStamp(stampDir).update(NAME, SIG_A)

        // A fresh instance, as StartupLogic creates on every boot.
        assertTrue(BootStamp(stampDir).isCurrent(NAME, SIG_A))
    }

    // ---- update ----------------------------------------------------------------------

    @Test
    fun `update creates the stamp directory and file`() {
        stamp.update(NAME, SIG_A)

        val file = File(stampDir, "$NAME.stamp")
        assertTrue(file.isFile)
        assertEquals(SIG_A, file.readText())
    }

    @Test
    fun `update creates nested parent directories`() {
        val nested = File(tmp.root, "a/b/c/boot_stamps")
        val deep = BootStamp(nested)

        deep.update(NAME, SIG_A)

        assertTrue(deep.isCurrent(NAME, SIG_A))
    }

    @Test
    fun `update overwrites a previous signature`() {
        stamp.update(NAME, SIG_A)
        stamp.update(NAME, SIG_B)

        assertFalse(stamp.isCurrent(NAME, SIG_A))
        assertTrue(stamp.isCurrent(NAME, SIG_B))
        assertEquals(SIG_B, File(stampDir, "$NAME.stamp").readText())
    }

    @Test
    fun `update with a shorter signature does not leave trailing bytes from the old one`() {
        stamp.update(NAME, "a-much-longer-signature-value")
        stamp.update(NAME, "short")

        assertTrue(stamp.isCurrent(NAME, "short"))
    }

    @Test
    fun `stamps with different names are independent`() {
        stamp.update("root_config", SIG_A)
        stamp.update("permissions", SIG_B)

        assertTrue(stamp.isCurrent("root_config", SIG_A))
        assertTrue(stamp.isCurrent("permissions", SIG_B))
        assertFalse(stamp.isCurrent("root_config", SIG_B))
        assertFalse(stamp.isCurrent("permissions", SIG_A))
    }

    @Test
    fun `realistic fingerprint plus build time plus version signature round trips`() {
        val signature =
            "JHT/vortex/vortex:11/RQ3A.211001.001/eng.build.20260901.123456:userdebug/test-keys|1756728896000|4.12.0.7"

        stamp.update(NAME, signature)

        assertTrue(stamp.isCurrent(NAME, signature))
    }

    @Test
    fun `signature containing newlines and unicode round trips`() {
        val signature = "line1\nline2\r\n|ünïcødé|\ttab"

        stamp.update(NAME, signature)

        assertTrue(stamp.isCurrent(NAME, signature))
    }

    @Test
    fun `empty signature round trips`() {
        stamp.update(NAME, "")

        assertTrue(stamp.isCurrent(NAME, ""))
        assertFalse(stamp.isCurrent(NAME, SIG_A))
    }

    // ---- field override --------------------------------------------------------------

    @Test
    fun `deleting the stamp file forces a re-run`() {
        stamp.update(NAME, SIG_A)
        assertTrue(stamp.isCurrent(NAME, SIG_A))

        assertTrue(File(stampDir, "$NAME.stamp").delete())

        assertFalse(stamp.isCurrent(NAME, SIG_A))
    }

    // ---- fail-open behaviour ---------------------------------------------------------

    @Test
    fun `update does not throw when the stamp directory cannot be created`() {
        // A regular file where the directory should be: mkdirs() fails and writeText() throws.
        val blocker = File(tmp.root, "blocked")
        assertTrue(blocker.createNewFile())
        val broken = BootStamp(blocker)

        broken.update(NAME, SIG_A)

        assertTrue("blocker should still be a plain file", blocker.isFile)
        verify(exactly = 1) { Log.w("BootStamp", "update failed for $NAME", any()) }
    }

    @Test
    fun `isCurrent is false when the stamp directory is a regular file`() {
        val blocker = File(tmp.root, "blocked")
        assertTrue(blocker.createNewFile())

        assertFalse(BootStamp(blocker).isCurrent(NAME, SIG_A))
    }

    @Test
    fun `isCurrent is false and logs when the stamp file cannot be read`() {
        stamp.update(NAME, SIG_A)
        val file = File(stampDir, "$NAME.stamp")
        // Permission changes are not enforced for root (e.g. some CI containers), so skip there.
        assumeTrue(file.setReadable(false) && !file.canRead())

        try {
            assertFalse(stamp.isCurrent(NAME, SIG_A))
            verify(exactly = 1) { Log.w("BootStamp", "isCurrent failed for $NAME", any()) }
        } finally {
            file.setReadable(true)
        }
    }

    @Test
    fun `isCurrent does not log on the normal not-current path`() {
        stamp.update(NAME, SIG_A)

        assertFalse(stamp.isCurrent(NAME, SIG_B))
        assertFalse(stamp.isCurrent("other", SIG_A))

        verify(exactly = 0) { Log.w(any(), any<String>(), any()) }
    }

    private companion object {
        const val NAME = "root_config"
        const val SIG_A = "fingerprint-a|1.0.0"
        const val SIG_B = "fingerprint-b|1.0.1"
    }
}
