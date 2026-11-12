package com.jht.sprint8.sprint8classic.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jht.sprint8.R
import com.jht.sprint8.Screen
import com.jht.sprint8.Sprint8ViewModel
import com.jht.sprint8.formatMinMaxText
import com.jht.sprint8.sprint8classic.components.ExperienceSelectionComp
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_1
import com.jht.sprint8.utils.Sprint8TypeData
import com.jht.sprint8.utils.WorkoutMode
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Composable screen that allows users to select their experience level
 * for the Sprint8 workout.
 * @param navController [NavController] used for navigation between screens.
 * @param viewModel [Sprint8ViewModel] providing data and state for the experience selection.
 * Defaults to a Hilt-injected ViewModel.
 */
@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun ExperienceSelectionScreen(
    navController: NavController,
    viewModel: Sprint8ViewModel = hiltViewModel(),
) {
    val sprint8Type = Sprint8TypeData.selectedMode
    val categoryData by viewModel.categorySprintSpeedRange.collectAsStateWithLifecycle()
    val labelRange = if (viewModel.isRPMBasedMachine) {
        stringResource(R.string.resistance_range_tc)
    } else {
        stringResource(R.string.sprint_range_tc)
    }

    val speedLabel = if (viewModel.isRPMBasedMachine) {
        ""
    } else {
        viewModel.speedLabel
    }


    LaunchedEffect(Unit) {
        viewModel.getSprint8Data()
    }

    if (categoryData.isNotEmpty()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            println("Tapped outside")
                        }
                    )
                }
                .background(
                    SPRINT8_BG_1,
                )
                .semantics {
                    testTagsAsResourceId = true
                }
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val baseTag = "ExperienceSelectionScreen"
            Box(
                modifier = Modifier
                    .size(screenWidth, screenHeight)
                    .background(
                        SPRINT8_BG_1,
                    )
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.CenterStart),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.select_your_workout_experience_tc),
                        fontSize = 32.sp.computeSize(),
                        color = DynamicTheme.colors.text1,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .padding(bottom = screenHeight * 0.046f)
                            .testTag("$baseTag:Heading:Text")
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ExperienceSelectionComp(
                            letter = "B", // TODO: CGC-23762 Update later to strings translations, set as literals for now until we decide what todo with the values in other languages
                            title = stringResource(R.string.beginner_tc),
                            subTitle = labelRange.plus(
                                " ${
                                    formatMinMaxText(
                                        categoryData[0].first,
                                        categoryData[0].second
                                    )
                                } $speedLabel"                            ),
                            borderColor = DynamicTheme.colors.background9,
                            gradientColors = listOf(
                                DynamicTheme.colors.text1,
                                DynamicTheme.colors.background9,
                                DynamicTheme.colors.background9
                            ),
                            borderGradientColor = listOf(
                                DynamicTheme.colors.background9,
                                DynamicTheme.colors.background9.copy(alpha = 0f)
                            ),
                            screenHeight = screenHeight,
                            screenWidth = screenWidth,
                            modifier = Modifier.clickable {
                                viewModel.getSprint8Data()
                                viewModel.selectCategory(0)
                                navController.navigate(Screen.Sprint8PreviewScreen.route)
                            }
                                .testTag("$baseTag:Beginner:Button")
                        )
                        Spacer(modifier = Modifier.size(24.dp.computeSize()))
                        ExperienceSelectionComp(
                            letter = "I", // TODO: CGC-23762 Update later to strings translations, set as literals for now until we decide what todo with the values in other languages
                            title = stringResource(R.string.intermediate_tc),
                            subTitle = labelRange.plus(
                                " ${
                                    formatMinMaxText(
                                        categoryData[1].first,
                                        categoryData[1].second
                                    )
                                } $speedLabel"
                            ),
                            borderColor = DynamicTheme.colors.background7,
                            gradientColors = listOf(
                                Color.White,
                                DynamicTheme.colors.background7,
                                DynamicTheme.colors.background7,
                            ),
                            borderGradientColor = listOf(
                                DynamicTheme.colors.background7,
                                DynamicTheme.colors.background7.copy(alpha = 0f)
                            ),
                            screenHeight = screenHeight,
                            screenWidth = screenWidth,
                            modifier = Modifier.clickable {
                                viewModel.getSprint8Data()
                                viewModel.selectCategory(1)
                                navController.navigate(Screen.Sprint8PreviewScreen.route)
                            }
                                .testTag("$baseTag:Intermediate:Button")
                        )
                        Spacer(modifier = Modifier.size(24.dp.computeSize()))
                        ExperienceSelectionComp(
                            letter = "A", // TODO: CGC-23762 Update later to strings translations, set as literals for now until we decide what todo with the values in other languages
                            title = stringResource(R.string.advanced_tc),
                            subTitle = labelRange.plus(
                                " ${
                                    formatMinMaxText(
                                        categoryData[2].first,
                                        categoryData[2].second
                                    )
                                } $speedLabel"
                            ),
                            borderColor = DynamicTheme.colors.background5,
                            gradientColors = listOf(
                                DynamicTheme.colors.text1,
                                DynamicTheme.colors.background5,
                                DynamicTheme.colors.background5
                            ),
                            borderGradientColor = listOf(
                                DynamicTheme.colors.background5,
                                DynamicTheme.colors.background5.copy(alpha = 0f)
                            ),
                            screenHeight = screenHeight,
                            screenWidth = screenWidth,
                            modifier = Modifier.clickable {
                                viewModel.getSprint8Data()
                                viewModel.selectCategory(2)
                                navController.navigate(Screen.Sprint8PreviewScreen.route)
                            }
                                .testTag("$baseTag:Advanced:Button")
                        )
                        Spacer(modifier = Modifier.size(24.dp.computeSize()))
                        ExperienceSelectionComp(
                            letter = "E", // TODO: CGC-23762 Update later to strings translations, set as literals for now until we decide what todo with the values in other languages
                            title = stringResource(R.string.elite_tc),
                            subTitle = labelRange.plus(
                                " ${
                                    formatMinMaxText(
                                        categoryData[3].first,
                                        categoryData[3].second
                                    )
                                } $speedLabel"
                            ),
                            borderColor = DynamicTheme.colors.background6,
                            gradientColors = listOf(
                                DynamicTheme.colors.text1,
                                DynamicTheme.colors.background6,
                                DynamicTheme.colors.background6
                            ),
                            borderGradientColor = listOf(
                                DynamicTheme.colors.background6,
                                DynamicTheme.colors.background6.copy(alpha = 0f)
                            ),
                            screenHeight = screenHeight,
                            screenWidth = screenWidth,
                            modifier = Modifier.clickable {
                                viewModel.getSprint8Data()
                                viewModel.selectCategory(3)
                                navController.navigate(Screen.Sprint8PreviewScreen.route)
                            }
                                .testTag("$baseTag:Elite:Button")
                        )
                        Spacer(modifier = Modifier.size(24.dp.computeSize()))
                        ExperienceSelectionComp(
                            letter = "C", // TODO: CGC-23762 Update later to strings translations, set as literals for now until we decide what todo with the values in other languages
                            title = stringResource(R.string.custom_dots_tc),
                            subTitle = stringResource(R.string.create_a_custom_sprint_8_pg),
                            borderColor = DynamicTheme.colors.background8,
                            gradientColors = listOf(
                                DynamicTheme.colors.text1,
                                DynamicTheme.colors.background8,
                                DynamicTheme.colors.background8
                            ),
                            borderGradientColor = listOf(
                                DynamicTheme.colors.background8,
                                DynamicTheme.colors.background8.copy(alpha = 0f)
                            ),
                            screenHeight = screenHeight,
                            screenWidth = screenWidth,
                            modifier = Modifier.clickable {
                                viewModel.getSprint8Data()
                                viewModel.selectCategory(4)
                                navController.navigate(Screen.Sprint8PreviewScreen.route)
                            }
                                .testTag("$baseTag:Custom:Button")
                        )
                    }
                }
                if (sprint8Type == WorkoutMode.Immersive) {
                    Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .wrapContentSize()
                        .padding(bottom = screenHeight * 0.08f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.do_not_know_your_level),
                        fontSize = 22.sp.computeSize(),
                        color = DynamicTheme.colors.text1,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(bottom = screenHeight * 0.024f)
                            .testTag("$baseTag:Label:Text")
                    )
                        Button(
                            onClick = { navController.navigate(Screen.AssessmentLevelSelectionScreen.route)},
                            enabled = true,
                            shape = RoundedCornerShape(18.dp.computeSize()),
                            border = BorderStroke(2.dp, color = Color.White),
                            colors = ButtonDefaults.buttonColors(
                                Color.Black
                            ), modifier = Modifier
                                .testTag("$baseTag:TakeAssessment:Button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(vertical = screenHeight * 0.008f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    stringResource(R.string.take_assessment_tc),
                                    fontSize = 22.sp.computeSize(),
                                    color = DynamicTheme.colors.text1,
                                    fontFamily = FontFamily.SansSerif,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .testTag("$baseTag:TakeAssessment:Text")
                                )
                                Image(
                                    painterResource(R.drawable.ic_double_right_arrow),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(DynamicTheme.colors.text1),
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
fun ExperienceSelectionPreview() {
    ExperienceSelectionScreen(
        navController = NavController(context = LocalContext.current),
    )
}