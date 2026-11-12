package com.jht.sprint8.ui.topbar

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jht.enums.FrameType
import com.jht.enums.UiPanelType
import com.jht.enums.WorkoutId
import com.jht.enums.WorkoutStage
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelBase
import com.jht.interfaces.ui.InnerPanelHolder
import com.jht.interfaces.ui.UiPanelOperations
import com.jht.sprint8.R
import com.jht.sprint8.sprint8classic.components.SprintStateType
import com.jht.sprint8.sprint8classic.components.formatTime
import com.jht.theme.DynamicTheme
import com.jht.ui.computeSize
import com.jht.unit.SpeedConvertUtil
import com.jht.unit.UnitFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.jht.widgets.helpers.getInjectionEntryPoint
import kotlinx.parcelize.Parcelize
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * The top panel for Sprint8 workouts.
 *  * Functions:
 *  * - PanelContent: Renders the top banner UI by invoking TopPanel().
 *  * - TopPanel: Builds the main layout with left, center, and right sections.
 *  * - setDataMatrix: Initializes data listeners and updates UI state.
 *  * - setSprintStateType: Determines the current workout state type.
 *  * - getCenterLabel: Returns the label text for the center panel.
 *  * - updateVisibility: Controls visibility of the top panel based on state.
 *  * - updateLayoutBasedOnEquipment: Defines layout based on equipment type.
 */
// Taken directly from app id used by the system
internal val SPRINT8_UUID: UUID = UUID.fromString("4a53c8b8-c7fc-429f-913e-076960a6ea0e")

@Parcelize
class Sprint8TopBarHolder : InnerPanelHolder() {

    override fun createPanel(
        context: Context, panelOperations: UiPanelOperations
    ): InnerPanelBase {
        val injection = getInjectionEntryPoint(context)
        val manager = Sprint8TopBarManager(
            injection.workout(),
            injection.ui(),
            injection.uiBase(),
            injection.navigation()
        )
        return Sprint8TopBar(panelOperations, manager)
    }
}

/**
 * This class manages all ui states to be sent to [Sprint8TopBar]
 *
 */
