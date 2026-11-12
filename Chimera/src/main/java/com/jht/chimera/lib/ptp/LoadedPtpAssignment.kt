package com.jht.chimera.lib.ptp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A PTP/PACE assignment loaded on this console, paired with the user it belongs to.
 */
@Parcelize
data class LoadedPtpAssignment(
    val exerciseId: String,
    val ownerUuid: String,
) : Parcelable
