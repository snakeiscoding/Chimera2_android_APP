package com.jht.chimera.lib.language

import android.content.Context
import android.os.SystemClock
import android.util.Log
import dev.b3nedikt.restring.MutableStringRepository
import dev.b3nedikt.restring.Restring
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * Loads runtime translations for the active locale from JSON files into [Restring].
 *
 * Expected layout: one flat JSON object per locale in [DEFAULT_TRANSLATION_DIR], named after
 * the locale. Keys are string resource names (matching names in strings.xml, or new names for
 * which Restring generates ids at runtime), values are the translated strings.
 *
 * The downloader writes the canonical name given by [TranslationFileNames.canonicalFileName]
 * (lower case, '_' for '-', e.g. en.json, zh_cn.json), but any spelling of a locale tag is
 * accepted on read. Where two spellings of the same tag are present the winner is picked
 * deterministically by [TranslationFileNames.resolveByTag], never by directory order.
 *
 * Only the file for the requested locale is loaded (not the whole directory), resolved in
 * this order, comparing case-insensitively and treating '-' and '_' as equal:
 *
 *  1. language_REGION  (e.g. zh_CN, zh_TW)  - when the locale has a region
 *  2. language         (e.g. zh)
 *  3. [DEFAULT_LANGUAGE] ("en")             - default when nothing else matches
 *
 * If a candidate file exists but cannot be loaded (corrupted JSON, read error, no usable
 * entries), the next candidate in the list is tried, so a corrupted locale file degrades
 * to the "en" file rather than to nothing.
 *
 * The loaded strings are registered under the requested locale itself, so lookups always
 * match regardless of which file satisfied the request.
 *
 * All failures are non-fatal: a missing directory, an unreadable file, malformed JSON or a
 * non-string entry are logged and skipped, so the app falls back to bundled resources.
 */
object RestringTranslationLoader {

    private const val TAG = "RestringTranslationLoader"

    /** Default location of the translation files on the console. */
    const val DEFAULT_TRANSLATION_DIR = "/sdcard/jht/translation"

    /** Language whose file is loaded when no file matches the requested locale. */
    const val DEFAULT_LANGUAGE = "en"

    /**
     * Prefix of the SharedPreferences files written by Restring's default
     * SharedPrefs-backed repository, e.g. "dev.b3nedikt.restring.Restring_Strings_en.xml".
     */
    private const val LEGACY_PREFS_PREFIX = "dev.b3nedikt.restring."

    /**
     * Whether translation loading is enabled in this process. Set by the Application before
     * any activity starts; processes that display no translated UI (e.g. the splash process)
     * disable it so [ensureLoaded] stays a no-op there.
     */
    @Volatile
    var isEnabledInThisProcess: Boolean = true

    /**
     * The result of the most recent load attempt: normalized locale tag -> success.
     * Makes repeat [ensureLoaded] calls for the same locale a lock-free no-op, including
     * failed attempts (so a missing sdcard does not cause disk probing on every activity
     * start; the next attempt happens on the next locale change or process restart).
     */
    @Volatile
    private var lastResult: Pair<String, Boolean>? = null

    private val loadLock = Any()

    /**
     * Coroutine variant of [ensureLoaded] for call sites that are already asynchronous
     * (application startup, configuration changes).
     */
    suspend fun loadTranslations(
        locale: Locale = Locale.getDefault(),
        translationDir: File = File(DEFAULT_TRANSLATION_DIR)
    ): Boolean = withContext(Dispatchers.IO) {
        ensureLoaded(locale, translationDir)
    }

    /**
     * Ensures the translations for [locale] are loaded into [Restring], loading them
     * synchronously if needed.
     *
     * Cheap when the locale is already handled (volatile read + string compare), so it is
     * safe to call from every activity's attachBaseContext(). This closes the race where an
     * activity is recreated by a locale configuration change before the application's async
     * reload has finished: whichever activity attaches first performs the load (~tens of ms,
     * once per language change), everyone else no-ops.
     *
     * @return true if translations for [locale] are loaded (now or previously).
     */
    fun ensureLoaded(
        locale: Locale = Locale.getDefault(),
        translationDir: File = File(DEFAULT_TRANSLATION_DIR)
    ): Boolean {
        if (!isEnabledInThisProcess) return false

        val localeTag = TranslationFileNames.normalizeTag(locale.toString())
        lastResult?.let { (tag, loaded) -> if (tag == localeTag) return loaded }

        synchronized(loadLock) {
            // Re-check inside the lock: another thread may have loaded while we waited.
            lastResult?.let { (tag, loaded) -> if (tag == localeTag) return loaded }

            val loaded = loadLocked(locale, translationDir)
            lastResult = localeTag to loaded
            return loaded
        }
    }

