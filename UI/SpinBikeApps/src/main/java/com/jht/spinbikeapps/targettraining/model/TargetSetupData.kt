package com.jht.spinbikeapps.targettraining.model

import androidx.compose.ui.graphics.Color
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import com.jht.widgets.numpad.NumPadDisplay

internal data class TargetSetupData(
    val titleRes: Int,
    val iconRes: Int,
    val descriptionRes: Int,
    val route: String,
    val workoutMode: WorkoutMode,
    val pillBoxes: List<PillBoxData>,
    val targetZoneRange: List<Pair<String,Color>> = listOf()
)

internal data class PillBoxData(
    val title: String,
    val unitValue: String,
    val decimalPlaces: Int,
    val displayType: NumPadDisplay,
    val testTag: String,
    val min: Float,
    val max: Float,
    val stepValue: Float = 1f,
    val defaultValue: Float
)