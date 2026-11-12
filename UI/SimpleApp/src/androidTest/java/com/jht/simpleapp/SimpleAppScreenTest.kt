package com.jht.simpleapp

import androidx.activity.ComponentActivity
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.jht.widgets.helpers.StatInfo
import com.jht.widgets.helpers.StatInfoCollection
import org.junit.Rule
import org.junit.Test

class SimpleAppScreenTest {

    private val stat0Text = "1"
    private val stat1Text = "2"
    private val stat2Text = "3"
    private val stat3Text = "4"
    private val stat4Text = "5"
    private val stat0LabelId = R.string.japanese_tc
    private val stat1LabelId = R.string.simplified_chinese_tc
    private val stat2LabelId = R.string.korean_tc
    private val stat3LabelId = R.string.english_tc
    private val stat4LabelId = R.string.russian_tc
    private val stat0Unit = R.string.min_lc
    private val stat1Unit = R.string.max_lc
    private val stat2Unit = R.string.medium_uc
    private val stat3Unit = R.string.small_lcd_reversed_tc
    private val stat4Unit = R.string.units_tc
    private val defaultIconId = R.drawable.ic_calories_white

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testEmptyList() {
        rule.setContent { SimpleAppScreen(statInfoCollection = StatInfoCollection.fromListOfList(listOf())) }
        // make sure empty list of list doesn't crash
        rule.onNodeWithTag(testTagLargeCard).assertDoesNotExist()
        rule.onNodeWithTag(testTagSmallCard).assertDoesNotExist()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testList_Of_1_List_Size_0() {
        rule.setContent {
            SimpleAppScreen(
                statInfoCollection = StatInfoCollection.fromListOfList(listOf(listOf()))
            )
        }
        // make sure 1 list of 0 stats doesn't crash
        rule.onNodeWithTag(testTagLargeCard).assertDoesNotExist()
        rule.onNodeWithTag(testTagSmallCard).assertDoesNotExist()
    }

    private fun assertStatInfoExists(testTag: String, text: String, labelId: Int, unit: Int) {
        rule.onNodeWithTag(testTag, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText(text).assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(labelId)).assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(unit)).assertIsDisplayed()

    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testList_Of_1_List_Size_1() {
        rule.setContent {
            SimpleAppScreen(
                statInfoCollection = StatInfoCollection.fromListOfList(listOf(
                    listOf(
                        StatInfo(stat0LabelId, defaultIconId, stat0Text, stat0Unit),
                    )
                )
            ))
        }

        // assert small card doesn't small
        rule.onNodeWithTag(testTagSmallCard).assertDoesNotExist()

        // large card main stat
        assertStatInfoExists(testTagLargeCard, stat0Text, stat0LabelId, stat0Unit)

        // large card top left stat
        rule.onNodeWithTag(testTagLargeCard + 1).assertDoesNotExist()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testList_Of_1_List_Size_2() {
        val stat1Text = "1"
        val stat2Text = "2"
        rule.setContent {
            SimpleAppScreen(
                statInfoCollection = StatInfoCollection.fromListOfList(listOf(
                    listOf(
                        StatInfo(stat0LabelId, defaultIconId, stat1Text, stat0Unit),
                        StatInfo(stat1LabelId, defaultIconId, stat2Text, stat1Unit),
                    )
                ))
            )
        }
        // assert small card doesn't small
        rule.onNodeWithTag(testTagSmallCard).assertDoesNotExist()

        // large card main stat
        assertStatInfoExists(testTagLargeCard, stat1Text, stat0LabelId, stat0Unit)

        // large card top left stat (LargeCardStat0Label)
        assertStatInfoExists(testTagLargeCardStat + "TopLeft" + "Label", stat2Text, stat1LabelId, stat1Unit)

        // large card top right stat
        rule.onNodeWithTag(testTagLargeCardStat + "TopRight" + "Label").assertDoesNotExist()
    }


    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testList_Of_1_List_Size_5() {
        rule.setContent {
            SimpleAppScreen(
                statInfoCollection = StatInfoCollection.fromListOfList(listOf(
                    listOf(
                        StatInfo(R.string.japanese_tc, android.R.drawable.ic_dialog_email, stat0Text, stat0Unit),
                        StatInfo(R.string.simplified_chinese_tc, R.drawable.ic_calories_white, stat1Text, stat1Unit),
                        StatInfo(R.string.korean_tc, R.drawable.ic_calories_white, stat2Text, stat2Unit),
                        StatInfo(R.string.english_tc, R.drawable.ic_calories_white, stat3Text, stat3Unit),
                        StatInfo(R.string.russian_tc, R.drawable.ic_calories_white, stat4Text, stat4Unit),
                    )
                ))
            )
        }

        // assert small card doesn't small
        rule.onNodeWithTag(testTagSmallCard).assertDoesNotExist()

        // large card main stat
        assertStatInfoExists(testTagLargeCard, stat0Text, stat0LabelId, stat0Unit)

        // large card top left stat
        assertStatInfoExists(testTagLargeCardStat + 0, stat1Text, stat1LabelId, stat1Unit)
        // large card top right stat
        assertStatInfoExists(testTagLargeCardStat + 1, stat2Text, stat2LabelId, stat2Unit)
        // large card bottom left stat
        assertStatInfoExists(testTagLargeCardStat + 2, stat3Text, stat3LabelId, stat3Unit)
        // large card bottom right stat
        assertStatInfoExists(testTagLargeCardStat + 3, stat4Text, stat4LabelId, stat4Unit)

        // large card invalid stat
        rule.onNodeWithTag(testTagLargeCard + 5).assertDoesNotExist()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testList_Of_1_List_Size_6() {
        rule.setContent {
            SimpleAppScreen(
                statInfoCollection = StatInfoCollection.fromListOfList(listOf(
                    listOf(
                        StatInfo(R.string.japanese_tc, android.R.drawable.ic_dialog_email, stat0Text, stat0Unit),
                        StatInfo(R.string.simplified_chinese_tc, R.drawable.ic_calories_white, stat1Text, stat1Unit),
                        StatInfo(R.string.korean_tc, R.drawable.ic_calories_white, stat2Text, stat2Unit),
                        StatInfo(R.string.english_tc, R.drawable.ic_calories_white, stat3Text, stat3Unit),
                        StatInfo(R.string.russian_tc, R.drawable.ic_calories_white, stat4Text, stat4Unit),
                    )
                ))
            )
        }

        // assert small card doesn't small
        rule.onNodeWithTag(testTagSmallCard).assertDoesNotExist()

        // large card main stat
        assertStatInfoExists(testTagLargeCard, stat0Text, stat0LabelId, stat0Unit)

        // large card top left stat
        assertStatInfoExists(testTagLargeCardStat + 0, stat1Text, stat1LabelId, stat1Unit)
        // large card top right stat
        assertStatInfoExists(testTagLargeCardStat + 1, stat2Text, stat2LabelId, stat2Unit)
        // large card bottom left stat
        assertStatInfoExists(testTagLargeCardStat + 2, stat3Text, stat3LabelId, stat3Unit)
        // large card bottom right stat
        assertStatInfoExists(testTagLargeCardStat + 3, stat4Text, stat4LabelId, stat4Unit)

        // large card invalid stat since there is only a max of 5 stats possible on main card
        rule.onNodeWithTag(testTagLargeCard + 5).assertDoesNotExist()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testSmallCardSwipe() {
        rule.setContent {
            SimpleAppScreen(
                statInfoCollection = StatInfoCollection.fromListOfList(listOf(
                    listOf(
                        StatInfo(R.string.japanese_tc, android.R.drawable.ic_dialog_email, stat0Text, stat0Unit),
                        StatInfo(R.string.simplified_chinese_tc, R.drawable.ic_calories_white, stat1Text, stat1Unit),
                        StatInfo(R.string.korean_tc, R.drawable.ic_calories_white, stat2Text, stat2Unit),
                        StatInfo(R.string.english_tc, R.drawable.ic_calories_white, stat3Text, stat3Unit),
                        StatInfo(R.string.russian_tc, R.drawable.ic_calories_white, stat4Text, stat4Unit),
                    ),
                    listOf(
                        StatInfo(R.string.japanese_tc, android.R.drawable.ic_dialog_email, stat0Text, stat0Unit),
                        StatInfo(R.string.simplified_chinese_tc, R.drawable.ic_calories_white, stat1Text, stat1Unit),
                        StatInfo(R.string.korean_tc, R.drawable.ic_calories_white, stat2Text, stat2Unit),
                        StatInfo(R.string.english_tc, R.drawable.ic_calories_white, stat3Text, stat3Unit),
                        StatInfo(R.string.russian_tc, R.drawable.ic_calories_white, stat4Text, stat4Unit),
                    ),
                ))
            )

        }

        // assert small card does exist and that we are on the first page of the small card
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+0, useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+1, useUnmergedTree = true).assertDoesNotExist() // doesn't 'exist' yet because it has not been displayed

        //swipe to next page in small card
        rule.onNodeWithTag(testTagSmallCard+0).performTouchInput { swipeLeft() }

        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+0, useUnmergedTree = true).assertIsNotDisplayed() // technically 'exists' since it has been on screen, but it is no longer visible
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+1, useUnmergedTree = true).assertIsDisplayed()

        rule.onNodeWithTag(testTagSmallCard+0).performTouchInput { swipeLeft() }

        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+1, useUnmergedTree = true).assertIsNotDisplayed() // technically 'exists' since it has been on screen, but it is no longer visible
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+2, useUnmergedTree = true).assertIsDisplayed()

