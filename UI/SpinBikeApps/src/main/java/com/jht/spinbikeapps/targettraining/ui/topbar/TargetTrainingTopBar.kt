package com.jht.spinbikeapps.targettraining.ui.topbar

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.enums.UiPanelType
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelBase
import com.jht.interfaces.ui.InnerPanelHolder
import com.jht.interfaces.ui.UiPanelOperations
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TOP_BAR_BG_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TOP_BAR_BG_2
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TOP_BAR_BG_3
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TOP_BAR_BG_4
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.YELLOW_1
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import com.jht.ui.computeSize
import com.jht.widgets.helpers.getInjectionEntryPoint
import kotlinx.parcelize.Parcelize
import java.util.UUID
import javax.inject.Inject

// Taken directly from app id used by the system

internal val TARGET_TRAINING_UUID: UUID = UUID.fromString("4580241a-d1f9-4729-8efe-46af52a3b0ba")

@Parcelize
class TargetTrainingTopBarHolder : InnerPanelHolder() {

    override fun createPanel(
        context: Context, panelOperations: UiPanelOperations
    ): InnerPanelBase {

        val injection = getInjectionEntryPoint(context)
        val manager = TargetTrainingTopBarManager(injection.navigation())
        return TargetTrainingTopBar(panelOperations, manager)
    }
}


class TargetTrainingTopBarManager @Inject constructor(
    private val navigationModel: NavigationModel
) : AutoCloseable {
    private var _showUi = false
    private var showCallback: () -> Unit = {}
    private var hideCallback: () -> Unit = {}

    fun setCallbacks(onShow: () -> Unit, onHide: () -> Unit) {
        showCallback = onShow
        hideCallback = onHide
    }

    fun updateVisibility(
        showUi: Boolean = _showUi,
        onShow: () -> Unit = showCallback,
        onHide: () -> Unit = hideCallback,
        selectedMode: WorkoutMode
    ) {
        _showUi = showUi
        val isTargetSet = selectedMode != WorkoutMode.None
        if (_showUi && isTargetSet) onShow() else onHide()
    }

    override fun close() {
        navigationModel.close()
    }

    fun navigate(destination: String) {
        navigationModel.launchApp(destination)
    }
}

internal class TargetTrainingTopBar(
    panelOperations: UiPanelOperations, private val manager: TargetTrainingTopBarManager
) : InnerPanelBase(
    panelId = TARGET_TRAINING_UUID,
    panelType = UiPanelType.TopPanel,
    panelOperations = panelOperations
) {
    init {
        manager.setCallbacks(onShow = { show() }, onHide = { hide() })
    }

    @Composable
    override fun PanelContent() {
        TopPanel(onNavigate = { manager.navigate(TARGET_TRAINING_UUID.toString()) })
    }

    override fun onPanelReceiveData(panelData: BasePanelData) {
        val panelData = panelData as? TargetTrainingPanelData
        panelData?.let { data ->
            manager.updateVisibility(
                showUi = data.isTargetTrainingTopBarVisible,
                selectedMode = data.selectedMode
            )
        }
    }

    override fun onPanelDestroyed() {
        manager.close()
    }
}

@Preview(heightDp = 66, widthDp = 1024)
@Preview(heightDp = 80, widthDp = 1366)
@Preview(heightDp = 80, widthDp = 1920)
@Composable
fun TopPanel(onNavigate: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        TOP_BAR_BG_1,
                        TOP_BAR_BG_2,
                        TOP_BAR_BG_3,
                        TOP_BAR_BG_4
                    )
                )
            )
            .clickable { onNavigate() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painterResource(R.drawable.ic_crosshair_bold),
            contentDescription = null,
        )

        Spacer(Modifier.size(8.dp.computeSize(1920f)))

        Text(
            text = stringResource(R.string.target_training_tc),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 32.sp.computeSize(1920f),
            color = YELLOW_1
        )

        Spacer(Modifier.size(8.dp.computeSize(1920f)))

        Image(
            painter = painterResource(R.drawable.ic_outline_arrow_outward),
            contentDescription = null,
        )
    }
}