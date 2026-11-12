package com.jht.widgets

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class LegalDocumentsParsingTest {

    // region parseSupportedLanguages

    @Test
    fun `parses languages and fallback`() {
        val json = JSONObject(
            """{"languages":["en","fr","zh-CN"],"updatedAt":"2026-07-01T21:33:30.971Z","fallbackLanguage":"en"}"""
        )
        val result = parseSupportedLanguages(json)
        assertEquals(listOf("en", "fr", "zh-CN"), result.languages)
        assertEquals("en", result.fallbackLanguage)
    }

    @Test
    fun `missing fallbackLanguage defaults to en`() {
        val json = JSONObject("""{"languages":["de","fr"]}""")
        assertEquals("en", parseSupportedLanguages(json).fallbackLanguage)
    }

    @Test
    fun `blank fallbackLanguage defaults to en`() {
        val json = JSONObject("""{"languages":["de"],"fallbackLanguage":""}""")
        assertEquals("en", parseSupportedLanguages(json).fallbackLanguage)
    }

    @Test
    fun `empty language list throws`() {
        val json = JSONObject("""{"languages":[],"fallbackLanguage":"en"}""")
        assertThrows(IOException::class.java) { parseSupportedLanguages(json) }
    }

    @Test
    fun `missing languages key throws`() {
        val json = JSONObject("""{"fallbackLanguage":"en"}""")
        assertThrows(Exception::class.java) { parseSupportedLanguages(json) }
    }

    // endregion

    // region extractDocumentMarkdown

    @Test
    fun `extracts document by manifest key`() {
        val manifest = JSONObject(
            """{"GLOBAL_PRIVACY-POLICY":"## Privacy Policy","GLOBAL_TERMS-OF-SERVICE":"## Terms"}"""
        )
        assertEquals(
            "## Privacy Policy",
            extractDocumentMarkdown(manifest, LegalDocument.PRIVACY_POLICY, "en")
        )
        assertEquals(
            "## Terms",
            extractDocumentMarkdown(manifest, LegalDocument.TERMS_OF_SERVICE, "en")
        )
    }

    @Test
    fun `missing document key throws`() {
        val manifest = JSONObject("""{"GLOBAL_TERMS-OF-SERVICE":"## Terms"}""")
        assertThrows(IOException::class.java) {
            extractDocumentMarkdown(manifest, LegalDocument.PRIVACY_POLICY, "en")
        }
    }

    @Test
    fun `blank document content throws`() {
        val manifest = JSONObject("""{"GLOBAL_PRIVACY-POLICY":"   "}""")
        assertThrows(IOException::class.java) {
            extractDocumentMarkdown(manifest, LegalDocument.PRIVACY_POLICY, "en")
        }
    }

    // endregion
}
