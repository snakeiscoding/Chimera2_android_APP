package com.jht.progression.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.jht.progression.data.convertRaceTimeToSeconds
import com.jht.progression.data.secondsToHrMinSecString
import com.jht.widgets.rememberKeyboardState

const val MAX_DURATION_LENGTH = 6
const val MAX_DURATION_HOURS = 99
const val MAX_DURATION_MINUTES = 59
const val MAX_DURATION_SECONDS = 59

@Composable
fun MmDurationInput(
    modifier: Modifier = Modifier,
    duration: String = "",
    onDurationChange: (String) -> Unit = {},
    label: String = "",
    hint: String = "",
    isInvalid: Boolean
) {
    // Reformat improperly formatted duration when user closes the keyboard
    val keyboardVisible by rememberKeyboardState()
    LaunchedEffect(keyboardVisible) {
        if (keyboardVisible) return@LaunchedEffect
        if (duration.isEmpty() || duration.length == MAX_DURATION_LENGTH) return@LaunchedEffect
        val totalSeconds = convertRaceTimeToSeconds(duration)
        onDurationChange(secondsToHrMinSecString(totalSeconds, "").trimStart { it == '0' })
    }

    MmTextField(
        modifier = modifier,
        text = duration,
        label = label,
        hint = hint,
        isInvalid = isInvalid,
        onTextChange = {
            val filteredDuration = it
                .filter { char -> char.isDigit() }
                .trimStart { char -> char == '0' }
            when {
                filteredDuration.length < MAX_DURATION_LENGTH -> onDurationChange(filteredDuration.ifEmpty { "" })
                filteredDuration.length == MAX_DURATION_LENGTH -> {
                    val builder = StringBuilder(filteredDuration)
                    val hours = builder.substring(0, 2).coerceIn("00", MAX_DURATION_HOURS.toString())
                    val minutes = builder.substring(2, 4).coerceIn("00", MAX_DURATION_MINUTES.toString())
                    val seconds = builder.substring(4, 6).coerceIn("00", MAX_DURATION_SECONDS.toString())
                    builder.setRange(0, 2, hours)
                    builder.setRange(2, 4, minutes)
                    builder.setRange(4, 6, seconds)
                    onDurationChange (builder.take(MAX_DURATION_LENGTH).toString())
                }
            }
        },
        keyboardType = KeyboardType.NumberPassword,
        visualTransformation = TimeVisualTransformation(),
    )
}

/**
 * Setup and format the text input to a time as needed. The cursor always stays at the rightmost
 * position of the time format. Input numbers will enter and move from right to left, it will
 * delete and move form left to right.
 */
class TimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }
        val formattedTime = if (digits.isNotEmpty()) {
            when (digits.length) {
                1 -> "00:00:0${digits[0]}"
                2 -> "00:00:${digits[0]}${digits[1]}"
                3 -> "00:0${digits[0]}:${digits[1]}${digits[2]}"
                4 -> "00:${digits[0]}${digits[1]}:${digits[2]}${digits[3]}"
                5 -> "0${digits[0]}:${digits[1]}${digits[2]}:${digits[3]}${digits[4]}"
                else -> "${digits[0]}${digits[1]}:${digits[2]}${digits[3]}:${digits[4]}${digits[5]}"
            }
        } else {
            ""
        }

        // the cursor always stays at the rightmost position of the formatted time
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return formattedTime.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                return text.text.length
            }
        }
        return TransformedText(AnnotatedString(formattedTime), offsetMapping)
    }
}