package com.jht.chimera.service.ui.panels.compose

import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.chimera.R
import com.jht.chimera.lib.common.RealScreen
import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.config.ProductConfig
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.workout.WorkoutIds
import com.jht.chimera.lib.workoutapps.AppManager
import com.jht.chimera.service.ui.panels.ExtraBarOptions
import com.jht.chimera.service.ui.panels.PANEL_OVERRIDES_FORCE_HIDE_PAUSE_PANEL_KEY
import com.jht.chimera.service.ui.panels.WorkoutPanelManager
import com.jht.data.FrameControlLocation
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.RelativeLocation
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.theme.disabledBackground
import com.jht.theme.themeButtonBorder
import com.jht.theme.themeButtonColor
import com.jht.ui.AllDevicePreviews
import com.jht.ui.DebounceAction
import com.jht.ui.DebounceActionState
import com.jht.ui.Device15inPreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd
import com.jht.unit.UnitFormatter
import com.jht.widgets.NumPadEntry
import com.jht.widgets.PagerJumpingDotIndicator
import com.jht.widgets.PopupFullscreen
import com.jht.widgets.RFIDIndicator
import com.jht.widgets.SettingsButton
import com.jht.widgets.helpers.getInjectionEntryPoint
import com.jht.widgets.rotatePages
import kotlin.math.abs

private const val TAG = "WorkoutComposeSidePanels"

//region LEFT PANEL
/** Left panel during an active workout */
class WorkoutComposeLeftPanel(context: Context) : BaseComposeSidePanel(context, isLeft = true) {
    private fun onHome() {
        // Can this have different functions based on whether in a third party interface?
        // Should that logic belong to the WorkoutPanelManager?
        WorkoutPanelManager.onHomeClick(this)
    }
    private fun onToggleFullscreen() {
        // use the model? use a workout panel manager static? wait til we modularize the panels?
        DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, if (isFullscreen.value) UiServiceKeys.WorkoutPanelState.REGULAR else UiServiceKeys.WorkoutPanelState.FULLSCREEN)
    }
    private fun onCoolDown() {
        // original code blocks the button click if workout was not running, not sure if that is necessary, skip seems to work fine while paused?
        DataMatrix.sendEvent(WorkoutKeys.ACTION_SKIP_TO_COOLDOWN)
    }

    @Composable
    override fun ThemedContent() {
        Box {
            LeftPanel(
                controlGroupModel.value,
                controlStyle = controlStyle.value,
                showPause = showPause.value,
                showHome = showHome.value,
                onHome = ::onHome,
                isFull = isFullscreen.value,
                onToggleFull = ::onToggleFullscreen,
                onCoolDown = ::onCoolDown,
                showCoolDown = showCoolDown.value,
                panelOffset = panelOffsetPixels.value,
                options = options.value,
                forceHidePause = forceHidePause.value,
                singleAppMode = singleAppMode.value,
            )
            RFIDIndicator(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 44.dp.computeSizeHd()),
                isVisible = (rfidLocation.value == RelativeLocation.LEFT_MIDDLE),
            )
        }
    }

    override fun getPanelParams(): WindowManager.LayoutParams {
        val width = (RealScreen.widthPixels - resources.getDimension(R.dimen.app_container_width).toInt()) / 2
        val height = RealScreen.heightPixels
        val viewParams = WindowManager.LayoutParams(
            width, height,
            mPanelMode.windowType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT
        )

        viewParams.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        viewParams.x = 0
        viewParams.y = 0
        return viewParams
    }
}

@AllDevicePreviews
@Composable
private fun PreviewLeftPanel() {
    val width = dimensionResource(R.dimen.side_bar_button_width)
    Box(Modifier.size(width, 768.dp))
    { LeftPanel() }
}

