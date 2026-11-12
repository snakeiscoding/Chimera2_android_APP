package com.jht.firsttimesetup.mappers

import com.jht.firsttimesetup.WEB_TO_CLOSEST_CONSOLE_TIMEZONES
import com.jht.firsttimesetup.WEB_TO_CONSOLE_TIMEZONES
import com.jht.firsttimesetup.WebTimeZoneMapper
import com.jht.firsttimesetup.WebTimeZoneMapper.MatchStep
import com.jht.firsttimesetup.mappers.WebTimeZoneTestData.displayList
import com.jht.firsttimesetup.mappers.WebTimeZoneTestData.expectedClosest
import com.jht.firsttimesetup.mappers.WebTimeZoneTestData.ianaList
import com.jht.firsttimesetup.mappers.WebTimeZoneTestData.webNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WebTimeZoneMapperTest {

    private val chicagoIndex get() = ianaList.indexOf("America/Chicago")

    private val errors = mutableListOf<String>()
    private val warnings = mutableListOf<String>()

    private fun map(input: String?, iana: List<String> = ianaList) = WebTimeZoneMapper.mapToIndex(
        webTimeZone = input,
        ianaList = iana,
        logError = { errors += it },
        logWarning = { warnings += it },
    )

    private fun resolve(input: String?) = WebTimeZoneMapper.resolve(input, ianaList)

    @Test
    fun `resource list has the expected shape`() {
        assertEquals(78, ianaList.size)
        assertEquals(ianaList.size, displayList.size)
        assertEquals("(GMT-06:00) Central Time (US and Canada)", displayList[9])
        assertEquals(9, chicagoIndex)
        assertEquals(418, webNames.size)
    }

    // region input cleanup

    @Test
    fun `whitespace and case are ignored`() {
        assertEquals(9, map("   america/CHICAGO   "))
        assertEquals(9, map("\tAmerica/Chicago\n"))
    }

    @Test
    fun `spaces become underscores`() {
        assertEquals(ianaList.indexOf("America/New_York"), map("America/New York"))
        assertEquals(ianaList.indexOf("America/New_York"), map("america / new york"))
        assertEquals(ianaList.indexOf("America/Argentina/Buenos_Aires"), map("America/Buenos  Aires"))
        assertEquals("America/New_York", resolve("America/New York").webId)
    }

    // endregion

    // region table content

    @Test
    fun `when several entries share a clock time the label names the place`() {
        mapOf(
            "America/Toronto" to "America/New_York",        // Eastern Time, not Indiana (East)
            "America/Detroit" to "America/New_York",
            "America/Indiana/Marengo" to "America/Indiana/Indianapolis", // Indiana stays Indiana
            "America/Mexico_City" to "America/Monterrey",   // not Saskatchewan
            "America/Guatemala" to "America/Costa_Rica",    // Central America, not Chihuahua
            "America/Swift_Current" to "America/Regina",    // Saskatchewan
            "America/Caracas" to "America/La_Paz",          // Caracas, La Paz, not Manaus
            "America/Manaus" to "America/Cuiaba",           // Manaus stays Manaus
            "America/Recife" to "America/Sao_Paulo",        // Brasilia, not Buenos Aires
            "Europe/Madrid" to "Europe/Paris",
            "Europe/Prague" to "Europe/Belgrade",
            "Europe/Volgograd" to "Europe/Moscow",          // not Istanbul
            "Asia/Karachi" to "Asia/Tashkent",              // not Ekaterinburg
            "Asia/Aqtobe" to "Asia/Almaty",                 // Kazakhstan -> Astana
            "Asia/Jakarta" to "Asia/Bangkok",               // not Novosibirsk
            "Asia/Manila" to "Asia/Singapore",              // not Beijing
            "Asia/Ulaanbaatar" to "Asia/Irkutsk",
            "Asia/Pyongyang" to "Asia/Seoul",
            "Asia/Chita" to "Asia/Yakutsk",
        ).forEach { (web, ours) ->
            assertEquals(web, ours, ianaList[map(web)])
        }
    }

    @Test
    fun `the two tables together cover exactly the web list`() {
        assertEquals(webNames.toSet() - expectedClosest.keys, WEB_TO_CONSOLE_TIMEZONES.keys)
        assertEquals(expectedClosest, WEB_TO_CLOSEST_CONSOLE_TIMEZONES)
        assertTrue(
            "a zone must be in only one table",
            WEB_TO_CONSOLE_TIMEZONES.keys.none { it in WEB_TO_CLOSEST_CONSOLE_TIMEZONES }
        )
    }

    @Test
    fun `index 28 and 30 share Europe Belgrade and each city gets the entry that names it`() {
        assertEquals("Europe/Belgrade", ianaList[28])
        assertEquals("Europe/Belgrade", ianaList[30])
        listOf("Europe/Belgrade", "Europe/Bratislava", "Europe/Budapest", "Europe/Ljubljana", "Europe/Prague")
            .forEach { assertEquals(it, 28, map(it)) }
        listOf("Europe/Sarajevo", "Europe/Skopje", "Europe/Warsaw", "Europe/Zagreb")
            .forEach { assertEquals(it, 30, map(it)) }
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `index hint is ignored when the console list has changed`() {
        // Same list without index 0: index 30 is no longer Europe/Belgrade, so the first Belgrade wins.
        val shifted = ianaList.drop(1)
        assertEquals(shifted.indexOf("Europe/Belgrade"), map("Europe/Warsaw", shifted))
        assertTrue(errors.isEmpty())
    }

    /** Ids shared by several console entries; any other duplicate would make an entry unreachable. */
    @Test
    fun `only the known ids are shared by several console entries`() {
        val duplicates = ianaList.groupBy { it.lowercase() }.filterValues { it.size > 1 }.keys
        assertEquals(setOf("europe/belgrade"), duplicates)
    }

    @Test
    fun `every console index is used by the tables`() {
        val used = webNames.map { map(it) }.toSet()
        val unused = ianaList.indices.filter { it !in used }.map { "$it ${ianaList[it]}" }
        assertTrue("Console entries no web zone maps to: $unused", unused.isEmpty())
    }

    @Test
    fun `every web entry maps without error, and closest matches log one warning`() {
        webNames.forEach { name ->
            errors.clear()
            warnings.clear()
            val index = map(name)
            assertTrue("$name logged $errors", errors.isEmpty())
            val closest = expectedClosest[name]
            if (closest != null) {
                assertEquals(name, closest, ianaList[index])
                assertEquals(name, 1, warnings.size)
                assertEquals(name, MatchStep.CLOSEST, resolve(name).step)
            } else {
                assertEquals(name, WEB_TO_CONSOLE_TIMEZONES.getValue(name), ianaList[index])
                assertTrue("$name warned $warnings", warnings.isEmpty())
                assertEquals(name, MatchStep.TABLE, resolve(name).step)
            }
        }
    }

    // endregion

    // region legacy names

    @Test
    fun `legacy name maps to its current zone with a warning`() {
        mapOf(
            "US/Central" to "America/Chicago",
            "US/Eastern" to "America/New_York",
            "US/Mountain" to "America/Denver",
            "US/Hawaii" to "Pacific/Honolulu",
            "Asia/Kolkata" to "Asia/Calcutta", // the web sends the older Asia/Calcutta; Kolkata is the newer name
        ).forEach { (legacy, console) ->
            errors.clear()
            warnings.clear()
            assertEquals(legacy, console, ianaList[map(legacy)])
            assertTrue("$legacy logged $errors", errors.isEmpty())
            assertEquals(legacy, 1, warnings.size)
            assertTrue(warnings.single(), legacy in warnings.single())
            assertEquals(legacy, MatchStep.LEGACY, resolve(legacy).step)
        }
    }

    @Test
    fun `legacy name warning says which zone it was matched to`() {
        map("US/Central")
        assertTrue(warnings.single(), "America/Chicago" in warnings.single())
    }

    @Test
    fun `legacy name is matched ignoring case and spaces`() {
        assertEquals(chicagoIndex, map("  us/central "))
        assertEquals(MatchStep.LEGACY, resolve("us/central").step)
    }

    @Test
    fun `legacy name for a closest-table zone uses the closest entry`() {
        // US/Samoa is American Samoa, the same zone as Pacific/Pago_Pago.
        assertEquals(ianaList.indexOf("Pacific/Honolulu"), map("US/Samoa"))
        assertTrue(errors.isEmpty())
        assertEquals(1, warnings.size)
    }

    @Test
    fun `valid name with no equivalent in the tables falls back to Chicago with an error`() {
        // A fixed offset of GMT-5 has no daylight saving time, so its rules match no table zone.
        assertEquals(chicagoIndex, map("Etc/GMT+5"))
        assertEquals(1, errors.size)
        assertTrue(warnings.isEmpty())
    }

    // endregion

    // region fallback

    @Test
    fun `null empty and blank fall back to Chicago with an error`() {
        listOf(null, "", "   ").forEach { input ->
            errors.clear()
            assertEquals("input='$input'", chicagoIndex, map(input))
            assertEquals("input='$input'", 1, errors.size)
        }
    }

    @Test
    fun `unknown zone falls back to Chicago with an error`() {
        assertEquals(chicagoIndex, map("Not/AZone"))
        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("Not/AZone"))
    }

    @Test
    fun `table value missing from the console list falls back to Chicago with an error`() {
        val withoutTokyo = ianaList.filter { it != "Asia/Tokyo" }
        assertEquals(withoutTokyo.indexOf("America/Chicago"), map("Asia/Tokyo", withoutTokyo))
        assertEquals(1, errors.size)
        assertTrue(errors.single(), errors.single().contains("Asia/Tokyo"))
    }

    @Test
    fun `console list without Chicago falls back to index 0`() {
        assertEquals(0, map("Not/AZone", listOf("Pacific/Honolulu")))
    }

    // endregion

    // region printing

    @Test
    fun `describeMapping prints one row per entry with index and step`() {
        val names = listOf("America/Chicago", "Africa/Abidjan", "Pacific/Midway", "US/Central", "Not/AZone")
        val rows = WebTimeZoneMapper.describeMapping(names, ianaList, displayList).lines().filter { "->" in it }
        assertEquals(
            listOf(
                "America/Chicago -> [9] America/Chicago (TABLE) -> (GMT-06:00) Central Time (US and Canada)",
                "Africa/Abidjan -> [25] Africa/Monrovia (TABLE) -> (GMT) Casablanca, Monrovia",
                "Pacific/Midway -> [1] Pacific/Honolulu (CLOSEST) -> (GMT-10:00) Hawaii",
                "US/Central (legacy name for America/Chicago) -> [9] America/Chicago (LEGACY) -> " +
                    "(GMT-06:00) Central Time (US and Canada)",
                "Not/AZone (not in table) -> [9] America/Chicago (DEFAULT) -> (GMT-06:00) Central Time (US and Canada)",
            ),
            rows
        )
    }

    @Test
    fun `describeMapping shows the fixed-up IANA id, not the raw input`() {
        val table = WebTimeZoneMapper.describeMapping(listOf("america / new york"), ianaList, displayList)
        assertTrue(table, table.lines().single { "->" in it }.startsWith("America/New_York -> [12] "))
    }

    @Test
    fun `describeMapping without display strings leaves the last column out`() {
        val table = WebTimeZoneMapper.describeMapping(listOf("America/Chicago"), ianaList)
        assertEquals("America/Chicago -> [9] America/Chicago (TABLE)", table.lines().single { "->" in it })
    }

    /**
     * Keeps web_timezone_mapping_jira.md (the table to paste into Jira) in step with the code.
     * On failure the fresh file is written to build/web_timezone_mapping_jira.md; copy it over.
     */
    @Test
    fun `Jira markdown table matches the code`() {
        val actual = jiraMarkdown()
        File("build/web_timezone_mapping_jira.md").writeText(actual)
        assertEquals(
            "Out of date: copy build/web_timezone_mapping_jira.md to src/test/resources",
            WebTimeZoneTestData.resource("web_timezone_mapping_jira.md"),
            actual
        )
    }

    private fun jiraMarkdown(): String = buildString {
        appendLine("Web timezone to console timezone mapping.")
        appendLine()
        appendLine("- Rows marked **(closest)** have no console entry with the same clock time. The console uses the")
        appendLine("  closest entry instead (at most 1 hour off) and logs a warning.")
        appendLine("- **Legacy names** such as US/Central are not in this table. The console looks up the zone")
        appendLine("  with the same time rules (US/Central is America/Chicago), uses that row and logs a warning.")
        appendLine("- **Default: US Central (America/Chicago).** If the web sends a timezone that is not in")
        appendLine("  this table and is not a legacy name for one, the console uses US Central and logs an error.")
        appendLine()
        appendLine("About **index 28 and index 30**:")
        appendLine("- Both entries are backed by the same IANA timezone, Europe/Belgrade, so their clock time is the")
        appendLine("  same. Only their labels differ: 28 is \"Belgrade, Bratislava, Budapest, Ljubljana, Prague\" and")
        appendLine("  30 is \"Sarajevo, Skopje, Warsaw, Zagreb\".")
        appendLine("- The mapping normally finds the console entry by IANA id, which would always give 28. For these")
        appendLine("  cities it uses a fixed index instead, so each city gets the entry whose label names it:")
        appendLine("  Sarajevo, Skopje, Warsaw and Zagreb go to 30; Belgrade, Bratislava, Budapest, Ljubljana and")
        appendLine("  Prague go to 28. Podgorica and Tirane are in neither label and stay on 28.")
        appendLine("- The fixed index is only used while that entry is still Europe/Belgrade. If the console list is")
        appendLine("  reordered, the mapping uses the first Europe/Belgrade entry, which shows the same clock time.")
        appendLine()
        appendLine("| Web timezone | Index | Console timezone | Console display (English) |")
        appendLine("|---|---|---|---|")
        webNames.forEach { name ->
            val match = WebTimeZoneMapper.resolve(name, ianaList)
            val console = ianaList[match.index] + if (match.step == MatchStep.CLOSEST) " (closest)" else ""
            appendLine("| $name | ${match.index} | $console | ${displayList[match.index].replace("|", "\\|")} |")
        }
    }

    // endregion
}
