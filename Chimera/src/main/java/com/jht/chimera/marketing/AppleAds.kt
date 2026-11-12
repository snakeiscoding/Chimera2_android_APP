package com.jht.chimera.marketing

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.jht.chimera.R
import com.jht.chimera.lib.common.getSDCardUri
import com.jht.data.AdHostController
import com.jht.data.FeaturedWorkoutAd
import com.jht.data.MarketingAd
import com.jht.data.MockAdHostController
import com.jht.theme.DynamicTheme
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.GlideAndPreviewImage
import com.jht.widgets.SmartWatchBankState
import com.jht.widgets.SmartWatchPopup
import com.jht.widgets.fillScreenHeight

/*
    Apple v2 marketing ads
 */

/** Apple Ad for welcome screen */
class AppleWelcomeAd : MarketingAd() {
    override val id = "apple2_welcome"

    @Composable
    override fun Content(modifier: Modifier) {
        Column(
            modifier = modifier.testTag("advert:$id"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val labelStyle = MaterialTheme.typography.titleSmall
            val infoStyle = MaterialTheme.typography.bodySmall
            Text(
                text = stringResource(R.string.connect_watch_phone_apple_tc),
                //modifier = Modifier.weight(1f, fill = false),
                style = labelStyle,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = labelStyle.fontSize),
            )
            AppleWelcomeAdText(
                //modifier = Modifier.weight(2f, fill = false),
                style = infoStyle,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = infoStyle.fontSize),
            )
        }
    }
}
/** Text that inlines the contactless icon for nfc at the marked location in the translated string */
@Composable
private fun AppleWelcomeAdText(
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    autoSize: TextAutoSize? = null,
) {
    val nfcId = "nfc"
    // Split the string around the logo. Use a placeholder token in the resource.
    val raw = stringResource(R.string.apple_welcome_ad_pg)  // contains a marker, see below
    val parts = raw.split("%1\$s")  // split at whatever marker text in the string we want to key off
    val text = buildAnnotatedString {
        append(parts.getOrElse(0) { "" })
        appendInlineContent(id = nfcId, alternateText = "[nfc]")
        append(parts.getOrElse(1) { "" })
    }
    val inlineContent = mapOf(
        nfcId to InlineTextContent(
            placeholder = Placeholder(
                width = 1.2.em,          // scales with font size
                height = 1.2.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_contactless),
                contentDescription = "contactless reader",
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize(),
            )
        }
    )
    Text(text = text, inlineContent = inlineContent, modifier = modifier, style = style, autoSize = autoSize)
}



/** Apple Ad for workout selection screen feature section */
class AppleFeatureAd : FeaturedWorkoutAd() {
    override val id = "apple2_feature"
    override val titleId = R.string.connect_watch_phone_apple_tc
    override val descriptionId = R.string.connect_watch_phone_apple_pg
    override val carouselButtonId = R.string.connect_tc
    override val backgroundUri = emptyList<Uri>()

    private val showPopup = mutableStateOf(false)
    override fun onClick() {
        showPopup.value = true
    }

    @Composable
    override fun Content(modifier: Modifier, controller: AdHostController) {
        val context = LocalContext.current
        val state = remember { SmartWatchBankState(context) }
        LaunchedEffect(Unit) { state.start() }
        DisposableEffect(Unit) { onDispose { state.stop(); showPopup.value = false } }
        GlideAndPreviewImage(
            uri = getSDCardUri(file = "gymkit_connect_animation.gif", folder = "advertising"),
            modifier = modifier.testTag("advert:$id"),
            contentScale = ContentScale.Fit,
        )
        if (showPopup.value) {
            DisposableEffect(Unit) {
                controller.pause()
                onDispose { controller.resume() }
            }
            SmartWatchPopup(
                isApple = true,
                onClose = { showPopup.value = false },
                resourceId = state.connectWatchResourceId,
                timeoutMilliseconds = 10_000,
                readerLocation = state.rfidReaderLocation,
                gymKitLevel = state.gymKitLevel.intValue,
            )
        }
    }
}


// NOTE: Some things like external assets might not render in previews
@AllDevicePreviews
@Composable
private fun PreviewAds() {
    val welcomeAd = AppleWelcomeAd()
    val featureAd = AppleFeatureAd()
    val modifier = Modifier
        .fillMaxWidth(0.5f)
        .fillScreenHeight(0.2f)
        .background(DynamicTheme.colors.background1)
    val controller = remember { MockAdHostController() }
    DynamicTheme {
        Column(
            Modifier.fillMaxSize().background(Color.Cyan),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            welcomeAd.Content(modifier)
            featureAd.Content(modifier, controller)
        }
    }
}
