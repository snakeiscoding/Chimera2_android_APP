package com.jht.widgets

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LegalDocumentsLanguageMatchTest {

    // mirrors https://static-translation.jht.services/manifests/global-legal/languages.json
    private val supported = listOf(
        "bg", "cs", "da", "de", "de-CH", "el", "en", "es", "es-CL", "es-MX",
        "fr", "fr-CA", "it", "ja", "ko", "nl", "pl", "pt", "ru", "th", "tr",
        "zh-CN", "zh-HK", "zh-TW",
    )
    private val fallback = "en"

    private fun match(locale: Locale) = matchSupportedLanguage(locale, supported, fallback)

    @Test
    fun `language-only match when full locale is not listed`() {
        assertEquals("fr", match(Locale("fr", "FR")))
        assertEquals("es", match(Locale("es", "ES")))
        assertEquals("de", match(Locale("de", "DE")))
    }

    @Test
    fun `direct match converts underscore to dash and returns manifest spelling`() {
        assertEquals("zh-CN", match(Locale("zh", "CN")))
        assertEquals("zh-TW", match(Locale("zh", "TW")))
        assertEquals("fr-CA", match(Locale("fr", "CA")))
        assertEquals("de-CH", match(Locale("de", "CH")))
        assertEquals("es-MX", match(Locale("es", "MX")))
    }

    @Test
    fun `match is case-insensitive`() {
        assertEquals("zh-CN", match(Locale("ZH", "cn")))
        assertEquals("fr", match(Locale("FR")))
    }

    @Test
    fun `language without country matches directly`() {
        assertEquals("ja", match(Locale("ja")))
        assertEquals("en", match(Locale("en")))
    }

    @Test
    fun `unsupported locale returns fallback`() {
        assertEquals(fallback, match(Locale("xx", "YY")))
        // zh alone isn't listed (only zh-CN, zh-HK, zh-TW), so an unlisted region falls back
        assertEquals(fallback, match(Locale("zh", "SG")))
    }

    @Test
    fun `empty locale returns fallback`() {
        assertEquals(fallback, match(Locale("")))
    }
}
