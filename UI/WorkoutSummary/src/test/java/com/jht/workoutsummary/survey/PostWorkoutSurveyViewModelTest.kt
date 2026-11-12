package com.jht.workoutsummary.survey

import androidx.lifecycle.SavedStateHandle
import com.jht.enums.UiEvent
import com.jht.interfaces.models.PostWorkoutSurveyExtras
import com.jht.mock.interfaces.MockIntValue
import com.jht.mock.interfaces.MockStringValue
import com.jht.mock.interfaces.models.MockIdleTimerModel
import com.jht.mock.interfaces.models.MockNavigationModel
import com.jht.mock.interfaces.models.MockSystemModel
import com.jht.mock.interfaces.models.MockUIBaseModel
import com.jht.mock.interfaces.models.MockUserModel
import com.jht.mock.interfaces.models.MockWorkoutSummaryModel
import com.jht.widgets.survey.SurveyBridgeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric because ThemedViewModel.loadTheme parses a Uri
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PostWorkoutSurveyViewModelTest {

    private val navigation = MockNavigationModel()
    private val idleTimers = MockIdleTimerModel()
    private val dispatcher = StandardTestDispatcher()
    private val programUuid = MockStringValue("")
    private lateinit var system: MockSystemModel
    private lateinit var user: MockUserModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        target: String? = TARGET,
        surveyUrl: String = SURVEY_URL,
        screenTimeoutSeconds: Int = 60,
        mlSurveyUrl: String = "",
        isGuest: Boolean = false,
        hasCompletedPaceSurvey: Boolean = true,
        facilityId: Int = 42,
    ): PostWorkoutSurveyViewModel {
        system = MockSystemModel(
            postWorkoutSurveyUrl = surveyUrl,
            screenTimeoutSeconds = screenTimeoutSeconds,
            mlPostWorkoutSurveyUrl = mlSurveyUrl,
            facilityID = MockIntValue(facilityId),
            currentLocale = "zh_TW",
        )
        user = MockUserModel(
            isGuest = isGuest,
            hasCompletedPaceSurvey = hasCompletedPaceSurvey,
            jumpAccessToken = "access",
            jumpIdToken = "id",
        )
        return PostWorkoutSurveyViewModel(
            systemModel = system,
            userModel = user,
            uiBaseModel = MockUIBaseModel(),
            navigationModel = navigation,
            idleTimerModel = idleTimers,
            summaryModel = MockWorkoutSummaryModel(lastProgramUuid = programUuid),
            savedStateHandle = SavedStateHandle(
                target?.let { mapOf(PostWorkoutSurveyExtras.TARGET_ACTIVITY to it) } ?: emptyMap()
            ),
            mainDispatcher = dispatcher,
        ).apply { start() }
    }

    @Test
    fun showsTheConfiguredUrlAsIs() {
        val vm = viewModel()
        assertEquals(PostWorkoutSurveyUiState.Showing(url = SURVEY_URL), vm.state.value)
        assertEquals(emptyList<String>(), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun emptyUrlSkipsToSummary() {
        val vm = viewModel(surveyUrl = "")
        assertEquals(PostWorkoutSurveyUiState.Leaving, vm.state.value)
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun loadFailureSkipsToSummary() {
        val vm = viewModel()
        vm.onSurveyLoadFailed(errorCode = 404, errorDescription = "Not Found")
        assertEquals(PostWorkoutSurveyUiState.Leaving, vm.state.value)
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun homeGoesToSummaryNotSplash() {
        val vm = viewModel()
        vm.onHome()
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
        assertEquals(0, navigation.splashLaunches)
    }

    @Test
    fun idleTimeoutGoesToSummary() {
        viewModel()
        idleTimers.idleCallbacks.values.single().invoke()
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
        assertEquals(0, navigation.splashLaunches)
    }

    @Test
    fun leavesOnlyOnce() {
        val vm = viewModel()
        vm.onHome()
        vm.onSurveyLoadFailed(errorCode = -2, errorDescription = "net::ERR_NAME_NOT_RESOLVED")
        idleTimers.idleCallbacks.values.forEach { it() }
        vm.onHome()
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun noIdleTimerWhenTimeoutDisabled() {
        viewModel(screenTimeoutSeconds = 0)
        assertEquals(emptyMap<String, () -> Unit>(), idleTimers.idleCallbacks)
    }

    @Test
    fun missingTargetMeansDefaultSummary() {
        val vm = viewModel(target = null)
        vm.onHome()
        assertEquals(listOf(""), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun webViewCreateFailureSkipsToSummary() {
        val vm = viewModel()
        vm.onWebViewUnavailable(error = IllegalStateException("no webview provider"))
        assertEquals(PostWorkoutSurveyUiState.Leaving, vm.state.value)
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun fallsBackToSplashWhenTheSummaryNeverLands() {
        val vm = viewModel(screenTimeoutSeconds = 60)
        vm.onHome()
        dispatcher.scheduler.advanceTimeBy(60_000L - 1)
        dispatcher.scheduler.runCurrent()
        assertEquals(0, navigation.splashLaunches)
        dispatcher.scheduler.advanceTimeBy(2)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, navigation.splashLaunches)
    }

    @Test
    fun noSplashFallbackOnceTheSurveyIsPaused() {
        val vm = viewModel(screenTimeoutSeconds = 60)
        vm.onHome()
        vm.stop()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, navigation.splashLaunches)
    }

    @Test
    fun splashFallbackStillRunsWithTheScreenTimeoutDisabled() {
        val vm = viewModel(screenTimeoutSeconds = 0)
        vm.onHome()
        dispatcher.scheduler.advanceTimeBy(PostWorkoutSurveyViewModel.DEFAULT_FALLBACK_SECONDS * 1000L + 1)
        dispatcher.scheduler.runCurrent()
        assertEquals(1, navigation.splashLaunches)
    }

    @Test
    fun noSplashFallbackWhileShowing() {
        viewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, navigation.splashLaunches)
    }

    @Test
    fun showingTheSurveyActsLikeTheSummaryForTheConsole() {
        viewModel()
        assertEquals(listOf(UiEvent.WORKOUT_SUMMARY), system.uiEvents)
        assertEquals(1, system.homeKeyEnables)
    }

    @Test
    fun homeReportsTheHomeButtonPressOnce() {
        val vm = viewModel()
        vm.onHome()
        vm.onHome()
        assertEquals(1, system.homeButtonPresses)
    }

    @Test
    fun otherExitsDoNotReportAHomeButtonPress() {
        val vm = viewModel()
        idleTimers.idleCallbacks.values.single().invoke()
        vm.onSurveyLoadFailed(errorCode = -2, errorDescription = "net::ERR_NAME_NOT_RESOLVED")
        assertEquals(0, system.homeButtonPresses)
    }

    @Test
    fun racingExitsLaunchTheSummaryOnce() {
        repeat(200) {
            val launches = MockNavigationModel()
            val vm = PostWorkoutSurveyViewModel(
                systemModel = MockSystemModel(postWorkoutSurveyUrl = SURVEY_URL, screenTimeoutSeconds = 60),
                userModel = MockUserModel(),
                uiBaseModel = MockUIBaseModel(),
                navigationModel = launches,
                idleTimerModel = MockIdleTimerModel(),
                summaryModel = MockWorkoutSummaryModel(),
                savedStateHandle = SavedStateHandle(),
                mainDispatcher = dispatcher,
            )
            val go = CountDownLatch(1)
            val racers = List(4) { thread { go.await(); vm.onHome() } }
            go.countDown()
            racers.forEach { it.join() }
            assertEquals(1, launches.summaryLaunchesAfterSurvey.size)
        }
    }


    private fun mlViewModel(
        target: String? = TARGET,
        surveyUrl: String = SURVEY_URL,
    ) = viewModel(target = target, surveyUrl = surveyUrl, mlSurveyUrl = ML_URL)

    private fun advance(ms: Long) {
        dispatcher.scheduler.advanceTimeBy(ms)
        dispatcher.scheduler.runCurrent()
    }

    @Test
    fun mlSurveyWaitsForTheProgramUuid() {
        val vm = mlViewModel()
        dispatcher.scheduler.runCurrent()
        assertEquals(PostWorkoutSurveyUiState.Waiting, vm.state.value)
        assertEquals(emptyList<String>(), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun mlSurveyShowsWhenTheUuidArrivesLate() {
        val vm = mlViewModel()
        advance(2_000)
        programUuid.emit("manual:abc")
        assertEquals(ML_SHOWING, vm.state.value)
    }

    @Test
    fun mlSurveyShowsWhenTheUuidIsAlreadyPublished() {
        programUuid.emit("manual:abc")
        assertEquals(ML_SHOWING, mlViewModel().state.value)
    }

    @Test
    fun mlSurveyTakesPrecedenceOverThePostSurveyUrl() {
        programUuid.emit("manual:abc")
        assertEquals(ML_SHOWING, mlViewModel(surveyUrl = SURVEY_URL).state.value)
    }

    @Test
    fun mlSurveyShowsWithThePostSurveyUrlOff() {
        programUuid.emit("manual:abc")
        assertEquals(ML_SHOWING, mlViewModel(surveyUrl = "").state.value)
    }

    @Test
    fun postSurveyShowsWhenTheMlSurveyDoesNotApply() {
        listOf(
            viewModel(mlSurveyUrl = ML_URL, isGuest = true),
            viewModel(mlSurveyUrl = ML_URL, hasCompletedPaceSurvey = false),
            viewModel(mlSurveyUrl = ML_URL, facilityId = 0),
            viewModel(mlSurveyUrl = ""),
        ).forEach { assertEquals(PostWorkoutSurveyUiState.Showing(url = SURVEY_URL), it.state.value) }
    }

    @Test
    fun mlSurveyStillWaitingJustBeforeTheUuidTimeout() {
        val vm = mlViewModel()
        advance(UUID_WAIT_MS - 1)
        assertEquals(PostWorkoutSurveyUiState.Waiting, vm.state.value)
    }

    @Test
    fun mlSurveyLeavesForTheSummaryWhenTheUuidNeverArrives() {
        val vm = mlViewModel()
        advance(UUID_WAIT_MS)
        assertEquals(PostWorkoutSurveyUiState.Leaving, vm.state.value)
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun lateUuidAfterTheTimeoutDoesNotReopenTheSurvey() {
        val vm = mlViewModel()
        advance(UUID_WAIT_MS)
        programUuid.emit("manual:abc")
        assertEquals(PostWorkoutSurveyUiState.Leaving, vm.state.value)
    }

    @Test
    fun shownMlSurveyIsNotCutOffByTheUuidWait() {
        val vm = mlViewModel()
        programUuid.emit("manual:abc")
        advance(UUID_WAIT_MS * 2)
        assertEquals(ML_SHOWING, vm.state.value)
        assertEquals(emptyList<String>(), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun stopCancelsTheUuidWait() {
        val vm = mlViewModel()
        vm.stop()
        advance(UUID_WAIT_MS * 2)
        assertEquals(emptyList<String>(), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun surveyCompleteLeavesForTheSummary() {
        programUuid.emit("manual:abc")
        val vm = mlViewModel()
        vm.onSurveyMessage(SurveyBridgeMessage.Complete)
        assertEquals(PostWorkoutSurveyUiState.Leaving, vm.state.value)
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun surveyFailureLeavesForTheSummary() {
        programUuid.emit("manual:abc")
        val vm = mlViewModel()
        vm.onSurveyMessage(SurveyBridgeMessage.Failed("survey-failed-submit"))
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun bridgeMessagesLeaveOnce() {
        programUuid.emit("manual:abc")
        val vm = mlViewModel()
        vm.onSurveyMessage(SurveyBridgeMessage.Complete)
        vm.onSurveyMessage(SurveyBridgeMessage.Failed("survey-failed-submit"))
        vm.onHome()
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
        assertEquals(0, system.homeButtonPresses)
    }

    @Test
    fun mlSurveyLoadFailureLeavesForTheSummary() {
        programUuid.emit("manual:abc")
        val vm = mlViewModel()
        vm.onSurveyLoadFailed(errorCode = -2, errorDescription = "net::ERR_NAME_NOT_RESOLVED")
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun mlSurveyHttpErrorLeavesForTheSummary() {
        programUuid.emit("manual:abc")
        val vm = mlViewModel()
        vm.onSurveyLoadFailed(errorCode = 503, errorDescription = "Service Unavailable")
        assertEquals(PostWorkoutSurveyUiState.Leaving, vm.state.value)
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
    }

    @Test
    fun mlSurveyTokensAreReadLiveSoAReloadGetsRefreshedOnes() {
        val vm = mlViewModel()
        programUuid.emit("manual:abc")
        user.jumpAccessToken = "access2"
        user.jumpIdToken = "id2"
        assertEquals("access2", vm.jumpAccessToken())
        assertEquals("id2", vm.jumpIdToken())
    }

    @Test
    fun homeLeavesTheMlSurveyWhileWaiting() {
        val vm = mlViewModel()
        vm.onHome()
        advance(UUID_WAIT_MS * 2)
        assertEquals(listOf(TARGET), navigation.summaryLaunchesAfterSurvey)
        assertEquals(1, system.homeButtonPresses)
    }

    private companion object {
        const val TARGET = "com.jht.seca2.SecaActivity"
        const val SURVEY_URL = "https://www.surveymonkey.com/r/abc"
        const val ML_URL = "https://survey.example.com/pace"
        const val UUID_WAIT_MS = PostWorkoutSurveyViewModel.PROGRAM_UUID_WAIT_MS
        val ML_SHOWING = PostWorkoutSurveyUiState.ShowingMl(
            url = "$ML_URL?exercise_id=manual%3Aabc&facility_id=42&lang=zh-TW",
        )
    }
}
