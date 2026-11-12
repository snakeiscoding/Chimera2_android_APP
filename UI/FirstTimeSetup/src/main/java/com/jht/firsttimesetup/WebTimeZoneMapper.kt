package com.jht.firsttimesetup

import android.util.Log
import java.time.ZoneId
import java.time.zone.ZoneRules

/**
 * Maps a timezone name from the web facility profile onto an index of the console's much shorter
 * `ianaTimezone` list, using the fixed tables [WEB_TO_CONSOLE_TIMEZONES] (same clock time) and
 * [WEB_TO_CLOSEST_CONSOLE_TIMEZONES] (closest clock time, logged as a warning).
 *
 * Extra spaces and upper/lower case in the web name are ignored. A legacy name that is in neither
 * table (such as `US/Central`) is matched to the table zone with the same time rules and logged as
 * a warning. Anything else is logged as an error and falls back to US Central (CGC-25917).
 */
object WebTimeZoneMapper {

    private const val TAG = "WebTimeZoneMapper"

    /** Used (by index) when nothing matches. */
    const val DEFAULT_IANA = "America/Chicago"

    /** Keys of both tables by lowercase, so lookup ignores upper/lower case. */
    private val webIdsByLowercase: Map<String, String> =
        (WEB_TO_CONSOLE_TIMEZONES.keys + WEB_TO_CLOSEST_CONSOLE_TIMEZONES.keys).associateBy { it.lowercase() }

    /** How a [Match] was found. */
    enum class MatchStep {
        /** The web name is in [WEB_TO_CONSOLE_TIMEZONES]: same clock time. */
        TABLE,
        /** The web name is in [WEB_TO_CLOSEST_CONSOLE_TIMEZONES]: closest clock time, may be up to 1 hour off. */
        CLOSEST,
        /**
         * The web name is in neither table, but it is a legacy name with the same time rules as a
         * table zone (`US/Central` is `America/Chicago`), so that zone's entry was used.
         */
        LEGACY,
        /**
         * No match in the tables or by legacy name, or the entry is missing, so [DEFAULT_IANA]
         * (US Central) was used.
         */
        DEFAULT,
    }

    /**
     * Result of [resolve].
     *
     * @property index index into the IANA list.
     * @property step how the index was found.
     * @property webId the table zone that was used (e.g. `America/New_York`), or null if none.
     * @property legacyId for [MatchStep.LEGACY], the legacy name the web sent (e.g. `US/Eastern`).
     */
    data class Match(val index: Int, val step: MatchStep, val webId: String? = null, val legacyId: String? = null)

    /**
     * Returns the index in [ianaList] for [webTimeZone].
     *
     * @param logError where the no-match error goes; defaults to logcat.
     * @param logWarning where the closest-match and legacy-name warnings go; defaults to logcat.
     */
    fun mapToIndex(
        webTimeZone: String?,
        ianaList: List<String>,
        logError: (String) -> Unit = { Log.e(TAG, it) },
        logWarning: (String) -> Unit = { Log.w(TAG, it) },
    ): Int {
        val match = resolve(webTimeZone, ianaList)
        when (match.step) {
            MatchStep.TABLE -> Unit
            MatchStep.CLOSEST -> logWarning(
                "No console timezone with the same clock time as '$webTimeZone'; " +
                    "using closest ${ianaList[match.index]}, the clock may be up to 1 hour off"
            )
            MatchStep.LEGACY -> logWarning(
                "Legacy timezone name '$webTimeZone' matched to ${match.webId} (same time rules); " +
                    "using console ${ianaList[match.index]}" +
                    if (match.webId in WEB_TO_CLOSEST_CONSOLE_TIMEZONES) ", the clock may be up to 1 hour off" else ""
            )
            MatchStep.DEFAULT -> {
                val reason = match.webId?.let { "table entry ${consoleIdFor(it)} is not in ianaTimezone" }
                    ?: "not in the web timezone table and not a legacy name for one"
                logError("No timezone match for '$webTimeZone' ($reason); defaulting to $DEFAULT_IANA")
            }
        }
        return match.index
    }

    /** Same as [mapToIndex] but also reports which [MatchStep] was used, and never logs. */
    fun resolve(webTimeZone: String?, ianaList: List<String>): Match {
        val cleaned = webTimeZone?.let { clean(it) }.orEmpty()
        webIdsByLowercase[cleaned.lowercase()]?.let { webId ->
            val step = if (webId in WEB_TO_CONSOLE_TIMEZONES) MatchStep.TABLE else MatchStep.CLOSEST
            return tableMatch(webId, step, ianaList)
        }
        val legacy = legacyEquivalent(cleaned) ?: return defaultMatch(ianaList, webId = null)
        return tableMatch(legacy.second, MatchStep.LEGACY, ianaList).copy(legacyId = legacy.first)
    }

