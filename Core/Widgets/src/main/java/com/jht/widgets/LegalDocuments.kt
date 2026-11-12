package com.jht.widgets

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeFlatButton
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.widgets.helpers.isLiveApplication
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val TAG = "LegalDocuments"

private const val GLOBAL_LEGAL_BASE_URL =
    "https://static-translation.jht.services/manifests/global-legal"
private const val LANGUAGES_MANIFEST_URL = "$GLOBAL_LEGAL_BASE_URL/languages.json"
// used if the languages manifest itself can't be fetched
private const val DEFAULT_FALLBACK_LANGUAGE = "en"
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000

/** Legal documents available in the global-legal manifest (a flat JSON map of key -> markdown). */
enum class LegalDocument(internal val manifestKey: String) {
    PRIVACY_POLICY("GLOBAL_PRIVACY-POLICY"),
    TERMS_OF_SERVICE("GLOBAL_TERMS-OF-SERVICE"),
}

private sealed interface LegalDocumentUiState {
    data object Loading : LegalDocumentUiState
    data class Success(val markdown: String) : LegalDocumentUiState
    data object Error : LegalDocumentUiState
}

/** Representative markdown for previews, exercising the elements the real documents use. */
private fun previewMarkdown(document: LegalDocument) = """
    ## ${document.name.replace('_', ' ')} for Johnson Health Tech Co., Ltd

    Updated and effective June 5, 2025.

    This is **preview** content exercising the markdown elements found in the
    real documents, including *emphasis* and links like privacy@johnsonfit.com
    or http://www.allaboutcookies.org (which must render as plain text, not
    clickable links).

    ## WHAT INFORMATION DO WE GATHER?

    We may collect information from you directly:

    - Contact information (such as your name and e-mail address)
    - Demographic information (for example, your gender and age)
    - Workout history, videos watched, and music stations played
""".trimIndent()

@AllDevicePreviews
@Composable
private fun PreviewLegalDocumentContent() {
    LegalDocumentContent(document = LegalDocument.PRIVACY_POLICY)
}

/**
 * Loads [document] from the global-legal manifest in a coroutine and renders it as markdown.
 * The manifest language is resolved from the device locale via [resolveLegalDocumentLanguage].
 * Shows a spinner while loading and an error message with a Retry button on failure.
 *
 * @param onLoadedChanged reports whether the document is currently loaded, so callers can
 * gate actions (e.g. an Accept button) on the user actually being able to read the document.
 */
