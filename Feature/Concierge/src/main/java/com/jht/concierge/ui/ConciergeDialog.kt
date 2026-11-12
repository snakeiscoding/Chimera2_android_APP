package com.jht.concierge.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.jht.concierge.R
import com.jht.concierge.interfaces.FacilityRequestType
import com.jht.theme.DynamicTheme
import com.jht.ui.Device15inPreviews
import com.jht.ui.clickableNoEffect
import com.jht.ui.combinedClickableNoEffect
import com.jht.widgets.PopupFullscreen
import kotlinx.coroutines.delay
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Composable for the concierge request states
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ConciergePopup(
    modifier: Modifier = Modifier,
    conciergeDialogHeight: ConciergeDialogHeight,
    iconAboveTitleDrawable: Int,
    iconAboveTitleColor: Color,
    iconAboveTitleClickable: Boolean,
    titleText: String,
    descriptionText: String,
    showSpinner: Boolean,
    autoHideDialog: Boolean,
    dialogInteractionTextId: Int,
    onDialogInteraction: () -> Unit,
    onClose: () -> Unit,
    onRequestOption: (String) -> Unit,
    facilityRequestOptions: List<FacilityRequestType>,
    showingRequestOptions: Boolean,
    showCancelRequestButton: Boolean,
    conciergeStateTestTag: String = ""
) {

    PopupFullscreen(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xcc000000))
        )
        ConciergeDialog(
            modifier = modifier.semantics { testTagsAsResourceId = true },
            conciergeDialogHeight = conciergeDialogHeight,
            iconAboveTitleDrawable = iconAboveTitleDrawable,
            iconAboveTitleColor = iconAboveTitleColor,
            iconAboveTitleClickable = iconAboveTitleClickable,
            titleText = titleText,
            descriptionText = descriptionText,
            showSpinner = showSpinner,
            autoHideDialog = autoHideDialog,
            dialogInteractionTextId = dialogInteractionTextId,
            onDialogInteraction = onDialogInteraction,
            onClose = onClose,
            onRequestOption = onRequestOption,
            facilityRequestOptions = facilityRequestOptions,
            showingRequestOptions = showingRequestOptions,
            showCancelRequestButton = showCancelRequestButton,
            conciergeStateTestTag = conciergeStateTestTag,
        )
    }
}

