package com.jht.workoutselection.vortex.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jht.data.CustomWorkout
import com.jht.data.CustomWorkouts
import com.jht.data.WorkoutInput
import com.jht.data.WorkoutInputUi
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeFlatButton
import com.jht.theme.ThemeSelector
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.unit.UnitFormatter
import com.jht.widgets.CenterMinRow
import com.jht.widgets.ConfirmDialog
import com.jht.widgets.FavoriteIcon
import com.jht.widgets.PillboxGrid
import com.jht.widgets.StartWorkoutButton
import com.jht.widgets.fillScreenHeight
import com.jht.widgets.helpers.TabItem
import com.jht.workoutselection.CustomWorkoutEditable
import com.jht.workoutselection.CustomWorkoutHandler
import com.jht.workoutselection.R
import java.math.RoundingMode
import kotlin.math.roundToInt

private val fillColor @Composable get() = DynamicTheme.colors.primary
private val emptyColor @Composable get() = DynamicTheme.colors.gray1
private val lineColor @Composable get() = DynamicTheme.colors.stop
private const val lineThicknessPx = 3f
private const val lineRadiusPx = 5f
private const val legendAspectRatio = 3f

/** Root content of custom workout tab */
@Composable
internal fun TabCustomWorkout(
    workoutInputs: List<WorkoutInputUi>,
    customWorkouts: SnapshotStateList<CustomWorkoutEditable>,
    selected: CustomWorkoutEditable? = null,
    onSelect: (CustomWorkoutEditable) -> Unit = {},
    onAddWorkout: () -> Unit = {},
    onDeleteWorkout: () -> Unit = {},
    onStartCustomWorkout: () -> Unit = {},
) {
    Row(Modifier.fillMaxSize()) {
        // LEFT SIDE PANEL
        CustomWorkoutList(
            Modifier
                .fillMaxHeight()
                .aspectRatio(0.3f),
            customWorkouts,
            selected,
            onSelect,
            onAddWorkout,
            onDeleteWorkout
        )

        // RIGHT SIDE - custom workout editing
        Column(
            Modifier.weight(1f)
        ) {
            selected?.let {workout ->
                CustomWorkoutGraph(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(DynamicTheme.colors.gray2),
                    workoutInputs, workout,
                )
                CustomWorkoutEdit(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(DynamicTheme.colors.text2),
                    workoutInputs, workout, onStartCustomWorkout,
                )
            }
        }
    }
}


@Composable
private fun CustomWorkoutList(
    modifier: Modifier,
    customWorkouts: SnapshotStateList<CustomWorkoutEditable>,
    selected: CustomWorkoutEditable?,
    onSelect: (CustomWorkoutEditable) -> Unit,
    onAddWorkout: () -> Unit,
    onDeleteWorkout: () -> Unit,
) {
    val showWorkoutDelete = remember { mutableStateOf(false) }
    ConfirmDialog(R.string.delete_custom_workout_tc, R.string.delete_custom_workout_pg, showWorkoutDelete) {
        onDeleteWorkout()
    }
    Column(
        modifier = modifier
            .background(DynamicTheme.colors.gray1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // LEFT SIDE - HEADING
        Text(
            stringResource(R.string.custom_workouts_tc),
            Modifier.padding(20.dp),
            color = DynamicTheme.colors.text1,
            style = DynamicTheme.typography.textStyle2,
        )
        // LEFT SIDE - LIST of custom workouts
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(customWorkouts, key = {it.originalData.uuid}) { item ->
                val isSelected = (selected == item) // compare instance or UUID?
                ThemeSelector(
                    selected = isSelected,
                    testTag = "customWorkout${item.originalData.uuid}",
                    color = DynamicTheme.colors.gray2,
                    modifier = Modifier.height(IntrinsicSize.Min),
                    contentModifier = Modifier.padding(10.dp),
                    onClick = { onSelect(item) },
                ) {
                    Text(
                        displayTitle(item),
                        Modifier
                            .weight(1f)
                            .padding(5.dp),
                        color = DynamicTheme.colors.text1,
                        style = DynamicTheme.typography.textStyle1,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isSelected) Icon(
                        painterResource(R.drawable.ic_trash_can),
                        null,
                        Modifier
                            .size(40.dp) // hard to size this proportionally since item can be bigger with bigger text?
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clickable { showWorkoutDelete.value = true },
                        DynamicTheme.colors.text1,
                    )
                }
            }
        }
        // LEFT SIDE - BOTTOM ADD BUTTON
        Box(
            Modifier
                .background(DynamicTheme.colors.gray2)
                .fillMaxWidth()
        ) {
            Button(
                onClick = onAddWorkout,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(2.dp, DynamicTheme.colors.text1),
            ) {
                CenterMinRow {
                    Icon(
                        painterResource(R.drawable.ic_plus_white_v2),
                        null,
                        Modifier.fillMaxHeight(0.7f),
                        DynamicTheme.colors.text1,
                    )
                    Text(
                        stringResource(R.string.new_tc), // new_exercise_tc ?
                        color = DynamicTheme.colors.text1,
                        style = DynamicTheme.typography.textStyle2
                    )
                }
            }
        }
    }
}


