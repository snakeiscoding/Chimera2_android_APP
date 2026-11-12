package com.jht.chimera.service.ui.panels.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.chimera.R
import com.jht.theme.DynamicTheme
import com.jht.theme.ThemeButton
import com.jht.theme.themeButtonColor
import com.jht.ui.largerThan16

/**
 * Some constants for Creative Comps and common composable(s)
 */

val panelControlTitleSize @Composable get() = if (largerThan16()) 14.sp else 12.sp    // "Speed" in German is pretty wide, maybe consider auto size text? todo CGC-21941
val panelControlValueSize @Composable get() = 32.sp
val panelControlUnitSize @Composable get() = if (largerThan16()) 14.sp else 12.sp
val panelBorderColor @Composable get() = DynamicTheme.colors.gray3
val barButtonTextSize @Composable get() = 24.sp

// do we really need 2 padding?
val buttonIconPadding = 4.dp
val buttonCommandPadding = 2.dp

// Need reusable theme variants
val panelBackgroundColor : Color @Composable get() = DynamicTheme.colors.gray1.copy(alpha = 0.71f)

const val panelIconButtonAspect = 1.4f
fun Modifier.panelIconButton(): Modifier = composed {
    this.aspectRatio(panelIconButtonAspect)
        .fillMaxWidth()
        .padding(DynamicTheme.padding.panelButton)
}
fun Modifier.panelTopButton(): Modifier = composed {
    this.padding(DynamicTheme.padding.panelButton)
        .height(dimensionResource(R.dimen.top_bar_height))
        .fillMaxWidth()
}
fun Modifier.panelBottomButton(): Modifier = composed {
    this.height(dimensionResource(R.dimen.bottom_bar_stats_height))
        .fillMaxWidth()
}


@Composable
fun PanelIconButton(
    iconId: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = themeButtonColor,
    onClick: () -> Unit,
) {
    PanelIconButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        containerColor = containerColor
    ) {
        if (iconId != 0) Icon(
            modifier = modifier.size(40.dp),
            painter = painterResource(id = iconId),
            tint = DynamicTheme.colors.text1,
            contentDescription = null,
        )
    }
}

@Composable
fun PanelIconButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color,
    onClick: () -> Unit,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    PanelBaseButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .padding(buttonIconPadding),
        containerColor = containerColor,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
fun PanelCommandButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color,
    onClick: () -> Unit,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
){
    PanelBaseButton(onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .padding(buttonCommandPadding),
        enabled = enabled,
        containerColor = containerColor,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
private fun PanelBaseButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color,
    onClick: () -> Unit,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    ThemeButton(
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = contentPadding,
        content = content
    )
}
