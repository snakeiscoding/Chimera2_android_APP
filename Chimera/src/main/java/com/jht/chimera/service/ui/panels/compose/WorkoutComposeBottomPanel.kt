package com.jht.chimera.service.ui.panels.compose

import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jht.chimera.R
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.RealScreen
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys
import com.jht.chimera.service.ui.panels.ExtraBarOptions
import com.jht.chimera.service.ui.panels.PANEL_OVERRIDES_FORCE_HIDE_APP_BAR_KEY
import com.jht.datamatrixlib.DataMatrix
import com.jht.enums.CaloriesSource
import com.jht.enums.WorkoutStage
import com.jht.interfaces.models.DeviceModel
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.UIServiceModel
import com.jht.interfaces.models.WorkoutModel
import com.jht.theme.DynamicTheme
import com.jht.theme.disabledBackground
import com.jht.ui.clickableNoEffect
import com.jht.widgets.PopupOverlayPanel
import com.jht.widgets.helpers.PressedAction
import com.jht.widgets.helpers.StatInfo
import com.jht.widgets.helpers.StatInfoCategory
import com.jht.widgets.helpers.StatInfoCollection
import com.jht.widgets.helpers.StatType
import com.jht.widgets.helpers.getInjectionEntryPoint
import com.jht.widgets.helpers.getMyZoneBackgroundColor
import com.jht.widgets.helpers.getMyZoneIcon
import com.jht.widgets.helpers.getMyZoneTextColor
import com.jht.widgets.helpers.stringResourceDefault
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Quick little class for figuring out column allocations
private class ColumnBreakDown(statInfoCollection: StatInfoCollection) {
    val totalColumns: Int // total number of columns, each category is a column in UI
    val goalIndex: Int // will be -1 if not found, which means the checks will return false
    val goalAdjustSpace = mutableStateOf(DpSize.Zero) // space required for a single goal adjustment button, there is one on each side of goal stat card (assumed to be equal size)
    init {
        totalColumns = statInfoCollection.categoryList.size
        goalIndex = statInfoCollection.categoryList.indexOf(statInfoCollection.findStatCategory(StatType.TOTAL_GOAL))
    }
    fun isGoal(columnIndex: Int) = (columnIndex == goalIndex) // is the index the goal column
    fun isAdjustNext(columnIndex: Int) = (columnIndex == goalIndex || columnIndex == (goalIndex-1)) // is goal adjust inserted after this column
}

/**
 * Bottom panel during an active workout
 */
class WorkoutComposeBottomPanel(context: Context) : ComposePanel(context) {
    private val isFull = mutableStateOf(false)
    override fun setFullscreen(isFull: Boolean) { this.isFull.value = isFull }
    private val appBarOptions: MutableState<ExtraBarOptions?> = mutableStateOf(null)
    override fun setAppBar(options: ExtraBarOptions?) {
        appBarOptions.value = options
    }
    private val forceHideAppBar = mutableStateOf(false)
    override fun setPanelOverrides(panelOverrides: Bundle) {
        if (panelOverrides.getBoolean(PANEL_OVERRIDES_FORCE_HIDE_APP_BAR_KEY, false)) forceHideAppBar.value = true
    }
    override fun clearPanelOverrides(panelOverrides: Bundle) {
        if (panelOverrides.getBoolean(PANEL_OVERRIDES_FORCE_HIDE_APP_BAR_KEY, false)) forceHideAppBar.value = false
    }

    private val uiServiceModel: UIServiceModel
    private val workoutModel: WorkoutModel
    private val uiBaseModel: UIBaseModel
    private val deviceModel: DeviceModel
    private val allowGoalChange = mutableStateOf(true)
    private val workoutStage = mutableStateOf(WorkoutStage.WORKOUT)
    private val workoutIsLaunching = mutableStateOf(false)
    private val goalProgress = mutableStateOf(0.0f)
    private var hasPendingBehaviorChange = false

