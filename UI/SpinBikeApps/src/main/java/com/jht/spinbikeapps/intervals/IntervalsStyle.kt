package com.jht.spinbikeapps.intervals

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jht.resources.robotoFonts
import com.jht.spinbikeapps.targettraining.utils.TargetTrainingColors

//region constants
internal val countdownGraphicWidth: Dp = 1013.dp
internal val intervalRowHeight: Dp = 44.dp
internal val intervalRowWidth: Dp = 490.dp
internal val rowNumberWidth: Dp = 60.dp
internal val rowTimeWidth: Dp = 143.dp
internal val rowNumberHeight: Dp = 28.dp
internal val intervalRowCorner: RoundedCornerShape = RoundedCornerShape(12.dp)
internal val intervalsGraphicWidth: Dp = 587.dp
internal val intervalRowPadding: Dp = 24.dp
internal val previousIntervalsBoxPadding: Dp = 48.dp
//endregion

//region text styles
/** Copied in order to change font size and color elsewhere */
internal val accumulatedValueTextStyle: TextStyle = TextStyle(
    fontSize = 80.sp,
    color = TargetTrainingColors.TIME_ACCUMULATED_TEXT,
    fontWeight = FontWeight.Bold,
    fontFamily = robotoFonts,
)

/** Copied in order to change font size elsewhere */
internal val previousIntervalsTableDataStyle: TextStyle = TextStyle(
    fontSize = 18.sp,
    fontFamily = robotoFonts,
    fontWeight = FontWeight.Normal,
)

/** Copied in order to change font size and color elsewhere */
internal val currentDistanceValueTextStyle: TextStyle = TextStyle(
    fontSize = 80.sp,
    color = TargetTrainingColors.TIME_REMAINING_LABEL_TEXT,
    fontWeight = FontWeight.Bold,
    fontFamily = robotoFonts,
)

internal val previousIntervalsTitleStyle = TextStyle(
    fontSize = 28.sp,
    color = Color.White,
    fontWeight = FontWeight.Medium,
    fontFamily = robotoFonts,
)

internal val intervalNumStyle = TextStyle(
    fontSize = 28.sp,
    color = Color.White,
    fontWeight = FontWeight.Normal,
    fontFamily = robotoFonts
)

internal val workoutPhaseTextStyle = TextStyle(
    fontSize = 48.sp,
    color = TargetTrainingColors.TEXT_TITLE_PRIMARY,
    fontWeight = FontWeight.Bold,
    fontFamily = robotoFonts,
)
//endregion
