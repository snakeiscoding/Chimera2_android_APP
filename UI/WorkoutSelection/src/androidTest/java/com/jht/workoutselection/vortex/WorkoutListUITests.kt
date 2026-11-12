package com.jht.workoutselection.vortex

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.workoutselection.WorkoutSelectionState
import com.jht.workoutselection.vortex.parts.mainscreen.CarouselItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutListUITests {

    @get:Rule
    val composeTestRule = getRule()

    fun getRule(): ComposeContentTestRule {
        val rule = createComposeRule()
        rule.mainClock.autoAdvance = false
        return rule
    }

    @Test
    fun featuredCarouselEmpty() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex()
        }
        composeTestRule.onNodeWithTag("WorkoutSelection:CarouselPager").assertDoesNotExist()
    }

    @Test
    fun featuredCarouselFull() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                featuredCarouselItems = MockWorkoutCatalogModel().getMockWorkoutList(20).map { CarouselItem.fromWorkoutItem(it) },
            )
        }
        composeTestRule.onNodeWithTag("WorkoutSelection:CarouselPager").assertExists()
    }

    @Test
    fun featuredEmpty() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex()
        }
        composeTestRule.onNodeWithTag("WorkoutSelection:WorkoutCard:0").assertDoesNotExist()
    }

    @Test
    fun featuredFull() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                featuredWorkoutItems = MockWorkoutCatalogModel().getMockWorkoutList(20),
            )
        }
        composeTestRule.onAllNodesWithTag("WorkoutSelection:WorkoutCard:0", useUnmergedTree = true)[0].assertExists()
    }

    @Test
    fun trainingCarouselEmpty() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                uiState = mutableStateOf(WorkoutSelectionState.Training),
            )
        }
        composeTestRule.onNodeWithTag("WorkoutSelection:CarouselPager").assertDoesNotExist()
    }

    @Test
    fun trainingCarouselFull() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                uiState = mutableStateOf(WorkoutSelectionState.Training),
                trainingCarouselItems = MockWorkoutCatalogModel().getMockWorkoutList(20).map { CarouselItem.fromWorkoutItem(it) },
            )
        }
        composeTestRule.onNodeWithTag("WorkoutSelection:CarouselPager").assertExists()
    }

    @Test
    fun trainingEmpty() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                uiState = mutableStateOf(WorkoutSelectionState.Training),
            )
        }
        composeTestRule.onNodeWithTag("WorkoutSelection:WorkoutCard:0").assertDoesNotExist()
    }

    @Test
    fun trainingFull() {
        composeTestRule.setContent {
            WorkoutSelectionScreenVortex(
                uiState = mutableStateOf(WorkoutSelectionState.Training),
                trainingWorkoutItems = MockWorkoutCatalogModel().getMockWorkoutList(20),
            )
        }
        composeTestRule.onAllNodesWithTag("WorkoutSelection:WorkoutCard:0", useUnmergedTree = true)[0].assertExists()
    }
}
