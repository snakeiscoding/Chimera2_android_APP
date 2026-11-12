package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Displays an information card for Sprint 8, showing an icon, title, subtitle,
 * and customizable border styling.
 * @param icon Drawable resource ID representing the icon to display.
 * @param title Main title text displayed on the information card.
 * @param subTitle Supporting text providing additional details or instructions.
 * @param borderColor Color used for the default border of the card.
 * @param screenWidth Screen width used for responsive layout calculations.
 * @param screenHeight Screen height used for responsive layout calculations.
 * @param iconColor Optional color to tint the icon. If null, default coloring is applied.
 * @param borderStroke Border stroke configuration. Defaults to a 1.dp stroke using [borderColor].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Sprint8InformationComp(
    icon: Int, title: String,
    subTitle: String,
    borderColor: Color,
    screenWidth: Dp,
    screenHeight: Dp,
    iconColor: Color? = null,
    borderStroke: BorderStroke = BorderStroke(1.dp.computeSize(), borderColor),
) {
    val componentTag = "Sprint8InformationComp"
    Card(
        modifier = Modifier
            .width(screenWidth * 0.485f)
            .height(screenHeight * 0.153f)
            .padding(start = screenWidth * 0.016f)
            .semantics {
                testTagsAsResourceId = true
            },
        shape = RoundedCornerShape(20.dp.computeSize()),
        colors = CardDefaults.cardColors(containerColor = borderColor.copy(0.1f)),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 16.dp.computeSize())
                    .testTag("$componentTag:Icon:Image"),
                colorFilter = iconColor?.let { ColorFilter.tint(it) },


                )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(end = 36.dp.computeSize()),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    title,
                    fontSize = 26.sp.computeSize(),
                    color = DynamicTheme.colors.text1,
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("$componentTag:Title:Text")
                )
                Text(
                    subTitle,
                    fontSize = 18.sp.computeSize(),
                    color = DynamicTheme.colors.text1,
                    fontStyle = FontStyle.Normal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("$componentTag:SubTitle:Text")
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
fun Sprint8InformationCompPreview(){
    Sprint8InformationComp(
        icon = R.drawable.mock_icon,
        title = stringResource(R.string.title_tc),
        subTitle = "Subtitle",
        borderColor = Color.Green,
        screenWidth = 1000.dp,
        screenHeight = 1000.dp,
        iconColor = Color.Yellow,
        borderStroke = BorderStroke(1.dp, Color.Red)
    )
}