package com.jht.widgets

import android.view.ViewTreeObserver
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jht.resources.Error
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeSpinner
import com.jht.widgets.helpers.AsyncUiState


/** Monitor if keyboard is being shown */
@Composable
fun rememberKeyboardState(): State<Boolean> {
    // concept from https://github.com/FRbrt/Compose-Keyboard-Covers-TextField-Demo/blob/main/KeyboardHidesTextField/app/src/main/java/edu/keyboardhidestextfield/ImeState.kt
    // some android version may need more? https://stackoverflow.com/a/66805360
    val keyboardState = remember { mutableStateOf(false) }
    val view = LocalView.current
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val isKeyboardOpen = ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
            keyboardState.value = isKeyboardOpen
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    return keyboardState
}


/** BaseInput tries to provide the most basic input box based on the creative comps */
@Composable
fun BaseInput(
    value: TextFieldValue, // we use this instead of string so we can include cursor position
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: String? = null, // if null omit label entirely, otherwise "" will reserve space for one
    placeholder: String = "", // Composable in original TextField
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (RowScope.() -> Unit)? = null, // we add row scope cause we insert error icon
    //prefix: @Composable (() -> Unit)? = null, // THIS might come up later, seems like we have some SN inputs with prefixes
    //suffix: @Composable (() -> Unit)? = null,
    //supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false, // can we handle this with errorText? what if there is error and no message, errorText=null? easier to separate them?
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    // stuff we added
    maxCharacters: Int = Int.MAX_VALUE, // normally InputData might handle this, but if someone wants to use BaseInput in another capacity, it's there
    errorText: String = "",
    asyncState: AsyncUiState = AsyncUiState.Unknown,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
    onFocusLoss: () -> Unit = {}, // take care of boilerplate for the caller
) {
    val lastFocus = remember { mutableStateOf(false) } // workaround initial focus loss during composition
    val showError = isError || asyncState == AsyncUiState.Failure // should caller do this, or maybe exposing isError directly causes confusion?

    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        if (label != null) {
            Text(
                label,
                style = fieldInputOptions.labelStyle,
                modifier = Modifier.padding(fieldInputOptions.labelPaddingValues)
            )
        }
        // maybe we need to consider using BasicTextField and overriding decoration with something custom for greater control?
        TextField(
            value = value,
            onValueChange = { if (it.text.length <= maxCharacters) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    // should we just add a textModifier parameter?
                    if (fieldInputOptions.minHeight != null) Modifier.defaultMinSize(minHeight = fieldInputOptions.minHeight) else Modifier
                )
                .onFocusChanged {
                    if (!it.hasFocus && lastFocus.value) onFocusLoss()
                    lastFocus.value = it.hasFocus
                }
                .then(
                    if (fieldInputOptions.borderStroke != null) {
                        Modifier.border(
                            width = fieldInputOptions.borderStroke.width,
                            color = if (lastFocus.value) DynamicTheme.colors.buttonPrimaryDefault
                            else if (isError) DynamicTheme.colors.stop
                            else DynamicTheme.colors.textFieldStroke,
                            shape = fieldInputOptions.shape
                        ).clip(shape = fieldInputOptions.shape)
                    } else Modifier
                )
                .padding(fieldInputOptions.contentPaddingValues),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = fieldInputOptions.textStyle,
            label = null,
            placeholder = {
                Text(
                    placeholder,
                    style = fieldInputOptions.placeholderStyle
                ) // if placeholder style is different than value style, it might align to the top
            },
            leadingIcon = leadingIcon,
            trailingIcon = {
                Row {
                    when {
                        asyncState == AsyncUiState.Waiting -> ThemeSpinner(
                            Modifier
                                .size(20.dp)
                                .align(CenterVertically)
                        )

                        isError || asyncState == AsyncUiState.Failure -> {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                Modifier.align(CenterVertically)
                                //tint = DynamicTheme.colors.error,
                            )
                        }

                        asyncState == AsyncUiState.Success -> Icon(
                            Icons.Filled.CheckCircle,
                            null,
                            Modifier.align(CenterVertically),
                            DynamicTheme.colors.success
                        )

                        else -> {}
                    }
                    if (trailingIcon != null) trailingIcon()
                }
            },
            supportingText = null,
            isError = showError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactionSource,
            shape = fieldInputOptions.shape,
            colors = fieldInputOptions.textFieldColors,
        )

        Text(
            if (showError) errorText else "",
            style = fieldInputOptions.supportTextStyle,
            modifier = Modifier.padding(fieldInputOptions.supportTextPaddingValues),
            color = DynamicTheme.colors.stop
        )
    }
}


/** Your basic show/hide toggle */
@Composable
fun InputHideToggle(
    hideValue: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable { onToggle() }
            .padding(10.dp)
    ) {
        Icon(
            modifier = Modifier.height(20.dp),
            painter = painterResource(id = if (hideValue) R.drawable.ic_show else R.drawable.ic_hide),
            contentDescription = null,
        )
    }
}


class DropdownInputItem(
    val value: Any, // Any can be troublesome, should we be abstract and force creation of derived class overriding value with strong type?
    val displayName: String, // localized string for display in UI
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownInput(
    modifier: Modifier,
    label: String,
    dropDownChoices: Iterable<DropdownInputItem>,
    value: DropdownInputItem,
    enabled: Boolean = true,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
    onValueChange: (DropdownInputItem)->Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded && enabled,
        onExpandedChange = {
            if (enabled) {
                expanded = !expanded
            }
        }
    ) {
        BaseInput(
            value = TextFieldValue(value.displayName),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxSize()
                .menuAnchor(),
            label = label,
            enabled = enabled,
            fieldInputOptions = fieldInputOptions
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(
                    DynamicTheme.colors.textFieldBackgroundDefault
                )
                .clip(DynamicTheme.corners.medium.makeCornerShape())
                .border(
                    BorderStroke(1.dp, DynamicTheme.colors.primary),
                    shape = DynamicTheme.corners.medium.makeCornerShape()
                ),
        ) {
            dropDownChoices.forEach { item ->
                DropdownMenuItem(
                    modifier = Modifier.padding(horizontal = 2.dp).background(if (value == item) DynamicTheme.colors.primary else DynamicTheme.colors.textFieldBackgroundDefault),
                    text = {
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (value == item) DynamicTheme.colors.text2 else DynamicTheme.colors.text1
                        )
                    },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = DynamicTheme.colors.text1
                    )
                )
            }
        }
    }
}
