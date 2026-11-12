package com.jht.progression.ui.setup.assessment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.progression.R
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.progression.ui.widgets.MmSetupFrame
import com.jht.progression.ui.widgets.MmThemedButton
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.widgets.AutoSizeText
import com.jht.widgets.scaleSize

@Composable
fun AssessmentSetupScreen(
    onBackPressed: () -> Unit,
    onStartAssessment: () -> Unit,
    onSkipAssessment: () -> Unit
) {
    MmSetupFrame(onBackPressed = onBackPressed) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("Assessment Setup Screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 80.dp.scaleSize()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AutoSizeText(
                    text = stringResource(id = R.string.assessment_setup_title_tc),
                    modifier = Modifier
                        .size(500.dp.scaleSize(), 40.dp.scaleSize())
                        .weight(1f)
                        .testTag("assessment_title_string"),
                    color = Color.White,
                    maxLines = 1,
                    style = oxaniumStyle.copy(
                        fontSize = 36.sp.scaleSize(),
                        fontWeight = FontWeight.Medium,
                        lineHeight = 36.sp.scaleSize()
                    ),
                )

               Text(
                    text = stringResource(id = R.string.assessment_setup_subtitle_pg),
                    modifier = Modifier
                        .size(1100.dp.scaleSize(), 50.dp.scaleSize())
                        .weight(1.5f)
                        .testTag("assessment_subtitle_string"),
                    color = Color.White,
                    style = oxaniumStyle.copy(
                        fontSize = 18.sp.scaleSize(),
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 26.sp.scaleSize(),
                        textAlign = TextAlign.Center
                    )
                )

                Image(
                    modifier = Modifier
                        .size(500.dp.scaleSize(), 220.dp.scaleSize())
                        .weight(5.5f)
                        .testTag("bar_graph_image"),
                    painter = painterResource(id = R.drawable.bar_graph),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    contentDescription = "bar graph"
                )

                MmThemedButton(
                    text = stringResource(id = R.string.take_assessment_uc),
                    onClick = onStartAssessment,
                    modifier = Modifier
                        .size(440.dp.scaleSize(), 120.dp.scaleSize())
                        .weight(2f)
                        .testTag("take_assessment_button"),
                    color = MmColors.futuristicBlue,
                )
            }

            val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = Modifier.fillMaxSize()
            ) {
                EnterManualTimeEntryButton(
                    modifier = Modifier
                        .padding(start = 40.dp.scaleSize(), bottom = 40.dp.scaleSize())
                        .size(160.dp.scaleSize())
                        .clickableNoEffect(
                            interactionSource = interactionSource,
                            onClick = onSkipAssessment
                        )
                        .border(
                            border = BorderStroke(width = 4.dp.scaleSize(), color = Color.White),
                            shape = CutCornerShape(
                                topStart = 20.dp.scaleSize(),
                                bottomEnd = 20.dp.scaleSize()
                            )
                        )
                        .background(
                            color = if (isPressed) Color.White.copy(alpha = 0.2f) else Color.Black.copy(
                                0.1f
                            ),
                            shape = CutCornerShape(
                                topStart = 20.dp.scaleSize(),
                                bottomEnd = 20.dp.scaleSize()
                            )
                        )
                        .testTag("enter_manual_time_entry_button"),
                )
            }
        }
    }
}

@Composable
fun EnterManualTimeEntryButton(modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(6.dp.scaleSize()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_edit_pencil),
                contentDescription = "edit icon",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(bottom = 10.dp.scaleSize())
                    .size(38.dp.scaleSize())
            )

            //TODO - We will implement a logic for the selected program name after the 10k is ready.
            val programName = stringResource(id = R.string.five_k_tc)
            Text(
                text = stringResource(id = R.string.enter_manual_time_entry_tc, programName),
                style = oxaniumStyle.copy(
                    fontSize = 18.sp.scaleSize(),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp.scaleSize(),
                    textAlign = TextAlign.Center
                ),
                color = Color.White,
                maxLines = 2,
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewProgressionAssessmentSelectionScreen() {
    AssessmentSetupScreen({}, {}, {})
}

