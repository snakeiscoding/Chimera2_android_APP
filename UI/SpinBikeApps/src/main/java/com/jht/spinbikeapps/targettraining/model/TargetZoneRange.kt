package com.jht.spinbikeapps.targettraining.model

import androidx.compose.ui.graphics.Color
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BLUE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.BTN_DEFAULT_TEXT
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.GREEN_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.ORANGE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.RED_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.TT_HT_ZONE_1
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors.YELLOW_1


enum class WattsZoneRange(
    val label: String,
    val color: Color,
) {
    BELOW_55("<55%", BTN_DEFAULT_TEXT),
    RANGE_56_75("56-75%", BLUE_1),
    RANGE_76_90("76-90%", GREEN_1),
    RANGE_91_105("91-105%", YELLOW_1),
    RANGE_106_120("106-120%", RED_1),
    RANGE_122_150("122-150%", RED_1),
    ABOVE_150("150% +", RED_1)
}

enum class HeartRateZoneRange(
    val label: String,
    val color: Color
) {
    BELOW_60("<60%", TT_HT_ZONE_1),
    RANGE_60_69("60-69%", BLUE_1),
    RANGE_70_79("70-79%", GREEN_1),
    RANGE_80_89("80-89%", ORANGE_1),
    ABOVE_90("90%", RED_1)
}

enum class HeartRateSelectedZone(val value: String) {
    LESS_THAN_60("<60%"),
    FROM_60_TO_69("60-69%"),
    FROM_70_TO_79("70-79%"),
    FROM_80_TO_89("80-89%"),
    GREATER_THAN_OR_EQUAL_90("90%")
}

enum class FtpZone(val value: String) {
    LESS_THAN_55("<55%"),
    FROM_56_TO_75("56-75%"),
    FROM_76_TO_90("76-90%"),
    FROM_91_TO_105("91-105%"),
    FROM_106_TO_120("106-120%"),
    FROM_122_TO_150("122-150%"),
    GREATER_THAN_150("150% +")
}