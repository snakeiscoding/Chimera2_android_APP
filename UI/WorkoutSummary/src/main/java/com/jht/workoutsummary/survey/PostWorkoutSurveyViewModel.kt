package com.jht.workoutsummary.survey

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.jht.annotations.MainDispatcher
import com.jht.enums.UiEvent
import com.jht.interfaces.models.IdleTimerModel
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.PostWorkoutSurveyExtras
import com.jht.interfaces.models.SystemModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UserModel
import com.jht.interfaces.models.WorkoutSummaryModel
import com.jht.theme.ThemedViewModel
import com.jht.theme.defaultDynamicTheme
import com.jht.widgets.survey.MlPostWorkoutSurvey
import com.jht.widgets.survey.SurveyBridgeMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PostWorkoutSurvey"

/** UI state for [PostWorkoutSurveyScreen] */
sealed interface PostWorkoutSurveyUiState {
    /** ML survey is waiting for the 701 program uuid it is keyed on */
    data object Waiting : PostWorkoutSurveyUiState

    /** Post Survey URL page is up at [url], loaded as configured */
    data class Showing(val url: String) : PostWorkoutSurveyUiState

    /**
     * ML survey page is up at [url], which already carries `exercise_id`, `facility_id` and `lang`
     *
     * Tokens aren't held here; read them through [PostWorkoutSurveyViewModel.jumpAccessToken] so a reload gets refreshed ones.
     */
    data class ShowingMl(val url: String) : PostWorkoutSurveyUiState

    /** Summary launch requested; nothing else should happen */
    data object Leaving : PostWorkoutSurveyUiState
}

/**
 * Runs the post workout survey that sits in front of the workout summary
 *
 * The launch gate already decided a survey applies. The ML survey wins over the Post Survey URL
 * whenever it applies. Every exit (Home, idle timeout, page load failure, a bridge message, no WebView,
 * no URL, or no program uuid in time) opens the summary named by [PostWorkoutSurveyExtras.TARGET_ACTIVITY],
 * exactly once. If the summary never takes over, splash does after the screen timeout.
 */
