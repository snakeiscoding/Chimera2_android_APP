package com.jht.sprint8.assessment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.jht.sprint8.R
import com.jht.sprint8.sprint8classic.components.Sprint8InformationComp
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_WARMUP_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_DISABLED_BTN_COLOR
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Assessment Information Screen
 * Displays information explaining how the assessment works.
 * The screen guides the user through the three assessment phases:
 * Warm-up, Run a Sprint, and Adjust. It is intended to help users
 * understand the assessment flow before starting.
 * @param navController Used to handle navigation actions from the assessment information screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AssessmentInformationScreen(
    navController: NavController? = null,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val baseTag = "AssessmentInformationScreen"

    Box(
        modifier = Modifier
            .wrapContentSize()
            .background(
                Color.Transparent,
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        println("Tapped outside")
                    }
                )
            }
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        IconButton(
            onClick = {
                navController?.popBackStack()
            }, modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(1f)
                .padding(end = screenWidth * 0.14f, top = screenHeight * 0.101f)
                .size(24.dp.computeSize())
                .testTag("$baseTag:IconClose:Button")

        ) {
            Image(
                painter = painterResource(R.drawable.ic_round_close),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = screenWidth * 0.141f, vertical = screenHeight * 0.101f)
                .border(
                    1.dp.computeSize(),
                    Color.Gray.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(30.dp.computeSize())
                )
                .clip(RoundedCornerShape(30.dp.computeSize()))
                .background(
                    DynamicTheme.colors.onSecondary,
                    shape = RoundedCornerShape(12.dp.computeSize())
                )
        ) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterEnd)
                    .padding(end = screenHeight * 0.088f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(top = screenHeight * 0.140f, bottom = screenHeight * 0.04f),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .height(2.dp.computeSize())
                            .width(screenWidth * 0.024f)
                            .padding(end = 2.dp.computeSize())
                            .align(Alignment.TopEnd),
                        color = DynamicTheme.colors.text1
                    )
                    VerticalDivider(
                        modifier = Modifier
                            .height(screenHeight * 0.65f)
                            .padding(bottom = 2.dp.computeSize())
                            .width(2.dp.computeSize()),
                        color = DynamicTheme.colors.text1
                    )
                    HorizontalDivider(
                        modifier = Modifier
                            .height(2.dp.computeSize())
                            .width(screenWidth * 0.024f)
                            .padding(end = 2.dp.computeSize())
                            .align(Alignment.BottomCenter),
                        color = DynamicTheme.colors.text1
                    )
                }
                Column(
                    modifier = Modifier.padding(start = screenWidth * 0.012f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_repeat),
                        contentDescription = null,
                        modifier = Modifier
                    )
                    Text(
                        stringResource(R.string.repeat_8_x),
                        fontSize = 20.sp.computeSize(),
                        color = Color.White,
                        fontStyle = FontStyle.Normal,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.testTag("$baseTag:Repeat8xText:Text")
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = screenHeight * 0.1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.how_does_assessment_work_tc),
                    fontSize = 32.sp.computeSize(),
                    color = DynamicTheme.colors.text1,
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = screenHeight * 0.050f)
                        .testTag("$baseTag:AssessmentText:Text")

                )
                Sprint8InformationComp(
                    icon = R.drawable.ic_person,
                    title = stringResource(R.string.warmup_tc),
                    subTitle = stringResource(R.string.sprint8_walk_desc_pg),
                    borderColor = ASSESSMENT_WARMUP_COLOR,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    borderStroke = BorderStroke(0.dp.computeSize(), Color.Transparent)
                )
                Spacer(modifier = Modifier.size(26.dp.computeSize()))
                Sprint8InformationComp(
                    icon = R.drawable.ic_run,
                    title = stringResource(R.string.run_a_sprint_tc),
                    subTitle = stringResource(R.string.assessment_run_info_pg),
                    borderColor = DynamicTheme.colors.background6,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    borderStroke = BorderStroke(0.dp.computeSize(), Color.Transparent)
                )
                Spacer(modifier = Modifier.size(26.dp.computeSize()))
                Sprint8InformationComp(
                    icon = R.drawable.ic_hand,
                    title = stringResource(R.string.adjust_tc),
                    subTitle = stringResource(R.string.sprint8_adjust_desc_pg),
                    borderColor = SPRINT8_DISABLED_BTN_COLOR,
                    screenWidth = screenWidth,
                    iconColor = Color.White,
                    screenHeight = screenHeight,
                    borderStroke = BorderStroke(0.dp.computeSize(), Color.Transparent)
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
private fun Sprint8InfoScreenPreview() {
    AssessmentInformationScreen(
        navController = NavController(context = LocalContext.current)
    )
}