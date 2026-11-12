package com.jht.progression.ui.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.progression.oxaniumStyle
import com.jht.progression.ui.MmColors
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.scaleSize


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MmTextField(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit = {},
    label: String = "",
    hint: String = "",
    isInvalid: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val hintColor = if (isInvalid && text.isNotEmpty()) MmColors.missedRed else Color.White
    val borderColor = when {
        !isInvalid -> MmColors.futuristicBlue
        isInvalid && text.isNotEmpty() -> MmColors.missedRed
        else -> MmColors.futuristicBlue.copy(alpha = 0.5f)
    }

    // Ensure text being typed in isn't covered by keyboard
    LaunchedEffect(key1 = text) { bringIntoViewRequester.bringIntoView() }

    Column(modifier = modifier.bringIntoViewRequester(bringIntoViewRequester)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RectangleShape
                )
                .drawBehind {
                    val strokeWidth = 3.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
                .padding(
                    horizontal = 20.dp.scaleSize(),
                    vertical = if (text.isNotEmpty()) 20.dp.scaleSize() else 25.dp.scaleSize()
                )
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = oxaniumStyle.copy(
                    color = Color.White,
                    fontSize = 26.sp.scaleSize(),
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                }),
                visualTransformation = visualTransformation,
                cursorBrush = SolidColor(Color.Unspecified),
                decorationBox = { innerTextField ->
                    Column {
                        Text(
                            text = label,
                            style = oxaniumStyle.copy(
                                fontSize = 21.sp.scaleSize(),
                                color = if (text.isEmpty()) Color.White else Color.White.copy(alpha = 0.5f),
                                fontWeight = if (text.isEmpty()) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                        if (text.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(5.dp.scaleSize()))
                            innerTextField()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused) {
                            keyboardController?.show()
                        } else {
                            keyboardController?.hide()
                        }
                    }
            )
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp.scaleSize(),
                    vertical = 10.dp.scaleSize()
                ),
            text = hint,
            color = hintColor,
            style = oxaniumStyle.copy(fontSize = 16.sp.scaleSize(), fontWeight = FontWeight.Medium),
            lineHeight = 24.sp.scaleSize()
        )
    }
}

@AllDevicePreviews
@Composable
private fun MmTextFieldPreview() {
    MmTextField(
        modifier = Modifier.width(480.dp.scaleSize()),
        text = "1234",
        label = "5K Time",
        hint = "*Your goal time must be within x minutes faster than your estimate.",
        isInvalid = false
    )
}