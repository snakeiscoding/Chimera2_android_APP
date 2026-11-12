package com.jht.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.computeSize
import com.jht.widgets.numpad.NumPadDefaults
import com.jht.widgets.numpad.NumPadDisplay
import com.jht.widgets.numpad.NumPadStyle
import androidx.core.text.isDigitsOnly
import com.jht.widgets.numpad.internal.ANIMATION_ITERATIONS
import com.jht.widgets.numpad.internal.ANIMATION_TIME
import com.jht.widgets.numpad.internal.AUTOCORRECT_TIMEOUT
import com.jht.widgets.numpad.internal.MAX_USER_ENTRY_CHARACTER_LENGTH
import com.jht.widgets.numpad.internal.NUMPAD_TIMEOUT
import com.jht.widgets.numpad.internal.ENTRY_INITIAL
import com.jht.widgets.numpad.internal.ENTRY_MAX
import com.jht.widgets.numpad.internal.ENTRY_MIN
import com.jht.widgets.numpad.internal.ENTRY_ROUND
import com.jht.widgets.numpad.internal.DeleteButton
import com.jht.widgets.numpad.internal.EnterButton
import com.jht.widgets.numpad.internal.EnteredTextPanel
import com.jht.widgets.numpad.internal.NumberButton
import com.jht.widgets.numpad.internal.NumberRow
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

//region Previews
@AllDevicePreviews
@Composable
fun PreviewNumPadEntry() {
    DynamicTheme {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)) {
        }
        val numPadVisible = remember { mutableStateOf(true) }
        val numFromPillBox = remember { mutableStateOf(1.5f) }

        NumPadEntry(
            numPadValue = numFromPillBox.value, // default number in box, since it is mutable it will update
            onValueCommit = { numFromPillBox.value = it },
            onDismiss = { numPadVisible.value = false },
            decimalPlaces = 1,
            min = 0f,
            max = 15f,
            displayType = NumPadDisplay.Number,
            usePercentageBar = true,
            unitLabel = "%",
            forceOnlyStepValues = true,
            stepValue = .5f
        )
    }
}

@AllDevicePreviews
@Composable
fun PreviewNumPadEntryMask() {
    DynamicTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
        }
        val numPadVisible = remember { mutableStateOf(true) }
        val numFromPillBox = remember { mutableStateOf(1234f) }

        NumPadEntry(
            numPadValue = numFromPillBox.value, // default number in box, since it is mutable it will update
            onValueCommit = { numFromPillBox.value = it },
            onDismiss = { numPadVisible.value = false },
            displayType = NumPadDisplay.Passcode,
        )
    }
}
//endregion Previews

/** Used in Product Dashboard */
@Composable
fun NumPadEntryPopup(
    numPadVisible: MutableState<Boolean>,
    numPadValue: MutableState<Float>,
    decimalPlaces: Int = 0,
    displayType: NumPadDisplay = NumPadDisplay.Number,
    min: Float = 0f,
    max: Float = displayType.getMax(),
    usePercentageBar: Boolean = false,
    unitLabel: String = "",
    forceOnlyStepValues: Boolean = false, // for incline problem
    stepValue: Float = Float.MAX_VALUE,
    displayNumberWithoutUserInput: Boolean = true, // show 0 when there is no user input, show nothing if false
) {
    if (numPadVisible.value) {
        NumPadEntryPopup(
            numPadValue.value,
            decimalPlaces, displayType, min, max, usePercentageBar, unitLabel, forceOnlyStepValues, stepValue, displayNumberWithoutUserInput,
            onCommit = {
                numPadValue.value = it
                numPadVisible.value = false
            },
            onDismiss = { numPadVisible.value = false }
        )
    }
}

/**
 * Used in FTSU Top Bar
 */
