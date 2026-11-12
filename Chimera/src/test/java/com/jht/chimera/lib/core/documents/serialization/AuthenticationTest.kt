package com.jht.chimera.lib.core.documents.serialization

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticationTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `AuthRequest serializes correctly`() {
        val request = AuthRequest(secret = "test_secret")
        val expectedJson = """{"cmapi_secret":"test_secret"}"""

        val serialized = json.encodeToString(AuthRequest.serializer(), request)
        assertEquals(expectedJson, serialized)
    }

    @Test
    fun `AuthRequest deserializes correctly`() {
        val jsonString = """{"cmapi_secret":"test_secret"}"""
        val expected = AuthRequest(secret = "test_secret")

        val deserialized = json.decodeFromString(AuthRequest.serializer(), jsonString)
        assertEquals(expected, deserialized)
    }

    @Test
    fun `AuthResponse serializes correctly`() {
        val response = AuthResponse(token = "test_token")
        val expectedJson = """{"token":"test_token"}"""

        val serialized = json.encodeToString(AuthResponse.serializer(), response)
        assertEquals(expectedJson, serialized)
    }

    @Test
    fun `AuthResponse deserializes correctly`() {
        val jsonString = """{"token":"test_token"}"""
        val expected = AuthResponse(token = "test_token")

        val deserialized = json.decodeFromString(AuthResponse.serializer(), jsonString)
        assertEquals(expected, deserialized)
    }
}