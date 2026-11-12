package com.jht.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.jht.data.WorkoutInputUi
import com.jht.data.WorkoutInput
import com.jht.enums.ConsoleWorkoutVariables
import com.jht.enums.Gender
import com.jht.mock.interfaces.models.MockWorkoutCatalogModel
import com.jht.theme.DynamicTheme
import com.jht.ui.Device15inPreviews
import com.jht.ui.computeSize
import com.jht.ui.testTagJoin
import com.jht.widgets.numpad.NumPadDisplay
import kotlinx.coroutines.delay

//region PREVIEWS
private val testOverride @Composable get() = PillBoxDefaults.style(
    borderStroke = BorderStroke(5.dp, Color.Gray),
    dividerStroke = BorderStroke(1.dp, Color.Transparent),
    shape = RectangleShape,
    buttonBackground = Color.White,
    buttonForeground = Color.Black,
    valueIconId = R.drawable.ic_edit_box,
)

@Device15inPreviews
@Composable
private fun PreviewPillBox() {
    DynamicTheme {
        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
        ) {
            val defaultValue = remember { mutableFloatStateOf(2.5f) }
            val modifier = Modifier.size(400.dp.computeSize(), 150.dp.computeSize())
            // default style
            PillBox(
                modifier = modifier,
                pillboxValue = defaultValue,
                titleLabel = "Speed (mph)",
                unitLabel = "mph",
                stepValue = .1f,
                decimalPlaces = 1,
                min = .1f,
                max = 10f
            )
            // override style
            PillBox(
                modifier = modifier,
                pillboxValue = defaultValue,
                titleLabel = "Speed (mph)",
                unitLabel = "mph",
                stepValue = .1f,
                decimalPlaces = 1,
                min = .1f,
                max = 10f,
                style = testOverride,
            )
        }
        //val defaultValue = remember { mutableStateOf(24.5f) }
        //PillBox(modifier = Modifier.size(width = 1000.dp.computeSize(), height = 250.dp.computeSize()), pillboxValue = defaultValue, incrementValue = .1f, min = 0f, max = 360f, decimalPlaces = 1)
    }
}

@Device15inPreviews
@Composable
private fun PreviewPillBoxGender() {
    DynamicTheme {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
        ) {
            PillBoxGender(
                modifier = Modifier.size(400.dp.computeSize(), 150.dp.computeSize()),
            )
        }
    }
}

@Device15inPreviews
@Composable
private fun PreviewPillBoxGrid(){
    val mock = MockWorkoutCatalogModel()
    DynamicTheme {
        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
        ) {
            val modifier = Modifier
                .weight(1f)
                .fillMaxWidth(0.8f)
            // default style
            PillboxGrid(
                modifier,
                workoutInputList = mock.getWorkoutInputsForPreview(),
                /*gridModifier = Modifier,
                pillBoxModifier = Modifier.size(400.dp.computeSize(), 150.dp.computeSize())*/
            )
            // override style
            PillboxGrid(
                modifier,
                workoutInputList = mock.getWorkoutInputsForPreview(),
                style = testOverride,
            )
        }
    }
}
//endregion PREVIEWS


/** A class containing style parameters for [PillBox]. */
@Immutable
data class PillBoxStyle(
    /** Styles the whole outer border */
    val borderStroke: BorderStroke,
    /** Styles the border between buttons and value area. */
    val dividerStroke: BorderStroke,
    /** Styles the whole outer shape */
    val shape: Shape,
    /** Styles the label text */
    val labelTextStyle: TextStyle,
    /** Styles the background of the value area */
    val valueBackground: Color, // tempted to use Brush where possible, but not all elements will take a brush and cannot get a color out of all brush types
    /** Styles the value text */
    val valueTextStyle: TextStyle,
    /** Specifies the icon for value area, if zero it is omitted */
    val valueIconId: Int,
    /** Styles the percentage bar, if enabled */
    val valueBar: Color,
    /** Styles the background of the buttons */
    val buttonBackground: Color,
    /** Styles the tint of the button icons */
    val buttonForeground: Color,
)

