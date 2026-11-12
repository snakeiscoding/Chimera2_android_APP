package com.jht.chimera.lib.core.documents.serialization

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TvChannelDocumentsSerializerTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `TvChannelDocumentsSerializer serializes correctly`() {
        val tvSource = SettingKeys.TVSource.entries.first() // Using first enum entry for testing
        val document = CoreDocument(url = "https://example.com/channel")
        val map = mapOf(tvSource to document)

        val expectedJson = buildJsonObject {
            put(tvSource.name.lowercase(), Json.encodeToJsonElement(CoreDocument.serializer(), document))
        }.toString()

        val serialized = json.encodeToString(TvChannelDocumentsSerializer, map)
        assertEquals(expectedJson, serialized)
    }

    @Test
    fun `TvChannelDocumentsSerializer deserializes correctly`() {
        val tvSource = SettingKeys.TVSource.entries.first()
        val jsonString = """{"${tvSource.name.lowercase()}":{"url":"https://example.com/channel"}}"""

        val expected = mapOf(tvSource to CoreDocument(url = "https://example.com/channel"))
        val deserialized = json.decodeFromString(TvChannelDocumentsSerializer, jsonString)

        assertEquals(expected, deserialized)
    }

    @Test
    fun `TvChannelDocumentsSerializer skips invalid entries`() {
        val tvSource = SettingKeys.TVSource.entries.first()
        val jsonString = """{
            "${tvSource.name.lowercase()}": {"url": "https://example.com/channel"},
            "invalid_source": {"url": "https://example.com/invalid"}
        }"""

        val expected = mapOf(tvSource to CoreDocument(url = "https://example.com/channel"))
        val deserialized = json.decodeFromString(TvChannelDocumentsSerializer, jsonString)

        assertEquals(expected, deserialized)
    }
}