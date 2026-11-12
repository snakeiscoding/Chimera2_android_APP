package com.jht.progression.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jht.progression.R
import com.jht.progression.ui.MmColors
import com.jht.resources.oxaniumFonts
import com.jht.ui.AllDevicePreviews
import com.jht.widgets.CadenceModalStyle
import com.jht.widgets.CadenceStatModal

private val PROGRESSION_CADENCE_MODAL_STYLE = CadenceModalStyle(
    fontFamily = oxaniumFonts,
    titleWeight = FontWeight.Bold,
    valueWeight = FontWeight.Bold,
    accentColor = MmColors.zone3Color,
    // Best-evidence estimate from the expanded-card measurement; not yet confirmed against the
    // actual collapsed-state width.
    cardWidth = 252.dp,
)

/**
 * Progression-specific theming and strings for the shared [CadenceStatModal]. See that composable
 * for the shape, spacing, and swap animation shared across screens.
 *
 * @param spm Current cadence value in steps per minute.
 * @param isHidden Whether the collapsed re-open button (true) or the full card (false) is shown.
 * @param onDismiss Invoked when the card's close button is tapped.
 * @param onReopen Invoked when the re-open button is tapped.
 * @param modifier Modifier applied to the root composable for layout and positioning.
 */
@Composable
internal fun ProgressionCadenceModal(
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
        style = PROGRESSION_CADENCE_MODAL_STYLE,
        isHidden = isHidden,
        onDismiss = onDismiss,
        onReopen = onReopen,
        modifier = modifier,
    )
}

@AllDevicePreviews
@Composable
private fun ProgressionCadenceModalVisiblePreview() {
    ProgressionCadenceModal(spm = 170, isHidden = false, onDismiss = {}, onReopen = {})
}

@AllDevicePreviews
@Composable
private fun ProgressionCadenceModalHiddenPreview() {
    ProgressionCadenceModal(spm = 170, isHidden = true, onDismiss = {}, onReopen = {})
}