/** Singleton of [PillBox] defaults. Inspired by material ButtonDefaults. */
object PillBoxDefaults {
    /** Copy construction of [PillBoxStyle] using the default values as a source. */
    @Composable
    fun style(
        borderStroke: BorderStroke = DynamicTheme.borders.pillbox.borderStroke(DynamicTheme.colors.gradient4.vertical()),
        // by default only use a divider if the buttons and value shared a background
        dividerStroke: BorderStroke = if (DynamicTheme.colors.pillboxValueBackground == DynamicTheme.colors.pillboxButtonBackground) borderStroke else BorderStroke(1.dp, Color.Unspecified),
        shape: Shape = MaterialTheme.shapes.medium,
        labelTextStyle: TextStyle = DynamicTheme.typography.textStyle3.copy(color = DynamicTheme.colors.text1),
        valueBackground: Color = DynamicTheme.colors.pillboxValueBackground,
        valueTextStyle: TextStyle = DynamicTheme.typography.textStyle3.copy(color = DynamicTheme.colors.pillboxValueForeground),
        valueIconId: Int = R.drawable.ic_keypad_black,
        valueBar: Color = DynamicTheme.colors.primary,
        buttonBackground: Color = DynamicTheme.colors.pillboxButtonBackground,
        buttonForeground: Color = DynamicTheme.colors.pillboxButtonForeground,
    ): PillBoxStyle = PillBoxStyle(
        borderStroke,
        dividerStroke,
        shape,
        labelTextStyle,
        valueBackground,
        valueTextStyle,
        valueIconId,
        valueBar,
        buttonBackground,
        buttonForeground,
    )
}

