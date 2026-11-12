package com.jht.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Indicates the source of the calories calculation */
@Parcelize
enum class CaloriesSource : Parcelable {
    /** Calories derived from standard calculation */
    Standard,
    /** Calories derived from EVolt SMM metrics */
    EVolt,
}