    private fun tableMatch(webId: String, step: MatchStep, ianaList: List<String>): Match {
        val consoleId = consoleIdFor(webId)
        val index = WEB_TO_CONSOLE_INDEX[webId]
            ?.takeIf { ianaList.getOrNull(it).equals(consoleId, ignoreCase = true) }
            ?: exactIndex(ianaList, consoleId)
            ?: return defaultMatch(ianaList, webId)
        return Match(index, step, webId)
    }

    /**
     * Finds the table zone with the same time rules as [name], for legacy names such as
     * `US/Central` that the device still knows. Returns (name as the device spells it, table zone),
     * or null if [name] is unknown or no table zone has the same rules. Same-time table first.
     */
    private fun legacyEquivalent(name: String): Pair<String, String>? {
        if (name.isEmpty()) return null
        val id = deviceZoneIdsByLowercase[name.lowercase()] ?: return null
        val rules = runCatching { ZoneId.of(id).rules }.getOrNull() ?: return null
        val webId = tableRules.firstOrNull { it.second == rules }?.first ?: return null
        return id to webId
    }

    /** Every zone id the device knows, by lowercase, because [ZoneId.of] is case-sensitive. */
    private val deviceZoneIdsByLowercase: Map<String, String> by lazy {
        ZoneId.getAvailableZoneIds().associateBy { it.lowercase() }
    }

    /** Time rules of every table zone, same-time table first; zones the device does not know are skipped. */
    private val tableRules: List<Pair<String, ZoneRules>> by lazy {
        (WEB_TO_CONSOLE_TIMEZONES.keys + WEB_TO_CLOSEST_CONSOLE_TIMEZONES.keys).mapNotNull { webId ->
            runCatching { webId to ZoneId.of(webId).rules }.getOrNull()
        }
    }

    /**
     * Builds a readable table with one row per entry of [webTimeZones]:
     *
     * `web IANA id -> [index] local IANA id (step) -> display text`
     *
     * The first column is the web name as written in the table. A legacy name is shown as
     * `name (legacy name for zone)`, and a name with no match as `name (not in table)`. The last
     * column is left out when [displayList] is empty. Used for review and debugging; see [logMapping].
     *
     * @param displayList the `timezoneDisplay` strings, index for index with [ianaList].
     */
    fun describeMapping(
        webTimeZones: List<String>,
        ianaList: List<String>,
        displayList: List<String> = emptyList(),
    ): String = buildString {
        appendLine(
            "Web timezone mapping (${WEB_TO_CONSOLE_TIMEZONES.size} same-time + " +
                "${WEB_TO_CLOSEST_CONSOLE_TIMEZONES.size} closest entries, ${ianaList.size} local zones)"
        )
        webTimeZones.forEach { web ->
            val match = resolve(web, ianaList)
            val webColumn = when {
                match.legacyId != null -> "${match.legacyId} (legacy name for ${match.webId})"
                match.webId != null -> match.webId
                else -> "${clean(web)} (not in table)"
            }
            val local = ianaList.getOrElse(match.index) { "?" }
            val display = displayList.getOrNull(match.index)?.let { " -> $it" }.orEmpty()
            appendLine("$webColumn -> [${match.index}] $local (${match.step})$display")
        }
    }

    /** Writes [describeMapping] to logcat, one line per row so logcat does not cut it off. */
    fun logMapping(
        webTimeZones: List<String>,
        ianaList: List<String>,
        displayList: List<String> = emptyList(),
    ) {
        describeMapping(webTimeZones, ianaList, displayList).lineSequence()
            .filter { it.isNotBlank() }
            .forEach { Log.d(TAG, it) }
    }

    /** Trims, then turns spaces into the underscores IANA uses (`America / New York` -> `America/New_York`). */
    private fun clean(raw: String): String = raw.trim()
        .replace(Regex("""\s*/\s*"""), "/")
        .replace(Regex("""\s+"""), "_")

    private fun consoleIdFor(webId: String): String =
        WEB_TO_CONSOLE_TIMEZONES[webId] ?: WEB_TO_CLOSEST_CONSOLE_TIMEZONES.getValue(webId)

    private fun exactIndex(ianaList: List<String>, id: String): Int? =
        ianaList.indexOfFirst { it.trim().equals(id, ignoreCase = true) }.takeIf { it >= 0 }

    private fun defaultMatch(ianaList: List<String>, webId: String?) =
        Match(exactIndex(ianaList, DEFAULT_IANA) ?: 0, MatchStep.DEFAULT, webId)
}
