package com.jht.workoutselection.common.parts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.theme.DynamicTheme
import com.jht.theme.GradientButton
import com.jht.theme.ThemeSelector
import com.jht.ui.computeSize
import com.jht.workoutselection.R


@Composable
internal fun GoButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    // TODO - This gradient is not in the theme?
    val gradient = Brush.verticalGradient(listOf(Color(0xFF17FFA2), Color(0xFF04FD49)))
    GradientButton(
        onClick,
        Modifier
            .padding(40.dp)
            .fillMaxWidth(),
        contentPadding = PaddingValues(10.dp),
        gradient = gradient,
        enabled = enabled,
    ) {
        Icon(
            painterResource(id = R.drawable.ic_quick_start),
            null,
            Modifier
                .size(65.dp)
                .padding(10.dp),
            Color.Black, // DynamicTheme.colors.text2 ?
        )
    }
}





/** Tab Header with pair of icon and text, with separate selection icon */
@Composable
internal fun TabBase(
    iconId: Int,
    iconSelectId: Int,
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    ThemeSelector(selected, testTag, onClick, Modifier.aspectRatio(200 / 100f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(id = if (selected) iconSelectId else iconId),
                null,
                Modifier.size(40.dp),
                if (selected) DynamicTheme.colors.primary else DynamicTheme.colors.text1
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(label, color = DynamicTheme.colors.text1, style = DynamicTheme.typography.textStyle1, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Tab Header with pair of icon and text, but single icon */
@Composable
internal fun TabBase(
    iconId: Int,
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    TabBase(iconId, iconId, label, selected, testTag, onClick)
}
