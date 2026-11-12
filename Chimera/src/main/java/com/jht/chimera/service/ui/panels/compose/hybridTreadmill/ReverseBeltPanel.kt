package com.jht.chimera.service.ui.panels.compose.hybridTreadmill

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jht.chimera.lib.common.config.ChimeraEnums
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.service.ui.panels.PanelIds
import com.jht.chimera.service.ui.panels.WorkoutPanelManager.Companion.instance
import com.jht.chimera.service.ui.panels.compose.ComposePanel
import com.jht.datamatrixlib.DataMatrix
import com.jht.switchStride.R
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

class ReverseBeltPanel(context: Context) : ComposePanel(context) {

    val mDataMatrix = DataMatrix()

    init {
        mDataMatrix.createBoolAdapter(HALKeys.ACTION_TM_REVERSE_BELT_BOOL) {
            instance?.showPanel(PanelIds.REVERSE_BELT_PANEL, true)
        }
    }

    @Composable
    override fun ThemedContent() {
        BeltStoppingDialogContent()
    }

    override fun getPanelParams(): WindowManager.LayoutParams? {
        val viewParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
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

@Composable
fun BeltStoppingDialogContent() {
    Box (modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF121212).copy(alpha = 0.8f))) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(600 / 1920f)
                .fillMaxHeight(236 / 1080f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp.computeSize()))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(vertical = 30.dp.computeSize(), horizontal = 20.dp.computeSize())

        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.stopping_belt_tc),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(16.dp.computeSize()))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.stay_facing_forward_pg),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }



}

@AllDevicePreviews
@Composable
private fun ReverseBeltPanelPreview() {
    BeltStoppingDialogContent()
}