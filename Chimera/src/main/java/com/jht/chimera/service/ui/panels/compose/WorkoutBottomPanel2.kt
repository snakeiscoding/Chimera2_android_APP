package com.jht.chimera.service.ui.panels.compose

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.chimera.R
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.RealScreen
import com.jht.chimera.lib.common.app.LaunchAppUtil
import com.jht.chimera.lib.common.config.FrameConfigManager
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys
import com.jht.chimera.lib.workoutapps.AppIds
import com.jht.chimera.service.ui.panels.ExtraBarOptions
import com.jht.chimera.service.ui.panels.WorkoutPanelManager
import com.jht.chimera.service.ui.panels.WorkoutPanelManager.AutoHideBehavior
import com.jht.data.FrameControlLocation
import com.jht.datamatrixlib.DataMatrix
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.theme.themeButtonColor
import com.jht.ui.smallerThan16

/**
 * Bottom panel during an active workout (Auto hide 2-panel POC)
 */
class WorkoutBottomPanel2(context: Context) : ComposePanel(context) {
    private val mDataMatrix = DataMatrix()
    private val showPause = mutableStateOf(false)
    private val inCoolDown = mutableStateOf(false)
    private val isFull = mutableStateOf(false)
    private val appBarOptions: MutableState<ExtraBarOptions?> = mutableStateOf(null)
    private val leftControl: MutableState<ControlGroupModel?> = mutableStateOf(null)
    private val rightControl: MutableState<ControlGroupModel?> = mutableStateOf(null)
    private val hasPendingChange: MutableState<Boolean> = mutableStateOf(false)

    private val hideMode = mutableStateOf(AutoHideBehavior.TOUCH)
    fun setAutoHideBehavior(behavior: AutoHideBehavior) { this.hideMode.value = behavior }
    override fun setFullscreen(isFull: Boolean) { this.isFull.value = isFull }
    private fun resetHide() { WorkoutPanelManager.instance?.resetAutoHidePanels() }

    private val pauseTime = mutableStateOf("")

    override fun setAppBar(options: ExtraBarOptions?) {
        appBarOptions.value = options
    }

    // onAttachedToWindow / onDetachedFromWindow could also be used for lifecycle events?