    init {
        val injection = getInjectionEntryPoint(context)
        uiBaseModel = injection.uiBase()
        workoutModel = injection.workout()
        uiServiceModel = injection.ui()
        deviceModel = injection.devices()
        /* TODO - do we need to sync with UiServiceKeys.DATA_COL_1_STAT_SELECTED_ORDER_INT etc ?
            or do we leverage something new for stat overlays?
         */
    }
    private fun close() {
        uiBaseModel.close()
        workoutModel.close()
        uiServiceModel.close()
        deviceModel.close()
    }

    override fun panelAdded() {
        super.panelAdded()
        resetStats(false)
    }

    override fun panelRemoved() {
        close()
        super.panelRemoved()
    }

    override fun onWorkoutReset() {
        super.onWorkoutReset()
        forceHideAppBar.value = false  // make sure this gets reset in case something goes wrong
        resetStats(true) // should cover frame change and workout specific change
    }

    override fun onPendingBehaviorChange(isPending: Boolean) {
        hasPendingBehaviorChange = isPending
    }

    private val statInfoCollection = mutableStateOf(createStatList()) // needs to be after init to access models
    private fun createStatList(): StatInfoCollection = StatInfoCollection.getList(
        frameType = uiBaseModel.frameType,
        showWatts = workoutModel.wattsDisplayAllowed,
        isTargetHeartRate = false, // workoutModel.workoutId.value.isTargetHeartRate // simple app shows target HR stat but not bottom bar
        showCadence = workoutModel.cadenceDisplayAllowed,
    )

    private fun resetStats(recreateList: Boolean) { // basically rewiring the stats to force update (could look into reworking stat helpers for a more direct forced update?)
        close()
        if (recreateList) statInfoCollection.value = createStatList() // recreating should account for frame and unit change
        statInfoCollection.value.registerListener(uiBaseModel, deviceModel, workoutModel)
        workoutModel.allowUserGoalChanges.listenAndTrigger { allowGoalChange.value = it }
        workoutModel.isWorkoutLaunching.listenAndTrigger {
            workoutIsLaunching.value = it
            updateStageFiltered(isLaunching = it)
        }
        workoutModel.workoutStage.listenAndTrigger { updateStageFiltered(stage = it) } // could this just be listen if isWorkoutLaunching will update it, or just play it safe?
        workoutModel.goalRemaining.listenAndTrigger {
            val total = workoutModel.goalTotal.value
            val complete = total - it
            val progress = if (total > 0) complete / total else 0f
            // at start progress may report 1f, by rights this should always be less than 1f
            // might be due to remaining always being reset to zero while goal could be non-zero?
            goalProgress.value = if (progress < 1f) progress else 0f
        }
        // DO NOT TRIGGER IMMEDIATE THESE OR YOU WILL LOOP BACK IN !
        workoutModel.isMetric.listen { resetStats(false) } // need to adjust units without loosing current selections
    }
    private fun updateStageFiltered(stage: WorkoutStage = workoutModel.workoutStage.value, isLaunching: Boolean = workoutIsLaunching.value) {
        // you can get warm up color briefly until workout actually starts since warmup is default stage (should we have a NONE stage?)
        // isWorkoutLaunching vs isWorkoutStopped? stopped you can see color change at very end of workout, launching is true partly into workout start so tiny delay in warmup color
        // should we consider passing extra parameter into composable and handle it in the color picking?
        // we could consider making workoutStage.value nullable to keep distinction between WORKOUT stage vs not valid yet?
        workoutStage.value = if (isLaunching) WorkoutStage.WORKOUT else stage
    }

    private fun onToggleFull() {
        if (!hasPendingBehaviorChange) {
            uiServiceModel.toggleFullscreen()
        }
    }
    private fun onBack() {
        if (!hasPendingBehaviorChange) {
            // original has some button debouncing code and runs command in a thread?
            Thread { ExecuteAsRoot.simulateBackButton() }.start()
        }
    }
    private fun onRefresh() {
        if (!hasPendingBehaviorChange) {
            DataMatrix.sendEvent(UiServiceKeys.EVENT_APP_BAR_REFRESH_BTN_CLICK) // probably need model equivalent?
        }
    }
    private fun decrementGoal() {
        workoutModel.decreaseWorkoutGoal()
    }