/** Top half of custom tab right section, contains the title and segment graph for current custom workout */
@Composable
private fun CustomWorkoutGraph(
    modifier: Modifier,
    workoutInputs: List<WorkoutInput>,
    currentWorkout: CustomWorkoutEditable,
) {
    val nonTimeInputs = workoutInputs.filter { it.identifier != ConsoleWorkoutVariables.TIME_GOAL_START } // should interface WorkoutInput provide a generic "isTime" helper to avoid specific values?
    val fillInput = if (nonTimeInputs.isNotEmpty()) nonTimeInputs[0] else null
    val lineInput = if (nonTimeInputs.size > 1)  nonTimeInputs[1] else null
    Column(
        modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Title Row
        GraphTitle(currentWorkout)
        // Graph Header
        Spacer(modifier = Modifier.fillScreenHeight(0.04f))
        GraphHeader(
            fillInput, lineInput,
            currentWorkout
        )
        // Graph Body
        GraphBody(
            Modifier.weight(1f),
            fillInput, lineInput,
            currentWorkout,
        )
    }
}

@Composable
private fun GraphTitle(
    currentWorkout: CustomWorkoutEditable,
) {
    val defaultTitle = stringResource(R.string.my_workout_tc)
    val adjustedTitle = displayTitle(currentWorkout)
    val isEdit = remember(currentWorkout) { mutableStateOf(false) }
    var stateTitle by remember(currentWorkout) { mutableStateOf(TextFieldValue(adjustedTitle, TextRange(adjustedTitle.length))) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val done = {
        isEdit.value = false
        focusManager.clearFocus()
        currentWorkout.title.value = stateTitle.text
        if (stateTitle.text.isEmpty()) stateTitle = TextFieldValue(defaultTitle, TextRange(defaultTitle.length))
    }
    CenterMinRow(Modifier.fillMaxWidth()) {
        // Fav Icon for retail
        FavoriteIcon(
            Modifier
                .fillMaxHeight(0.5f)
                .aspectRatio(1f),
        )
        // Title (editable)
        BasicTextField(
            stateTitle,
            { if (it.text.length < 30) stateTitle = it }, // old UI has 30 for max length
            Modifier
                //.width(IntrinsicSize.Min) // we can make it min width but then it seems to scroll when editing?
                .padding(horizontal = 10.dp)
                .focusRequester(focusRequester),
            textStyle = DynamicTheme.typography.textStyle3.copy(color = DynamicTheme.colors.text1),
            cursorBrush = SolidColor(DynamicTheme.colors.primary),
            singleLine = true,
            readOnly = !isEdit.value,
            keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { done() }),
        )
        LaunchedEffect(isEdit.value) {
            if (isEdit.value) focusRequester.requestFocus() else focusManager.clearFocus() // launching request so it fires after composing
        }
        DisposableEffect(Unit) { onDispose { focusManager.clearFocus() } } // if we are suddenly disposed, the launch might be cancelled
        // Edit Icon
        Icon(
            painterResource(id = R.drawable.ic_edit_box),
            null,
            Modifier
                .fillMaxHeight(0.5f)
                .aspectRatio(1f)
                .clickable {
                    if (isEdit.value) done()
                    else isEdit.value = true
                },
            DynamicTheme.colors.text1,
        )
    }
}