@Composable
fun NumPadEntryPopup(
    numPadValue: Float = 0f,
    decimalPlaces: Int = 0,
    displayType: NumPadDisplay = NumPadDisplay.Number,
    min: Float = 0f,
    max: Float = displayType.getMax(),
    usePercentageBar: Boolean = false,
    unitLabel: String = "",
    forceOnlyStepValues: Boolean = false, // for incline problem
    stepValue: Float = Float.MAX_VALUE,
    displayNumberWithoutUserInput: Boolean = true, // show 0 when there is no user input, show nothing if false
    onCommit: (Float) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    PopupFullscreen(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .7f))
                .clickableNoEffect(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            NumPadEntry(
                numPadValue = numPadValue,
                onValueCommit = { onCommit(it) },
                onDismiss = onDismiss,
                decimalPlaces = decimalPlaces,
                min = min,
                max = max,
                displayType = displayType,
                usePercentageBar = usePercentageBar,
                unitLabel = unitLabel,
                forceOnlyStepValues = forceOnlyStepValues,
                stepValue = stepValue,
                displayZeroForNoUserInput = displayNumberWithoutUserInput
            )
        }
    }
}


/**
 * @param numPadValue - the initial value of the numpad that is displayed to the user.
 *                      Enter a negative number to not display any initial value.
 * @param modifier - modifier
 * @param decimalPlaces - decimal precision for displayed numbers
 * @param displayAsTime - true to display as HH:MM:SS
 * @param min - minimum accepted value for user input from the keypad
 * @param max - maximum accepted value for user input from the keypad
 * @param usePercentageBar - true to display the bar showing the current value's % of the max
 * @param unitLabel - label to display in the numpad textbox
 * @param forceOnlyStepValues - true to only accept values that coincide with the step value
 *                              e.g. incline, 0.0% -> 0.5% -> 1.0% etc. 0.4 would not be allowed,
 *                              and would be rounded to 0.5
 * @param stepValue - step value for incrementing values, useful in tandem with forceOnlyStepValues
 * @param displayZeroForNoUserInput - true to display 0 in the correct format when a user has begun
 *                                    typing so that the initial value is no longer showing, but has
 *                                    cleared all input. (they pressed the delete button) This will
 *                                    be autocorrected to the min if it is not also 0.  false to
 *                                    display nothing when there is no user input.
 * @param onValueCommit - code that will execute when the numpad submit button has been tapped, with
 *                        the current float in the numpad passed as a parameter.
 * @param onDismiss - code that will execute when the NUMPAD_TIMEOUT has been reached.
 */
