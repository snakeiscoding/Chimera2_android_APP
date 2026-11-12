package com.jht.widgets

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.enums.FrameType
import com.jht.enums.WorkoutGoalType
import com.jht.enums.WorkoutStage
import com.jht.interfaces.IStartStop
import com.jht.interfaces.models.UIBaseModel
import com.jht.interfaces.models.WorkoutGraphConfig
import com.jht.interfaces.models.WorkoutModel
import com.jht.data.DataPoint
import com.jht.data.GraphInfoChannel
import com.jht.data.INVALID_GRAPH_DATA
import com.jht.theme.DynamicColors
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.unit.UnitFormatter
import com.jht.widgets.helpers.getInjectionEntryPoint
import com.jht.widgets.helpers.stringResourceDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Stores info for a series of graph data */
private class GraphDataSeries(
    val legendName: Int,
    val legendColor: Color,
    val maxValue: Float,
    rawData: Array<DataPoint>,
    val indicatorId: Int = R.drawable.ic_stat_indicator,
    alwaysActive: Boolean = true,
    rawFuture: Array<DataPoint> = emptyArray(),
) {
    val data: Array<DataPoint>
    val futureData: Array<DataPoint>
    val isActive: Boolean
    init {
        // goal values apparently could include warm up phase in them, old code uses first data point for an offset
        val goalOffset = rawData.firstOrNull()?.goalValue ?: 0f
        data = Array(rawData.size) {
            rawData[it].copy(
                goalValue = rawData[it].goalValue - goalOffset,
            )
        }
        // Align the future to the same offset so it shares the x axis
        futureData = Array(rawFuture.size) {
            rawFuture[it].copy(
                goalValue = rawFuture[it].goalValue - goalOffset,
            )
        }
        isActive = alwaysActive || (data.isNotEmpty() && data.last().data > 0f)
    }
    companion object {
        fun fromHR(maxValue: Float, channel: GraphInfoChannel, themeColors: DynamicColors) = GraphDataSeries(R.string.heart_rate_tc, themeColors.heart5, maxValue, channel.historyDataPoints, R.drawable.ic_heart_rate_monitor_enabled, false, channel.futureDataPoints ?: emptyArray())
        fun fromSpeed(maxValue: Float, channel: GraphInfoChannel, themeColors: DynamicColors) = GraphDataSeries(R.string.speed_tc, themeColors.legend1, maxValue, channel.historyDataPoints, rawFuture = channel.futureDataPoints ?: emptyArray())
        fun fromResistance(maxValue: Float, channel: GraphInfoChannel, themeColors: DynamicColors) = GraphDataSeries(R.string.resistance_tc, themeColors.legend1, maxValue, channel.historyDataPoints, rawFuture = channel.futureDataPoints ?: emptyArray())
        fun fromIncline(maxValue: Float, channel: GraphInfoChannel, themeColors: DynamicColors) = GraphDataSeries(R.string.incline_tc, themeColors.legend2, maxValue, channel.historyDataPoints, rawFuture = channel.futureDataPoints ?: emptyArray())
        fun fromSpm(maxValue: Float, channel: GraphInfoChannel, themeColors: DynamicColors) = GraphDataSeries(R.string.spm_uc, themeColors.complete, maxValue, channel.historyDataPoints, rawFuture = channel.futureDataPoints ?: emptyArray())
        fun fromRpm(maxValue: Float, channel: GraphInfoChannel, themeColors: DynamicColors) = GraphDataSeries(R.string.rpm_uc, themeColors.coolDown, maxValue, channel.historyDataPoints, rawFuture = channel.futureDataPoints ?: emptyArray())
    }
}

/** Represents a vertical marker at a point along x-axis */
private class GraphMarker(
    val x: Float,
    val label: String,
)

/** Stores data for a whole graph which can contain multiple series of data */
private class GraphData(
    val series: List<GraphDataSeries> = listOf(),
    val xMarkers: List<GraphMarker> = listOf(),
)


