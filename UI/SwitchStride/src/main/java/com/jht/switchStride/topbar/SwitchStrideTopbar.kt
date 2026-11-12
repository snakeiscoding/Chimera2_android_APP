package com.jht.switchStride.topbar

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jht.enums.UiPanelType
import com.jht.enums.WorkoutId
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelBase
import com.jht.interfaces.ui.InnerPanelHolder
import com.jht.interfaces.ui.UiPanelOperations
import com.jht.switchStride.R
import com.jht.switchStride.utils.topBarGradientColor1
import com.jht.switchStride.utils.topBarGradientColor2
import com.jht.switchStride.utils.topBarGradientColor3
import com.jht.switchStride.utils.topBarGradientColor4
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.widgets.helpers.getInjectionEntryPoint
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * The top panel for Switch Stride workouts.
 *  * Functions:
 *  * - PanelContent: Renders the top banner UI by invoking TopPanel().
 *  * - TopPanel: Builds the main layout with switch stride center logo.
 *  * - setDataMatrix: Initializes data listeners and updates UI state.
 *  * - updateVisibility: Controls visibility of the top panel based on state.
 */
// Taken directly from app id used by the system
internal val SWITCH_STRIDE_UUID: UUID =
    UUID.fromString("f8d5c7be-a1e0-414d-b446-a1d13febb502")

@Parcelize
class SwitchStrideTopbarHolder : InnerPanelHolder() {
    override fun createPanel(
        context: Context, panelOperations: UiPanelOperations
    ): InnerPanelBase {
        return SwitchStrideTopbar(context, panelOperations)
    }
}

internal class SwitchStrideTopbar(
    context: Context, panelOperations: UiPanelOperations
) : InnerPanelBase(
    panelId = SWITCH_STRIDE_UUID,
    panelType = UiPanelType.TopPanel,
    panelOperations = panelOperations
) {
    private var lastPanelData = mutableStateOf<SwitchStridePanelData?>(null)
    private val mWorkoutModel: WorkoutModel
    private val uiServiceModel: UIServiceModel
    private val navigationModel: NavigationModel
    private var mIsFullscreen = false
    private var mIsSwitchStride = false
    private var mIsWorkoutStopped = false

    private val mIsBeltInReverse = mutableStateOf(false)
    private val componentTag = "SwitchStrideTopBar"

    init {
        val injection = getInjectionEntryPoint(context)
        mWorkoutModel = injection.workout()
        uiServiceModel = injection.ui()
        navigationModel = injection.navigation()
        setDataMatrix()
    }

    private fun setDataMatrix() {

        mWorkoutModel.let { it ->
            it.isWorkoutStopped.listenAndTrigger { stop ->
                mIsWorkoutStopped = stop
                updateVisibility()
            }
            it.workoutId.listenAndTrigger {
                val matchingId = (it == WorkoutId.HYBRID_TD_SWITCH_STRIDE)
                if (matchingId != mIsSwitchStride) {
                    mIsSwitchStride = matchingId
                    updateVisibility()
                }
            }
            it.isBeltInReverse.listenAndTrigger {
                mIsBeltInReverse.value = it
            }
            uiServiceModel.stateFullScreen.listenAndTrigger {
                mIsFullscreen = it
                updateVisibility()
            }
        }
    }

    private fun updateVisibility(appInForeground: Boolean = lastPanelData.value?.isSwitchStrideTopBarVisible == true) {
        // Top bar will be visible if workout is active and SwitchStride is not in foreground
        val show = mIsSwitchStride && !mIsWorkoutStopped && !appInForeground && !mIsFullscreen
        if (show) {
            show()
        } else {
            hide()
        }
    }

    override fun onPanelReceiveData(panelData: BasePanelData) {
        lastPanelData.value = panelData as? SwitchStridePanelData
        lastPanelData.value?.let { data ->
            updateVisibility(data.isSwitchStrideTopBarVisible)
        }
    }

    @Composable
    override fun PanelContent() {
        TopBar(
            mIsBeltInReverse = mIsBeltInReverse.value,
            componentTag,
            onClick = { navigationModel.launchApp(SWITCH_STRIDE_UUID.toString()) })
    }

    override fun onPanelDestroyed() {
        mWorkoutModel.close()
        uiServiceModel.close()
        navigationModel.close()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopBar(mIsBeltInReverse: Boolean, componentTag: String, onClick: () -> Unit = {}) {
    val gradientColors = if (mIsBeltInReverse) {
        listOf(topBarGradientColor1, topBarGradientColor2)
    } else {
        listOf(topBarGradientColor3, topBarGradientColor4)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = Brush.horizontalGradient(colors = gradientColors))
            .clickable { onClick() }
            .semantics { testTagsAsResourceId = true }
            .testTag("$componentTag:LaunchSwitchStride:Box"),
        contentAlignment = Alignment.Center,

        ) {
        if (mIsBeltInReverse) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    modifier = Modifier,
                    painter = painterResource(id = R.drawable.hybrid),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(16.dp.computeSize()))
                Text(
                    text = stringResource(R.string.belt_is_currently_in_reverse_pg),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            Image(
                modifier = Modifier,
                painter = painterResource(id = R.drawable.switchstridelogo),
                contentDescription = null
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun TopBarForwardPreview() {
    TopBar(mIsBeltInReverse = false, componentTag = "SwitchStrideTopBar")
}

@AllDevicePreviews
@Composable
private fun TopBarReversePreview() {
    TopBar(mIsBeltInReverse = true, componentTag = "SwitchStrideTopBar")
}