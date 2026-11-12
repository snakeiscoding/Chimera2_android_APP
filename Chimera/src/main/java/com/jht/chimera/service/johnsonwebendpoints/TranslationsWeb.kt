package com.jht.chimera.service.johnsonwebendpoints

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.jht.chimera.lib.fleet.FleetCredentialStore
import com.jht.chimera.lib.fleet.FleetEnvironment
import com.jht.chimera.lib.language.RestringTranslationLoader
import com.jht.chimera.lib.language.TranslationFileNames
import com.jht.services.InlineConsoleService
import com.jht.utility.runCatchingCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.content.edit
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "TranslationsWeb"

/**
 * Pulls the dynamic-string translation JSON files from the Johnson static translation web service
 * and lands them in [RestringTranslationLoader.DEFAULT_TRANSLATION_DIR], where they override the
 * bundled strings on the next process start or locale change (this service does not hot-reload).
 *
 * The pull runs when [onBearerReady] fires - used purely as a "provisioned / network is up"
 * gate; the translation host is public so no Bearer token is sent. The fleet identity resolves
 * which environment's translation host to read (see [translationBaseUrl]). Each pull:
 *   1. GETs `languages.json` to learn the available languages.
 *   2. For each language, HEADs `{lang}.json` and compares its ETag to the one last stored.
 *   3. GETs and rewrites the languages whose ETag changed, whose local file is missing, or whose
 *      local file no longer matches the content hash recorded for that ETag.
 *   4. Prunes files and stored entries for anything the manifest does not offer.
 *
 * The ETag is treated as an opaque change token - any difference means "changed"; we do not
 * reason about newer vs older. It is paired with a content hash of what we wrote, because the
 * ETag alone cannot detect our own copy being altered after the fact.
 *
 * The manifest is treated as the complete list of languages that exist: a file in the
 * translation directory that does not correspond to a manifest tag is removed, since no future
 * pull would ever refresh it and [RestringTranslationLoader] would keep serving it.
 */
class TranslationsWeb(context: Context): InlineConsoleService(context) {
    companion object {
        const val ID = TAG // UUID or unique friendly name for this service

        private const val TRANSLATION_CLIENT_ID = "chimera"

        /**
         * SharedPreferences file holding, per language tag, the last-seen ETag (key: the tag)
         * and the SHA-256 of the file body we wrote for that ETag (key: tag + [HASH_KEY_SUFFIX]).
         */
        private const val ETAG_PREFS_NAME = "translations_web_etags"

        /** Suffix distinguishing a content-hash entry from an ETag entry in [ETAG_PREFS_NAME]. */
        private const val HASH_KEY_SUFFIX = ".sha256"

        /** Prod translation host; other environments derive theirs in [translationBaseUrl]. */
        private const val PROD_TRANSLATION_BASE_URL = "https://static-translation.jht.services"

        private const val DEBUG_LOGS = false
    }
    override val id: String = ID // for service as a whole

    private lateinit var serviceScope: CoroutineScope
    private var readWebJob: Job? = null