/**
 * EXPERIMENTAL VALUES
 * With these values we're getting the functionality equivalents to the requirement stated
 */
// For open-ended (no-goal) workouts the plot width grows with elapsed time
// and eventually exceeds Compose's ~262,143 px Constraints limit (crash). The sliding window +
// width clamp live in ProfileGraphGeometry (unit-tested). Window length is driven by the single
// shared setting WorkoutGraphConfig.windowMinutes (also used by the View-based GraphSetup
// graph and the WorkoutData snapshot in the model).
private val graphWindowMs: Int get() = WorkoutGraphConfig.windowMs // sliding window span

private val indicatorSize = DpSize(24.dp, 24.dp) // should this be themed, or screen scaled?
private val guidelineColor = Color(0x4DFFFFFF) // this is also used in track app bottom stats, should probably replace with a theme color?
private val graphForeground = Color.White
private val graphBackground = Color.Black

private val futureLineColor = Color(0x90909090)
private val futureFillColor = Color(0xFF909090)

private const val MAX_HR = 200f
private const val MAX_RPM = 200f
private const val MAX_SPM = 200f

/** Private state tracking class for [ProfileGraphComponent] */
private class ProfileGraphState(
    context: Context, // should we retain context in case we need a string for marker labels?
    private val density: Density,
    private val themeColors: DynamicColors,
): IStartStop {
    private val workoutModel: WorkoutModel
    private val uiBaseModel: UIBaseModel

    val frameType: FrameType
    val speedMaxima: Float
    val inclineMaxima: Float
    val resistanceMaxima: Float

    var workoutStage by mutableStateOf(WorkoutStage.WARM_UP)
    var goalType = WorkoutGoalType.GOAL_TYPE_NONE
    var goalValue by mutableFloatStateOf(0f) // this has to be calculated from total and remaining (if no goal type, just tracking elapsed time)
    var goalTotal = 0f
    var goalRemaining = 0f

    // at the moment just drawing whole graph, but would it be more efficient to have a moving draw window?
    var graphData by mutableStateOf(GraphData()) // could get by with this not being state, but legend needs to update for isActive or maybe legend data should be separate?
    var overrideGraphWidthPx = 0                // if non-zero, forces a graph width, used to freeze graph when user is scrolling
    var graphWidthPx = 0                        // actual graph width used internally, but in Px (combination of plot and padding)
    var graphWidthDp by mutableStateOf(0.dp)    // graph width driving the compose (could be overridden for UI purposes, so might not match graphWidthPx)
    var viewWidthPx = 0                         // width of the view port holding the graph

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null         // conflation: at most one refresh in flight

    init {
        val injection = getInjectionEntryPoint(context)
        workoutModel = injection.workout()
        uiBaseModel = injection.uiBase()
        frameType = uiBaseModel.frameType
        speedMaxima = workoutModel.maxSpeedKph
        inclineMaxima = workoutModel.maxIncline
        resistanceMaxima = workoutModel.maxResistance
        updateDataPoints() // prime the data so legends are available immediately
    }
    override fun start() {
        restartGraph()
    }
    override fun stop() {
        scope.cancel()
        workoutModel.close()
        uiBaseModel.close()
    }
    private fun restartGraph() {
        workoutModel.close() // drop old listeners

        // don't use listenAndTrigger because trigger runs right away (not in handler) and it's calling itself (restartGraph)
        // which will register 2 callbacks
        goalType = workoutModel.goalType.value
        workoutModel.goalType.listen {
            // if graph was created before the workout started, the goal type may not be set yet
            if (it != goalType) {
                goalType = it
                restartGraph() // as listener fires on UI handler, this should not run until the current call is done
            }
        }
        workoutModel.workoutStage.listenAndTrigger { workoutStage = it }

        workoutModel.graphUpdateEvent.listen {
            // A notification arriving mid-refresh is dropped, not queued — the next ~1Hz
            // history notification repaints, so at most one second of staleness in
            // exchange for no event pile-up.
            if (refreshJob?.isActive != true) {
                refreshJob = scope.launch {
                    workoutModel.refreshGraphData() // suspends; cross-process I/O runs on IO
                    updateDataPoints()              // back on Main; reads the in-memory snapshot
                }
            }
        }
        if (goalType.isGoalNone) {
            workoutModel.timeElapsedMs.listen { time ->
                if (workoutStage == WorkoutStage.WORKOUT) { // graph services only track the regular stage
                    goalValue = time.toFloat()
                    updateGraphSize()
                }
            }
        } else {
            workoutModel.goalTotal.listenAndTrigger {
                if (workoutStage == WorkoutStage.WORKOUT) { // graph services only track the regular stage
                    goalTotal = it
                    goalValue = computeGoalValue()
                    updateDataPoints()
                }
            }
            workoutModel.goalRemaining.listenAndTrigger {
                if (workoutStage == WorkoutStage.WORKOUT) { // graph services only track the regular stage
                    goalRemaining = it
                    goalValue = computeGoalValue()
                }
            }
        }
    }
    /** Process changes in view port size */
    fun onViewPortSize(size: IntSize) {
        viewWidthPx = size.width
        updateGraphSize()
    }
    /** Processes detectHorizontalScrolls delta and returns a new scroll position if needed */
    fun onHorizontalScroll(scrollDelta: Float): Int? {
        if (!goalType.isGoalNone) {
            // do nothing
        } else if (scrollDelta < 0) {
            overrideGraphWidthPx = 0
            return 0
        } else if (overrideGraphWidthPx == 0 && graphWidthPx > viewWidthPx) {
            overrideGraphWidthPx = graphWidthPx
        }
        return null
    }
    private fun computeGoalValue(total: Float = goalTotal, remaining: Float = goalRemaining): Float {
        return minOf(maxOf(total - remaining, 0f), total)
    }
    fun computePxForX(x: Float): Float {
        return when (goalType) {
            // Offset by the sliding window start so x maps into [0, window]
            WorkoutGoalType.GOAL_TYPE_NONE -> ProfileGraphGeometry.xForTimeMs(x, goalValue, graphWindowMs)
            else -> x * (viewWidthPx / goalTotal) // x is in goal units, so convert from goal unit to px
        }
    }
    fun computePxForX(dataPoint: DataPoint): Float {
        return when (goalType) {
            WorkoutGoalType.GOAL_TYPE_NONE -> computePxForX(dataPoint.timeMs)
            else -> computePxForX(dataPoint.goalValue)
        }
    }
    private fun computeMarkers(): List<GraphMarker> {
        // There is a EVENT_DISTANCE_MARKERS_PARCELABLE that can provide distance markers for mile/km, seems to operate separate of regular graph event?
        // We might have to convert that to a DATA key if we want to use consistently across interface? Time scale is also seconds instead of ms, should it be consistent with elapsed?
        // Distance markers were not part of origin request, so maybe just stick to simple time markers on everything for now?
        val xMarkers = mutableListOf<GraphMarker>()
        when (goalType) {
            WorkoutGoalType.GOAL_TYPE_NONE -> {
                ProfileGraphGeometry.minuteMarkerTimesMs(goalValue.toInt(), graphWindowMs).forEach { x ->
                    xMarkers.add(GraphMarker(
                        computePxForX(x.toFloat()),
                        UnitFormatter.formatTimeMilliseconds(x),
                    ))
                }
            }
            WorkoutGoalType.GOAL_TYPE_TIME -> {
                val timeMs = goalValue.toInt()
                val stepMs = (goalTotal / 10).toInt() // make step 1/10 of the goal
                if (stepMs > 0) for (x in stepMs..timeMs step stepMs) { // do not draw marker at zero
                    xMarkers.add(GraphMarker(
                        computePxForX(x.toFloat()),
                        UnitFormatter.formatTimeMilliseconds(x),
                    ))
                }
            }
            else -> {}
        }
        return xMarkers
    }
    private fun updateGraphSize() {
        with (density) {
            if (goalType.isGoalNone) {
                // Window-capped + clamped width so it can never exceed Compose's Constraints limit
                graphWidthPx = ProfileGraphGeometry.plotWidthPx(
                    elapsedMs = goalValue,
                    windowMs = graphWindowMs,
                    indicatorCushionPx = indicatorSize.width.toPx() / 2, // cushion for the indicator
                )
                graphWidthDp = (if (overrideGraphWidthPx > 0) overrideGraphWidthPx else graphWidthPx).toDp() // use override if set
            } else {
                // if there is a goal, then graph is a fixed size
                graphWidthPx = viewWidthPx
                graphWidthDp = graphWidthPx.toDp()
            }
        }
    }
    private fun updateDataPoints() {
        val xMarkers = computeMarkers()
        // Pass the whole channel so both history and future flow through
        when (frameType) {
            FrameType.Treadmill -> {
                graphData = GraphData(listOf(
                    GraphDataSeries.fromHR(MAX_HR, workoutModel.heartGraphDataPoints.value, themeColors),
                    GraphDataSeries.fromSpeed(speedMaxima, workoutModel.speedGraphDataPoints.value, themeColors),
                    GraphDataSeries.fromIncline(inclineMaxima, workoutModel.inclineGraphDataPoints.value, themeColors),
                ), xMarkers)
            }
            FrameType.Elliptical,
            FrameType.Bike,
            FrameType.SpinBike -> {
                graphData = GraphData(listOf(
                    GraphDataSeries.fromHR(MAX_HR, workoutModel.heartGraphDataPoints.value, themeColors),
                    GraphDataSeries.fromResistance(resistanceMaxima, workoutModel.resistanceGraphDataPoints.value, themeColors),
                    GraphDataSeries.fromRpm(MAX_RPM, workoutModel.rpmGraphDataPoint.value, themeColors),
                ), xMarkers)
            }
            FrameType.Ascent -> {
                graphData = GraphData(listOf(
                    GraphDataSeries.fromHR(MAX_HR, workoutModel.heartGraphDataPoints.value, themeColors),
                    GraphDataSeries.fromResistance(resistanceMaxima, workoutModel.resistanceGraphDataPoints.value, themeColors),
                    GraphDataSeries.fromIncline(inclineMaxima, workoutModel.inclineGraphDataPoints.value, themeColors),
                    GraphDataSeries.fromRpm(MAX_RPM, workoutModel.rpmGraphDataPoint.value, themeColors),
                ), xMarkers)
            }
            FrameType.Stepper,
            FrameType.Climbmill -> {
                graphData = GraphData(listOf(
                    GraphDataSeries.fromHR(MAX_HR, workoutModel.heartGraphDataPoints.value, themeColors),
                    GraphDataSeries.fromResistance(resistanceMaxima, workoutModel.resistanceGraphDataPoints.value, themeColors),
                    GraphDataSeries.fromSpm(MAX_SPM, workoutModel.spmGraphDataPoint.value, themeColors),
                ), xMarkers)
            }
        }
    }
} // END CLASS: profile state class


