package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.BORDER_GRADIENT
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_BG_1
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews

/**
 * Displays a "Skip" button that allows the user to bypass a step or screen.
 * @param modifier Modifier applied to the button for layout, styling, and interaction.
 * @param screenHeight Current screen height used for responsive sizing.
 * @param screenWidth Current screen width used for responsive sizing.
 * @param onClickAction Callback invoked when the skip button is clicked.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SkipButton(
    modifier: Modifier,
    screenHeight: Dp,
    screenWidth: Dp,
    onClickAction: () -> Unit
) {
    val testTagPrefix = "Sprint8"
    Box(
        modifier = modifier
            .padding(bottom = screenHeight * 40 / 913f, end = screenWidth * 140 / 1600f)
            .width(screenWidth * 160 / 1600f)
            .height(screenHeight * 58 / 913f)
            .clickable { onClickAction.invoke() }
            .background(
                color = Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(screenWidth * 18 / 1600f)
            )
            .border(
                screenWidth * 3 / 1600f,
                BORDER_GRADIENT,
                RoundedCornerShape(screenWidth * 18 / 1600f)
            )
            .semantics {
                testTagsAsResourceId = true
            }
            .testTag("$testTagPrefix:Skip:Button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.skip_uc),
                fontSize = with(LocalDensity.current) { (screenHeight * 24 / 913f).toSp() },
                modifier = Modifier.testTag("$testTagPrefix:Skip:Text"),
                color = SPRINT8_BG_1,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(screenWidth * 8 / 1600f))
            Icon(
                painter = painterResource(R.drawable.ic_double_right),
                contentDescription = null,
                modifier = Modifier
                    .size(screenHeight * 32 / 913f)
                    .testTag("$testTagPrefix:RightArrowIcon:Image")
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun SkipButtonPreview() {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
    {
        DynamicTheme {
            SkipButton(
                modifier = Modifier,
                screenHeight = screenHeight,
                screenWidth = screenWidth
            ) {}
        }
    }
}
