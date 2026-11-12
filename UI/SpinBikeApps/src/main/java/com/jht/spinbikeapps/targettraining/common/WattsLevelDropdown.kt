package com.jht.spinbikeapps.targettraining.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jht.widgets.DropdownInput
import com.jht.widgets.DropdownInputItem
import com.jht.widgets.FieldInputDefaults
import com.jht.widgets.FieldInputOptions
import com.jht.widgets.R

@Composable
fun WattsLevelDropdown(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: FitnessLevel = FitnessLevel.DEFAULT,
    fieldInputOptions: FieldInputOptions = FieldInputDefaults.defaultStyle(),
    onValueChange: (FitnessLevel)->Unit = {},
) {
    val items = listOf(
        DropdownInputItem(FitnessLevel.VeryHigh, stringResource(id = R.string.very_high_tc)),
        DropdownInputItem(FitnessLevel.High, stringResource(id = R.string.high_tc)),
        DropdownInputItem(FitnessLevel.Medium, stringResource(com.jht.spinbikeapps.R.string.medium_tc)),
        DropdownInputItem(FitnessLevel.Low, stringResource(id = R.string.low_tc)),
        DropdownInputItem(FitnessLevel.VeryLow, stringResource(id = R.string.very_low_tc))
    )
    val current = items.find { it.value == value } ?: items.last()
    DropdownInput(modifier, stringResource(com.jht.spinbikeapps.R.string.cycling_fitness_level_tc) , items, current, enabled, fieldInputOptions) { onValueChange(it.value as FitnessLevel) }
}


enum class FitnessLevel (val value: Int) {
    VeryHigh(0),
    High(1),
    Medium(2),
    Low(3),
    VeryLow(4);

    companion object {
        val DEFAULT = Low
        fun from(id: Int) = entries.find { it.value == id } ?: DEFAULT
    }
}