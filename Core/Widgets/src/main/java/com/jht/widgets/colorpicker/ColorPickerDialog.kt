package com.jht.widgets.colorpicker

import android.graphics.Color.parseColor
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.ui.Device15inPreviews
import com.jht.ui.clickableNoEffect
import com.jht.widgets.DialogFullscreen
import com.jht.widgets.R

val colorPickerVisible = mutableStateOf(true)

fun setContent(
    composeView: ComposeView,
    color: Int,
    showPopup: Boolean,
    onColorChanged: (Int) -> Unit = {}
) {
    composeView.setContent {
        colorPickerVisible.value = showPopup
        ColorPickerPopup(color, colorPickerVisible, onColorChanged = onColorChanged)
    }
}

/**
 * Composable for the Color Picker
 */
@Composable
fun ColorPickerPopup(
    color: Int,
    colorPickerVisible: MutableState<Boolean>,
    onColorChanged: (Int) -> Unit = {}
) {
    if (colorPickerVisible.value) {
        DialogFullscreen (
            onDismissRequest = {
                colorPickerVisible.value = false
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xcc000000))
            )
            ColorPickerDialog(
                onClose = {
                    colorPickerVisible.value = false
                }, color = color, onColorChanged = onColorChanged
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColorPickerDialog(
    controller: ColorPickerController = rememberColorPickerController(),
    onClose: () -> Unit = {},
    color: Int,
    onColorChanged: (Int) -> Unit = {}
) {
    val colorInt = remember { mutableStateOf(color)}
    val colorHexString = remember { mutableStateOf(Integer.toHexString(color)) }
    // Added to allow keyboard to properly show for editing the hex value
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var updateColor = false
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClose)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(700 / 1366f)
                .fillMaxHeight(400 / 768f)
                .align(Alignment.Center)
                .background(DynamicTheme.colors.background1)
                .padding(10.dp)
                .clickableNoEffect {}
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close), contentDescription = null,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(40 / 900f)
                    .aspectRatio(1f)
                    .align(Alignment.TopEnd)
                    .clickable {
                        onClose()
                    },
                tint = DynamicTheme.colors.text1,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.select_color_tc),
                    modifier = Modifier
                        .testTag("ColorPicker:PageTitle:Text")
                        .fillMaxWidth(0.9f)
                        .padding(top = 15.dp, bottom = 15.dp)
                        .wrapContentHeight(),
                    color = DynamicTheme.colors.text1,
                    textAlign = TextAlign.Center,
                    style = DynamicTheme.typography.textStyle2,
                    lineHeight = 36.sp,
                    fontSize = 32.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(colorInt.value))
                            .padding(10.dp)
                    )
                    TextField(
                        value =  colorHexString.value,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .wrapContentHeight()
                            .padding(10.dp)
                            .focusable(true)
                            .focusRequester(focusRequester),
                        // Can show an icon or text field that isn't editable!
                        leadingIcon =
                        {
                            Text(
                                text = "#",
                                color = Color.Black,
                                modifier = Modifier.padding(start = 24.dp, end = 8.dp)
                            )
                        },
                        onValueChange = {
                            colorHexString.value = it
                            if (isValidColor("#$it")) {
                                colorInt.value = parseColor("#$it")
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }),
                    )
                    ThemeButton(
                        onClick = {
                            onColorChanged(colorInt.value)
                            onClose()
                        },
                        enabled = true,
                        contentPadding = PaddingValues(),
                        modifier = Modifier.width(60.dp)
                    ) {
                        Icon(
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .padding(5.dp),
                            painter = painterResource(id = R.drawable.ic_confirm),
                            contentDescription = null,
                            tint = DynamicTheme.colors.success,
                        )
                    }
                }
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(275.dp)
                        .padding(10.dp),
                    controller = controller,
                    onColorChanged = { colorEnvelope ->
                        if (updateColor) {
                            colorInt.value = colorEnvelope.color.toArgb()
                            colorHexString.value = Integer.toHexString(colorEnvelope.color.toArgb())
                        }
                        updateColor = true
                    }
                )
            }
        }
    }
}

private fun isValidColor(color: String): Boolean {
    return try {
        Color(parseColor(color))
        true
    } catch (ignored : Exception) {
        Log.w("ColorPickerDialog", "This color is not valid: $color")
        false
    }
}


@Device15inPreviews
@Composable
private fun PreviewColorPicker() {
    colorPickerVisible.value = true
    ColorPickerPopup(0xFFFF000, colorPickerVisible)
}