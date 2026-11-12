package com.jht.firsttimesetup.mappers

import java.io.File

/** Shared fixtures for the web timezone tests, read from the real resources where possible. */
internal object WebTimeZoneTestData {

    private val arraysXml: String by lazy {
        val xml = File("../../Chimera/src/main/res/values/arrays.xml")
        check(xml.exists()) { "Cannot find ${xml.absolutePath}" }
        xml.readText()
    }

    private fun stringArray(name: String): List<String> {
        val body = arraysXml.substringAfter("<string-array name=\"$name\">").substringBefore("</string-array>")
        return Regex("<item>([^<]+)</item>").findAll(body).map { it.groupValues[1].trim() }.toList()
    }

    /** The console's `ianaTimezone` array. */
    val ianaList: List<String> by lazy { stringArray("ianaTimezone") }

    /**
     * English `timezoneDisplay` text, index for index with [ianaList]. Keys come from arrays.xml,
     * text from a fixture because the translations catalogue is a prebuilt artifact.
     */
    val displayList: List<String> by lazy {
        val english = resource("timezone_display_en.txt")
            .lines().filter { it.isNotBlank() && !it.startsWith("#") }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
        stringArray("timezoneDisplay").map { item ->
            val key = item.removePrefix("@string/")
            english[key] ?: error("timezone_display_en.txt has no text for '$key'")
        }
    }

    /** The web list, e.g. `America/Chicago`. */
    val webNames: List<String> by lazy {
        resource("web_timezones.txt").lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Web zones with no console entry at the same clock time all year, and the closest entry they
     * use instead (smallest difference over January and July). Differences are as of 2026.
     */
    val expectedClosest = mapOf(
        "America/Adak" to "Pacific/Honolulu",       // 0 / 1 h
        "Pacific/Gambier" to "America/Anchorage",   // 0 / 1 h
        "Pacific/Marquesas" to "Pacific/Honolulu",  // 30 min
        "Pacific/Midway" to "Pacific/Honolulu",     // 1 h
        "Pacific/Niue" to "Pacific/Honolulu",       // 1 h
        "Pacific/Pago_Pago" to "Pacific/Honolulu",  // 1 h
        "Australia/Eucla" to "Asia/Tokyo",          // 15 min
        "Australia/Lord_Howe" to "Australia/Sydney", // 0 / 30 min
        "Pacific/Chatham" to "Pacific/Tongatapu",   // 45 / 15 min
        "Pacific/Kiritimati" to "Pacific/Tongatapu", // 1 h
        // +6: "Astana, Dhaka" is backed by Asia/Almaty, which moved to +5 in 2024.
        "Asia/Bishkek" to "Asia/Kathmandu",         // 15 min
        "Asia/Dhaka" to "Asia/Kathmandu",
        "Asia/Omsk" to "Asia/Kathmandu",
        "Asia/Thimphu" to "Asia/Kathmandu",
        "Asia/Urumqi" to "Asia/Kathmandu",
        "Indian/Chagos" to "Asia/Kathmandu",
        // Match only part of the year, because their daylight saving time differs.
        "America/Miquelon" to "America/St_Johns",   // 30 min / 30 min
        "Antarctica/Troll" to "Europe/Berlin",      // 1 h / 0
        "Pacific/Easter" to "America/Regina",       // 1 h / 0
        "Pacific/Norfolk" to "Pacific/Guadalcanal", // 1 h / 0
        "Pacific/Pitcairn" to "America/Anchorage",  // 1 h / 0
    )

    fun resource(name: String): String =
        javaClass.classLoader!!.getResource(name)?.readText() ?: error("Missing test resource $name")
}
