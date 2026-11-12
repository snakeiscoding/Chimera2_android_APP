package com.jht.spinbikeapps.targettraining.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GREY_TEXT
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

@Composable
internal fun ConstantWattsComp(
    modifier: Modifier = Modifier,
    watts: String = "192",
    averageWatts: String = "27",
    userFtp:Int = 0
) {
    Column(
        modifier = modifier
            .wrapContentSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_lightning),
            contentDescription = null,
            modifier = Modifier.size(60.dp.computeSize(1920f))
        )
        Spacer(modifier = Modifier.height(8.dp.computeSizeHd()))
        Text(
            watts,
            fontSize = 120.sp.computeSize(1920f),
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp.computeSizeHd()))
        Text(
            stringResource(R.string.watts_lc),
            fontSize = 20.sp.computeSize(1920f),
            color = GREY_TEXT,
            fontFamily = FontFamily.SansSerif
        )
        HorizontalDivider(
            modifier = modifier.width(416.5.dp.computeSize(1920f)).padding(vertical = 12.dp.computeSizeHd()),
            thickness = 1.dp.computeSize(1920f),
            color = DynamicTheme.colors.gray6
        )
        Text(
            averageWatts,
            fontSize = 50.sp.computeSize(1920f),
            color = Color.White,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(8.dp.computeSizeHd()))
        Text(
            stringResource(R.string.average_lc),
            fontSize = 20.sp.computeSize(1920f),
            color = GREY_TEXT,
            fontFamily = FontFamily.SansSerif,
        )
        Spacer(modifier = Modifier.height(32.dp.computeSizeHd()))
        Text(
            modifier = Modifier,
            text = "$userFtp %",
            fontSize = 50.sp.computeSize(1920f),
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.ftp_tc),
            fontSize = 20.sp.computeSize(1920f),
            color = GREY_TEXT,
            fontFamily = FontFamily.SansSerif
        )

    }
}

@AllDevicePreviews
@Composable
private fun ConstantWattsCompPreview(){
    ConstantWattsComp()
}
