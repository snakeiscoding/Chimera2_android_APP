package com.jht.progression

import android.content.res.Resources
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jht.enums.ProgramType
import com.jht.progression.data.Baseline15Min
import com.jht.progression.data.Program
import com.jht.progression.data.program.program5k.Program5k
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.ProgramSelectScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgramSelectScreenTests {

    @get:Rule
    val rule = createComposeRule()

    private val res: Resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun setupScreen() {
        rule.setContent {
            ProgramSelectScreen(
                programs = listOf(
                    Program(
                        programType = ProgramType.Program5k,
                        nameId = R.string.five_k_tc,
                        workoutInterface = Program5k(),
                        baselineInterface = Baseline15Min(),
                        color = MmColors.futuristicBlue,
                    )
                ),
                navigateToSplash = {},
                onStartPressed = {}
            )
        }
    }

    @Test
    fun testTitles(){
        setupScreen()
        val race = res.getString(R.string.race_uc)
        rule.onNodeWithTag("choose_a_training_program").assertExists()
        rule.onNodeWithTag("choose_a_training_program").assertIsDisplayed()
        rule.onNodeWithTag("program_race").assertExists()
        rule.onNodeWithTag("program_race").assertIsDisplayed()
        rule.onNodeWithTag("program_race").assertTextContains(value = race, substring = true)
        rule.onNodeWithTag("program_race").assertTextEquals("5K RACE")
        rule.onNodeWithTag("program_weeks").assertExists()
        rule.onNodeWithTag("program_weeks").assertIsDisplayed()
        rule.onNodeWithTag("program_weeks").assertTextEquals("8 WEEK PROGRAM")
    }

    @Test
    fun testProgramName(){
        setupScreen()
        val programName = res.getString(R.string.five_k_tc)
        rule.onNodeWithTag("program_name").assertExists()
        rule.onNodeWithTag("program_name").assertIsDisplayed()
        rule.onNodeWithTag("program_name").assertTextEquals(programName)
    }

    @Test
    fun testProgramImage(){
        setupScreen()
        rule.onNodeWithTag("program_image").assertExists()
        rule.onNodeWithTag("program_image").assertIsDisplayed()
        rule.onNodeWithTag("program_image").assertWidthIsAtLeast(300.dp)
        rule.onNodeWithTag("program_image").assertHeightIsAtLeast(200.dp)
    }

    @Test
    fun testStartButton(){
        setupScreen()
        val buttonText = res.getString(R.string.start_uc)
        rule.onNodeWithTag("start_button").assertExists()
        rule.onNodeWithTag("start_button").assertIsDisplayed()
        rule.onNodeWithTag("start_button").performClick()
        rule.onNodeWithTag("start_button").assertIsEnabled()
        rule.onNodeWithTag("start_button").assertTextEquals(buttonText)
    }

    @Test
    fun testExitIcon(){
        setupScreen()
        rule.onNodeWithTag("top_bar_exit_button").assertExists()
        rule.onNodeWithTag("top_bar_exit_button").assertIsEnabled()
        rule.onNodeWithTag("top_bar_exit_button").assertIsDisplayed()
        rule.onNodeWithTag("top_bar_exit_button").performClick()
        rule.onNodeWithTag("top_bar_exit_button").assertWidthIsAtLeast(30.dp)
        rule.onNodeWithTag("top_bar_exit_button").assertHeightIsAtLeast(30.dp)
    }
}