@Composable
fun ConciergeDialog(
    modifier: Modifier = Modifier,
    conciergeDialogHeight: ConciergeDialogHeight,
    iconAboveTitleDrawable: Int = 0,
    iconAboveTitleColor: Color = DynamicTheme.colors.text1,
    iconAboveTitleClickable: Boolean = false,
    titleText: String,
    descriptionText: String = "",
    showSpinner: Boolean,
    autoHideDialog: Boolean, // auto hide on cancel/complete/error
    dialogInteractionTextId: Int = 0,
    onDialogInteraction: () -> Unit,
    onClose: () -> Unit = {},
    onRequestOption: (String) -> Unit = {},
    facilityRequestOptions: List<FacilityRequestType> = listOf(),
    showingRequestOptions: Boolean = false,
    showCancelRequestButton: Boolean = false,
    conciergeStateTestTag: String = "",
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .clickable { onClose() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(1000 / 1920f)
                .fillMaxHeight(
                    when (conciergeDialogHeight) {
                        ConciergeDialogHeight.SMALL -> {
                            400 / 1080f
                        }
                        ConciergeDialogHeight.MEDIUM -> {
                            600 / 1080f
                        }
                        ConciergeDialogHeight.VARIABLE -> {
                            (300 + (facilityRequestOptions.size * 100)) / 1080f
                        }
                    }
                )
                .border(width = 5.dp, color = DynamicTheme.colors.background1, shape = RoundedCornerShape(25.dp))
                .background(shape = RoundedCornerShape(25.dp), color = DynamicTheme.colors.background1)
                .padding(10.dp)
                .clickableNoEffect {} // top stop from closing when clicking the dialog
                .testTag("ConciergeDialog:${conciergeDialogHeight.name}")
            ,
        ) {
            // close dialog (x in corner) can also click outside dialog
            Icon(
                painter = painterResource(id = R.drawable.ic_close), contentDescription = null,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(40 / 900f)
                    .aspectRatio(1f)
                    .align(Alignment.TopEnd)
                    .clickable { onClose() }
                    .testTag("ConciergeDialog:Close:Button"),
                tint = DynamicTheme.colors.text1,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (autoHideDialog) {
                    //Auto close after 5 seconds if complete or cancelled
                    LaunchedEffect(Unit) {
                        delay(5.toDuration(DurationUnit.SECONDS))
                        onClose()
                    }
                }
                // Icon above title & description
                if (iconAboveTitleDrawable > 0) {
                    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    var iconTint = iconAboveTitleColor
                    if (isPressed && iconAboveTitleClickable) {
                        iconTint = DynamicTheme.colors.primary
                    }
                    Icon(
                        modifier = Modifier
                            .padding(top = 25.dp, bottom = 25.dp)
                            .fillMaxSize(
                                100 / when (conciergeDialogHeight) {
                                    ConciergeDialogHeight.SMALL -> { 900f }
                                    ConciergeDialogHeight.MEDIUM -> { 700f }
                                    else -> { 900f }
                                }
                            )
                            .aspectRatio(1f)
                            .combinedClickableNoEffect(
                                interactionSource,
                                onLongClick = { onDialogInteraction() }) {}
                            .testTag("ConciergeDialog:$conciergeStateTestTag:Icon")
                        ,
                        painter = painterResource(id = iconAboveTitleDrawable),
                        contentDescription = null,
                        tint = iconTint,
                    )
                }
                // Title and description
                TitleSubtitle(titleText, descriptionText)
                // Creating concierge request (spinning icon)
                if (showSpinner) {
                    LoadingSpinner()
                }
                // Request options
                if (showingRequestOptions) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        facilityRequestOptions.forEachIndexed { index, facilityRequestType ->
                            RequestOption(
                                modifier = Modifier,
                                requestOption = facilityRequestType,
                                index = index,
                                onRequestHelp = onRequestOption,
                                showBottomLine = index < facilityRequestOptions.size-1
                            )
                        }
                    }
                }
                // cancel button/text to hold icon to mark request complete
                if (dialogInteractionTextId > 0) {
                    if (showCancelRequestButton) {
                        Button(
                            onClick = onDialogInteraction,
                            Modifier
                                .width(250.dp)
                                .padding(top = 30.dp)
                                .aspectRatio(215 / 50f)
                                .testTag("ConciergeDialog:Cancel:Button"),
                            shape = RoundedCornerShape(10f),
                            colors = ButtonDefaults.buttonColors(containerColor = DynamicTheme.colors.primary)
                        ) {
                            Text(
                                stringResource(id = dialogInteractionTextId),
                                color = DynamicTheme.colors.text2,
                                style = DynamicTheme.typography.textStyle1,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(id = dialogInteractionTextId),
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .padding(top = 55.dp)
                                .wrapContentHeight()
                                .testTag("ConciergeDialog:HoldToCompleteRequest:Text"),
                            color = DynamicTheme.colors.text1,
                            textAlign = TextAlign.Center,
                            style = DynamicTheme.typography.textStyle1
                        )
                    }
                }
            }
        }
        // emergency disclaimer
        // now outside dialog, shows on everything except cancelled and complete requests.
        // cancelled/completed requests auto hide the dialog after 5 seconds, so we can reuse a variable.
        if (!autoHideDialog) {
            Text(
                text = stringResource(id = R.string.emergency_warning_pg),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(top = 20.dp)
                    .wrapContentHeight(),
                color = DynamicTheme.colors.text1,
                textAlign = TextAlign.Center,
                style = DynamicTheme.typography.textStyle4
            )
        }
    }
}