        rule.onNodeWithTag(testTagSmallCard+0).performTouchInput { swipeLeft() }

        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+2, useUnmergedTree = true).assertIsNotDisplayed() // technically 'exists' since it has been on screen, but it is no longer visible
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+3, useUnmergedTree = true).assertIsDisplayed()

        rule.onNodeWithTag(testTagSmallCard+0).performTouchInput { swipeLeft() }

        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+3, useUnmergedTree = true).assertIsNotDisplayed() // technically 'exists' since it has been on screen, but it is no longer visible
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+4, useUnmergedTree = true).assertIsDisplayed()

        rule.onNodeWithTag(testTagSmallCard+0).performTouchInput { swipeLeft() }

        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+5, useUnmergedTree = true).assertDoesNotExist() // no 6th page in small card
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testLargeCardMainStatSwap() {
        rule.setContent {
            SimpleAppScreen(
                statInfoCollection = StatInfoCollection.fromListOfList(listOf(
                    listOf(
                        StatInfo(stat0LabelId, defaultIconId, stat0Text, stat0Unit),
                        StatInfo(stat1LabelId, defaultIconId, stat1Text, stat1Unit),
                        StatInfo(stat2LabelId, defaultIconId, stat2Text, stat2Unit),
                        StatInfo(stat3LabelId, defaultIconId, stat3Text, stat3Unit),
                        StatInfo(stat4LabelId, defaultIconId, stat4Text, stat4Unit),
                    ),
                ))
            )
        }

        // assert small card doesn't exist (only have stats for big card
        rule.onNodeWithTag(testTagSmallCard).assertDoesNotExist()

        // verify main center stats
        rule.onNodeWithTag(testTagLargeCardStat + "MainLabel").assertTextContains(rule.activity.getString(stat0LabelId))
        rule.onNodeWithTag(testTagLargeCardStat + "MainText").assertTextContains(stat0Text)
        rule.onNodeWithTag(testTagLargeCardStat + "MainUnit").assertTextContains(rule.activity.getString(stat0Unit))

        // verify top left stats
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "Label", useUnmergedTree = true).assertTextContains(rule.activity.getString(stat1LabelId))
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "Text", useUnmergedTree = true).assertTextContains(stat1Text)
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "Unit", useUnmergedTree = true).assertTextContains(rule.activity.getString(stat1Unit))

        // tap top left stat to swap it and the main center stat
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "Text", useUnmergedTree = true).performClick()

        rule.waitForIdle()

        // verify main center stats
        rule.onNodeWithTag(testTagLargeCardStat + "MainLabel").assertTextContains(rule.activity.getString(stat1LabelId))
        rule.onNodeWithTag(testTagLargeCardStat + "MainText").assertTextContains(stat1Text)
        rule.onNodeWithTag(testTagLargeCardStat + "MainUnit").assertTextContains(rule.activity.getString(stat1Unit))

        // verify top left stats
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "Label", useUnmergedTree = true).assertTextContains(rule.activity.getString(stat0LabelId))
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "Text", useUnmergedTree = true).assertTextContains(stat0Text)
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "Unit", useUnmergedTree = true).assertTextContains(rule.activity.getString(stat0Unit))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun testDualStatLargeAndSmallCard() {
        rule.setContent {
            SimpleAppScreen(
                statInfoCollection = StatInfoCollection.fromListOfList(listOf(
                    listOf(
                        StatInfo(stat2LabelId, defaultIconId, stat2Text, stat2Unit, false, stat3Text, stat3Unit),
                        StatInfo(stat1LabelId, defaultIconId, stat1Text, stat1Unit),
                        StatInfo(stat2LabelId, defaultIconId, stat2Text, stat2Unit),
                        StatInfo(stat3LabelId, defaultIconId, stat3Text, stat3Unit),
                        StatInfo(stat4LabelId, defaultIconId, stat4Text, stat4Unit),
                    ),
                    listOf(
                        StatInfo(stat0LabelId, defaultIconId, stat0Text, stat0Unit, false, stat4Text, stat4Unit),
                        StatInfo(stat1LabelId, defaultIconId, stat1Text, stat1Unit, false, stat3Text, stat3Unit),
                        StatInfo(stat2LabelId, defaultIconId, stat2Text, stat2Unit),
                        StatInfo(stat3LabelId, defaultIconId, stat3Text, stat3Unit),
                        StatInfo(stat4LabelId, defaultIconId, stat4Text, stat4Unit),
                    ),
                ))
            )
        }

        // verify main center stats
        rule.onNodeWithTag(testTagLargeCardStat + "MainLabel").assertTextContains(rule.activity.getString(stat0LabelId))
        // left
        rule.onNodeWithTag(testTagLargeCardStat + "MainDualText"+"Left").assertTextContains(stat0Text)
        rule.onNodeWithTag(testTagLargeCardStat + "MainDualUnit"+"Left").assertTextContains(rule.activity.getString(stat0Unit))
        // right
        rule.onNodeWithTag(testTagLargeCardStat + "MainDualText"+"Right").assertTextContains(stat4Text)
        rule.onNodeWithTag(testTagLargeCardStat + "MainDualUnit"+"Right").assertTextContains(rule.activity.getString(stat4Unit))

        // verify top left stats
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "Label", useUnmergedTree = true).assertTextContains(rule.activity.getString(stat1LabelId))
        // left
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "DualText"+"Left", useUnmergedTree = true).assertTextContains(stat1Text)
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "DualUnit"+"Left", useUnmergedTree = true).assertTextContains(rule.activity.getString(stat1Unit))
        // right
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "DualText"+"Right", useUnmergedTree = true).assertTextContains(stat3Text)
        rule.onNodeWithTag(testTagLargeCardStat + "TopLeft" + "DualUnit"+"Right", useUnmergedTree = true).assertTextContains(rule.activity.getString(stat3Unit))

        // verify small card dual stats
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+0+"Label", useUnmergedTree = true).assertTextContains(rule.activity.getString(stat2LabelId))
        // left
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+0+"DualText"+"Left", useUnmergedTree = true).assertTextContains(stat2Text)
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+0+"DualUnit"+"Left", useUnmergedTree = true).assertTextContains(rule.activity.getString(stat2Unit))
        // right
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+0+"DualText"+"Right", useUnmergedTree = true).assertTextContains(stat3Text)
        rule.onNodeWithTag(testTagSmallCard+0+testTagPage+0+"DualUnit"+"Right", useUnmergedTree = true).assertTextContains(rule.activity.getString(stat3Unit))
    }

}