@HiltViewModel
class PostWorkoutSurveyViewModel @Inject constructor(
    private val systemModel: SystemModel,
    private val userModel: UserModel,
    private val uiBaseModel: UIBaseModel,
    private val navigationModel: NavigationModel,
    private val idleTimerModel: IdleTimerModel,
    private val summaryModel: WorkoutSummaryModel,
    savedStateHandle: SavedStateHandle,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : ThemedViewModel() {
    /** Theme for the console header */
    val theme by lazy { mutableStateOf(defaultDynamicTheme) }

    private val _state = MutableStateFlow<PostWorkoutSurveyUiState>(
        if (mlSurveyApplies()) PostWorkoutSurveyUiState.Waiting
        else PostWorkoutSurveyUiState.Showing(url = systemModel.postWorkoutSurveyUrl)
    )
    /** Current survey state; ends at [PostWorkoutSurveyUiState.Leaving] */
    val state: StateFlow<PostWorkoutSurveyUiState> = _state.asStateFlow()

    private val target: String = savedStateHandle.get<String>(PostWorkoutSurveyExtras.TARGET_ACTIVITY) ?: ""

    private var splashFallback: Job? = null
    private var uuidWait: Job? = null

    override fun start() {
        uiBaseModel.themeUri.listenAndTrigger { theme.value = loadTheme(it) }
        // the console treats the survey as the summary, so match its resume
        systemModel.sendUiEvent(UiEvent.WORKOUT_SUMMARY)
        systemModel.enablePhysicalHomeKey()
        // settings can change between the gate and here, so don't show a blank page
        val current = _state.value
        if (current is PostWorkoutSurveyUiState.Showing && current.url.isBlank()) {
            leave(reason = "no url")
            return
        }
        val timeoutSeconds = systemModel.screenTimeoutSeconds
        if (timeoutSeconds > 0) {
            idleTimerModel.addIdleTimer(TAG, timeoutSeconds * 1000) { leave(reason = "idle timeout") }
            idleTimerModel.startIdleTimers()
        }
        if (_state.value == PostWorkoutSurveyUiState.Waiting) waitForProgramUuid()
    }

    override fun stop() {
        // pausing means the next screen took over
        splashFallback?.cancel()
        uuidWait?.cancel()
        systemModel.close()
        uiBaseModel.close()
        idleTimerModel.close()
        summaryModel.close()
    }

    /** Current JUMP access token, read on every call so an ML survey reload picks up a refresh */
    fun jumpAccessToken(): String = userModel.jumpAccessToken

    /** Current JUMP id token, read on every call like [jumpAccessToken] */
    fun jumpIdToken(): String = userModel.jumpIdToken

    /** Any message from the ML survey page closes it */
    fun onSurveyMessage(message: SurveyBridgeMessage) {
        leave(reason = "bridge $message")
    }

    /** The page failed to load, so skip straight to the summary */
    fun onSurveyLoadFailed(errorCode: Int, errorDescription: String) {
        leave(reason = "load failed $errorCode '$errorDescription'")
    }

    /** The WebView could not be created, so skip straight to the summary */
    fun onWebViewUnavailable(error: Throwable) {
        Log.e(TAG, "Survey WebView unavailable", error) //NON-NLS
        leave(reason = "no webview")
    }

    /** Home leaves the survey for the summary rather than skipping the summary */
    fun onHome() {
        if (leave(reason = "home")) systemModel.reportHomeButtonPress()
    }

    private fun mlSurveyApplies(): Boolean = MlPostWorkoutSurvey.applies(
        surveyUrl = systemModel.mlPostWorkoutSurveyUrl,
        isGuest = userModel.isGuest,
        hasCompletedPaceSurvey = userModel.hasCompletedPaceSurvey,
        clubId = systemModel.facilityID.value,
    )

    private fun waitForProgramUuid() {
        // the 701 is built in parallel with this launch, so the uuid can land a moment later
        summaryModel.lastProgramUuid.listenAndTrigger(::onProgramUuid)
        if (_state.value != PostWorkoutSurveyUiState.Waiting) return
        uuidWait?.cancel()
        uuidWait = viewModelScope.launch(mainDispatcher) {
            delay(PROGRAM_UUID_WAIT_MS)
            if (_state.value == PostWorkoutSurveyUiState.Waiting) leave(reason = "no program uuid")
        }
    }

    private fun onProgramUuid(programUuid: String) {
        if (programUuid.isEmpty()) return
        val url = MlPostWorkoutSurvey.buildUrl(
            baseUrl = systemModel.mlPostWorkoutSurveyUrl,
            programUuid = programUuid,
            facilityId = systemModel.facilityID.value,
            locale = systemModel.currentLocale,
        )
        val showing = PostWorkoutSurveyUiState.ShowingMl(url = url)
        if (!_state.compareAndSet(PostWorkoutSurveyUiState.Waiting, showing)) return
        uuidWait?.cancel()
        // tokens go in by script, never the URL, so this is safe to log for field checks
        Log.i(TAG, "Showing ML survey: $url") //NON-NLS
    }

    /** Returns false when another exit already won */
    private fun leave(reason: String): Boolean {
        val current = _state.value
        if (current == PostWorkoutSurveyUiState.Leaving) return false
        // a timer callback can race a Home tap
        if (!_state.compareAndSet(current, PostWorkoutSurveyUiState.Leaving)) return false
        Log.i(TAG, "Leaving survey: $reason") //NON-NLS
        uuidWait?.cancel()
        navigationModel.launchWorkoutSummaryAfterSurvey(target)
        val fallbackSeconds = systemModel.screenTimeoutSeconds.takeIf { it > 0 } ?: DEFAULT_FALLBACK_SECONDS
        splashFallback = viewModelScope.launch(mainDispatcher) {
            delay(fallbackSeconds * 1000L)
            Log.w(TAG, "Summary never opened, going to splash") //NON-NLS
            navigationModel.launchSplashScreen()
        }
        return true
    }

    internal companion object {
        /** Splash fallback delay when the screen timeout is off */
        const val DEFAULT_FALLBACK_SECONDS = 60

        /** How long the ML survey waits for the 701 program uuid before skipping to the summary */
        const val PROGRAM_UUID_WAIT_MS = 3_000L
    }
}
