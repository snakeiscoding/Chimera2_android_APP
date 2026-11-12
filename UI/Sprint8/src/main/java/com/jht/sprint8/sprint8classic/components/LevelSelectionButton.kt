package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Displays a group of selectable buttons for choosing a Sprint 8 level.
 *
 * @param levels List of level labels displayed on the buttons.
 * @param modifier Modifier applied to the button group for layout and styling.
 * @param buttonColors List of colors used to style the buttons (e.g., normal,
 * selected, or gradient states).
 * @param screenWidth Screen width used to calculate responsive dimensions.
 * @param screenHeight Screen height used to calculate responsive dimensions.
 * @param selectedIndex Index of the currently selected level.
 * @param onLevelSelected Callback invoked when a level is selected, providing
 * the selected index.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8LevelSelectionButtonGroup(
    levels: List<String>,
    modifier: Modifier,
    buttonColors: List<Color>,
    screenWidth: Dp,
    screenHeight: Dp,
    selectedIndex: Int = 0,
    onLevelSelected: (Int) -> Unit = {}
) {
    val componentTag = "Sprint8LevelSelectionButtonGroup"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(top = screenHeight * 0.034f, bottom = screenHeight * 0.034f)
            .semantics {
                testTagsAsResourceId = true
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        levels.forEachIndexed { index, level ->
            Button(
                modifier = modifier
                    .padding(horizontal = 12.dp.computeSize())
                    .border(
                        border = BorderStroke(
                            1.dp.computeSize(),
                            if (selectedIndex == index) Color.Transparent else buttonColors[index]
                        ),
                        shape = RoundedCornerShape(32.dp.computeSize())
                    )
                    .heightIn(min = screenHeight * 0.064f)
                    .fillMaxHeight()
                    .width(screenWidth)
                    .testTag("$componentTag:Level$index:Button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedIndex == index) buttonColors[index] else buttonColors[index].copy(
                        0.1f
                    )
                ),
                onClick = {
                    onLevelSelected(index)
                },
            ) {
                Text(
                    text = level,
                    fontSize = 22.sp.computeSize(),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            if (selectedIndex == index) {
                                listOf(Color.Black, Color.Black)
                            } else {
                                listOf(DynamicTheme.colors.text1, Color.Gray)
                            }
                        )
                    ),
                    fontFamily = FontFamily.SansSerif,
                    modifier = modifier.testTag("$componentTag:Level:Text")
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
fun Sprint8LevelSelectionButtonGroupPreview(){
    Sprint8LevelSelectionButtonGroup(
        levels = listOf(stringResource(R.string.a_uc)),
        modifier = Modifier,
        buttonColors = listOf(Color.Red),
        screenWidth = 1000.dp,
        screenHeight = 1000.dp,
        onLevelSelected = {}
    )
}
