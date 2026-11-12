package com.jht.chimera.lib.fleet

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SgmBridgeTest {

    @Test
    fun `buildWalletConfig for Google encodes three 4-byte big-endian ints`() {
        val envelope = EciesEnvelope(
            ephemeralPublicKey = "AAAA",
            ciphertext = "BBBB",
            authTag = "CCCC",
            collectorId = 107071,
            keyVersion = 1,
            passTypeFilter = 0
        )

        val result = SgmBridge.buildWalletConfig(envelope, WalletSlot.GOOGLE)

        assertEquals(12, result.size)
        val buf = ByteBuffer.wrap(result).order(ByteOrder.BIG_ENDIAN)
        assertEquals(107071, buf.int)
        assertEquals(1, buf.int)
        assertEquals(0, buf.int)
    }

    @Test
    fun `buildWalletConfig for Google defaults passTypeFilter to 0 when null`() {
        val envelope = EciesEnvelope(
            ephemeralPublicKey = "AAAA",
            ciphertext = "BBBB",
            authTag = "CCCC",
            collectorId = 42,
            keyVersion = 3,
            passTypeFilter = null
        )

        val result = SgmBridge.buildWalletConfig(envelope, WalletSlot.GOOGLE)
        val buf = ByteBuffer.wrap(result).order(ByteOrder.BIG_ENDIAN)
        assertEquals(42, buf.int)
        assertEquals(3, buf.int)
        assertEquals(0, buf.int)
    }

    @Test(expected = IllegalStateException::class)
    fun `buildWalletConfig for Apple throws when passTypeId is missing`() {
        val envelope = EciesEnvelope(
            ephemeralPublicKey = "AAAA",
            ciphertext = "BBBB",
            authTag = "CCCC"
        )
        SgmBridge.buildWalletConfig(envelope, WalletSlot.APPLE)
    }

    @Test(expected = IllegalStateException::class)
    fun `buildWalletConfig for Google throws when collectorId is missing`() {
        val envelope = EciesEnvelope(
            ephemeralPublicKey = "AAAA",
            ciphertext = "BBBB",
            authTag = "CCCC"
        )
        SgmBridge.buildWalletConfig(envelope, WalletSlot.GOOGLE)
    }

}
