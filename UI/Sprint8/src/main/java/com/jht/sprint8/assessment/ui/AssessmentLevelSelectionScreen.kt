package com.jht.sprint8.assessment.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jht.sprint8.R
import com.jht.sprint8.Screen
import com.jht.sprint8.Sprint8ViewModel
import com.jht.sprint8.assessment.components.AssessmentLevelSelectionComp
import com.jht.sprint8.formatMinMaxText
import com.jht.sprint8.model.AssessmentSelectionCardData
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_ELITE_COLOR
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_INTERMEDIATE_COLOR
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_LIGHT_WHITE_COLOR
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_WHITE_COLOR
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_LIST_ITEM_COLOR
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_START_WORKOUT_BTN_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_1
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_2
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_RED
import com.jht.sprint8.utils.Sprint8Colors.WARMUP_ICON_BORDER_COLOR
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize

/**
 * Assessment Level Selection Screen
 * Displays a list of assessment levels (Beginner, Intermediate,
 * Advanced, and Elite) allowing the user to select their preferred
 * level before starting the assessment.
 * @param navController Handles navigation actions from the assessment level selection screen.
 * @param startSelectedWorkout Callback invoked when the user confirms and starts the selected
 * assessment workout.
 * @param sprint8ViewModel ViewModel responsible for providing assessment data and handling
 * business logic for level selection.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AssessmentLevelSelectionScreen(
    navController: NavController,
    startSelectedWorkout: () -> Unit,
    sprint8ViewModel: Sprint8ViewModel
) {
    val baseTag = "AssessmentLevelSelectionScreen"
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val categoryData by sprint8ViewModel.categorySprintSpeedRange.collectAsStateWithLifecycle()
    val categoryInclineData by sprint8ViewModel.categoryInclineRange.collectAsStateWithLifecycle()
    val labelRange = if (sprint8ViewModel.isRPMBasedMachine) {
        stringResource(R.string.resistance_range_tc)
    } else {
        stringResource(R.string.sprint_range_tc)
    }
    val speedLabel = if (sprint8ViewModel.isRPMBasedMachine) {
        ""
    } else {
        sprint8ViewModel.speedLabel
    }
    LaunchedEffect(Unit) {
        sprint8ViewModel.getSprint8Data()
    }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    if (categoryData.isNotEmpty()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val levelSelectionList = listOf(
                AssessmentSelectionCardData(
                    title = stringResource(R.string.beginner_tc),
                    borderColor = SPRINT8_NEW_RED
                ),
                AssessmentSelectionCardData(
                    title = stringResource(R.string.intermediate_tc),
                    borderColor = ASSESSMENT_INTERMEDIATE_COLOR
                ),
                AssessmentSelectionCardData(
                    title = stringResource(R.string.advanced_tc),
                    borderColor = CUSTOM_WORKOUT_START_WORKOUT_BTN_COLOR
                ),
                AssessmentSelectionCardData(
                    title = stringResource(R.string.elite_tc),
                    borderColor = ASSESSMENT_ELITE_COLOR
                )
            )
            Column(
                modifier = Modifier
                    .clickableNoEffect(onClick = {})
                    .size(screenWidth, screenHeight)
                    .background(
                        SPRINT8_BG_1,
                    )
                    .semantics {
                        testTagsAsResourceId = true
                    },
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 18.dp.computeSize(),
                            top = 18.dp.computeSize(),
                            end = 18.dp.computeSize()
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { navController.popBackStack() }
                            .wrapContentSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(painterResource(R.drawable.ic_back), contentDescription = null)
                        Text(
                            stringResource(R.string.back_tc),
                            fontSize = 22.sp.computeSize(),
                            color = SPRINT8_BG_2,
                            fontFamily = FontFamily.SansSerif,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.testTag("$baseTag:BackText:Text")
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clickable { navController.navigate(Screen.AssessmentInformationScreen.route) }
                            .wrapContentSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painterResource(R.drawable.ic_question),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp.computeSize())
                        )
                        Text(
                            stringResource(R.string.how_does_assessment_work_tc),
                            fontSize = 22.sp.computeSize(),
                            color = DynamicTheme.colors.text1,
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier
                                .wrapContentSize()
                                .testTag("$baseTag:AssessmentText:Text")
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 50.dp.computeSize()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        stringResource(R.string.select_start_point_tc),
                        fontSize = 42.sp.computeSize(),
                        color = ASSESSMENT_WHITE_COLOR,
                        fontFamily = FontFamily.SansSerif,
                        fontStyle = FontStyle.Normal,
                        modifier = Modifier
                            .padding(vertical = 8.dp.computeSize())
                            .testTag("$baseTag:Title:Text")
                    )
                    Text(
                        stringResource(R.string.sprint_assessment_desc_pg),
                        fontSize = 20.sp.computeSize(),
                        color = ASSESSMENT_LIGHT_WHITE_COLOR,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .padding(
                                bottom = 42.dp.computeSize(),
                                start = screenWidth * 0.15f,
                                end = screenWidth * 0.15f
                            )
                            .testTag("$baseTag:SubTitle:Text"),
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        levelSelectionList.forEachIndexed { index, assessmentSelectionCardData ->
                            val isSelected = selectedIndex == index
                            val cardColor =
                                if (isSelected) assessmentSelectionCardData.borderColor else assessmentSelectionCardData.borderColor.copy(
                                    alpha = 0.1f
                                )
                            val textColor =
                                if (isSelected) CUSTOM_WORKOUT_LIST_ITEM_COLOR else ASSESSMENT_WHITE_COLOR
                            val titleColor =
                                if (isSelected) CUSTOM_WORKOUT_LIST_ITEM_COLOR else assessmentSelectionCardData.borderColor
                            val inclineRange = categoryInclineData.getOrNull(index)?.let {
                                "0 - ${formatMinMaxText(it.first ?: 0f, it.second ?: 0f)}%"
                            } ?: ""
                            val speedOrResistanceRange = formatMinMaxText(
                                categoryData[index].first,
                                categoryData[index].second
                            )
                            val speedOrResistanceRangeText = "$speedOrResistanceRange $speedLabel"

                            AssessmentLevelSelectionComp(
                                title = assessmentSelectionCardData.title,
                                borderColor = assessmentSelectionCardData.borderColor,
                                onCardClick = {
                                    selectedIndex = if (isSelected) -1 else index
                                    selectedCategory = assessmentSelectionCardData.title
                                    sprint8ViewModel.selectCategory(index)
                                },
                                cardColors = cardColor,
                                textColor = textColor,
                                titleColor = titleColor,
                                frameType = sprint8ViewModel.frameType.value,
                                inclineRange = inclineRange,
                                speedOrResistanceRangeTitle = labelRange,
                                speedOrResistanceRangeSubtitle = speedOrResistanceRangeText
                            )
                        }
                    }

                    if (selectedIndex != -1) {
                        Button(
                            onClick = {
                                startSelectedWorkout()
                                sprint8ViewModel.onAssessmentButtonClicked()
                            },
                            shape = RoundedCornerShape(18.dp.computeSize()),
                            colors = ButtonDefaults.buttonColors(
                                WARMUP_ICON_BORDER_COLOR
                            ),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .wrapContentSize()
                                .padding(top = 42.dp.computeSize())
                                .testTag("$baseTag:StartWorkout:Button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(vertical = 8.dp.computeSize()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    stringResource(R.string.start_workout_tc),
                                    fontSize = 22.sp.computeSize(),
                                    color = SPRINT8_BG_1,
                                    fontFamily = FontFamily.SansSerif,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.testTag("$baseTag:ButtonLabel:Text")
                                )
                                Image(
                                    painterResource(R.drawable.ic_double_right_arrow),
                                    contentDescription = null,
                                    modifier = Modifier.testTag("$baseTag:RightArrowIcon:Image")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun AssessmentLevelSelectionScreenPreview() {
    AssessmentLevelSelectionScreen(
        navController = NavController(context = LocalContext.current),
        startSelectedWorkout = {},
        sprint8ViewModel = hiltViewModel()
    )
}