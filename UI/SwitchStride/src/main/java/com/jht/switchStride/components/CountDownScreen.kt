package com.jht.switchStride.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jht.switchStride.R
import com.jht.switchStride.utils.runScreenForwardBlue
import com.jht.switchStride.utils.runScreenReversePurple
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize


@Composable
internal fun CountdownScreen(countdownTime: Int = 3, isBeltInReverse: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(id = if (isBeltInReverse) R.drawable.purple_background else R.drawable.blue_background),
                contentScale = ContentScale.FillBounds
            ), contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val offset = Offset(4f, 4f)
            Text(
                text = stringResource(id = R.string.are_you_ready_uc), style = TextStyle(
                    fontSize = 68.sp.computeSize(designWidth = 1920f),
                    color = if (isBeltInReverse) runScreenReversePurple else runScreenForwardBlue,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color(0x800055ff), offset = Offset(x = 4f, y = 4f), blurRadius = 4f
                    )
                )
            )
            Text(
                text = countdownTime.toString(), style = TextStyle(
                    fontSize = 420.sp.computeSize(designWidth = 1920f),
                    color = if (isBeltInReverse) runScreenReversePurple else runScreenForwardBlue,
                    fontWeight = FontWeight.SemiBold,
                    shadow = Shadow(
                        color = Color(color = if (isBeltInReverse) 0x809747ff else 0x800055ff),
                        offset = offset,
                        blurRadius = 4f
                    ),
                )
            )
        }
    }
}

@AllDevicePreviews
@Composable
fun CountdownScreenPreview(){
    CountdownScreen()
}