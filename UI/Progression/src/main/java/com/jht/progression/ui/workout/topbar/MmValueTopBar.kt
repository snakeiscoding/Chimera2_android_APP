package com.jht.progression.ui.workout.topbar

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.enums.IntervalType
import com.jht.enums.UiPanelType
import com.jht.interfaces.models.NavigationModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.UserModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.progression.interfaces.ProgressionModel
import com.jht.interfaces.ui.BasePanelData
import com.jht.interfaces.ui.InnerPanelBase
import com.jht.interfaces.ui.InnerPanelHolder
import com.jht.interfaces.ui.UiPanelOperations
import com.jht.data.ProgressionInterval
import com.jht.progression.PROGRESSION_APP_UUID
import com.jht.progression.R
import com.jht.progression.data.program.getSpeedForIntervalType
import com.jht.progression.interfaces.getProgressionInjectionEntryPoint
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.getIntervalTypeColor
import com.jht.progression.userinfo.UserInfoManager
import com.jht.ui.AllDevicePreviews
import com.jht.unit.UnitFormatter
import com.jht.widgets.AutoSizeText
import com.jht.widgets.helpers.getInjectionEntryPoint
import com.jht.widgets.scaleSize
import kotlinx.parcelize.Parcelize
import java.util.UUID

val MM_TOP_BAR_ID: UUID = UUID.fromString("fc5ff521-d02b-4755-961a-9bd04c4055e2")

@Parcelize
class MmTopBarHolder : InnerPanelHolder() {
    override fun createPanel(
        context: Context, panelOperations: UiPanelOperations
    ): InnerPanelBase {
        return MmValueTopBar(context, panelOperations)
    }
}