@Composable
fun PillBox (
    modifier: Modifier = Modifier,
    pillboxValue: MutableState<Float>,
    titleLabel: String = "",
    unitLabel: String = "",
    stepValue: Float = 1f,
    decimalPlaces: Int = 0,
    displayType: NumPadDisplay = NumPadDisplay.Number,
    min: Float = 0f,
    max: Float = displayType.getMax(),
    usePercentageBar: Boolean = false,
    forceOnlyStepValues: Boolean = false, // for keypad attached to pillbox
    testTag: String = "",
    style: PillBoxStyle = PillBoxDefaults.style(),
){
    // LOGIC

    //time calculation ( in if statement so no errors when not using it )
    val displayString = displayType.format(pillboxValue.value, decimalPlaces)
    // button pushing and holding
    val minusBtnInteractionSource = remember { MutableInteractionSource() }
    val isMinusBtnPressed by minusBtnInteractionSource.collectIsPressedAsState()
    val plusBtnInteractionSource = remember { MutableInteractionSource() }
    val isPlusBtnPressed by plusBtnInteractionSource.collectIsPressedAsState()

    val numPadVisible = remember { mutableStateOf(false) }

    // UI

    NumPadEntryPopup(
        numPadVisible = numPadVisible,
        numPadValue = pillboxValue, // default number in box, since mutable will update
        decimalPlaces = decimalPlaces,
        min = min,
        max = max,
        displayType = displayType,
        usePercentageBar = usePercentageBar,
        unitLabel = unitLabel,
        forceOnlyStepValues = forceOnlyStepValues,
        stepValue = stepValue
    )

    PillBoxContainer(
        titleLabel,
        modifier,
        style,
    ) {
        // MINUS SIDE OF PILLBOX
        PillboxButton(
            modifier = Modifier
                .testTagJoin(testTag, "Decrement")
                .fillMaxSize()
                .weight(1f),
            style = style,
            iconId = R.drawable.ic_minus_white_v2,
            isPressed = isMinusBtnPressed,
            interactionSource = minusBtnInteractionSource
        ) {
            if (pillboxValue.value - stepValue >= min) {
                pillboxValue.value -= stepValue
            } else {
                pillboxValue.value = min
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(style.dividerStroke.width)
                .background(style.dividerStroke.brush)
        )
        // MIDDLE OF PILLBOX
        Box(modifier = Modifier
            .testTagJoin(testTag, "Button")
            .fillMaxSize()
            .clickable { numPadVisible.value = true }
            .weight(2.5f),
        ){
            if (usePercentageBar) {
                Box (
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .width(8.dp.computeSize())
                        .background(style.valueBar)
                        .fillMaxHeight((pillboxValue.value - min) / (max - min))
                )
            }
            KeyPadImage(style.valueIconId)
            PillBoxValue(displayString, testTag, style)
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(style.dividerStroke.width)
                .background(style.dividerStroke.brush)
        )
        // PLUS SIDE OF PILLBOX
        PillboxButton(
            modifier = Modifier
                .testTagJoin(testTag, "Increment")
                .fillMaxSize()
                .weight(1f),
            style = style,
            iconId = R.drawable.ic_plus_white_v2,
            isPressed = isPlusBtnPressed,
            interactionSource = plusBtnInteractionSource
        ) {
            if (pillboxValue.value + stepValue <= max) {
                pillboxValue.value += stepValue
            } else {
                pillboxValue.value = max
            }
        }
    }
}

@Composable
fun PillBoxGender(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "",
    style: PillBoxStyle = PillBoxDefaults.style(),
    value: Gender = Gender.DEFAULT,
    onValueChange: (Gender)->Unit = {},
) {
    val items = listOf(
        DropdownInputItem(Gender.Male, stringResource(id = R.string.male_tc)),
        DropdownInputItem(Gender.Female, stringResource(id = R.string.female_tc)),
        DropdownInputItem(Gender.NonBinary, stringResource(id = R.string.non_binary_gender_tc))
    )
    val current = items.find { it.value == value } ?: items.last() // if not male or female, then make it other
    PillBoxOptions(modifier, stringResource(id = R.string.gender_tc), items, current, enabled, testTag, style) { onValueChange(it.value as Gender) }
}

@Composable
fun PillBoxOptions(
    modifier: Modifier,
    label: String,
    dropDownChoices: Iterable<DropdownInputItem>,
    value: DropdownInputItem,
    enabled: Boolean = true,
    testTag: String = "",
    style: PillBoxStyle = PillBoxDefaults.style(),
    onValueChange: (DropdownInputItem)->Unit = {},
) {
    val showOptions = remember { mutableStateOf(false) }
    PillBoxContainer(
        label,
        modifier,
        style,
    ) {
        BoxWithConstraints(
            Modifier
                .testTagJoin(testTag, "Button")
                .weight(1f)
                .clickable(enabled) { showOptions.value = true }
        ) {
            KeyPadImage(style.valueIconId)
            PillBoxValue(value.displayName, testTag, style)
            if (showOptions.value) Popup(
                onDismissRequest = { showOptions.value = false },
            ) {
                Column(
                    Modifier
                        .background(style.valueBackground, style.shape)
                        .border(style.borderStroke, style.shape)
                        .width(maxWidth) // think this works, otherwise could use onSizeChanged
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    dropDownChoices.forEach {
                        DropdownMenuItem(
                            text = {
                                Text(it.displayName, style = style.valueTextStyle)
                            },
                            onClick = {
                                onValueChange(it)
                                showOptions.value = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/** The outer shell of the PillBox */
@Composable
private fun PillBoxContainer(
    titleLabel: String,
    modifier: Modifier,
    style: PillBoxStyle,
    content: @Composable RowScope.() -> Unit,
) {
    Column(modifier = modifier) {
        AutoSizeText(
            text = titleLabel,
            modifier = Modifier
                .padding(5.dp.computeSize())
                .fillMaxWidth()
                .weight(1f),
            style = style.labelTextStyle,
            textAlign = TextAlign.Start,
            maxLines = 1,
        )
        Row(
            modifier = Modifier
                .border(style.borderStroke, style.shape)
                .background(style.valueBackground, style.shape)
                .clip(style.shape)
                .padding(style.borderStroke.width)
                .weight(2.25f)
                .fillMaxSize(),
            content = content,
        )
    }
}

/** Value displayed in center of PillBox */
@Composable
private fun BoxScope.PillBoxValue(
    displayString: String,
    testTag: String,
    style: PillBoxStyle,
) {

    AutoSizeText(text = displayString,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(10.dp.computeSize()),
        style = style.valueTextStyle,
        textAlign = TextAlign.Center,
        maxLines = 1,
        testTag = if (testTag.isEmpty()) testTag else "${testTag}Value",
    )
}

/** Keypad icon shown in the corner of PillBox value */
@Composable
private fun BoxScope.KeyPadImage(iconId: Int) {
    if (iconId != 0) Image(painter = painterResource(
        id = iconId),
        contentDescription = "pillbox_keypad_icon",
        colorFilter = ColorFilter.tint(DynamicTheme.colors.tertiary),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(5.dp.computeSize())
            .fillMaxHeight(0.15f)
    )
}

/** Button inside of PillBox */
@Composable
private fun PillboxButton(
    modifier: Modifier,
    style: PillBoxStyle,
    iconId:Int,
    isPressed:Boolean,
    interactionSource: MutableInteractionSource,
    onClick:() -> Unit
) {
    TextButton(
        modifier = modifier,
        interactionSource = interactionSource,
        colors = ButtonDefaults.textButtonColors(containerColor = style.buttonBackground),
        shape = RectangleShape, // let parent clip it
        onClick = {
            onClick()
        },
    ) {
        Image(painter = painterResource(
            id = iconId),
            contentDescription = "plus",
            colorFilter = ColorFilter.tint(style.buttonForeground),
            modifier = Modifier.fillMaxSize()
        )
        if (isPressed) {
            LaunchedEffect(true) {
                delay(500)
                while(true) {
                    delay(100)
                    onClick()
                }
            }
        }
    }
}

/**
 * Add logic to make sure min < max and vice versa for pair of input variables.
 */
@Composable
private fun AddMinMaxLogic(workoutStartingInputs:List<WorkoutInputUi>, minVariable: String, maxVariable: String) {
    // iterate through workout input list to find the step size
    var minValueState: MutableState<Float>? = null
    var maxValueState: MutableState<Float>? = null
    var stepSize = 0f
    for (input in workoutStartingInputs) {
        when (input.identifier) {
            minVariable -> {
                minValueState = input.currentValue
                stepSize = input.stepSize
            }
            maxVariable -> maxValueState = input.currentValue
        }
    }
    if (minValueState != null && maxValueState != null) {
        // logic to keep max > min
        LaunchedEffect(minValueState.value) {
            if (minValueState.value >= maxValueState.value) {
                maxValueState.value = minValueState.value + stepSize
            }
        }

        // logic to keep min < max
        LaunchedEffect(maxValueState.value) {
            if (maxValueState.value <= minValueState.value) {
                minValueState.value = maxValueState.value - stepSize
            }
        }
    }
}

/**
 * Add logic to make sure min < max and vice versa for resistance, target watts, and speed
 */
@Composable
private fun AddMinMaxLogic(workoutStartingInputs:List<WorkoutInputUi>) {
    // add min/max resistance logic
    AddMinMaxLogic(workoutStartingInputs,ConsoleWorkoutVariables.MIN_RESISTANCE, ConsoleWorkoutVariables.MAX_RESISTANCE)
    // add min/max target watts logic
    AddMinMaxLogic(workoutStartingInputs,ConsoleWorkoutVariables.MIN_TARGET_WATTS_PAIR, ConsoleWorkoutVariables.MAX_TARGET_WATTS_PAIR)
    // add min/max speed logic
    // Are there setups that uses min/max speed? There's logic in MinMaxParameterLogic Java class
    AddMinMaxLogic(workoutStartingInputs,ConsoleWorkoutVariables.MIN_SPEED, ConsoleWorkoutVariables.MAX_SPEED)
}

@Composable
fun PillboxGrid(
    modifier: Modifier = Modifier,
    workoutInputList:List<WorkoutInputUi>,
    rows: Int = 3,
    columns: Int = 2,
    spacing: Dp = 0.dp,
    itemModifierProvider: @Composable BoxScope.(BoxGridCell<WorkoutInputUi>) -> Modifier = { Modifier.fillMaxSize(0.95f) },
    labelPrefix: String = "",
    style: PillBoxStyle = PillBoxDefaults.style(),
) {
    AddMinMaxLogic(workoutInputList)

    BoxGrid(
        rows = rows, columns = columns, items = workoutInputList, spacing = spacing, modifier = modifier,
    ) { cell ->
        PillboxGridHelper(
            itemModifierProvider(cell), cell.item, cell.item.currentValue, labelPrefix, style
        )
    }
}

/** Inputs whose value is held in minutes and shown as MM:SS rather than a plain number */
private val minuteBasedTimeInputs = setOf(
    ConsoleWorkoutVariables.TIME_GOAL_START,
    ConsoleWorkoutVariables.INTERVAL_TIME_GOAL,
    ConsoleWorkoutVariables.REST_TIME_GOAL,
)

@Composable
private fun PillboxGridHelper(
    modifier: Modifier,
    workoutInput: WorkoutInput,
    currentValue: MutableState<Float>,
    labelPrefix: String = "",
    style: PillBoxStyle,
) {
    // in case of a race condition where the float value is not in the map before the page
    // attempts to load it.  Is this way because of callbacks to load workout inputs, which the
    // default values of need to be loaded into the map to allow the ui to manipulate them

    val testTagPrefix = "${workoutInput.identifier}:"
    if (workoutInput.identifier != ConsoleWorkoutVariables.GENDER) {
        PillBox(
            modifier = modifier,
            pillboxValue = currentValue,
            titleLabel = labelPrefix + workoutInput.label.replace('\n', ' '), // some labels have new lines in them, replace with space
            unitLabel = workoutInput.unitString,
            stepValue = workoutInput.stepSize,
            decimalPlaces = workoutInput.precision,
            displayType = if (workoutInput.identifier in minuteBasedTimeInputs) NumPadDisplay.TimeValueMinutes else NumPadDisplay.Number, // ConsoleWorkoutVariable
            min = workoutInput.min,
            max = workoutInput.max,
            usePercentageBar = workoutInput.identifier != ConsoleWorkoutVariables.WEIGHT, // ConsoleWorkoutVariable, we don't show bar for weight
            forceOnlyStepValues = workoutInput.identifier == ConsoleWorkoutVariables.START_INCLINE ||
            workoutInput.identifier == ConsoleWorkoutVariables.MAXIMUM_INCLINE, // ConsoleWorkoutVariable
            testTag = testTagPrefix,
            style = style,
        )
    } else {
        PillBoxGender(
            modifier = modifier,
            testTag = testTagPrefix,
            style = style,
            value = Gender.from(currentValue.value.toInt()),
            onValueChange = { gender ->
                currentValue.value = gender.value.toFloat()
            }
        )
    }
}