package com.jht.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


/** Indicates state of a software update */
enum class SoftwareUpdateState {
    /** Software update is not doing anything */
    Idle,
    /** Software update is downloading */
    Download,
    /** Software update is ready to be applied */
    Ready,
    /** Software update is being applied */
    Apply
}

/** Callback signature for software update status */
typealias ISoftwareUpdateStatus = (
    /** Current state of software update process */
    state: SoftwareUpdateState,
    /** Progress of the current state as float between 0 and 1. Null if progress is not known. */
    progress0to1: Float?
) -> Unit

/** Stores generic information about a TV Tuner attached to the console */
@Parcelize
class TvTunerInformation(
    val hasTuner: Boolean = false,
    val ip: String = "",
    val mac: String = "",
) : Parcelable

