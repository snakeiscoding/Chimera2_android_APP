package com.jht.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.jht.data.UserField
import com.jht.data.UserFieldDate
import com.jht.data.UserFieldDigits
import com.jht.data.UserFieldEmail
import com.jht.data.UserFieldFunction
import com.jht.data.UserFieldGender
import com.jht.data.UserFieldInteger
import com.jht.data.UserFieldNumber
import com.jht.data.UserFieldUnits
import com.jht.data.UserFieldValidity
import com.jht.theme.DynamicTheme
import com.jht.ui.computeSize
import com.jht.widgets.helpers.AsyncUiState

/*
    This file provides classes and composables to generically handle user input boxes.
    "FieldInputData" and subclasses provides backing data and validation.
    "FieldInputData.build" creates "FieldInputData" based on generic UserField classes from the interfacing.
    "FieldInput" provides the corresponding composable.
 */

// do we worry about validating as user types, on focus loss, or form submit? cover all of them or some of them?
// do we need provision for custom error messages or can we get by with canned ones?


/** Provides UI specific tracking wrappers to a generic UserField */
open class FieldInputData(
    open val testTag: String, // label will likely be localized, we need a fixed name for testing tags
    open val field: UserField, // the UI agnostic data field class
    open val onDone: (() -> Unit)? = null, // if no callback, field is given "next" behavior (or do we need to expose the full keyboard options and actions?)
) {
    val validity = mutableStateOf(UserFieldValidity.VALID) // replace with error string and validation state? (but do we want string resources at this level?)
    val customError = mutableStateOf("") // async checks may have specific messaging, so perhaps need a custom option?

    internal fun setNewValue(nextValue: String) : String? {
        // should we block input while async is running or just track values being checked?
        return if (field.inputValid(nextValue)) {
            field.setStringValue(nextValue)
            field.getStringValue() ?: nextValue
        } else {
            null // block input
        }
    }

    fun checkValidity(includeAsync: Boolean = true) : UserFieldValidity {
        val valid = field.getValidity()
        if (!valid.isValid()) {
            validity.value = valid
            customError.value = ""
            return valid
        }
        if (includeAsync && field.hasAsyncValidation()) {
            if (validity.value != UserFieldValidity.VALIDATING) {
                customError.value = ""
                validity.value = UserFieldValidity.VALIDATING
                field.runAsyncValidation { checkPerformed, error ->
                    if (checkPerformed) {
                        if (error.isNotEmpty()) {
                            validity.value = UserFieldValidity.INVALID
                            customError.value = error
                        } else {
                            validity.value = UserFieldValidity.VALIDATED
                        }
                    } else {
                        validity.value = UserFieldValidity.VALID
                    }
                }
            }
        } else {
            validity.value = valid
        }
        return valid
    }
    internal fun callDone() { onDone?.let { it() } }

    companion object {
        fun build(field: UserField, testTag: String, onDone: (() -> Unit)? = null) : FieldInputData {
            return when (field) {
                is UserFieldGender -> GenderInputData(testTag, field, onDone)
                is UserFieldUnits -> UnitsInputData(testTag, field, onDone)
                is UserFieldDate -> DateInputData(testTag, field, onDone)
                else -> FieldInputData(testTag, field, onDone)
            }
        }
    }
}

/** Manages a collection of FieldInputData */
open class FieldInputCollection : ArrayList<FieldInputData>() {
    // runs validity checks and returns result (what about async)
    fun checkValidity(includeAsync: Boolean = true) : Boolean {
        var allGood = true
        for (data in this) { // loop through all of them so all invalids are shown or cleared
            if (!data.checkValidity(includeAsync).isValid()) allGood = false
        }
        if (isPasswordMismatch()) allGood = false // does password mismatch need special messaging? should it be separate?
        return allGood
    }
    // checks the current validity states
    fun getLastValidity(includeAsync: Boolean = true) : Boolean {
        var allGood = true
        for (data in this) { // loop through all of them so all invalids are shown or cleared
            if (!data.validity.value.isValid()) allGood = false
        }
        if (isPasswordMismatch()) allGood = false // does password mismatch need special messaging? should it be separate?
        return allGood
    }    // should we just expose a test or should we actually mark the fields invalid?
    fun isPasswordMismatch(): Boolean {
        val pw = this.find { it.field.function == UserFieldFunction.Password }
        val pw2 = this.find { it.field.function == UserFieldFunction.ConfirmPassword }
        if (pw == null || pw2 == null) return false // can't be mismatch if they are not there
        val isMismatch = pw.field.getStringValue() != pw2.field.getStringValue()
        if (isMismatch) {
            //pw.validity.value = UserFieldValidity.INVALID // think old UI only marked confirm invalid for a mismatch
            pw2.validity.value = UserFieldValidity.INVALID
        }
        return isMismatch
    }
}