    private val etagPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(ETAG_PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun start() {
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        Log.d(TAG, "Service started")
    }

    override fun stop() {
        serviceScope.cancel()
        Log.d(TAG, "Service stopped")
    }

    override fun onBearerReady(ready: Boolean) {
        if (DEBUG_LOGS) Log.d(TAG, "Bearer ready $ready")
        if (ready) triggerWebPull()
    }

    private fun triggerWebPull() {
        if (readWebJob != null) Log.d(TAG, "Cancelling in-flight translation pull before starting a new one")
        readWebJob?.cancel()
        if (DEBUG_LOGS) Log.d(TAG, "Triggering translation web pull")
        readWebJob = serviceScope.launch { runCatchingCancellable { readFromWeb() } }
            //Check for current job identity so parallel runs don't null the current job
            .also { job -> job.invokeOnCompletion { if (readWebJob === job) readWebJob = null } }
    }

    private suspend fun readFromWeb() {
        Log.i(TAG, "Starting translation pull")

        val client = buildClient() ?: return

        // 1. Lookup available languages.
        val manifest = client.fetchLanguages().getOrElse {
            Log.w(TAG, "Could not fetch languages manifest; skipping translation pull")
            return
        }
        if (manifest.languages.isEmpty()) {
            Log.w(TAG, "Languages manifest is empty; nothing to pull")
            return
        }
        if (DEBUG_LOGS) Log.i(TAG, "Available languages ${manifest.languages} (updatedAt=${manifest.updatedAt})")

        // 2 + 3. Refresh each language whose ETag changed. (A future optimization could store
        // manifest.updatedAt and skip the per-language HEADs when it is unchanged.)
        var updated = 0
        for (language in manifest.languages) {
            if (DEBUG_LOGS) Log.d(TAG, "Checking '$language' for changes")
            if (refreshLanguageIfChanged(client, language)) updated++
        }
        Log.i(TAG, "Translation pull complete: $updated of ${manifest.languages.size} language(s) updated")

        // 4. The manifest is the authority on which languages exist, so anything else in the
        // directory is unreachable by future pulls and must not keep being served.
        pruneTranslationDir(manifest.languages)
        pruneStoredEntries(manifest.languages)
    }

    /**
     * Builds the translation client. Returns null (skipping the pull) if no fleet identity is
     * available - which should not happen once [onBearerReady] has fired, but is handled
     * defensively so a missing identity never crashes the pull. The fleet identity also resolves
     * which environment's translation host to read (see [translationBaseUrl]).
     */
    private suspend fun buildClient(): TranslationsWebClient? {
        val deployment = FleetCredentialStore.getInstance(context).load()?.deployment
        if (deployment == null) {
            Log.w(TAG, "No fleet identity; cannot resolve environment, skipping translation pull")
            return null
        }
        val env = FleetEnvironment.fromDeployment(deployment)
        val baseUrl = translationBaseUrl(env)
        if (DEBUG_LOGS) Log.i(TAG, "Resolved environment=$env (deployment=$deployment), baseUrl=$baseUrl, clientId='$TRANSLATION_CLIENT_ID'")
        return TranslationsWebClient(baseUrl, TRANSLATION_CLIENT_ID, debugLogs = DEBUG_LOGS)
    }

    /**
     * Downloads and stores [language] only when its remote ETag differs from the stored one, or
     * when the local file is missing. Returns true if the on-device file was (re)written.
     */
    private fun refreshLanguageIfChanged(
        client: TranslationsWebClient,
        language: String,
    ): Boolean {
        val remoteEtag = client.fetchLanguageEtag(language).getOrElse { return false }
        val storedEtag = etagPrefs.getString(language, null)

        // Skip only when the remote is unchanged AND our copy is still byte-for-byte the file we
        // wrote for that ETag. The ETag describes what the server holds; on its own it says
        // nothing about what is in our copy, so a file replaced or truncated after the fact would
        // otherwise be trusted indefinitely.
        if (remoteEtag != null && remoteEtag == storedEtag && localCopyMatchesStoredHash(language)) {
            if (DEBUG_LOGS) Log.d(TAG, "'$language' unchanged (etag=$remoteEtag); skipping")
            return false
        }

        if (DEBUG_LOGS) Log.d(TAG, "'$language' changed (remote etag=${remoteEtag ?: "none"}, stored=${storedEtag ?: "none"}); downloading")
        val downloaded = client.downloadLanguage(language).getOrElse { return false }

        //Confirm the downloaded file can be parsed as JSON before saving
        try {
            if (Json.parseToJsonElement(downloaded.body).jsonObject.isEmpty()) return false
        } catch (_: Exception){
            //File failed to parse as expected JSON format, skip writing
            Log.w(TAG, "'$language' failed to parse as JSON; skipping")
            return false
        }

        if (!writeTranslationFile(language, downloaded.body)) return false

        // Record the ETag to compare against next run. Prefer the GET's ETag; fall back to the
        // HEAD's so a later run can still short-circuit even if the GET omitted the header.
        val newEtag = downloaded.etag ?: remoteEtag
        etagPrefs.edit {
            if (newEtag != null) putString(language, newEtag) else remove(language)
            putString(hashKey(language), sha256(downloaded.body.toByteArray()))
        }

        if (DEBUG_LOGS) Log.i(TAG, "Updated '$language' (etag ${storedEtag ?: "none"} -> ${newEtag ?: "none"})")
        return true
    }

    /**
     * Writes [body] to `{dir}/{language}.json` via a temp file + rename, so
     * [RestringTranslationLoader] never observes a partially written file. Returns false on any
     * IO failure (logged), leaving any existing file untouched.
     */
    private fun writeTranslationFile(language: String, body: String): Boolean {
        return try {
            val dir = File(RestringTranslationLoader.DEFAULT_TRANSLATION_DIR)
            if (!dir.isDirectory && !dir.mkdirs()) {
                Log.w(TAG, "Could not create translation directory ${dir.absolutePath}")
                return false
            }
            val target = translationFile(language)
            val temp = File.createTempFile("translation-", ".json.tmp", dir)
            try {
                temp.writeText(body)
                if (!temp.renameTo(target)) {
                    throw IOException("Failed to rename $temp to $target")
                }
            } finally {
                temp.delete() // no-op if rename consumed it
            }
            if (DEBUG_LOGS) Log.d(TAG, "Wrote '$language' -> ${target.absolutePath} (${body.length} chars)")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write translation file for '$language'", e)
            false
        }
    }

    /**
     * The one path a language is stored at. Uses the canonical (lower case, '_' for '-') spelling
     * so the name we write always matches the name [RestringTranslationLoader] prefers on read;
     * writing the manifest tag verbatim used to let a legacy "zh-CN.json" and a fresh
     * "zh_cn.json" coexist and shadow each other unpredictably.
     */
    private fun translationFile(language: String) =
        File(RestringTranslationLoader.DEFAULT_TRANSLATION_DIR, TranslationFileNames.canonicalFileName(language))

    private fun hashKey(language: String) = "$language$HASH_KEY_SUFFIX"

    /**
     * Hex SHA-256 of [bytes]. Both call sites hash the same byte sequence - the UTF-8 the body is
     * written as, and the raw bytes read back - rather than a decoded String, so no UTF-8
     * round trip sits between what we wrote and what we verify.
     */
    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    /**
     * Whether the on-disk file for [language] is exactly what we last wrote for its stored ETag.
     *
     * False when the file is gone, when no hash was recorded (an install upgraded from a build
     * that stored ETags only - one re-download per language establishes it), or when the content
     * has drifted from the hash.
     */
    private fun localCopyMatchesStoredHash(language: String): Boolean {
        val file = translationFile(language)
        if (!file.exists()) return false

        val storedHash = etagPrefs.getString(hashKey(language), null)
        if (storedHash == null) {
            if (DEBUG_LOGS) Log.d(TAG, "'$language' has no stored content hash; treating as changed")
            return false
        }

        val actualHash = runCatching { sha256(file.readBytes()) }.getOrElse { e ->
            Log.w(TAG, "Could not hash '${file.name}'; treating as changed", e)
            return false
        }
        if (actualHash != storedHash) {
            Log.w(TAG, "'${file.name}' does not match the content we stored for its ETag; re-downloading")
            return false
        }
        return true
    }

    /**
     * Removes translation files the manifest does not account for. Called only after a successful
     * manifest fetch, so a network failure never triggers deletion.
     *
     * The two cases are deliberately handled differently:
     *  - A file whose tag is not in the manifest at all (a withdrawn language, or a file placed on
     *    the console by hand) is deleted: no future pull will ever look at it, so it would go on
     *    being served forever.
     *  - A non-canonical spelling of a manifest tag ("zh-CN.json" beside "zh_cn.json") is deleted
     *    only once the canonical file is actually present, so a failed download can never leave a
     *    language with no file at all.
     */
    private fun pruneTranslationDir(languages: List<String>) {
        val dir = File(RestringTranslationLoader.DEFAULT_TRANSLATION_DIR)
        if (!dir.isDirectory) return

        val canonicalNames = languages.mapTo(mutableSetOf()) { TranslationFileNames.canonicalFileName(it) }
        val manifestTags = languages.mapTo(mutableSetOf()) { TranslationFileNames.normalizeTag(it) }

        var removed = 0
        TranslationFileNames.listTranslationFiles(dir).forEach { file ->
            if (file.name in canonicalNames) return@forEach

            val tag = TranslationFileNames.normalizeTag(file.nameWithoutExtension)
            val canonicalName = TranslationFileNames.canonicalFileName(tag)
            val replacement = File(dir, canonicalName)
            val reason = when {
                tag !in manifestTags -> "not offered by the manifest"
                replacement.exists() && !isSameFile(file, replacement) -> "superseded by '$canonicalName'"
                else -> {
                    Log.i(TAG, "Keeping '${file.name}': it is the only file providing '$tag'")
                    return@forEach
                }
            }

            if (file.delete()) {
                removed++
                Log.i(TAG, "Removed translation file '${file.name}' ($reason)")
            } else {
                Log.w(TAG, "Could not remove translation file '${file.name}' ($reason)")
            }
        }
        if (removed > 0) Log.i(TAG, "Pruned $removed stale translation file(s)")
    }

    /**
     * Whether two paths resolve to the same file. Differing only in case they do on a
     * case-insensitive filesystem (a dev host) though not on the console's, and treating that
     * pair as one file superseding the other would delete the only copy.
     */
    private fun isSameFile(a: File, b: File): Boolean =
        runCatching { a.canonicalFile == b.canonicalFile }.getOrDefault(a == b)

    /** Drops stored ETag and hash entries for languages the manifest no longer offers. */
    private fun pruneStoredEntries(languages: List<String>) {
        val manifestTags = languages.mapTo(mutableSetOf()) { TranslationFileNames.normalizeTag(it) }
        val stale = etagPrefs.all.keys.filterNot { key ->
            TranslationFileNames.normalizeTag(key.removeSuffix(HASH_KEY_SUFFIX)) in manifestTags
        }
        if (stale.isEmpty()) return

        etagPrefs.edit { stale.forEach { remove(it) } }
        Log.i(TAG, "Dropped ${stale.size} stored entry/entries for languages no longer in the manifest")
    }

    /**
     * Base URL of the static translation host for [env], mirroring the fleet `{env}` scheme and
     * matching how every other Johnson client resolves its host (see `ConciergeApi`,
     * `AppStoreApi`, `FacilityServiceApiClient`, `DapiUtil`): prod drops the prefix and uses
     * `jht.services`; dev/qa/staging use the `{env}-` prefix on `jhtdev.services`.
     *
     * NOTE: only the prod and staging buckets are populated today. The dev and qa hosts resolve
     * and serve, but hold no objects under `manifests/{clientId}/`, so the languages manifest
     * fetch 403s there and the pull is skipped - those consoles fall back to bundled strings
     * until translations are published to dev/qa. That 403 is not a permissions problem: this
     * host is `s3:GetObject`-public without `s3:ListBucket`, so S3 reports a missing key as
     * `AccessDenied` rather than 404 (prod returns the identical body for a made-up filename).
     * Publishing content to those buckets is all that is needed; no client change.
     */
    private fun translationBaseUrl(env: FleetEnvironment): String {
        return when (env) {
            FleetEnvironment.PROD -> PROD_TRANSLATION_BASE_URL
            else -> "https://${env.name.lowercase()}-static-translation.jhtdev.services"
        }
    }
}