@Composable
private fun GraphHeader(
    fillInput: WorkoutInput?,
    lineInput: WorkoutInput?,
    currentWorkout: CustomWorkoutEditable,
) {
    val totalMinutes = currentWorkout.intervals.sumOf { it.data[ConsoleWorkoutVariables.TIME_GOAL_START]?.value?.toDouble() ?: 0.0 }
    Box(Modifier.fillMaxWidth()) {
        // fill legend
        fillInput?.let {
            CenterMinRow(Modifier.align(Alignment.CenterStart)) {
                Box(
                    Modifier
                        .fillMaxHeight(0.8f)
                        .aspectRatio(legendAspectRatio)
                        .background(fillColor))
                Spacer(Modifier.size(10.dp))
                Text(it.label, color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle1)
            }
        }
        // total time
        CenterMinRow(Modifier.align(Alignment.Center)) {
            Icon(
                painterResource(R.drawable.ic_time_total_white),
                null,
                Modifier.fillMaxHeight(),
                DynamicTheme.colors.text1,
            )
            Spacer(Modifier.size(10.dp))
            Text(
                UnitFormatter.formatTimeSeconds((totalMinutes * 60).roundToInt()),
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle1,
            )
        }
        // line legend
        lineInput?.let {
            CenterMinRow(Modifier.align(Alignment.CenterEnd)) {
                Text(it.label, color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle1)
                Spacer(Modifier.size(10.dp))
                Box(
                    Modifier
                        .fillMaxHeight(0.8f)
                        .aspectRatio(legendAspectRatio)
                        .background(lineColor))
            }
        }
    }
}

@Composable
private fun GraphBody(
    modifier: Modifier,
    fillInput: WorkoutInput?,
    lineInput: WorkoutInput?,
    currentWorkout: CustomWorkoutEditable,
) {
    // bars and space between bars
    val spaceWeight = 0.12f
    val barWeight = 1f
    // axis labels and arrow buttons
    val sideBarWeight = 1f
    val fillValues = fillInput?.let { currentWorkout.getIntervalsFor(it) }
    val lineValues = lineInput?.let { currentWorkout.getIntervalsFor(it) }
    // column counts
    val shownColumns = 12
    val maxColumns = 20
    val usedColumns = minOf(maxColumns, minOf(fillValues?.size ?: Int.MAX_VALUE, lineValues?.size ?: Int.MAX_VALUE))
    val startingColumn = currentWorkout.uiStartingColumn
    Column(modifier) {
        // Row for axis labels and graph bars
        CenterMinRow(Modifier.weight(1f)) {
            val spacerModifier = Modifier
                .weight(spaceWeight)
                .fillMaxHeight()
            val barModifier = Modifier
                .weight(barWeight)
                .fillMaxHeight()
            val sideBarModifier = Modifier
                .weight(sideBarWeight)
                .fillMaxHeight()
            GraphAxisLabel(sideBarModifier, fillInput)
            var previousLineRatio: Float? = null
            repeat(shownColumns) {
                val intervalIndex = startingColumn.intValue + it
                val currentLineRatio = if (lineValues != null && intervalIndex < lineValues.size) normalize(lineValues[intervalIndex], lineInput) else null
                val currentFillRatio = if (fillValues != null && intervalIndex < fillValues.size) normalize(fillValues[intervalIndex], fillInput) else null
                GraphBarSpace(spacerModifier, leftRatio0to1 = previousLineRatio, rightRatio0to1 = currentLineRatio)
                GraphBarFill(
                    barModifier.clickableNoEffect { if (intervalIndex < usedColumns) currentWorkout.selectInterval(intervalIndex) },
                    fillRatio0to1 = currentFillRatio, lineRatio0to1 = currentLineRatio,
                )
                previousLineRatio = currentLineRatio
            }
            GraphBarSpace(spacerModifier, leftRatio0to1 = previousLineRatio, rightRatio0to1 = null)
            GraphAxisLabel(sideBarModifier, lineInput)
        }
        Spacer(Modifier.size(5.dp))
        // Row for interval navigation arrows and interval select/add buttons
        CenterMinRow {
            val spacerModifier = Modifier.weight(spaceWeight)
            ArrowButton(R.drawable.ic_previous_arrow, Modifier.weight(sideBarWeight)) {
                val newIndex = currentWorkout.previousInterval()
                if (newIndex < startingColumn.intValue) startingColumn.intValue = newIndex
            }
            repeat(shownColumns) {
                val intervalIndex = startingColumn.intValue + it
                Spacer(spacerModifier)
                if (intervalIndex < usedColumns) GraphButton( // select
                    intervalIndex,
                    Modifier.weight(barWeight),
                    (intervalIndex == currentWorkout.selectedInterval.intValue),
                    onClick = currentWorkout::selectInterval,
                )
                else if (intervalIndex == usedColumns && intervalIndex < maxColumns) GraphButton( // add
                    intervalIndex,
                    Modifier.weight(barWeight),
                    null,
                    onClick = {
                        currentWorkout.addInterval()
                        if (intervalIndex + 1 == startingColumn.intValue + shownColumns && currentWorkout.intervals.size < maxColumns) ++startingColumn.intValue
                    },
                )
                else GraphButton( // empty
                    -1,
                    Modifier.weight(barWeight),
                )
            }
            Spacer(spacerModifier)
            ArrowButton(R.drawable.ic_next_arrow, Modifier.weight(sideBarWeight)) {
                val addButtonOffset = if (usedColumns < maxColumns) -1 else 0
                val newIndex = currentWorkout.nextInterval()
                if (newIndex >= startingColumn.intValue + shownColumns + addButtonOffset) ++startingColumn.intValue
            }
        }
    }
}

