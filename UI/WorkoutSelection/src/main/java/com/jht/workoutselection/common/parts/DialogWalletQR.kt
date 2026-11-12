package com.jht.workoutselection.common.parts

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jht.theme.DynamicTheme
import com.jht.theme.themeButtonBorder
import com.jht.ui.AllDevicePreviews
import com.jht.ui.clickableNoEffect
import com.jht.widgets.PanelOperationMode
import com.jht.widgets.PopupFullscreen
import com.jht.workoutselection.R


// Note: Quickly copied and modified SmartWartPopup.kt composable
/** Dialog popup for wallet setup */
@Composable
internal fun DialogWalletQR(
    bitmap: Bitmap? = null,
    onClose: () -> Unit = {},
) {
    PopupFullscreen(
        panelOperationMode = PanelOperationMode.Inplace,
        onDismissRequest = onClose,
    ) {
        DialogWalletQRInternal(bitmap, onClose)
    }
}

/** Internals of popup, separated for easier previewing */
@Composable
private fun DialogWalletQRInternal(
    bitmap: Bitmap? = null,
    onClose: () -> Unit = {},
) {
    DialogWalletQRBase(onClose) {
        DialogWalletQRContent(
            bitmap = bitmap
        )
    }
}

@Composable
private fun DialogWalletQRBase(
    onClose: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    // whole screen click to close pop up
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xcc000000))
        .clickable { onClose() }) {
        // actual pop up box
        Box(
            modifier = Modifier
                .fillMaxWidth(650 / 1366f)
                .fillMaxHeight(500 / 768f)
                .align(Alignment.Center)
                .border(themeButtonBorder, MaterialTheme.shapes.medium)
                .clip(MaterialTheme.shapes.medium)
                .background(DynamicTheme.colors.gray1.copy(alpha = 0.9f))
                .padding(10.dp)
                .clickableNoEffect {}
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close), contentDescription = null,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(45 / 900f)
                    .aspectRatio(1f)
                    .align(Alignment.TopEnd)
                    .clickable { onClose() },
                tint = DynamicTheme.colors.text1,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(.9f)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ColumnScope.DialogWalletQRContent(bitmap: Bitmap?) {
    // Header text
    Text(
        text = stringResource(R.string.login_with_your_phone_tc),
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .align(Alignment.CenterHorizontally),
        color = Color.White,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium,
        maxLines = 2,
    )
    // Instruction text
    Text(
        text = stringResource(R.string.login_with_your_phone_pg),
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .align(Alignment.CenterHorizontally),
        color = Color.White,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 2,
    )
    // QR + Badges row
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(48.dp)
            .weight(1f)
            .align(Alignment.CenterHorizontally),
    ) {
        // QR
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                )
            } else {
                LoadingSpinner()
            }
        }
        // Badges
        Column(
            Modifier
                .weight(1f)
                .padding(10.dp)
        ) {
            // Apple wallet
            Image(
                painter = painterResource(R.drawable.ic_add_to_apple_wallet),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp)
                    .fillMaxWidth(0.75f)
                    .aspectRatio(3.596491f) // aspect from comps
                    .align(Alignment.CenterHorizontally),
            )
            // Google wallet
            Image(
                painter = painterResource(R.drawable.ic_add_to_google_wallet),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 10.dp)
                    .fillMaxWidth(0.75f)
                    .aspectRatio(3.596491f) // aspect from comps
                    .align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun LoadingSpinner() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "Spinner"
    )

    Icon(
        modifier = Modifier
            .padding(top = 55.dp, bottom = 25.dp)
            .fillMaxSize(100 / 900f)
            .aspectRatio(1f)
            .rotate(angle),
        painter = painterResource(id = R.drawable.ic_spinner),
        contentDescription = null,
        tint = DynamicTheme.colors.text1
    )
}

@AllDevicePreviews
@Composable
fun PreviewDialogWalletQR() {
    DynamicTheme {
        DialogWalletQRInternal()
    }
}