    /** Performs the actual file resolution and load. Must be called with [loadLock] held. */
    private fun loadLocked(locale: Locale, translationDir: File): Boolean {
        val startMs = SystemClock.elapsedRealtime()

        // Drop translations of previously active locales BEFORE loading. Restring falls back
        // by language across regions at lookup time, so a leftover en_GB map would otherwise
        // be served for en_AU. This also caps memory at one locale per process.
        clearOtherLocales(locale)

        if (!translationDir.isDirectory) {
            Log.w(TAG, "Translation directory does not exist: ${translationDir.absolutePath}")
            return false
        }

        val candidates = findTranslationFileCandidates(translationDir, locale)
        if (candidates.isEmpty()) {
            Log.w(TAG, "No translation file for locale '$locale' and no $DEFAULT_LANGUAGE fallback in ${translationDir.absolutePath}")
            return false
        }

        candidates.forEach { file ->
            if (loadFile(file, locale)) {
                val elapsedMs = SystemClock.elapsedRealtime() - startMs
                Log.i(TAG, "Loaded '${file.name}' for locale '$locale' in $elapsedMs ms")
                return true
            }
            Log.w(TAG, "Failed to load '${file.name}' for locale '$locale', trying next fallback")
        }

        Log.e(TAG, "No usable translation file for locale '$locale' in ${translationDir.absolutePath}")
        return false
    }