@Composable
private fun GraphAxisLabel(
    modifier: Modifier,
    workoutInput: WorkoutInput?,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        workoutInput?.let {input->
            // math taken from old CustomWorkoutFragment.java
            val axisLabels = listOf(
                input.max,
                (input.max * 0.66 / input.stepSize).roundToInt() * input.stepSize,
                (input.max * 0.33 / input.stepSize).roundToInt() * input.stepSize,
                input.min,
            )
            axisLabels.forEach {
                Text(
                    UnitFormatter.formatDecimal(input.precision, it, RoundingMode.HALF_UP),
                    color = DynamicTheme.colors.text1,
                    style = DynamicTheme.typography.textStyle1,
                )
            }
        }
    }
}

@Composable
private fun GraphBarFill(
    modifier: Modifier,
    fillRatio0to1: Float?,
    lineRatio0to1: Float?,
) {
    // draws the filled bar and/or cross line if needed
    val fill = fillColor
    val line = lineColor
    val empty = emptyColor
    Canvas(
        modifier.background(DynamicTheme.colors.gray4) // this is active background fill
    ) {
        fillRatio0to1?.let {
            val fillSize = Size(size.width, size.height * fillRatio0to1)
            val fillOffset = Offset(0f, size.height - fillSize.height)
            // draw fill
            drawRect(fill, fillOffset, fillSize)
        }
        lineRatio0to1?.let {
            val lineY = size.height - size.height * lineRatio0to1
            // draw line
            drawLine(line, Offset(0f, lineY), Offset(size.width, lineY), lineThicknessPx)
        }
        if (lineRatio0to1 == null && fillRatio0to1 == null) {
            drawRect(empty, Offset.Zero, size) // draw empty fill
        }
    }
}
@Composable
private fun GraphBarSpace(
    modifier: Modifier,
    leftRatio0to1: Float?,
    rightRatio0to1: Float?,
) {
    // draw the in-between space
    // nothing if nothing, line if same ratio, dot capped if only one, transition if both
    val color = lineColor
    val hasTransition = leftRatio0to1 != rightRatio0to1
    Canvas(
        modifier.zIndex(2f), // use zIndex to ensure spaces are drawn above the bars
    ) {
        val middleX = size.center.x
        leftRatio0to1?.let {
            // draw left line
            val leftY = size.height - size.height * leftRatio0to1
            drawLine(color, Offset(0f, leftY), Offset(middleX, leftY), lineThicknessPx)
            if (hasTransition) {
                // draw left dot
                drawCircle(color, lineRadiusPx, Offset(middleX, leftY))
                if (rightRatio0to1 != null) {
                    // draw line between dots
                    val rightY = size.height - size.height * rightRatio0to1
                    drawLine(color, Offset(middleX, leftY), Offset(middleX, rightY), lineThicknessPx)
                }
            }
        }
        rightRatio0to1?.let {
            // draw right line
            val rightY = size.height - size.height * rightRatio0to1
            drawLine(color, Offset(middleX, rightY), Offset(size.width, rightY), lineThicknessPx)
            if (hasTransition) {
                // draw right dot
                drawCircle(color, lineRadiusPx, Offset(middleX, rightY))
            }
        }
    }
}


