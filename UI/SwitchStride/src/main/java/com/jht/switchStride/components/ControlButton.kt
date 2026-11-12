package com.jht.switchStride.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.switchStride.R
import com.jht.switchStride.model.ButtonConfig
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

@Composable
internal fun ControlButton(buttonConfig: ButtonConfig) {
    Button(
        onClick = buttonConfig.onClick,
        modifier = Modifier.size(width = 460.dp.computeSizeHd(), height = 100.dp.computeSizeHd()),
        shape = RoundedCornerShape(size = 18.dp.computeSizeHd()),
        border = BorderStroke(
            width = 1.dp.computeSizeHd(), brush = Brush.linearGradient(
                colors = listOf(
                    buttonConfig.buttonColor.copy(alpha = 0.6f), buttonConfig.buttonColor
                )
            )
        ),
        colors = ButtonDefaults.buttonColors(containerColor = buttonConfig.buttonColor.copy(alpha = 0.15f))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 10.dp.computeSizeHd()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = buttonConfig.buttonIcon),
                contentDescription = "Button Icon",
                modifier = Modifier.size(size = 48.dp.computeSizeHd()),
                tint = buttonConfig.buttonColor
            )
            Text(
                text = stringResource(id = buttonConfig.buttonName),
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp.computeSize(designWidth = 1920f),
                color = buttonConfig.buttonColor
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun ControlButtonPreview(){
    ControlButton(
        buttonConfig = ButtonConfig(
            buttonIcon = R.drawable.icon_heart_rate_white,
            buttonName = R.string.id_tc,
            buttonColor = Color.White,
            onClick = {  }
        )
    )
}