/** Represents drawing layer of x axis markers */
@Composable
private fun MarkerGraph(
    modifier: Modifier = Modifier,
    markers: List<GraphMarker>,
) {
    val textMeasurer = rememberTextMeasurer()
    val textPadPx = 5f
    val textPadPx2 = textPadPx * 2
    val textStyle = TextStyle(
        fontSize = 12.sp.computeSize(),
        color = graphForeground, // should this be theme color or is graph fixed colors?
        textAlign = TextAlign.Center,
    )
    Canvas(
        modifier.graphicsLayer { clip = true },
    ) {
        val strokeWidth = 1f
        var previousX = 0f
        markers.forEach {
            val textLayoutResult = textMeasurer.measure(it.label, textStyle)
            val textCenter = textLayoutResult.size.center
            // center label if there is room, otherwise align it to the left (try to avoid last marker drawing off graph)
            val textX = if (it.x + textCenter.x + textPadPx > size.width) it.x - textLayoutResult.size.width else it.x - textCenter.x
            // maybe check if this x is greater than previous x to prevent overlap? add textPadPx to previousX to enforce equal text margin?
            if (textX > previousX + textPadPx) {
                // draw vertical line at x (leave space at top for label)
                drawLine(
                    color = textStyle.color,
                    start = Offset(it.x, textLayoutResult.size.height + textPadPx2), // provide for text padding at top and bottom of text
                    end = Offset(it.x, size.height),
                    strokeWidth = strokeWidth
                )
                // draw label box
                drawRectFilled(
                    colorFill = graphBackground,
                    colorStroke = textStyle.color,
                    strokeWidth = strokeWidth,
                    topLeft = Offset(textX - textPadPx, 0f),
                    size = Size(textLayoutResult.size.width + textPadPx2, textLayoutResult.size.height + textPadPx2),
                )
                // draw label
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(textX, textPadPx),
                )
                previousX = textX + textLayoutResult.size.width
            }
        }
    }
}
private fun DrawScope.drawRectFilled(
    colorFill: Color,
    colorStroke: Color,
    strokeWidth: Float,
    topLeft: Offset,
    size: Size,
) {
    // fill
    drawRect(colorFill, topLeft, size, style = Fill)
    // border
    drawRect(colorStroke, topLeft, size, style = Stroke(strokeWidth))
}


