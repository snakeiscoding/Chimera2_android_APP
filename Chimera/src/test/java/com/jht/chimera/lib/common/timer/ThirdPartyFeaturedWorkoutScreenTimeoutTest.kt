package com.jht.chimera.lib.common.timer

import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.common.testing.TestingUtils
import com.jht.chimera.lib.workoutapps.AppIds
import com.jht.chimera.lib.workoutapps.AppInfo
import com.jht.chimera.lib.workoutapps.AppManager
import com.jht.datamatrixlib.DataMatrix
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Idle-timeout policy of [ThirdPartyFeaturedWorkoutScreenTimeout].
 *
 * Featured third-party apps (iFit, YMCA360 featured) replace the native workout summary, so once
 * their workout ends the console must idle-timeout back to the splash screen — nothing else would
 * ever bring it home. FullIntegration apps (Hero's Journey, the Matrix test app, and the custom
 * override app used for integrations like IMPower) own their entire user flow including
 * post-workout, so the console must never force them back to splash.
 *
 * The real [IdleTimerManagement] and JHTTimer/UptimeTimer chain runs the timers (android.os.Handler
 * and SystemClock are the src/test stubs); only true boundaries are mocked: DataMatrix (binder),
 * AppManager (app registry), TestingUtils (SharedPreferences), and the splash launch itself.
 */
class ThirdPartyFeaturedWorkoutScreenTimeoutTest {

    private companion object {
        const val SCREEN_TIMEOUT_SEC = 1
        const val FIRE_WAIT_MS = 5000L // generous upper bound for the 1s timer to fire
        const val NO_FIRE_WAIT_MS = 2500L // how long we wait to call a timer "not started"
    }

    private lateinit var appManager: AppManager
    private var currentAppId: String = AppIds.DEFAULT_APP_UUID

    /** [AppInfo]'s constructor is package-private, so integration apps are stubbed with the real public field set. */
    private fun appInfo(category: AppInfo.Category): AppInfo =
        mockk<AppInfo>(relaxed = true).apply { this.category = category }