@SuppressLint("ViewConstructor")
class MmValueTopBar(
    val context: Context,
    panelOperations: UiPanelOperations
) : InnerPanelBase(
    panelId = MM_TOP_BAR_ID,
    panelType = UiPanelType.ValueUpdatePanelOverride,
    panelOperations = panelOperations
) {

    // Mutable states that control the state of the UI panel
    private val timeRemaining = mutableStateOf("0:00")
    private val leftValue = mutableStateOf("")
    private val leftTitle = mutableStateOf("Current")
    private val rightValue = mutableStateOf("")
    private val rightTitle = mutableStateOf("UP NEXT")
    private val leftColor = mutableStateOf(Color.Cyan)
    private val rightColor = mutableStateOf(Color.Red)

    private val uiServiceModel: UIServiceModel
    private val workoutModel: WorkoutModel
    private val progressionModel: ProgressionModel
    private val userModel: UserModel
    private val navigationModel: NavigationModel

    // Variables that determine whether the panel should be displayed
    private var isFullscreen = false
    private var isChangingInterval = false
    private var isOnProgressionScreen = false
    private var isMetric = false
    private var intervals: List<ProgressionInterval> = ArrayList()
    private var isWorkoutStopped = true

    // next speed in kph, null means there is no next interval
    private var nextSpeedKph: Float? = 0f

    init {
        val injection = getInjectionEntryPoint(context)
        uiServiceModel = injection.ui()
        workoutModel = injection.workout()
        progressionModel = getProgressionInjectionEntryPoint(context).progression()
        navigationModel = injection.navigation()
        userModel = injection.user()

        val userInfoManager = UserInfoManager(userModel, progressionModel)
        userInfoManager.loadUserInfo()

        leftTitle.value = context.getString(R.string.current_uc)
        rightTitle.value = context.getString(R.string.next_uc)

        workoutModel.isMetric.listenAndTrigger {
            isMetric = it
            recalculateNextSpeed(userInfoManager.userInfo.baselineKph)
            rightValue.value = getNextSpeedText(nextSpeedKph, isMetric)
            leftValue.value = getSpeedText(workoutModel.speedKph.value, isMetric)
        }

        progressionModel.workoutIntervalList.listenAndTrigger { intervals = it.intervalList }

        uiServiceModel.stateFullScreen.listenAndTrigger {
            isFullscreen = it
            updateTopBarVisibility()
        }

        workoutModel.intervalGoalRemaining.listenAndTrigger { goalRemaining ->
            isChangingInterval = goalRemaining < 10000
            timeRemaining.value = UnitFormatter.formatTimeMilliseconds(goalRemaining)
            updateTopBarVisibility()
        }

        workoutModel.speedKph.listenAndTrigger { speedKph ->
            val units = context.getString(if (isMetric) R.string.kph_uc else R.string.mph_uc)
            val speed = UnitFormatter.formatSpeed(speedKph, isMetric)
            leftValue.value = "$speed $units"
        }

        workoutModel.intervalNumber.listenAndTrigger { intervalNum ->
            // get color for current interval
            if (intervals.size > (intervalNum)) { // If the current interval exists
                val currentInterval = intervals[intervalNum]
                val currentIntervalColor = getIntervalTypeColor(currentInterval.intervalType)
                leftColor.value = currentIntervalColor
            }

            // Next interval type, null means no next interval
            val nextIntervalType: IntervalType?

            // Figure out what the next speed and interval type will be
            if (intervals.size > intervalNum + 1) { // If the next interval exists
                val nextInterval = intervals[intervalNum + 1]
                nextIntervalType = nextInterval.intervalType
                nextSpeedKph = getSpeedForIntervalType(
                    nextIntervalType,
                    userInfoManager.userInfo.baselineKph,
                    clampSpeed = { workoutModel.clampSpeedToFrameLimits(it) }
                )
            } else {
                nextSpeedKph = null
                nextIntervalType = null
            }

            // Set next speed and interval color
            rightValue.value = getNextSpeedText(nextSpeedKph, isMetric)
            val nextIntervalColor = getIntervalTypeColor(nextIntervalType)
            rightColor.value = nextIntervalColor
        }

        workoutModel.isWorkoutStopped.listenAndTrigger {
            isWorkoutStopped = it
            updateTopBarVisibility()
        }
    }

    override fun onPanelReceiveData(panelData: BasePanelData) {
        val progressionPanelData = panelData as? ProgressionPanelData
        if (progressionPanelData != null)
        {
            isOnProgressionScreen = progressionPanelData.isProgressionAppVisible
            updateTopBarVisibility()
        }
    }

    private fun getNextSpeedText(nextSpeedKph: Float?, useMetric: Boolean): String {
        return if (nextSpeedKph != null) {
            getSpeedText(nextSpeedKph, useMetric)
        } else {
            //TODO: Figure out a better end string?
            context.getString(R.string.well_done_uc)
        }
    }

    private fun getSpeedText(speedKph: Float, useMetric: Boolean): String {
        val speed = UnitFormatter.formatSpeed(speedKph, isMetric)
        val unit = context.getString(if (useMetric) R.string.kph_uc else R.string.mph_uc)
        return "$speed $unit"
    }

    private fun updateTopBarVisibility() {
        val show = !isWorkoutStopped &&
                ((isFullscreen && isChangingInterval) || (!isFullscreen && !isOnProgressionScreen))

        if (show) {
            show()
        } else {
            hide()
        }
    }

    private fun startMmActivity() {
        if (uiServiceModel.stateFullScreen.value) {
            uiServiceModel.toggleFullscreen()
        }

        navigationModel.launchApp(PROGRESSION_APP_UUID)
    }

    /**
     * Updates the next speed value when the current selected unit changes. This can change
     * the min and max value it clamps to so we need to recalculate the value.
     */
    private fun recalculateNextSpeed(baseline: Float) {
        // Exit immediately if there's no value to recalculate
        if (nextSpeedKph == null) return

        // If we have a next interval get the clamped value
        val intervalNum = workoutModel.intervalNumber.value
        if (intervals.size > intervalNum + 1) { // If the next interval exists
            val nextInterval = intervals[intervalNum + 1]
            val nextIntervalType = nextInterval.intervalType
            nextSpeedKph = getSpeedForIntervalType(
                nextIntervalType,
                baseline,
                clampSpeed = { workoutModel.clampSpeedToFrameLimits(it) }
            )
        }
    }

    @Composable
    override fun PanelContent() {
        TopBar(
            timeRemaining = timeRemaining.value,
            leftValue = leftValue.value,
            leftTitle = leftTitle.value,
            rightValue = rightValue.value,
            rightTitle = rightTitle.value,
            leftColor = leftColor.value,
            rightColor = rightColor.value,
            onClickTopBar = { startMmActivity() }
        )
    }

    override fun onPanelDestroyed() {
        progressionModel.close()
        uiServiceModel.close()
        userModel.close()
        workoutModel.close()
        navigationModel.close()
    }
}