/** Represents drawing layer of a single data series */
@Composable
private fun LineGraph(
    state: ProfileGraphState,
    modifier: Modifier = Modifier,
    points: Array<DataPoint>,
    futurePoints: Array<DataPoint> = emptyArray(),
    maxPointValueY: Float,
    lineColor: Color,
    fillColor: Color,
    imageResourceId: Int,
    bitmapTintColor: Color,
    isActive: Boolean,
    workoutStage: WorkoutStage,
) {
    val painter = painterResource(imageResourceId)
    val colorEnd = DynamicTheme.colors.coolDown.copy(0.5f)
    val currentGoalValue = state.goalValue
    Canvas(
        modifier.graphicsLayer { clip = true },
    ) {
        // NOTE: x,y names refer to drawing coordinates, "point" names refer to data coordinates (drawing coordinates origin is top left)
        val strokeWidth = 2.dp.toPx()
        val graphWidth = size.width
        val graphHeight = size.height
        val imageSize = Size(indicatorSize.width.toPx(), indicatorSize.height.toPx())
        val imageCenter = imageSize.center
        val maxX = if (state.goalType.isGoalNone) graphWidth - (imageSize.width / 2) else state.computePxForX(currentGoalValue) // because of indicator, width is not same as max X value

        // A sample whose value is INVALID_GRAPH_DATA (e.g. heart rate with no strap) lifts the
        // pen: no line or fill is drawn across the invalid stretch, matching the old View graph's
        // GraphSeries INVALID_PIXEL behavior. Each run is a vertex list; the line path draws the
        // runs disjointly (moveTo between runs) and the fill closes each run to the baseline.
        // The sentinel is checked on EVERY series, which is safe because it is -Float.MAX_VALUE —
        // no real reading (negative decline inclines included) can ever equal it.
        val runs = mutableListOf<MutableList<Offset>>()
        if (points.isNotEmpty()) {
            var currentRun: MutableList<Offset>? = null
            var previousY = 0f
            points.forEachIndexed { i, point ->
                if (point.data == INVALID_GRAPH_DATA) {
                    currentRun = null // pen up; the next valid sample starts a new run
                    return@forEachIndexed
                }
                val x = state.computePxForX(point)
                val y = graphHeight - (point.data / maxPointValueY * graphHeight)
                val run = currentRun
                if (run == null) {
                    val newRun = mutableListOf<Offset>()
                    // Only rise from the baseline (0,0 -> first value) when this is the very first
                    // sample AND it is the actual workout start (timeMs ~= 0). Mid-window or
                    // post-gap starts begin at their real value.
                    if (i == 0 && point.timeMs <= 0f) newRun.add(Offset(x, graphHeight))
                    newRun.add(Offset(x, y))
                    runs.add(newRun)
                    currentRun = newRun
                } else {
                    run.add(Offset(x, previousY)) // flat to the change
                    run.add(Offset(x, y))         // step to the new value
                }
                previousY = y
            }
            // trailing extension to the cursor, only when the newest sample is valid
            if (points.last().data != INVALID_GRAPH_DATA && runs.isNotEmpty()) {
                runs.last().add(Offset(maxX, previousY))
            }
        }
        // Value Line Path: draw each run disjointly
        val linePath = Path().apply {
            runs.forEach { run ->
                run.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
            }
        }
        // Fill Area Path: close every run straight down to the baseline on both sides (closing
        // diagonally back to the run start would produce triangle fills).
        val fillPath = Path().apply {
            runs.forEach { run ->
                run.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
                lineTo(run.last().x, graphHeight)
                lineTo(run.first().x, graphHeight)
                close()
            }
        }
        // Fill Area Draw
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    fillColor.copy(alpha = 0.5f),
                    Color.Transparent
                ),
                endY = graphHeight
            ),
            style = Fill
        )
        // Value Line Draw
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
        // Drawn from the current cursor to the end of the workout. Only for goal workouts: with no
        // goal the graph width ends at the current time, so there is no room right of the cursor.
        if (futurePoints.isNotEmpty() && points.isNotEmpty() && !state.goalType.isGoalNone) {
            fun yOf(point: DataPoint) = graphHeight - (point.data / maxPointValueY * graphHeight)
            // The profile change already IN EFFECT at the cursor sets the start height. Without
            // this, the line would hold the last HISTORY value until the next change — which at
            // workout start is 0 (still ramping), making the future visibly droop to the baseline.
            val inEffect = futurePoints.lastOrNull { state.computePxForX(it) <= maxX }
            val startY = inEffect?.let { yOf(it) }
                ?: (graphHeight - (points.last().data / maxPointValueY * graphHeight))
            val upcoming = futurePoints.filter { state.computePxForX(it) > maxX }
            val futureLinePath = Path().apply {
                moveTo(maxX, startY) // start at the cursor, at the in-effect profile value
                var previousY = startY
                for (point in upcoming) {
                    val x = state.computePxForX(point)
                    val y = yOf(point)
                    lineTo(x, previousY)
                    lineTo(x, y)
                    previousY = y
                }
                lineTo(graphWidth, previousY) // hold the final value to the end of the workout
            }
            val futureFillPath = futureLinePath.copy().apply {
                // close straight down on both sides so the fill is the area under the future line
                lineTo(graphWidth, graphHeight)
                lineTo(maxX, graphHeight)
            }

            drawPath(
                path = futureFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        futureFillColor.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    endY = graphHeight
                ),
                style = Fill
            )
            drawPath(
                path = futureLinePath,
                color = futureLineColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Draw Indicator Image
        if (isActive && workoutStage == WorkoutStage.WORKOUT && points.isNotEmpty()) {
            val lastY = graphHeight - (points.last().data / maxPointValueY * graphHeight)
            with(painter) {
                translate(maxX - imageCenter.x, lastY - imageCenter.y) {
                    draw(
                        size = imageSize,
                        colorFilter = ColorFilter.tint(bitmapTintColor),
                    )
                }
            }
        }
        // Draw stop line
        if (workoutStage == WorkoutStage.COOL_DOWN) {
            drawLine(
                color = colorEnd,
                start = Offset(maxX, 0f),
                end = Offset(maxX, graphHeight),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}


/** Contains the graph view port and the graph itself */
@Composable
private fun ProfileGraphViewPort(
    state: ProfileGraphState,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // view port box
    Box(
        modifier = modifier
            .background(graphBackground)
            .onGloballyPositioned { coordinates ->
                state.onViewPortSize(coordinates.size)
            }
    ) {
        // horizontal guidelines
        Column(
            modifier = Modifier.matchParentSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(5) {
                HorizontalDivider(Modifier.fillMaxWidth(), color = guidelineColor)
            }
        }

        // graph box
        Box(
            modifier = Modifier
                .width(state.graphWidthDp)
                .horizontalScroll(scrollState, reverseScrolling = true) // reverse the scroll so the end is zero
                .pointerInput(Unit) {
                    // in emulator I sometimes see a rogue -0.5 when scrolling over, so maybe use a small threshold here
                    detectHorizontalScrolls(1f) { scrollDelta ->
                        state.onHorizontalScroll(scrollDelta)?.let {
                            coroutineScope.launch { scrollState.scrollTo(it) }
                        }
                    }
                }
                .testTag("ProfileGraph:GraphArea")
        ) {
            // Data series layers
            state.graphData.series.forEach {
                LineGraph(
                    state = state,
                    modifier = Modifier
                        .width(state.graphWidthDp)
                        .fillMaxHeight(),
                    points = it.data,
                    futurePoints = it.futureData,
                    maxPointValueY = it.maxValue,
                    lineColor = it.legendColor,
                    fillColor = it.legendColor,
                    imageResourceId = it.indicatorId,
                    bitmapTintColor = it.legendColor,
                    isActive = it.isActive,
                    workoutStage = state.workoutStage,
                )
            }
            // X Axis Maker layer
            MarkerGraph(
                modifier = Modifier
                    .width(state.graphWidthDp)
                    .fillMaxHeight(),
                markers = state.graphData.xMarkers,
            )
        }
    }
}

// Helper function to detect horizontal scroll direction
private suspend fun PointerInputScope.detectHorizontalScrolls(threshold: Float, onScroll: (delta: Float) -> Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val dragAmount = event.changes.firstOrNull()?.positionChange()?.x ?: 0f
            if (abs(dragAmount) > threshold) {
                onScroll(dragAmount)
            }
        }
    }
}

