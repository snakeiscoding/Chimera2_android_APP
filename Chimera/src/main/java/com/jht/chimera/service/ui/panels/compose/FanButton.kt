package com.jht.chimera.service.ui.panels.compose

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.datamatrixlib.DataMatrix
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.theme.darkDynamicTheme
import com.jht.theme.themeButtonBorder
import com.jhtp.dapiservice.an.types.ButtonPressType

private const val TAG = "FanButton.kt"

/*
    Contains FanButton and backing class for FanButton
 */

private class FanButtonModel(
    val isShown: MutableState<Boolean> = mutableStateOf(true),
    val mode: MutableState<Int> = mutableStateOf(SettingKeys.FanMode.Default.ordinal),
    val level: MutableState<Int> = mutableStateOf(0),
) {
    private val mDataMatrix: DataMatrix = DataMatrix()

    fun start() {
        mDataMatrix.createIntAdapter(HALKeys.DATA_IO_SET_FAN_SPEED_INT, { level.value = it }, true)
        mDataMatrix.createIntAdapter(SettingKeys.Hardware.DATA_FAN_MODE_INT, { updateFanButton(it) },true)
    }

    fun stop() {
        mDataMatrix.finish()
    }

    fun onClick() {
        if (isModeNormal()) {
            var fanLevel = DataMatrix.getInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT)
            val maxFanLevel = 4
            if (++fanLevel >= maxFanLevel) fanLevel = 0
            sendFanAnalytics(fanLevel)
            DataMatrix.putInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT, fanLevel)
        } else {
            Log.d(TAG, "fan level is $level")
            val fanOnValue: Int = if (mode.value == SettingKeys.FanMode.Limited1.ordinal) 1 else 2
            val fanOn = isFanOn()
            val nextFanLevel = if (fanOn) 0 else fanOnValue
            sendFanAnalytics(nextFanLevel)
            Log.d(TAG, "sending $nextFanLevel")
            DataMatrix.putInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT, nextFanLevel)
        }
    }

    fun isModeNormal() = mode.value == SettingKeys.FanMode.Default.ordinal
    fun isFanOn() = level.value > 0

    private fun updateFanButton(fanMode: Int) {
        val fanEnabled = fanMode != SettingKeys.FanMode.Disabled.ordinal
        isShown.value = ProductConfig.hasFan() && fanEnabled
        mode.value = fanMode
    }

    private fun sendFanAnalytics(fanLevel: Int) {
        //Stores fan button press events for Dapi Analytics
        var fanLevelIdentifier: String? = null
        when (fanLevel) {
            0 -> fanLevelIdentifier = ButtonPressType.FAN_LEVEL_0_BTN_PRESS.name
            1 -> fanLevelIdentifier = ButtonPressType.FAN_LEVEL_1_BTN_PRESS.name
            2 -> fanLevelIdentifier = ButtonPressType.FAN_LEVEL_2_BTN_PRESS.name
            3 -> fanLevelIdentifier = ButtonPressType.FAN_LEVEL_3_BTN_PRESS.name
        }
        DataMatrix.putString(DAPIKeys.EVENT_DAPI_ANALYTICS_BUTTON_PRESS_STRING, fanLevelIdentifier)
    }
}


//region PREVIEWS
@Preview(heightDp = 86, widthDp = 1024)
@Composable
private fun PreviewFanButton3() {
    val model = remember { FanButtonModel(level = mutableStateOf(3)) }
    Box { FanButton(model = model, Modifier.aspectRatio(1f)) }
}
@Preview(heightDp = 86, widthDp = 1024)
@Composable
private fun PreviewFanButton1() {
    val model = remember { FanButtonModel(level = mutableStateOf(1)) }
    Box { FanButton(model = model, Modifier.aspectRatio(1f)) }
}
@Preview(heightDp = 86, widthDp = 1024)
@Composable
private fun PreviewDarkFanButton1() {
    val model = remember { FanButtonModel(level = mutableStateOf(1)) }
    DynamicTheme(darkDynamicTheme) {
        Box { FanButton(model = model, Modifier.aspectRatio(1f)) }
    }
}
@Preview(heightDp = 86, widthDp = 1024)
@Composable
private fun PreviewFanButtonLimitedOn() {
    val model = remember { FanButtonModel(level = mutableStateOf(1), mode = mutableStateOf(SettingKeys.FanMode.Limited1.ordinal)) }
    Box { FanButton(model = model, Modifier.aspectRatio(1f)) }
}
@Preview(heightDp = 86, widthDp = 1024)
@Composable
private fun PreviewFanButtonLimitedOff() {
    val model = remember { FanButtonModel(level = mutableStateOf(0), mode = mutableStateOf(SettingKeys.FanMode.Limited1.ordinal)) }
    Box { FanButton(model = model, Modifier.aspectRatio(1f)) }
}
//endregion PREVIEWS

@Composable
fun FanButton(
    modifier: Modifier = Modifier,
    border: BorderStroke? = themeButtonBorder,
    noFanComposable: @Composable () -> Unit = {},
) {
    val model = remember { FanButtonModel() }
    LaunchedEffect(Unit) { model.start() }
    DisposableEffect(Unit) { onDispose { model.stop() } }
    FanButton(model, modifier, border, noFanComposable)
}

@Composable
private fun FanButton(
    model: FanButtonModel,
    modifier: Modifier = Modifier,
    border: BorderStroke? = themeButtonBorder,
    noFanComposable: @Composable () -> Unit = {},
) {
    if (model.isShown.value) {
        // for on/off fans just show full bars or no bars
        val levels = if (model.isModeNormal()) {
            model.level.value
        } else {
            if (model.isFanOn()) 3 else 0
        }
        ThemeButton(
            modifier = modifier,
            onClick = { model.onClick() },
            contentPadding = PaddingValues(DynamicTheme.borders.medium.width),
            border = border,
        ) {
            // need fan level image
            Column(
                Modifier
                    .clip(DynamicTheme.corners.medium.shapeRoundStart)
                    .weight(1f)
                    .padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FanLevel(modifier = Modifier.weight(1f), show = (levels > 2))
                FanLevel(modifier = Modifier.weight(1f), show = (levels > 1))
                FanLevel(modifier = Modifier.weight(1f), show = (levels > 0))
            }
            // need fan image
            Icon(
                modifier = Modifier.size(40.dp),
                painter = painterResource(id = R.drawable.ic_icon_fan),
                tint = if (model.isFanOn()) DynamicTheme.colors.primary else DynamicTheme.colors.text1,
                contentDescription = null
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    } else {
        noFanComposable()
    }
}

@Composable
private fun FanLevel(
    modifier: Modifier = Modifier,
    show: Boolean
) {
    Box(
        modifier
            //.background(if (show) DynamicTheme.colors.primary else Color.Transparent)
            .background(
                if (show) DynamicTheme.colors.primary else DynamicTheme.colors.primary.copy(
                    alpha = 0.2f
                )
            )
            .width(5.dp)
    )
}
