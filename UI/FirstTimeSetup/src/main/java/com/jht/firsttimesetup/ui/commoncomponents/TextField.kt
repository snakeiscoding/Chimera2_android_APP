package com.jht.firsttimesetup.ui.commoncomponents

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.firsttimesetup.R
import com.jht.ui.computeSize


@Composable
fun FTSUTextField(
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    label: String,
    text: String,
    onTextChange: (newText: String) -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
    testTag: String,
    keyboardActions: KeyboardActions = KeyboardActions(onDone = {}),
    borderColor: Color = colorResource(R.color.selected_color),
    bottomMessage: String? = null,
    bottomMessageColor: Color = colorResource(R.color.light_text),
    placeholderString: String = "",
) {
    Column(
        modifier = modifier.height(IntrinsicSize.Max),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            label,
            color = Color.White,
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp)
                .testTag("$testTag:Text"),
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(Modifier.height(4.dp.computeSize()))

        TextField(
            value = text,
            onValueChange = { onTextChange(it) },
            maxLines = 1,
            textStyle = MaterialTheme.typography.labelLarge.copy(
                color = colorResource(R.color.light_text),
                textAlign = TextAlign.Start,
                fontSize = 20.sp
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = textFieldModifier
                .testTag("$testTag:TextField")
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .border(
                    width = 1.dp,
                    shape = RoundedCornerShape(4.dp),
                    color = borderColor
                ),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = colorResource(R.color.language_card_color),
                unfocusedSupportingTextColor = colorResource(R.color.light_text),
                focusedTextColor = colorResource(R.color.light_text),
                focusedContainerColor = colorResource(R.color.language_card_color),
            ),
            placeholder = {
                Text(
                    placeholderString,
                    color = colorResource(R.color.med_text),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colorResource(R.color.light_text),
                        textAlign = TextAlign.Start,
                        fontSize = 20.sp
                    )
                )
            }
        )

        Spacer(Modifier.height(4.dp.computeSize()))

        bottomMessage?.let {
            Text(
                it,
                color = bottomMessageColor,
                modifier = Modifier
                    .padding(start = 8.dp.computeSize(), end = 8.dp.computeSize())
                    .testTag("$testTag:BottomText"),
                style = MaterialTheme.typography.labelSmall
            )
        }

    }
}