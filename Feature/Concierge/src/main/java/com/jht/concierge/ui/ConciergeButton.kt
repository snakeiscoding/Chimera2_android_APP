package com.jht.concierge.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.jht.concierge.R
import com.jht.concierge.interfaces.ConciergeHandler
import com.jht.concierge.interfaces.ConciergeModel
import com.jht.concierge.interfaces.CustomTextEnum
import com.jht.concierge.interfaces.getConciergeInjectionEntryPoint
import com.jht.interfaces.models.UIServiceModel
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.widgets.helpers.getInjectionEntryPoint

@Composable
fun ConciergeButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val showConciergePopup = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val conciergeUIHandler : ConciergeUIHandler = remember { ConciergeUIHandler(context) }
    DisposableEffect(Unit) { onDispose { conciergeUIHandler.close() } }

    if (conciergeUIHandler.isConciergeEnabled.value && conciergeUIHandler.isConciergeAvailable.value) {
        //Variables for the concierge button
        val defaultIconColor = DynamicTheme.colors.text1
        val iconId = remember { mutableStateOf(R.drawable.ic_concierge_bell) }
        val iconColor = remember { mutableStateOf(defaultIconColor) }

        //Variables for the concierge dialog
        val facilityRequestOptions = remember { mutableStateOf(conciergeUIHandler.conciergeModel.getFacilityConfigResponse().requestTypes) }
        val conciergeDialogHeight = remember { mutableStateOf(ConciergeDialogHeight.VARIABLE) }
        val iconAboveTitleDrawable = remember { mutableStateOf(0) }
        val iconAboveTitleColor = remember { mutableStateOf(Color.Gray) }
        val iconAboveTitleClickable = remember { mutableStateOf(false) }
        val titleTextId = remember { mutableStateOf(0) }
        val descriptionTextId = remember { mutableStateOf(0) }
        val canShowCustomText = remember { mutableStateOf(false) }
        val titleKeyName = remember { mutableStateOf(CustomTextEnum.SELECT_REQUEST_HEADER) }
        val descriptionKeyName = remember { mutableStateOf(CustomTextEnum.SELECT_REQUEST_SUBTITLE) }
        val customTextEnum = remember { mutableStateOf(conciergeUIHandler.conciergeModel.getFacilityConfigResponse().customTextFields) }
        val showSpinner = remember { mutableStateOf(false) }
        val autoHideDialog = remember { mutableStateOf(false) }
        val showFacilityRequestOptions = remember { mutableStateOf(false) }
        val showCancelRequestButton = remember { mutableStateOf(false) }
        val dialogInteractionTextId = remember { mutableStateOf(0) }
        var onDialogInteraction = {}
        var onClose = {}
        var onRequestOption:(String)-> Unit = {}

        //Concierge State Handling
        when (conciergeUIHandler.state.intValue) {
            ConciergeStates.CLOSED.ordinal -> {
                // icon variables
                iconId.value = R.drawable.ic_concierge_bell
                iconColor.value = defaultIconColor
                // dialog variables
                conciergeDialogHeight.value = ConciergeDialogHeight.VARIABLE
                iconAboveTitleDrawable.value = 0
                iconAboveTitleColor.value = DynamicTheme.colors.gray2
                iconAboveTitleClickable.value = false
                titleTextId.value = R.string.require_assistance_tc
                descriptionTextId.value = R.string.require_assistance_pg
                titleKeyName.value = CustomTextEnum.SELECT_REQUEST_HEADER
                descriptionKeyName.value = CustomTextEnum.SELECT_REQUEST_SUBTITLE
                canShowCustomText.value = true
                showSpinner.value = false
                autoHideDialog.value = false
                showFacilityRequestOptions.value = true
                showCancelRequestButton.value = false
                dialogInteractionTextId.value = 0
                onDialogInteraction = {  }
                onClose = {
                    showConciergePopup.value = false
                }
                onRequestOption = { conciergeUIHandler.conciergeModel.createRequest(it) }
            }

            ConciergeStates.OPENED.ordinal -> {
                conciergeUIHandler.sendMessage(R.string.help_requested_new_tc, showConciergePopup.value)
                // icon variables
                iconId.value = R.drawable.ic_concierge_requested
                iconColor.value = DynamicTheme.colors.tertiary
                // dialog variables
                conciergeDialogHeight.value = ConciergeDialogHeight.MEDIUM
                iconAboveTitleDrawable.value = R.drawable.ic_concierge_requested
                iconAboveTitleColor.value = DynamicTheme.colors.tertiary
                iconAboveTitleClickable.value = false
                titleTextId.value = R.string.help_requested_new_tc
                descriptionTextId.value = R.string.help_requested_pg
                titleKeyName.value = CustomTextEnum.HELP_REQUESTED_HEADER
                descriptionKeyName.value = CustomTextEnum.HELP_REQUESTED_SUBTITLE
                canShowCustomText.value = true
                showSpinner.value = false
                autoHideDialog.value = false
                showFacilityRequestOptions.value = false
                showCancelRequestButton.value = true
                dialogInteractionTextId.value = R.string.cancel_request_tc
                onDialogInteraction = { // cancel request button
                    conciergeUIHandler.conciergeModel.cancelConciergeState()
                    conciergeUIHandler.conciergeModel.cancelRequest()
                }
                onClose = { showConciergePopup.value = false }
                onRequestOption = {  }
            }

            ConciergeStates.CANCELLED.ordinal -> {
                conciergeUIHandler.sendMessage(R.string.request_canceled_tc, showConciergePopup.value)
                // icon variables
                iconId.value = R.drawable.ic_concierge
                iconColor.value = defaultIconColor
                // dialog variables
                conciergeDialogHeight.value = ConciergeDialogHeight.SMALL
                iconAboveTitleDrawable.value = R.drawable.ic_cancelled
                iconAboveTitleColor.value = DynamicTheme.colors.warning // there is also a cancelled color, don't get confused
                iconAboveTitleClickable.value = false
                titleTextId.value = R.string.request_canceled_tc
                descriptionTextId.value = R.string.request_canceled_pg
                canShowCustomText.value = false
                showSpinner.value = false
                autoHideDialog.value = true
                showFacilityRequestOptions.value = false
                showCancelRequestButton.value = false
                dialogInteractionTextId.value = 0
                onDialogInteraction = {  }
                onClose = {
                    showConciergePopup.value = false
                    conciergeUIHandler.conciergeModel.resetConciergeState()
                }
                onRequestOption = {  }
            }

            ConciergeStates.ACKNOWLEDGED.ordinal -> {
                conciergeUIHandler.sendMessage(R.string.help_on_the_way_tc, showConciergePopup.value)
                // icon variables
                iconId.value = R.drawable.ic_concierge_requested
                iconColor.value = DynamicTheme.colors.primary
                // dialog variables
                conciergeDialogHeight.value = ConciergeDialogHeight.MEDIUM
                iconAboveTitleDrawable.value = R.drawable.concierge_en_route
                iconAboveTitleColor.value = DynamicTheme.colors.tertiary
                iconAboveTitleClickable.value = true
                titleTextId.value = R.string.help_on_the_way_tc
                descriptionTextId.value = R.string.help_on_the_way_pg
                titleKeyName.value = CustomTextEnum.HELP_EN_ROUTE_HEADER
                descriptionKeyName.value = CustomTextEnum.HELP_EN_ROUTE_SUBTITLE
                canShowCustomText.value = true
                showSpinner.value = false
                autoHideDialog.value = false
                showFacilityRequestOptions.value = false
                showCancelRequestButton.value = false
                dialogInteractionTextId.value = R.string.hold_mark_complete_pg
                onDialogInteraction = { // holding the icon to mark the concierge request complete
                    conciergeUIHandler.conciergeModel.cancelRequest()
                    conciergeUIHandler.conciergeModel.completeConciergeState()
                }
                onClose = {
                    showConciergePopup.value = false
                }
                onRequestOption = {  }
            }
            ConciergeStates.LOADING.ordinal -> {
                // icon variables
                iconId.value = R.drawable.ic_concierge
                iconColor.value = defaultIconColor
                // dialog variables
                conciergeDialogHeight.value = ConciergeDialogHeight.SMALL
                iconAboveTitleDrawable.value = 0
                iconAboveTitleColor.value = DynamicTheme.colors.gray2
                iconAboveTitleClickable.value = false
                titleTextId.value = R.string.requesting_help_tc
                descriptionTextId.value = 0
                canShowCustomText.value = false
                showSpinner.value = true
                autoHideDialog.value = false
                showFacilityRequestOptions.value = false
                showCancelRequestButton.value = false
                dialogInteractionTextId.value = 0
                onDialogInteraction = {  }
                onClose = {
                    showConciergePopup.value = false
                }
                onRequestOption = {  }
            }

            ConciergeStates.COMPLETED.ordinal -> {
                conciergeUIHandler.sendMessage(R.string.request_complete, showConciergePopup.value)
                // icon variables
                iconId.value = R.drawable.ic_concierge
                iconColor.value = DynamicTheme.colors.success
                // dialog variables
                conciergeDialogHeight.value = ConciergeDialogHeight.SMALL
                iconAboveTitleDrawable.value = R.drawable.ic_confirm
                iconAboveTitleColor.value = DynamicTheme.colors.complete
                iconAboveTitleClickable.value = false
                titleTextId.value = R.string.request_complete
                descriptionTextId.value = R.string.request_resolved_pg
                canShowCustomText.value = false
                showSpinner.value = false
                autoHideDialog.value = true
                showFacilityRequestOptions.value = false
                showCancelRequestButton.value = false
                dialogInteractionTextId.value = 0
                onDialogInteraction = {  }
                onClose = {
                    showConciergePopup.value = false
                    conciergeUIHandler.conciergeModel.resetConciergeState()
                }
                onRequestOption = {  }
            }

            ConciergeStates.Error.ordinal -> {
                // icon variables
                iconId.value = R.drawable.ic_concierge
                iconColor.value = defaultIconColor
                // dialog variables
                conciergeDialogHeight.value = ConciergeDialogHeight.SMALL
                iconAboveTitleDrawable.value = 0
                iconAboveTitleColor.value = DynamicTheme.colors.gray2
                iconAboveTitleClickable.value = false
                titleTextId.value = R.string.error_occurred_tc
                descriptionTextId.value = R.string.error_occurred_try_again_later_pg
                showSpinner.value = false
                autoHideDialog.value = true
                showFacilityRequestOptions.value = false
                showCancelRequestButton.value = false
                dialogInteractionTextId.value = 0
                onDialogInteraction = {  }
                onClose = {
                    showConciergePopup.value = false
                    conciergeUIHandler.conciergeModel.resetConciergeState()
                }
                onRequestOption = {  }
            }
        }

        ThemeButton(
            onClick = { showConciergePopup.value = true },
            enabled = enabled,
            contentPadding = PaddingValues(),
            modifier = modifier
                .aspectRatio(120/70f) // TODO figure out how we size buttons
                .testTag("ConciergeButton")
        ) {
            Icon(
                modifier = Modifier
                    .fillMaxSize(.5f)
                    .testTag("Concierge:${conciergeUIHandler.state.intValue}:Icon"),
                painter = painterResource(id = iconId.value),
                contentDescription = null,
                tint = iconColor.value,
            )
        }

        if (showConciergePopup.value) {
            LaunchedEffect(Unit) {
                val facilityResponse = conciergeUIHandler.conciergeModel.getFacilityConfigResponse()
                facilityRequestOptions.value = facilityResponse.requestTypes.sortedBy { it.buttonPriority } // ascending order
                customTextEnum.value = facilityResponse.customTextFields
            }

            val titleText = if (titleTextId.value > 0) stringResource(titleTextId.value) else ""
            val descriptionText = if (descriptionTextId.value > 0) stringResource(descriptionTextId.value) else ""

            val requestHeader = if (canShowCustomText.value) customTextEnum.value?.get(titleKeyName.value) ?: titleText else titleText
            val requestSubtitle = if (canShowCustomText.value) customTextEnum.value?.get(descriptionKeyName.value) ?: descriptionText else descriptionText

            ConciergePopup(
                conciergeDialogHeight = conciergeDialogHeight.value,
                iconAboveTitleDrawable = iconAboveTitleDrawable.value,
                iconAboveTitleColor = iconAboveTitleColor.value,
                iconAboveTitleClickable = iconAboveTitleClickable.value,
                titleText = requestHeader,
                descriptionText = requestSubtitle,
                showSpinner = showSpinner.value,
                autoHideDialog = autoHideDialog.value,
                dialogInteractionTextId = dialogInteractionTextId.value,
                onDialogInteraction = onDialogInteraction,
                onClose = onClose,
                onRequestOption = onRequestOption,
                facilityRequestOptions = facilityRequestOptions.value,
                showingRequestOptions = showFacilityRequestOptions.value,
                showCancelRequestButton = showCancelRequestButton.value,
                conciergeStateTestTag = ConciergeStates.entries.getOrNull(conciergeUIHandler.state.intValue)?.name ?: ""
            )
        }
    }
}

