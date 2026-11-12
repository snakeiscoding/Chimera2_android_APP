package com.jht.spinbikeapps.targettraining.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GREY_TEXT
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import java.util.Locale

@Composable
internal fun RPMComp(
    modifier: Modifier = Modifier, rpm: String = "", textGlowColor: Color = Color.White
){
    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tight glow
        Image(
            painter = painterResource(R.drawable.ic_rpm_meter),
            contentDescription = null,
            modifier = modifier.size(
                60.dp.computeSize(1920f)
            )
        )

        Text(
            text = rpm,
            fontSize = 120.sp.computeSize(1920f),
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            style = LocalTextStyle.current.copy(
                shadow = Shadow(
                    color = textGlowColor.copy(alpha = 0.8f),
                    offset = Offset(0f, 0f),
                    blurRadius = 8f
                )
            )
        )

        Text(
            text = stringResource(R.string.rpm_uc).lowercase(Locale.getDefault()),
            fontSize = 20.sp.computeSize(1920f),
            color = GREY_TEXT,
            fontFamily = FontFamily.SansSerif,
            style = LocalTextStyle.current.copy(
                shadow = Shadow(
                    color = textGlowColor.copy(alpha = 0.6f),
                    offset = Offset(0f, 0f)
                )
            )
        )
    }
}

@AllDevicePreviews
@Composable
private fun RPMCompPreview(){
    RPMComp()
}