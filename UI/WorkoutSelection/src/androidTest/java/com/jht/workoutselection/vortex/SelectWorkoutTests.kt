package com.jht.workoutselection.vortex

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jht.data.BaseFeaturedWorkoutPlugin
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutItem
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectWorkoutTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun goButton() {
        var callbackTriggered = false
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                featuredWorkoutItems = MockWorkoutCatalogModel().getFeaturedWorkouts,
                onQuickStart = {callbackTriggered = true},
            )
        }
        composeTestRule.onNodeWithTag("WorkoutSelection:GoButton").performClick()
        assert(callbackTriggered)
    }

    @Test
    fun selectWorkout() {
        var selectedWorkout: WorkoutItem? = null
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                featuredWorkoutItems = MockWorkoutCatalogModel().getFeaturedWorkouts,
                onSelectWorkoutItem = {selectedWorkout = it},
            )
        }
        composeTestRule.onAllNodesWithTag("WorkoutSelection:WorkoutCard:0")[0].performClick()
        assert(selectedWorkout != null)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun startSelectedWorkout() {
        val mockWorkoutModel = MockWorkoutCatalogModel()
        var callbackTriggered = false
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                onStartSelectedWorkout = {callbackTriggered = true},
                selectedWorkoutItem = mockWorkoutModel.getFeaturedWorkouts[0],
                workoutStartingInputs = mockWorkoutModel.getWorkoutInputsForPreview(),
                showWorkoutSetupModal = mutableStateOf(true),
            )
        }
        //Composables contain a delay for loading the workout and time needs to be manually advanced
        composeTestRule.mainClock.advanceTimeBy(1000)

        composeTestRule.waitUntilExactlyOneExists(hasTestTag("WorkoutSelection:StartWorkout:Button"))
        composeTestRule.onNodeWithTag("WorkoutSelection:StartWorkout:Button").performClick()

        assert(callbackTriggered)
    }

    @Test
    fun selectVA() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                showVirtualActiveModal = mutableStateOf(true),
            )
        }
        composeTestRule.onNodeWithTag("WorkoutSelection:LocationSelectionModal").assertExists()
    }

    @Test
    fun selectLandmarks() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                showLandmarksModal = mutableStateOf(true),
            )
        }
        composeTestRule.onNodeWithTag("WorkoutSelection:LocationSelectionModal").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectCustomHiit() {
        val mockWorkoutModel = MockWorkoutCatalogModel()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workoutItem = WorkoutItem(workoutId = 0, appId = "")
        val activePlugin = TestPlugin(context, workoutItem)
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                workoutStartingInputs = mockWorkoutModel.getWorkoutInputsForPreview(),
                activeFeaturePlugin = mutableStateOf(activePlugin),
                showFeaturePluginModal = mutableStateOf(true),
            )
        }
        //Composables contain a delay for loading the workout and time needs to be manually advanced
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithTag("WorkoutSelection:CustomHIITModal", useUnmergedTree = true).assertExists()
    }

    //No current tests for Sprint 8 and Target Training, due to hilt injection and java fragments respectively
}

private class TestPlugin(
    appContext: Context,
    workoutItem: WorkoutItem,
) : BaseFeaturedWorkoutPlugin(appContext, workoutItem) {
    @Composable
    override fun SetupScreen(workoutInputs: List<WorkoutInputUi>, onDismissRequest: () -> Unit) {
        Box(
            Modifier.testTag("WorkoutSelection:CustomHIITModal"),
        ) {
            Text("TEST")
        }
    }

    override fun startWorkout(workoutInputs: List<WorkoutInputUi>) {}
    override fun onLoad(workoutInputs: List<WorkoutInputUi>) {}
    override fun onUnload() {}
}