    override fun panelAdded() {
        super.panelAdded()
        // Could replace listeners with direct calls from the manager?
        mDataMatrix.createStringAdapter(SettingKeys.General.DATA_FRAME_TYPE_STRING) {
            setupControls() // frame specific controls change
        }
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_ID_INT) {
            setupControls() // workout specific controls change
        }
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_RUN_STATE_INT, {
            if (it == WorkoutKeys.Constants.RUN_STATE_PAUSE) {
                showPause.value = true
            } else if (it == WorkoutKeys.Constants.RUN_STATE_STOP || it == WorkoutKeys.Constants.RUN_STATE_RUNNING) {
                showPause.value = false
            }
        }, false)
        mDataMatrix.createIntAdapter(WorkoutKeys.DATA_WORKOUT_STAGE_INT) {
            inCoolDown.value = (it == WorkoutKeys.Constants.WORKOUT_STAGE_COOLDOWN)
            setupControls() // target watts will hide resistance control during normal stage
        }
        mDataMatrix.createStringAdapter(UiServiceKeys.DATA_PAUSE_TIMER_STRING) {
            pauseTime.value = it ?: ""
        }
        setupControls()
    }

    override fun panelRemoved() {
        mDataMatrix.finish()
        cleanupControls()
        super.panelRemoved()
    }

    override fun onPendingBehaviorChange(isPending: Boolean) {
        hasPendingChange.value = isPending
    }

    private fun setupControls() {
        cleanupControls()
        val config = FrameConfigManager.getInstance().frameConfig
        val workoutId = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_ID_INT)
        val workoutStage = DataMatrix.getInt(WorkoutKeys.DATA_WORKOUT_STAGE_INT)
        leftControl.value = createFrameControlLeft(config, workoutId)
        rightControl.value = createFrameControlRight(config, workoutId, workoutStage)
        leftControl.value?.onUserInteraction = this::resetHide
        rightControl.value?.onUserInteraction = this::resetHide
        leftControl.value?.start()
        rightControl.value?.start()
    }
    private fun cleanupControls() {
        leftControl.value?.stop()
        rightControl.value?.stop()
    }

    @Composable
    override fun ThemedContent() {
        BottomBar(
            hideMode = hideMode.value,
            onUnhide = {
                if (hideMode.value == AutoHideBehavior.SWIPE || hideMode.value == AutoHideBehavior.ICON) resetHide()
            },
            leftControl = leftControl.value,
            rightControl = rightControl.value,
            appOptions = appBarOptions.value,
            showPause = showPause.value,
            inCoolDown = inCoolDown.value,
            isFull = isFull.value,
            pauseTime = pauseTime.value,
            onResume = { onResume() }, onStop = { onStop() }, onCoolDown = { onCoolDown() }, // pause/resume
            onVolume = { onVolume() },
            onLock = { onLock() },
            onStats = { onStats() },

            /**** App bar functions ****/
            onToggleFull = { if (!hasPendingChange.value) onToggleFull() },
            onBack = { if (!hasPendingChange.value) onBack() },
            onRefresh = { if (!hasPendingChange.value) onRefresh() },
        )
    }

    private fun onResume() {
        // there are some analytics and debounce in the original panel
        DataMatrix.sendEvent(WorkoutKeys.ACTION_RESUME_WORKOUT)
    }

    private fun onStop() {
        // TODO - WorkoutRightPanel has more logic than this
        if (showPause.value) {
            DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT)
        } else {
            // any need to check DATA_APP_HANDLES_PAUSE_BOOL, old panel does not?
            DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT)
        }
    }

    private fun onCoolDown() {
        // original code blocks this if workout was not running
        DataMatrix.sendEvent(WorkoutKeys.ACTION_SKIP_TO_COOLDOWN)
        // since we paused to access cooldown button, we need to resume
        onResume()
    }

    private fun onLock() {
        // TODO - we should probably change the way we do lock so that each panel locks instead of just overlaying yet another panel? BUT need to cover the app area!
        //DataMatrix.sendEvent(UiServiceKeys.EVENT_STOP_SIDE_BAR_INCREMENT)
        //WorkoutPanelManager.instance?.showPanel(PanelIds.LOCK_SCREEN_PANEL, true)
    }

    private fun onVolume() {
        // the volume button feels redundant when we have settings up top ?
        //WorkoutPanelManager.onMenuClick(this)
    }

    private fun onStats() {
        // TODO - show stats in some way?
        LaunchAppUtil.launchApp(context, AppIds.SIMPLE_UUID, this) // use simple app for stats until we get something setup
    }

    private fun onToggleFull() {
        if (DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT) == UiServiceKeys.WorkoutPanelState.FULLSCREEN)
            DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR)
        else DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.FULLSCREEN)
    }
    private fun onBack() {
        // original has some button debouncing code and runs command in a thread?
        Thread { ExecuteAsRoot.simulateBackButton() }.start()
    }
    private fun onRefresh() {
        DataMatrix.sendEvent(UiServiceKeys.EVENT_APP_BAR_REFRESH_BTN_CLICK)
    }

    override fun getPanelParams(): WindowManager.LayoutParams {
        var width = RealScreen.widthPixels
        var height =
            if (isFull.value) resources.getDimension(R.dimen.hidden_bar_height).toInt()
            else resources.getDimension(R.dimen.bottom_bar_stats_height).toInt() // control bar height
        if (isFull.value && hideMode.value == AutoHideBehavior.ICON) {
            width = LayoutParams.WRAP_CONTENT
            height = width
        }
        val viewParams = WindowManager.LayoutParams(
            width, height,
            mPanelMode.windowType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT
        )

        viewParams.gravity = Gravity.BOTTOM or Gravity.LEFT
        viewParams.x = 0
        viewParams.y = 0
        return viewParams
    }
}



