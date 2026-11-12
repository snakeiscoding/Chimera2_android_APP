package com.jht.workoutsummary.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.jht.theme.DynamicTheme
import com.jht.theme.DynamicThemeData
import com.jht.theme.darkDynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.HomeButton
import com.jht.widgets.WebViewWidget
import com.jht.widgets.WebViewWidgetOptions
import com.jht.widgets.fillScreenHeight
import com.jht.widgets.survey.SurveyBridgeMessage
import com.jht.widgets.survey.mlSurveyWebViewOptions

/**
 * Console header with Home over the full-bleed post workout survey page
 *
 * Leaves the area below the header empty while [PostWorkoutSurveyUiState.Waiting], and once
 * [PostWorkoutSurveyUiState.Leaving] so the page can't take input while the summary opens.
 */
@Composable
fun PostWorkoutSurveyScreen(
    state: PostWorkoutSurveyUiState,
    onSurveyMessage: (SurveyBridgeMessage) -> Unit,
    onSurveyLoadFailed: (errorCode: Int, errorDescription: String) -> Unit,
    onWebViewUnavailable: (error: Throwable) -> Unit,
    onHome: () -> Unit,
    jumpAccessToken: () -> String,
    jumpIdToken: () -> String,
    modifier: Modifier = Modifier,
    theme: DynamicThemeData = defaultDynamicTheme,
) {
    DynamicTheme(theme) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DynamicTheme.colors.background1)
                // lets uiautomator find HomeButton by its test tag
                .semantics { testTagsAsResourceId = true }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillScreenHeight(0.1f) // matches the summary header so the hand-off doesn't jump
                    .background(DynamicTheme.colors.panelTopBackground.brush()),
            ) {
                Row(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(30.dp, 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeButton(modifier = Modifier.aspectRatio(120 / 70f), onClick = onHome)
                }
            }
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = DynamicTheme.colors.gray3
            )
            Box(Modifier.fillMaxSize()) {
                when (state) {
                    PostWorkoutSurveyUiState.Waiting -> Unit
                    is PostWorkoutSurveyUiState.ShowingMl -> MlSurveyPage(
                        state = state,
                        accessToken = jumpAccessToken,
                        idToken = jumpIdToken,
                        onMessage = onSurveyMessage,
                        onLoadFailed = onSurveyLoadFailed,
                        onWebViewUnavailable = onWebViewUnavailable,
                        modifier = Modifier.matchParentSize(),
                    )
                    is PostWorkoutSurveyUiState.Showing -> SurveyPage(
                        url = state.url,
                        onLoadFailed = onSurveyLoadFailed,
                        onWebViewUnavailable = onWebViewUnavailable,
                        modifier = Modifier.matchParentSize(),
                    )
                    PostWorkoutSurveyUiState.Leaving -> Unit
                }
            }
        }
    }
}

/**
 * Loads [url] without auto granted location or DRM access
 *
 * Only a network failure on the first load counts as failed, so challenge pages that answer 403 or 503
 * and then reload, or error pages after submit, keep the user on the survey.
 */
@Composable
private fun SurveyPage(
    url: String,
    onLoadFailed: (errorCode: Int, errorDescription: String) -> Unit,
    onWebViewUnavailable: (error: Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(Color.White), contentAlignment = Alignment.Center) {
            Text(text = url, color = Color.DarkGray)
        }
        return
    }
    // the WebView keeps the first options it sees, so route the callback through updated state
    val currentOnLoadFailed by rememberUpdatedState(onLoadFailed)
    val currentOnWebViewUnavailable by rememberUpdatedState(onWebViewUnavailable)
    val options = remember(url) {
        WebViewWidgetOptions(
            onFirstLoadFailed = { code, description -> currentOnLoadFailed(code, description) },
            onCreateFailed = { currentOnWebViewUnavailable(it) },
            autoGrantPermissions = false,
        )
    }
    WebViewWidget(url = url, modifier = modifier, options = options)
}

/** Loads the ML survey with JUMP auth and the Apollo bridge, both limited to the survey's origin */
@Composable
private fun MlSurveyPage(
    state: PostWorkoutSurveyUiState.ShowingMl,
    accessToken: () -> String,
    idToken: () -> String,
    onMessage: (SurveyBridgeMessage) -> Unit,
    onLoadFailed: (errorCode: Int, errorDescription: String) -> Unit,
    onWebViewUnavailable: (error: Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(Color.White), contentAlignment = Alignment.Center) {
            Text(text = state.url, color = Color.DarkGray)
        }
        return
    }
    // built once per URL because the WebView is created once
    val options = remember(state.url) {
        mlSurveyWebViewOptions(
            url = state.url,
            accessToken = accessToken,
            idToken = idToken,
            onMessage = onMessage,
            onLoadFailed = onLoadFailed,
            onCreateFailed = onWebViewUnavailable,
        )
    }
    WebViewWidget(url = state.url, modifier = modifier, options = options)
}

//region Previews
@Composable
private fun PreviewSurvey(state: PostWorkoutSurveyUiState, theme: DynamicThemeData = defaultDynamicTheme) {
    PostWorkoutSurveyScreen(
        state = state,
        onSurveyMessage = {},
        onSurveyLoadFailed = { _, _ -> },
        onWebViewUnavailable = {},
        onHome = {},
        jumpAccessToken = { "access" },
        jumpIdToken = { "id" },
        theme = theme,
    )
}

@AllDevicePreviews
@Composable
private fun PreviewPostWorkoutSurveyShowing() = PreviewSurvey(
    state = PostWorkoutSurveyUiState.Showing(url = "https://www.surveymonkey.com/r/abc"),
)

@AllDevicePreviews
@Composable
private fun PreviewPostWorkoutSurveyShowingDark() = PreviewSurvey(
    state = PostWorkoutSurveyUiState.Showing(url = "https://www.surveymonkey.com/r/abc"),
    theme = darkDynamicTheme,
)

@AllDevicePreviews
@Composable
private fun PreviewPostWorkoutSurveyLongUrl() = PreviewSurvey(
    state = PostWorkoutSurveyUiState.Showing(
        url = "https://surveys.example.com/clubs/1964/post-workout/feedback?campaign=autumn-2026&source=console&variant=b",
    ),
)

@AllDevicePreviews
@Composable
private fun PreviewPostWorkoutSurveyShowingMl() = PreviewSurvey(
    state = PostWorkoutSurveyUiState.ShowingMl(
        url = "https://dev-matrixworkouts.jfit.co/survey/after-exercise?exercise_id=manual%3A1234&facility_id=1964&lang=en",
    ),
)

@AllDevicePreviews
@Composable
private fun PreviewPostWorkoutSurveyShowingMlDark() = PreviewSurvey(
    state = PostWorkoutSurveyUiState.ShowingMl(
        url = "https://dev-matrixworkouts.jfit.co/survey/after-exercise?exercise_id=manual%3A1234&facility_id=1964&lang=zh-TW",
    ),
    theme = darkDynamicTheme,
)

@AllDevicePreviews
@Composable
private fun PreviewPostWorkoutSurveyLeaving() = PreviewSurvey(state = PostWorkoutSurveyUiState.Leaving)
//endregion Previews
