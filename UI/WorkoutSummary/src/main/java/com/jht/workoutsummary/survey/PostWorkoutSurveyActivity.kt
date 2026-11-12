package com.jht.workoutsummary.survey

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.interfaces.models.PostWorkoutSurveyExtras
import com.jht.widgets.RestringThemedActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Post workout survey shown in front of the workout summary
 *
 * Launched by the summary gate with [PostWorkoutSurveyExtras.TARGET_ACTIVITY]; opens that summary when it closes.
 */
@AndroidEntryPoint
class PostWorkoutSurveyActivity : RestringThemedActivity() {
    override val themedViewModel: PostWorkoutSurveyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by themedViewModel.state.collectAsStateWithLifecycle()
            PostWorkoutSurveyScreen(
                state = state,
                onSurveyMessage = themedViewModel::onSurveyMessage,
                onSurveyLoadFailed = themedViewModel::onSurveyLoadFailed,
                onWebViewUnavailable = themedViewModel::onWebViewUnavailable,
                onHome = themedViewModel::onHome,
                jumpAccessToken = themedViewModel::jumpAccessToken,
                jumpIdToken = themedViewModel::jumpIdToken,
                theme = themedViewModel.theme.value,
            )
        }
    }
}