@Composable
private fun ColumnScope.RequestOption(
    modifier: Modifier = Modifier,
    requestOption: FacilityRequestType,
    index: Int = -1,
    onRequestHelp: (String) -> Unit = {},
    showBottomLine: Boolean = false,
){
    val context = LocalContext.current
    val iconColor = DynamicTheme.colors.text1
    val buttonText : String
    val requestId : String
    if (requestOption.name == "DEFAULT" || requestOption.buttonText == "DEFAULT") {
        buttonText = stringResource(id = R.string.need_assistance_tc)
        requestId = "" // no id passed for default
    } else {
        buttonText = requestOption.buttonText
        requestId = requestOption.id
    }

    Row(
        modifier
            .fillMaxWidth(628 / 808f) // based on comps
            .fillMaxHeight(100 / 758f) // based on comps
            .weight(1f)
            .testTag("ConciergeDialog:RequestOption:$index:Button")
            .clickable { onRequestHelp(requestId) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(requestOption.buttonIconUrl)
                .decoderFactory(SvgDecoder.Factory()).build(),
            error = painterResource(id = R.drawable.concierge_front_desk),
            placeholder = painterResource(id = R.drawable.concierge_front_desk),
            fallback = painterResource(id = R.drawable.concierge_front_desk),
            contentDescription = "",
            modifier = Modifier
                .weight(.1f)
                .clip(CircleShape)
                .border(1.dp, iconColor, shape = CircleShape)
                .padding(12.dp)
                .aspectRatio(1f),
            colorFilter = ColorFilter.tint(iconColor)
        )
        Spacer(modifier = Modifier.weight(.03f))
        // Autosizetext or regular text?
        Text(modifier = Modifier
            .fillMaxWidth()
            .weight(.7f)
            .testTag("ConciergeDialog:RequestOption:$index:Text"),
            text = buttonText,
            color = Color.White,
            fontSize = 22.sp,
            textAlign = TextAlign.Start,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.weight(.03f))
        Icon(modifier = Modifier
            .weight(.07f)
            .clip(CircleShape)
            .border(1.dp, DynamicTheme.colors.gray2, shape = CircleShape)
            .padding(12.dp)
            ,
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "",
            tint = DynamicTheme.colors.primary
        )
    }
    if (showBottomLine) {
        HorizontalDivider(thickness = 1.dp, color = DynamicTheme.colors.gray2, modifier = Modifier.fillMaxWidth(628 / 808f))
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
        ), label = "Concierge Requesting Help Spinner"
    )

    Icon(
        modifier = Modifier
            .padding(top = 55.dp, bottom = 25.dp)
            .fillMaxSize(100 / 900f)
            .aspectRatio(1f)
            .rotate(angle)
            .testTag("ConciergeDialog:Spinner"),
        painter = painterResource(id = R.drawable.ic_spinner),
        contentDescription = null,
        tint = DynamicTheme.colors.text1
    )
}
@Composable
private fun TitleSubtitle(
    titleText: String,
    descriptionText: String
) {
    if (titleText.isNotEmpty()) {
        Text(
            text = titleText,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(top = 30.dp)
                .wrapContentHeight()
                .testTag("ConciergeDialog:Title:Text"),
            color = DynamicTheme.colors.text1,
            textAlign = TextAlign.Center,
            style = DynamicTheme.typography.textStyle2,
            lineHeight = 40.sp,
            fontSize = 38.sp
        )
    }
    if (descriptionText.isNotEmpty()) {
        Text(
            text = descriptionText,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(top = 15.dp)
                .wrapContentHeight(),
            color = DynamicTheme.colors.text1,
            textAlign = TextAlign.Center,
            style = DynamicTheme.typography.textStyle1
        )
    }
}

enum class ConciergeDialogHeight {
    SMALL, // cancelled/complete/error/loading
    MEDIUM, // opened/acknowledged
    VARIABLE // closed (is variable based on amount of request options)
}

private val previewRequestOptions = listOf(
    FacilityRequestType(buttonText = "Need Assistance"),
    FacilityRequestType(buttonText = "Technical Support how long do I have to make this to test two lines"),
    FacilityRequestType(buttonText = "Discuss Personal Training"),
    FacilityRequestType(buttonText = "Discuss Membership"),
    FacilityRequestType(buttonText = "Request Towel"),
)

@Device15inPreviews
@Composable
private fun PreviewConciergeRequestDialog() {
    ConciergeDialog(
        iconAboveTitleDrawable = 0,
        titleText = stringResource(R.string.require_assistance_tc),
        descriptionText = stringResource(R.string.require_assistance_pg),
        showSpinner = false,
        autoHideDialog = false,
        dialogInteractionTextId = 0,
        onDialogInteraction = {},
        onClose = {},
        onRequestOption = {},
        facilityRequestOptions = previewRequestOptions,
        showingRequestOptions = true,
        conciergeDialogHeight = ConciergeDialogHeight.VARIABLE,
    )
}

