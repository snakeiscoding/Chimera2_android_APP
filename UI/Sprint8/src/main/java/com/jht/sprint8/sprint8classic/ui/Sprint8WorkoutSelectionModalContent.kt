package com.jht.sprint8.sprint8classic.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jht.data.WorkoutInput
import com.jht.data.WorkoutInputUi
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.resources.robotoFonts
import com.jht.sprint8.R
import com.jht.sprint8.Screen
import com.jht.sprint8.sprint8classic.components.Sprint8ToggleButton
import com.jht.sprint8.utils.Sprint8TypeData
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.widgets.FocusCropImage
import com.jht.widgets.PillboxGrid

/**
 * Displays the content of a modal for selecting a Sprint8 workout.
 *
 * This composable shows a title, image, description, and inputs for configuring
 * the workout.
 *
 * @param navController [NavController] used for navigation within the modal.
 * @param modifier Modifier applied to the root composable for layout and styling.
 * @param title Title text displayed at the top of the modal.
 * @param imageUri URI of the image displayed in the modal.
 * @param description Description text providing details about the workout.
 * @param workoutInputList List of [WorkoutInput] representing user-adjustable inputs.
 * @param mapInputToFloat Map linking input names to mutable float states for user input.
 * @param onDismiss Callback for dismissing parent.
 * @param startSelectedWorkout Callback invoked when the user starts the selected workout.
 * @param content Composable lambda that allows adding custom content to the modal,
 * receiving the startSelectedWorkout callback.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Sprint8WorkoutSelectionModalContent(
    navController: NavController,
    modifier: Modifier,
    title: String,
    imageUri: Uri,
    description: String,
    workoutInputList: List<WorkoutInputUi>,
    onDismiss: () -> Unit = {},
    startSelectedWorkout: () -> Unit,
    content: @Composable (startSelectedWorkout: () -> Unit) -> Unit,
) {
    val baseTag = "Sprint8SetupModal"
    Box (modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(30.dp)
                )
                .fillMaxSize(0.7f)
                .height(710.dp.computeSize())
                .align(Alignment.Center)
        ) {
            Row(
                modifier = modifier.semantics {
                    testTagsAsResourceId = true
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1.8f)
                        .clip(shape = RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp))
                ) {
                    FocusCropImage(
                        modifier = Modifier
                            .fillMaxSize(),
                        model = imageUri,
                    )
                    Image(
                        painter = painterResource(R.drawable.sprint_8_graph),
                        contentDescription = "sprint8_graph",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                horizontal = 24.dp.computeSize(),
                                vertical = 24.dp.computeSize()
                            )
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(3.2f)
                        .padding(
                            top = 36.dp.computeSize(),
                            start = 24.dp.computeSize(),
                            end = 24.dp.computeSize(),
                            bottom = 24.dp.computeSize()
                        ),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = title,
                        color = DynamicTheme.colors.text1,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = robotoFonts,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp.computeSize(),
                        ),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.testTag("$baseTag:SelectedWorkoutTitle:Text"),
                    )
                    Spacer(Modifier.height(12.dp.computeSize()))
                    Text(
                        text = description,
                        color = Color(0xCCEFEFEF), // this shade is not defined in codebase
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = robotoFonts,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp.computeSize(),
                            lineHeight = 1.4.em,
                            letterSpacing = 0.02.sp,
                        ),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.testTag("$baseTag:DescriptionTitle:Text")
                    )
                    Spacer(Modifier.height(20.dp.computeSize()))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp.computeSize()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = com.jht.resources.R.drawable.ic_sprint_8),
                            contentDescription = "Sprint8 Icon",
                            modifier = Modifier
                                .size(25.dp.computeSize())
                                .testTag("$baseTag:Sprint8Icon:Image")
                        )
                        Text(
                            text = stringResource(id = R.string.sprint8_tc),
                            color = Color(0xFFD1D1D1), // this shade is not defined in the codebase
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = robotoFonts,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp.computeSize(),
                            ),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.testTag("$baseTag:SwitchStrideLabel:Text")
                        )
                    }
                    Spacer(Modifier.height(15.dp.computeSize()))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp.computeSize()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = com.jht.resources.R.drawable.ic_clock),
                            contentDescription = "Clock Icon",
                            modifier = Modifier
                                .size(25.dp.computeSize())
                                .testTag("$baseTag:ClockIcon:Image")
                        )
                        Text(
                            text = stringResource(R.string.mins_tc),
                            color = Color(0xFFD1D1D1), // this shade is not defined in the codebase
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = robotoFonts,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp.computeSize(),
                            ),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.testTag("$baseTag:SprintTime:Text")
                        )
                    }
                    Spacer(Modifier.height(15.dp.computeSize()))
                    if (Sprint8TypeData.allowSelection) {
                        Sprint8ToggleButton()
                    }
                    Spacer(Modifier.height(15.dp.computeSize()))

                    PillboxGrid(
                        modifier = Modifier
                            .height(80.dp.computeSize())
                            .aspectRatio(4f),
                        workoutInputList = workoutInputList,
                        rows = 1,
                        columns = 1
                    )
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth().height(50.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(
                            modifier = Modifier
                                .clickable { navController.navigate(Screen.Sprint8InformationScreen.route) }
                        ){
                            Image(
                                painter = painterResource(com.jht.resources.R.drawable.ic_info),
                                contentDescription = null,
                                modifier = Modifier.testTag("$baseTag:InfoIcon:Image")
                            )
                            Text(
                                text = stringResource(R.string.how_does_sprint8_work_tc),
                                modifier = Modifier
                                    .requiredWidth(IntrinsicSize.Max)
                                    .testTag("$baseTag:Heading:Text"),
                                color = DynamicTheme.colors.text1,
                                fontSize = 16.sp.computeSize(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textDecoration = TextDecoration.Underline
                                ),
                                textAlign = TextAlign.End,
                            )
                        }
                        content { startSelectedWorkout() }
                    }
                }
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun Sprint8WorkoutSelectionModalPreview() {
    val mock = MockWorkoutCatalogModel()
    Box(
        modifier = Modifier
            .fillMaxSize()
    )

    // get modifier from FeaturedModalBase

    Sprint8WorkoutSelectionModalContent(
        navController = NavController(LocalContext.current),
        title = "Title",
        description = "A level based 20-minute High Intensity Interval Training (HIIT) workout program that automatically adjusts the exercise intensity to maximize results.",
        imageUri = Uri.EMPTY,
        workoutInputList = mock.getWorkoutInputsForPreview(),
        startSelectedWorkout = {},
        modifier = Modifier
            .background(
                color = DynamicTheme.colors.background4,
                shape = RoundedCornerShape(30.dp)
            )
            .border(0.2.dp, Color.White.copy(alpha = 0.4f), shape = RoundedCornerShape(30.dp))
            .clickableNoEffect(
                onClick = { }, //do nothing, so clicking inside the settings window doesn't dismiss it
            )
            .fillMaxSize(),
        content = {}
    )
}
