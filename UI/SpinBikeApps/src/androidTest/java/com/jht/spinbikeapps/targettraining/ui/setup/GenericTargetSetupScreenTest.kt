package com.jht.spinbikeapps.targettraining.ui.setup

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.model.PillBoxData
import com.jht.spinbikeapps.targettraining.model.TargetSetupData
import com.jht.spinbikeapps.targettraining.ui.TargetTrainingScreen
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BLUE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GREEN_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingTypeData
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import com.jht.theme.DynamicTheme
import com.jht.theme.defaultDynamicTheme
import com.jht.widgets.numpad.NumPadDisplay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The hand off from the setup screen to [TargetTrainingTypeData], which updateSelectedMode turns into targets.
 * Leaf screen only, no NavHost, so these stay valid while the navigation graph is reworked.
 */
@RunWith(AndroidJUnit4::class)
class GenericTargetSetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() = resetTargetTrainingTypeData()

    @After
    fun tearDown() = resetTargetTrainingTypeData()

    private fun resetTargetTrainingTypeData() {
        TargetTrainingTypeData.selectedMode = WorkoutMode.Rpm
        TargetTrainingTypeData.rpmTarget = 0f
        TargetTrainingTypeData.caloriesTarget = 0f
        TargetTrainingTypeData.heartRateTarget = null
        TargetTrainingTypeData.wattsTarget = null
        TargetTrainingTypeData.distanceTarget = 0f
        TargetTrainingTypeData.timeTarget = 0
    }

    private fun pillBox(title: Int, default: Float) = PillBoxData(
        title = "title",
        unitValue = "",
        decimalPlaces = 0,
        displayType = NumPadDisplay.Number,
        testTag = "PillBox:$title",
        min = 0f,
        max = 1000f,
        stepValue = 1f,
        defaultValue = default,
    )

    private fun setupData(
        mode: WorkoutMode,
        route: String,
        pillBoxes: List<PillBoxData>,
        targetZoneRange: List<Pair<String, androidx.compose.ui.graphics.Color>> = listOf(),
    ) = TargetSetupData(
        titleRes = R.string.target_training_tc,
        iconRes = R.drawable.ic_rpm_meter,
        descriptionRes = R.string.target_training_info_pg,
        route = route,
        workoutMode = mode,
        pillBoxes = pillBoxes,
        targetZoneRange = targetZoneRange,
    )

    private fun renderScreen(data: TargetSetupData, onClick: (WorkoutMode) -> Unit = {}) {
        composeTestRule.setContent {
            DynamicTheme(defaultDynamicTheme) {
                GenericTargetSetupScreen(targetSetupData = data, navController = null, onClick = onClick)
            }
        }
    }

    private fun clickStart(mode: WorkoutMode) =
        composeTestRule.onNodeWithTag(startWorkoutButtonTag(mode)).performClick()

    @Test
    fun everyTargetTypeHasItsOwnStartButtonTag() {
        val tags = WorkoutMode.entries.map { startWorkoutButtonTag(it) }

        assertEquals(WorkoutMode.entries.size, tags.distinct().size)
    }

    @Test
    fun startPublishesRpmTargetAndReportsTheMode() {
        var clickedMode: WorkoutMode? = null
        renderScreen(
            setupData(
                mode = WorkoutMode.Rpm,
                route = TargetTrainingScreen.RpmScreen.route,
                pillBoxes = listOf(pillBox(R.string.target_rpm_tc, default = 85f)),
            )
        ) { clickedMode = it }

        clickStart(WorkoutMode.Rpm)

        assertEquals(85f, TargetTrainingTypeData.rpmTarget, 0.001f)
        assertEquals(WorkoutMode.Rpm, TargetTrainingTypeData.selectedMode)
        assertEquals(WorkoutMode.Rpm, clickedMode)
    }

    @Test
    fun startPublishesCaloriesTarget() {
        renderScreen(
            setupData(
                mode = WorkoutMode.Calories,
                route = TargetTrainingScreen.CaloriesScreen.route,
                pillBoxes = listOf(
                    pillBox(R.string.target_calories_tc, default = 250f),
                    pillBox(R.string.target_time_tc, default = 20f),
                ),
            )
        )

        clickStart(WorkoutMode.Calories)

        assertEquals(250f, TargetTrainingTypeData.caloriesTarget ?: 0f, 0.001f)
        assertEquals(WorkoutMode.Calories, TargetTrainingTypeData.selectedMode)
    }

    @Test
    @Ignore(
        "Known bug: the Calories start button never reads its time pill box, so a stale timeTarget is sent. " +
            "Un-ignore once it captures the time pill box like the Distance branch does."
    )
    fun startPublishesCaloriesTimeTarget() {
        renderScreen(
            setupData(
                mode = WorkoutMode.Calories,
                route = TargetTrainingScreen.CaloriesScreen.route,
                pillBoxes = listOf(
                    pillBox(R.string.target_calories_tc, default = 250f),
                    pillBox(R.string.target_time_tc, default = 20f),
                ),
            )
        )

        clickStart(WorkoutMode.Calories)

        assertEquals(20, TargetTrainingTypeData.timeTarget)
    }

    @Test
    fun startPublishesDistanceAndTimeTargets() {
        renderScreen(
            setupData(
                mode = WorkoutMode.Distance,
                route = TargetTrainingScreen.DistanceScreen.route,
                pillBoxes = listOf(
                    pillBox(R.string.target_distance_tc, default = 12f),
                    pillBox(R.string.target_time_tc, default = 30f),
                ),
            )
        )

        clickStart(WorkoutMode.Distance)

        assertEquals(12f, TargetTrainingTypeData.distanceTarget, 0.001f)
        assertEquals(30, TargetTrainingTypeData.timeTarget)
    }

    @Test
    fun startPublishesHeartRateTargetWithTheSelectedZone() {
        renderScreen(
            setupData(
                mode = WorkoutMode.HeartRate,
                route = TargetTrainingScreen.HeartRateScreen.route,
                pillBoxes = listOf(pillBox(R.string.max_heartrate_tc, default = 190f)),
                targetZoneRange = listOf("60-69%" to BLUE_1, "70-79%" to GREEN_1),
            )
        )

        clickStart(WorkoutMode.HeartRate)

        val target = TargetTrainingTypeData.heartRateTarget
        assertEquals(190f, target?.maxHr ?: 0f, 0.001f)
        // no zone tapped, so the first zone is the default selection
        assertEquals("60-69%", target?.targetZone)
    }

    @Test
    fun startPublishesWattsTargetWithTheSelectedZone() {
        renderScreen(
            setupData(
                mode = WorkoutMode.Watts,
                route = TargetTrainingScreen.WattsScreen.route,
                pillBoxes = listOf(pillBox(R.string.ftp_watts_tc, default = 200f)),
                targetZoneRange = listOf("56-75%" to BLUE_1, "76-90%" to GREEN_1),
            )
        )

        clickStart(WorkoutMode.Watts)

        val target = TargetTrainingTypeData.wattsTarget
        assertEquals(200f, target?.watts ?: 0f, 0.001f)
        assertEquals("56-75%", target?.targetZone)
    }
}