@Composable
fun NumPadEntry(
    modifier: Modifier = Modifier,
    numPadValue: Float,
    decimalPlaces: Int = 0,
    displayType: NumPadDisplay = NumPadDisplay.Number,
    min: Float = 0f,
    max: Float = displayType.getMax(),
    usePercentageBar: Boolean = false,
    unitLabel: String = "",
    forceOnlyStepValues: Boolean = false,
    stepValue: Float = 1f,
    displayZeroForNoUserInput: Boolean = true,
    style: NumPadStyle = NumPadDefaults.style(),
    onValueCommit: (Float) -> Unit = { },
    onDismiss: () -> Unit = {},
) {

    // LOGIC
    // timer to trigger changing invalid input to valid, as well as closing the numpad on inactivity
    var timeSinceUserEntry by remember { mutableStateOf(0) }
    // keeps track of when we are changing invalid user input to valid user input, while showing the user an animation
    var blockUserEntryForAnimation by remember { mutableStateOf(false) }
    // keep track of the closest acceptable value for the incline edge case (only accept incremental values)
    var roundToClosestAcceptedValue by remember { mutableStateOf(min) }
    // everything is based on this string
    val userEntryAsString = remember { mutableStateOf(ENTRY_INITIAL) }
    // this float will recompose everytime the string above is changed, thus giving me an easy number for math
    val userEntryAsFloat = getUserEntryAsFloat(userEntryAsString.value, decimalPlaces, displayType, min, max, numPadValue, roundToClosestAcceptedValue, displayZeroForNoUserInput)
    // likewise this will recompose and keep track of whether user input in is the range specified
    val outOfRangeGiven = userEntryAsFloat !in min..max
    // yet again this will recompose and keep track of whether user input is an acceptable step value
    // useful when forcing only step values
    val scaledValue = if (decimalPlaces == 0) userEntryAsFloat.toInt() else (userEntryAsFloat * 10 * decimalPlaces).toInt() // see validateInput in old widgets Keypad
    val scaledResolution = if (decimalPlaces == 0) stepValue.toInt() else (stepValue * 10 * decimalPlaces).toInt()
    val isAcceptableStepValue = !outOfRangeGiven && (scaledResolution == 0 || scaledValue % scaledResolution == 0)
    // if max is an hour or more, display the hour digit for all shown times
    val useHours = displayType.needHours(max)
    // maximum allowed user entry
    val maxUserEntry: Int = when {
        useHours -> 6              // HH:MM:SS
        displayType.isTime() -> 4  // MM:SS
        else -> minOf(max.toInt().toString().length + decimalPlaces, MAX_USER_ENTRY_CHARACTER_LENGTH)
    }


    // keep track of time (to do animation/block user input and close keypad on inactivity)
    if (!blockUserEntryForAnimation) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1.seconds)
                timeSinceUserEntry++
            }
        }
    }

    // inactivity close
    if (timeSinceUserEntry > NUMPAD_TIMEOUT) {
        onDismiss()
    }

    // wait for blinking animation to finish when invalid user input is changed to valid input
    if (blockUserEntryForAnimation) {
        LaunchedEffect(Unit) {
            delay((ANIMATION_TIME*ANIMATION_ITERATIONS).toLong()) // wait for animation to play
            blockUserEntryForAnimation = false // allow users to type on keypad
        }
    }

    if (timeSinceUserEntry > AUTOCORRECT_TIMEOUT && userEntryAsString.value != ENTRY_INITIAL && displayType != NumPadDisplay.Passcode) {
        if (outOfRangeGiven) {
            blockUserEntryForAnimation = true
            if (userEntryAsFloat > max) {
                userEntryAsString.value = ENTRY_MAX
            } else if (userEntryAsFloat < min) {
                userEntryAsString.value = ENTRY_MIN
            }
        } else if (forceOnlyStepValues && !isAcceptableStepValue) {
            val difference = userEntryAsFloat % stepValue
            roundToClosestAcceptedValue = if (difference >= (stepValue * .5f)) {
                userEntryAsFloat + (stepValue - difference)
            } else {
                userEntryAsFloat - difference
            }
            blockUserEntryForAnimation = true
            userEntryAsString.value = ENTRY_ROUND
        }
    }
    val ableToSubmitValue = !(outOfRangeGiven || (forceOnlyStepValues && !isAcceptableStepValue))

    val onDigit: (String) -> Unit = { digit ->
        timeSinceUserEntry = 0
        val current = userEntryAsString.value
        when {
            current.isNotEmpty() && !current.isDigitsOnly() -> userEntryAsString.value = digit
            current.isNotEmpty() && current.isDigitsOnly() && userEntryAsFloat == 0f && digit == "0" -> { /* prevent multiple leading zeros */ }
            current.length < maxUserEntry -> userEntryAsString.value += digit
        }
    }

    Box(
        modifier = modifier
            .size(width = 400.dp.computeSize(), height = 600.dp.computeSize())
            .background(color = style.background, shape = RoundedCornerShape(style.popUpStroke)),
        contentAlignment = Alignment.TopStart
    ) {
        Column(Modifier.padding(5.dp.computeSize())) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                EnteredTextPanel(
                    userEnteredFloat = userEntryAsFloat,
                    decimalPlaces = decimalPlaces,
                    min = min,
                    max = max,
                    blockUserEntryForAnimation = blockUserEntryForAnimation,
                    displayType = displayType,
                    useHours = useHours,
                    usePercentageBar = usePercentageBar,
                    unitLabel = unitLabel,
                    style = style,
                    ableToSubmitValue = ableToSubmitValue,
                )
                DeleteButton(
                    style = style,
                    onDelete = {
                        timeSinceUserEntry = 0
                        if (!blockUserEntryForAnimation) {
                            val current = userEntryAsString.value
                            userEntryAsString.value = if (current.isDigitsOnly()) current.dropLast(1) else ""
                        }
                    },
                    onClear = {
                        timeSinceUserEntry = 0
                        if (!blockUserEntryForAnimation) {
                            userEntryAsString.value = ""
                        }
                    },
                )
            }
            NumberRow(
                numbers = listOf("1", "2", "3"),
                isBlocked = blockUserEntryForAnimation,
                style = style,
                onDigit = onDigit,
            )
            NumberRow(
                numbers = listOf("4", "5", "6"),
                isBlocked = blockUserEntryForAnimation,
                style = style,
                onDigit = onDigit,
            )
            NumberRow(
                numbers = listOf("7", "8", "9"),
                isBlocked = blockUserEntryForAnimation,
                style = style,
                onDigit = onDigit,
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                NumberButton(
                    number = "0",
                    isBlocked = blockUserEntryForAnimation,
                    style = style,
                    onClick = { onDigit("0") },
                )
                EnterButton(
                    isEnabled = ableToSubmitValue,
                    style = style,
                    onClick = {
                        onValueCommit(userEntryAsFloat)
                        userEntryAsString.value = ENTRY_INITIAL
                        onDismiss()
                    },
                )
            }
        }
    }
}

