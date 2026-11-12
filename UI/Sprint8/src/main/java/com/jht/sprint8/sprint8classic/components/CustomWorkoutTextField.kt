package com.jht.sprint8.sprint8classic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.CUSTOM_WORKOUT_TEXT_FIELD_BORDER_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_DISABLED_BTN_COLOR
import com.jht.sprint8.utils.Sprint8Colors.SPRINT8_DISABLED_BTN_CONTENT_COLOR
import com.jht.ui.computeSize

@Composable
fun CustomWorkoutTextField(
    modifier: Modifier = Modifier,
) {
    val text = ""
    val searchTextFieldValue = remember { mutableStateOf(TextFieldValue(text)) }
    val isFocused = remember { mutableStateOf(false) }
    BasicTextField(
        value = searchTextFieldValue.value,
        onValueChange = { searchTextFieldValue.value = it },
        textStyle = TextStyle(
            fontSize = 22.sp.computeSize(1920f),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        ),
        modifier = modifier
            .height(56.dp.computeSize(1920f))
            .border(
                width = if (isFocused.value) {
                    2.dp.computeSize(1920f)
                } else 0.dp,
                color = if (isFocused.value) {
                    CUSTOM_WORKOUT_TEXT_FIELD_BORDER_COLOR
                } else Color.Unspecified,
                shape = RoundedCornerShape(12.dp.computeSize(1920f))
            )
            .background(
                if (!isFocused.value) {
                    SPRINT8_DISABLED_BTN_CONTENT_COLOR
                } else {
                    Color.Unspecified
                },
                shape = RoundedCornerShape(12.dp.computeSize(1920f))
            )
            .clickable { isFocused.value = true }
            //.focusRequester(focusRequester)
            .onFocusChanged { state ->
                isFocused.value = state.hasFocus
                if (state.hasFocus) {
                    searchTextFieldValue.value = searchTextFieldValue.value.copy(
                        selection = TextRange(
                            0,
                            searchTextFieldValue.value.text.length
                        )
                    )
                }
            },
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 16.dp.computeSize(1920f)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (searchTextFieldValue.value.text.isEmpty()) {
                    Text(
                        modifier = Modifier.wrapContentSize(),
                        text = stringResource(R.string.enter_your_workout_pg),
                        style = TextStyle(
                            fontSize = 22.sp.computeSize(1920f),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                innerTextField()
                if (!isFocused.value) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_box),
                        contentDescription = null,
                        tint = SPRINT8_DISABLED_BTN_COLOR,
                        modifier = Modifier
                            .padding(start = 16.dp.computeSize(1920f), end = 16.dp.computeSize(1920f))
                            .size(32.dp.computeSize(1920f))
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text
        ),
    )
}