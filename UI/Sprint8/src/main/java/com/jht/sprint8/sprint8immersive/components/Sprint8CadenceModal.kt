package com.jht.sprint8.sprint8immersive.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.jht.sprint8.R
import com.jht.sprint8.utils.Sprint8Colors.KEYPAD_SPRINTING_CONFIRM_BTN_COLOR
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.CadenceModalStyle
import com.jht.widgets.CadenceStatModal

private val SPRINT8_CADENCE_MODAL_STYLE = CadenceModalStyle(
    fontFamily = FontFamily.SansSerif,
    titleWeight = FontWeight.Medium,
    valueWeight = FontWeight.SemiBold,
    accentColor = KEYPAD_SPRINTING_CONFIRM_BTN_COLOR,
)

/**
 * Sprint8-specific theming and strings for the shared [CadenceStatModal]. See that composable for
 * the shape, spacing, and swap animation shared across screens.
 *
 * @param spm Current cadence value in steps per minute.
 * @param isHidden Whether the collapsed re-open button (true) or the full card (false) is shown.
 * @param onDismiss Invoked when the card's close button is tapped.
 * @param onReopen Invoked when the re-open button is tapped.
 * @param modifier Modifier applied to the root composable for layout and positioning.
 */
@Composable
internal fun Sprint8CadenceModal(
    spm: Int,
    isHidden: Boolean,
    onDismiss: () -> Unit,
    onReopen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CadenceStatModal(
        spm = spm,
        title = stringResource(R.string.current_cadence_tc),
        unitLabel = stringResource(R.string.spm_uc),
        icon = painterResource(R.drawable.ic_cadence),
        iconContentDescription = stringResource(R.string.cadence_tc),
        style = SPRINT8_CADENCE_MODAL_STYLE,
        isHidden = isHidden,
        onDismiss = onDismiss,
        onReopen = onReopen,
        modifier = modifier,
    )
}

@AllDevicePreviews
@Composable
private fun Sprint8CadenceModalVisiblePreview() {
    Sprint8CadenceModal(spm = 170, isHidden = false, onDismiss = {}, onReopen = {})
}

@AllDevicePreviews
@Composable
private fun Sprint8CadenceModalHiddenPreview() {
    Sprint8CadenceModal(spm = 170, isHidden = true, onDismiss = {}, onReopen = {})
}
