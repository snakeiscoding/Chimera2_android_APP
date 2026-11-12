package com.jht.chimera.service.ui.panels.compose.hybridTreadmill

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.jht.chimera.R
import com.jht.chimera.lib.common.config.ChimeraEnums
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.service.ui.panels.PanelIds
import com.jht.chimera.service.ui.panels.WorkoutPanelManager.Companion.instance
import com.jht.chimera.service.ui.panels.compose.ComposePanel
import com.jht.datamatrixlib.DataMatrix
import com.jht.ui.AllDevicePreviews
import kotlinx.coroutines.delay

/**
 * Special countdown panel only for the hybrid treadmill reverse belt functionality
 */
class ReverseCountdownPanel(context: Context) : ComposePanel(context) {

    val mDataMatrix = DataMatrix()

    @Composable
    override fun ThemedContent() {
        instance?.showPanel(PanelIds.REVERSE_BELT_PANEL, false)
        ReverseBeltCountdown(DataMatrix.getBoolean(HALKeys.DATA_TM_IS_REVERSE_BELT_BOOL))
    }

    override fun getPanelParams(): WindowManager.LayoutParams? {
        val width = WindowManager.LayoutParams.MATCH_PARENT
        val height = WindowManager.LayoutParams.MATCH_PARENT

        val viewParams = WindowManager.LayoutParams(
            width, height,
            ChimeraEnums.TYPE_SYSTEM_ERROR, (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH), PixelFormat.TRANSPARENT
        )
        viewParams.gravity = Gravity.NO_GRAVITY
        viewParams.x = 0
        viewParams.y = 0
        return viewParams
    }
}

/**
 * Logic to run once the countdown is finished
 */
private fun finishCountdown(){
    instance?.showPanel(PanelIds.REVERSE_COUNTDOWN_PANEL, false)
    DataMatrix.putBoolean(UiServiceKeys.ACTION_START_WORKOUT_UI_READY_BOOL, true)
}

/**
 * Hardware beep sound on countdown
 */
private fun beep(){
    //Further logic needed to see if the boolean check can be moved to the receive of this end
    if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_COUNTDOWN_BEEP_BOOL)){
        DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP)
    }
}

/**
 * Composable for the countdown. There are two styles, one for reverse and one for forward
 */
@Composable
fun ReverseBeltCountdown(isReverse: Boolean) {
    // Coroutine to do the countdown and beeps
    val count = remember { mutableIntStateOf(3) }
    LaunchedEffect(Unit) {
        while (true) {
            beep()
            delay(1000) // Wait for 1 second
            if(count.intValue <= 1){
                break
            }else{
                count.intValue--
            }
        }
        finishCountdown()
    }

    val color = if (isReverse) Color(0xFF9C56FF) else Color(0xFF007BFF)
    val background =  if (isReverse) R.drawable.belt_reverse_background else R.drawable.belt_forward_background

    Box(modifier = Modifier
        .fillMaxSize()
        .paint(
            painterResource(background),
            contentScale = ContentScale.FillBounds
        )){

        Column (modifier = Modifier
            .wrapContentSize()
            .align(Alignment.Center))
        {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.are_you_ready_uc),
                color = color,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 68.sp
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = count.intValue.toString(),
                color = color,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 420.sp
            )
        }
    }
}


@AllDevicePreviews
@Composable
private fun ReverseBeltPanelPreview() {
    ReverseBeltCountdown(isReverse = true)
}

@AllDevicePreviews
@Composable
private fun ReverseBeltPanelForwardPreview() {
    ReverseBeltCountdown(isReverse = false)
}