    /**
     * Removes every locale except [requested] from the Restring repository.
     *
     * Restring's repository is an observable map that inserts an empty entry for any locale the
     * moment something *reads* it - `Restring.putStrings` relies on that. A string lookup that
     * misses falls back from the active locale to its bare language (en for en_AU), and that
     * fallback read leaves behind an empty "phantom" entry for the bare language. Every locale
     * the console ships is region-qualified, so this happens on healthy systems as a matter of
     * course; the phantoms hold no translations and clearing them is uninteresting.
     *
     * Only locales that actually held strings are therefore logged, so the message means "real
     * translations were dropped" rather than firing on every language change. Removal itself
     * still covers the phantoms.
     *
     * Note the iteration goes through `entries`: indexing `repository.strings[locale]` to test
     * for emptiness would insert the very entries this is trying to clean up. `entries`, `keys`
     * and `values` are all read-only in that respect; only `get` inserts.
     */
    private fun clearOtherLocales(requested: Locale) {
        try {
            val repository = Restring.stringRepository as? MutableStringRepository ?: return

            // Snapshot before mutating - this is the map we are about to remove from.
            val stale = repository.strings.entries
                .filter { it.key != requested }
                .map { it.key to it.value.isNotEmpty() }

            stale.forEach { (locale, _) -> repository.strings.remove(locale) }

            val localesWithStrings = stale.filter { it.second }.map { it.first }
            if (localesWithStrings.isNotEmpty()) {
                Log.i(TAG, "Cleared stale translation locales: $localesWithStrings")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear stale translation locales", e)
        }
    }

    /**
     * One-time cleanup of SharedPreferences files persisted by builds that used Restring's
     * default SharedPrefs-backed repository. Those files caused multi-second boot delays
     * (full deserialization of all entries on every lookup miss) and are unused now that
     * translations live in a memory-only repository. Near-free when no files exist (a single
     * directory listing), so it is safe to call on every process start. Never throws.
     */
    suspend fun cleanUpLegacyRestringPrefs(context: Context) = withContext(Dispatchers.IO) {
        try {
            val prefsDir = File(context.dataDir, "shared_prefs")

            // Prefix match also covers Android's SharedPreferences backup files (*.xml.bak).
            val legacyFiles = prefsDir
                .listFiles { file -> file.isFile && file.name.startsWith(LEGACY_PREFS_PREFIX) }
                .orEmpty()

            legacyFiles.forEach { file ->
                if (file.delete()) {
                    Log.i(TAG, "Deleted legacy Restring prefs file '${file.name}'")
                } else if (file.exists()) {
                    // Another process may have deleted it between listing and here; only warn
                    // if the file is genuinely still present.
                    Log.w(TAG, "Could not delete legacy Restring prefs file '${file.name}'")
                }
            }
        } catch (e: Exception) {
            // Cleanup is best-effort; never let it interfere with startup.
            Log.e(TAG, "Legacy Restring prefs cleanup failed", e)
        }
    }

    /**
     * Returns the candidate translation files for [locale] in fallback order:
     * language_REGION -> language -> [DEFAULT_LANGUAGE]. Only files that exist are included,
     * without duplicates. File names are compared case-insensitively with '-' treated as '_'
     * (so "zh-cn.json" matches zh_CN), and same-tag duplicates are resolved by
     * [TranslationFileNames.resolveByTag] rather than by whatever order the directory listing
     * happened to return.
     */
    private fun findTranslationFileCandidates(translationDir: File, locale: Locale): List<File> {
        val jsonFiles = TranslationFileNames.listTranslationFiles(translationDir)

        if (jsonFiles.isEmpty()) {
            Log.w(TAG, "No translation files found in ${translationDir.absolutePath}")
            return emptyList()
        }

        // Several files can map to the same tag (e.g. a downloaded "zh_cn.json" beside a legacy
        // "zh-CN.json"). Resolve deterministically - canonical name, then newest - instead of
        // letting directory order decide, and say out loud which files lost.
        val resolutions = TranslationFileNames.resolveByTag(jsonFiles)
        resolutions.forEach { (tag, resolution) ->
            if (resolution.shadowed.isNotEmpty()) {
                Log.w(
                    TAG,
                    "Multiple translation files for '$tag'; using '${resolution.chosen.name}', " +
                        "ignoring ${resolution.shadowed.map { it.name }}"
                )
            }
        }
        val filesByName = resolutions.mapValues { (_, resolution) -> resolution.chosen }

        val language = TranslationFileNames.normalizeTag(locale.language)
        val region = locale.country

        val candidateNames = buildList {
            if (language.isNotEmpty() && region.isNotEmpty()) add(TranslationFileNames.normalizeTag("${language}_$region"))
            if (language.isNotEmpty()) add(language)
            add(DEFAULT_LANGUAGE)
        }

        return candidateNames.distinct().mapNotNull { filesByName[it] }.distinct()
    }

    /**
     * Loads a single translation file into Restring, registering its strings under [locale].
     *
     * @return true if the file was loaded successfully.
     */
    private fun loadFile(file: File, locale: Locale): Boolean {
        return try {
            // Parse to a JsonObject rather than Map<String, String> so a single null or
            // non-string entry only skips that entry instead of failing the whole file.
            val root = Json.parseToJsonElement(file.readText()).jsonObject

            val strings = buildMap<String, CharSequence> {
                root.forEach { (key, element) ->
                    val value = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
                    if (key.isNotBlank() && value != null) {
                        put(key, value)
                    } else {
                        Log.w(TAG, "Ignoring entry '$key' in '${file.name}': not a string value")
                    }
                }
            }

            if (strings.isEmpty()) {
                Log.w(TAG, "Skipping '${file.name}': no usable entries")
                false
            } else {
                Restring.putStrings(locale, strings)
                Log.i(TAG, "Loaded ${strings.size} strings for locale '$locale' from '${file.name}'")
                true
            }
        } catch (e: SerializationException) {
            Log.e(TAG, "Skipping '${file.name}': malformed JSON", e)
            //Delete the file so a corrected copy can be redownloaded
            file.delete()
            false
        } catch (e: IllegalArgumentException) {
            // Thrown by .jsonObject when the root element is not a JSON object
            Log.e(TAG, "Skipping '${file.name}': root element is not a JSON object", e)
            //Delete the file so a corrected copy can be redownloaded
            file.delete()
            false
        } catch (e: IOException) {
            Log.e(TAG, "Skipping '${file.name}': read failed", e)
            false
        } catch (e: Exception) {
            // Last-resort guard: a single bad file must never take down string loading.
            Log.e(TAG, "Skipping '${file.name}': unexpected failure", e)
            false
        }
    }
}
