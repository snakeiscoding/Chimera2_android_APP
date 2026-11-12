package com.jht.chimera.lib.language

import java.io.File

/**
 * Single source of truth for how translation JSON files are named on disk and how a locale tag
 * maps to a file.
 *
 * Both sides of the translation pipeline depend on this agreeing with itself:
 *  - [com.jht.chimera.service.johnsonwebendpoints.TranslationsWeb] names the files it downloads
 *    with [canonicalFileName].
 *  - [RestringTranslationLoader] resolves the active locale to a file with [resolveByTag].
 *
 * Historically the downloader wrote the manifest tag verbatim ("zh-CN.json") while the loader
 * matched on a normalized form ('-' as '_', lower case), so a hand-placed "zh_CN.json" and a
 * downloaded "zh-CN.json" both mapped to the tag "zh_cn" and the winner was whichever
 * `File.listFiles()` happened to return last - filesystem order, not newest. Everything here is
 * deliberately deterministic so that cannot happen again.
 *
 * Intentionally free of Android dependencies (no `android.util.Log`) so it is directly unit
 * testable; callers do the logging.
 */
object TranslationFileNames {

    /** Extension of the translation files, without the dot. */
    const val JSON_EXTENSION = "json"

    /**
     * Normalizes a locale tag or file base name for comparison: trimmed, lower case, with '-'
     * replaced by '_'. So "zh-CN", "zh_CN" and "ZH_cn" all normalize to "zh_cn".
     */
    fun normalizeTag(value: String): String =
        value.trim().lowercase().replace('-', '_')

    /**
     * The one file name a given locale tag is allowed to be stored as, e.g. "zh-CN" ->
     * "zh_cn.json". Files with any other spelling of the same tag are shadowing duplicates and
     * are reported by [resolveByTag] / removed by the downloader's prune pass.
     */
    fun canonicalFileName(tag: String): String = "${normalizeTag(tag)}.$JSON_EXTENSION"

    /** True when [file] is named exactly as [tag]'s canonical file. */
    fun isCanonicalFor(file: File, tag: String): Boolean = file.name == canonicalFileName(tag)

    /** Lists the translation JSON files in [dir], newest-last order not guaranteed. */
    fun listTranslationFiles(dir: File): List<File> =
        dir.listFiles { file: File ->
            file.isFile && file.extension.equals(JSON_EXTENSION, ignoreCase = true)
        }.orEmpty().toList()

    /**
     * Groups [files] by normalized tag and picks exactly one winner per tag.
     *
     * When several files map to the same tag the winner is chosen deterministically:
     *  1. the file named exactly [canonicalFileName] for that tag, if present;
     *  2. otherwise the most recently modified file;
     *  3. otherwise (identical timestamps) the lexicographically first name, purely so the
     *     result is stable across runs rather than following directory order.
     *
     * The losers are returned in [Resolution.shadowed] so the caller can log or delete them.
     */
    fun resolveByTag(files: List<File>): Map<String, Resolution> =
        files.groupBy { normalizeTag(it.nameWithoutExtension) }
            .mapValues { (tag, candidates) -> resolve(tag, candidates) }

    private fun resolve(tag: String, candidates: List<File>): Resolution {
        if (candidates.size == 1) return Resolution(candidates.first(), emptyList())

        val ordered = candidates.sortedWith(
            compareByDescending<File> { isCanonicalFor(it, tag) }
                .thenByDescending { it.lastModified() }
                .thenBy { it.name }
        )
        return Resolution(chosen = ordered.first(), shadowed = ordered.drop(1))
    }

    /**
     * The file chosen for a tag, plus any same-tag files it beat. [shadowed] is empty in the
     * normal case of one file per tag.
     */
    data class Resolution(
        val chosen: File,
        val shadowed: List<File>,
    )
}
