package com.jht.applibrary

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.jht.resources.R
import com.jht.theme.DynamicTheme
import com.jht.theme.darkDynamicTheme
import com.jht.ui.Device22inPreviews
import com.jht.widgets.LockedOverlay
import com.jht.widgets.scaleSize

@Composable
fun AppButton(
    title: String,
    icon: Drawable,
    modifier: Modifier = Modifier,
    testID: String = "",
    locked: Boolean = false,
    imageCornerRadius: Dp = 0.dp,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .testTag("AppIcon:$testID:Button") // should we pass appId and truncate it to the test tag?,
            .clickable { onClick() },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LockedOverlay(
            isLocked = locked,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            lockTestTag = "AppIcon:$testID:Lock",
        ) {
            val shape = RoundedCornerShape(imageCornerRadius)
            Image(
                painter = rememberDrawablePainter(drawable = icon),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .border(1.dp, DynamicTheme.colors.gray3, shape)
                    .clip(shape),
                contentScale = ContentScale.FillBounds,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center, // Centers both vertically & horizontally
        ) {
            Text(
                title,
                color = DynamicTheme.colors.gray13,
                fontSize = 14.sp.scaleSize(),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp.scaleSize(),
                modifier = Modifier
                    .testTag("AppIcon:$testID:Text")
                    .fillMaxWidth(),
            )
        }
    }
}

//region PREVIEWS
@Device22inPreviews
@Composable
private fun AppButtonPreview() {
    val icon = AppCompatResources.getDrawable(LocalContext.current, R.drawable.ic_app_button_x)
    DynamicTheme(darkDynamicTheme) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(200.dp)
        ) {
            AppButton(
                title = "Twitter",
                icon = icon!!,
                modifier = Modifier.fillMaxSize(),
            ) {
            }
        }
    }

}

@Device22inPreviews
@Composable
private fun AppButtonLockPreview() {
    val icon = AppCompatResources.getDrawable(LocalContext.current, R.drawable.ic_app_button_x)
    DynamicTheme(darkDynamicTheme) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(200.dp)
        ) {
            AppButton(
                title = "Twitter",
                icon = icon!!,
                locked = true,
                modifier = Modifier.fillMaxSize(),
            ) {
            }
        }
    }

}
//endregion PREVIEWS
