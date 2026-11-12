package com.jht.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jht.enums.RelativeLocation
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.ui.pxToDp
import kotlinx.coroutines.delay

// Note: Hopefully creative can design a generic design so we don't have to distinguish between apple, samsung, or other watches
@Composable
fun SmartWatchPopup(
    isApple: Boolean,
    onClose: () -> Unit = {},
    resourceId: Int = 0,
    timeoutMilliseconds: Long = 0,
    readerLocation: RelativeLocation = RelativeLocation.NONE,
    gymKitLevel: Int = 0,
) {
    LaunchedEffect(timeoutMilliseconds) {
        if (timeoutMilliseconds > 0) {
            delay(timeoutMilliseconds)
            onClose()
        }
    }
    PopupFullscreen(onDismissRequest = onClose) {
        SmartWatchPopupBase(onClose) {
            SmartWatchPopUpContent(isApple, resourceId, readerLocation, gymKitLevel)
        }
    }
}

@Composable
private fun SmartWatchPopupBase(
    onClose: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    // whole screen click to close pop up
    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xcc000000))
    ) {
        // actual pop up box
        Box(modifier = Modifier
            .fillMaxWidth(650 / 1366f)
            .fillMaxHeight(500 / 768f)
            .align(Alignment.Center))
        {
            BlurBackgroundOverlay(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                showCloseButton = true,
                onCloseButtonClick = onClose
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
}

@Composable
private fun ColumnScope.SmartWatchPopUpContent(
    isApple: Boolean,
    resourceId: Int,
    readerLocation: RelativeLocation = RelativeLocation.NONE,
    gymKitLevel: Int = 0,
) {
    val extraNote = if (isApple) getAppleNoteTextId(gymKitLevel) else ""
    // if no valid image resource, use a generic image
    val adjustedResourceId = if (resourceId == 0) {
        if (isApple) {
            R.drawable.ic_help_hand_apple_watch
        } else {
            R.drawable.ic_help_hand_galaxy_watch
        }
    } else resourceId

    Spacer(Modifier.size(48.dp))

    Text(
        text = stringResource(
            id = if (isApple) {
                getAppleConnectTextId(gymKitLevel)
            } else {
                R.string.connect_watch_samsung_tc
            }
        ),
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .wrapContentHeight()
            .align(Alignment.CenterHorizontally)
            .testTag("SmartWatch:Dialog:Title:Text"),
        color = DynamicTheme.colors.text1,
        style = MaterialTheme.typography.headlineMedium.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    )

    Spacer(Modifier.size(24.dp))

    Text(
        text = if (isApple) {
                getAppleInstructionTextId(gymKitLevel, readerLocation)
            } else {
                stringResource(R.string.samsung_help_pg)
            },
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .wrapContentHeight()
            .align(Alignment.CenterHorizontally)
            .testTag("SmartWatch:Dialog:Body:Text"),
        color = DynamicTheme.colors.text1,
        style = MaterialTheme.typography.bodyMedium.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal
        ) // Comp says body large, but this follows the look of the comp more closely
    )

    // GymKit 2 comp features a small italic note
    if (extraNote.isNotEmpty()) {
        Spacer(Modifier.size(12.dp))
        Text(
            text = extraNote,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .wrapContentHeight()
                .align(Alignment.CenterHorizontally)
                .testTag("SmartWatch:Dialog:Note:Text"),
            color = DynamicTheme.colors.text1,
            style = MaterialTheme.typography.labelSmall.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Italic,
            )
        )
    }

    Spacer(Modifier.size(24.dp))

    Image(
        painter = painterResource(
            adjustedResourceId
        ),
        contentDescription = null,
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .aspectRatio(688 / 500f)
            .align(Alignment.CenterHorizontally)
            .testTag("SmartWatch:Dialog:Help:Image"),
    )

    Spacer(Modifier.size(25.dp))

    Image(
        painter = painterResource(
            if (isApple) getAppleBadgeImageId(gymKitLevel)
            else R.drawable.ic_samsung_watch
        ),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth(244 / 1080f)
            .aspectRatio(3.596491f) // aspect from comps
            .defaultMinSize(minHeight = 40.pxToDp()) // Apple requires minimum of 40 px
            .align(Alignment.CenterHorizontally)
            .testTag("SmartWatch:Dialog:Badge:Image"),
    )
}

@AllDevicePreviews
@Composable
private fun PreviewSmartWatchPopupGeneric() {
    SmartWatchPopupBase {
        SmartWatchPopUpContent(true, 0)
    }
}

@AllDevicePreviews
@Composable
private fun PreviewSmartWatchPopupFrameSpecific() {
    SmartWatchPopupBase {
        SmartWatchPopUpContent(true, R.drawable.connect_watch_climbmill_p1)
    }
}
@AllDevicePreviews
@Composable
private fun PreviewSmartWatchPopupGymKitNext() {
    SmartWatchPopupBase {
        SmartWatchPopUpContent(true, R.drawable.connect_watch_climbmill_p1, RelativeLocation.BOTTOM_MIDDLE, 2)
    }
}

// Preview to check strings longer than English
@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape", locale="ru")
@Composable
private fun PreviewSmartWatchPopupFrameSpecificRussian() {
    SmartWatchPopupBase {
        SmartWatchPopUpContent(true, R.drawable.connect_watch_climbmill_p1)
    }
}