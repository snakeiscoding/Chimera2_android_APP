package com.jht.data

import android.os.Parcelable
import com.jht.enums.IntervalGoalType
import com.jht.enums.IntervalType
import com.jht.enums.ProgramType
import kotlinx.parcelize.Parcelize

/**
 * A list of intervals for a progression workout
 */
@Parcelize
data class MmIntervalList(val intervalList: List<ProgressionInterval>) : Parcelable

/**
 * A single interval for a progression workout. This will usually be a time goal, but can rarely
 * be a distance goal.
 */
@Parcelize
data class ProgressionInterval(

    val intervalType: IntervalType,
    val goalType: IntervalGoalType,
    val goalValue: Float,
) : Parcelable


/**
 * Information for a single event in a progression program
 */
@Parcelize
data class EventIndex (
    var programType: ProgramType,
    var eventIndex: Int
) : Parcelable

/** A progression save stamped with the generation active when it was sent, so a stale one can be dropped instead of reapplied. */
@Parcelize
data class ProgressionUpdate(
    val progressionJson: String,
    val generation: Int
) : Parcelable
