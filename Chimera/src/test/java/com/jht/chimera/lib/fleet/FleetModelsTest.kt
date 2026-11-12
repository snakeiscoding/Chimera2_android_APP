package com.jht.chimera.lib.fleet

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FleetModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `Apple envelope deserializes with passTypeId`() {
        val input = """
            {
                "ephemeralPublicKey": "AAAA",
                "ciphertext": "BBBB",
                "authTag": "CCCC",
                "passTypeId": "pass.com.jht.vas"
            }
        """.trimIndent()

        val envelope = json.decodeFromString<EciesEnvelope>(input)
        assertEquals("pass.com.jht.vas", envelope.passTypeId)
        assertNull(envelope.collectorId)
        assertNull(envelope.keyVersion)
        assertNull(envelope.passTypeFilter)
    }

    @Test
    fun `Google envelope deserializes with collectorId, keyVersion, passTypeFilter`() {
        val input = """
            {
                "ephemeralPublicKey": "AAAA",
                "ciphertext": "BBBB",
                "authTag": "CCCC",
                "collectorId": 107071,
                "keyVersion": 1,
                "passTypeFilter": 0
            }
        """.trimIndent()

        val envelope = json.decodeFromString<EciesEnvelope>(input)
        assertNull(envelope.passTypeId)
        assertEquals(107071L, envelope.collectorId)
        assertEquals(1, envelope.keyVersion)
        assertEquals(0, envelope.passTypeFilter)
    }

    @Test
    fun `Envelope without wallet config fields deserializes with nulls`() {
        val input = """
            {
                "ephemeralPublicKey": "AAAA",
                "ciphertext": "BBBB",
                "authTag": "CCCC"
            }
        """.trimIndent()

        val envelope = json.decodeFromString<EciesEnvelope>(input)
        assertNull(envelope.passTypeId)
        assertNull(envelope.collectorId)
        assertNull(envelope.keyVersion)
        assertNull(envelope.passTypeFilter)
    }

    @Test
    fun `WalletKeysResponse deserializes full response with both slots`() {
        val input = """
            {
                "apple": {
                    "ephemeralPublicKey": "AAAA",
                    "ciphertext": "BBBB",
                    "authTag": "CCCC",
                    "passTypeId": "pass.com.jht.vas"
                },
                "google": {
                    "ephemeralPublicKey": "DDDD",
                    "ciphertext": "EEEE",
                    "authTag": "FFFF",
                    "collectorId": 107071,
                    "keyVersion": 1,
                    "passTypeFilter": 0
                }
            }
        """.trimIndent()

        val response = json.decodeFromString<WalletKeysResponse>(input)

        assertEquals("pass.com.jht.vas", response.apple?.passTypeId)
        assertNull(response.apple?.collectorId)

        assertEquals(107071L, response.google?.collectorId)
        assertEquals(1, response.google?.keyVersion)
        assertEquals(0, response.google?.passTypeFilter)
        assertNull(response.google?.passTypeId)
    }

    @Test
    fun `WalletKeysResponse handles missing slots`() {
        val input = """
            {
                "apple": {
                    "ephemeralPublicKey": "AAAA",
                    "ciphertext": "BBBB",
                    "authTag": "CCCC",
                    "passTypeId": "pass.com.jht.vas"
                }
            }
        """.trimIndent()

        val response = json.decodeFromString<WalletKeysResponse>(input)
        assertEquals("pass.com.jht.vas", response.apple?.passTypeId)
        assertNull(response.google)
    }
}
