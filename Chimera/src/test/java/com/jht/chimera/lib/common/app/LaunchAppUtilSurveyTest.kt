package com.jht.chimera.lib.common.app

import android.app.Application
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.service.ui.CommonScreens
import com.jht.chimera.service.ui.LaunchScreenData
import com.jht.datamatrixlib.DataMatrix
import com.jht.interfaces.models.PostWorkoutSurveyExtras
import com.jht.seca.data.SecaSurveyState
import com.jht.seca.data.SecaUserData
import com.jht.workoutsummary.survey.PostWorkoutSurveyActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// Robolectric so the launch Intent keeps its extras and component
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LaunchAppUtilSurveyTest {
    private val launches = mutableListOf<LaunchScreenData>()
    private var surveyOn = true
    private var mlSurveyUrl = ""

    @Before
    fun setUp() {
        mockkStatic(DataMatrix::class, ContextUtil::class, ProductConfig::class)
        every { ContextUtil.getApplicationContext() } returns RuntimeEnvironment.getApplication()
        every { ProductConfig.isRetail() } returns false
        every { DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL) } answers { surveyOn }
        every { DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING) } returns SURVEY_URL
        every { DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_ML_POST_WORKOUT_SURVEY_URL_STRING) } answers { mlSurveyUrl }
        every { DataMatrix.getBoolean(SettingKeys.General.DATA_IS_GUEST_USER_BOOL) } returns false
        every { DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT) } returns 1964
        val secaUser = mockk<SecaUserData> { every { surveyState } returns SecaSurveyState.COMPLETED }
        every { DataMatrix.getParcelable(DAPIKeys.DATA_SECA_USER_INFO_PARCELABLE, SecaUserData::class.java) } returns secaUser
        every { DataMatrix.putParcelable(UiServiceKeys.ACTION_LAUNCH_UI_PARCELABLE, any()) } answers {
            launches += secondArg<LaunchScreenData>()
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private val launched get() = launches.single()

    @Test
    fun `survey launch carries the summary target`() {
        LaunchAppUtil.launchWorkoutSummary(SECA)
        assertEquals(CommonScreens.PostWorkoutSurvey, launched.screen)
        assertEquals(SECA, launched.intent.getStringExtra(PostWorkoutSurveyExtras.TARGET_ACTIVITY))
        assertEquals(PostWorkoutSurveyActivity::class.java.canonicalName, launched.intent.component?.className)
    }

    @Test
    fun `survey launch for the default summary carries an empty target`() {
        LaunchAppUtil.launchWorkoutSummary()
        assertEquals(CommonScreens.PostWorkoutSurvey, launched.screen)
        assertEquals("", launched.intent.getStringExtra(PostWorkoutSurveyExtras.TARGET_ACTIVITY))
    }

    @Test
    fun `summary after the survey is marked survey shown and opens the target`() {
        LaunchAppUtil.launchWorkoutSummaryAfterSurvey(SECA)
        assertEquals(CommonScreens.WorkoutSummary, launched.screen)
        assertEquals(true, launched.intent.getBooleanExtra(PostWorkoutSurveyExtras.SURVEY_SHOWN, false))
        assertEquals(SECA, launched.intent.component?.className)
    }

    @Test
    fun `summary after the survey with an empty target leaves the default to the ui service`() {
        LaunchAppUtil.launchWorkoutSummaryAfterSurvey("")
        assertEquals(CommonScreens.WorkoutSummary, launched.screen)
        assertEquals(true, launched.intent.getBooleanExtra(PostWorkoutSurveyExtras.SURVEY_SHOWN, false))
        assertNull(launched.intent.component)
    }

    @Test
    fun `summary without the survey carries no survey extras`() {
        surveyOn = false
        LaunchAppUtil.launchWorkoutSummary(SECA)
        assertEquals(CommonScreens.WorkoutSummary, launched.screen)
        assertFalse(launched.intent.hasExtra(PostWorkoutSurveyExtras.SURVEY_SHOWN))
        assertFalse(launched.intent.hasExtra(PostWorkoutSurveyExtras.TARGET_ACTIVITY))
        assertEquals(SECA, launched.intent.component?.className)
    }

    @Test
    fun `ml survey launches the survey with the post survey off`() {
        surveyOn = false
        mlSurveyUrl = ML_URL
        LaunchAppUtil.launchWorkoutSummary(SECA)
        assertEquals(CommonScreens.PostWorkoutSurvey, launched.screen)
        assertEquals(SECA, launched.intent.getStringExtra(PostWorkoutSurveyExtras.TARGET_ACTIVITY))
    }

    private companion object {
        const val ML_URL = "https://dev-matrixworkouts.jfit.co/survey/after-exercise"
        const val SECA = "com.jht.seca2.SecaActivity"
        const val SURVEY_URL = "https://www.surveymonkey.com/r/abc"
    }
}