    private fun incrementGoal() {
        workoutModel.increaseWorkoutGoal()
    }

    private fun onUnitChange() {
        workoutModel.isMetric.value = !workoutModel.isMetric.value
    }

    @Composable
    override fun ThemedContent() {
        BottomPanel(
            statInfoCollection.value,
            appOptions = appBarOptions.value,
            forceHideAppBar = forceHideAppBar.value,
            onToggleFull = ::onToggleFull,
            onBack = ::onBack,
            onRefresh = ::onRefresh,
            isFull = isFull.value,
            allowGoalChange = allowGoalChange.value,
            workoutStage = workoutStage.value,
            progress = goalProgress.value,
            onIncrementGoalClick = ::incrementGoal,
            onDecrementGoalClick = ::decrementGoal,
            onUnitChange = ::onUnitChange,
        )
    }

    override fun getPanelParams(): WindowManager.LayoutParams {
        val hasAppBar = appBarOptions.value?.needExtraBar() == true && !forceHideAppBar.value
        val width = if (isFull.value) RealScreen.widthPixels else resources.getDimension(R.dimen.app_container_width).toInt()
        val height =
            if (isFull.value) resources.getDimension(R.dimen.app_buttons_bottom_bar_height).toInt() // app bar height
            else if (hasAppBar) resources.getDimension(R.dimen.bottom_bar_height).toInt() // combined height
            else resources.getDimension(R.dimen.bottom_bar_stats_height).toInt() // control bar height
        val viewParams = WindowManager.LayoutParams(
            width, height,
            mPanelMode.windowType, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT
        )

        viewParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        viewParams.x = 0
        viewParams.y = 0
        return viewParams
    }
}


/** TOP LEVEL COMPOSE for Workout Bottom Panel */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BottomPanel(
    statInfoCollection: StatInfoCollection,
    isFull: Boolean = false,
    appOptions: ExtraBarOptions? = null,
    forceHideAppBar: Boolean = false,
    onToggleFull: () -> Unit = {},
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    allowGoalChange: Boolean = true,
    workoutStage: WorkoutStage = WorkoutStage.WORKOUT,
    progress: Float = 0.5f,
    onIncrementGoalClick: () -> Unit = {},
    onDecrementGoalClick: () -> Unit = {},
    onUnitChange: () -> Unit = {},
) {
    Column(
        Modifier.semantics { testTagsAsResourceId = true },
    ) {
        val borderMode = (appOptions?.showStats == false)
        if (appOptions?.needExtraBar() == true && !forceHideAppBar) AppBar(isFull, appOptions, onToggleFull, onBack, onRefresh)
        if (!isFull && !borderMode) {
            HorizontalStatBorder(false)
            StatBar(statInfoCollection, allowGoalChange, workoutStage, progress, onIncrementGoalClick, onDecrementGoalClick, onUnitChange)
        }
    }
}

/** constants for cell borders to avoid doubling where cells meet */
private val spacing = 1.dp
private val statBorderColor @Composable get() = DynamicTheme.colors.gray3
private val extSpacing = spacing // alignment seems best if bar and extension match
private val extStatBorderColor @Composable get() = Color.Transparent
// define a padding as alternative to border in the extension cells (still need transparent border to keep alignment)
private val extPadding = 5.dp
@Composable private fun HorizontalStatBorder(ext: Boolean) = Box(
    Modifier
        .fillMaxWidth()
        .height(if (ext) extSpacing else spacing)
        .background(if (ext) extStatBorderColor else statBorderColor))
@Composable private fun VerticalStatBorder() = Box(
    Modifier
        .fillMaxHeight()
        .width(spacing)
        .background(statBorderColor))