@Device15inPreviews
@Composable
private fun PreviewConciergeLoadingDialog() {
    ConciergeDialog(
        iconAboveTitleDrawable = 0,
        titleText = stringResource(R.string.requesting_help_tc),
        descriptionText = "",
        showSpinner = true,
        autoHideDialog = false,
        dialogInteractionTextId = 0,
        onDialogInteraction = {},
        onClose = {},
        onRequestOption = {},
        facilityRequestOptions = previewRequestOptions,
        showingRequestOptions = false,
        conciergeDialogHeight = ConciergeDialogHeight.SMALL,
    )
}

@Device15inPreviews
@Composable
private fun PreviewConciergeRequestOpenedDialog() {
    ConciergeDialog(
        iconAboveTitleDrawable = R.drawable.ic_concierge_requested,
        iconAboveTitleColor = DynamicTheme.colors.tertiary,
        titleText = stringResource(R.string.help_requested_tc),
        descriptionText = stringResource(R.string.help_requested_pg),
        showSpinner = false,
        autoHideDialog = false,
        dialogInteractionTextId = R.string.cancel_request_tc,
        showCancelRequestButton = true,
        onDialogInteraction = {},
        onClose = {},
        onRequestOption = {},
        facilityRequestOptions = previewRequestOptions,
        showingRequestOptions = false,
        conciergeDialogHeight = ConciergeDialogHeight.MEDIUM,
    )
}

@Device15inPreviews
@Composable
private fun PreviewConciergeRequestAcknowledgedDialog() {
    ConciergeDialog(
        iconAboveTitleDrawable = R.drawable.concierge_en_route,
        iconAboveTitleColor = DynamicTheme.colors.tertiary,
        iconAboveTitleClickable = true,
        titleText = stringResource(R.string.help_is_on_the_way_pg),
        descriptionText = stringResource(R.string.help_on_the_way_pg),
        showSpinner = false,
        autoHideDialog = false,
        dialogInteractionTextId = R.string.hold_mark_complete_pg,
        onDialogInteraction = {},
        onClose = {},
        onRequestOption = {},
        facilityRequestOptions = previewRequestOptions,
        showingRequestOptions = false,
        conciergeDialogHeight = ConciergeDialogHeight.MEDIUM,
    )
}



@Device15inPreviews
@Composable
private fun PreviewConciergeCompleteDialog() {
    ConciergeDialog(
        iconAboveTitleDrawable = R.drawable.ic_confirm,
        iconAboveTitleColor = DynamicTheme.colors.complete,
        titleText = stringResource(R.string.request_resolved_tc),
        descriptionText = stringResource(R.string.request_resolved_pg),
        showSpinner = false,
        autoHideDialog = true,
        dialogInteractionTextId = 0,
        onDialogInteraction = {},
        onClose = {},
        onRequestOption = {},
        facilityRequestOptions = previewRequestOptions,
        showingRequestOptions = false,
        conciergeDialogHeight = ConciergeDialogHeight.SMALL,
    )
}

@Device15inPreviews
@Composable
private fun PreviewConciergeCancelledDialog() {
    ConciergeDialog(
        iconAboveTitleDrawable = R.drawable.ic_cancelled,
        iconAboveTitleColor = DynamicTheme.colors.warning,
        titleText = stringResource(R.string.request_canceled_tc),
        descriptionText = stringResource(R.string.request_canceled_pg),
        showSpinner = false,
        autoHideDialog = true,
        dialogInteractionTextId = 0,
        onDialogInteraction = {},
        onClose = {},
        onRequestOption = {},
        facilityRequestOptions = previewRequestOptions,
        showingRequestOptions = false,
        conciergeDialogHeight = ConciergeDialogHeight.SMALL,
    )
}

@Device15inPreviews
@Composable
private fun PreviewConciergeErrorDialog() {
    ConciergeDialog(
        iconAboveTitleDrawable = 0,
        titleText = stringResource(R.string.error_occurred_tc),
        descriptionText = stringResource(R.string.error_occurred_try_again_later_pg),
        showSpinner = false,
        autoHideDialog = true,
        dialogInteractionTextId = 0,
        onDialogInteraction = {},
        onClose = {},
        onRequestOption = {},
        facilityRequestOptions = previewRequestOptions,
        showingRequestOptions = false,
        conciergeDialogHeight = ConciergeDialogHeight.SMALL,
    )
}