@Preview(heightDp = 86, widthDp = 1024)
@Preview(heightDp = 86, widthDp = 1366)
@Preview(heightDp = 86, widthDp = 1920)
@Composable
private fun PreviewBottomBar() {
    val left = ControlGroupModel(location = FrameControlLocation.Left)
    val right = ControlGroupModel(location = FrameControlLocation.Right)
    BottomBar(showPause = false, leftControl = left, rightControl = right)
}

@Preview(heightDp = 86, widthDp = 1024)
@Preview(heightDp = 86, widthDp = 1366)
@Preview(heightDp = 86, widthDp = 1920)
@Composable
private fun PreviewBottomBarPaused() {
    BottomBar(showPause = true)
}

@Preview(heightDp = 86, widthDp = 1024)
@Preview(heightDp = 86, widthDp = 1366)
@Preview(heightDp = 86, widthDp = 1920)
@Composable
private fun PreviewBottomBarApp() {
    val left = ControlGroupModel(location = FrameControlLocation.Left)
    val right = ControlGroupModel(location = FrameControlLocation.Right)
    val appOptions = ExtraBarOptions(showBack = true, showRefresh = true)
    BottomBar(showPause = false, leftControl = left, rightControl = right, appOptions = appOptions)
}


/**
 * This represents the top level panel UI group
 */
@Composable
private fun BottomBar(
    appOptions: ExtraBarOptions? = null,
    isFull: Boolean = false,
    showPause: Boolean = false,
    inCoolDown : Boolean = false,
    leftControl: ControlGroupModel? = ControlGroupModel(),
    rightControl: ControlGroupModel? = ControlGroupModel(),
    pauseTime: String = "5:00",
    onToggleFull: () -> Unit = {},
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    onCoolDown: () -> Unit = {},
    onLock: () -> Unit = {},
    onVolume: () -> Unit = {},
    onStats: () -> Unit = {},
    onUnhide: () -> Unit = {},
    hideMode: AutoHideBehavior = AutoHideBehavior.TOUCH,
) {
    if (isFull) ControlBarHidden(onUnhide, hideMode)
    else ControlBar(appOptions, showPause, inCoolDown, leftControl, rightControl, pauseTime, onResume, onCoolDown, onStop, onRefresh, onBack, onLock, onStats)
}

@Composable
private fun ControlBarHidden(
    onSwipe: () -> Unit = {},
    behavior: AutoHideBehavior = AutoHideBehavior.TOUCH,
) {
    if (behavior == AutoHideBehavior.ICON) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(50.dp)
                .background(color = Color(0x80000000))
                .padding(10.dp)
                .clickable(onClick = { onSwipe() })
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_pf_up_arrow),
                contentDescription = null,
            )
        }
    } else {
        Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val (x, y) = dragAmount
                    when {
                        x > 0 -> {}
                        x < 0 -> {}
                    }
                    when {
                        y > 0 -> {}
                        y < 0 -> {
                            onSwipe()
                        }
                    }
                }
            }
        )
    }
}

/**
 * Bar with the controls on it (as separate of the AppBar)
 */