/** Bottom half of custom tab right section, contains the segment editing and start button */
@Composable
private fun CustomWorkoutEdit(
    modifier: Modifier,
    workoutInputs: List<WorkoutInputUi>,
    currentWorkout: CustomWorkoutEditable,
    onStartCustomWorkout: () -> Unit,
) {
    val currentInterval = currentWorkout.getInterval()
    val showIntervalDelete = remember { mutableStateOf(false) }
    ConfirmDialog(R.string.delete_segment_tc, R.string.delete_segment_pg, showIntervalDelete) {
        currentWorkout.deleteInterval()
    }
    Column(modifier.padding(20.dp)) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()) {
            // pill boxes
            if (currentInterval != null){
                //Overwrite the workout input mutable states for the current interval
                workoutInputs.forEach { input->
                    currentInterval.data[input.identifier]?.let{
                        input.currentValue = it
                    }
                }

                // ensure the grid isn't skipped holding stale states
                key(currentWorkout, currentWorkout.selectedInterval.intValue) {
                    PillboxGrid(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        workoutInputList = workoutInputs,
                        rows = 2,
                        columns = 2,
                        labelPrefix = stringResource(R.string.segment_tc) + " ${currentWorkout.selectedInterval.intValue + 1} ",
                    )
                }
            }
            Spacer(Modifier.weight(0.1f))
            // segment buttons
            Column(
                Modifier
                    .width(IntrinsicSize.Min)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SegmentButton(
                    R.drawable.ic_trash_can, stringResource(R.string.delete_tc),
                    Modifier
                        .testTag("CustomWorkoutDeleteInterval")
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = { if (currentWorkout.canDeleteInterval()) showIntervalDelete.value = true },
                )
                SegmentButton(
                    R.drawable.ic_refresh_white, stringResource(R.string.reset_tc),
                    Modifier
                        .testTag("CustomWorkoutResetInterval")
                        .fillMaxWidth()
                        .weight(1f),
                    onClick = { currentWorkout.refreshInterval() },
                )
                Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.weight(0.1f))
        Row(
            Modifier
                .weight(0.3f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            /* CGC-15350 "include cooldown" is still under discussion, feature was added back in retail
            // cool down
            ThemeCheckBox(
                checked = currentWorkout.hasCoolDown.value,
                onChecked = {currentWorkout.hasCoolDown.value = it},
                Modifier.testTag("CustomWorkoutCoolDownCheckBox"),
            ) {
                Text(
                    stringResource(R.string.include_cooldown_tc),
                    Modifier.padding(10.dp),
                    color = DynamicTheme.colors.text1,
                    style = DynamicTheme.typography.textStyle2,
                )
            }*/
            Spacer(Modifier.weight(1f))
            // start
            StartWorkoutButton(
                onClick = onStartCustomWorkout,
                Modifier.testTag("CustomWorkout:Start:Button"),
                fullWidthText = false,
            )
        }
    }
}


//region parts and helpers
private fun normalize(value: Float, workoutInput: WorkoutInput?): Float {
    if (workoutInput == null) return 0f
    val min = workoutInput.min
    val max = workoutInput.max
    val calc = (value - min) / (max - min)
    return if (calc < 0) 0f else if (calc > 1) 1f else calc
}

// not sure if this should be up to the UI or if model should provide default title? the data can be empty but UI inserts a name?
@Composable
private fun displayTitle(workout: CustomWorkoutEditable): String = workout.title.value.ifEmpty { stringResource(R.string.my_workout_tc) }

@Composable
private fun SegmentButton(
    iconId: Int,
    text: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    ThemeFlatButton(onClick, modifier) {
        Row(
            Modifier.width(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconId != 0) Icon(painterResource(iconId), null,
                Modifier
                    .fillMaxHeight(0.8f)
                    .padding(horizontal = 10.dp.computeSize(), vertical = 0.dp),
                DynamicTheme.colors.text1)
            Text(
                text,
                Modifier.requiredWidth(IntrinsicSize.Max),
                fontSize = 20.sp.computeSize(),
                color = DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle1,
            )
        }
    }
}