// for now assume if there is no button padding that controls are centered (probably only the case with basic theme)
private val noButtonPadding @Composable get() = (DynamicTheme.padding.panelButton.value == 0f)
private val aboveControlWeight @Composable get() = if (noButtonPadding) 1f else 3f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LeftPanel(
    controlGroupModel: ControlGroupModel? = ControlGroupModel(),
    controlStyle: List<BaseComposeSidePanel.ControlStyle> = listOf(BaseComposeSidePanel.ControlStyle.Normal),
    showPause: Boolean = false,
    showHome: Boolean = true,
    onHome: () -> Unit = {},
    isFull: Boolean = false,
    onToggleFull: () -> Unit = {},
    onCoolDown: () -> Unit = {},
    showCoolDown: Boolean = false,
    panelOffset: Offset = Offset.Zero,
    options: ExtraBarOptions = ExtraBarOptions.defaultOptions,
    forceHidePause: Boolean = false,
    singleAppMode: Boolean = false,
) {
    val showCoolDownConfirm = remember(showPause) { mutableStateOf(false) } // pause seems to reset cooldown confirm
    val showControls = !showPause && !showCoolDownConfirm.value
    val hasMultipleControlStyles = (controlStyle.size > 1) && controlGroupModel != null // option is on and there is control
    val pagerState = rememberPagerState { controlStyle.size }
    Row {
        Column(
            Modifier
                .semantics { testTagsAsResourceId = true }
                .clickableNoEffect { pagerState.rotatePages() }
                .background(DynamicTheme.colors.panelSideBackground.brush())
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                singleAppMode -> Modifier
                showHome -> HomeAppsButton(Modifier.panelTopButton(), onHome)
                else -> FullscreenButton(isFull, onToggleFull, Modifier.panelTopButton())
            }
            HorizontalPager(
                modifier = Modifier
                    .testTag("LeftPanelPager")
                    .weight(1f),
                state = pagerState,
                userScrollEnabled = false, // swipe detection seems to delay regular press (but could turn on swipe only when pages > 1?)
            ) { page ->
                when (controlStyle[page]) {
                    BaseComposeSidePanel.ControlStyle.QuickKeys -> Column {
                       LockScreenButton(
                            Modifier.panelIconButton(),
                            rootOffset = panelOffset,
                            border = if (noButtonPadding) null else themeButtonBorder,
                        )
                        ControlPresets(controlGroupModel, showControls)
                    }


                    else -> LeftControlPanel(
                        controlGroupModel,
                        showControls,
                        panelOffset,
                        controlStyle = controlStyle,
                        singleAppMode = singleAppMode,
                    )
                }
            }
            if (hasMultipleControlStyles && !showPause) {
                DotIndicator(pagerState, Modifier.fillMaxWidth())
            }
                CoolDownPanel(
                    modifier = Modifier.padding(DynamicTheme.padding.panelButton),
                    enabled = !showPause, // old UI did not allow cooldown when paused
                    showCoolDown = showCoolDown,
                    showConfirm = showCoolDownConfirm.value,
                    onConfirm = { showCoolDownConfirm.value = it },
                    onCoolDown = onCoolDown,
                )
        }
        VerticalDivider(thickness = 1.dp, color = panelBorderColor)
    }
}

@Composable
private fun LeftControlPanel(
    controlGroupModel: ControlGroupModel? = ControlGroupModel(location = FrameControlLocation.Left),
    showControls: Boolean = true,
    panelOffset: Offset = Offset.Zero,
    controlStyle: List<BaseComposeSidePanel.ControlStyle> = listOf(BaseComposeSidePanel.ControlStyle.Normal),
    singleAppMode: Boolean = false,
) {
    Column {
        LockScreenButton(
            modifier = Modifier.panelIconButton(),
            rootOffset = panelOffset,
            border = if (noButtonPadding) null else themeButtonBorder,
        )
        if (singleAppMode){
            Spacer(Modifier.panelIconButton())
        }
        Spacer(Modifier.weight(aboveControlWeight))

        if (showControls) {ControlGroup(
            Modifier
                .padding(DynamicTheme.padding.panelButton),
            controlGroupModel,
            rootOffset = panelOffset,
            controlStyle = controlStyle,
        )}
        Spacer(
            Modifier.weight(
                if (!controlStyle.contains(BaseComposeSidePanel.ControlStyle.SwitchStrideHybridTdKeys)) 1f else {
                    3f
                }
            )
        )
    }
}


@Composable
private fun FullscreenButton(
    isFull: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ThemeButton(
        modifier = modifier.testTag("ResizeButton"),
        onClick = onClick,
    ) {
        Icon(
            painterResource(if (isFull) R.drawable.ic_shrink_white else R.drawable.ic_expand_white),
            null,
            Modifier.size(40.dp),
            tint = DynamicTheme.colors.text1,
        )
    }
}
//endregion LEFT PANEL



//region RIGHT PANEL
/** Right panel during an active workout */
class WorkoutComposeRightPanel(context: Context) : BaseComposeSidePanel(context, isLeft = false) {
    private fun onResume() {
        if(!waitForMachineToPause.value) {
            // there are some analytics and debounce in the original panel
            DataMatrix.sendEvent(WorkoutKeys.ACTION_RESUME_WORKOUT)
        }
    }
    // TODO - WorkoutRightPanel buttons had analytics and debounce?
    private fun onPause() {
        if(!waitForMachineToPause.value) {
            // any need to check DATA_APP_HANDLES_PAUSE_BOOL, old panel does not?
            DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT)
            // safe guard from old panel to get out of a dead workout?
            if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP) {
                Log.e(TAG, "==> Error: workout not running!")
                LaunchAppUtil.launchSplashScreen()
            }
        }
    }
    private fun onStop() {
        if(!waitForMachineToPause.value) {
            DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT)
        }
    }

    @Composable
    override fun ThemedContent() {
        RightPanel(
            controlGroupModel.value,
            controlStyle = controlStyle.value,
            allowPause = allowPause.value,
            showPause = showPause.value,
            pauseTime = pauseTime.value,
            onResume = ::onResume,
            onPause = ::onPause,
            onStop = ::onStop,
            panelOffset = panelOffsetPixels.value,
            waitForMachineToPause = waitForMachineToPause.value,
            options = options.value,
            forceHidePause = forceHidePause.value,
        )
    }

    override fun getPanelParams(): WindowManager.LayoutParams {
        val width = (RealScreen.widthPixels - resources.getDimension(R.dimen.app_container_width).toInt()) / 2
        val height = RealScreen.heightPixels
        val viewParams = WindowManager.LayoutParams(
            width, height,
            mPanelMode.windowType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT
        )

        viewParams.gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        viewParams.x = 0
        viewParams.y = 0
        return viewParams
    }
}

