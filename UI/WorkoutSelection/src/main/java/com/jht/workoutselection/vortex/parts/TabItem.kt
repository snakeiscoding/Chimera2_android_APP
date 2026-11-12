package com.jht.workoutselection.vortex.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.theme.DynamicTheme
import com.jht.ui.computeSize

@Composable
internal fun TabItem(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    selectedColor: Color = DynamicTheme.colors.buttonPrimaryDefault,
    cornerRadius: Dp = 24.dp.computeSize(),
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(IntrinsicSize.Max)
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (isSelected) selectedColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp)
            .testTag("WorkoutSelect:${text}:Button"),
        contentAlignment = Alignment.Center
    ) {
        Row{
            Spacer(modifier = Modifier.size(25.dp))

            Text(
                text = text,
                color = if (isSelected) DynamicTheme.colors.text2 else DynamicTheme.colors.buttonTertiaryDefault,
                style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),)

            Spacer(modifier = Modifier.size(25.dp))
        }
    }
}