package com.jht.chimera.service.johnsonwebendpoints

import android.util.Log
import com.jht.chimera.lib.common.http.IHttpClient
import com.jht.chimera.lib.common.http.IHttpResponse
import com.jht.chimera.lib.common.http.SimpleHttpClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Network client for the static translation service.
 *
 * All endpoints live under `{baseUrl}/manifests/{clientId}/` (prod:
 * `https://static-translation.jht.services/manifests/{clientId}/`):
 *
 *  - `GET  languages.json` -> [LanguagesManifest], the list of available language tags.
 *  - `HEAD {lang}.json`    -> the response ETag, used purely as an opaque change token: we only
 *                             care whether it differs from the one we stored, not its value or
 *                             whether it is "newer".
 *  - `GET  {lang}.json`    -> the flat translation JSON for that language.
 *
 * This is a public static host (readable without credentials), so no Authorization header is
 * sent - a stray Bearer header can itself trigger a 403 on S3/CloudFront-backed hosts. Every
 * method returns a [Result] and never throws; failures are logged and surfaced as
 * [Result.failure]. All calls are blocking (backed by [SimpleHttpClient] / HttpURLConnection)
 * and are expected to be invoked from an IO dispatcher.
 */
class TranslationsWebClient(
    private val baseUrl: String,
    private val clientId: String,
    private val httpClient: IHttpClient = SimpleHttpClient(),
    private val debugLogs: Boolean = false
) {
    companion object {
        private const val TAG = "TranslationsWebClient"
        private const val TIMEOUT_MS = 30_000
        private const val LANGUAGES_FILE = "languages.json"
        private val json = Json { ignoreUnknownKeys = true }
    }

    private fun manifestUrl(fileName: String) = "$baseUrl/manifests/$clientId/$fileName"

    private fun languageFile(language: String) = "$language.json"

    /** `GET languages.json` -> the available languages. */
    fun fetchLanguages(): Result<LanguagesManifest> = runCatching {
        val url = manifestUrl(LANGUAGES_FILE)
        if (debugLogs) Log.d(TAG, "GET $url")
        val response = httpClient.get(url, timeout = TIMEOUT_MS)
        if (debugLogs) Log.d(TAG, "GET $url -> HTTP ${response.statusCode}")
        check(response.statusCode == 200) { "GET $url failed: HTTP ${response.statusCode}" }
        json.decodeFromString<LanguagesManifest>(response.body).also {
            if (debugLogs) Log.d(TAG, "Parsed languages.json: ${it.languages} (updatedAt=${it.updatedAt})")
        }
    }.onFailure { Log.w(TAG, "fetchLanguages failed", it) }

    /**
     * `HEAD {lang}.json` -> the current ETag, or `null` when the server returned 200 without one
     * (treated by the caller as "unknown", i.e. download to be safe).
     */
    fun fetchLanguageEtag(language: String): Result<String?> = runCatching {
        val url = manifestUrl(languageFile(language))
        if (debugLogs) Log.d(TAG, "HEAD $url")
        val response = httpClient.head(url, timeout = TIMEOUT_MS)
        val etag = response.etag()
        if (debugLogs) Log.d(TAG, "HEAD $url -> HTTP ${response.statusCode}, etag=${etag ?: "none"}")
        check(response.statusCode == 200) { "HEAD $url failed: HTTP ${response.statusCode}" }
        etag
    }.onFailure { Log.w(TAG, "fetchLanguageEtag failed for '$language'", it) }

    /** `GET {lang}.json` -> the raw JSON body together with the ETag it was served with. */
    fun downloadLanguage(language: String): Result<DownloadedLanguage> = runCatching {
        val url = manifestUrl(languageFile(language))
        if (debugLogs) Log.d(TAG, "GET $url")
        val response = httpClient.get(url, timeout = TIMEOUT_MS)
        if (debugLogs) Log.d(TAG, "GET $url -> HTTP ${response.statusCode}, ${response.body.length} chars, etag=${response.etag() ?: "none"}")
        check(response.statusCode == 200) { "GET $url failed: HTTP ${response.statusCode}" }
        DownloadedLanguage(language = language, body = response.body, etag = response.etag())
    }.onFailure { Log.w(TAG, "downloadLanguage failed for '$language'", it) }
}

/**
 * Case-insensitive ETag header lookup (HttpURLConnection preserves the server's header casing),
 * normalized by stripping the weak-validator prefix. This host returns a strong ETag on HEAD
 * (`"x"`) but a weak one on GET (`W/"x"`) for the same resource; since we treat the ETag purely
 * as an opaque change token, stripping `W/` lets HEAD, GET, and the stored value compare equal -
 * without it the "unchanged, skip" branch never fires and every pull re-downloads everything.
 */
private fun IHttpResponse.etag(): String? =
    headers.entries.firstOrNull { it.key.equals("ETag", ignoreCase = true) }
        ?.value?.firstOrNull()
        ?.removePrefix("W/")

/** Response body of `GET languages.json`. Unknown fields are ignored. */
@Serializable
data class LanguagesManifest(
    val languages: List<String> = emptyList(),
    val updatedAt: String? = null,
)

/** A downloaded language file plus the ETag it was served with (for change tracking). */
data class DownloadedLanguage(
    val language: String,
    val body: String,
    val etag: String?,
)