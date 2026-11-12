package com.jht.widgets.survey

import java.net.URLEncoder

/**
 * Decides whether the ML post workout survey applies and builds its URL
 *
 * Pure Kotlin so the launch gate in the app and the survey screen share one rule set.
 */
object MlPostWorkoutSurvey {

    /**
     * Returns true when the ML survey should be shown before the workout summary
     *
     * @param surveyUrl the configured `mlAfterExerciseSurvey` system setting; blank disables the survey
     * @param hasCompletedPaceSurvey true only when the user finished the PACE onboarding survey
     * @param clubId the console's facility id; the web ties responses to it, so <= 0 disables the survey
     */
    fun applies(
        surveyUrl: String,
        isGuest: Boolean,
        hasCompletedPaceSurvey: Boolean,
        clubId: Int,
    ): Boolean = surveyUrl.isNotBlank() && !isGuest && hasCompletedPaceSurvey && clubId > 0

    /**
     * Appends `exercise_id`, `facility_id` and `lang` to [baseUrl], URL-encoded
     *
     * @param programUuid the program uuid sent in the 701, e.g. `manual:<uuid>`
     * @param locale the console locale, e.g. `en_US`; see [languageCode]
     */
    fun buildUrl(
        baseUrl: String,
        programUuid: String,
        facilityId: Int,
        locale: String,
    ): String {
        val separator = when {
            !baseUrl.contains('?') -> "?"
            baseUrl.endsWith('?') || baseUrl.endsWith('&') -> ""
            else -> "&"
        }
        val query = listOf(
            "exercise_id" to programUuid,
            "facility_id" to facilityId.toString(),
            "lang" to languageCode(locale),
        ).joinToString("&") { (key, value) -> "$key=${encode(value)}" }
        return baseUrl + separator + query
    }

    /**
     * Maps a console locale like `de_DE` to the survey's language code
     *
     * Chinese keeps its region (`zh-CN`, `zh-TW`) since the survey has both scripts; everything
     * else is the bare language. An empty locale falls back to `en`.
     */
    fun languageCode(locale: String): String {
        val parts = locale.split('_', '-')
        val language = parts[0].lowercase()
        if (language.isEmpty()) return "en"
        val region = parts.getOrNull(1)?.uppercase()
        return if (language == "zh" && !region.isNullOrEmpty()) "$language-$region" else language
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
