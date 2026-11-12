package com.jht.widgets

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeFlatButton
import com.jht.theme.ThemeFlatButtonText
import com.jht.theme.darkDynamicTheme
import com.jht.theme.disabledBackground
import com.jht.theme.lightDynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.Device15inPreviews
import com.jht.ui.clickableNoEffect


@Composable
fun ConfirmDialog(
    titleId: Int,
    messageId: Int,
    shownState: MutableState<Boolean>,
    onConfirm: () -> Unit,
) {
    if (shownState.value) MessageDialog(
        title = stringResource(titleId),
        message = stringResource(messageId),
        modifier = Modifier
            .fillMaxWidth(0.4f)
            .padding(20.dp),
        onDismiss = { shownState.value = false },
        okText = stringResource(R.string.confirm_tc),
        cancelText = stringResource(R.string.cancel_tc),
    ) {
        if (it) onConfirm()
        shownState.value = false
    }
}

@Composable
fun MessageDialog(
    title: String,
    message: String,
    modifier: Modifier = Modifier, // should we pull this and force all MessageDialog to conform to same rules? or do opposite and create MessageDialogOptions?
    onDismiss: ()->Unit = {},
    okText: String = "",
    cancelText: String = "",
    usePopupFullscreen: Boolean = true,
    onConfirm: ((Boolean)->Unit)? = null,
) {
    if(usePopupFullscreen) {
        PopupFullscreen(
            properties = PopupProperties(focusable = true), // block interacting with UI outside of popup until it is dismissed
            onDismissRequest = onDismiss,
        ) {
            InternalMessageDialog(
                title = title,
                message = message,
                modifier = modifier,
                onDismiss = onDismiss,
                okText = okText,
                cancelText = cancelText,
                onConfirm = onConfirm
            )
        }
    }
    else{
        InternalMessageDialog(
            title = title,
            message = message,
            modifier = modifier,
            onDismiss = onDismiss,
            okText = okText,
            cancelText = cancelText,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun InternalMessageDialog(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: ()->Unit = {},
    okText: String = "",
    cancelText: String = "",
    onConfirm: ((Boolean)->Unit)? = null,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(disabledBackground) // should we define a constant for disabled background somewhere? make it part of the theme?
            .clickableNoEffect(onClick = onDismiss) // consider interaction with UI outside of popup as a dismiss request
    ) {
        Box(
            Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(DynamicTheme.colors.gradient1.vertical())
                .align(Alignment.Center)
                .then(modifier),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (title.isNotEmpty()) Text(
                    title,
                    color = DynamicTheme.colors.text1,
                    style = DynamicTheme.typography.textStyle2,
                    textAlign = TextAlign.Center,
                )
                if (message.isNotEmpty()) Text(
                    message,
                    Modifier.padding(20.dp),
                    color = DynamicTheme.colors.text1,
                    style = DynamicTheme.typography.textStyle1,
                    textAlign = TextAlign.Center,
                )
                if (onConfirm != null) {
                    // use Row or our BoxGrid widget?
                    Row(
                        Modifier.height(IntrinsicSize.Min),
                    ) {
                        val localButtonModifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(20.dp)
                        ThemeFlatButton(
                            onClick = { onConfirm(false) },
                            localButtonModifier,
                            isPrimary = false
                        ) {
                            if (cancelText.isEmpty()) {
                                Icon(
                                    Icons.Outlined.Clear,
                                    "cancel",
                                    tint = DynamicTheme.colors.error
                                )
                            } else {
                                ThemeFlatButtonText(
                                    cancelText,
                                    false,
                                    DynamicTheme.typography.textStyle2
                                )
                            }
                        }
                        ThemeFlatButton(
                            onClick = { onConfirm(true) },
                            localButtonModifier,
                            isPrimary = true
                        ) {
                            if (okText.isEmpty()) {
                                Icon(
                                    Icons.Outlined.Check,
                                    "confirm",
                                    tint = DynamicTheme.colors.success
                                )
                            } else {
                                ThemeFlatButtonText(
                                    okText,
                                    true,
                                    DynamicTheme.typography.textStyle2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


//region PREVIEWS
@Device15inPreviews
@Composable
private fun PreviewMessagePopupLight() {
    Box(Modifier.fillMaxSize()) {
        DynamicTheme(lightDynamicTheme) {
            InternalMessageDialog(
                "Title Text",
                "Message text here.",
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1.8f),
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}

@Device15inPreviews
@Composable
private fun PreviewMessagePopupNoConfirm() {
    Box(Modifier.fillMaxSize()) {
        DynamicTheme(darkDynamicTheme) {
            InternalMessageDialog(
                "Title Text",
                "Message text here.",
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1.8f),
                onDismiss = {},
            )
        }
    }
}

@Device15inPreviews
@Composable
private fun PreviewMessagePopupRfid() {
    Box(Modifier.fillMaxSize()) {
        DynamicTheme(darkDynamicTheme) {
            InternalMessageDialog(
                title = "header",
                message = "message",
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1.8f),
                onDismiss = {  },
                okText = stringResource(id = R.string.pair_rfid_tag_tc),
                cancelText = stringResource(id = R.string.skip_tc),
                onConfirm = {  },
            )
        }
    }
}

@AllDevicePreviews
@Composable
private fun PreviewMessagePopupDark() {
    Box(Modifier.fillMaxSize()) {
        DynamicTheme(darkDynamicTheme) {
            InternalMessageDialog(
                "Title Text",
                "Message text here.",
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.4f)
                    .aspectRatio(1.8f),
                onDismiss = {},
                okText = "Yes",
                cancelText = "No",
                onConfirm = {},
            )
        }
    }
}
//endregion PREVIEWS
