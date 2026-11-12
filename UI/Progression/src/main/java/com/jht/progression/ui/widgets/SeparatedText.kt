package com.jht.progression.ui.widgets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.progression.oxaniumStyle
import com.jht.widgets.scaleSize

val colorGray = Color(0xFF7A7979)

/**
 * Creates 2 text views with a line in between them.
 */
@Composable
fun SeparatedText(
    modifier: Modifier = Modifier,
    textStart: String,
    textStartStyle: TextStyle = oxaniumStyle,
    textEnd: String,
    textEndStyle: TextStyle = oxaniumStyle
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = textStart,
            fontSize = 20.sp.scaleSize(),
            fontWeight = FontWeight.Bold,
            style = textStartStyle
        )

        Divider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp.scaleSize()),
            color = colorGray,
            thickness = 1.dp
        )

        Text(
            text = textEnd,
            fontSize = 20.sp.scaleSize(),
            fontWeight = FontWeight.Bold,
            style = textEndStyle
        )
    }
}

@Preview
@Composable
private fun TextLineWidgetPreview() {
    SeparatedText(
        textStart = "Left",
        textEnd = "Right"
    )
}