class GenderInputData(
    override val testTag: String,
    override val field: UserFieldGender,
    override val onDone: (() -> Unit)? = null, // not applicable, should we remove and force null?
) : FieldInputData(testTag, field, onDone)

class UnitsInputData(
    override val testTag: String,
    override val field: UserFieldUnits,
    override val onDone: (() -> Unit)? = null, // not applicable, should we remove and force null?
) : FieldInputData(testTag, field, onDone)

class DateInputData(
    override val testTag: String,
    override val field: UserFieldDate,
    override val onDone: (() -> Unit)? = null, // not applicable, should we remove and force null?
) : FieldInputData(testTag, field, onDone)


private fun getKeyboardType(field: UserField) : KeyboardType {
    val keyboard : KeyboardType = when (field) {
        is UserFieldDigits -> KeyboardType.NumberPassword // this keyboard seems to be purely digits
        is UserFieldEmail -> KeyboardType.Email
        is UserFieldInteger -> if (field.allowNegative()) KeyboardType.Decimal else KeyboardType.NumberPassword
        is UserFieldNumber<*> -> {
            KeyboardType.Decimal // supposed to have a decimal separator (but does it promise negative?)
        }
        else -> KeyboardType.Text
    }
    // using password keyboard as a workaround to remove the auto suggestions that show for text keyboard
    return if (keyboard == KeyboardType.Text) KeyboardType.Password else keyboard
}


@Composable
fun FieldInput(
    data: FieldInputData,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
) {
    when (data) {
        is GenderInputData -> GenderInput(
            data = data,
            modifier = modifier,
            enabled = enabled,
            fieldInputOptions = fieldInputOptions,
        )

        is UnitsInputData -> UnitSystemInput(
            data = data,
            modifier = modifier,
            enabled = enabled,
            fieldInputOptions = fieldInputOptions,
        )

        is DateInputData -> DateInput(
            data = data,
            modifier = modifier,
            enabled = enabled,
            fieldInputOptions = fieldInputOptions,
        )

        else -> TextInput(
            data = data,
            modifier = modifier,
            enabled = enabled,
            fieldInputOptions = fieldInputOptions
        )
    }
}


@Composable
fun GenderInput(
    data: GenderInputData,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
) {
    val current = remember { mutableStateOf(data.field.value) }
    GenderInput(modifier, enabled, current.value, fieldInputOptions) { data.field.value = it; current.value = it }
}

@Composable
fun UnitSystemInput(
    data: UnitsInputData,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
) {
    val current = remember { mutableStateOf(data.field.value) }
    UnitSystemInput(modifier, enabled, current.value, fieldInputOptions) { data.field.value = it; current.value = it }
}

@Composable
fun DateInput(
    data: DateInputData,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
) {
    val errorText = when (data.validity.value) {
        UserFieldValidity.INVALID -> stringResource(id = R.string.invalid_tc)
        UserFieldValidity.LENGTH_TOO_SHORT -> stringResource(id = R.string.invalid_tc) + " < ${data.field.getLowerLimit()}"
        UserFieldValidity.LENGTH_TOO_LONG -> stringResource(id = R.string.invalid_tc) + "  > ${data.field.getUpperLimit()}"
        UserFieldValidity.VALUE_TOO_SMALL -> stringResource(id = R.string.invalid_tc) + " < ${data.field.getLowerLimit()}"
        UserFieldValidity.VALUE_TOO_BIG -> stringResource(id = R.string.invalid_tc) + "  > ${data.field.getUpperLimit()}"
        else -> ""
    }
    DateInput(modifier, enabled, data.field.value, { data.field.value = it }, { data.checkValidity() }, errorText, fieldInputOptions)
}

@Immutable
data class FieldInputOptions(
    // Styles for the label text
    val labelStyle: TextStyle,
    // Style for the support text
    val supportTextStyle: TextStyle,
    // Styles for the input text
    val textStyle: TextStyle,
    // Colors for the TextField
    val textFieldColors: TextFieldColors,
    // Styles for the placeholder text
    val placeholderStyle: TextStyle,
    // Field Border values.
    val borderStroke: BorderStroke?,
    // Content Padding values
    val contentPaddingValues: PaddingValues,
    // Label text Padding values
    val labelPaddingValues: PaddingValues,
    // Support text Padding values
    val supportTextPaddingValues: PaddingValues,
    // Shape
    val shape:Shape,
    // Optionally provide a fixed minimum height, or leave null to compute automatically.
    val minHeight: Dp?,
)