@Composable
private fun StatElement(
    text: String = "",
    textColor: Color = graphForeground,
    boxColor: Color = graphBackground,
    isActive: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val shape = RoundedCornerShape(3.dp)
        Box(
            Modifier
                .background(if (isActive) boxColor else Color.Transparent)
                .width(10.dp.computeSize())
                .height(25.dp.computeSize())
                .clip(shape)
                .border(BorderStroke(1.dp, boxColor), shape),
        )
        Text(text = text, style = DynamicTheme.typography.textStyle1, color = if (isActive) textColor else DynamicTheme.colors.gray5, fontSize = 16.sp.computeSize())
    }
}


/** Top level graph component featuring the plot and legend */
@Composable
fun ProfileGraphComponent(modifier: Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val themeColors = DynamicTheme.colors
    val state = remember { ProfileGraphState(context, density, themeColors) }
    LaunchedEffect(Unit) { state.start() }
    DisposableEffect(Unit) { onDispose { state.stop() } }

    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
                .align(Alignment.CenterVertically),
            verticalArrangement = Arrangement.SpaceAround
        ) {
            state.graphData.series.forEach { legend ->
                StatElement(text = stringResourceDefault(legend.legendName), boxColor = legend.legendColor, isActive = legend.isActive)
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            // graphing backend is presently geared toward the regular stage only?
            if (state.workoutStage == WorkoutStage.WORKOUT) ProfileGraphViewPort(state, Modifier.fillMaxSize())
        }
    }
}


@AllDevicePreviews
@Composable
private fun GraphPreview() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // is there a handy way to mock state for preview? or maybe better to see it on actual console

        Column {
            ProfileGraphComponent(modifier = Modifier.fillMaxSize(0.5f))
        }
    }
}