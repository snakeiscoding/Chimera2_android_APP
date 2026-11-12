package com.jht.interfaces.models

import com.jht.seca.data.SecaSurveyState

/**
 * True when a logged in user finished the PACE onboarding survey
 *
 * Kept here so Core/Interfaces sees a Boolean and never imports `com.jht.seca`.
 *
 * @param surveyState null when the user has no PACE data
 */
fun hasCompletedPaceSurvey(isGuest: Boolean, surveyState: SecaSurveyState?): Boolean =
    !isGuest && surveyState == SecaSurveyState.COMPLETED
