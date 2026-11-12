package com.jht.customhiit.common.models

import android.os.Parcelable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.jht.customhiit.R
import com.jht.customhiit.ui.HIITColors
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue


/** Interval enumerator for custom hiit workout */
internal enum class HIITIntervalEnum {
    EASY_INTERVAL,
    MEDIUM_INTERVAL,
    HARD_INTERVAL,
    ADDITIONAL_FIELD
}

/** A WorkoutMap with additional fields for custom hiit workout */
@Parcelize
internal data class HIITWorkoutMap(
    val intervalMap: @RawValue Map<HIITIntervalEnum, Map<String, MutableState<Float>>>,
    val selectedInterval: Int = 0,
    val selectionTime: Int = 0
) : Parcelable {
    fun deepCopy(): HIITWorkoutMap = copy(
        // the map needs rebuilding to avoid shallow ref copy
        intervalMap = intervalMap.mapValues { (_, innerMap) ->
            innerMap.mapValues { (_, state) ->
                mutableStateOf(state.value)
            }
        }
    )
}


internal enum class CustomHIITInterval(val intervalImage: Int, val intervalName: String, val intervalColor: Color) {
    EASY_INTERVAL(R.drawable.ic_custom_hiit_easy, "Easy", HIITColors.easyIntervalColor),
    MEDIUM_INTERVAL(R.drawable.ic_custom_hiit_medium, "Medium", HIITColors.mediumIntervalColor),
    HARD_INTERVAL(R.drawable.ic_custom_hiit_hard, "Hard", HIITColors.hardIntervalColor)
}
