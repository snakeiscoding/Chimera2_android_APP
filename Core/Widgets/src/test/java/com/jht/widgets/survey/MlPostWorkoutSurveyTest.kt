package com.jht.widgets.survey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MlPostWorkoutSurveyTest {

    private fun applies(
        url: String = "https://survey.example.com/pace",
        isGuest: Boolean = false,
        hasCompletedPaceSurvey: Boolean = true,
        clubId: Int = 42,
    ) = MlPostWorkoutSurvey.applies(
        surveyUrl = url,
        isGuest = isGuest,
        hasCompletedPaceSurvey = hasCompletedPaceSurvey,
        clubId = clubId,
    )

    @Test fun appliesForLoggedInUserWithCompletedSurvey() = assertTrue(applies())

    @Test fun notForUserWithoutCompletedSurvey() = assertFalse(applies(hasCompletedPaceSurvey = false))

    @Test fun notForGuest() = assertFalse(applies(isGuest = true))

    @Test fun notForEmptyUrl() = assertFalse(applies(url = ""))

    @Test fun notForBlankUrl() = assertFalse(applies(url = "   "))

    @Test fun notForZeroClubId() = assertFalse(applies(clubId = 0))

    @Test fun notForNegativeClubId() = assertFalse(applies(clubId = -1))

    @Test fun appliesForClubIdOne() = assertTrue(applies(clubId = 1))

    @Test
    fun buildsUrlWithEncodedParams() {
        val url = MlPostWorkoutSurvey.buildUrl(
            baseUrl = "https://survey.example.com/pace",
            programUuid = "manual:1234-abcd",
            facilityId = 42,
            locale = "en_US",
        )
        assertEquals(
            "https://survey.example.com/pace?exercise_id=manual%3A1234-abcd&facility_id=42&lang=en",
            url,
        )
    }

    @Test
    fun encodesEveryColonInProgramUuid() {
        val url = MlPostWorkoutSurvey.buildUrl(
            baseUrl = "https://s.example.com",
            programUuid = "ml:a:b",
            facilityId = 7,
            locale = "de_DE",
        )
        assertEquals("https://s.example.com?exercise_id=ml%3Aa%3Ab&facility_id=7&lang=de", url)
    }

    @Test
    fun appendsWithAmpersandWhenBaseHasQuery() {
        val url = MlPostWorkoutSurvey.buildUrl(
            baseUrl = "https://s.example.com/p?src=console",
            programUuid = "manual:x",
            facilityId = 1,
            locale = "en_US",
        )
        assertEquals("https://s.example.com/p?src=console&exercise_id=manual%3Ax&facility_id=1&lang=en", url)
    }

    @Test
    fun doesNotDoubleSeparatorWhenBaseEndsWithQuestionMark() {
        val url = MlPostWorkoutSurvey.buildUrl(
            baseUrl = "https://s.example.com/p?",
            programUuid = "manual:x",
            facilityId = 1,
            locale = "en_US",
        )
        assertEquals("https://s.example.com/p?exercise_id=manual%3Ax&facility_id=1&lang=en", url)
    }

    @Test fun mapsEnglish() = assertEquals("en", MlPostWorkoutSurvey.languageCode("en_US"))

    @Test fun mapsGerman() = assertEquals("de", MlPostWorkoutSurvey.languageCode("de_DE"))

    @Test fun keepsRegionForSimplifiedChinese() = assertEquals("zh-CN", MlPostWorkoutSurvey.languageCode("zh_CN"))

    @Test fun keepsRegionForTraditionalChinese() = assertEquals("zh-TW", MlPostWorkoutSurvey.languageCode("zh_TW"))

    @Test fun passesLanguageOnlyLocaleThrough() = assertEquals("fr", MlPostWorkoutSurvey.languageCode("fr"))

    @Test fun fallsBackToEnglishForEmptyLocale() = assertEquals("en", MlPostWorkoutSurvey.languageCode(""))
}