class Sprint8TopBarManager @Inject constructor(
    private val workoutModel: WorkoutModel,
    private val uiService: UIServiceModel,
    private val uiBaseModel: UIBaseModel,
    private val navigationModel: NavigationModel
) : AutoCloseable {

    private var _uiState = MutableStateFlow(Sprint8TopBarUi(frameType = uiBaseModel.frameType))

    val uiState = _uiState.asStateFlow()

    init {
        updateLayoutBasedOnEquipment()
        listenToDataMatrixKeys()
    }

    @VisibleForTesting
    fun setFrameType(frame: FrameType) {
        _uiState.value = _uiState.value.copy(frameType = frame)
    }

    @VisibleForTesting
    fun setIsSprint8Workout(value: Boolean) {
        _uiState.value = _uiState.value.copy(isSprint8Workout = value)
    }

    @VisibleForTesting
    fun setIsWorkoutStopped(value: Boolean) {
        _uiState.value = _uiState.value.copy(isWorkoutStopped = value)
    }

    fun skipInterval() {
        val isSprinting = _uiState.value.isSprinting
        if (isSprinting) {
            workoutModel.actionSkipInterval()
        }
    }

    fun navigate(destination: String) {
        navigationModel.launchApp(destination)
    }

    fun updateSprintState() {
        if (_uiState.value.isSprinting) {
            _uiState.value = _uiState.value.copy(sprintState = SprintStateType.Sprint)
        } else if (_uiState.value.isCoolDown) {
            _uiState.value = _uiState.value.copy(sprintState = SprintStateType.Cooldown)
        } else if (_uiState.value.isWarmUp) {
            _uiState.value = _uiState.value.copy(sprintState = SprintStateType.WarmUp)
        } else {
            _uiState.value = _uiState.value.copy(sprintState = SprintStateType.Recovery)
        }
    }

    fun listenToDataMatrixKeys() {
        workoutModel.let {
            it.isSprinting.listenAndTrigger { isSprinting ->
                _uiState.value = uiState.value.copy(isSprinting = isSprinting)
                updateSprintState()
            }
            it.isRamping.listenAndTrigger { isRamping ->
                _uiState.value = uiState.value.copy(isRamping = isRamping && it.hasFrameControl())
            }
            it.intervalRemaining.listenAndTrigger { intervalRemaining ->
                _uiState.value =
                    uiState.value.copy(intervalRemaining = formatTime(intervalRemaining))

            }
            it.workoutStage.listenAndTrigger { stage ->
                _uiState.value = uiState.value.copy(
                    isCoolDown = stage == WorkoutStage.COOL_DOWN,
                    isWarmUp = stage == WorkoutStage.WARM_UP
                )
                updateSprintState()
            }
            it.workoutId.listenAndTrigger { id ->
                val isSprint8Workout = (id == WorkoutId.SPRINT8)
                _uiState.value = uiState.value.copy(isSprint8Workout = isSprint8Workout)
                updateVisibility()
            }
            it.isWorkoutStopped.listenAndTrigger { stop ->
                _uiState.value = uiState.value.copy(isWorkoutStopped = stop)
                updateVisibility()
            }
            it.isMetric.listenAndTrigger { isMetric ->
                _uiState.value = uiState.value.copy(isMetric = isMetric)
            }
            it.scriptNextIncline.listenAndTrigger { value ->
                _uiState.value = uiState.value.copy(nextIncline = value.toString())
            }
            it.scriptNextSpeed.listenAndTrigger { speed ->
                val convertedSpeed = convertNextSpeed(speed)
                _uiState.value = uiState.value.copy(updateNextSpeed = convertedSpeed)
            }
            it.scriptNextResistance.listenAndTrigger { resistance ->
                _uiState.value = uiState.value.copy(nextResistance = resistance.toString())
            }
        }
        uiService.stateFullScreen.listenAndTrigger { isFullScreen ->
            _uiState.value = uiState.value.copy(isFullScreen = isFullScreen)
        }
    }

    private fun convertNextSpeed(speed: Float): String {
        val isMetric = _uiState.value.isMetric
        val nextSpeedFormatted =
            if (isMetric) speed else SpeedConvertUtil.fixKPH(speed)
        return UnitFormatter.formatSpeed(nextSpeedFormatted, isMetric)
    }

    fun updateLayoutBasedOnEquipment() {
        val frameType = _uiState.value.frameType
        val (incline, resistance, speed) = when (frameType) {
            FrameType.Bike, FrameType.SpinBike, FrameType.Climbmill,
            FrameType.Elliptical, FrameType.Stepper -> Triple(false, true, false)

            FrameType.Ascent -> Triple(true, true, false)

            FrameType.Treadmill -> Triple(true, false, true)

            else -> Triple(false, false, false)
        }
        _uiState.value = _uiState.value.copy(
            isInclineVisible = incline,
            isSpeedVisible = speed,
            isResistanceVisible = resistance

        )
    }

    override fun close() {
        workoutModel.close()
        uiBaseModel.close()
        uiService.close()
        navigationModel.close()
    }

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
        onHide: () -> Unit = hideCallback
    ) {
        _showUi = showUi
        val shouldShow = _uiState.value.isSprint8Workout && _showUi && !_uiState.value.isWorkoutStopped
        if (shouldShow) onShow() else onHide()
    }

    data class Sprint8TopBarUi(
        val isSprinting: Boolean = false,
        val updateVisibility: Boolean = false,
        val isTopAppbarVisible: Boolean = false,
        val isRamping: Boolean = false,
        val intervalRemaining: String = "",
        val isCoolDown: Boolean = false,
        val isWarmUp: Boolean = false,
        val isSprint8Workout: Boolean = false,
        val isWorkoutStopped: Boolean = false,
        val isMetric: Boolean = false,
        val nextIncline: String = "",
        val updateNextSpeed: String = "",
        val nextResistance: String = "",
        val isFullScreen: Boolean = false,
        val frameType: FrameType,
        val isInclineVisible: Boolean = false,
        val isResistanceVisible: Boolean = false,
        val isSpeedVisible: Boolean = false,
        val sprintState: SprintStateType = SprintStateType.WarmUp
    )

}