@Composable
private fun ControlBar(
    appOptions: ExtraBarOptions? = null,
    showPause: Boolean = false,
    inCoolDown : Boolean = false,
    leftControl: ControlGroupModel? = ControlGroupModel(),
    rightControl: ControlGroupModel? = ControlGroupModel(),
    pauseTime: String = "5:00",
    onResume: () -> Unit = {},
    onCoolDown: () -> Unit = {},
    onStop: () -> Unit = {},
    onAppRefresh: () -> Unit = {},
    onAppBack: () -> Unit = {},
    onLock: () -> Unit = {},
    onStats: () -> Unit = {},
    // auto hide POC removed fan and volume
    /*fanControl: FanButtonModel = FanButtonModel(),
    onVolume: () -> Unit = {},*/
) {
    val goColor = colorResource(id = R.color.button_resume)
    val stopColor = colorResource(id = R.color.button_pause)
    val coolColor = colorResource(id = R.color.button_cooldown)
    //val confirmCoolDown by remember { mutableStateOf(false) }
    BoxWithConstraints {
        val smallScreen = smallerThan16(this.maxWidth)
        val spacerWidth = if (smallScreen) 0.dp else 4.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.bottom_bar_stats_height)), // .background(panelBackgroundColor),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // LEFT SECTION
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Row {
                    if (!showPause && leftControl != null) ControlGroup(control = leftControl, showBack = !smallScreen)
                    Spacer(modifier = Modifier.size(spacerWidth))
                    /*if (fanControl.isShown.value) FanButton(model = fanControl)
                    else PanelIconButton(iconId = R.drawable.ic_volume_white, onClick = onVolume)*/
                    PanelIconButton(iconId = R.drawable.ic_lock_screen, onClick = onLock)
                    if (appOptions != null && appOptions.showBack) PanelIconButton(iconId = R.drawable.ic_back_button_white, onClick = onAppBack)
                }
            }

            // CENTER SECTION
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showPause) {
                        Row(
                            modifier = Modifier.background(panelBackgroundColor),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PanelCommandButton(containerColor = goColor, onClick = onResume) { Text(text = stringResource(id = R.string.accumulated_resume_tc), fontSize = barButtonTextSize) }
                            if (!inCoolDown) PanelCommandButton(
                                containerColor = coolColor,
                                onClick = onCoolDown
                            ) { Text(text = stringResource(id = R.string.cooldown_tc), fontSize = barButtonTextSize) }
                            StopButton(containerColor = stopColor, pauseTime = pauseTime, onClick = onStop)
                        }
                    } else {
                        PanelCommandButton(containerColor = stopColor, onClick = onStop) { Text(text = stringResource(id = R.string.stop_tc), fontSize = barButtonTextSize) }
                    }
            }

            // RIGHT SECTION
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Row {
                    if (appOptions != null && appOptions.showRefresh) PanelIconButton(iconId = R.drawable.ic_refresh_white, onClick = onAppRefresh)
                    /*if (confirmCoolDown) {
                        Row(Modifier.background(coolColor)) {
                            PanelIconButton(
                                containerColor = goColor,
                                iconId = R.drawable.ic_icon_skip_arrows,
                                onClick = { onCoolDown(); confirmCoolDown = false })
                            PanelIconButton(
                                containerColor = stopColor,
                                iconId = R.drawable.ic_close_circle_white,
                                onClick = { confirmCoolDown = false })
                        }
                    } else { // Normal view, not looking for cool down confirmation
                        PanelIconButton(
                            //containerColor = colorResource(id = R.color.button_cooldown),
                            enabled = !inCoolDown,
                            iconId = R.drawable.ic_cool_down_white,
                            onClick = { confirmCoolDown = true }
                        )
                        PanelIconButton(iconId = R.drawable.ic_profile_white, onClick = onStats)
                    }*/
                    PanelIconButton(iconId = R.drawable.ic_profile_white, onClick = onStats)
                    Spacer(modifier = Modifier.size(spacerWidth))
                    if (!showPause && rightControl != null) ControlGroup(control = rightControl, showBack = !smallScreen)
                }
            }
        }
    }
}

@Composable
private fun StopButton(
    modifier: Modifier = Modifier,
    containerColor: Color = themeButtonColor,
    onClick: () -> Unit,
    pauseTime: String = "5:00",
) {
    PanelCommandButton(
        modifier = modifier,
        onClick = onClick,
        containerColor = containerColor
    ) {
        Column (horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.end_workout_tc), fontSize = barButtonTextSize)
            if (pauseTime.isNotEmpty()) Text(
                text = pauseTime,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = barButtonTextSize
            )
        }
    }
}

