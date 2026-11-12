package com.jht.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.jht.enums.Gender
import com.jht.enums.UnitSystem
import com.jht.theme.DynamicTheme
import com.jht.widgets.helpers.MaskVisualTransformation
import java.time.LocalDate


private const val maxDateCharacters = 8

@Composable
fun DateInput(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: LocalDate? = null,
    onChange: (LocalDate?) -> Unit = {},
    onFocusLoss: () -> Unit = {},
    errorText: String = "",
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
){
    // TODO - set an initial date value if provided?
    var dateText by remember { mutableStateOf(TextFieldValue("")) }
    val format = android.text.format.DateFormat.getDateFormatOrder(LocalContext.current)
    val dateLabel = when (format[0].toString()) {
        "d" -> R.string.dd_mm_yyyy_ac
        "M" -> R.string.mm_dd_yyyy_ac
        "Y" -> R.string.yyyy_mm_dd_ac
        else -> R.string.dd_mm_yyyy_ac
    }
    val dateMask = when (format[0].toString()) {
        "Y" -> "####/##/##"
        else -> "##/##/####"
    }

    BaseInput(
        modifier = modifier,
        label = stringResource(id = R.string.birthdate_tc),
        placeholder = stringResource(id = dateLabel),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword, // digits only
            imeAction = ImeAction.Next
        ),
        value = dateText,
        onValueChange = { textFieldValue ->
            val newString = textFieldValue.text.filter { it.isDigit() }
            if (newString.length <= maxDateCharacters) {
                if (newString == textFieldValue.text) { // if nothing was removed
                    dateText = textFieldValue
                    val date = when (format[0].toString()) {
                        "M" -> decodeMDY(newString)
                        "Y" -> decodeYMD(newString)
                        else -> decodeDMY(newString)
                    }
                    onChange(date)
                }
            }
        },
        isError = errorText.isNotEmpty(),
        errorText = errorText,
        visualTransformation = MaskVisualTransformation(dateMask),
        enabled = enabled,
        onFocusLoss = onFocusLoss,
        fieldInputOptions = fieldInputOptions,
    )
}

private fun decodeDMY(s: String) : LocalDate? {
    if (s.length < maxDateCharacters) return null
    val d = s.substring(0, 2).toIntOrNull()
    val m = s.substring(2, 4).toIntOrNull()
    val y = s.substring(4, 8).toIntOrNull()
    if (d == null || m == null || y == null) return null
    return try { LocalDate.of(y, m, d) } catch (ignored : Exception) { null }
}
private fun decodeMDY(s: String) : LocalDate? {
    if (s.length < maxDateCharacters) return null
    val m = s.substring(0, 2).toIntOrNull()
    val d = s.substring(2, 4).toIntOrNull()
    val y = s.substring(4, 8).toIntOrNull()
    if (d == null || m == null || y == null) return null
    return try { LocalDate.of(y, m, d) } catch (ignored : Exception) { null }
}
private fun decodeYMD(s: String) : LocalDate? {
    if (s.length < maxDateCharacters) return null
    val y = s.substring(0, 4).toIntOrNull()
    val m = s.substring(4, 6).toIntOrNull()
    val d = s.substring(6, 8).toIntOrNull()
    if (d == null || m == null || y == null) return null
    return try { LocalDate.of(y, m, d) } catch (ignored : Exception) { null }
}




@Composable
fun GenderInput(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: Gender = Gender.DEFAULT,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
    onValueChange: (Gender)->Unit = {},
) {
    val items = listOf(
        DropdownInputItem(Gender.Male, stringResource(id = R.string.male_tc)),
        DropdownInputItem(Gender.Female, stringResource(id = R.string.female_tc)),
        DropdownInputItem(Gender.NonBinary, stringResource(id = R.string.non_binary_gender_tc))
    )
    val current = items.find { it.value == value } ?: items.last() // if not male or female, then make it other
    DropdownInput(modifier, stringResource(id = R.string.gender_tc), items, current, enabled, fieldInputOptions) { onValueChange(it.value as Gender) }
}




@Composable
fun UnitSystemInput(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: UnitSystem = UnitSystem.DEFAULT,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
    onChange: (UnitSystem)->Unit = {},
) {
    UnitSystemInput(modifier, enabled, value.isMetric(), fieldInputOptions) { onChange(if (it) UnitSystem.Metric else UnitSystem.Imperial) }
}

@Composable
fun UnitSystemInput(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isMetric: Boolean = true,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
    onChange: (Boolean)->Unit = {},
) {
    Column(
        modifier = modifier.fillMaxHeight()
    ) {
        // recent changes to text input widget include an outside label text
        Text(
            stringResource(R.string.units_tc),
            style = fieldInputOptions.labelStyle,
            modifier = Modifier.padding(fieldInputOptions.labelPaddingValues)
        )
        Row(
            Modifier
                .weight(1f)
                //.height(IntrinsicSize.Max)
                .padding(fieldInputOptions.contentPaddingValues)
        ) {
            UnitButton(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically),
                text = stringResource(R.string.metric_tc),
                onClick = { if (enabled) onChange(true) },
                selected = isMetric,
                enabled = enabled,
                fieldInputOptions = fieldInputOptions,
            )
            UnitButton(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically),
                text = stringResource(R.string.imperial_tc),
                onClick = { if (enabled) onChange(false) },
                selected = !isMetric,
                enabled = enabled,
                fieldInputOptions = fieldInputOptions,
            )
        }
        // supporting text area to match other inputs
        Text(
            "",
            style = fieldInputOptions.supportTextStyle,
            modifier = Modifier.padding(fieldInputOptions.supportTextPaddingValues),
            color = DynamicTheme.colors.stop
        )
    }
}
@Composable
private fun UnitButton(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    enabled: Boolean = true,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
    onClick: ()->Unit = {},
) {
    // This is like the Theme Flat Button except it needs to look like the unselected is disabled while actually enabled
    val colors: ButtonColors = ButtonDefaults.buttonColors(containerColor = if (selected) DynamicTheme.colors.primary else DynamicTheme.colors.gray1)
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = colors,
        enabled = enabled,
    ) {
        if (selected) Text(text, color = DynamicTheme.colors.text2, style = fieldInputOptions.textStyle)
        else Text(text, color = DynamicTheme.colors.text1.copy(alpha = 0.8f), style = fieldInputOptions.textStyle)
    }
}

