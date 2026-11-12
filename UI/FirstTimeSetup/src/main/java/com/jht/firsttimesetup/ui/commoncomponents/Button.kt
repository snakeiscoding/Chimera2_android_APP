package com.jht.firsttimesetup.ui.commoncomponents

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.firsttimesetup.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


enum class FTSUButtonType {
    Primary,
    Secondary,
    Tertiary,
    Disabled
}

@Composable
fun FTSUButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    label: String,
    buttonType: FTSUButtonType = FTSUButtonType.Primary,
    imageStart: @Composable ((modifier: Modifier) -> Unit)? = null,
    imageEnd: @Composable ((modifier: Modifier) -> Unit)? = null,
    textSize: TextStyle = MaterialTheme.typography.titleMedium

) {

    var isEnabled by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val labelColor = when (buttonType) {
        FTSUButtonType.Primary -> colorResource(R.color.dark_button_text)
        FTSUButtonType.Secondary -> colorResource(R.color.light_button_text)
        FTSUButtonType.Tertiary -> colorResource(R.color.light_button_text)
        FTSUButtonType.Disabled -> colorResource(R.color.light_button_text)
    }

    if (buttonType == FTSUButtonType.Primary || buttonType == FTSUButtonType.Secondary) {

        val btnColor = when (buttonType) {
            FTSUButtonType.Primary -> colorResource(R.color.primary_btn)
            FTSUButtonType.Secondary -> colorResource(R.color.secondary_btn)
            else -> colorResource(R.color.primary_btn)
        }

        Button(// primary/ secondary
            onClick = {
                if (isEnabled) {
                    isEnabled = false
                    onClick()
                }
                scope.launch {
                    delay(1.seconds)
                    isEnabled = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                btnColor,
                disabledContainerColor = colorResource(R.color.blue_4)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.height(intrinsicSize = IntrinsicSize.Max)
        ) {
            Row(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .height(IntrinsicSize.Max)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (imageEnd != null && imageStart == null) {
                    Spacer(Modifier.width(40.dp))
                }
                imageStart?.let {
                    imageStart(Modifier.size(32.dp))
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    label,
                    modifier = Modifier.width(IntrinsicSize.Max).height(IntrinsicSize.Max) ,
                    color = labelColor, style = textSize, textAlign = TextAlign.Center,
                )

                imageEnd?.let {
                    Spacer(Modifier.width(8.dp))
                    imageEnd(Modifier.size(32.dp))
                }
            }
        }
    } else if (buttonType == FTSUButtonType.Tertiary) { //tertiary
        TextButton(modifier = modifier.width(IntrinsicSize.Min), onClick = onClick) {
            Column(
                modifier = modifier.width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier.width(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    imageStart?.let {
                        imageStart(Modifier.size(32.dp))
                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        label,
                        color = labelColor,
                        style = textSize,
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .padding(horizontal = 3.dp),
                        maxLines = 1
                    )

                    imageEnd?.let {
                        Spacer(Modifier.width(8.dp))
                        imageEnd(Modifier.size(32.dp))
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    val y = size.height / 2
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    } else if (buttonType == FTSUButtonType.Disabled) {
        val buttonColor = colorResource(R.color.blue_4)
        Button(// Disabled
            onClick = {},
            enabled = false,
            colors = ButtonDefaults.buttonColors(disabledContainerColor = buttonColor.copy(.3F)),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.width(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (imageEnd != null && imageStart == null) {
                    Spacer(Modifier.width(40.dp))
                }
                imageStart?.let {
                    imageStart(Modifier.size(32.dp))
                    Spacer(Modifier.width(8.dp))
                }

                Text(label, color = Color.Black, style = textSize)

                imageEnd?.let {
                    Spacer(Modifier.width(8.dp))
                    imageEnd(Modifier.size(32.dp))
                }
            }
        }
    }
}


@Preview
@Composable
private fun FTSUButtonPreviewBlue() {
    FTSUButton(
        label = "Network",
        modifier = Modifier.width(250.dp)
    )
}

@Preview
@Composable
private fun FTSUButtonPreviewGray() {
    FTSUButton(
        label = "Network",
        modifier = Modifier.width(250.dp),
        buttonType = FTSUButtonType.Secondary
    )
}

@Preview
@Composable
private fun FTSUButtonPreviewBlueRightImage() {
    FTSUButton(
        label = "Network",
        modifier = Modifier.width(250.dp),
        imageEnd = {
            Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, "", tint = Color.Black)
        }
    )
}

@Preview
@Composable
private fun FTSUTertiaryButton() {
    FTSUButton(
        label = "Skip First Time Setup",
        modifier = Modifier,
        buttonType = FTSUButtonType.Tertiary
    )
}

@Preview
@Composable
private fun FTSUDisabledButton() {
    FTSUButton(
        label = "Disabled Button",
        modifier = Modifier.width(250.dp),
        buttonType = FTSUButtonType.Disabled
    )
}