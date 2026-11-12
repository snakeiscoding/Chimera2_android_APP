package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.sprint8.R
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews

/**
 * Displays a "Get Ready" UI component used to prepare the user before an action
 * or workout begins.
 * @param title Text displayed to inform the user to get ready.
 * @param backGroundColor Background color of the component container.
 * @param modifier Modifier applied to the root composable for layout and styling.
 * @param screenWidth Screen width used to calculate responsive dimensions.
 * @param screenHeight Screen height used to calculate responsive dimensions.
 * @param borderColors List of colors used to draw the component border
 * (e.g., gradient or layered border).
 * @param isFullScreen Indicates whether the component is displayed in fullscreen
 * mode, affecting layout or sizing behavior.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun GetReadyComp(
    title: String,
    backGroundColor: Color,
    modifier: Modifier,
    screenWidth: Dp,
    screenHeight: Dp,
    borderColors: List<Color>,
    isFullScreen: Boolean = false
) {
    val componentTag = "GetReadyComp"
    Box(
        modifier = modifier
            .width(screenWidth * if (isFullScreen) 966 / 1920f else 788 / 1600f)
            .height(screenHeight * 80 / if (isFullScreen) 1080f else 913f)
            .border(
                BorderStroke(
                    screenWidth * 3 / 1600f,
                    brush = Brush.verticalGradient(borderColors)
                ), shape = RoundedCornerShape(screenWidth * 20 / 1600f)
            )
            .background(backGroundColor, shape = RoundedCornerShape(screenWidth * 20 / 1600f))
            .semantics {
                testTagsAsResourceId = true
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontSize = with(LocalDensity.current) { (screenHeight * 28 / 913f).toSp() },
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier
                .testTag("$componentTag:InfoText:Text"),
            color = Color.White
        )
    }
}

@AllDevicePreviews
@Composable
private fun GetReadyCompPreview() {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter)
    {
        DynamicTheme {
            GetReadyComp(
                title = stringResource(R.string.get_ready_tc),
                backGroundColor = Color.Black,
                modifier = Modifier,
                screenHeight = screenHeight,
                screenWidth = screenWidth,
                borderColors = listOf(Color.Green, Color.Red)
            )
        }
    }
}