package com.jht.chimera.lib.core.documents.serialization

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreDocumentsTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `CoreDocuments serializes correctly`() {
        val tvSource = SettingKeys.TVSource.entries.first()
        val document = CoreDocument(url = "https://example.com/channel")
        val coreDocuments = CoreDocuments(tvChannelDocuments = mapOf(tvSource to document))

        val expectedJson = """{"tv_channels":{"${tvSource.name.lowercase()}":{"url":"https://example.com/channel"}}}"""
        val serialized = json.encodeToString(CoreDocuments.serializer(), coreDocuments)

        assertEquals(expectedJson, serialized)
    }

    @Test
    fun `CoreDocuments deserializes correctly`() {
        val tvSource = SettingKeys.TVSource.entries.first()
        val jsonString = """{"tv_channels":{"${tvSource.name.lowercase()}":{"url":"https://example.com/channel"}}}"""

        val expected = CoreDocuments(tvChannelDocuments = mapOf(tvSource to CoreDocument(url = "https://example.com/channel")))
        val deserialized = json.decodeFromString(CoreDocuments.serializer(), jsonString)

        assertEquals(expected, deserialized)
    }

    @Test
    fun `CoreDocuments handles empty tvChannelDocuments`() {
        val jsonString = """{"tv_channels":{}}"""
        val expected = CoreDocuments(tvChannelDocuments = emptyMap())

        val deserialized = json.decodeFromString(CoreDocuments.serializer(), jsonString)
        assertEquals(expected, deserialized)
    }

    @Test
    fun `CoreDocument serializes correctly`() {
        val document = CoreDocument(url = "https://example.com/channel")
        val expectedJson = """{"url":"https://example.com/channel"}"""

        val serialized = json.encodeToString(CoreDocument.serializer(), document)
        assertEquals(expectedJson, serialized)
    }

    @Test
    fun `CoreDocument deserializes correctly`() {
        val jsonString = """{"url":"https://example.com/channel"}"""
        val expected = CoreDocument(url = "https://example.com/channel")

        val deserialized = json.decodeFromString(CoreDocument.serializer(), jsonString)
        assertEquals(expected, deserialized)
    }

    @Test
    fun `CoreDocumentMetadata serializes correctly`() {
        val metadata = CoreDocumentMetadata(
            url = "https://example.com/document",
            contentType = "application/pdf",
            contentLength = 1024L,
            lastModified = "Wed, 21 Oct 2023 07:28:00 GMT",
            etag = "abc123"
        )

        val expectedJson = """{"url":"https://example.com/document","contentType":"application/pdf","contentLength":1024,"lastModified":"Wed, 21 Oct 2023 07:28:00 GMT","etag":"abc123"}"""
        val serialized = json.encodeToString(CoreDocumentMetadata.serializer(), metadata)

        assertEquals(expectedJson, serialized)
    }

    @Test
    fun `CoreDocumentMetadata deserializes correctly`() {
        val jsonString = """{"url":"https://example.com/document","contentType":"application/pdf","contentLength":1024,"lastModified":"Wed, 21 Oct 2023 07:28:00 GMT","etag":"abc123"}"""

        val expected = CoreDocumentMetadata(
            url = "https://example.com/document",
            contentType = "application/pdf",
            contentLength = 1024L,
            lastModified = "Wed, 21 Oct 2023 07:28:00 GMT",
            etag = "abc123"
        )

        val deserialized = json.decodeFromString(CoreDocumentMetadata.serializer(), jsonString)
        assertEquals(expected, deserialized)
    }

    @Test
    fun `CoreDocumentMetadata handles null values`() {
        val jsonString = """{"url":"https://example.com/document"}"""
        val expected = CoreDocumentMetadata(url = "https://example.com/document")

        val deserialized = json.decodeFromString(CoreDocumentMetadata.serializer(), jsonString)
        assertEquals(expected, deserialized)
    }
}