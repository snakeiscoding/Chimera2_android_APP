package com.jht.spinbikeapps.targettraining.ui.topbar

import com.jht.interfaces.ui.BasePanelData
import com.jht.spinbikeapps.targettraining.utils.WorkoutMode
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TargetTrainingPanelData (
    val isTargetTrainingTopBarVisible: Boolean,
    val selectedMode: WorkoutMode
): BasePanelData(TARGET_TRAINING_UUID)