    @Before
    fun setUp() {
        mockkStatic(DataMatrix::class)
        every { DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING) } answers { currentAppId }
        every { DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT) } returns SCREEN_TIMEOUT_SEC

        // IdleTimerManagement creates its own DataMatrix instance for wake/reset adapters
        mockkConstructor(DataMatrix::class)
        every { anyConstructed<DataMatrix>().createBoolAdapter(any(), any(), any()) } returns mockk(relaxed = true)
        every { anyConstructed<DataMatrix>().createEventAdapter(any(), any()) } returns mockk(relaxed = true)
        every { anyConstructed<DataMatrix>().createIntAdapter(any(), any()) } returns mockk(relaxed = true)
        every { anyConstructed<DataMatrix>().finish() } just Runs

        mockkStatic(TestingUtils::class)
        every { TestingUtils.isAutomationEnabled() } returns false

        mockkStatic(ProductConfig::class)
        every { ProductConfig.isParker() } returns false

        mockkStatic(LaunchAppUtil::class)
        every { LaunchAppUtil.launchSplashScreen() } just Runs

        appManager = mockk()
        mockkStatic(AppManager::class)
        every { AppManager.getInstance() } returns appManager
        every { appManager.getApp(any()) } returns null
        every { appManager.getApp(AppIds.IFIT_UUID) } returns appInfo(AppInfo.Category.Integration)
        every { appManager.getApp(AppIds.HEROS_JOURNEY_UUID) } returns appInfo(AppInfo.Category.FullIntegration)
        every { appManager.getApp(AppIds.CUSTOM_OVERRIDE_APP_UUID) } returns appInfo(AppInfo.Category.FullIntegration)
        every { appManager.getApp(AppIds.MATRIX_TEST_UUID) } returns appInfo(AppInfo.Category.FullIntegration)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /** Build the class under test with the given workout app active, then run a workout to its end. */
    private fun endWorkoutFor(appUuid: String): ThirdPartyFeaturedWorkoutScreenTimeout {
        currentAppId = appUuid
        val timeout = ThirdPartyFeaturedWorkoutScreenTimeout()
        timeout.processWorkoutRunState(WorkoutKeys.Constants.RUN_STATE_RUNNING)
        timeout.processWorkoutRunState(WorkoutKeys.Constants.RUN_STATE_STOP)
        return timeout
    }

    @Test
    fun `ifit workout end times out back to the splash screen`() {
        endWorkoutFor(AppIds.IFIT_UUID)

        verify(timeout = FIRE_WAIT_MS, exactly = 1) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `workout end for an app without special handling still times out`() {
        // Apps that fall back to system behavior (getApp returns null) keep the pre-FullIntegration behavior
        endWorkoutFor(AppIds.DEFAULT_APP_UUID)

        verify(timeout = FIRE_WAIT_MS, exactly = 1) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `heros journey workout end never times out to splash`() {
        endWorkoutFor(AppIds.HEROS_JOURNEY_UUID)

        Thread.sleep(NO_FIRE_WAIT_MS)
        verify(exactly = 0) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `custom override integration workout end never times out to splash`() {
        // The custom override app is how external integrations like IMPower run
        endWorkoutFor(AppIds.CUSTOM_OVERRIDE_APP_UUID)

        Thread.sleep(NO_FIRE_WAIT_MS)
        verify(exactly = 0) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `matrix test app workout end never times out to splash`() {
        endWorkoutFor(AppIds.MATRIX_TEST_UUID)

        Thread.sleep(NO_FIRE_WAIT_MS)
        verify(exactly = 0) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `running workout does not time out even for ifit`() {
        currentAppId = AppIds.IFIT_UUID
        val timeout = ThirdPartyFeaturedWorkoutScreenTimeout()

        timeout.processWorkoutRunState(WorkoutKeys.Constants.RUN_STATE_RUNNING)

        Thread.sleep(NO_FIRE_WAIT_MS)
        verify(exactly = 0) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `ifit in workout selection times out back to the splash screen`() {
        currentAppId = AppIds.DEFAULT_APP_UUID
        val timeout = ThirdPartyFeaturedWorkoutScreenTimeout()

        timeout.processWorkoutAppUUID(AppIds.IFIT_UUID)

        verify(timeout = FIRE_WAIT_MS, exactly = 1) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `heros journey in workout selection does not time out`() {
        currentAppId = AppIds.DEFAULT_APP_UUID
        val timeout = ThirdPartyFeaturedWorkoutScreenTimeout()

        timeout.processWorkoutAppUUID(AppIds.HEROS_JOURNEY_UUID)

        Thread.sleep(NO_FIRE_WAIT_MS)
        verify(exactly = 0) { LaunchAppUtil.launchSplashScreen() }
    }
    @Test
    fun `summary timeout is held while the survey shows`() {
        val timeout = endWorkoutFor(AppIds.DEFAULT_APP_UUID)

        timeout.holdForSurvey()

        Thread.sleep(NO_FIRE_WAIT_MS)
        verify(exactly = 0) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `hold survives an app uuid update during the survey`() {
        val timeout = endWorkoutFor(AppIds.DEFAULT_APP_UUID)
        timeout.holdForSurvey()

        timeout.processWorkoutAppUUID(AppIds.DEFAULT_APP_UUID)

        Thread.sleep(NO_FIRE_WAIT_MS)
        verify(exactly = 0) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `hold requested before the stop state lands still holds`() {
        // the survey launch and the run state stop arrive on separate adapters, in either order
        currentAppId = AppIds.DEFAULT_APP_UUID
        val timeout = ThirdPartyFeaturedWorkoutScreenTimeout()
        timeout.processWorkoutRunState(WorkoutKeys.Constants.RUN_STATE_RUNNING)

        timeout.holdForSurvey()
        timeout.processWorkoutRunState(WorkoutKeys.Constants.RUN_STATE_STOP)

        Thread.sleep(NO_FIRE_WAIT_MS)
        verify(exactly = 0) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `summary after the survey gets a fresh full timeout`() {
        val timeout = endWorkoutFor(AppIds.DEFAULT_APP_UUID)
        timeout.holdForSurvey()
        Thread.sleep(NO_FIRE_WAIT_MS)

        timeout.restartAfterSurvey()

        Thread.sleep(SCREEN_TIMEOUT_SEC * 1000L / 2)
        verify(exactly = 0) { LaunchAppUtil.launchSplashScreen() }
        verify(timeout = FIRE_WAIT_MS, exactly = 1) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `restart after the survey does not time out a full integration`() {
        val timeout = endWorkoutFor(AppIds.HEROS_JOURNEY_UUID)
        timeout.holdForSurvey()

        timeout.restartAfterSurvey()

        Thread.sleep(NO_FIRE_WAIT_MS)
        verify(exactly = 0) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `next workout times out normally after a held survey`() {
        // a survey that never reached the summary must not leave the next workout's summary held
        val timeout = endWorkoutFor(AppIds.DEFAULT_APP_UUID)
        timeout.holdForSurvey()

        timeout.processWorkoutRunState(WorkoutKeys.Constants.RUN_STATE_RUNNING)
        timeout.processWorkoutRunState(WorkoutKeys.Constants.RUN_STATE_STOP)

        verify(timeout = FIRE_WAIT_MS, exactly = 1) { LaunchAppUtil.launchSplashScreen() }
    }

    @Test
    fun `splash reset clears the survey hold`() {
        val timeout = endWorkoutFor(AppIds.DEFAULT_APP_UUID)
        timeout.holdForSurvey()

        timeout.reset()
        timeout.processWorkoutAppUUID(AppIds.IFIT_UUID)

        verify(timeout = FIRE_WAIT_MS, exactly = 1) { LaunchAppUtil.launchSplashScreen() }
    }
}
