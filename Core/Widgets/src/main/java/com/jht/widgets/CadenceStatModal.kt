package com.jht.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.ui.AllDevicePreviews
import com.jht.ui.computeSize
import com.jht.ui.computeSizeHd

internal const val cadenceStatModalRootTestTag = "CadenceStatModal:Root"

/** Which corner [CadenceStatModal] contracts the full card toward when collapsing. */
enum class CadenceModalCollapseCorner {
    TOP_START,
    TOP_END,
}

/**
 * Visual styling that differs per consuming screen for [CadenceStatModal]; the shape and spacing
 * are shared and do not vary.
 */
data class CadenceModalStyle(
    val fontFamily: FontFamily,
    val titleWeight: FontWeight,
    val valueWeight: FontWeight,
    val accentColor: Color,
    val backgroundColor: Color = Color(0xFF191919),
    val cardWidth: Dp = 275.dp,
    val collapseCorner: CadenceModalCollapseCorner = CadenceModalCollapseCorner.TOP_END,
)

/**
 * Collapsible "current cadence" card, shared across screens that show live SPM. The full card
 * (title, value, unit, close button) and a small circular re-open button are swapped by
 * [isHidden], animated by contracting/expanding toward [CadenceModalStyle.collapseCorner] - the
 * same transition plays in reverse for the opposite direction, driven by the one flag.
 *
 * The root always reserves [CadenceModalStyle.cardWidth] of horizontal space and aligns its
 * content to that box's [CadenceModalStyle.collapseCorner], so the collapsed button's edge on
 * that side lands exactly where the expanded card's edge was - regardless of how a parent layout
 * aligns this composable. Without this, a parent that aligns this composable toward the opposite
 * side (e.g. a `Column` shared with a start-aligned sibling, as in Progression's `WorkoutScreen`)
 * would instead shrink the card from that side, landing the collapsed button under the wrong edge
 * of the card.
 *
 * @param spm Current cadence value in steps per minute.
 * @param title Screen-specific title text (e.g. "Current Cadence").
 * @param unitLabel Screen-specific unit text (e.g. "SPM").
 * @param icon Icon shown on the re-open button. Passed in (rather than hardcoded) so a screen can
 * supply a different icon if its design ever calls for one.
 * @param iconContentDescription Accessibility label for [icon].
 * @param style Font/color theming that differs per screen; see [CadenceModalStyle].
 * @param isHidden Whether the collapsed re-open button (true) or the full card (false) is shown.
 * @param onDismiss Invoked when the card's close button is tapped.
 * @param onReopen Invoked when the re-open button is tapped.
 * @param modifier Modifier applied to the root composable for layout and positioning.
 */
@Composable
fun CadenceStatModal(
    spm: Int,
    title: String,
    unitLabel: String,
    icon: Painter,
    iconContentDescription: String,
    style: CadenceModalStyle,
    isHidden: Boolean,
    onDismiss: () -> Unit,
    onReopen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cornerAlignment = when (style.collapseCorner) {
        CadenceModalCollapseCorner.TOP_START -> Alignment.TopStart
        CadenceModalCollapseCorner.TOP_END -> Alignment.TopEnd
    }
    val transformOriginX = when (style.collapseCorner) {
        CadenceModalCollapseCorner.TOP_START -> 0f
        CadenceModalCollapseCorner.TOP_END -> 1f
    }
    Box(
        modifier = modifier
            .width(style.cardWidth.computeSizeHd())
            .testTag(cadenceStatModalRootTestTag),
        contentAlignment = cornerAlignment,
    ) {
        AnimatedContent(
            targetState = isHidden,
            contentAlignment = cornerAlignment,
            transitionSpec = {
                val transformOrigin = TransformOrigin(transformOriginX, 0f)
                (scaleIn(transformOrigin = transformOrigin) + fadeIn())
                    .togetherWith(scaleOut(transformOrigin = transformOrigin) + fadeOut())
            },
            label = "CadenceStatModalVisibility"
        ) { hidden ->
            if (hidden) {
                CadenceStatModalHiddenButton(
                    icon = icon,
                    iconContentDescription = iconContentDescription,
                    style = style,
                    onClick = onReopen,
                )
            } else {
                CadenceStatModalCard(
                    spm = spm,
                    title = title,
                    unitLabel = unitLabel,
                    style = style,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun CadenceStatModalCard(
    spm: Int,
    title: String,
    unitLabel: String,
    style: CadenceModalStyle,
    onDismiss: () -> Unit,
) {
    val componentTag = "CadenceStatModal"
    val cornerShape = RoundedCornerShape(18.dp.computeSizeHd())
    Box(
        modifier = Modifier
            .width(style.cardWidth.computeSizeHd())
            .background(style.backgroundColor, cornerShape)
            .border(1.dp.computeSizeHd(), style.accentColor, cornerShape)
            .padding(
                top = 24.dp.computeSizeHd(),
                end = 24.dp.computeSizeHd(),
                bottom = 8.dp.computeSizeHd(),
                start = 24.dp.computeSizeHd()
            )
            .semantics { testTagsAsResourceId = true }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp.computeSizeHd())
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .padding(end = 32.dp.computeSizeHd())
                    .testTag("$componentTag:Title:Text"),
                color = Color(0xFFF5F5F5),
                fontSize = 18.sp.computeSize(1920f),
                lineHeight = 26.sp.computeSize(1920f),
                fontWeight = style.titleWeight,
                fontFamily = style.fontFamily,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp.computeSizeHd())
            ) {
                Text(
                    text = spm.toString(),
                    modifier = Modifier.testTag("$componentTag:Value:Text"),
                    color = style.accentColor,
                    fontSize = 54.sp.computeSize(1920f),
                    lineHeight = 56.sp.computeSize(1920f),
                    fontWeight = style.valueWeight,
                    fontFamily = style.fontFamily,
                )
                Text(
                    text = unitLabel,
                    modifier = Modifier.testTag("$componentTag:Unit:Text"),
                    color = Color(0xFFC5C5C5),
                    fontSize = 18.sp.computeSize(1920f),
                    lineHeight = 26.sp.computeSize(1920f),
                    fontWeight = style.titleWeight,
                    fontFamily = style.fontFamily,
                )
            }
            HorizontalDivider(
                thickness = 1.dp.computeSizeHd(),
                color = style.accentColor.copy(alpha = 0.2f)
            )
        }
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close",
            tint = Color(0xFFD1D1D1),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp.computeSizeHd())
                .clickable(onClick = onDismiss)
                .testTag("$componentTag:Dismiss:Button")
        )
    }
}