/**
 * Composable UI for top bar
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    timeRemaining: String,
    leftValue: String,
    leftTitle: String,
    rightValue: String,
    rightTitle: String,
    leftColor: Color,
    rightColor: Color,
    onClickTopBar: () -> Unit
) {
    Row(modifier = modifier
        .background(Color(0xFF010F1B))
        .clickable { onClickTopBar() }
        .semantics {
            testTagsAsResourceId = true
            contentDescription = "Standard Workout Top Bar"
        }
    ) {
        Spacer(modifier = Modifier.weight(1f))
        TopBarValue(
            modifier = Modifier.weight(3f),
            value = leftValue,
            label = leftTitle,
            color = leftColor
        )
        Spacer(modifier = Modifier.weight(1f))
        TimeRemaining(modifier = Modifier.weight(4f), time = timeRemaining, leftColor)
        Spacer(modifier = Modifier.weight(1f))
        TopBarValue(
            modifier = Modifier.weight(3f),
            value = rightValue,
            label = rightTitle,
            color = rightColor
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TopBarValue(modifier: Modifier = Modifier, value: String, label: String, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AutoSizeText(
            modifier = Modifier
                .weight(2f)
                .padding(top = 10.dp, bottom = 5.dp)
                .wrapContentHeight()
                .testTag(label.lowercase()+"_interval_speed"),
            maxFontSize = 36.sp.scaleSize(),
            fontWeight = FontWeight.Bold,
            text = value,
            color = color,
            style = oxaniumStyle,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .background(color.copy(alpha = 0.2f))
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            AutoSizeText(
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .align(Alignment.Center)
                    .testTag(label.lowercase()+"_babel"),
                fontWeight = FontWeight.Bold,
                maxFontSize = 21.sp.scaleSize(),
                lineHeight = 31.sp.scaleSize(),
                text = label,
                color = Color.White,
                style = oxaniumStyle,
            )
        }
    }
}

@Composable
private fun TimeRemaining(modifier: Modifier = Modifier, time: String, color: Color) {
    Box(modifier = modifier) {
        Image(
            modifier = Modifier.fillMaxSize().testTag("workout_top_bar_center_circle"),
            painter = painterResource(id = R.drawable.mm_center_circle),
            contentDescription = "",
            contentScale = ContentScale.FillWidth,
            colorFilter = ColorFilter.tint(color.copy(alpha = 0.2f))
        )
        AutoSizeText(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(all = 10.dp)
                .fillMaxSize()
                .testTag("current_interval_time_countdown"),
            fontWeight = FontWeight.SemiBold,
            maxFontSize = 48.sp.scaleSize(),
            text = time,
            color = Color.White,
            style = oxaniumStyle,
        )
    }
}

@Composable
@AllDevicePreviews
private fun MmTopBarPreview() {
    TopBar(
        modifier = Modifier.aspectRatio(1000f / 80f),
        timeRemaining = "0:00",
        leftValue = "2.0 MPH",
        leftTitle = "CURRENT",
        rightValue = "3.2 MPH",
        rightTitle = "UP NEXT",
        leftColor = getIntervalTypeColor(IntervalType.ZONE_1),
        rightColor = getIntervalTypeColor(IntervalType.ZONE_3),
        onClickTopBar = {}
    )
}