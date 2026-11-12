package com.jht.switchStride.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.jht.widgets.helpers.StatInfo

@Composable
internal fun statInfoToCardData(
    stat: StatInfo,
    icon: Int,
    label: Int
): CardData {

    val text = remember(stat) { stat.text }
    val splitText = remember(stat) { stat.splitText }
    val units = remember(stat) { stat.units }
    val splitUnits = remember(stat) { stat.splitUnits }

    return CardData(
        icon = icon,
        attributeName = stringResource(id = label),
        value = if (splitText.value.isNotBlank()) {
            // For calories stat
            listOf(
                splitText.value to stringResource(id = splitUnits.value),
                text.value to stringResource(id = units.value)
            )
        } else {
            // Single valued stat
            listOf(
                text.value to stringResource(id = units.value)
            )
        }
    )
}