@Composable
private fun CadenceStatModalHiddenButton(
    icon: Painter,
    iconContentDescription: String,
    style: CadenceModalStyle,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp.computeSizeHd())
            .background(style.backgroundColor, CircleShape)
            .clickable(onClick = onClick)
            .semantics { testTagsAsResourceId = true }
            .testTag("CadenceStatModalHidden:Button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = iconContentDescription,
            tint = style.accentColor,
            modifier = Modifier.size(24.dp.computeSizeHd())
        )
    }
}

private val PREVIEW_SPRINT8_STYLE = CadenceModalStyle(
    fontFamily = FontFamily.SansSerif,
    titleWeight = FontWeight.Medium,
    valueWeight = FontWeight.SemiBold,
    accentColor = Color(0xFFFFA600),
)

private val PREVIEW_METHOD_STYLE = CadenceModalStyle(
    fontFamily = FontFamily.SansSerif, // Oxanium isn't available from this module's preview
    titleWeight = FontWeight.Bold,
    valueWeight = FontWeight.Bold,
    accentColor = Color(0xFF33DB95),
)

private val PREVIEW_VIRTUAL_ACTIVE = CadenceModalStyle(
    fontFamily = FontFamily.SansSerif,
    titleWeight = FontWeight.Bold,
    valueWeight = FontWeight.Bold,
    accentColor = Color(0xFFFFCB27),
    collapseCorner = CadenceModalCollapseCorner.TOP_START,
)

@AllDevicePreviews
@Composable
private fun CadenceStatModalSprint8VisiblePreview() {
    CadenceStatModal(
        spm = 170,
        title = "Current Cadence",
        unitLabel = "SPM",
        icon = painterResource(R.drawable.ic_cadence),
        iconContentDescription = "Cadence",
        style = PREVIEW_SPRINT8_STYLE,
        isHidden = false,
        onDismiss = {},
        onReopen = {},
    )
}

@AllDevicePreviews
@Composable
private fun CadenceStatModalSprint8HiddenPreview() {
    CadenceStatModal(
        spm = 170,
        title = "Current Cadence",
        unitLabel = "SPM",
        icon = painterResource(R.drawable.ic_cadence),
        iconContentDescription = "Cadence",
        style = PREVIEW_SPRINT8_STYLE,
        isHidden = true,
        onDismiss = {},
        onReopen = {},
    )
}

@AllDevicePreviews
@Composable
private fun CadenceStatModalMethodVisiblePreview() {
    CadenceStatModal(
        spm = 170,
        title = "Current Cadence",
        unitLabel = "SPM",
        icon = painterResource(R.drawable.ic_cadence),
        iconContentDescription = "Cadence",
        style = PREVIEW_METHOD_STYLE,
        isHidden = false,
        onDismiss = {},
        onReopen = {},
    )
}

@AllDevicePreviews
@Composable
private fun CadenceStatModalVirtualActiveVisiblePreview() {
    CadenceStatModal(
        spm = 170,
        title = "Current Cadence",
        unitLabel = "SPM",
        icon = painterResource(R.drawable.ic_cadence),
        iconContentDescription = "Cadence",
        style = PREVIEW_VIRTUAL_ACTIVE,
        isHidden = false,
        onDismiss = {},
        onReopen = {},
    )
}

@AllDevicePreviews
@Composable
private fun CadenceStatModalVirtualActiveHiddenPreview() {
    CadenceStatModal(
        spm = 170,
        title = "Current Cadence",
        unitLabel = "SPM",
        icon = painterResource(R.drawable.ic_cadence),
        iconContentDescription = "Cadence",
        style = PREVIEW_VIRTUAL_ACTIVE,
        isHidden = true,
        onDismiss = {},
        onReopen = {},
    )
}
