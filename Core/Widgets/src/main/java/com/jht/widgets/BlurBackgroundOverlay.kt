package com.jht.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize

/**
 * A reusable background overlay component with optional close button support.
 * Typically used as a blurred and dark-tinted background for vortex UI layers such as
 * login, register, or popup dialogs across the app.
 * @param shape Shape of the overlay background. Default is a 16dp rounded corner.
 * @param showCloseButton Whether to show a close (X) button on the top right. some screens don't use it
 * @param onCloseButtonClick Callback triggered when the close button is clicked.
 * @param content The content to be displayed inside the overlay.
 */
@Composable
fun BlurBackgroundOverlay(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp.computeSize()),
    showCloseButton:Boolean = false,
    showBackButton:Boolean = false,
    onCloseButtonClick: () -> Unit = {},
    onBackButtonClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Box {
        if (showCloseButton)
            CloseButton(modifier = Modifier.align(Alignment.TopEnd).zIndex(2f)) { onCloseButtonClick() }

        Column(
            modifier = modifier
                .padding(end = 14.dp.computeSize(), top = 14.dp.computeSize())
                .clip(shape = shape)
                .background(Color.Black.copy(alpha = 0.9f)) // will change it once efficient way of applying blur is figured out.
        ) {
            if (showBackButton)
                Row(modifier=Modifier.padding(top = 24.dp.computeSize(), start = 24.dp.computeSize()).clickable { onBackButtonClick() },verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.caret_left),
                        contentDescription = "back",
                        tint = Color.White
                    )
                    Spacer(Modifier.padding(end = 4.dp))
                    Text(
                        text = stringResource(R.string.back_tc),
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

            content()
        }
    }
}

/**
 * CloseButton composable typically used in popups, dialogs, or overlay components.
 * This button displays a circular icon (usually a close or cancel icon) with customizable styling
 * such as size, shape, background color, border, and icon tint. It triggers the provided `onClick`
 * callback when tapped.
 * For now integrating it inside the [BlurBackgroundOverlay] component.
 */
@Composable
private fun CloseButton(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp.computeSize(),
    shape: RoundedCornerShape = RoundedCornerShape(30.dp.computeSize()),
    backgroundColor:Color = DynamicTheme.colors.gray1,
    iconRes: Int = R.drawable.ic_close,
    border: BorderStroke = BorderStroke(1.dp, Color.White),
    drawableTint: Color = Color.White,
    iconSize: Dp = 18.dp.computeSize(),
    onClick: () -> Unit
) {

    Box(
        modifier = modifier
            .size(size)
            .clip(shape = shape)
            .background(backgroundColor)
            .border(border, shape)
            .clickable { onClick() }
    ) {

        Icon(
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.Center),
            painter = painterResource(iconRes),
            contentDescription = "close icon",
            tint = drawableTint
        )

    }

}

@AllDevicePreviews
@Composable
private fun BlurCardBackgroundComponentPreview() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BlurBackgroundOverlay(
            modifier = Modifier
                .wrapContentSize()
                .fillMaxWidth(0.5f)
                .fillMaxHeight(0.5f)
        ) {
            Box(Modifier.background(Color.Black))
        }
    }
}