private fun getUserEntryAsFloat(
    userEntryAsString:String = "",
    decimalPlaces: Int = 0,
    displayType: NumPadDisplay = NumPadDisplay.Number,
    min: Float,
    max: Float,
    initialNum: Float,
    roundToClosestAcceptedValue: Float,
    displayNumberWithoutUserInput: Boolean
): Float {
    when (userEntryAsString) {
        ENTRY_MIN -> return min
        ENTRY_MAX -> return max
        ENTRY_INITIAL -> return initialNum
        ENTRY_ROUND -> return roundToClosestAcceptedValue
    }

    // yes I know it is pedantic af to take the user entered numbers, turn them into time in seconds,
    // and then turn them back into a string to display.  but this is easier to follow when reading imo
    if (displayType.isTime()) {
        var hours = 0f
        var minutes = 0f
        var seconds = 0f
        when (userEntryAsString.length) {
            // only have seconds
            1,2 -> {
                seconds = userEntryAsString.toFloat()
            }
            // minutes and seconds
            3 -> {
                minutes = userEntryAsString.dropLast(2).toFloat()
                seconds = userEntryAsString.drop(1).toFloat()
            }
            4 -> {
                minutes = userEntryAsString.dropLast(2).toFloat()
                seconds = userEntryAsString.drop(2).toFloat()
            }
            // hours, minutes, seconds
            5 -> {
                hours = userEntryAsString.dropLast(4).toFloat()
                minutes = userEntryAsString.dropLast(2).drop(1).toFloat()
                seconds = userEntryAsString.drop(3).toFloat()
            }
            6 -> {
                hours = userEntryAsString.dropLast(4).toFloat()
                minutes = userEntryAsString.dropLast(2).drop(2).toFloat()
                seconds = userEntryAsString.drop(4).toFloat()
            }
        }
        val totalSeconds = (hours * 60 * 60) + (minutes * 60) + seconds
        return if (displayType == NumPadDisplay.TimeValueMinutes) totalSeconds / 60 else totalSeconds
    } else {
        var stringToReturnAsFloat = userEntryAsString
        if (decimalPlaces > 0) {
            if (userEntryAsString.length <= decimalPlaces) {
                val difference = decimalPlaces - userEntryAsString.length
                stringToReturnAsFloat = "0."
                for (i in 1..difference) stringToReturnAsFloat += "0"
                stringToReturnAsFloat += userEntryAsString
            } else {
                stringToReturnAsFloat = userEntryAsString.dropLast(decimalPlaces) + "." +
                        userEntryAsString.drop(userEntryAsString.length - decimalPlaces)
            }
        }
        if (stringToReturnAsFloat.isEmpty()) {
            stringToReturnAsFloat = if (displayNumberWithoutUserInput) "0" else "-1"
        }
        return stringToReturnAsFloat.toFloat()
    }
}
