package com.jht.spinbikeapps.targettraining.common


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.spinbikeapps.R
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TEXT_TITLE_PRIMARY
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.dpToPx

@Composable
internal fun TargetTrainingToggleButton(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.know_your_ftp_tc),
    selectedMode: UserFtpMode,
    onModeChanged: (UserFtpMode) -> Unit,
    ) {
    val userFtpKnown = stringResource(R.string.yes_tc)
    val userFtpUnknown = stringResource(R.string.no_tc)
    val selectedColor = DynamicTheme.colors.primary
    val unselectedColor = DynamicTheme.colors.gray13
    val stroke = DynamicTheme.borders.pillbox.width.value
    val cornerRadius = 12.dp.dpToPx()
    val baseTag = "TargetTrainingToggleButton"
    Column(
        modifier = modifier
            .wrapContentSize()
            .semantics {
                testTagsAsResourceId = true
            }
    ) {
        Text(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp.computeSize(1920f),
                    vertical = 6.dp.computeSize(1920f)
                )
                .testTag("$baseTag:Label:Text"),
            text = title,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 18.sp.computeSize(1920f),
                color = TEXT_TITLE_PRIMARY
            ),
        )
        Row(
            modifier = Modifier
                .padding(start = 8.dp.computeSize(1920f))
                .height(64.dp.computeSize(1920f))
                .width(347.dp.computeSize(1920f))
                .clip(RoundedCornerShape(cornerRadius)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clickable {
                        onModeChanged(UserFtpMode.UserFtpKnown)
                    }
                    .background(DynamicTheme.colors.gray1)
                    .fillMaxHeight()
                    .weight(1f)
                    .drawBehind {
                        val borderColor =
                            if (selectedMode == UserFtpMode.UserFtpKnown) selectedColor else unselectedColor
                        drawArc(
                            color = borderColor,
                            startAngle = 180f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = Size(cornerRadius * 2, cornerRadius * 2),
                            topLeft = Offset(0f, 0f),
                            style = Stroke(width = stroke)
                        )
                        drawArc(
                            color = borderColor,
                            startAngle = 90f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = Size(cornerRadius * 2, cornerRadius * 2),
                            topLeft = Offset(0f, size.height - cornerRadius * 2),
                            style = Stroke(width = stroke)
                        )
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = stroke
                        )
                        drawLine(
                            color = selectedColor,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke
                        )
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = stroke
                        )

                        drawLine(
                            color = borderColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke
                        )
                    }
                    .testTag("TargetTrainingToggleButton:$userFtpKnown"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userFtpKnown,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 22.sp.computeSize(1920f),
                        color = if (selectedMode == UserFtpMode.UserFtpKnown) selectedColor else unselectedColor
                    ),
                    modifier = Modifier.testTag("$baseTag:Label:Text"),
                )
            }

            Box(
                modifier = Modifier
                    .clickable {
                        onModeChanged(UserFtpMode.UserFtpUnknown)
                    }
                    .background(DynamicTheme.colors.gray1)
                    .fillMaxHeight()
                    .weight(1f)
                    .drawBehind {
                        val borderColor =
                            if (selectedMode == UserFtpMode.UserFtpUnknown) selectedColor else unselectedColor
                        drawArc(
                            color = borderColor,
                            startAngle = 270f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = Size(cornerRadius * 2, cornerRadius * 2),
                            topLeft = Offset(size.width - cornerRadius * 2, 0f),
                            style = Stroke(width = stroke)
                        )
                        drawArc(
                            color = borderColor,
                            startAngle = 0f,
                            sweepAngle = 90f,
                            useCenter = false,
                            size = Size(cornerRadius * 2, cornerRadius * 2),
                            topLeft = Offset(
                                size.width - cornerRadius * 2,
                                size.height - cornerRadius * 2
                            ),
                            style = Stroke(width = stroke)
                        )

                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = stroke
                        )

                        drawLine(
                            color = borderColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke
                        )
                        drawLine(
                            color = borderColor,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = stroke
                        )
                    }
                    .testTag("CustomToggleButton:$userFtpUnknown"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userFtpUnknown,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 22.sp.computeSize(1920f),
                        color = if (selectedMode == UserFtpMode.UserFtpUnknown) selectedColor else unselectedColor
                    ),
                    modifier = Modifier.testTag("$baseTag:Label:Text"),
                )
            }
        }
    }
}

@AllDevicePreviews
@Composable
internal fun TargetTrainingToggleButtonPreview(){
    TargetTrainingToggleButton(
        onModeChanged = {},
        modifier = Modifier,
        title = "",
        selectedMode = UserFtpMode.UserFtpUnknown
    )
}

internal enum class UserFtpMode {
    UserFtpKnown ,
    UserFtpUnknown
}