@Preview(heightDp = 768, widthDp = 122)
@Composable
private fun PreviewRightPanel() {
    RightPanel()
}
@Preview(heightDp = 768, widthDp = 122)
@Composable
private fun PreviewRightPanelPaused() {
    RightPanel(showPause = true)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RightPanel(
    controlGroupModel: ControlGroupModel? = ControlGroupModel(location = FrameControlLocation.Right),
    controlStyle: List<BaseComposeSidePanel.ControlStyle> = listOf(BaseComposeSidePanel.ControlStyle.Normal),
    allowPause: Boolean = true,
    showPause: Boolean = false,
    pauseTime: String = "5:00",
    onResume: () -> Unit = {},
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
    waitForMachineToPause: Boolean = false,
    panelOffset: Offset = Offset.Zero,
    options: ExtraBarOptions = ExtraBarOptions.defaultOptions,
    forceHidePause: Boolean = false,
) {
    val showControls = !showPause
    val hasMultipleControlStyles = (controlStyle.size > 1) && controlGroupModel != null // option is on and there is control
    val pagerState = rememberPagerState { controlStyle.size }
    Row {
        VerticalDivider(thickness = 1.dp, color = panelBorderColor)
        Column(
            Modifier
                .semantics { testTagsAsResourceId = true }
                .clickableNoEffect { pagerState.rotatePages() }
                .background(DynamicTheme.colors.panelSideBackground.brushFlipHorizontal())
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SettingsButton(Modifier.panelTopButton())

            HorizontalPager(
                modifier = Modifier
                    .testTag("RightPanelPager")
                    .weight(1f),
                state = pagerState,
                userScrollEnabled = false, // swipe detection seems to delay regular press (but could turn on swipe only when pages > 1?)
            ) { page ->
                when (controlStyle[page]) {
                    BaseComposeSidePanel.ControlStyle.QuickKeys -> Column {
                        FanButton(
                            Modifier.panelIconButton(),
                            border = if (noButtonPadding) null else themeButtonBorder,
                        ) { Spacer(Modifier.panelIconButton()) }
                        ControlPresets(controlGroupModel, showControls)
                    }
                    else -> RightControlPanel(
                        controlGroupModel,
                        showControls,
                        panelOffset,
                        controlStyle = controlStyle,
                    )
                }
            }
            if (hasMultipleControlStyles && !showPause) {
                DotIndicator(pagerState, Modifier.fillMaxWidth())
            }

            StopPanel(
                onResume, onPause, onStop,
                Modifier
                    .padding(DynamicTheme.padding.panelButton),
                !forceHidePause,
                allowPause, showPause,
                pauseTime,
                waitForMachineToPause,
            )
        }
    }
}

@Composable
private fun RightControlPanel(
    controlGroupModel: ControlGroupModel?,
    showControls: Boolean = true,
    panelOffset: Offset = Offset.Zero,
    controlStyle: List<BaseComposeSidePanel.ControlStyle> = listOf(BaseComposeSidePanel.ControlStyle.Normal),
) {
    Column {
        FanButton(
            Modifier.panelIconButton(),
            border = if (noButtonPadding) null else themeButtonBorder,
        ) { Spacer(Modifier.panelIconButton()) }

        Spacer(Modifier.weight(aboveControlWeight))
        if (showControls) {ControlGroup(
            Modifier
                .padding(DynamicTheme.padding.panelButton),
            controlGroupModel,
            rootOffset = panelOffset,
            controlStyle = controlStyle,
        )}
        Spacer(
            Modifier.weight(
                if (!controlStyle.contains(BaseComposeSidePanel.ControlStyle.SwitchStrideHybridTdKeys)) 1f else {
                    3f
                }
            )
        )
    }
}

@Composable
private fun StopPanel(
    onResume: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    showPanel: Boolean = true,
    allowPause: Boolean = true,
    showPause: Boolean = true,
    pauseTime: String = "5:00",
    waitForMachineToPause: Boolean = false,
) {
    Column(
        Modifier
            //.background(if (showPause) DynamicTheme.colors.gray1 else Color.Transparent) // creative is presently asking for no pause pane color
            .fillMaxWidth()
            .width(IntrinsicSize.Min)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val buttonModifier = Modifier.panelBottomButton()
        if (showPanel) {
            if (showPause) {
                StopButton(onClick = onStop, buttonModifier, true, !waitForMachineToPause)
                Text(pauseTime, Modifier.align(Alignment.CenterHorizontally), color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle2)
                if (waitForMachineToPause) Text(stringResource(R.string.pausing_tc), Modifier.align(Alignment.CenterHorizontally), color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle2)
                ResumeButton(onClick = onResume, buttonModifier, !waitForMachineToPause)
            } else {
                StopButton(onClick = if (!allowPause) onStop else onPause, buttonModifier, !allowPause, !waitForMachineToPause)
            }
        } else {
            Spacer(buttonModifier)
        }
    }
}

private fun pausePanelColor(baseColor: Color, enabled: Boolean) = if (enabled) baseColor else baseColor.copy(0.2f)
@Composable
private fun pausePanelButtonColors(baseColor: Color) =  ButtonDefaults.buttonColors(containerColor = baseColor, disabledContainerColor = pausePanelColor(baseColor, false))

@Composable
private fun StopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    stopIcon: Boolean = true,
    enabled: Boolean = true,
) {
    Button(
        modifier = modifier.testTag(if (stopIcon) "StopButton" else "PauseButton"),
        onClick = onClick,
        colors = pausePanelButtonColors(DynamicTheme.colors.stop),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
    ) {
        Icon(
            painterResource(id = if (stopIcon) R.drawable.ic_stop_white else R.drawable.ic_pause_white),
            null,
            Modifier.size(40.dp),
            tint = pausePanelColor(DynamicTheme.colors.text1, enabled),
        )
    }
}
@Composable
private fun ResumeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        modifier = modifier.testTag("ResumeButton"),
        onClick = onClick,
        colors = pausePanelButtonColors(DynamicTheme.colors.play),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
    ) {
        Icon(
            painterResource(id = R.drawable.ic_play_white),
            null,
            Modifier.size(40.dp),
            tint = pausePanelColor(DynamicTheme.colors.text1, enabled),
        )
    }
}
//endregion RIGHT PANEL