internal class Sprint8TopBar(
    panelOperations: UiPanelOperations,
    private val manager: Sprint8TopBarManager
) : InnerPanelBase(
    panelId = SPRINT8_UUID, panelType = UiPanelType.TopPanel, panelOperations = panelOperations
) {
    private val componentTag = "Sprint8TopBar"

    init {
        manager.setCallbacks(onShow = { show() }, onHide = { hide() })
    }

    @Composable
    override fun PanelContent() {
        val uiState = manager.uiState.collectAsStateWithLifecycle()
        TopPanel(
            isFullScreen = uiState.value.isFullScreen,
            onNavigate = { manager.navigate(SPRINT8_UUID.toString()) },
            onSkipClicked = { manager.skipInterval() },
            isSprinting = uiState.value.isSprinting,
            isInclineVisible = uiState.value.isInclineVisible,
            isCoolDown = uiState.value.isCoolDown,
            isRamping = uiState.value.isRamping,
            sprintState = uiState.value.sprintState,
            intervalRemaining = uiState.value.intervalRemaining,
            isSpeedVisible = uiState.value.isSpeedVisible,
            nextResistance = uiState.value.nextResistance,
            isMetric = uiState.value.isMetric,
            nextSpeed = uiState.value.updateNextSpeed,
            nextIncline = uiState.value.nextIncline,
            isWarmUp = uiState.value.isWarmUp
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun TopPanel(
        isFullScreen: Boolean,
        onNavigate: () -> Unit = {},
        onSkipClicked: () -> Unit = {},
        isSprinting: Boolean,
        isInclineVisible: Boolean,
        isCoolDown: Boolean,
        isRamping: Boolean,
        sprintState: SprintStateType,
        intervalRemaining: String,
        isSpeedVisible: Boolean,
        nextResistance: String,
        isMetric: Boolean,
        nextSpeed: String,
        nextIncline: String,
        isWarmUp: Boolean
    ) {
        Box(
            modifier = Modifier
                .testTag("$componentTag:LaunchSprint8:Box")
                .fillMaxWidth()
                .background(
                    if (isFullScreen) {
                        DynamicTheme.colors.transparent
                    } else {
                        DynamicTheme.colors.topPanelBackground
                    }
                )
                .semantics { testTagsAsResourceId = true }
                .padding(5.dp.computeSize())
                .clickable { onNavigate() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                // Left
                Box(
                    modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart
                ) {
                    NextInclineData(
                        isInclineVisible = isInclineVisible,
                        isSprinting = isSprinting,
                        isCoolDown = isCoolDown,
                        nextIncline = nextIncline,


                        )
                }

                // Center
                Box(
                    modifier = Modifier.weight(1f), contentAlignment = Alignment.Center
                ) {
                    CenterData(
                        isRamping = isRamping,
                        sprintState = sprintState,
                        intervalRemaining = intervalRemaining,
                        isSprinting = isSprinting,
                        isCoolDown = isCoolDown,
                        isWarmUp = isWarmUp
                    )
                }

                // Right
                Box(
                    modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd
                ) {
                    if (isSprinting) {
                        Skip(
                            isSprinting = isSprinting,
                            onSkipClicked = { onSkipClicked() }
                        )
                    } else {
                        NextSpeedResistanceData(
                            isSprinting = isSprinting,
                            isCoolDown = isCoolDown,
                            isSpeedVisible = isSpeedVisible,
                            nextResistance = nextResistance,
                            isMetric = isMetric,
                            nextSpeed = nextSpeed
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun NextInclineData(
        isInclineVisible: Boolean,
        isSprinting: Boolean,
        isCoolDown: Boolean,
        nextIncline: String
    ) {
        Row(
            modifier = Modifier
                .alpha(
                    if (!isInclineVisible || isSprinting || isCoolDown) 0f else 1f
                )
                .background(
                    color = DynamicTheme.colors.nextInclineSpeedBackground,
                    shape = RoundedCornerShape(12.dp.computeSize())
                )
                .width(250.dp.computeSize())
                .padding(horizontal = 20.dp.computeSize(), 10.dp.computeSize()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .testTag("$componentTag:NextIncline:Title:Text"),
                text = stringResource(R.string.next_incline_tc),
                fontSize = 18.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = DynamicTheme.colors.nextInclineSpeed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier
                    .wrapContentWidth()
                    .testTag("$componentTag:NextIncline:Text"),
                text = stringResource(R.string.percentage_format, nextIncline),
                fontSize = 20.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = DynamicTheme.colors.nextInclineSpeed
            )
        }
    }

    @Composable
    fun CenterData(
        isRamping: Boolean,
        sprintState: SprintStateType,
        intervalRemaining: String,
        isSprinting: Boolean,
        isCoolDown: Boolean,
        isWarmUp: Boolean
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "Blink")
        // Animate alpha only when ramping
        val alphaAnim = infiniteTransition.animateFloat(
            initialValue = if (isRamping) 1f else 1f,
            targetValue = if (isRamping) 0f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "AlphaAnim"
        )
        Row(
            modifier = Modifier
                .graphicsLayer(alpha = if (isRamping) alphaAnim.value else 1f)
                .background(
                    when (sprintState) {
                        SprintStateType.WarmUp -> DynamicTheme.colors.centerWarmupBackground
                        SprintStateType.Recovery -> DynamicTheme.colors.centerRecoveryBackground
                        SprintStateType.Cooldown -> DynamicTheme.colors.centerCooldownBackground
                        SprintStateType.Sprint -> DynamicTheme.colors.nextInclineSpeedBackground
                    }, shape = RoundedCornerShape(12.dp.computeSize())
                )
                .width(250.dp.computeSize())
                .padding(horizontal = 20.dp.computeSize(), 10.dp.computeSize()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.testTag("$componentTag:SprintStage:Text"),
                text = getCenterLabel(
                    isSprinting = isSprinting,
                    isRamping = isRamping,
                    isCoolDown = isCoolDown,
                    isWarmUp = isWarmUp
                ),
                fontSize = 22.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = when (sprintState) {
                    SprintStateType.WarmUp -> DynamicTheme.colors.centerWarmupText
                    SprintStateType.Recovery -> DynamicTheme.colors.centerRecoveryText
                    SprintStateType.Cooldown -> DynamicTheme.colors.centerCooldownText
                    SprintStateType.Sprint -> DynamicTheme.colors.nextInclineSpeed
                }
            )
            Text(
                modifier = Modifier.testTag("$componentTag:IntervalRemaining:Text"),
                text = intervalRemaining,
                fontSize = 26.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = when (sprintState) {
                    SprintStateType.WarmUp -> DynamicTheme.colors.centerWarmupText
                    SprintStateType.Recovery -> DynamicTheme.colors.centerRecoveryText
                    SprintStateType.Cooldown -> DynamicTheme.colors.centerCooldownText
                    SprintStateType.Sprint -> DynamicTheme.colors.nextInclineSpeed
                }
            )
        }
    }

    @Composable
    fun NextSpeedResistanceData(
        isSprinting: Boolean,
        isCoolDown: Boolean,
        isSpeedVisible: Boolean,
        isMetric: Boolean,
        nextResistance: String,
        nextSpeed: String
    ) {
        Row(
            modifier = Modifier
                .alpha(
                    if (isSprinting || isCoolDown) 0f else 1f
                )
                .background(
                    color = DynamicTheme.colors.nextInclineSpeedBackground,
                    shape = RoundedCornerShape(12.dp.computeSize())
                )
                .width(250.dp.computeSize())
                .padding(horizontal = 20.dp.computeSize(), 10.dp.computeSize()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .testTag("$componentTag:NextSpeedResistance:Title:Text"),
                text = if (isSpeedVisible) stringResource(R.string.next_speed_tc) else stringResource(
                    R.string.next_resistance_tc
                ),
                fontSize = 18.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = DynamicTheme.colors.nextInclineSpeed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier
                    .wrapContentWidth()
                    .testTag("$componentTag:NextSpeedResistance:Text"),
                text = if (isSpeedVisible) {
                    "${nextSpeed} ${stringResource(if (isMetric) R.string.kph_lc else R.string.mph_lc)}"
                } else {
                    nextResistance
                },
                fontSize = 20.sp.computeSize(),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = DynamicTheme.colors.nextInclineSpeed
            )
        }
    }

    @Composable
    fun Skip(
        onSkipClicked: () -> Unit,
        isSprinting: Boolean
    ) {
        Card(
            onClick = {
                onSkipClicked()
            },
            shape = RoundedCornerShape(12.dp.computeSize()),
            border = BorderStroke(1.dp.computeSize(), DynamicTheme.colors.centerRecoveryText),
            colors = CardDefaults.cardColors(
                containerColor = DynamicTheme.colors.centerWarmupBackground
            ),
            modifier = Modifier
                .wrapContentSize()
                .alpha(if (isSprinting) 1f else 0f)
                .testTag("$componentTag:SkipClick:Card"),
        ) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 24.dp.computeSize(), 10.dp.computeSize())
                    .background(color = DynamicTheme.colors.centerWarmupBackground),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.skip_tc),
                    modifier = Modifier
                        .testTag("$componentTag:Skip:Title:Text")
                        .heightIn(max = 100.dp.computeSize()),
                    fontSize = 22.sp.computeSize(),
                    color = DynamicTheme.colors.centerRecoveryText,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(10.dp.computeSize()))
                Image(
                    modifier = Modifier.testTag("$componentTag:SkipForwardIcon:Image"),
                    painter = painterResource(R.drawable.skip_forward_new_fill),
                    contentDescription = null
                )
            }
        }
    }

    @Composable
    private fun getCenterLabel(
        isSprinting: Boolean,
        isRamping: Boolean,
        isCoolDown: Boolean,
        isWarmUp: Boolean
    ): String {
        return when {
            isSprinting -> stringResource(R.string.sprint_tc)
            isRamping -> stringResource(R.string.ramping_up_tc)
            isCoolDown -> stringResource(R.string.cooldown_tc)
            isWarmUp -> stringResource(R.string.warmup_tc)
            else -> stringResource(R.string.recovery_tc)
        }
    }

    fun formatTime(milliseconds: Float): String {
        val seconds = (milliseconds / 1000f).roundToInt()
        return String.format(Locale.ENGLISH, "%d:%02d", seconds / 60, seconds % 60)
    }

    override fun onPanelReceiveData(panelData: BasePanelData) {
        val sprint8PanelData = panelData as? Sprint8PanelData
        sprint8PanelData?.let { data ->
            manager.updateVisibility(
                showUi = data.isSprint8TopBarVisible
            )
        }
    }

    override fun onPanelDestroyed() {
        manager.close()
    }
}
