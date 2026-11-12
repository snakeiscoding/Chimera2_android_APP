package com.jht.workoutselection.vortex.parts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.jht.data.WorkoutBadge
import com.jht.resources.R
import com.jht.theme.DynamicTheme
import com.jht.theme.darkDynamicTheme
import com.jht.theme.lightDynamicTheme

/**
 * Composable for displaying a workout badge. Scales to whatever size it is given.
 */
@Composable
internal fun ThemedBadgeView(badge: WorkoutBadge, modifier: Modifier = Modifier) {
    if (badge.hasBackground()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            Image(
                painterResource(badge.backgroundIcon),
                contentDescription = "badge_background",
                modifier = Modifier.fillMaxSize()
            )

            if (badge.hasTintedIcon()) {
                Image(
                    painterResource(badge.tintedInnerIcon),
                    contentDescription = "badge_background",
                    colorFilter = ColorFilter.tint(DynamicTheme.colors.primary),
                    modifier = Modifier
                        .fillMaxSize(0.5f)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Preview(name = "Training Badge 40dp (dark)", showBackground = true, widthDp = 40, heightDp = 40)
@Composable private fun Preview_WorkoutBadge_Dark() {
    DynamicTheme(darkDynamicTheme) {
        val badge = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
        ThemedBadgeView(badge)
    }
}

@Preview(name = "Training Badge 40dp (light)", showBackground = true, widthDp = 40, heightDp = 40)
@Composable private fun Preview_WorkoutBadge_Light() {
    DynamicTheme(lightDynamicTheme) {
        val badge = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
        ThemedBadgeView(badge)
    }
}

@Preview(name = "Egym Badge 40dp (dark)", showBackground = true, widthDp = 40, heightDp = 40)
@Composable private fun Preview_WorkoutBadge_Egym_Dark() {
    DynamicTheme(darkDynamicTheme) {
        val badge = WorkoutBadge(R.drawable.ic_badge_egym)
        ThemedBadgeView(badge)
    }
}

@Preview(name = "Training Badge 60dp (dark)", showBackground = true, widthDp = 60, heightDp = 60)
@Composable private fun Preview_WorkoutBadge_Large_Dark() {
    DynamicTheme(darkDynamicTheme) {
        val badge = WorkoutBadge(R.drawable.ic_badge_background, R.drawable.ic_badge_runner)
        ThemedBadgeView(badge)
    }
}