/**
 * Group of UI elements to present and control one operation metric
 */
@Preview(heightDp = 86)
@Composable
private fun PreviewControlGroup() {
    val previousValue = remember {mutableStateOf("10.0")}
    val currentValue = remember {mutableStateOf("12.0")}
    ControlGroup(control = ControlGroupModel(previousValue = previousValue, currentValue = currentValue))
}

@Composable
private fun ControlGroup(
    showBack: Boolean = true,
    control: ControlGroupModel = ControlGroupModel(),
) {
    Row {
        if (showBack && !control.isLeft) {
            Column(horizontalAlignment = Alignment.Start) {
                ControlBack(value = control.previousValue.value, onClick = { control.onPreviousValueClicked() }, labelOption = control.previousLabel.value)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Spacer(modifier = Modifier.size(4.dp))
                ControlStep(isIncrement = false, onPress = { control.onDecrementPress(it) })
                ControlCurrent(label = control.label.value, value = control.currentValue.value, unit = control.unit.value, onClick = { control.onUserInteraction() }, isLeft = control.isLeft)
                ControlStep(isIncrement = true, onPress = { control.onIncrementPress(it) })
                Spacer(modifier = Modifier.size(4.dp))
            }
        }
        if (showBack && control.isLeft) {
            Column(horizontalAlignment = Alignment.End) {
                ControlBack(value = control.previousValue.value, onClick = { control.onPreviousValueClicked() }, labelOption = control.previousLabel.value)
            }
        }
    }
}

/**
 * Inc/Dec Button
 */
@Composable
private fun ControlStep(
    isIncrement: Boolean = true,
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
    ThemeButton(
        interactionSource = interactionSource,
        onClick = onClick,
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = buttonIconPadding),
    ) {
        Icon(
            modifier = Modifier.size(26.dp),
            painter = painterResource(id = if (isIncrement) R.drawable.ic_plus_white else R.drawable.ic_minus_white),
            tint = DynamicTheme.colors.text1,
            contentDescription = null
        )
    }
}

/**
 * Button with current value and unit
 */
@Composable
private fun ControlCurrent(
    isLeft: Boolean = true,
    label: String = "SPEED",
    value: String = "6.0",
    unit: String = "MPH",
    onClick: () -> Unit = {},
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = buttonIconPadding)
            .aspectRatio(1.5f),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = panelBackgroundColor),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = value,
                color = DynamicTheme.colors.text1,
                fontWeight = FontWeight.Bold,
                fontSize = panelControlValueSize
            )
            Text(
                modifier = Modifier.align(Alignment.TopCenter),
                text = label,
                color = DynamicTheme.colors.text1,
                fontWeight = FontWeight.Bold,
                fontSize = panelControlTitleSize
            )
            Text(
                modifier = Modifier.align(Alignment.BottomCenter),
                text = unit,
                color = DynamicTheme.colors.text1,
                fontWeight = FontWeight.Bold,
                fontSize = panelControlUnitSize
            )
            Icon(
                modifier = Modifier
                    .padding(4.dp)
                    .size(10.dp)
                    .align(if (isLeft) Alignment.BottomEnd else Alignment.BottomStart),
                painter = painterResource(id = R.drawable.ic_keypad_black),
                tint = DynamicTheme.colors.text1,
                contentDescription = null,
            )
        }
    }
}


/**
 * Button with back icon and last value (or a value with a title)
 */
@Composable
private fun ControlBack(
    labelOption: String? = null, // in rare situations the back button is used to display extra stat
    value: String = "2.2",
    onClick: () -> Unit = {},
) {
    PanelIconButton(onClick = onClick,
        modifier = Modifier
            .aspectRatio(1.3f)
            .fillMaxHeight(),
        containerColor = themeButtonColor,
        contentPadding = PaddingValues()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