@Composable
private fun GraphButton(
    segmentIndex: Int, // negative if empty
    modifier: Modifier,
    selected: Boolean? = false, // null if add button
    onClick: (Int) -> Unit = {},
) {
    val isEmpty = segmentIndex < 0
    val isAdd = selected == null
    val isSelected = selected == true
    val color =
        if (isEmpty) emptyColor // EMPTY
        else if (isAdd) DynamicTheme.colors.play // ADD
        else if (isSelected) DynamicTheme.colors.text2 // SELECTED
        else DynamicTheme.colors.background2 // UNSELECTED
    Button(
        { onClick(segmentIndex) },
        modifier.aspectRatio(1f),
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = color),
        contentPadding = PaddingValues(5.dp),
        enabled = !isEmpty,
    ) {
        if (isAdd) {
            Icon(painterResource(R.drawable.ic_plus_white_v2), null, Modifier.testTag("CustomWorkout:AddInterval:Icon"), DynamicTheme.colors.text1)
        } else if (!isEmpty) {
            val segmentNumber = (segmentIndex + 1)
            Text(
                UnitFormatter.formatInt(segmentNumber),
                color = if (isSelected) DynamicTheme.colors.primary else DynamicTheme.colors.text1,
                style = DynamicTheme.typography.textStyle3
            )
        }
    }
}

@Composable
private fun ArrowButton(
    iconId: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick,
        modifier,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
    ) {
        Icon(painterResource(iconId), null, Modifier.fillMaxHeight(), DynamicTheme.colors.text1)
    }
}
//endregion parts and helpers


//region Previews
@Composable private fun dummyWorkouts() = CustomWorkouts(
    workoutInputs = listOf(
        WorkoutInputUi(
            WorkoutInput(
            0f,
            15f,
            0f,
            0.5f,
            1,
            stringResource(R.string.incline_tc),
            "%",
            identifier = "START_INCLINE",
            ),
            currentValue = remember { mutableFloatStateOf(0.5f) },
        ),
        WorkoutInputUi(
            WorkoutInput(
            0.5f,
            12.5f,
            0f,
            0.1f,
            1,
            stringResource(R.string.speed_tc),
            stringResource(R.string.mph_uc),
            identifier = "START_SPEED"
            ),
            currentValue = remember { mutableFloatStateOf(0f) },
        ),
        WorkoutInputUi(
            WorkoutInput(
            1f,
            480f,
            5f,
            1f,
            0,
            stringResource(R.string.time_tc),
            stringResource(R.string.minutes_lc),
            identifier = "TIME_GOAL_START"
            ),
            currentValue = remember { mutableFloatStateOf(5f) },
        ),
    ),
    workouts = mutableListOf(
        CustomWorkout(
            title = "test1",
            intervalData = mapOf(
                Pair("START_SPEED", listOf(5f, 10f, 12f)),
                Pair("START_INCLINE", listOf(0.5f, 1.5f, 5.0f)),
                Pair("TIME_GOAL_START", listOf(5f, 5f, 5f)),
            )
        ),
        CustomWorkout(
            title = "test a long title string for workout",
            intervalData = mapOf(
                Pair("START_SPEED", listOf(5f, 10f, 15f)),
                Pair("START_INCLINE", listOf(0.5f, 1.5f, 5.0f)),
                Pair("TIME_GOAL_START", listOf(5f, 5f, 5f)),
            )
        )
    )
)

@Preview
@Composable
private fun PreviewGraphBars() {
    val spaceModifier = Modifier.size(10.dp, 60.dp)
    val fillModifier = Modifier.size(20.dp, 60.dp)
    Row {
        GraphBarSpace(
            spaceModifier,
            null,
            0.1f,
        )
        GraphBarFill(
            fillModifier,
            0.25f,
            0.1f,
        )
        GraphBarSpace(
            spaceModifier,
            0.1f,
            0.8f,
        )
        GraphBarFill(
            fillModifier,
            0.9f,
            0.8f,
        )
        GraphBarSpace(
            spaceModifier,
            0.8f,
            null,
        )
        GraphBarFill(
            fillModifier,
            null,
            null,
        )
    }
}

@AllDevicePreviews
@Composable
private fun PreviewTabCustomWorkout() {
    val workoutData = dummyWorkouts()
    val handler = remember {
        CustomWorkoutHandler().copyFrom(workoutData)
    }
    TabCustomWorkout(
        workoutInputs = handler.workoutInputs,
        customWorkouts = handler.customWorkouts,
        selected = handler.selectedWorkout.value,
    )
}
//endregion Previews