object FieldInputDefaults {
    @Composable
    fun defaultStyle(
        labelStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(color = Color.White),
        textStyle: TextStyle = MaterialTheme.typography.labelMedium,
        supportTextStyle:TextStyle = MaterialTheme.typography.bodySmall,
        textFieldColors: TextFieldColors = DynamicTheme.textFieldColors.copy(
            unfocusedContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        placeholderStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.7f)),
        borderStroke: BorderStroke? = BorderStroke(width = 1.dp.computeSize(), color = DynamicTheme.colors.textFieldStroke),
        contentPaddingValues: PaddingValues = PaddingValues(0.dp),
        labelPaddingValues: PaddingValues = PaddingValues(start = 16.dp.computeSize(), bottom = 4.dp.computeSize()),
        supportTextPaddingValues: PaddingValues = PaddingValues(start = 16.dp.computeSize(), top = 4.dp.computeSize()),
        shape: Shape = RoundedCornerShape(12.dp.computeSize()),
        minHeight: Dp? = null,
    ): FieldInputOptions = FieldInputOptions(
        labelStyle = labelStyle,
        textStyle = textStyle,
        supportTextStyle = supportTextStyle,
        textFieldColors = textFieldColors,
        placeholderStyle = placeholderStyle,
        borderStroke = borderStroke,
        contentPaddingValues = contentPaddingValues,
        labelPaddingValues = labelPaddingValues,
        supportTextPaddingValues = supportTextPaddingValues,
        shape = shape,
        minHeight = minHeight,
    )
}

@Composable
fun TextInput(
    data: FieldInputData,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
) {
    var text by remember(data) { mutableStateOf(TextFieldValue(data.field.getStringValue() ?: "")) }
    var hideValue by remember(data) { mutableStateOf(true) }
    val transform = if (data.field.isPassword() && hideValue) PasswordVisualTransformation() else VisualTransformation.None
    val imeAction = if (data.onDone == null) ImeAction.Next else ImeAction.Done
    val errorText = data.customError.value.ifEmpty {
        when (data.validity.value) {
            UserFieldValidity.INVALID -> stringResource(id = R.string.invalid_tc)
            UserFieldValidity.LENGTH_TOO_SHORT -> stringResource(id = R.string.invalid_tc) + " < ${data.field.getLowerLimit()}"
            UserFieldValidity.LENGTH_TOO_LONG -> stringResource(id = R.string.invalid_tc) + "  > ${data.field.getUpperLimit()}"
            UserFieldValidity.VALUE_TOO_SMALL -> stringResource(id = R.string.invalid_tc) + " < ${data.field.getLowerLimit()}"
            UserFieldValidity.VALUE_TOO_BIG -> stringResource(id = R.string.invalid_tc) + "  > ${data.field.getUpperLimit()}"
            else -> ""
        }
    }
    val keyboardType = getKeyboardType(data.field)
    // number data does not have an intrinsic character limit, it depends on the string interpretation, but UI should limit it?
    // this limit would be observed before data field checks since it is forced inside BaseInput value changes
    val maxCharacters = when (data.field) {
        is UserFieldInteger -> Int.MIN_VALUE.toString().length // would negative be longest or do we need to max both ends? should it be based on field min/maxValue?
        is UserFieldNumber<*> -> 65 // non-integer values could get interesting, but is there a reasonable length?
        else -> Int.MAX_VALUE // defer to data validation
    }
    BaseInput(
        value = text,
        onValueChange = {
            val newText = data.setNewValue(it.text)
            if (newText != null) text = it
        },
        modifier = modifier.testTag(data.testTag),
        enabled = enabled,
        label = data.field.label,
        placeholder = data.field.hint,
        trailingIcon = { if (data.field.isPassword()) InputHideToggle(hideValue, { hideValue = !hideValue }) },
        isError = !data.validity.value.isValid(),
        visualTransformation = transform,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, autoCorrect = false),
        keyboardActions = KeyboardActions(onDone = { data.callDone() }),
        maxCharacters = maxCharacters,
        errorText = errorText,
        asyncState = when (data.validity.value) {
            UserFieldValidity.VALIDATING -> AsyncUiState.Waiting
            UserFieldValidity.VALIDATED -> AsyncUiState.Success
            else -> AsyncUiState.Unknown
        },
        onFocusLoss = { data.checkValidity() },
        fieldInputOptions = fieldInputOptions
    )
}