/** Private enum to transfer data to the ui. Mutually exclusive states */
private enum class ConciergeStates {
    CLOSED,
    OPENED,
    ACKNOWLEDGED,
    COMPLETED,
    CANCELLED,
    LOADING,
    Error
}

private class ConciergeUIHandler(context: Context) : ConciergeHandler() {
    val isConciergeEnabled = mutableStateOf(true)
    val isConciergeAvailable = mutableStateOf(true)
    val state = mutableIntStateOf(0)
    var currentTextId: Int = 0

    val conciergeModel: ConciergeModel
    private val uiServiceModel: UIServiceModel
    init {
        val hiltEntryPoint = getInjectionEntryPoint(context)
        conciergeModel = getConciergeInjectionEntryPoint(context).concierge()
        uiServiceModel = hiltEntryPoint.ui()
        conciergeModel.isConciergeEnabled.listenAndTrigger { isConciergeEnabled.value = it }
        conciergeModel.isConciergeAvailable.listenAndTrigger { isConciergeAvailable.value = it }
        conciergeModel.startConciergeStateHandler(this)
    }
    fun close() {
        conciergeModel.close()
        uiServiceModel.close()
    }

    @Composable
    fun sendMessage(textId: Int, popupShowing: Boolean): Boolean {
        /* CGC-18163 - they no longer want Concierge message in message center, they just want the icon to do visual indications
        // Check to only send messages if the the text value changed, i.e. the request was updated to a new state
        // Only show messages if the popup isn't showing
        if (!popupShowing && currentTextId != textId) {
            currentTextId = textId
            val msg = MessageCenterMessage(stringResource(id = R.string.concierge_tc), stringResource(id = textId), 30)
            msg.hideTime = 4
            msg.isHiddenOnSplash = true
            uiServiceModel.sendMessageCenterMessage(msg)
            return true
        }*/
        return false
    }

    override fun onRequestClosed() {
        state.intValue = ConciergeStates.CLOSED.ordinal
    }
    override fun onRequestOpened() {
        state.intValue = ConciergeStates.OPENED.ordinal
    }
    override fun onRequestCancelled() {
        state.intValue = ConciergeStates.CANCELLED.ordinal
    }
    override fun onRequestAcknowledged() {
        state.intValue = ConciergeStates.ACKNOWLEDGED.ordinal
    }
    override fun onRequestCompleted() {
        state.intValue = ConciergeStates.COMPLETED.ordinal
    }
    override fun onRequestLoading() {
        state.intValue = ConciergeStates.LOADING.ordinal
    }
    override fun onRequestError() {
        state.intValue = ConciergeStates.Error.ordinal
    }
}

@Preview
@Composable
private fun PreviewConciergeButton() {
    ConciergeButton()
}
