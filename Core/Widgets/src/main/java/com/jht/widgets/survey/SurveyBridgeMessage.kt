package com.jht.widgets.survey

import org.json.JSONException
import org.json.JSONObject

/**
 * Message posted by the survey page through `window.ApolloSurveyBridge.postMessage`
 *
 * Every message closes the survey, including ones we can't read, so a page bug never
 * strands the user in front of the summary.
 */
sealed interface SurveyBridgeMessage {
    /** True when the console should leave the survey for the summary */
    val closesSurvey: Boolean get() = true

    /** The user submitted the survey */
    data object Complete : SurveyBridgeMessage

    /** The page reported a failure; [type] is the full `survey-failed-*` value, kept for logs */
    data class Failed(val type: String) : SurveyBridgeMessage

    /** Anything else, including non-JSON input; [raw] is kept for logs */
    data class Unknown(val raw: String) : SurveyBridgeMessage

    companion object {
        private const val TYPE_COMPLETE = "survey-complete"
        private const val TYPE_FAILED_PREFIX = "survey-failed-"

        /** Parses a JSON `{"type": ...}` message; never throws */
        fun parse(raw: String?): SurveyBridgeMessage {
            if (raw == null) return Unknown("")
            val type = try {
                JSONObject(raw).optString("type")
            } catch (e: JSONException) {
                return Unknown(raw)
            }
            return when {
                type == TYPE_COMPLETE -> Complete
                type.startsWith(TYPE_FAILED_PREFIX) -> Failed(type)
                else -> Unknown(raw)
            }
        }
    }
}
