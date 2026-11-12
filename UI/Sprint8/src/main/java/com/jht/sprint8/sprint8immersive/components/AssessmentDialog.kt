package com.jht.sprint8.sprint8immersive.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.model.AssessmentButtonConfig
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_DIALOG_BUTTON_BG_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.ASSESSMENT_DIALOG_BUTTON_BG_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BTN_CONTENT_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_1
import com.jht.sprint8.utils.Sprint8Colors.SUMMARY_GRAPH_BAR_COLOR_2
import com.jht.sprint8.utils.Sprint8Colors.TEXT_COLOR_1
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

/**
 * Composable function to display an assessment dialog.
 * @param modifier
 * Modifier applied to the dialog for layout, styling, or positioning.
 * @param buttonConfig
 * List of [AssessmentButtonConfig] objects defining the buttons
 * displayed in the dialog, including their labels, icons, colors,
 * and click behavior.
 * @param baseTestTag
 * Base test tag used for UI testing to uniquely identify dialog elements.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AssessmentDialog(
    modifier: Modifier = Modifier,
    buttonConfig: List<AssessmentButtonConfig>,
    baseTestTag: String
) {
    Column(
        modifier = modifier
            .wrapContentSize()
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.assessment_dialog_pg),
            fontSize = 20.sp.computeSize(1920f),
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = Color.White
        )
        Spacer(modifier = Modifier.size(20.dp.computeSizeHd()))
        buttonConfig.forEach { buttonConfig ->
            Button(
                onClick = buttonConfig.onButtonClick,
                shape = RoundedCornerShape(corner = CornerSize(10.dp.computeSizeHd())),
                border = BorderStroke(1.dp.computeSizeHd(), buttonConfig.buttonTextColor),
                colors = ButtonColors(
                    containerColor = buttonConfig.buttonBackgroundColor,
                    contentColor = buttonConfig.buttonTextColor,
                    disabledContainerColor = buttonConfig.buttonBackgroundColor,
                    disabledContentColor = buttonConfig.buttonTextColor
                ),
                modifier = Modifier
                    .width(277.dp.computeSizeHd())
                    .height(70.dp.computeSizeHd())
                    .testTag("$baseTestTag:${buttonConfig.buttonName}:Button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp.computeSizeHd())
                ) {
                    Icon(
                        painter = painterResource(buttonConfig.buttonIcon),
                        contentDescription = "${buttonConfig.buttonName} Button",
                        modifier = Modifier.size(24.dp.computeSizeHd())
                    )
                    Text(
                        buttonConfig.buttonName,
                        fontSize = 18.sp.computeSize(1920f),
                        color = buttonConfig.buttonTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.size(16.dp.computeSizeHd()))
        }
    }
}

@Composable
@AllDevicePreviews
private fun AssessmentDialogPreview() {
    Box(
        contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
    ) {
        AssessmentDialog(
            buttonConfig =
                listOf(
                    AssessmentButtonConfig(
                        buttonName = stringResource(R.string.level_up_tc),
                        buttonIcon = R.drawable.ic_sprint8_up,
                        buttonBackgroundColor = ASSESSMENT_DIALOG_BUTTON_BG_COLOR_1,
                        buttonTextColor = SUMMARY_GRAPH_BAR_COLOR_1,
                        onButtonClick = {}
                    ),
                    AssessmentButtonConfig(
                        buttonName = stringResource(R.string.level_down_tc),
                        buttonIcon = R.drawable.ic_sprint8_down,
                        buttonBackgroundColor = ASSESSMENT_DIALOG_BUTTON_BG_COLOR_2,
                        buttonTextColor = SUMMARY_GRAPH_BAR_COLOR_2,
                        onButtonClick = {}
                    ),
                    AssessmentButtonConfig(
                        buttonName = stringResource(R.string.stay_here_tc),
                        buttonIcon = R.drawable.ic_sprint8_check,
                        buttonBackgroundColor = TEXT_COLOR_1,
                        buttonTextColor = SPRINT8_BTN_CONTENT_COLOR_2,
                        onButtonClick = {}
                    )
                ), baseTestTag = "Sprint8Plus"
        )
    }
}
