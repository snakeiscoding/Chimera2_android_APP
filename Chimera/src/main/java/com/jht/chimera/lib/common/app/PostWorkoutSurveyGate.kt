package com.jht.chimera.lib.common.app

import android.util.Log
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.webapp.JumpTokenHost
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.models.hasCompletedPaceSurvey
import com.jht.progression.MmActivity
import com.jht.seca.data.SecaUserData
import com.jht.widgets.survey.MlPostWorkoutSurvey

private const val TAG = "PostWorkoutSurveyGate"

/** Where a workout summary launch should go */
sealed interface WorkoutSummaryDestination {
    /** Show the post workout survey first; [target] is the summary it opens next, empty for the default */
    data class Survey(val target: String) : WorkoutSummaryDestination

    /** Open the summary directly; [surveyShown] marks that the survey already ran */
    data class Summary(val target: String, val surveyShown: Boolean) : WorkoutSummaryDestination
}

/**
 * Decides whether a workout summary launch goes through the post workout survey
 *
 * @param surveyApplies evaluated only when the survey hasn't run yet for this workout
 */
class PostWorkoutSurveyGate(private val surveyApplies: () -> Boolean) {

    /**
     * @param target summary activity name, null or empty for the default summary. Progression's
     *   [MmActivity] returns to itself this way and never gets the survey.
     * @param surveyShown true when called after the survey, which stops it looping
     */
    fun route(target: String?, surveyShown: Boolean): WorkoutSummaryDestination {
        val summary = target ?: ""
        return if (!surveyShown && summary !in NOT_SUMMARIES && surveyApplies()) {
            WorkoutSummaryDestination.Survey(target = summary)
        } else {
            WorkoutSummaryDestination.Summary(target = summary, surveyShown = surveyShown)
        }
    }

    companion object {
        // apps that return to themselves through the summary launch, so the survey would land mid flow
        private val NOT_SUMMARIES = setOf(MmActivity::class.java.canonicalName)

        /**
         * True when the survey goes in front of the summary
         *
         * Retail keeps its opt-in survey inside the summary, so only commercial consoles get this screen.
         *
         * @param enabled the Post Survey URL setting
         * @param mlSurveyApplies the ML survey applies, see [mlSurveyApplies]; it shows on its own
         *   even with the Post Survey URL off
         */
        fun surveyApplies(isRetail: Boolean, enabled: Boolean, url: String, mlSurveyApplies: Boolean): Boolean =
            !isRetail && (mlSurveyApplies || (enabled && url.isNotBlank()))

        /** True when the summary offers the survey itself, which only retail does */
        @JvmStatic
        fun showsInSummarySurvey(isRetail: Boolean, enabled: Boolean): Boolean = isRetail && enabled

        /** Gate backed by the live DataMatrix settings and login state */
        @JvmStatic
        fun fromDataMatrix() = PostWorkoutSurveyGate(surveyApplies = {
            surveyApplies(
                isRetail = ProductConfig.isRetail(),
                enabled = DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL),
                url = DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING),
                mlSurveyApplies = mlSurveyAppliesFromDataMatrix(),
            )
        })

        /**
         * True when the ML survey goes in front of the summary
         *
         * Also needs [surveyUrl] on a host that may get JUMP tokens, see [allowedMlSurveyUrl].
         */
        fun mlSurveyApplies(surveyUrl: String, isGuest: Boolean, hasCompletedPaceSurvey: Boolean, clubId: Int): Boolean =
            MlPostWorkoutSurvey.applies(
                surveyUrl = allowedMlSurveyUrl(surveyUrl),
                isGuest = isGuest,
                hasCompletedPaceSurvey = hasCompletedPaceSurvey,
                clubId = clubId,
            )

        /** [url] when [JumpTokenHost.allows] it, else empty so the ML survey is skipped */
        @JvmStatic
        fun allowedMlSurveyUrl(url: String): String {
            if (url.isEmpty() || JumpTokenHost.allows(url)) return url
            Log.w(TAG, "Skipping ML survey, JUMP tokens not allowed for $url") //NON-NLS
            return ""
        }

        private fun mlSurveyAppliesFromDataMatrix(): Boolean {
            val isGuest = DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL)
            val secaUserData = DataMatrix.getParcelable(DAPIKeys.DATA_SECA_USER_INFO_PARCELABLE, SecaUserData::class.java)
            return mlSurveyApplies(
                surveyUrl = DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_ML_POST_WORKOUT_SURVEY_URL_STRING),
                isGuest = isGuest,
                hasCompletedPaceSurvey = hasCompletedPaceSurvey(isGuest = isGuest, surveyState = secaUserData?.surveyState),
                clubId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT),
            )
        }
    }
}
