package com.jht.seca2.utils

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jht.seca2.secaUiTheme
import com.jht.theme.DynamicTheme
import com.jht.ui.AllSecaDevicePreviews

@Composable
fun closestSpacingSum(targetDp: Float): Dp {
    val spacingMap = mapOf(
        DynamicTheme.padding.spacing0 to 0f,
        DynamicTheme.padding.spacing1 to 4f,
        DynamicTheme.padding.spacing2 to 8f,
        DynamicTheme.padding.spacing3 to 10f,
        DynamicTheme.padding.spacing4 to 12f,
        DynamicTheme.padding.spacing5 to 16f,
        DynamicTheme.padding.spacing6 to 20f,
        DynamicTheme.padding.spacing7 to 24f,
        DynamicTheme.padding.spacing8 to 32f,
        DynamicTheme.padding.spacing9 to 40f,
        DynamicTheme.padding.spacing10 to 48f,
        DynamicTheme.padding.spacing11 to 64f,
        DynamicTheme.padding.spacing12 to 80f,
        DynamicTheme.padding.spacing13 to 160f,
    )

    val sortedSpacings = spacingMap.entries.sortedByDescending { it.value }

    var remaining = targetDp
    val selectedIds = mutableListOf<Dp>()

    for ((id, value) in sortedSpacings) {
        if (value <= 0f) continue
        while (remaining >= value) {
            selectedIds.add(id)
            remaining -= value
        }
    }

    var finalDp = 0f

    for (id in selectedIds) {
        finalDp += id.value
    }

    return finalDp.dp
}

@AllSecaDevicePreviews
@Composable
fun ClosestSpacingTest() {
    DynamicTheme(secaUiTheme){
        val targetDp = 90f
        Text(
            text = "Spacing in dp for design spacing of $targetDp: " + closestSpacingSum(targetDp)
        )
    }
}