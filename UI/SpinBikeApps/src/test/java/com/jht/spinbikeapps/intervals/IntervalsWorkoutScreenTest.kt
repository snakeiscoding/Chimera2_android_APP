package com.jht.spinbikeapps.intervals

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jht.spinbikeapps.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1920dp-h1600dp")
class IntervalsWorkoutScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val previousIntervalsData = listOf(
        IntervalData(intervalTime = "1.00", intervalDistance = "1.00", intervalType = IntervalPhase.Rest),
        IntervalData(intervalTime = "0.30", intervalDistance = "1.00", intervalType = IntervalPhase.Work),
    )

    private fun getString(id: Int) = composeTestRule.activity.getString(id)
    private fun getString(id: Int, vararg args: Any) = composeTestRule.activity.getString(id, *args)

    // ---- isMetric: Time goal ----

    @Test
    fun timeGoal_imperial_showsMilesUnit() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = .5f,
                intervalGoal = IntervalGoal.Time,
                mainText = "0:30",
                subText = "2.02",
                previousIntervalsData = previousIntervalsData,
                isMetric = false,
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.time_remaining_tc)).assertExists()
        composeTestRule.onNodeWithText(getString(R.string.mi_lc)).assertExists()
    }

    @Test
    fun timeGoal_metric_showsKilometerUnit() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = .5f,
                intervalGoal = IntervalGoal.Time,
                mainText = "0:30",
                subText = "2.02",
                previousIntervalsData = previousIntervalsData,
                isMetric = true,
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.time_remaining_tc)).assertExists()
        composeTestRule.onNodeWithText(getString(R.string.km_lc)).assertExists()
    }

    // ---- isMetric: Distance goal ----

    @Test
    fun distanceGoal_imperial_showsMilesRemainingLabel() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = .5f,
                intervalGoal = IntervalGoal.Distance,
                mainText = "4.21",
                subText = "0:30",
                previousIntervalsData = previousIntervalsData,
                isMetric = false,
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.miles_remaining_tc)).assertExists()
    }

    @Test
    fun distanceGoal_metric_showsKilometersRemainingLabel() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = .5f,
                intervalGoal = IntervalGoal.Distance,
                mainText = "0.45",
                subText = "1:30",
                previousIntervalsData = previousIntervalsData,
                isMetric = true,
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.km_remaining_tc)).assertExists()
    }

    // ---- completionProgress edge cases: should not crash, and should still render mainText ----

    @Test
    fun completionProgress_nan_rendersMainText() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = Float.NaN,
                intervalGoal = IntervalGoal.Time,
                mainText = "0:30",
                subText = "2.02",
                previousIntervalsData = previousIntervalsData,
                isMetric = false,
            )
        }

        composeTestRule.onNodeWithText("0:30").assertExists()
    }

    @Test
    fun completionProgress_aboveOne_rendersMainText() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = 1.5f,
                intervalGoal = IntervalGoal.Time,
                mainText = "0:30",
                subText = "2.02",
                previousIntervalsData = previousIntervalsData,
                isMetric = false,
            )
        }

        composeTestRule.onNodeWithText("0:30").assertExists()
    }

    @Test
    fun completionProgress_negative_rendersMainText() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = -1f,
                intervalGoal = IntervalGoal.Time,
                mainText = "0:30",
                subText = "2.02",
                previousIntervalsData = previousIntervalsData,
                isMetric = false,
            )
        }

        composeTestRule.onNodeWithText("0:30").assertExists()
    }

    // ---- Interval index and phase ----

    @Test
    fun currentIntervalIndex_showsIntervalNumber() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = .5f,
                intervalGoal = IntervalGoal.Time,
                mainText = "0:30",
                subText = "2.02",
                currentIntervalIndex = 4,
                previousIntervalsData = previousIntervalsData,
                isMetric = false,
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.interval_number_tc, 4)).assertExists()
    }

    @Test
    fun currentPhase_work_showsWorkText() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = .5f,
                intervalGoal = IntervalGoal.Time,
                mainText = "0:30",
                subText = "2.02",
                currentPhase = IntervalPhase.Work,
                previousIntervalsData = emptyList(),
                isMetric = false,
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.work_tc)).assertExists()
    }

    @Test
    fun currentPhase_rest_showsRestText() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = .5f,
                intervalGoal = IntervalGoal.Time,
                mainText = "0:30",
                subText = "2.02",
                currentPhase = IntervalPhase.Rest,
                previousIntervalsData = emptyList(),
                isMetric = false,
            )
        }

        composeTestRule.onNodeWithText(getString(R.string.rest_tc)).assertExists()
    }

    // ---- mainText / subText render as passed in, regardless of goal ----

    @Test
    fun mainTextAndSubText_renderAsPassedIn() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = .5f,
                intervalGoal = IntervalGoal.Distance,
                mainText = "4.21",
                subText = "0:30",
                previousIntervalsData = previousIntervalsData,
                isMetric = false,
            )
        }

        composeTestRule.onNodeWithText("4.21").assertExists()
        composeTestRule.onNodeWithText("0:30", substring = true).assertExists()
    }

    // ---- previous intervals table: totals are summed from previousIntervalsData ----

    @Test
    fun previousIntervalsTable_totalsRenderAsPassedIn() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = .5f,
                intervalGoal = IntervalGoal.Time,
                mainText = "0:30",
                subText = "2.02",
                previousIntervalsData = previousIntervalsData,
                isMetric = false,
                totalDistanceText = "2.00",
                totalTimeText = "1:30",
            )
        }

        composeTestRule.onNodeWithText("1:30").assertExists()
        composeTestRule.onNodeWithText("2.00").assertExists()
    }

    @Test
    fun previousIntervalsTable_totalsNotPassedIn_showDefaultZero() {
        composeTestRule.setContent {
            IntervalsWorkoutScreen(
                completionProgress = .5f,
                intervalGoal = IntervalGoal.Time,
                mainText = "0:30",
                subText = "2.02",
                previousIntervalsData = emptyList(),
                isMetric = false,
            )
        }

        composeTestRule.onNodeWithText("0:00").assertExists()
        composeTestRule.onNodeWithText("0.00").assertExists()
    }
}