//region COOLDOWN
@Composable
private fun CoolButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        modifier = modifier.testTag("CoolDownButton"),
        onClick = onClick,
        colors = pausePanelButtonColors(DynamicTheme.colors.coolDown),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
    ) {
        Icon(
            painterResource(id = R.drawable.ic_cool_down_white),
            null,
            Modifier.size(40.dp),
            tint = pausePanelColor(DynamicTheme.colors.text1, enabled),
        )
    }
}
@Composable
private fun ConfirmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        modifier = modifier.testTag("ConfirmButton"),
        onClick = onClick,
        colors = pausePanelButtonColors(DynamicTheme.colors.play),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
    ) {
        Icon(
            painterResource(id = R.drawable.ic_icon_skip_arrows),
            null,
            Modifier.size(40.dp),
            tint = pausePanelColor(DynamicTheme.colors.text1, enabled),
        )
    }
}
@Composable
private fun CancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        modifier = modifier.testTag("CancelButton"),
        onClick = onClick,
        colors = pausePanelButtonColors(DynamicTheme.colors.stop),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
    ) {
        Icon(
            painterResource(id = R.drawable.ic_close_circle_white),
            null,
            Modifier.size(40.dp),
            tint = pausePanelColor(DynamicTheme.colors.text1, enabled),
        )
    }
}
@Composable
private fun CoolDownPanel(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showCoolDown: Boolean = true,
    showConfirm: Boolean = false,
    onConfirm: (Boolean) -> Unit = {},
    onCoolDown: () -> Unit = {},
) {
    Column(
        Modifier
            //.background(if (showPause) DynamicTheme.colors.gray1 else Color.Transparent) // creative is presently asking for no pause pane color
            .fillMaxWidth()
            .width(IntrinsicSize.Min)
            .then(modifier),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val buttonModifier = Modifier.panelBottomButton()
        if (showCoolDown) {
            if (showConfirm) {
                ConfirmButton(onClick = { onCoolDown(); onConfirm(false) }, buttonModifier, enabled)
                CancelButton(onClick = { onConfirm(false) }, buttonModifier, enabled)
            } else {
                CoolButton(onClick = { onConfirm(true) }, buttonModifier, enabled)
            }
        } else Spacer(buttonModifier)
    }
}
//endregion COOLDOWN


//region Common bits between side panels