@Composable private fun ExtVerticalBorder(leftItems: Int, rightItems: Int) {
    val cellsHigh = maxOf(leftItems, rightItems) - 1 // subtract one for bottom row
    val height = (dimensionResource(R.dimen.bottom_bar_stats_height) + extSpacing) * (if (cellsHigh > 0) cellsHigh else 0)
    Box(
        Modifier
            .height(height)
            .width(extSpacing)
            .background(extStatBorderColor)
    )
}
@Composable private fun getStageColor(stage: WorkoutStage?): Color? = when (stage) { // colors from original or theme?
    WorkoutStage.WARM_UP -> DynamicTheme.colors.stop //heart5 // Color(0xffE1261C) //Color.Red
    WorkoutStage.COOL_DOWN -> DynamicTheme.colors.coolDown //heart1 //Color(0xff31719e) //Color.Blue
    else -> null // defer to current color
}

@Composable
private fun StatBar(
    statInfoCollection: StatInfoCollection,
    allowGoalChange: Boolean = true,
    workoutStage: WorkoutStage = WorkoutStage.WORKOUT,
    progress: Float = 0.5f,
    onIncrementGoalClick: () -> Unit = {},
    onDecrementGoalClick: () -> Unit = {},
    onUnitChange: () -> Unit = {},
) {
    // track columns and space for goal adjust buttons so extension can match
    val columnBreakDown = remember(statInfoCollection, allowGoalChange) { ColumnBreakDown(statInfoCollection) }
    // track categories shown in extension popup
    val shownStatCategories = remember(statInfoCollection) { mutableStateListOf(*Array<StatInfoCategory?>(columnBreakDown.totalColumns) { null }) }
    val toggleStatCategory: (index: Int)->Unit = {
        shownStatCategories[it] = if (shownStatCategories[it] == null) statInfoCollection[it] else null
    }
    // need to show total goal for 3 sec after user adjusts it
    val scope = rememberCoroutineScope()
    val goalStatOverrideJob = remember(statInfoCollection) { mutableStateOf<Job?>(null) }
    val goalStatOverride = remember(statInfoCollection) { mutableStateOf<StatInfo?>(null) }
    val triggerGoalOverride: ()->Unit = { // increment/decrement goal briefly shows the total goal
        goalStatOverrideJob.value?.cancel() // restart time out
        goalStatOverrideJob.value = scope.launch {
            goalStatOverride.value = statInfoCollection.findStatInfo(StatType.TOTAL_GOAL)
            delay(3000)
            goalStatOverride.value = null
        }
    }
    // row consists of selected stat cards and optional goal adjust buttons
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Blue),
        verticalAlignment = Alignment.Bottom,
    ) {
        val itemModifier = Modifier
            .height(dimensionResource(R.dimen.bottom_bar_stats_height))
        val cardModifier = itemModifier.weight(1f)

        VerticalStatBorder()
        repeat(columnBreakDown.totalColumns) { c->
            val isGoalColumn = columnBreakDown.isGoal(c)
            val showGoal = (isGoalColumn && allowGoalChange)
            val goalColor = if (isGoalColumn) getStageColor(workoutStage) else null
            val goalProgress = if (isGoalColumn) progress else null
            val stat = if (isGoalColumn && goalStatOverride.value != null) goalStatOverride.value else statInfoCollection[c]?.selected?.value
            if (showGoal) {
                GoalStep(itemModifier, false,
                    { onDecrementGoalClick(); triggerGoalOverride() }, onSize = { columnBreakDown.goalAdjustSpace.value = it })
                VerticalStatBorder()
            }
            StatCard(
                cardModifier, { toggleStatCategory(c) }, statInfoCollection, stat, overrideColor = goalColor, progress = goalProgress,
                rightIconId = if (shownStatCategories[c] != null) R.drawable.ic_arrow_head_down else R.drawable.ic_arrow_head_up,
                testTag = "StatBar$c",
            )
            VerticalStatBorder()
            if (showGoal) {
                GoalStep(itemModifier, true,
                    { onIncrementGoalClick(); triggerGoalOverride() })
                VerticalStatBorder()
            }
        }

        // if any of the shown categories is not null, show extension panel
        if (shownStatCategories.firstNotNullOfOrNull { it } != null) {
            StatExtension(statInfoCollection, shownStatCategories, columnBreakDown,
                onDismissRequest = { shownStatCategories.fill(null) }, // dismiss should hide all extensions
                onCategoryClick = { shownStatCategories[it] = null}, // clicking within a column should hide that column
                onUnitChange)
        }
    }
}

