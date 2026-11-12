package com.jht.sprint8.sprint8immersive.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_NEW_ACTIVE_RECOVERY_COLOR
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Composable function to display a "Skip" button in the Sprint8 workflow.
 * @param modifier Modifier applied to the skip button for layout, styling, or positioning.
 * @param onSkipClick Callback invoked when the user taps the skip button.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8NewSkipButton(
    modifier: Modifier,
    onSkipClick: () -> Unit
) {
    val componentTag = "Sprint8Skip"
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(20.dp.computeSize())
            .clickable { onSkipClick() }
            .border(
                border = BorderStroke(
                    1.dp.computeSize(),
                    color = SPRINT8_NEW_ACTIVE_RECOVERY_COLOR
                ),
                shape = RoundedCornerShape(12.dp.computeSize())
            )
            .background(
                color = SPRINT8_NEW_ACTIVE_RECOVERY_COLOR.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp.computeSize())
            )
            .semantics {
                testTagsAsResourceId = true
            }
            .testTag("$componentTag:Button")
    ) {
        Row(
            modifier = modifier
                .padding(horizontal = 16.dp.computeSize(), vertical = 12.dp.computeSize()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.skip_tc),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                color = SPRINT8_NEW_ACTIVE_RECOVERY_COLOR,
                modifier = modifier.testTag("$componentTag:Text")
            )
            Image(
                painter = painterResource(R.drawable.ic_skip_new,),
                modifier = modifier.testTag("$componentTag:Image"),
                contentDescription = null
            )
        }
    }
}


@AllDevicePreviews
@Composable
fun Sprint8NewSkipBtnPreview(){
    Sprint8NewSkipButton(
        modifier = Modifier,
    ) { }
}