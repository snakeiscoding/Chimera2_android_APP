package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_SAVE_BTN_COLOR
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * Displays a "Save" button used to save workout as a draft.
 */
@AllDevicePreviews
@Composable
fun SaveButtonComp() {
    Button(
        onClick = {
        },
        shape = RoundedCornerShape(18.dp.computeSize(1920f)),
        colors = ButtonDefaults.buttonColors(
            CUSTOM_WORKOUT_SAVE_BTN_COLOR
        ),
        modifier = Modifier
            .height(56.dp.computeSize(1920f))
            .width(330.dp.computeSize(1920f))
    ) {
        Text(
            text = "Save As Draft",
            fontSize = 24.sp.computeSize(1920f),
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,

            )
    }
}