@Composable
private fun StatExtension(
    statInfoCollection: StatInfoCollection,
    shownCategories: List<StatInfoCategory?>,
    columnBreakDown: ColumnBreakDown,
    onDismissRequest: (() -> Unit)? = null,
    onCategoryClick: (Int) -> Unit = {},
    onUnitChange: () -> Unit = {},
) {
    // taken from StatPanelExtension, would be nice to replace with something smarter and less hard coded (we need these in pixels, not Dp)
    val top: Int = LocalContext.current.resources.getDimension(R.dimen.top_bar_height_bordered).toInt()
    val bottom: Int = LocalContext.current.resources.getDimension(R.dimen.bottom_bar_stats_height).toInt()
    val width: Int = LocalContext.current.resources.getDimension(R.dimen.bottom_bar_width).toInt()
    val offset: Int = LocalContext.current.resources.getDimension(R.dimen.stat_bottom_margin).toInt()
    PopupOverlayPanel(
        offset = IntOffset(0, offset),
        size = IntSize(width, RealScreen.heightPixels - top - bottom),
        layoutGravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickableNoEffect { onDismissRequest?.invoke() }
                .background(disabledBackground)
        ) {
            Column(Modifier.align(Alignment.BottomCenter)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    ExtVerticalBorder(0, if (shownCategories[0] != null) statInfoCollection.getCategoryActiveSize(0) else 0)
                    repeat(columnBreakDown.totalColumns) { c->
                        val adjustWidth = columnBreakDown.goalAdjustSpace.value.width
                        val needAdjustSpace = (columnBreakDown.isGoal(c) && adjustWidth > 0.dp)
                        val needAdjustSpaceNext = (columnBreakDown.isAdjustNext(c) && adjustWidth > 0.dp)
                        val currentCount = shownCategories[c]?.getActiveSize() ?: 0
                        val nextCount = if (c+1 < shownCategories.size) shownCategories[c+1]?.getActiveSize() ?: 0 else 0
                        if (needAdjustSpace) {
                            Spacer(Modifier.size(adjustWidth))
                            ExtVerticalBorder(0, currentCount)
                        }
                        StatColumn(statInfoCollection, shownCategories[c], c) { clickedStat->
                            if (clickedStat.type.isSecondary()) {
                                onUnitChange() // change the unit, not the selection
                            } else {
                                shownCategories[c]?.selected?.value = clickedStat
                            }
                            onCategoryClick(c)
                        }
                        if (needAdjustSpace) {
                            ExtVerticalBorder(currentCount, 0)
                            Spacer(Modifier.size(adjustWidth))
                            ExtVerticalBorder(0, nextCount)
                        } else {
                            ExtVerticalBorder(currentCount, if (needAdjustSpaceNext) 0 else nextCount)
                        }
                    }
                }
                HorizontalStatBorder(true) // run a border along bottom of entire panel (panel seems to overlap the bottom bar border)
            }
        }
    }
}
@Composable
private fun RowScope.StatColumn(
    statInfoCollection: StatInfoCollection,
    statCategory: StatInfoCategory?, // if this is null, we should still draw border cells
    columnIndex: Int, // useful for testTag
    onClick: (StatInfo) -> Unit,
) {
    // show non-selected cards
    Column(
        Modifier
            .weight(1f)
            .padding(extPadding)
    ) {
        // compose will unroll the elements top to bottom, so we need to render in reverse
        statCategory?.getUnselectedList()?.reversed()?.forEachIndexed { index, statInfo ->
            if (statInfo.active.value) {
                // each cell adds a top border or top pad
                if (extPadding > 0.dp) Spacer(Modifier.size(extPadding))
                else HorizontalStatBorder(true)
                StatCard(
                    Modifier.height(dimensionResource(R.dimen.bottom_bar_stats_height)),
                    { onClick(statInfo) }, statInfoCollection, stat = statInfo, testTag = "StatBarExt${columnIndex}:${index}",
                )
            }
        }
    }
}