@Composable
private fun ControlGroup(
    modifier: Modifier,
    control: ControlGroupModel? = ControlGroupModel(),
    enabled: Boolean = true,
    rootOffset: Offset = Offset.Zero,
    controlStyle: List<BaseComposeSidePanel.ControlStyle> = listOf(BaseComposeSidePanel.ControlStyle.Normal),
) {
    if (control != null) {
        val positionOfButton = remember { mutableStateOf(DpOffset.Zero) }
        val sizeOfButton = remember { mutableStateOf(DpSize.Zero) }
        val showKeypad = remember { mutableStateOf(false) }
        val buttonPaddingModifier = Modifier.height(if (noButtonPadding) 0.dp else 10.dp)
        val switchStrideBlocks = controlStyle.contains(BaseComposeSidePanel.ControlStyle.SwitchStrideHybridTdKeys)
        Column(
            modifier,
        ) {
            val canUserChange = control.getUserChangesAllowed()
            val backButtonModifier = Modifier
                .aspectRatio(1.5f)
                .fillMaxWidth()
            // Control Back / Last button
            if (canUserChange && control.previousVisible.value && !switchStrideBlocks) {
                ControlBackButton(control, backButtonModifier, enabled = enabled)
            } else {
                Spacer(backButtonModifier) // to keep the sides lined up, reserve the space
            }
            // Pad between Back / Last and control block
            Spacer(buttonPaddingModifier)
            // Control block of current and possibly inc/dec buttons (block has a background and spacing that fakes unified borders)
            val borderWidth = DynamicTheme.borders.medium.width
            val borderWidthConditional = if (noButtonPadding) 0.dp else borderWidth
            val buttonModifier = Modifier
                .padding(horizontal = borderWidth, vertical = 0.dp)
                .fillMaxWidth()
                .aspectRatio(panelIconButtonAspect)
            Column(
                Modifier
                    .background(
                        DynamicTheme.colors.gradient1.verticalReverse(),
                        MaterialTheme.shapes.medium
                    )
                    .clip(MaterialTheme.shapes.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.size(borderWidthConditional))
                with(LocalDensity.current) {
                    ControlCurrentButton(
                        control,
                        Modifier.padding(horizontal = borderWidthConditional, vertical = 0.dp)
                            .testTag(if (showKeypad.value) "" else control.tag("CurrentButton")) // defer to popup if up
                            .onGloballyPositioned { coordinates ->
                                val sizePixels = coordinates.size
                                sizeOfButton.value =
                                    DpSize(sizePixels.width.toDp(), sizePixels.height.toDp())
                                val offsetPixels = coordinates.positionInRoot() + rootOffset
                                positionOfButton.value =
                                    DpOffset(offsetPixels.x.toDp(), offsetPixels.y.toDp())
                            },
                        enabled = enabled && canUserChange && !switchStrideBlocks,
                        partialBorder = canUserChange,
                        onClick = { showKeypad.value = true },
                        controlStyle = controlStyle,
                    )
                }
                if (canUserChange) {
                    Spacer(Modifier.size(borderWidth))
                    ControlStepButton(
                        buttonModifier.testTag(control.tag("Increment")),
                        isIncrement = true, onPress = { control.onIncrementPress(it) }, enabled = enabled)
                    Spacer(Modifier.size(borderWidth))
                    ControlStepButton(
                        buttonModifier.testTag(control.tag("Decrement")),
                        isIncrement = false, onPress = { control.onDecrementPress(it) }, enabled = enabled)
                }
                Spacer(Modifier.size(borderWidth))
            }
            if (!canUserChange) {
                // approximate space that would have been inc/dec buttons
                Spacer(Modifier.size(borderWidth))
                Spacer(buttonModifier)
                Spacer(Modifier.size(borderWidth))
                Spacer(buttonModifier)
            }
        }
        if (showKeypad.value) {
            KeypadPopup(control, positionOfButton.value, sizeOfButton.value, onDismissRequest = { showKeypad.value = false }, controlStyle = controlStyle)
        }
    } // END has control
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun KeypadPopup(
    control: ControlGroupModel,
    buttonPosition: DpOffset,
    buttonSize: DpSize,
    onDismissRequest: () -> Unit,
    controlStyle: List<BaseComposeSidePanel.ControlStyle> = listOf(BaseComposeSidePanel.ControlStyle.Normal),
) {
    PopupFullscreen(onDismissRequest) {
        // whole background screen as a dismiss click
        Box(
            Modifier
                .semantics { testTagsAsResourceId = true }
                .fillMaxSize()
                .background(disabledBackground)
                .clickableNoEffect(onClick = onDismissRequest)
        ) {
            // control button area
            ControlCurrentButton(
                control,
                Modifier
                    .testTag(control.tag("CurrentButton"))
                    .absoluteOffset(buttonPosition.x, buttonPosition.y)
                    .size(buttonSize),
                keypadShown = true,
                onClick = onDismissRequest,
                controlStyle = controlStyle,
            )
            // keypad area
            val keypadWidth = LocalConfiguration.current.screenWidthDp.dp / 2 // I just need a known width to position the box, keypad won't take up all the space
            val keypadOffset = if (control.isLeft) buttonPosition.x + buttonSize.width else buttonPosition.x - keypadWidth
            val keypadAlignment = if (control.isLeft) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                Modifier
                    .absoluteOffset(keypadOffset, 0.dp)
                    .fillMaxHeight()
                    .width(keypadWidth)
            ) {
                NumPadEntry(
                    numPadValue = control.getValueFloat(),
                    modifier = Modifier.align(keypadAlignment),
                    decimalPlaces = if (control.range.value.stepSize < 1) 1 else 0, // side panels are basically 0 or 1 decimal place (the old Resolution class has more thorough calculation which use log10)
                    min = control.range.value.min,
                    max = control.range.value.max,
                    stepValue = control.range.value.stepSize,
                    forceOnlyStepValues = true,
                    usePercentageBar = true,
                    unitLabel = control.keypadUnit.value,
                    onDismiss = onDismissRequest,
                    onValueCommit = { control.onKeypadCommit(it) }
                )
            }
        }
    }
}

/**
 * Inc/Dec Button
 */