@Composable
fun LegalDocumentContent(
    document: LegalDocument,
    modifier: Modifier = Modifier,
    onLoadedChanged: (Boolean) -> Unit = {},
) {
    val isPreview = !isLiveApplication()
    var uiState by remember(document) {
        mutableStateOf<LegalDocumentUiState>(
            if (isPreview) LegalDocumentUiState.Success(previewMarkdown(document))
            else LegalDocumentUiState.Loading
        )
    }
    // bumping this re-runs the LaunchedEffect below (retry after a failed load)
    var loadAttempt by remember { mutableIntStateOf(0) }

    if (!isPreview) {
        LaunchedEffect(document, loadAttempt) {
            uiState = LegalDocumentUiState.Loading
            onLoadedChanged(false)
            uiState = loadLegalDocumentMarkdown(document)
                .fold(
                    onSuccess = {
                        onLoadedChanged(true)
                        LegalDocumentUiState.Success(it)
                    },
                    onFailure = {
                        Log.e(TAG, "Failed to load ${document.name}", it)
                        LegalDocumentUiState.Error
                    }
                )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is LegalDocumentUiState.Loading -> {
                CircularProgressIndicator(color = DynamicTheme.colors.primary)
            }
            is LegalDocumentUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.network_error_pg), color = Color.White)
                    ThemeFlatButton(
                        "Retry",
                        onClick = { loadAttempt++ },
                        isPrimary = false,
                        modifier = Modifier
                            .fillMaxWidth(.3f)
                            .padding(10.dp.computeSize())
                    )
                }
            }
            is LegalDocumentUiState.Success -> {
                MarkdownText(
                    markdown = state.markdown,
                    // MarkdownText takes its text color from style.color, falling back to
                    // LocalContentColor - set it explicitly or the text can render invisibly
                    // against the background (white-on-white)
                    style = LocalTextStyle.current.copy(color = Color.White),
                    // links shouldn't be interactive on the console:
                    // don't auto-linkify emails/URLs/phone numbers...
                    linkifyMask = 0,
                    // ...don't allow clicks on explicit markdown links...
                    disableLinkMovementMethod = true,
                    // ...and style them as plain text so they don't look tappable
                    linkColor = Color.White,
                    enableUnderlineForLink = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp.computeSize())
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Supported languages
// ---------------------------------------------------------------------------------------------

internal data class SupportedLanguages(
    val languages: List<String>,
    val fallbackLanguage: String,
)

private val languagesMutex = Mutex()
@Volatile
private var cachedLanguages: SupportedLanguages? = null

/**
 * Resolves which global-legal manifest language to fetch for [locale] (default: device locale).
 *
 * Fetches (and caches) the supported-language list from languages.json, then matches:
 * 1. direct match on the full locale (case-insensitive, "-" and "_" treated as equal,
 *    since Android locales use underscore and the manifest uses dashes)
 * 2. match on the 2-character language code
 * 3. the manifest's fallbackLanguage
 *
 * Examples: fr_FR -> "fr", zh_CN -> "zh-CN", xx_YY -> "en".
 * Returns the entry exactly as listed in the manifest, ready to build the URL
 * (e.g. "$GLOBAL_LEGAL_BASE_URL/fr.json").
 */
suspend fun resolveLegalDocumentLanguage(locale: Locale = Locale.getDefault()): String =
    withContext(Dispatchers.IO) {
        val supported = runCatching {
            languagesMutex.withLock {
                cachedLanguages ?: fetchSupportedLanguages().also { cachedLanguages = it }
            }
        }.getOrElse {
            // never swallow coroutine cancellation
            if (it is CancellationException) throw it
            Log.e(TAG, "Failed to load supported languages, using $DEFAULT_FALLBACK_LANGUAGE", it)
            return@withContext DEFAULT_FALLBACK_LANGUAGE
        }

        matchSupportedLanguage(locale, supported.languages, supported.fallbackLanguage).also {
            Log.d(TAG, "Locale $locale resolved to manifest language: $it")
        }
    }

/** Normalizes a language tag for comparison: case-insensitive, dashes treated as underscores. */
private fun normalizeLanguageTag(tag: String): String =
    tag.trim().lowercase(Locale.ROOT).replace('-', '_')

/**
 * Pure matching logic, separated from network/cache so it's unit-testable.
 * Returns the matching entry as spelled in [supportedLanguages], or [fallbackLanguage].
 */
internal fun matchSupportedLanguage(
    locale: Locale,
    supportedLanguages: List<String>,
    fallbackLanguage: String,
): String {
    val androidLocale = listOf(locale.language, locale.country)
        .filter { it.isNotBlank() }
        .joinToString("_")
    if (androidLocale.isBlank()) return fallbackLanguage
    val normalized = normalizeLanguageTag(androidLocale)

    // 1. direct match, e.g. zh_CN -> zh-CN
    supportedLanguages.firstOrNull { normalizeLanguageTag(it) == normalized }
        ?.let { return it }

    // 2. language-only match, e.g. fr_FR -> fr
    val languageOnly = normalized.take(2)
    supportedLanguages.firstOrNull { normalizeLanguageTag(it) == languageOnly }
        ?.let { return it }

    // 3. nothing supported for this locale
    return fallbackLanguage
}

private fun fetchSupportedLanguages(): SupportedLanguages {
    Log.d(TAG, "Fetching supported languages from $LANGUAGES_MANIFEST_URL")
    return parseSupportedLanguages(fetchJson(LANGUAGES_MANIFEST_URL)).also {
        Log.d(TAG, "Supported languages: ${it.languages}, fallback=${it.fallbackLanguage}")
    }
}

/** Pure parsing of languages.json, separated from network/logging so it's unit-testable. */
internal fun parseSupportedLanguages(json: JSONObject): SupportedLanguages {
    val languagesArray = json.getJSONArray("languages")
    val languages = List(languagesArray.length()) { languagesArray.getString(it) }
    if (languages.isEmpty()) {
        throw IOException("Supported language list is empty")
    }
    val fallback = json.optString("fallbackLanguage").ifBlank { DEFAULT_FALLBACK_LANGUAGE }
    return SupportedLanguages(languages, fallback)
}

// ---------------------------------------------------------------------------------------------
// Legal document manifests
// ---------------------------------------------------------------------------------------------

// The manifest holds every legal document, so cache it per language: switching between privacy
// policy and terms of service (or reopening the popup) doesn't refetch.
private val manifestMutex = Mutex()
private val cachedManifests = mutableMapOf<String, JSONObject>()

/**
 * Resolves the manifest language for the device locale and extracts the markdown for [document].
 * If loading or parsing the non-default language fails at any step (missing file, bad JSON,
 * missing key, blank content), retries once with the default language before giving up.
 */
private suspend fun loadLegalDocumentMarkdown(document: LegalDocument): Result<String> =
    withContext(Dispatchers.IO) {
        val language = resolveLegalDocumentLanguage()
        // prefer the fallback declared by languages.json; hardcoded default if that fetch failed
        val fallback = cachedLanguages?.fallbackLanguage ?: DEFAULT_FALLBACK_LANGUAGE
        val result = runCatching { extractDocument(document, language) }
        if (result.isFailure && language != fallback) {
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            Log.w(TAG, "Failed to load ${document.name} for $language, " +
                    "retrying with $fallback", result.exceptionOrNull())
            return@withContext runCatching { extractDocument(document, fallback) }
                .onFailure { if (it is CancellationException) throw it }
        }
        result.onFailure {
            // never swallow coroutine cancellation
            if (it is CancellationException) throw it
        }
    }

/** Fetches (or reuses the cached) manifest for [language] and extracts the [document] markdown. */
private suspend fun extractDocument(document: LegalDocument, language: String): String {
    val manifest = manifestMutex.withLock {
        cachedManifests[language]?.also {
            Log.d(TAG, "Using cached global-legal manifest for $language")
        } ?: fetchManifest(language).also { cachedManifests[language] = it }
    }
    return extractDocumentMarkdown(manifest, document, language).also {
        Log.d(TAG, "Loaded ${document.name} (key=${document.manifestKey}, language=$language): " +
                "${it.length} chars, starts with: ${it.take(80).replace("\n", "\\n")}")
    }
}

/** Pure extraction of a document from a manifest, separated from network/cache so it's unit-testable. */
internal fun extractDocumentMarkdown(
    manifest: JSONObject,
    document: LegalDocument,
    language: String,
): String {
    if (!manifest.has(document.manifestKey)) {
        throw IOException("Key ${document.manifestKey} missing from $language global-legal manifest")
    }
    return manifest.getString(document.manifestKey).ifBlank {
        throw IOException("${document.name} content is empty")
    }
}

private fun fetchManifest(language: String): JSONObject {
    val url = "$GLOBAL_LEGAL_BASE_URL/$language.json"
    Log.d(TAG, "Fetching global-legal manifest from $url")
    val manifest = fetchJson(url)
    Log.d(TAG, "Manifest fetched for $language: keys=${manifest.keys().asSequence().toList()}")
    return manifest
}

// ---------------------------------------------------------------------------------------------
// Networking
// ---------------------------------------------------------------------------------------------

private fun fetchJson(url: String): JSONObject {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("Unexpected HTTP response $responseCode from $url")
        }

        val body = connection.inputStream.bufferedReader().use { it.readText() }
        Log.d(TAG, "HTTP $responseCode from $url (${body.length} chars)")
        return JSONObject(body)
    } finally {
        connection.disconnect()
    }
}