@Composable
private fun PanelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = DynamicTheme.colors.panelStatBackground),
        contentPadding = PaddingValues(),
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
private fun StatCard(
    modifier: Modifier,
    onClick: () -> Unit = {},
    statInfoCollection: StatInfoCollection, // if there is some interdependence or higher level knowledge we need
    stat: StatInfo? = StatInfo(R.string.mph_uc, 0, "0.0"),
    overrideColor: Color? = null,
    progress: Float? = null,
    rightIconId: Int = 0,
    testTag: String,
) {
    val color = overrideColor ?: DynamicTheme.colors.panelStatForeground
    val progressColor = overrideColor ?: DynamicTheme.colors.primary
    // some stats have special coloring when MyZone is connected (maybe need to look into how to handle these specializations more generically?)
    val useMyZoneColors = (statInfoCollection.myZoneActive.value && (stat?.type == StatType.HEART_RATE || stat?.type == StatType.MYZONE_HR_PERCENT))
    val myZoneHrPercent = statInfoCollection.myZoneHrPercent.value
    val showMyZone = statInfoCollection.myZoneActive.value && (stat?.type?.isMyZone() == true || stat?.type == StatType.CALORIES || stat?.type == StatType.HEART_RATE)
    val showEVolt = (statInfoCollection.caloriesSource.value == CaloriesSource.EVolt && stat?.type == StatType.CALORIES)
    val extraIconId =
        if (showEVolt) R.drawable.ic_evolt
        else if (showMyZone) {
            if (useMyZoneColors) getMyZoneIcon(myZoneHrPercent) else R.drawable.logo_myzone
        }
        else 0
    val backColor = if (useMyZoneColors) getMyZoneBackgroundColor(myZoneHrPercent) else Color.Transparent
    val textColor = if (useMyZoneColors) getMyZoneTextColor(myZoneHrPercent) else color
    PanelButton(onClick, modifier.testTag(testTag + "Button")) {
        Box(
            Modifier
                .fillMaxSize()
                .background(backColor)) {
            val iconModifier = Modifier
                .height(25.dp) // not all icons are square, just limit height?
                .padding(5.dp)
            stat?.let {
                if (it.hasSplit()) DualStatFill(it, textColor, testTag) else SingleStatFill(it, textColor, testTag)
                if (extraIconId != 0) Image(
                    painterResource(extraIconId),
                    null,
                    iconModifier.align(Alignment.TopStart),
                    colorFilter = if (showEVolt) ColorFilter.tint(textColor) else null,
                )
            }
            if (rightIconId != 0) {
                Icon(
                    painter = painterResource(id = rightIconId),
                    null,
                    iconModifier.align(Alignment.TopEnd),
                    tint = DynamicTheme.colors.panelStatForeground,
                )
            }
            progress?.let {
                LinearProgressIndicator(
                    { it },
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.1f),
                    progressColor, progressColor.copy(0.1f))
            }
        }
    }
}

// certain stat types use their unit as a label
@Composable private fun getStatLabel(stat: StatInfo): String {
    val label = stringResourceDefault(stat.labelId.value)
    val unit =  stringResourceDefault(stat.units.value)
    return if (stat.type.hasSecondary() || stat.type.isSecondary() || label.isEmpty()) unit else label
}