@Composable
private fun ControlStepButton(
    modifier: Modifier,
    isIncrement: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onPress: (up : Boolean) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    // this was the method from GoalButton, an alternative would be to use modifier.pointerInteropFilter to get MotionEvent
    var wasPressed by remember { mutableStateOf(false) }
    val isPressed by interactionSource.collectIsPressedAsState()

    if (wasPressed != isPressed) {
        onPress(wasPressed)
        wasPressed = !wasPressed
    }
    Button(
        interactionSource = interactionSource,
        shape = if (isIncrement) RectangleShape else DynamicTheme.corners.medium.shapeRoundBottom,
        colors = ButtonDefaults.buttonColors(containerColor = themeButtonColor),
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(0.6f),
            painter = painterResource(id = if (isIncrement) R.drawable.ic_arrow_up_white else R.drawable.ic_arrow_down_white),
            tint = DynamicTheme.colors.panelControlForeground,
            contentDescription = null
        )
    }
}

/**
 * Button with current value and unit
 */
@Composable
private fun ControlCurrentButton(
    control: ControlGroupModel,
    modifier: Modifier,
    enabled: Boolean = true,
    partialBorder: Boolean = true,
    keypadShown: Boolean = false,
    onClick: () -> Unit,
    controlStyle: List<BaseComposeSidePanel.ControlStyle> = listOf(BaseComposeSidePanel.ControlStyle.Normal),
) {
    // square the corners against the keypad if shown
    val topStart = if (!control.isLeft && keypadShown) 0.dp else DynamicTheme.corners.medium.topStart
    val topEnd = if (control.isLeft && keypadShown) 0.dp else DynamicTheme.corners.medium.topEnd
    val bottomEnd = if (partialBorder) 0.dp else DynamicTheme.corners.medium.bottomEnd
    val bottomStart = if (partialBorder) 0.dp else DynamicTheme.corners.medium.bottomStart
    val shape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = DynamicTheme.colors.panelValueBackground,
            disabledContainerColor = DynamicTheme.colors.panelValueBackground,
        ),
        contentPadding = PaddingValues(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // if there is no padding the value text seems small since box is much larger
                val textScale = if (noButtonPadding) 1.2f else 1f
                Text(
                    text = control.label.value,
                    color = DynamicTheme.colors.panelValueForeground,
                    fontWeight = FontWeight.Bold,
                    fontSize = panelControlTitleSize * textScale
                )
                Text(
                    text = control.currentValue.value,
                    Modifier.testTag(control.tag("CurrentValue")),
                    color = DynamicTheme.colors.panelValueForeground,
                    fontWeight = FontWeight.Bold,
                    fontSize = panelControlValueSize * textScale
                )
                if (control.unit.value.isNotEmpty()) Text(
                    text = control.unit.value,
                    color = DynamicTheme.colors.panelValueForeground,
                    fontWeight = FontWeight.Bold,
                    fontSize = panelControlUnitSize * textScale
                )
            }
            if (enabled) Icon(
                modifier = Modifier
                    .padding(
                        vertical = 5.dp,
                        horizontal = maxOf(
                            5.dp,
                            if (control.isLeft) DynamicTheme.corners.medium.topEnd else DynamicTheme.corners.medium.topStart
                        )
                    )
                    .size(10.dp)
                    .align(if (control.isLeft) Alignment.TopEnd else Alignment.TopStart),
                painter = painterResource(id = if (keypadShown) R.drawable.ic_close else R.drawable.ic_keypad_black),
                tint = DynamicTheme.colors.panelValueForeground,
                contentDescription = null,
            )
        }
    }
}


/**
 * Button with back icon and last value (or a value with a title)
 */
@Composable
private fun ControlBackButton(
    control: ControlGroupModel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ThemeButton(
        enabled = enabled,
        onClick = control::onPreviousValueClicked,
        modifier = modifier
            .testTag(control.tag("LastButton")),
        contentPadding = PaddingValues(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val value = control.previousValue.value
            val labelOption = control.previousLabel.value
            if (labelOption != null) {
                Text(text = labelOption, color = DynamicTheme.colors.text1, fontSize = panelControlTitleSize)
                Text(text = value, color = DynamicTheme.colors.text1, fontSize = 24.sp)
            } else {
                Icon(
                    modifier = Modifier.size(28.dp),
                    painter = painterResource(id = R.drawable.ic_return_white),
                    tint = DynamicTheme.colors.text1,
                    contentDescription = null
                )
                Text(text = value, color = DynamicTheme.colors.text1, fontSize = 24.sp)
            }
        }
    }
}

