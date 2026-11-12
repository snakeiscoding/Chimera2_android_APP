package com.jht.firsttimesetup.mappers

import com.jht.firsttimesetup.WEB_TO_CLOSEST_CONSOLE_TIMEZONES
import com.jht.firsttimesetup.WEB_TO_CONSOLE_TIMEZONES
import com.jht.firsttimesetup.mappers.WebTimeZoneTestData.ianaList
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Checks that each table entry still shows the right clock time, and that each closest entry is
 * still the closest. It fails when a country changes its time rules (as Kazakhstan did in 2024)
 * and the table needs a new choice.
 *
 * Needs a JDK with time zone data 2025b or newer. On an older JDK these tests are skipped, not
 * failed, so the rest of the suite still runs on it.
 */
class WebTimeZoneTableFreshnessTest {

    companion object {
        /** Year whose January and July clock times are compared. */
        private const val YEAR = 2026

        /** America/Coyhaique, the newest zone in the web list, first appeared in 2025b. */
        private const val MIN_TZDATA = "2025b"

        @BeforeClass
        @JvmStatic
        fun requireCurrentTzdata() {
            // ZoneRulesProvider is JVM-only (absent from android.jar), so reach it reflectively.
            val versions = Class.forName("java.time.zone.ZoneRulesProvider")
                .getMethod("getVersions", String::class.java)
                .invoke(null, "UTC") as java.util.NavigableMap<*, *>
            val version = versions.lastKey() as String
            assumeTrue(
                "Skipped: JDK time zone data $version is older than $MIN_TZDATA " +
                    "(java.home=${System.getProperty("java.home")}). Use a newer JDK, e.g. Android Studio's JBR.",
                version >= MIN_TZDATA
            )
        }
    }

    private fun offset(zone: String, at: Instant): ZoneOffset = ZoneId.of(zone).rules.getOffset(at)

    private fun midMonth(month: Int): Instant =
        LocalDate.of(YEAR, month, 15).atStartOfDay().toInstant(ZoneOffset.UTC)

    @Test
    fun `every table entry matches in January and July, including daylight saving time`() {
        val january = midMonth(1)
        val july = midMonth(7)
        val wrong = WEB_TO_CONSOLE_TIMEZONES.filter { (web, console) ->
            offset(web, january) != offset(console, january) || offset(web, july) != offset(console, july)
        }.map { (web, console) -> "$web -> $console" }
        assertTrue("Clock time differs in January or July:\n" + wrong.joinToString("\n"), wrong.isEmpty())
    }

    /** Total clock-time difference in seconds between two zones, over January and July. */
    private fun difference(a: String, b: String): Int = listOf(midMonth(1), midMonth(7)).sumOf { at ->
        kotlin.math.abs(offset(a, at).totalSeconds - offset(b, at).totalSeconds)
    }

    @Test
    fun `every closest entry is still the closest, and still not exact`() {
        val wrong = WEB_TO_CLOSEST_CONSOLE_TIMEZONES.mapNotNull { (web, console) ->
            val best = ianaList.minOf { difference(web, it) }
            when {
                best == 0 -> "$web now has an exact console entry; move it to WEB_TO_CONSOLE_TIMEZONES"
                difference(web, console) != best ->
                    "$web -> $console is ${difference(web, console) / 60} min off; closest is ${best / 60} min " +
                        "(${ianaList.filter { difference(web, it) == best }})"
                else -> null
            }
        }
        assertTrue(wrong.joinToString("\n"), wrong.isEmpty())
    }

    /** When several entries are equally close, one in the web zone's region (`Australia/`, `Pacific/`…) wins. */
    @Test
    fun `ties between closest entries are won by the same region`() {
        val region = { zone: String -> zone.substringBefore('/') }
        val wrong = WEB_TO_CLOSEST_CONSOLE_TIMEZONES.mapNotNull { (web, console) ->
            val best = ianaList.minOf { difference(web, it) }
            val tied = ianaList.filter { difference(web, it) == best }
            val sameRegion = tied.filter { region(it) == region(web) }
            val wrongRegion = sameRegion.isNotEmpty() && region(console) != region(web)
            if (wrongRegion) "$web -> $console, but $sameRegion tie" else null
        }
        assertTrue(wrong.joinToString("\n"), wrong.isEmpty())
    }
}