@Composable
private fun BoxScope.SingleStatFill(
    stat: StatInfo,
    color: Color,
    testTag: String,
) {
    Column(
        Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stat.text.value, Modifier.testTag(testTag + "Value"), color = color, style = DynamicTheme.typography.textStyle3)
        Text(getStatLabel(stat), color = color, style = DynamicTheme.typography.textStyle1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BoxScope.DualStatFill(
    stat: StatInfo,
    color: Color,
    testTag: String,
) {
    Column(
        Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(getStatLabel(stat), color = color, style = DynamicTheme.typography.textStyle1)
        Row {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if(stat.type.isMyZone()) stat.text.value else stat.splitText.value, Modifier.testTag(testTag + "Value"), color = color, style = DynamicTheme.typography.textStyle2)
                Text(stringResourceDefault(if(stat.type.isMyZone()) stat.units.value else stat.splitUnits.value), color = color, style = DynamicTheme.typography.textStyle1, textAlign = TextAlign.Center)
            }
            Box(
                Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxHeight(0.8f)
                    .width(spacing)
                    .background(statBorderColor))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (stat.type.isMyZone()) stat.splitText.value else stat.text.value, Modifier.testTag(testTag + "Value2"), color = color, style = DynamicTheme.typography.textStyle2)
                Text(stringResourceDefault(if(stat.type.isMyZone()) stat.splitUnits.value else stat.units.value), color = color, style = DynamicTheme.typography.textStyle1)
            }
        }
    }
}


@Composable
private fun GoalStep(
    modifier: Modifier,
    isIncrement: Boolean = true,
    onClick: () -> Unit = {},
    onSize: (size: DpSize) -> Unit = {},
) {
    // using a pressed action in place of normal click, must use same interaction source as button to sync visual effect
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    PressedAction(interactionSource, action = onClick)

    with(LocalDensity.current) {
        PanelButton(
            interactionSource = interactionSource,
            onClick = {}, // using PressedAction
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    val sizePixels = coordinates.size
                    onSize(DpSize(sizePixels.width.toDp(), sizePixels.height.toDp()))
                }
                .then(modifier)
        ) {
            Icon(
                modifier = Modifier.size(26.dp),
                painter = painterResource(id = if (isIncrement) R.drawable.ic_plus_white else R.drawable.ic_minus_white),
                tint = DynamicTheme.colors.panelGoalForeground,
                contentDescription = null
            )
        }
    }
}


@Composable
private fun AppBarButton(
    modifier: Modifier = Modifier,
    iconId: Int,
    onClick: () -> Unit = {},
) {
    Button(
        onClick,
        modifier
            .fillMaxSize(),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
    ) {
        Icon(
            modifier = modifier.size(40.dp),
            painter = painterResource(id = iconId),
            tint = Color.White,
            contentDescription = null,
        )
    }
}

@Composable
private fun AppBar(
    isFull: Boolean = false,
    appOptions: ExtraBarOptions? = null,
    onToggleFull: () -> Unit = {},
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(dimensionResource(R.dimen.app_buttons_bottom_bar_height)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appOptions != null) {
            if (appOptions.showBack) AppBarButton(
                Modifier
                    .testTag("BackButton")
                    .weight(1f), iconId = R.drawable.ic_back_button_white, onClick = onBack)
            if (appOptions.showRefresh) AppBarButton(
                Modifier
                    .testTag("RefreshButton")
                    .weight(1f), iconId = R.drawable.ic_refresh_white, onClick = onRefresh)
            if (appOptions.showResize) AppBarButton(
                Modifier
                    .testTag("ResizeButton")
                    .weight(1f), iconId = if (isFull) R.drawable.ic_shrink_white else R.drawable.ic_expand_white, onClick = onToggleFull)
        }
    }
}

//region PREVIEWS

@Preview(heightDp = 86, widthDp = 1024)
@Preview(heightDp = 86, widthDp = 1366)
@Preview(heightDp = 86, widthDp = 1920)
@Composable
private fun PreviewBottomBar() {
    BottomPanel(StatInfoCollection.dummyBikeStatInfoCollection())
}

@Preview(heightDp = 60, widthDp = 1920)
@Composable
private fun PreviewAppBar() {
    AppBar(appOptions = ExtraBarOptions(true, showBack = true, showRefresh = true, showStats = true))
}

//endregion PREVIEWS