/** Provide "quick keys" selection of preset control values */
@Composable
private fun ControlPresets(
    control: ControlGroupModel?,
    visible: Boolean = true,
) {
    if (control != null && visible) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

                // keys
                Column(
                    Modifier.weight(1f),
                ) {
                    Spacer(Modifier.weight(0.1f))
                    val decimalPlaces = 0 //if (control.range.value.stepSize < 1) 1 else 0
                    val debounceActionState = remember { DebounceActionState(250) }
                    control.presetValues.value.reversed().forEach {
                        // if value is within the rounding precision, consider it selected
                        val selected = remember(
                            control.currentValue.value,
                            it
                        ) { abs(control.getValueFloat() - it) < 0.1f }
                        val debounceAction = remember(it) {
                            DebounceAction(debounceActionState) {
                                control.onKeypadCommit(it)
                            }
                        }
                        QuickKey(
                            it,
                            selected,
                            decimalPlaces,
                            Modifier
                                .weight(1f)
                                .padding(DynamicTheme.padding.panelButton / 2)
                                .fillMaxWidth(),
                        ) {
                            debounceAction.invoke()
                        }
                    }
                    Spacer(Modifier.weight(0.1f))
                }
                // label
                Column(
                    Modifier
                        .padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val iconModifier = Modifier
                        .padding(5.dp)
                        .height(32.dp.computeSize())
                    if (control.iconId.intValue != 0) {
                        Icon(
                            painterResource(control.iconId.intValue),
                            null,
                            iconModifier,
                            tint = DynamicTheme.colors.text1,
                        )
                    } else Spacer(iconModifier)
                    Text(
                        control.keypadUnit.value,
                        color = DynamicTheme.colors.text1,
                        style = DynamicTheme.typography.textStyle1
                    )
                }
        }
    }
    else {
            Spacer(Modifier.fillMaxSize())

    }
    }

@Composable
private fun QuickKey(
    value: Float,
    isSelected: Boolean,
    decimalPlaces: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    ThemeButton(
        onClick,
        modifier,
        border = if (isSelected) themeButtonBorder.copy(brush = SolidColor(DynamicTheme.colors.primary)) else themeButtonBorder,
    ) {
        Text(
            UnitFormatter.formatDecimal(decimalPlaces, value),
            color = if (isSelected) DynamicTheme.colors.primary else DynamicTheme.colors.text1,
            style = DynamicTheme.typography.textStyle2,
            fontSize = 20.sp.computeSize(),
        )
    }
}
@Device15inPreviews
@Composable
private fun PreviewControlPresets() {
    Box(
        Modifier.height(768.dp).width(122.dp)
    ) {
        ControlPresets(
            ControlGroupModel(),
        )
    }
}

/** Provides a clickable dot indicator for pager */
@Composable
private fun DotIndicator(
    pagerState: PagerState,
    modifier: Modifier,
) {
    // do we make this indicator clickable increment or swipe able or just overlay it over the pager so pager gets swipe event?
    // when scroll stops, make sure we snapped to a single page and not between pages
    LaunchedEffect(pagerState.isScrollInProgress) {
        // do we need any debouncing? https://stackoverflow.com/a/74948279
        if (!pagerState.isScrollInProgress) pagerState.requestScrollToPage(pagerState.currentPage)
    }
    Box(
        modifier
            .clickableNoEffect { pagerState.rotatePages() }
    ) {
        PagerJumpingDotIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(10.dp),
            state = pagerState,
            activeColor = DynamicTheme.colors.primary,
            dotSize = 10.dp,
        )
    }
}

//endregion Common bits between side panels



// Adapted code from the top+bottom auto hide POC and tried to retro fit for use in single panels
// Should we look at way for two panels to be controlled from one class? Panel manager currently restricts to one with some data passing?
abstract class BaseComposeSidePanel(context: Context, protected val isLeft: Boolean) : ComposePanel(context) {
    enum class ControlStyle { // move to panel options?
        Normal,
        QuickKeys,
        SwitchStrideHybridTdKeys
    }
    protected val workoutModel: WorkoutModel
    protected val rfidLocation = mutableStateOf(RelativeLocation.TOP_RIGHT)
    protected val controlStyle = mutableStateOf(listOf(ControlStyle.Normal))
    protected val mDataMatrix = DataMatrix()
    protected val controlGroupModel: MutableState<ControlGroupModel?> = mutableStateOf(null)
    protected val options = mutableStateOf(ExtraBarOptions.defaultOptions)

    protected val isFullscreen = mutableStateOf(false)
    override fun setFullscreen(isFull: Boolean) { isFullscreen.value = isFull }

