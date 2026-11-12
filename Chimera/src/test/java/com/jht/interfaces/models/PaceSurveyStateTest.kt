package com.jht.interfaces.models

import com.jht.seca.data.SecaSurveyState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceSurveyStateTest {

    @Test fun completedForLoggedInUser() =
        assertTrue(hasCompletedPaceSurvey(isGuest = false, surveyState = SecaSurveyState.COMPLETED))

    @Test fun notForDeclined() =
        assertFalse(hasCompletedPaceSurvey(isGuest = false, surveyState = SecaSurveyState.DECLINED))

    @Test fun notForShowSurvey() =
        assertFalse(hasCompletedPaceSurvey(isGuest = false, surveyState = SecaSurveyState.SHOW_SURVEY))

    @Test fun notWithoutSecaData() =
        assertFalse(hasCompletedPaceSurvey(isGuest = false, surveyState = null))

    @Test fun notForGuest() =
        assertFalse(hasCompletedPaceSurvey(isGuest = true, surveyState = SecaSurveyState.COMPLETED))
}