    protected val showHome = mutableStateOf(true)
    protected val showCoolDown = mutableStateOf(false)
    protected val allowPause = mutableStateOf(true)
    protected val showPause = mutableStateOf(false)
    private fun onPauseShown(pause: Boolean) {
        showPause.value = pause
        waitForMachineToPause.value = !isSafeToStart.value && pause
    }
    protected val pauseTime = mutableStateOf("")
    protected val isSafeToStart = mutableStateOf(DataMatrix.getBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL))
    private fun onSafeToStart(safe2Start: Boolean) {
        isSafeToStart.value = safe2Start
        waitForMachineToPause.value = !safe2Start && showPause.value
    }
    protected val waitForMachineToPause = mutableStateOf(!isSafeToStart.value && showPause.value)

    /** if true, hide home button or other means to leave the currently shown app */
    val singleAppMode = mutableStateOf(false)
    /** hide the pause/stop control panel */
    val forceHidePause = mutableStateOf(false)
    override fun setPanelOverrides(panelOverrides: Bundle) {
        if (panelOverrides.getBoolean(PANEL_OVERRIDES_FORCE_HIDE_PAUSE_PANEL_KEY, false)) forceHidePause.value = true
    }
    override fun clearPanelOverrides(panelOverrides: Bundle) {
        if (panelOverrides.getBoolean(PANEL_OVERRIDES_FORCE_HIDE_PAUSE_PANEL_KEY, false)) forceHidePause.value = false
    }

    private var workoutId: Int = 0
    private var workoutStage: Int = 0
    private var workoutPaused: Boolean = false
    private var coolDownBlocked: Boolean = false
    private var allowSkipWorkout: Boolean = true
    private fun updatePauseAndCoolDown() {
        // allowSkipWorkout is specifically for cooldown, workout scripts have a separate ALLOW_PAUSE parameter
        onPauseShown(workoutPaused && allowPause.value)
        showCoolDown.value = (workoutStage != WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN && allowSkipWorkout && !coolDownBlocked)
    }

    override fun setAppBar(options: ExtraBarOptions?) {
        this.options.value = options ?: ExtraBarOptions.defaultOptions
        updateControlStyle()
    }

    init {
        // eventually need to consider migrating to use the models
        workoutModel = getInjectionEntryPoint(context).workout()
        // Could replace listeners with direct calls from the manager?
        mDataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING, {
            setupControls() // frame specific controls change
        }, false)
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_ID_INT, {
            workoutId = it
            setupControls() // workout specific controls change
        }, false)
        mDataMatrix.createStringAdapter(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING, {
            onAppId(it)
            updatePauseAndCoolDown()
        }, false)
        mDataMatrix.createBoolAdapter(WorkoutKeys.DATA_ALLOW_SKIP_WORKOUT_BOOL, {
            allowSkipWorkout = it
            updatePauseAndCoolDown()
        }, false)
        mDataMatrix.createBoolAdapter(WorkoutKeys.DATA_ALLOW_USER_PAUSE_BOOL, {
            allowPause.value = it
            updatePauseAndCoolDown()
        }, false)
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT, {
            workoutStage = it
            updatePauseAndCoolDown()
            setupControls() // target watts will hide resistance control during normal stage
        }, false)
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, {
            if (it == WorkoutKeys.Constants.RUN_STATE_PAUSE) {
                workoutPaused = true
            } else if (it == WorkoutKeys.Constants.RUN_STATE_STOP || it == WorkoutKeys.Constants.RUN_STATE_RUNNING) {
                workoutPaused = false
            }
            updatePauseAndCoolDown()
        }, false)
        mDataMatrix.createStringAdapter(UiServiceKeys.DATA_PAUSE_TIMER_STRING) {
            pauseTime.value = it ?: ""
        }
        mDataMatrix.createBoolAdapter(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL) {
            onSafeToStart(it)
        }
        // initial control setup (and states in case of crash recovery)
        workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT)
        workoutStage = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT)
        workoutPaused = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_PAUSE
        allowPause.value = DataMatrix.getBoolean(WorkoutKeys.DATA_ALLOW_USER_PAUSE_BOOL)
        allowSkipWorkout = DataMatrix.getBoolean(WorkoutKeys.DATA_ALLOW_SKIP_WORKOUT_BOOL)
        onAppId(DataMatrix.getString(WorkoutKeys.DATA_WORKOUT_APP_UUID_STRING))
        updatePauseAndCoolDown()
        setupControls()
    }
    private fun onAppId(id: String?) {
        val appInfo = AppManager.getInstance().getApp(id)
        val isInterface = appInfo?.useThirdPartyInterface ?: false
        showHome.value = !isInterface
        coolDownBlocked = appInfo?.suppressCoolDownControls ?: false
    }
    override fun dispose() {
        mDataMatrix.finish()
        cleanupControls()
        workoutModel.close()
        super.dispose()
    }

    override fun panelAdded() {
        super.panelAdded()
        // technically panel is always listening because it needs to react in fullscreen (unless we change it so it remains part of panel set but hides when fullscreen)
    }
    override fun panelRemoved() {
        controlGroupModel.value?.stopIncrementing()
        super.panelRemoved()
    }

    private fun setupControls() {
        cleanupControls()
        val consoleType = ProductConfig.getConsoleType()
        rfidLocation.value = consoleType.rfidLocation
        val config = FrameConfigManager.getInstance().frameConfig
        controlGroupModel.value = if (isLeft) {
            createFrameControlLeft(config, workoutId)
        } else { // right
            createFrameControlRight(config, workoutId, workoutStage)
        }
        controlGroupModel.value?.start()
        updateControlStyle()
    }

    protected fun cleanupControls() {
        controlGroupModel.value?.stop()
    }

    private fun updateControlStyle() {
        // figure out the style of the controls
        controlStyle.value =
            when (workoutId) { // could we use some form of workout meta data instead of checking individual IDs?
                WorkoutIds.HIIT -> listOf(ControlStyle.QuickKeys)
                WorkoutIds.HYBRID_TD_SWITCH_STRIDE -> listOf(ControlStyle.SwitchStrideHybridTdKeys)
                else -> {
                    if (options.value.showQuickKeys) ControlStyle.entries.toList() // should we change panel options to have list of styles?
                    else listOf(ControlStyle.Normal)
                }
            }
        // not sure if this is the best spot, but we need to see if we are in single app lock down
        singleAppMode.value = workoutModel.singleAppMode.isNotEmpty()
    }
}


